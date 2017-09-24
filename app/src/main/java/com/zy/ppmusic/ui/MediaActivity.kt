package com.zy.ppmusic.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.ResultReceiver
import android.support.design.widget.BottomSheetDialog
import android.support.v4.app.DialogFragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialog
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.LoopModelAdapter
import com.zy.ppmusic.adapter.MainMenuAdapter
import com.zy.ppmusic.adapter.PlayQueueAdapter
import com.zy.ppmusic.adapter.TimeClockAdapter
import com.zy.ppmusic.bl.BlScanActivity
import com.zy.ppmusic.contract.IMediaActivityContract
import com.zy.ppmusic.entity.MainMenuEntity
import com.zy.ppmusic.presenter.MediaPresenterImpl
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.DataTransform
import com.zy.ppmusic.utils.DateUtil
import com.zy.ppmusic.utils.StringUtils
import com.zy.ppmusic.view.BorderTextView
import com.zy.ppmusic.view.EasyTintView
import kotlinx.android.synthetic.main.activity_media.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * 与MediaService两种通信方式：
 *      1.MediaController.transportControls.playFromMediaId(String, Bundle);//只发送消息（最好与播放器状态相关）
 *      2.SessionCompat.sendCommand(String,Bundle,ResultReceiver);//需要获取结果
 */
class MediaActivity : AppCompatActivity(), IMediaActivityContract.IView {
    private var mMediaBrowser: MediaBrowserCompat? = null
    private var mMainMenuRecycler: RecyclerView? = null
    private var mMainMenuAdapter: MainMenuAdapter? = null
    private var mMediaId: String? = null
    private var mMediaController: MediaControllerCompat? = null//媒体控制器
    private var mPlayQueueList: MutableList<MediaSessionCompat.QueueItem>? = null//播放列表与service同步
    private var ivNextAction: AppCompatImageView? = null//下一首
    private var ivPreviousAction: AppCompatImageView? = null//上一首
    private var ivPlayAction: AppCompatImageView? = null//播放按钮
    private var ivModelAction: AppCompatImageView? = null//模式切换按钮
    private var ivShowQueueAction: AppCompatImageView? = null//展示播放列表
    private var tvDisPlayName: TextView? = null//歌曲名称显示
    private var tvSubName: TextView? = null//作者名称显示
    private var mQueueRecycler: RecyclerView? = null//播放列表的recycler
    private var mBottomQueueDialog: BottomSheetDialog? = null//展示播放列表的dialog
    private var mBottomQueueAdapter: PlayQueueAdapter? = null//播放列表的适配器
    private var mCurrentMediaIdStr: String? = null//当前播放的媒体id
    private var mProgressSeekBar: SeekBar? = null//播放进度条
    private var endPosition: Long = 0//结束位置
    private var stepPosition: Long = 0//自增量
    private var startPosition: Long = 0//起始位置
    private var percent: Float = 0f//当前percent
    private var mLooperHandler: LoopHandler? = null//循环处理
    private var mResultReceive: MediaResultReceive? = null//媒体播放进度处理
    private var mIsTrackingBar: Boolean? = false//是否正在拖动进度条

    private var mPresenter: IMediaActivityContract.IPresenter? = null
    private var mLoadingDialog: AppCompatDialog? = null//加载中的dialog
    private var mLoopModelRecycler: RecyclerView? = null//播放模式的recycler
    private var mLoopModelAdapter: LoopModelAdapter? = null//播放模式的适配器
    private var mBottomLoopModeDialog: BottomSheetDialog? = null//播放模式的dialog
    private var mLoopModelContentView: View? = null//播放模式dialog的content

    private var mTimeClockDialog: BottomSheetDialog? = null//倒计时选择的dialog
    private var mTimeContentView: View? = null
    private var mTimeClockAdapter: TimeClockAdapter? = null
    private var mTimeClockRecycler: RecyclerView? = null
    private var mDelayHandler: Handler? = null
    private var mBorderTextView: BorderTextView? = null
    private var mBottomQueueContentView: View? = null

