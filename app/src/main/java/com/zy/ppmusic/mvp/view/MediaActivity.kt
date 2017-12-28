package com.zy.ppmusic.mvp.view

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.SeekBar
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.MediaHeadAdapter
import com.zy.ppmusic.adapter.MenuAdapter
import com.zy.ppmusic.adapter.PlayQueueAdapter
import com.zy.ppmusic.adapter.TimeClockAdapter
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.presenter.MediaPresenterImpl
import com.zy.ppmusic.service.LoopService
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.*
import com.zy.ppmusic.widget.BorderTextView
import com.zy.ppmusic.widget.EasyTintView
import com.zy.ppmusic.widget.RecycleViewDecoration
import com.zy.ppmusic.widget.TimBackGroundDrawable
import kotlinx.android.synthetic.main.activity_media.*
import kotlinx.android.synthetic.main.dl_content_del_item.view.*
import java.util.*

/**
 * 与MediaService两种通信方式：
 *      1.MediaController.transportControls.playFromMediaId(String, Bundle);//只发送消息（最好与播放器状态相关）
 *      2.SessionCompat.sendCommand(String,Bundle,ResultReceiver);//需要获取结果
 */
class MediaActivity : AppCompatActivity(), IMediaActivityContract.IView {
    private val TAG = "MediaActivity"
    private var mMediaBrowser: MediaBrowserCompat? = null
    private var mMediaId: String? = null
    private var mMediaController: MediaControllerCompat? = null//媒体控制器
    private var mPlayQueueList: MutableList<MediaSessionCompat.QueueItem>? = null//播放列表与service同步
    private var mQueueRecycler: RecyclerView? = null//播放列表的recycler
    private var mBottomQueueDialog: BottomSheetDialog? = null//展示播放列表的dialog
    private var mBottomQueueAdapter: PlayQueueAdapter? = null//播放列表的适配器
    private var mCurrentMediaIdStr: String? = null//当前播放的媒体id
    private var endPosition: Long = 0//结束位置
    private var stepPosition: Long = 0//自增量
    private var startPosition: Long = 0//起始位置
    private var percent: Float = 0f//当前percent
    private var mResultReceive: MediaResultReceive? = null//媒体播放进度处理
    private var mIsTrackingBar: Boolean? = false//是否正在拖动进度条

    private var mPresenter: IMediaActivityContract.IPresenter? = null
    private var mLoadingDialog: AppCompatDialog? = null//加载中的dialog
    private var mBottomLoopModePop: ListPopupWindow? = null//播放模式的dialog

    private var mTimeClockDialog: BottomSheetDialog? = null//倒计时选择的dialog
    private var mTimeContentView: View? = null
    private var mTimeClockAdapter: TimeClockAdapter? = null
    private var mTimeClockRecycler: RecyclerView? = null
    private var mDelayHandler: Handler? = null
    private var mBorderTextView: BorderTextView? = null
    private var mBottomQueueContentView: View? = null
    private var mQueueCountTv: TextView? = null
    private var mHeadAdapter: MediaHeadAdapter? = null

