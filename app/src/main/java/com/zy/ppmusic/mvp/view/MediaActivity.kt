package com.zy.ppmusic.mvp.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import androidx.annotation.Keep
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.MediaHeadAdapter
import com.zy.ppmusic.adapter.PlayQueueAdapter
import com.zy.ppmusic.adapter.TimeClockAdapter
import com.zy.ppmusic.adapter.base.OnItemViewClickListener
import com.zy.ppmusic.databinding.ActivityMediaBinding
import com.zy.ppmusic.databinding.DlContentDelItemBinding
import com.zy.ppmusic.mvp.base.AbstractBaseMvpActivity
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.presenter.MediaPresenterImpl
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.*
import com.zy.ppmusic.widget.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * 与MediaService两种通信方式：
 *      1.MediaController.transportControls.playFromMediaId(String, Bundle);//只发送消息（最好与播放器状态相关）
 *      2.SessionCompat.sendCommand(String,Bundle,ResultReceiver);//需要获取结果
 */
@Keep
class MediaActivity : AbstractBaseMvpActivity<MediaPresenterImpl>(), IMediaActivityContract.IMediaActivityView {

    private var mMediaBrowser: MediaBrowserCompat? = null
    /*** 媒体控制器*/
    private var mMediaController: MediaControllerCompat? = null
    /*** 播放列表的recycler*/
    private var mMediaQueueRecycler: androidx.recyclerview.widget.RecyclerView? = null
    /*** 展示播放列表的dialog*/
    private var mMediaQueueDialog: BottomSheetDialog? = null
    /*** 播放列表的适配器*/
    private var mMediaQueueAdapter: PlayQueueAdapter? = null
    /*** 当前播放的媒体id*/
    private var mCurrentMediaIdStr: String? = null
    /*** 结束位置*/
    private var endPosition: Long = 0
    /*** 自增量*/
    private var stepPosition: Long = 0
    /*** 起始位置*/
    private var startPosition: Long = 0
    /*** 请求删除权限*/
    private val requestDelPermissionCode = 10002
    /*** 媒体播放进度处理*/
    private var mResultReceive: MediaResultReceive? = null
    /*** 是否正在拖动进度条*/
    private var mIsTrackingBar: Boolean = false
    /*** 倒计时选择的dialog*/
    private var mTimeClockDialog: BottomSheetDialog? = null
    /*** 倒计时弹窗ContentView*/
    private var mTimeContentView: View? = null
    /*** 倒计时Recycler的适配器*/
    private var mTimeClockAdapter: TimeClockAdapter? = null
    /*** 倒计时选择弹窗Recycler*/
    private var mTimeClockRecycler: androidx.recyclerview.widget.RecyclerView? = null
    /*** 倒计时展示的自定义TextView*/
    private var mBorderTextView: BorderTextView? = null
    /*** 播放列表弹窗的contentView*/
    private var mPlayQueueContentView: View? = null
    /*** 显示播放列表数量*/
    private var mQueueCountTv: TextView? = null
    /*** 歌曲图片适配器*/
    private var mHeadAdapter: MediaHeadAdapter? = null
    /*** 记录循环是否已经开始*/
    private var isStarted = false