    /*
     * 实现自循环1s后请求播放器播放的位置
     * controllerCompat 媒体控制器
     * receiver     接收消息回调
     *
     * kotlin静态内部类
     */
    class LoopHandler(controllerCompat: MediaControllerCompat, receiver: ResultReceiver) : Handler() {
        private val mMediaController: WeakReference<MediaControllerCompat> = WeakReference(controllerCompat)

        private val mReceiver: WeakReference<ResultReceiver> = WeakReference(receiver)

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg!!.what) {
                0 -> { //开始循环
                    if (mMediaController.get() != null) {
                        this.removeCallbacksAndMessages(null)
                        this.removeMessages(0)
                        mMediaController.get()!!.sendCommand(MediaService.COMMAND_POSITION, null, mReceiver.get()!!)
                        if (mMediaController.get()!!.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                            this.sendEmptyMessageDelayed(0, 1000)
                        }
                    }
                }
                else -> {//结束循环
                    this.removeCallbacksAndMessages(null)
                }
            }
        }
    }

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
                        mProgressSeekBar!!.progress = percent.toInt()
                    }
                    control_display_time_tv.text = DateUtil.getInstance().getTime(position)
                }
                MediaService.COMMAND_UPDATE_QUEUE_CODE -> {
                    if (mMediaController!!.queue != null && mMediaController!!.queue.size > 0) {
                        if (mBottomQueueAdapter != null) {
                            mBottomQueueAdapter!!.setData(mMediaController!!.queue)
                            showMsg("更新播放列表")
                        }
                        mPlayQueueList = mMediaController!!.queue
                    }
                }
                else -> {
                    println("MediaResultReceive other result....$resultCode," + resultData.toString())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        if (supportActionBar != null) {
            supportActionBar!!.elevation = 0f
        }

        ivNextAction = findViewById(R.id.control_action_next) as AppCompatImageView
        ivPlayAction = findViewById(R.id.control_action_play_pause) as AppCompatImageView
        ivShowQueueAction = findViewById(R.id.control_action_show_queue) as AppCompatImageView
        ivPreviousAction = findViewById(R.id.control_action_previous) as AppCompatImageView
        ivModelAction = findViewById(R.id.control_action_loop_model) as AppCompatImageView

        mMainMenuRecycler = findViewById(R.id.more_function_recycle) as RecyclerView
        mPresenter = MediaPresenterImpl(this)

        val dataList = ArrayList<MainMenuEntity>()
        dataList.add(MainMenuEntity("扫描音乐", R.drawable.ic_search_music))
        dataList.add(MainMenuEntity("蓝牙管理", R.drawable.ic_bl_manager))
        dataList.add(MainMenuEntity("定时关闭", R.drawable.ic_time_clock))
        mMainMenuAdapter = MainMenuAdapter(dataList)
        mMainMenuRecycler!!.adapter = mMainMenuAdapter
        mMainMenuRecycler!!.layoutManager = GridLayoutManager(this, 3)

        mMainMenuAdapter!!.setListener { _, position ->
            //view,position
            when (position) {
                0 -> {//刷新播放列表
                    showMsg(getString(R.string.start_scanning_the_local_file))
                    mPresenter!!.refreshQueue(applicationContext, true)
                }
                1 -> {//蓝牙管理
                    val intent = Intent(this@MediaActivity, BlScanActivity::class.java)
                    startActivity(intent)
                }
                2 -> {//定时关闭
                    mTimeClockDialog = BottomSheetDialog(this@MediaActivity)
                    mTimeContentView = LayoutInflater.from(this@MediaActivity).
                            inflate(R.layout.layout_time_lock, null)
                    mTimeClockRecycler = mTimeContentView!!.findViewById(R.id.time_selector_recycler) as RecyclerView
                    mTimeClockRecycler!!.layoutManager = LinearLayoutManager(this@MediaActivity)
                    mTimeClockAdapter = TimeClockAdapter()
                    //如果正在倒计时显示取消计时选项，否则隐藏
                    if (mBorderTextView != null && mBorderTextView!!.visibility == View.VISIBLE) {
                        mTimeClockAdapter!!.setTicking(true)
                    } else {
                        mTimeClockAdapter!!.setTicking(false)
                    }
                    mTimeClockRecycler!!.adapter = mTimeClockAdapter
                    mTimeClockAdapter!!.setListener { _, p ->
                        mTimeClockDialog!!.cancel()

                        var length: Int = if (mTimeClockAdapter!!.isTick) {
                            if (p == 0) {//发送停止倒计时，隐藏倒计时文本
                                mMediaController!!.transportControls.sendCustomAction(MediaService.ACTION_STOP_COUNT_DOWN, null)
                                mBorderTextView!!.hide()
                                0
                            } else {
                                mTimeClockAdapter!!.getItem(p - 1)
                            }
                        } else {
                            mTimeClockAdapter!!.getItem(p)
                        }
                        if (length != 0) {
                            //如果正在倒计时判断是否需要停止倒计时
                            length *= 1000 * 60
                            val bundle = Bundle()
                            bundle.putLong(MediaService.ACTION_COUNT_DOWN_TIME, length.toLong())
                            mMediaController!!.transportControls.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, bundle)
                            mTimeClockDialog = null
                        }
                    }
                    val lengthSeekBar = mTimeContentView!!.findViewById(R.id.time_selector_seek_bar) as AppCompatSeekBar
                    val progressHintTv = mTimeContentView!!.findViewById(R.id.time_selector_progress_hint_tv) as TextView
                    progressHintTv.visibility = View.VISIBLE
                    progressHintTv.text = String.format(Locale.CHINA, "%d", (lengthSeekBar.progress + 1))
                    lengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        var percent = 0f
                        override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                            progressHintTv.translationX = percent * progress
                            progressHintTv.text = String.format(Locale.CHINA, "%d", (progress + 1))
                        }

                        override fun onStartTrackingTouch(s: SeekBar?) {
                            val transXRound = (s!!.measuredWidth - s.paddingLeft - s.paddingEnd
                                    + progressHintTv.measuredWidth / 2).toFloat()
                            val mMaxProgress = s.max.toFloat()
                            percent = transXRound / mMaxProgress
                            if (mDelayHandler == null) {
                                mDelayHandler = Handler()
                            }
                            mDelayHandler!!.removeCallbacksAndMessages(null)
                            progressHintTv.visibility = View.VISIBLE
                        }

                        override fun onStopTrackingTouch(s: SeekBar?) {
                            mDelayHandler!!.postDelayed({
                                progressHintTv.visibility = View.INVISIBLE
                            }, 1000)
                        }
                    })
                    val btnSure = mTimeContentView!!.findViewById(R.id.time_selector_sure_btn) as Button
                    btnSure.setOnClickListener {
                        val bundle = Bundle()
                        bundle.putLong(MediaService.ACTION_COUNT_DOWN_TIME,
                                ((lengthSeekBar.progress + 1) * 1000 * 60).toLong())
                        mMediaController!!.transportControls.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, bundle)
                        mTimeClockDialog!!.cancel()
                        mTimeClockDialog = null
                    }
                    mTimeClockDialog!!.setContentView(mTimeContentView)
                    mTimeClockDialog!!.show()
                }
            }
        }

        mProgressSeekBar = findViewById(R.id.control_display_progress) as SeekBar
        mProgressSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
                mMediaController!!.transportControls.playFromMediaId(mCurrentMediaIdStr, extra)
                mIsTrackingBar = false
            }
        })

        tvDisPlayName = findViewById(R.id.control_display_title) as TextView
        tvSubName = findViewById(R.id.control_display_sub_title) as TextView
        //下一首的监听
        ivNextAction!!.setOnClickListener({
            mMediaController!!.transportControls.skipToNext()
        })
        //上一首的监听
        ivPreviousAction!!.setOnClickListener({
            mMediaController!!.transportControls.skipToPrevious()
        })
        //循环模式点击监听
        ivModelAction!!.setOnClickListener({
            if (mLoopModelContentView == null) {
                mBottomLoopModeDialog = BottomSheetDialog(this@MediaActivity)
                mLoopModelContentView = layoutInflater.inflate(R.layout.layout_loop_model, null)
                mBottomLoopModeDialog!!.setContentView(mLoopModelContentView, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                mLoopModelRecycler = mLoopModelContentView!!.findViewById(R.id.pop_loop_model_recycler) as RecyclerView
                mLoopModelAdapter = LoopModelAdapter()
                mLoopModelAdapter!!.setListener { _, position ->
                    val item = mLoopModelAdapter!!.getItem(position)
                    if (item != null) {
                        item.isSelected = true
                    }
                    when (item.icon) {
                        R.drawable.ic_loop_model_normal -> {
                            mMediaController!!.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                        }
                        R.drawable.ic_loop_model_only -> {
                            mMediaController!!.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                        }
                        R.drawable.ic_loop_model_random -> {
                            mMediaController!!.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                        }
                    }
                    ivModelAction!!.setImageResource(item.icon)
                    mLoopModelAdapter!!.changeItem()
                    mBottomLoopModeDialog!!.dismiss()
                }
                mLoopModelRecycler!!.adapter = mLoopModelAdapter
                mLoopModelRecycler!!.layoutManager = LinearLayoutManager(this@MediaActivity)
            }
            mBottomLoopModeDialog!!.show()
        })
        //播放按钮监听
        ivPlayAction!!.setOnClickListener({
            //初始化的时候点击的按钮直接播放当前的media
            val extra = Bundle()
            extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
            if (mCurrentMediaIdStr != null) {
                mMediaController!!.transportControls.playFromMediaId(mCurrentMediaIdStr, extra)
            } else {
                println(getString(R.string.empty_play_queue))
                mMediaController!!.transportControls.playFromMediaId("-1", extra)
            }
        })
        //播放列表监听
        ivShowQueueAction!!.setOnClickListener({
            //遗留问题：删除一条item之后再添加进来列表item数量显示错误----------------------------
            mBottomQueueDialog = BottomSheetDialog(this@MediaActivity)
            mBottomQueueContentView = LayoutInflater.from(this@MediaActivity).inflate(R.layout.play_queue_layout, null)
            mQueueRecycler = mBottomQueueContentView!!.findViewById(R.id.control_queue_recycle) as RecyclerView
            mBottomQueueAdapter = PlayQueueAdapter()
            mBottomQueueAdapter!!.setOnQueueItemClickListener { obj, _ ->
                if (obj is MediaSessionCompat.QueueItem) {
                    //点击播放列表直接播放选中的media
                    val extra = Bundle()
                    extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
                    mMediaController!!.transportControls.playFromMediaId(obj.description.mediaId, extra)
                }
                mBottomQueueAdapter!!.setOnQueueItemClickListener(null)
                mBottomQueueAdapter!!.setOnDelListener(null)
                if (mBottomQueueDialog!!.isShowing) {
                    mBottomQueueDialog!!.cancel()
                    mBottomQueueDialog!!.dismiss()
                    mBottomQueueDialog = null
                    mBottomQueueAdapter = null
                }
            }

            mBottomQueueAdapter!!.setOnDelListener { position ->
                val dialog = AlertDialog.Builder(this@MediaActivity)
                dialog.setTitle(getString(R.string.string_sure_del))
                dialog.setMessage(getString(R.string.string_del_desc))
                dialog.setPositiveButton(getString(R.string.string_del)) { _, _ ->
                    mMediaController!!.removeQueueItemAt(position)
                    mPlayQueueList!!.removeAt(position)
                    mBottomQueueAdapter!!.setData(mPlayQueueList)
                    mBottomQueueAdapter!!.notifyItemRemoved(position)
                }
                dialog.setNegativeButton(getString(R.string.string_cancel)) { d, _ ->
                    d.cancel()
                    d.dismiss()
                }
                dialog.create().show()
            }

            mBottomQueueAdapter!!.setLongClickListener { position ->
                val item = mPlayQueueList!![position].description
                val dialog = AlertDialog.Builder(this@MediaActivity)
                dialog.setTitle(String.format(Locale.CHINA, "%s-%s", item.title, item.subtitle))
                dialog.setMessage(item.mediaUri.toString())
                dialog.create().show()
                true
            }

            if (mCurrentMediaIdStr != null) {
                mBottomQueueAdapter!!.selectIndex = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
            }
            mBottomQueueDialog!!.setContentView(mBottomQueueContentView)
            mQueueRecycler!!.adapter = mBottomQueueAdapter
            mQueueRecycler!!.layoutManager = LinearLayoutManager(this)
//            val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
//                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
//                private var isMove = false
//                override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?,
//                                    target: RecyclerView.ViewHolder?): Boolean {
//                    val from = viewHolder!!.adapterPosition
//                    val to = target!!.adapterPosition
//                    mBottomQueueAdapter!!.childMoveTo(from, to)
//                    DataTransform.getInstance().moveTo(from, to)
//                    isMove = true
//                    return isMove
//                }
//
//                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
//                    val dialog = AlertDialog.Builder(this@MediaActivity)
//                    dialog.setTitle(getString(R.string.string_sure_del))
//                    dialog.setMessage(getString(R.string.string_del_desc))
//                    dialog.setPositiveButton(getString(R.string.string_del)) { _, _ ->
//                        mMediaController!!.removeQueueItemAt(viewHolder!!.adapterPosition)
//                        mPlayQueueList!!.removeAt(viewHolder.adapterPosition)
//                        mBottomQueueAdapter!!.setData(mPlayQueueList)
//                        mBottomQueueAdapter!!.notifyItemRemoved(viewHolder.adapterPosition)
//                    }
//                    dialog.setNegativeButton(getString(R.string.string_cancel)) { d, _ ->
//                        d.cancel()
//                        d.dismiss()
//                        mBottomQueueAdapter!!.notifyItemChanged(viewHolder!!.adapterPosition)
//                    }
//                    dialog.create().show()
//                }
//
//                override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?) {
//                    super.clearView(recyclerView, viewHolder)
//                    if (isMove) {
//                        if (viewHolder!!.itemView.background != null) {
//                            viewHolder.itemView.background.alpha = 0
//                        }
//                        mMediaController!!.sendCommand(MediaService.COMMAND_UPDATE_QUEUE, null, mResultReceive)
//                        if (mCurrentMediaIdStr != null && mBottomQueueAdapter != null) {
//                            mBottomQueueAdapter!!.selectIndex = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
//                        }
//                        isMove = false
//                    }
//                }
//            })
//            helper.attachToRecyclerView(mQueueRecycler!!)
            mBottomQueueAdapter!!.setData(mPlayQueueList)
            mBottomQueueDialog!!.show()
        })
    }

    /**
     * IView
     * 刷新列表的回调
     */
    override fun refreshQueue(mPathList: java.util.ArrayList<String>?, isRefresh: Boolean) {
        if (mMediaController == null)
            return
        if (mPathList != null && mPathList.size > 0) {
            showMsg(String.format(Locale.CHINA, getString(R.string.format_string_search_media_count), mPathList.size))
            mMediaController!!.sendCommand(MediaService.COMMAND_UPDATE_QUEUE, null, mResultReceive)
        } else {
            showMsg(getString(R.string.no_media_searched))
            mMediaController!!.transportControls.playFromMediaId("-1", null)
        }
    }

    /**
     * 首次加载完成
     */
    override fun loadFinished() {
        if (mMediaBrowser == null) {
            val extra = Bundle()
            extra.putParcelableArrayList("queueList", DataTransform.getInstance().mediaItemList)
            val serviceComponentName = ComponentName(this, MediaService::class.java)
            mMediaBrowser = MediaBrowserCompat(this, serviceComponentName, mConnectionCallBack, extra)
        }
        if (mMediaBrowser!!.isConnected) {
            mMediaBrowser!!.disconnect()
        }
        mMediaBrowser!!.connect()
    }

    override fun onStart() {
        super.onStart()
        //加载数据
        mPresenter!!.refreshQueue(this@MediaActivity, false)
    }

    override fun onRestart() {
        super.onRestart()
        if(mMediaBrowser != null){
            if (mMediaBrowser!!.isConnected) {
                mMediaBrowser!!.disconnect()
            }
            mMediaBrowser!!.connect()
        }

        startLoop()
    }

    override fun showLoading() {
        println("showLoading。。。。。")
        if (mLoadingDialog == null) {
            mLoadingDialog = AppCompatDialog(this, R.style.TransDialog)
            mLoadingDialog!!.setContentView(LayoutInflater.from(this).inflate(R.layout.loading_layout, null))
        }
        mLoadingDialog!!.show()
    }

    override fun hideLoading() {
        println("hideLoading。。。。。")
        if (mLoadingDialog != null && mLoadingDialog!!.isShowing) {
            mLoadingDialog!!.dismiss()
        }
    }

    /**
     * 连接状态回调
     */
    private val mConnectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            println("connected service....")
            mMediaId = mMediaBrowser!!.root
            mMediaBrowser!!.subscribe(mMediaId!!, subscriptionCallBack)
            mMediaController = MediaControllerCompat(this@MediaActivity, mMediaBrowser!!.sessionToken)
            mResultReceive = MediaResultReceive(Handler())
            mLooperHandler = LoopHandler(mMediaController!!, mResultReceive!!)
            MediaControllerCompat.setMediaController(this@MediaActivity, mMediaController)
            mMediaController!!.registerCallback(mControllerCallBack)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended....")
            mMediaBrowser!!.unsubscribe(mMediaBrowser!!.root, subscriptionCallBack)
            val mediaController = MediaControllerCompat.getMediaController(this@MediaActivity)
            if (mediaController != null) {
                mediaController.unregisterCallback(mControllerCallBack)
                MediaControllerCompat.setMediaController(this@MediaActivity, null)
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed.....")
            mMediaBrowser!!.connect()
        }
    }


    /**
     * 播放器相关回调
     */
    private val subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback() {
        //service加载完成列表回调
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            println("onChildrenLoaded.....activity..." + children)
            if (children.size > 0) {
                //如果当前在播放状态
                if (mMediaController!!.playbackState.state != PlaybackStateCompat.STATE_NONE) {
                    val metadata = mMediaController!!.metadata
                    //请求获取当前播放位置
                    if (metadata != null) {
                        mCurrentMediaIdStr = metadata.description.mediaId
                        val displayTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                        val subTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                        endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        stepPosition = endPosition / 100L
                        startPosition = 0
                        control_display_duration_tv.text = DateUtil.getInstance().getTime(endPosition)
                        mLooperHandler!!.sendEmptyMessage(0)
                        setMediaInfo(displayTitle, subTitle)
                    }
                    handlePlayState(mMediaController!!.playbackState.state)
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
                    mMediaController!!.transportControls.playFromMediaId(mCurrentMediaIdStr, extra)
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
            println("SubscriptionCallback onError called.....")
        }
    }
    /**
     * 媒体控制
     */
    private val mControllerCallBack = object : MediaControllerCompat.Callback() {
        //当前歌曲信息变化回调
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("onMetadataChanged ------------- called")
            if (metadata != null) {
                mCurrentMediaIdStr = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                val displayTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                val subTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                stepPosition = endPosition / 100L
                startPosition = 0
                control_display_duration_tv.text = DateUtil.getInstance().getTime(endPosition)
                println("endPosition=$endPosition,step=$stepPosition")
                setMediaInfo(StringUtils.ifEmpty(displayTitle), StringUtils.ifEmpty(subTitle))
            }
        }

        //播放列表变化回调
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            if (queue!!.size == 0) {
                setMediaInfo(getString(R.string.app_name), getString(R.string.app_name))
                return
            }
            if (mBottomQueueAdapter != null) {
                mBottomQueueAdapter!!.setData(queue)
                showMsg("更新播放列表")
            }
            mPlayQueueList = queue
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            println("onRepeatModeChanged.....")
        }

        //播放器状态改变回调
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            runOnUiThread({
                println("onPlaybackStateChanged....." + state!!.state)
                println("position=" + state.position + ",buffer=" + state.bufferedPosition)
                startPosition = state.position
                percent = ((startPosition * 1.0f) / endPosition * 1.0f)
                mProgressSeekBar!!.progress = (percent * 100f).toInt()
                control_display_time_tv.text = DateUtil.getInstance().getTime(startPosition)
                handlePlayState(state.state)
            })
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
                        mBorderTextView!!.show(ivNextAction, String.format(Locale.CHINA, formatStr, h, m, s))
                    } else {
                        formatStr = getString(R.string.format_string_time_count_down_no_hour)
                        mBorderTextView!!.show(ivNextAction, String.format(Locale.CHINA, formatStr, m, s))
                    }

                }
                MediaService.ACTION_COUNT_DOWN_END -> {
                    if (mBorderTextView != null) {
                        mBorderTextView!!.hide()
                        disConnectService()
                        finish()
                    }
                }
                else -> {
                    println("onSessionEvent....." + event + "," + extras.toString())
                }
            }
        }

        //目前仅当播放器发生错误时调用
        override fun onQueueTitleChanged(title: CharSequence?) {
            super.onQueueTitleChanged(title)
            println("onQueueTitleChanged .... $title")
            showMsg((title as String?)!!)
            //播放下一首
            mMediaController!!.transportControls.skipToNext()
        }

        override fun onExtrasChanged(extras: Bundle?) {
            super.onExtrasChanged(extras)
            println("onExtrasChanged.....")
        }

    }

    /**
     * 播放器状态处理
     */
    fun handlePlayState(state: Int) {
        println("handlePlayState=" + state)
        if (state != PlaybackStateCompat.STATE_PLAYING) {
            stopLoop()
            ivPlayAction!!.setImageResource(R.drawable.ic_black_play)
            if (state == PlaybackStateCompat.STATE_STOPPED) {
                mProgressSeekBar!!.progress = 0
            }
        } else {
            startLoop()
            ivPlayAction!!.setImageResource(R.drawable.ic_black_pause)
        }
    }

    private fun startLoop() {
        if (mLooperHandler != null) {
            mLooperHandler!!.removeCallbacksAndMessages(null)
            mLooperHandler = null
        }
        if (mMediaController != null && mResultReceive != null) {
            mLooperHandler = LoopHandler(mMediaController!!, mResultReceive!!)
            mLooperHandler!!.sendEmptyMessage(0)
        }
    }

    private fun stopLoop() {
        if (mLooperHandler != null) {
            mLooperHandler!!.removeCallbacksAndMessages(null)
            mLooperHandler = null
        }
    }

    fun showMsg(msg: String) {
        EasyTintView.makeText(ivNextAction!!, msg, EasyTintView.TINT_SHORT).show()
    }

    /**
     * 更新媒体信息
     */
    fun setMediaInfo(displayTitle: CharSequence?, subTitle: CharSequence?) {
        tvDisPlayName!!.text = StringUtils.ifEmpty(displayTitle)
        tvSubName!!.text = StringUtils.ifEmpty(subTitle)
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
                mMediaBrowser!!.unsubscribe(mMediaId!!, subscriptionCallBack)
            }
            //断开与媒体服务的链接
            mMediaBrowser!!.disconnect()
            mMediaBrowser = null
        }
    }


    override fun onStop() {
        super.onStop()
        println("onStop called")
        disConnectService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mPresenter != null) {
            mPresenter!!.destroyView()
        }
        if (mLoadingDialog != null) {
            mLoadingDialog!!.dismiss()
            mLoadingDialog!!.cancel()
        }
        println("MediaActivity is destroy")
    }
}