    /**
     * 接收媒体服务回传的信息，这里处理的是当前播放的位置和进度
     * kotlin普通内部类
     */
    inner class MediaResultReceive(handler: Handler) : ResultReceiver(handler) {

        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            super.onReceiveResult(resultCode, resultData)
            when (resultCode) {
                MediaService.COMMAND_POSITION_CODE -> {
                    val position = resultData.getInt("position").toLong()
                    startPosition = position
                    percent = ((startPosition * 100f) / endPosition * 1.0f)
                    if (mIsTrackingBar!!.not()) {
                        control_display_progress.progress = percent.toInt()
                    }
                    control_display_time_tv.text = DateUtil.getInstance().getTime(position)
                }
                MediaService.COMMAND_UPDATE_QUEUE_CODE -> {
                    if (mMediaController?.queue != null && mMediaController!!.queue.size > 0) {
                        if (mBottomQueueAdapter != null) {
                            mBottomQueueAdapter?.setData(mMediaController?.queue)
                            showMsg("更新播放列表")
                        }
                        mPlayQueueList = mMediaController?.queue
                    }
                }
                else -> {
                    PrintOut.print("MediaResultReceive other result....$resultCode," + resultData.toString())
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        if (supportActionBar != null) {
            supportActionBar?.elevation = 0f
        }

        val drawable = TimBackGroundDrawable()
        drawable.setDrawableColor(ContextCompat.getColor(this, R.color.colorTheme))
        drawable.setPercent(TimBackGroundDrawable.TOP)

        ViewCompat.setBackground(media_title_tint, drawable)

        mPresenter = MediaPresenterImpl(this)

        //刷新播放列表
        iv_search_media.setOnClickListener {
            showMsg(getString(R.string.start_scanning_the_local_file))
            mPresenter?.refreshQueue(applicationContext, true)
        }
        //蓝牙管理
        iv_bl_manage.setOnClickListener {
            val intent = Intent(this@MediaActivity, BlScanActivity::class.java)
            startActivity(intent)
        }
        //定时关闭
        iv_time_clock.setOnClickListener {
            createTimeClockDialog()
        }

        control_display_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    control_display_time_tv.text = DateUtil.getInstance().getTime(progress * stepPosition)
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
                mMediaController?.transportControls?.playFromMediaId(mCurrentMediaIdStr, extra)
                mIsTrackingBar = false
            }
        })
        //下一首的监听
        control_action_next.setOnClickListener({
            mMediaController?.transportControls?.skipToNext()
        })
        //上一首的监听
        control_action_previous.setOnClickListener({
            mMediaController?.transportControls?.skipToPrevious()
        })
        //循环模式点击监听
        control_action_loop_model.setOnClickListener({
            createLoopModePop()
        })
        //播放按钮监听
        control_action_play_pause.setOnClickListener({
            //初始化的时候点击的按钮直接播放当前的media
            val extra = Bundle()
            extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
            if (mCurrentMediaIdStr != null) {
                mMediaController?.transportControls?.playFromMediaId(mCurrentMediaIdStr, extra)
            } else {
                PrintOut.print(getString(R.string.empty_play_queue))
                mMediaController?.transportControls?.playFromMediaId("-1", extra)
            }
        })
        //播放列表监听
        control_action_show_queue.setOnClickListener({
            createBottomQueueDialog()
        })


    }

    private val mHeadChangeListener = object : ViewPager.OnPageChangeListener {

        private var selectedPosition = 0

        override fun onPageScrollStateChanged(state: Int) {
            val nextMediaId = mPlayQueueList!![selectedPosition].description.mediaId
            val currentIndex = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
            if(currentIndex == selectedPosition && Math.abs(currentIndex - selectedPosition) > 1){
                PrintOut.e("current $currentIndex selected $selectedPosition")
                return
            }

            if (state == ViewPager.SCROLL_STATE_IDLE && nextMediaId != mCurrentMediaIdStr) {

                val extra = Bundle()
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
                mMediaController?.transportControls?.playFromMediaId(
                        mPlayQueueList!![selectedPosition].description.mediaId, extra)
            }
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        }

        override fun onPageSelected(position: Int) {
            this.selectedPosition = position
        }
    }

    /**
     * 创建播放列表
     */
    private fun createBottomQueueDialog() {
        mBottomQueueDialog = BottomSheetDialog(this@MediaActivity)
        if (mBottomQueueAdapter == null) {
            mBottomQueueAdapter = PlayQueueAdapter()
            mBottomQueueAdapter?.setOnQueueItemClickListener { obj, _ ->
                if (obj is MediaSessionCompat.QueueItem) {
                    //点击播放列表直接播放选中的media
                    val extra = Bundle()
                    extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
                    mMediaController?.transportControls?.playFromMediaId(obj.description.mediaId, extra)
                }
                mBottomQueueAdapter?.setOnQueueItemClickListener(null)
                mBottomQueueAdapter?.setOnDelListener(null)
                if (mBottomQueueDialog!!.isShowing) {
                    mBottomQueueDialog?.cancel()
                    mBottomQueueDialog?.dismiss()
                    mBottomQueueDialog = null
                    mBottomQueueAdapter = null
                }
            }

            mBottomQueueAdapter?.setOnDelListener { position ->
                createDelQueueItemDialog(position)
            }

            mBottomQueueAdapter?.setLongClickListener { position ->
                createQueueItemDetailDialog(position)
            }
        }
        if (mBottomQueueContentView == null) {
            mBottomQueueContentView = LayoutInflater.from(this@MediaActivity).
                    inflate(R.layout.play_queue_layout, null)
            mQueueRecycler = mBottomQueueContentView?.findViewById(R.id.control_queue_recycle)
            mQueueCountTv = mBottomQueueContentView?.findViewById(R.id.control_queue_count)
        } else {
            val parent = mBottomQueueContentView?.parent as ViewGroup
            parent.removeView(mBottomQueueContentView)
        }

        if (mCurrentMediaIdStr != null && mBottomQueueAdapter != null) {
            mBottomQueueAdapter?.selectIndex = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
        }
        mQueueCountTv?.text = String.format(Locale.CHINA, getString(R.string.string_queue_playing_position),
                (mBottomQueueAdapter!!.selectIndex + 1), if (mPlayQueueList == null) 0 else mPlayQueueList?.size)
        mBottomQueueDialog?.setContentView(mBottomQueueContentView)
        mQueueRecycler?.adapter = mBottomQueueAdapter
        mQueueRecycler?.layoutManager = LinearLayoutManager(this)
        mQueueRecycler?.addItemDecoration(RecycleViewDecoration(this@MediaActivity, LinearLayoutManager.VERTICAL,
                R.drawable.recyclerview_vertical_line, UIUtils.dp2px(this@MediaActivity, 25)))
        mBottomQueueAdapter?.setData(mPlayQueueList)
        mBottomQueueDialog?.show()
    }

    private fun createQueueItemDetailDialog(position: Int): Boolean {
        if (mPlayQueueList == null) {
            return false
        }

        if (position < 0 || position >= mPlayQueueList!!.size) {
            return false
        }

        val item = mPlayQueueList!![position].description ?: return false
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(String.format(Locale.CHINA, getString(R.string.show_name_and_author),
                item.title, item.subtitle))
        dialog.setMessage(item.mediaUri.toString())
        dialog.create().show()
        return true
    }

    private fun createDelQueueItemDialog(position: Int) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(getString(R.string.string_sure_del))
        val delContentView = LayoutInflater.from(this).inflate(R.layout.dl_content_del_item, null)
        delContentView.setPadding(UIUtils.dp2px(this, 20), UIUtils.dp2px(this, 20),
                UIUtils.dp2px(this, 10), UIUtils.dp2px(this, 10))
        dialog.setView(delContentView)
        dialog.setPositiveButton(getString(R.string.string_del)) { _, _ ->
            if (delContentView.checkbox_dl_content_message.isChecked) {
                mPresenter?.deleteFile(DataTransform.getInstance().getPath(position)) as Boolean
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(DataTransform.getInstance().getPath(position)))
                sendBroadcast(intent)
            }
            mMediaController!!.removeQueueItem(mPlayQueueList!![position].description)
            mPlayQueueList?.removeAt(position)
            mBottomQueueAdapter?.setData(mPlayQueueList)
            mBottomQueueAdapter?.notifyItemRemoved(position)
            mQueueCountTv?.text = String.format(Locale.CHINA, getString(R.string.string_queue_playing_position),
                    (mBottomQueueAdapter!!.selectIndex + 1), mPlayQueueList?.size)
        }
        dialog.setNegativeButton(getString(R.string.string_cancel)) { d, _ ->
            d.cancel()
            d.dismiss()
        }
        dialog.create().show()
    }

    /**
     * 创建循环模式
     */
    private fun createLoopModePop() {
        if (mBottomLoopModePop == null) {
            mBottomLoopModePop = ListPopupWindow(this@MediaActivity)
            mBottomLoopModePop?.anchorView = control_action_loop_model
            mBottomLoopModePop?.setDropDownGravity(Gravity.TOP and Gravity.END)
            mBottomLoopModePop?.setAdapter(MenuAdapter(this@MediaActivity))
            mBottomLoopModePop?.setContentWidth(UIUtils.dp2px(this@MediaActivity, 110))
            mBottomLoopModePop?.horizontalOffset = control_action_loop_model.measuredWidth
            mBottomLoopModePop?.setOnItemClickListener { _, _, position, _ ->
                setPlayMode(position)
                mBottomLoopModePop?.dismiss()
            }
        }
        mBottomLoopModePop?.show()
    }

    /**
     * 创建倒计时
     */
    private fun createTimeClockDialog() {
        mTimeClockDialog = BottomSheetDialog(this@MediaActivity)
        mTimeContentView = LayoutInflater.from(this@MediaActivity).
                inflate(R.layout.layout_time_lock, null)
        mTimeClockRecycler = mTimeContentView?.findViewById(R.id.time_selector_recycler)
        mTimeClockRecycler?.layoutManager = LinearLayoutManager(this@MediaActivity)
        mTimeClockAdapter = TimeClockAdapter()
        //如果正在倒计时显示取消计时选项，否则隐藏
        if (mBorderTextView != null && mBorderTextView?.visibility == View.VISIBLE) {
            mTimeClockAdapter?.setTicking(true)
        } else {
            mTimeClockAdapter?.setTicking(false)
        }
        mTimeClockRecycler?.adapter = mTimeClockAdapter
        mTimeClockAdapter?.setListener { _, p ->
            mTimeClockDialog?.cancel()
            if (getCurrentTimeClockLength(p) != 0) {
                //如果正在倒计时判断是否需要停止倒计时
                val bundle = Bundle()
                bundle.putLong(MediaService.ACTION_COUNT_DOWN_TIME, (getCurrentTimeClockLength(p) * 1000 * 60).toLong())
                mMediaController?.transportControls?.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, bundle)
                mTimeClockDialog = null
            }
        }
        val lengthSeekBar = mTimeContentView?.findViewById<SeekBar>(R.id.time_selector_seek_bar)
        val progressHintTv = mTimeContentView?.findViewById<TextView>(R.id.time_selector_progress_hint_tv)
        progressHintTv?.visibility = View.VISIBLE
        progressHintTv?.text = String.format(Locale.CHINA, "%d", (lengthSeekBar!!.progress + 1))
        lengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var percent = 0f
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                progressHintTv?.translationX = percent * progress
                progressHintTv?.text = String.format(Locale.CHINA, "%d", (progress + 1))
            }

            override fun onStartTrackingTouch(s: SeekBar?) {
                val transXRound = (s!!.measuredWidth - s.paddingLeft - s.paddingRight
                        + progressHintTv!!.measuredWidth / 2).toFloat()
                val mMaxProgress = s.max.toFloat()
                percent = transXRound / mMaxProgress
                if (mDelayHandler == null) {
                    mDelayHandler = Handler()
                }
                mDelayHandler?.removeCallbacksAndMessages(null)
                progressHintTv.visibility = View.VISIBLE
            }

            override fun onStopTrackingTouch(s: SeekBar?) {
                mDelayHandler?.postDelayed({
                    progressHintTv?.visibility = View.INVISIBLE
                }, 1000)
            }
        })
        val btnSure = mTimeContentView?.findViewById<View>(R.id.time_selector_sure_btn)
        btnSure?.setOnClickListener {
            val bundle = Bundle()
            bundle.putLong(MediaService.ACTION_COUNT_DOWN_TIME,
                    ((lengthSeekBar.progress + 1) * 1000 * 60).toLong())
            mMediaController?.transportControls?.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, bundle)
            mTimeClockDialog?.cancel()
            mTimeClockDialog = null
        }
        mTimeClockDialog?.setContentView(mTimeContentView)
        mTimeClockDialog?.show()
    }

    /**
     * 获取当前倒计时长度
     */
    private fun getCurrentTimeClockLength(position: Int): Int {
        //如果处于倒计时状态，第一条为取消倒计时
        return if (mTimeClockAdapter!!.isTick) {
            //发送停止倒计时，隐藏倒计时文本
            if (position == 0) {
                mMediaController?.transportControls?.sendCustomAction(MediaService.ACTION_STOP_COUNT_DOWN, null)
                mBorderTextView?.hide()
                0
            } else {
                mTimeClockAdapter!!.getItem(position - 1)
            }
        } else {
            mTimeClockAdapter!!.getItem(position)
        }
    }

    /**
     * IView
     * 刷新列表的回调
     */
    override fun refreshQueue(mPathList: java.util.ArrayList<String>?, isRefresh: Boolean) {
        if (mMediaController == null)
            return
        updateHead()
        if (mPathList != null && mPathList.size > 0) {
            showMsg(String.format(Locale.CHINA, getString(R.string.format_string_search_media_count), mPathList.size))
            mMediaController?.sendCommand(MediaService.COMMAND_UPDATE_QUEUE, null, mResultReceive)
        } else {
            showMsg(getString(R.string.no_media_searched))
            mMediaController?.transportControls?.playFromMediaId("-1", null)
        }
    }

    private fun updateHead() {
        if (mHeadAdapter == null) {
            mHeadAdapter = MediaHeadAdapter(supportFragmentManager, DataTransform.getInstance().pathList)
            vp_show_media_head.offscreenPageLimit = 1
            vp_show_media_head.addOnPageChangeListener(mHeadChangeListener)
            vp_show_media_head.adapter = mHeadAdapter
        } else {
            mHeadAdapter!!.setPathList(DataTransform.getInstance().pathList)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {

        //清除状态缓存，避免出现异常，界面刷新由onStart方法中完成
        //猜测是由于fragment数据大小超出限制
        outState?.clear()
        outState?.keySet()?.forEachIndexed { index, s ->
            outState.remove(s)
        }
        super.onSaveInstanceState(outState)
        println("onSaveInstanceState............................." + outState?.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        println("onRestoreInstanceState.............................")
    }

    /**
     * 首次加载完成
     */
    override fun loadFinished() {
        updateHead()
        if (mMediaBrowser == null) {
            val extra = Bundle()
            extra.putParcelableArrayList("queueList", DataTransform.getInstance().mediaItemList)
            val serviceComponentName = ComponentName(this, MediaService::class.java)
            mMediaBrowser = MediaBrowserCompat(this, serviceComponentName, mConnectionCallBack, extra)
        }
        if (mMediaBrowser!!.isConnected) {
            mMediaBrowser?.disconnect()
        }
        mMediaBrowser?.connect()
    }

    override fun onStart() {
        super.onStart()
        //加载数据
        mPresenter?.refreshQueue(this@MediaActivity, false)
    }

    override fun onRestart() {
        super.onRestart()
        if (mMediaBrowser != null) {
            if (mMediaBrowser!!.isConnected) {
                mMediaBrowser?.disconnect()
            }
            mMediaBrowser?.connect()
        }
        startLoop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun showLoading() {
        PrintOut.print("showLoading。。。。。")
        if (mLoadingDialog == null) {
            mLoadingDialog = AppCompatDialog(this, R.style.TransDialog)
            mLoadingDialog?.setContentView(LayoutInflater.from(this).inflate(R.layout.loading_layout, null))
            mLoadingDialog?.setCanceledOnTouchOutside(false)
        }
        if (mLoadingDialog!!.isShowing) {
            return
        }
        mLoadingDialog?.show()
    }

    override fun hideLoading() {
        PrintOut.print("hideLoading。。。。。")
        if (mLoadingDialog != null && mLoadingDialog!!.isShowing) {
            mLoadingDialog?.dismiss()
        }
    }

    /**
     * 连接状态回调
     */
    private val mConnectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            PrintOut.print("connected service....")
            mMediaId = mMediaBrowser?.root
            mMediaBrowser?.subscribe(mMediaId!!, subscriptionCallBack)
            mMediaController = MediaControllerCompat(this@MediaActivity, mMediaBrowser!!.sessionToken)
            mResultReceive = MediaResultReceive(Handler())
            MediaControllerCompat.setMediaController(this@MediaActivity, mMediaController)
            mMediaController?.registerCallback(mControllerCallBack)
            loadMode()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            PrintOut.print("onConnectionSuspended....")
            mMediaBrowser?.unsubscribe(mMediaBrowser!!.root, subscriptionCallBack)
            val mediaController = MediaControllerCompat.getMediaController(this@MediaActivity)
            if (mediaController != null) {
                mediaController.unregisterCallback(mControllerCallBack)
                MediaControllerCompat.setMediaController(this@MediaActivity, null)
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            PrintOut.print("onConnectionFailed.....")
            mMediaBrowser?.connect()
        }
    }

    private fun loadMode() {
        if (mPresenter != null && mMediaController != null) {
            setPlayMode(mPresenter?.getLocalMode(applicationContext)!!)
        }
    }

    private fun setPlayMode(mode: Int) {
        when (mode) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> {
                control_action_loop_model.setImageResource(R.drawable.ic_loop_model_normal)
            }
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                control_action_loop_model.setImageResource(R.drawable.ic_loop_model_only)
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                control_action_loop_model.setImageResource(R.drawable.ic_loop_mode_list)
            }
        }
        mMediaController?.transportControls?.setRepeatMode(mode)
        if (mPresenter != null) {
            mPresenter?.changeMode(this, mode)
        }
    }


    /**
     * 播放器相关回调
     */
    private val subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback() {
        //service加载完成列表回调
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            PrintOut.print("onChildrenLoaded.....activity..." + children)
            if (children.size > 0) {
                //如果当前在播放状态
                if (mMediaController?.playbackState?.state != PlaybackStateCompat.STATE_NONE) {
                    val metadata = mMediaController?.metadata
                    //请求获取当前播放位置
                    if (metadata != null) {
                        mCurrentMediaIdStr = metadata.description.mediaId
                        val displayTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                        val subTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                        endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        stepPosition = endPosition / 100L
                        startPosition = 0
                        control_display_duration_tv.text = DateUtil.getInstance().getTime(endPosition)
                        setMediaInfo(displayTitle, subTitle)
                        val position = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
                        if (mHeadAdapter == null) {
                            updateHead()
                        }
                        vp_show_media_head.setCurrentItem(position, false)
                        PrintOut.e("seek to position $position")
                    }
                    handlePlayState(mMediaController!!.playbackState!!.state)
                } else {
                    val subTitle = if (children[0].description.subtitle == null) {
                        "unknown"
                    } else {
                        children[0].description.subtitle as String
                    }
                    setMediaInfo(children[0].mediaId as String, subTitle)
                    mCurrentMediaIdStr = children[0].description.mediaId
                    val extra = Bundle()
                    extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_INIT)
                    mMediaController?.transportControls?.playFromMediaId(mCurrentMediaIdStr, extra)
                }
            } else {
                control_display_time_tv.text = getString(R.string.string_time_init)
                control_display_duration_tv.text = getString(R.string.string_time_init)
                setMediaInfo(getString(R.string.app_name), getString(R.string.app_name))
            }
            hideLoading()
        }

        //播放列表加载失败
        override fun onError(parentId: String) {
            super.onError(parentId)
            PrintOut.print("SubscriptionCallback onError called.....")
        }
    }
    /**
     * 媒体控制
     */
    private val mControllerCallBack = object : MediaControllerCompat.Callback() {
        //当前歌曲信息变化回调
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            PrintOut.print("onMetadataChanged ------------- called")
            if (metadata != null) {
                mCurrentMediaIdStr = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                val displayTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                val subTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                stepPosition = endPosition / 100L
                startPosition = 0
                control_display_duration_tv.text = DateUtil.getInstance().getTime(endPosition)
                PrintOut.print("endPosition=$endPosition,step=$stepPosition")
                if (mCurrentMediaIdStr != null && mBottomQueueAdapter != null) {
                    mBottomQueueAdapter?.selectIndex = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
                    mBottomQueueAdapter?.notifyDataSetChanged()
                }
                setMediaInfo(StringUtils.ifEmpty(displayTitle, getString(R.string.unknown_name)),
                        StringUtils.ifEmpty(subTitle, getString(R.string.unknown_name)))
                val position = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
                if (mHeadAdapter != null && mHeadAdapter!!.count > position) {
                    vp_show_media_head.setCurrentItem(position, false)
                }
            }
        }

        //播放列表变化回调
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            if (queue?.size == 0) {
                setMediaInfo(getString(R.string.app_name), getString(R.string.app_name))
                return
            }
            if (mBottomQueueAdapter != null) {
                mBottomQueueAdapter?.setData(queue)
                showMsg("更新播放列表")
            }
            mPlayQueueList = queue
            updateHead()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            PrintOut.print("onRepeatModeChanged.....")
        }

        //播放器状态改变回调
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            PrintOut.print("onPlaybackStateChanged....." + state?.state)
            PrintOut.print("position=" + state?.position + ",buffer=" + state?.bufferedPosition)
            startPosition = state!!.position
            percent = ((startPosition * 1.0f) / endPosition * 1.0f)
            control_display_progress.progress = (percent * 100f).toInt()
            control_display_time_tv.text = DateUtil.getInstance().getTime(startPosition)
            handlePlayState(state.state)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
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
                    val mis = extras!!.getLong(MediaService.ACTION_COUNT_DOWN_TIME)
                    if (mBorderTextView == null) {
                        mBorderTextView = BorderTextView(this@MediaActivity)
                    }
                    val h = mis / (60L * 60L * 1000L)
                    val m = (mis / (60L * 1000L)) - h * 60L
                    val s = (mis % (60L * 1000L)) / 1000L
                    val formatStr: String
                    if (h > 0) {
                        formatStr = getString(R.string.format_string_time_count_down_with_hour)
                        mBorderTextView?.show(control_action_next, String.format(Locale.CHINA, formatStr, h, m, s))
                    } else {
                        formatStr = getString(R.string.format_string_time_count_down_no_hour)
                        mBorderTextView?.show(control_action_next, String.format(Locale.CHINA, formatStr, m, s))
                    }

                }
                MediaService.ACTION_COUNT_DOWN_END -> {
                    if (mBorderTextView != null) {
                        mBorderTextView?.hide()
                        disConnectService()
                        finish()
                    }
                }
                else -> {
                    PrintOut.print("onSessionEvent....." + event + "," + extras.toString())
                }
            }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            super.onExtrasChanged(extras)
            PrintOut.print("onExtrasChanged.....")
        }

    }

    /**
     * 播放器状态处理
     */
    fun handlePlayState(state: Int) {
        PrintOut.print("handlePlayState=" + state)
        if (state != PlaybackStateCompat.STATE_PLAYING) {
            stopLoop()
            control_action_play_pause.setImageResource(R.drawable.ic_black_play)
            if (state == PlaybackStateCompat.STATE_STOPPED) {
                control_display_progress.progress = 0
            }
        } else {
            startLoop()
            control_action_play_pause.setImageResource(R.drawable.ic_black_pause)
        }
    }

    private var filter: IntentFilter? = null
    private var isStarted = false

    private fun startLoop() {
        if (!isStarted) {
            if (filter == null) {
                filter = IntentFilter("com.zy.ppmusic.LoopReceiver")
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, filter!!)
            startService(Intent(this, LoopService::class.java))
        }
    }


    private val localReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (mMediaController != null && mResultReceive != null) {
                if (mMediaController!!.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                    mMediaController!!.sendCommand(MediaService.COMMAND_POSITION, null, mResultReceive)
                }
            }
        }
    }

    private fun stopLoop() {
        if (!isStarted) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)
            stopService(Intent(this, LoopService::class.java))
            isStarted = false
        }
    }

    fun showMsg(msg: String) {
        EasyTintView.makeText(control_action_next, msg, EasyTintView.TINT_SHORT).show()
    }

    /**
     * 更新媒体信息
     */
    fun setMediaInfo(displayTitle: String?, subTitle: String?) {
        supportActionBar?.title = displayTitle
        supportActionBar?.subtitle = subTitle
    }

    /**
     * 断开媒体服务
     */
    fun disConnectService() {
        //释放控制器
        val mediaController = MediaControllerCompat.getMediaController(this@MediaActivity)
        if (mediaController != null) {
            mediaController.unregisterCallback(mControllerCallBack)
            MediaControllerCompat.setMediaController(this@MediaActivity, null)
        }
        //停止进度更新
        stopLoop()
        //取消播放状态监听
        if (mMediaBrowser != null) {
            if (mMediaId != null) {
                mMediaBrowser?.unsubscribe(mMediaId!!, subscriptionCallBack)
            }
            //断开与媒体服务的链接
            mMediaBrowser?.disconnect()
            mMediaBrowser = null
        }
    }


    override fun onStop() {
        super.onStop()
        PrintOut.print("onStop called")
        disConnectService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mPresenter != null) {
            mPresenter?.destroyView()
        }

        vp_show_media_head.removeOnPageChangeListener(mHeadChangeListener)

        if (mLoadingDialog != null) {
            mLoadingDialog?.dismiss()
            mLoadingDialog?.cancel()
        }
        PrintOut.print("MediaActivity is destroy")
    }
}