    /*** 接收媒体服务回传的信息，这里处理的是当前播放的位置和进度*/
    @Keep
    inner class MediaResultReceive(activity: MediaActivity, handler: Handler) : ResultReceiver(handler) {
        private var mWeakView: WeakReference<MediaActivity> = WeakReference(activity)

        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            super.onReceiveResult(resultCode, resultData)
            if (mWeakView.get() == null) {
                return
            }
            val activity = mWeakView.get() ?: return
            when (resultCode) {
                MediaService.COMMAND_POSITION_CODE -> {
                    val position = resultData.getInt(MediaService.EXTRA_POSITION).toLong()
                    activity.startPosition = position
                    activity.updateTime()
                }
                MediaService.COMMAND_UPDATE_QUEUE_CODE -> {
                    val isNotEmpty = activity.mMediaController?.queue?.isNotEmpty() ?: false
                    if (isNotEmpty) {
                        activity.mMediaQueueAdapter?.setData(activity.mMediaController?.queue)
                    }
                    activity.hideLoading()
                }
                else -> {
                    PrintLog.print("MediaResultReceive other result....$resultCode,$resultData")
                }
            }
        }
    }

    companion object {
        fun action(context: Context) {
            Intent(context, MediaActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }

    private var loader: Loader? = null
    private val binding: ActivityMediaBinding by lazy { ActivityMediaBinding.bind(contentView) }
    private val mediaToolbar: Toolbar by lazy { binding.mediaToolbar }
    private val mediaTitleTintView: View by lazy { binding.mediaTitleTintView }
    private val bottomTintView: View by lazy { binding.bottomTintView }
    private val contentViewPager: ViewPager by lazy { binding.contentViewPager }
    private val mediaSeekBar: AppCompatSeekBar by lazy { binding.mediaSeekBar }
    private val playingTimeTextView: AppCompatTextView by lazy { binding.playingTimeTextView }
    private val durationTimeTextView: AppCompatTextView by lazy { binding.durationTimeTextView }
    private val loopModelImageView: AppCompatImageView by lazy { binding.loopModelImageView }
    private val playOrPauseImageView: AppCompatImageView by lazy { binding.playOrPauseImageView }
    private val showPlayQueueImageView: AppCompatImageView by lazy { binding.showPlayQueueImageView }

    override fun getContentViewId(): Int = R.layout.activity_media

    override fun createPresenter(): MediaPresenterImpl = MediaPresenterImpl(this)

    private fun setUpTitleBar() {
        mediaToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTheme))
        mediaToolbar.elevation = 0f
        setSupportActionBar(mediaToolbar)
    }

    private fun setUpTopEdgeView(){
        //顶部斜边View的背景
        val drawable = TimBackGroundDrawable()
        drawable.setDrawableColor(ContextCompat.getColor(this, R.color.colorTheme))
        drawable.setPercent(TimBackGroundDrawable.TOP)
        ViewCompat.setBackground(mediaTitleTintView, drawable)
    }

    private fun setUpBottomEdgeView() {
        //设置底部的斜边
        val bottomBackGround = TimBackGroundDrawable()
        bottomBackGround.setDrawableColor(UIUtils.getColor(R.color.colorTheme))
        bottomBackGround.setCorner(TimBackGroundDrawable.LEFT)
        bottomBackGround.setPercent(TimBackGroundDrawable.BOTTOM)
        ViewCompat.setBackground(bottomTintView, bottomBackGround)
    }

    private fun setUpCenterBackGround() {
        val dp2px = UIUtils.dp2px(this, 110)
        val vpDrawable = RoundDrawable(dp2px, ContextCompat.getColor(this, R.color.colorGray))
        ViewCompat.setBackground(contentViewPager, vpDrawable)
    }

    private var mModeIndex = 0

    override fun initViews() {
        setUpTitleBar()
        setUpTopEdgeView()
        setUpBottomEdgeView()
        //专辑图片的圆形背景
        setUpCenterBackGround()
        mediaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playingTimeTextView.text = DateUtil.get().getTime(progress * stepPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                mIsTrackingBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //初始化的时候点击的按钮直接播放当前的media
                val extra = Bundle()
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_SEEK_TO)
                extra.putInt(MediaService.SEEK_TO_POSITION_PARAM, (seekBar!!.progress * stepPosition).toInt())
                mPresenter?.playWithId(mCurrentMediaIdStr!!, extra)
                mIsTrackingBar = false
            }
        })
        //循环模式点击监听
        loopModelImageView.setOnClickListener {
            mModeIndex++
            setPlayMode(mModeIndex % 3)
        }
        //播放按钮监听
        playOrPauseImageView.setOnClickListener {
            //初始化的时候点击的按钮直接播放当前的media
            val extra = Bundle()
            extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
            if (mCurrentMediaIdStr != null) {
                mPresenter?.playWithId(mCurrentMediaIdStr!!, extra)
            } else {
                PrintLog.print(getString(R.string.empty_play_queue))
                mPresenter?.playWithId("-1", extra)
            }
        }
        showPlayQueueImageView.setOnClickListener {
            createBottomQueueDialog()
        }
    }


    /*专辑图片位置改变监听*/
    private val mHeadChangeListener = object : ViewPager.OnPageChangeListener {
        private var dragBeforeIndex = -1

        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                dragBeforeIndex = contentViewPager.currentItem
            }
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            if (positionOffset == 0f && positionOffsetPixels == 0) {
                if (dragBeforeIndex == contentViewPager.currentItem) {
                    return
                }
                PrintLog.i("-----------ViewPager index 更新 ${contentViewPager.currentItem}")
                val currentMediaId = DataProvider.get().mediaIdList[contentViewPager.currentItem]
                if (currentMediaId == mCurrentMediaIdStr) {
                    return
                }
                PrintLog.e("准备播放第${contentViewPager.currentItem}首")
                mPresenter?.skipToPosition(contentViewPager.currentItem.toLong())
            }
        }

        /**
         * 直接跳转到最后一个或者第一个只有这个
         */
        override fun onPageSelected(position: Int) {
            onPageScrolled(position, 0f, 0)
            println("dragIndex====$dragBeforeIndex but now position is $position")
        }
    }

    @Keep
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (menu != null) {
            if (menu::class == MenuBuilder::class) {
                try {
                    val method = menu::class.java.getDeclaredMethod("setOptionalIconsVisible",
                            Boolean::class.java)
                    method.isAccessible = true
                    method.invoke(menu, true)
                } catch (e: Exception) {
                    PrintLog.d("反射显示图标失败")
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_media_scan -> {
                showMsg(getString(R.string.start_scanning_the_local_file))
                mPresenter?.refreshQueue(true)
            }
//            R.id.menu_blue_connect -> {
//                val intent = Intent(this, BlScanActivity::class.java)
//                startActivity(intent)
//            }
            R.id.menu_notify_style -> {
                ChooseStyleDialog().show(supportFragmentManager, "选择通知栏样式")
            }
            R.id.menu_count_time -> {
                createTimeClockDialog()
            }
        }
        return true
    }

    /**
     * 创建播放列表
     */
    @SuppressLint("InflateParams")
    private fun createBottomQueueDialog() {
        mMediaQueueDialog = BottomSheetDialog(this)
        if (mMediaQueueAdapter === null) {
            mMediaQueueAdapter = PlayQueueAdapter()
            mMediaQueueAdapter?.setItemClickListener { v, index ->
                if (v.id == R.id.queue_item_del) {
                    createDelQueueItemDialog(index)
                    return@setItemClickListener
                }
                //点击播放列表直接播放选中的media
                mPresenter?.skipToPosition(index.toLong())
                if (mMediaQueueDialog!!.isShowing) {
                    mMediaQueueDialog?.cancel()
                    mMediaQueueDialog?.dismiss()
                    mMediaQueueDialog = null
                    mMediaQueueAdapter = null
                }
            }

            mMediaQueueAdapter?.setItemLongClickListener { _, position ->
                createQueueItemDetailDialog(position)
            }
        }
        if (mPlayQueueContentView == null) {
            mPlayQueueContentView = LayoutInflater.from(this).inflate(R.layout.play_queue_layout, null)
            mMediaQueueRecycler = mPlayQueueContentView?.findViewById(R.id.control_queue_recycle)
            mQueueCountTv = mPlayQueueContentView?.findViewById(R.id.control_queue_count)
        } else {
            val parent = mPlayQueueContentView?.parent as? ViewGroup
            parent?.removeView(mPlayQueueContentView)
        }

        if (mCurrentMediaIdStr != null) {
            mMediaQueueAdapter?.selectIndex = DataProvider.get().getMediaIndex(mCurrentMediaIdStr!!)
        }
        mQueueCountTv?.text = String.format(Locale.CHINA, getString(R.string.string_queue_playing_position),
                (mMediaQueueAdapter?.selectIndex?.plus(1) ?: 1), DataProvider.get().pathList.size)
        mMediaQueueDialog?.setContentView(mPlayQueueContentView!!)
        mMediaQueueRecycler?.adapter = mMediaQueueAdapter
        mMediaQueueDialog?.dismissWithAnimation = true
        mMediaQueueRecycler?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        mMediaQueueRecycler?.addItemDecoration(RecycleViewDecoration(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL,
                R.drawable.recyclerview_vertical_line, UIUtils.dp2px(this, 25)))
        mMediaQueueAdapter?.setData(DataProvider.get().queueItemList)
        mMediaQueueDialog?.show()
    }

    /**
     * 显示列表item的详细信息
     */
    private fun createQueueItemDetailDialog(position: Int): Boolean {
        if ((position in 0..DataProvider.get().queueItemList.size).not()) {
            return false
        }

        val item = DataProvider.get().queueItemList[position].description ?: return false
        AlertDialog.Builder(this)
                .setTitle(String.format(Locale.CHINA, getString(R.string.show_name_and_author), item.title, item.subtitle))
                .setMessage(item.mediaUri.toString())
                .create()
                .show()
        return true
    }

    /**
     * 显示确认删除提示
     */
    private fun createDelQueueItemDialog(position: Int) {
        val delContentBinding = DlContentDelItemBinding.inflate(layoutInflater)
        val delContentView = delContentBinding.root
        delContentView.setPadding(UIUtils.dp2px(this, 20), UIUtils.dp2px(this, 20),
                UIUtils.dp2px(this, 10), UIUtils.dp2px(this, 10))
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.string_sure_del))
                .setView(delContentView)
                .setPositiveButton(getString(R.string.string_del)) { _, _ ->
                    when (val success = mPresenter?.deleteFile(delContentBinding.checkboxDlContentMessage.isChecked, position)) {
                        null -> needDocumentPermission(position)
                        else -> toast(if (success) "删除成功" else "删除失败")
                    }
                }
                .setNegativeButton(getString(R.string.string_cancel)) { d, _ ->
                    d.cancel()
                    d.dismiss()
                }
                .create()
                .show()
    }

    override fun setDeleteResult(isSuccess: Boolean, path: String?) {

    }

    override fun needDocumentPermission(position: Int) {
        toast("需要授予权限")
        doDelActionPosition = position
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            startActivityForResult(this, requestDelPermissionCode)
        }
    }

    private var doDelActionPosition = -1

    private fun getRootId(uri: String): String {
        val start = uri.lastIndexOf("/") + 1
        val end = uri.lastIndexOf("%")
        return uri.substring(start, end)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        //请求删除外置存储权限
        if (requestCode == requestDelPermissionCode && resultCode == Activity.RESULT_OK) {
            if (intent == null || intent.data == null) {
                return
            }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(intent.data,
                DocumentsContract.getTreeDocumentId(intent.data))
            mPresenter?.setGrantedRootUri(intent.data!!.toString(), childrenUri.toString())
            if (doDelActionPosition != -1) {
                mPresenter?.deleteFile(includeFile = true, doDelActionPosition)
                doDelActionPosition = -1
            }
        }
    }

    /**
     * 创建倒计时
     */
    @SuppressLint("InflateParams")
    private fun createTimeClockDialog() {
        mTimeClockDialog = BottomSheetDialog(this)
        mTimeContentView = LayoutInflater.from(this).inflate(R.layout.layout_time_lock, null)
        mTimeClockRecycler = mTimeContentView?.findViewById(R.id.time_selector_recycler) as androidx.recyclerview.widget.RecyclerView?
        mTimeClockRecycler?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        mTimeClockAdapter = TimeClockAdapter()
        //无用的参数直接下划线表示
        mTimeClockAdapter?.setOnItemClickListener(OnItemViewClickListener { _, position ->
            mTimeClockDialog?.cancel()
            if (getCurrentTimeClockLength(position) != 0L) {
                //如果正在倒计时判断是否需要停止倒计时
                val bundle = Bundle()
                bundle.putLong(MediaService.ACTION_COUNT_DOWN_TIME, getCurrentTimeClockLength(position))
                mPresenter?.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, bundle)
                mTimeClockDialog = null
            }
        })
        //如果正在倒计时显示取消计时选项，否则隐藏
        if (mBorderTextView != null && mBorderTextView?.visibility == View.VISIBLE) {
            mTimeClockAdapter?.setTicking(true)
        } else {
            mTimeClockAdapter?.setTicking(false)
        }
        mTimeClockRecycler?.adapter = mTimeClockAdapter
        val lengthSeekBar = mTimeContentView?.findViewById(R.id.time_selector_seek_bar) as SeekBar
        val progressHintTv = mTimeContentView?.findViewById(R.id.time_selector_progress_hint_tv) as TextView
        progressHintTv.visibility = View.VISIBLE
        progressHintTv.text = String.format(Locale.CHINA, "%d", (lengthSeekBar.progress + 1))
        lengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var percent = 0f
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                progressHintTv.translationX = percent * progress
                progressHintTv.text = String.format(Locale.CHINA, "%d", (progress + 1))
            }

            override fun onStartTrackingTouch(s: SeekBar?) {
                val transXRound = (s!!.measuredWidth - s.paddingLeft - s.paddingRight
                        + progressHintTv.measuredWidth / 2).toFloat()
                percent = transXRound / s.max.toFloat()
                progressHintTv.visibility = View.VISIBLE
            }

            override fun onStopTrackingTouch(s: SeekBar?) {
                mTimeClockRecycler?.postDelayed({
                    progressHintTv.visibility = View.INVISIBLE
                }, 1000)
            }
        })
        mTimeContentView?.findViewById<Button>(R.id.time_selector_sure_btn)?.setOnClickListener {
            val bundle = Bundle()
            bundle.putLong(MediaService.ACTION_COUNT_DOWN_TIME,
                    ((lengthSeekBar.progress + 1) * 1000 * 60).toLong())
            mPresenter?.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, bundle)
            mTimeClockDialog?.cancel()
            mTimeClockDialog = null
        }
        mTimeClockDialog?.setContentView(mTimeContentView!!)
        mTimeClockDialog?.show()
    }

    override fun onResume() {
        super.onResume()
        mPresenter?.refreshQueue(false)
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    /**
     * 获取当前倒计时长度
     * 如果处于倒计时状态，第一条为取消倒计时
     */
    private fun getCurrentTimeClockLength(position: Int): Long =
            if (mTimeClockAdapter!!.isTick) {
                //发送停止倒计时，隐藏倒计时文本
                if (position == 0) {
                    mPresenter?.sendCustomAction(MediaService.ACTION_STOP_COUNT_DOWN, Bundle())
                    mBorderTextView?.hide()
                    0L
                } else {
                    mTimeClockAdapter!!.getItem(position - 1) * 1000L * 60L
                }
            } else {
                mTimeClockAdapter!!.getItem(position) * 1000L * 60L
            }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                mMediaController?.dispatchMediaButtonEvent(event)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateHead() = if (mHeadAdapter == null) {
        mHeadAdapter = MediaHeadAdapter(supportFragmentManager, DataProvider.get().pathList)
        contentViewPager.offscreenPageLimit = 2
        contentViewPager.addOnPageChangeListener(mHeadChangeListener)
        contentViewPager.adapter = mHeadAdapter
    } else {
        mHeadAdapter?.setPathList(DataProvider.get().pathList)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //清除状态缓存，避免出现异常，界面刷新由onStart方法中完成
        //猜测是由于fragment数据大小超出限制
//        outState?.clear()
        super.onSaveInstanceState(Bundle())
        println("onSaveInstanceState.............................$outState")
    }

    override fun loadFinished(isForce: Boolean) {
        if (mMediaController == null) {
            PrintLog.d("media controller为空---")
            connectMediaService()
            return
        }
        if (DataProvider.get().pathList.size > 0) {
            if (mResultReceive != null) {
                mPresenter?.sendCommand(MediaService.COMMAND_UPDATE_QUEUE, Bundle().apply {
                    putBoolean(MediaService.EXTRA_SCAN_COMPLETE, isForce)
                }, mResultReceive!!)
            } else {
                PrintLog.d("参数ResultReceive为空。")
            }
        } else {
            showMsg(getString(R.string.no_media_searched))
            mPresenter?.playWithId("-1", Bundle())
        }
    }

    private fun connectMediaService() {
        if (mMediaBrowser?.isConnected == true) {
            mMediaBrowser?.disconnect()
            mMediaBrowser = null
        }

        if (mMediaBrowser == null) {
            mMediaBrowser = MediaBrowserCompat(this,
                    ComponentName(this, MediaService::class.java), mConnectionCallBack, null)
        }
        try {
            mMediaBrowser?.connect()
        } catch (e: IllegalStateException) {
            PrintLog.e("正在连接服务...")
        }
    }

    /**
     * 显示加载框
     */
    override fun showLoading() {
        if(loader == null){
            loader = Loader.show(this)
        }
    }

    /**
     * 隐藏加载框
     */
    override fun hideLoading() {
        PrintLog.d("hideLoading")
        loader?.hide()
        loader = null
    }

    /**
     * 连接状态回调
     */
    private val mConnectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            if (isFinishing) {
                return
            }
            PrintLog.print("已连接服务....")
            //mMediaBrowser!!.root 对应service的BrowserRoot 可以是包名
            mMediaBrowser?.subscribe(mMediaBrowser!!.root, subscriptionCallBack)
            mMediaController = MediaControllerCompat(this@MediaActivity,
                    mMediaBrowser!!.sessionToken)
            mResultReceive = MediaResultReceive(this@MediaActivity, Handler(Looper.myLooper() ?: Looper.getMainLooper()))
            MediaControllerCompat.setMediaController(this@MediaActivity, mMediaController)
            mMediaController?.registerCallback(mControllerCallBack)
            loadMode()
            mPresenter.attachModelController(mMediaController)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            PrintLog.print("服务已断开....")
            if (isFinishing.not()) {
                connectMediaService()
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            PrintLog.print("连接服务失败.....")
            if (isFinishing.not()) {
                connectMediaService()
            }
        }
    }

    override fun setRepeatMode(mode: Int) {
        if (mMediaController != null) {
            mModeIndex = mode
            setPlayMode(mModeIndex)
        }
    }

    /**
     * 加载本地缓存的播放模式
     */
    private fun loadMode() {
        mPresenter?.getLocalMode(applicationContext)
    }

    /**
     * 设置播放模式
     */
    private fun setPlayMode(mode: Int) {
        when (mode) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> {
                showMsg("顺序播放")
                loopModelImageView.setImageResource(R.drawable.ic_loop_mode_normal_svg)
            }
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                showMsg("单曲循环")
                loopModelImageView.setImageResource(R.drawable.ic_loop_mode_only_svg)
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                showMsg("列表循环")
                loopModelImageView.setImageResource(R.drawable.ic_loop_mode_list_svg)
            }
        }
        mPresenter?.setRepeatMode(applicationContext, mode)
    }

    /**
     * 播放器相关回调
     */
    @Keep
    private val subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback() {
        //service加载完成列表回调
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            PrintLog.print("onChildrenLoaded.....activity... size " + children.size)
            if (children.size > 0) {
                if (mHeadAdapter == null) {
                    updateHead()
                }
                //如果当前在播放状态
                if (mMediaController?.playbackState?.state != PlaybackStateCompat.STATE_NONE) {
                    //请求获取当前播放位置
                    mMediaController?.metadata?.apply {
                        endPosition = this.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        updateTime()
                        mCurrentMediaIdStr = this.description.mediaId
                        setMediaInfo(this.description.title?.toString(), this.description.subtitle?.toString())
                        val position = mCurrentMediaIdStr?.let {
                            DataProvider.get().getMediaIndex(it)
                        } ?: 0
                        contentViewPager.setCurrentItem(position, false)
//                        updateQueueSize(position + 1, mHeadAdapter!!.count)
                    }
                    handlePlayState(mMediaController!!.playbackState!!.state)
                } else {
                    setMediaInfo(children[0].description.title?.toString(), children[0].description.subtitle?.toString())
                    mCurrentMediaIdStr = children[0].description.mediaId
                    mCurrentMediaIdStr?.apply {
                        //                        val position = DataProvider.get().getMediaIndex(this)
//                        updateQueueSize(position + 1, DataProvider.get().mediaIdList.size)
                        val extra = Bundle()
                        extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_INIT)
                        mPresenter?.playWithId(this, extra)
                    }
                }
            } else {
                playingTimeTextView.text = getString(R.string.string_time_init)
                durationTimeTextView.text = getString(R.string.string_time_init)
                setMediaInfo(getString(R.string.app_name), getString(R.string.app_name))
            }
            hideLoading()
        }

        //播放列表加载失败
        override fun onError(parentId: String) = PrintLog.print("SubscriptionCallback onError called.....")
    }

    private fun updateTime() {
        //正在拖动不更新
        if (mIsTrackingBar) {
            return
        }
        stepPosition = endPosition / 100L
        if (endPosition != 0L) {
            val percent = ((startPosition * 1.0f) / endPosition * 1.0f)
            mediaSeekBar.progress = (percent * 100f).toInt()
            durationTimeTextView.text = DateUtil.get().getTime(endPosition)
        } else {
            durationTimeTextView.text = resources.getString(R.string.string_time_init)
        }
        startPosition = takeIf { startPosition > endPosition }?.endPosition ?: startPosition
        playingTimeTextView.text = DateUtil.get().getTime(startPosition)
    }

    /**
     * 媒体控制
     */
    @Keep
    private val mControllerCallBack = object : MediaControllerCompat.Callback() {
        //当前歌曲信息变化回调
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            PrintLog.print("onMetadataChanged ------------- called")
            updateMetadata(metadata)
        }

        fun updateMetadata(metadata: MediaMetadataCompat?) {
            if (metadata == null) return
            mCurrentMediaIdStr = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            updateTime()
            val position: Int? = mCurrentMediaIdStr?.let { DataProvider.get().getMediaIndex(it) }
            if (position == null || position < 0) return
            //只在dialog显示时刷新
            mMediaQueueDialog?.apply {
                if (this.isShowing) {
                    mQueueCountTv?.text = String.format(Locale.CHINA, getString(R.string.string_queue_playing_position),
                            position + 1, mMediaQueueAdapter?.itemCount)
                    mMediaQueueAdapter?.selectIndex = position
                    mMediaQueueAdapter?.notifyDataSetChanged()
                }
            }

            setMediaInfo(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE),
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
            if (contentViewPager.currentItem != position) {
                contentViewPager.setCurrentItem(position, false)
            }
//            updateQueueSize(position + 1, DataProvider.get().pathList.size)
        }

        //播放列表变化回调
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            PrintLog.print("onQueueChanged called size=${queue?.size}")
            if (queue == null || queue.isEmpty()) {
                setMediaInfo(getString(R.string.app_name), getString(R.string.app_name))
                mMediaQueueDialog?.dismiss()
                return
            }
            contentViewPager.clearOnPageChangeListeners()
            updateHead()
            mMediaQueueDialog?.let {
                if (it.isShowing) {
                    val currentIndex = DataProvider.get().getMediaIndex(mCurrentMediaIdStr!!)
                    contentViewPager.setCurrentItem(currentIndex, false)
//                    updateQueueSize(currentIndex + 1, DataProvider.get().pathList.size)
                    mMediaQueueAdapter?.selectIndex = currentIndex
                    mMediaQueueAdapter?.setData(queue)
                    mQueueCountTv?.text = String.format(Locale.CHINA, getString(R.string.string_queue_playing_position), currentIndex + 1, queue.size)
                }
            }
            showMsg("更新播放列表")
            contentViewPager.addOnPageChangeListener(mHeadChangeListener)
        }

        //播放器状态改变回调
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            PrintLog.print("onPlaybackStateChanged....." + state?.state)
            PrintLog.print("position=" + state?.position + ",buffer=" + state?.bufferedPosition)
            PrintLog.print("endPosition=$endPosition")
            startPosition = state?.position ?: 0
            if (DataProvider.get().pathList.isEmpty()) {
                setMediaInfo(getString(R.string.app_name), getString(R.string.app_name))
                mMediaQueueDialog?.dismiss()
            }
            updateTime()
            handlePlayState(state?.state ?: PlaybackStateCompat.STATE_NONE)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                MediaService.LOCAL_CACHE_POSITION_EVENT -> {
                    startPosition = extras?.getLong(MediaService.LOCAL_CACHE_POSITION_EVENT) ?: 0L
                    PrintLog.d("收到穿过来的缓存位置----$startPosition")
                    updateTime()
                }
                MediaService.ERROR_PLAY_QUEUE_EVENT -> {
                    showMsg(getString(R.string.empty_play_queue))
                }
                MediaService.LOADING_QUEUE_EVENT -> {
                    showMsg(getString(R.string.queue_loading))
                    showLoading()
                }
                MediaService.LOAD_COMPLETE_EVENT -> {
                    showMsg(getString(R.string.loading_complete))
                    hideLoading()
                }
                MediaService.ACTION_COUNT_DOWN_TIME -> {
                    val mis = extras?.getLong(MediaService.ACTION_COUNT_DOWN_TIME)
                    if (mBorderTextView == null) {
                        mBorderTextView = BorderTextView(this@MediaActivity)
                    }
                    mBorderTextView?.show(contentViewPager, DateUtil.get().getTime(mis))
                }

                MediaService.ACTION_COUNT_DOWN_END -> {
                    mBorderTextView?.hide()
                    disConnectService()
                    finish()
                }
                MediaService.RESET_SESSION_EVENT -> {
                    PrintLog.d("重新连接服务")
                    connectMediaService()
                }
                MediaService.UPDATE_POSITION_EVENT -> {
                    startPosition = extras?.getInt(MediaService.UPDATE_POSITION_EVENT, 0)?.toLong() ?: 0
                    PrintLog.d("更新列表的位置-----$startPosition")
                    updateTime()
                }
                else -> {
                    PrintLog.e("this event was not intercepted")
                }
            }
        }
    }

    /**
     * 播放器状态处理
     */
    fun handlePlayState(state: Int) {
        PrintLog.print("handlePlayState=$state")
        if (state != PlaybackStateCompat.STATE_PLAYING) {
            stopLoop()
            playOrPauseImageView.setImageResource(R.drawable.ic_black_play)
            if (state == PlaybackStateCompat.STATE_STOPPED) {
                startPosition = 0
                updateTime()
            }
        } else {
            startLoop()
            playOrPauseImageView.setImageResource(R.drawable.ic_black_pause)
        }
    }

    private fun startLoop() {
        if (!isStarted) {
            mMediaController?.sendCommand(MediaService.COMMAND_START_LOOP, null, null)
            isStarted = true
        }
    }

    private fun stopLoop() {
        if (isStarted) {
            mMediaController?.sendCommand(MediaService.COMMAND_STOP_LOOP, null, null)
            isStarted = false
        }
    }

    private fun showMsg(msg: String) {
        EasyTintView.makeText(contentViewPager, msg, EasyTintView.TINT_SHORT).show()
    }

    /**
     * 更新媒体信息
     */
    private fun setMediaInfo(displayTitle: String?, subTitle: String?) {
        supportActionBar?.title = displayTitle ?: UIUtils.getString(R.string.unknown_name)
        supportActionBar?.subtitle = subTitle ?: UIUtils.getString(R.string.unknown_author)
    }

    /**
     * 断开媒体服务
     */
    private fun disConnectService() {
        //停止进度更新
        stopLoop()
        //释放控制器
        mMediaController?.unregisterCallback(mControllerCallBack)
        mMediaController = null
        MediaControllerCompat.setMediaController(this, null)
        //取消播放状态监听
        //在一些特殊情况，状态还是正在连接时调用getRoot会出现状态错误
        if (mMediaBrowser?.isConnected == true && mMediaBrowser?.root != null) {
            mMediaBrowser?.unsubscribe(mMediaBrowser!!.root, subscriptionCallBack)
        }
        //断开与媒体服务的链接
        mMediaBrowser?.disconnect()
        mMediaBrowser = null
    }


    override fun onStop() {
        super.onStop()
        PrintLog.print("onStop called")
        if (isFinishing) {
            println("即将关闭----")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PrintLog.print("MediaActivity is destroy")
        //正常按键返回退出调用
        disConnectService()

        //去除ViewPager的监听
        contentViewPager.clearOnPageChangeListeners()
        ViewCompat.setBackground(mediaTitleTintView, null)
        ViewCompat.setBackground(contentViewPager, null)
        ViewCompat.setBackground(bottomTintView, null)
        //释放播放列表弹窗
        if (mMediaQueueDialog != null) {
            mMediaQueueDialog?.dismiss()
            mQueueCountTv = null
            mPlayQueueContentView = null
            mMediaQueueDialog = null
            mMediaQueueAdapter = null
        }
        //释放时间计时弹窗
        if (mTimeClockDialog != null) {
            mTimeClockDialog?.dismiss()
            mTimeContentView = null
            mTimeClockDialog = null
            mTimeClockAdapter = null
        }
        mResultReceive = null
        //去除SeekBar的监听
        mediaSeekBar.setOnSeekBarChangeListener(null)

        showPlayQueueImageView.setOnClickListener(null)
        loopModelImageView.setOnClickListener(null)
        playOrPauseImageView.setOnClickListener(null)
    }
}
