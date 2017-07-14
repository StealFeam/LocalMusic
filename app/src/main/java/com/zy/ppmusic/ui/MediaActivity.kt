package com.zy.ppmusic.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.ResultReceiver
import android.support.design.widget.BottomSheetDialog
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.ArrayMap
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.MainMenuAdapter
import com.zy.ppmusic.adapter.PlayQueueAdapter
import com.zy.ppmusic.contract.IMediaActivityContract
import com.zy.ppmusic.entity.MainMenuEntity
import com.zy.ppmusic.presenter.MediaPresenterImpl
import com.zy.ppmusic.ui.fragment.BlScanFragment
import com.zy.ppmusic.service.MediaService
import java.lang.ref.WeakReference

class MediaActivity : AppCompatActivity(),IMediaActivityContract.IView{
    private var mMediaBrowser: MediaBrowserCompat? = null
    var mMediaId: String? = null
    var mediaController: MediaControllerCompat? = null
    var playQueue: MutableList<MediaBrowserCompat.MediaItem>? = null
    var ivNextAction: ImageView? = null
    var ivPlayAction: ImageView? = null
    var ivMenuAction: ImageView? = null
    var tvDisPlayName: TextView? = null
    var tvSubName: TextView? = null
    var queueRecycle: RecyclerView? = null
    var queueDialog: BottomSheetDialog? = null
    var queueAdapter: PlayQueueAdapter? = null
    var currentMediaId: String? = null
    var displayProgress: SeekBar? = null
    var endPosition: Long = 0//结束位置
    var stepPosition: Long = 0//自增量
    var startPosition: Long = 0//起始位置
    var percent: Float = 0f//当前percent
    var mLooperHandler: LoopHandler? = null//循环处理
    var mResultReceive: MediaResultReceive? = null//媒体播放进度处理
    var mIsTrackingBar: Boolean? = false//是否正在拖动进度条
    var mFunctionRecycler:RecyclerView?=null
    var mFragmentManager:FragmentManager ?= null
    var mFragmentMap:ArrayMap<String,Fragment> ?= null
    var mPresenter:IMediaActivityContract.IPresenter ?=null
    /*
     * kotlin静态内部类
     * 实现自循环1s后请求播放器播放的位置
     * controllerCompat 媒体控制器
     * receiver     接收消息回调
     */
    class LoopHandler(controllerCompat: MediaControllerCompat, receiver: ResultReceiver) : Handler() {
        val mMediaController: WeakReference<MediaControllerCompat> = WeakReference<MediaControllerCompat>(controllerCompat)
        val mReceiver: WeakReference<ResultReceiver> = WeakReference<ResultReceiver>(receiver)

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg!!.what) {
                0 -> { //开始循环
                    mMediaController.get()!!.sendCommand(MediaService.COMMAND_POSITION, null, mReceiver.get()!!)
                    this.sendEmptyMessageDelayed(0, 1000)
                }
                else -> {//结束循环
                    this.removeCallbacksAndMessages(null)
                }
            }
        }
    }

    /**
     * kotlin普通内部类
     * 接收媒体服务回传的信息，这里处理的是当前播放的位置和进度
     */
    inner class MediaResultReceive(handler: Handler) : ResultReceiver(handler) {

        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            super.onReceiveResult(resultCode, resultData)
            when (resultCode) {
                MediaService.COMMAND_POSITION_CODE -> {
                    val position = resultData.getInt("position").toLong()
                    startPosition = position
                    percent = ((startPosition * 1.0f) / endPosition * 1.0f)
                    if (mIsTrackingBar!!.not()) {
                        displayProgress!!.progress = (percent * 100f).toInt()
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
        val serviceComponentName = ComponentName(this, MediaService::class.java)
        mMediaBrowser = MediaBrowserCompat(this, serviceComponentName, mConnectionCallBack, null)

        ivNextAction = findViewById(R.id.control_action_next) as ImageView
        ivPlayAction = findViewById(R.id.control_action_play_pause) as ImageView
        ivMenuAction = findViewById(R.id.control_action_show_queue) as ImageView

        mFunctionRecycler = findViewById(R.id.more_function_recycle) as RecyclerView

        mPresenter = MediaPresenterImpl(this)

        val dataList = ArrayList<MainMenuEntity>()
        dataList.add(MainMenuEntity("扫描音乐", R.drawable.ic_search_music))
        dataList.add(MainMenuEntity("蓝牙管理", R.drawable.ic_bl_manager))
        val adapter = MainMenuAdapter(dataList)
        mFunctionRecycler!!.adapter = adapter
        mFunctionRecycler!!.layoutManager = GridLayoutManager(this,2)
        mFragmentManager = supportFragmentManager

        mFragmentMap = ArrayMap()

        adapter.setListener { _, position -> //view,position
            val beginTransaction = mFragmentManager!!.beginTransaction()

            when(position){
                0->{//刷新播放列表
                    mPresenter!!.refreshQueue()
                }
                1->{
                    if(mFragmentMap!![BlScanFragment.TAG] == null){
                        mFragmentMap!!.put(BlScanFragment.TAG, BlScanFragment())
                        beginTransaction.add(R.id.function_content,mFragmentMap!![BlScanFragment.TAG])
                    }
                    if(mFragmentMap!![BlScanFragment.TAG]!!.isHidden){
                        beginTransaction.show(mFragmentMap!![BlScanFragment.TAG])
                    }else{
                        beginTransaction.hide(mFragmentMap!![BlScanFragment.TAG])
                    }
                }
            }
            beginTransaction.commitNowAllowingStateLoss()
        }

        displayProgress = findViewById(R.id.control_display_progress) as SeekBar
        displayProgress!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                mIsTrackingBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //初始化的时候点击的按钮直接播放当前的media
                val extra = Bundle()
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_SEEK_TO)
                extra.putInt("position", (seekBar!!.progress * stepPosition).toInt())
                mediaController!!.transportControls.playFromMediaId(currentMediaId, extra)
                mIsTrackingBar = false
            }
        })

        tvDisPlayName = findViewById(R.id.control_display_title) as TextView
        tvSubName = findViewById(R.id.control_display_sub_title) as TextView

        ivNextAction!!.setOnClickListener({
            mediaController!!.transportControls.skipToNext()
        })

        ivPlayAction!!.setOnClickListener({
            //初始化的时候点击的按钮直接播放当前的media
            val extra = Bundle()
            if (mediaController!!.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_OR_PAUSE)
            } else {
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
            }
            mediaController!!.transportControls.playFromMediaId(currentMediaId, extra)
        })

        ivMenuAction!!.setOnClickListener({
            queueDialog = BottomSheetDialog(this@MediaActivity)
            val contentView = LayoutInflater.from(this@MediaActivity).inflate(R.layout.play_queue_layout, null)
            queueDialog!!.setContentView(contentView)
            queueRecycle = contentView.findViewById(R.id.control_queue_recycle) as RecyclerView
            println("plauQueue.size="+playQueue!!.size)
            queueAdapter = PlayQueueAdapter(playQueue)
            queueAdapter!!.setOnQueueItemClickListener { obj, _ ->
                if (obj is MediaBrowserCompat.MediaItem) {
                    //点击播放列表直接播放选中的media
                    val extra = Bundle()
                    extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
                    mediaController!!.transportControls.playFromMediaId(obj.mediaId, extra)
                }
                if (queueDialog!!.isShowing) {
                    queueDialog!!.cancel()
                    queueDialog!!.dismiss()
                }
            }
            queueRecycle!!.adapter = queueAdapter
            queueRecycle!!.layoutManager = LinearLayoutManager(this)
            queueDialog!!.show()
        })
    }

    override fun refreshQueue(mPathList: java.util.ArrayList<String>?) {

    }

    override fun onStart() {
        super.onStart()
        mMediaBrowser!!.connect()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("called onNewIntent")
    }

    /**
     * 连接状态回调
     */
    val mConnectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            println("connected service....")
            mMediaId = mMediaBrowser!!.root
            mMediaBrowser!!.subscribe(mMediaId!!, subscriptionCallBack)
            mediaController = MediaControllerCompat(this@MediaActivity, mMediaBrowser!!.sessionToken)
            mResultReceive = MediaResultReceive(Handler())
            mLooperHandler = LoopHandler(mediaController!!, mResultReceive!!)
            MediaControllerCompat.setMediaController(this@MediaActivity, mediaController)
            mediaController!!.registerCallback(mControllerCallBack)
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
    val subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback() {
        //service加载完成列表回调
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            println("onChildrenLoaded.....activity")
            if (queueAdapter != null) {
                queueAdapter!!.setData(children)
            }
            playQueue = children
            if (playQueue != null && playQueue!!.size > 0) {
                //如果当前在播放状态
                if (mediaController!!.playbackState.state != PlaybackStateCompat.STATE_NONE) {
                    val metadata = mediaController!!.metadata
                    //请求获取当前播放位置
                    if (metadata != null) {
                        currentMediaId = metadata.description.mediaId
                        val displayTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                        val subTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                        endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        mLooperHandler!!.sendEmptyMessage(0)
                        setMediaInfo(displayTitle, subTitle)
                    }
                    handlePlayState(mediaController!!.playbackState.state)
                } else {
                    setMediaInfo(playQueue!![0].description.title as String, playQueue!![0].description.subtitle as String)
                    currentMediaId = playQueue!![0].mediaId
                    val extra = Bundle()
                    extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_INIT)
                    mediaController!!.transportControls.playFromMediaId(currentMediaId, extra)
                }
            }
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
    val mControllerCallBack = object : MediaControllerCompat.Callback() {
        //当前歌曲信息变化回调
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("onMetadataChanged called")
            if (metadata != null) {
                currentMediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                val displayTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                val subTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                stepPosition = endPosition / 100L
                startPosition = 0
                println("endPosition=$endPosition,step=$stepPosition")
                setMediaInfo(displayTitle, subTitle)
            }
        }

        //播放列表变化回调
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            queue!!.forEachIndexed { _, queueItem ->
                println(queueItem.toString())
            }
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
                displayProgress!!.progress = (percent * 100f).toInt()
                handlePlayState(state.state)
            })
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            println("onSessionEvent.....")
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
            if(state == PlaybackStateCompat.STATE_STOPPED){
                displayProgress!!.progress = 0
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
        mLooperHandler = LoopHandler(mediaController!!, mResultReceive!!)
        mLooperHandler!!.sendEmptyMessage(0)
    }

    private fun stopLoop() {
        if (mLooperHandler != null) {
            mLooperHandler!!.removeCallbacksAndMessages(null)
            mLooperHandler = null
        }
    }

    /**
     * 更新媒体信息
     */
    fun setMediaInfo(displayTitle: String, subTitle: String) {
        tvDisPlayName!!.text = displayTitle
        tvSubName!!.text = subTitle
    }

    override fun onStop() {
        super.onStop()
        //释放控制器
        val mediaController = MediaControllerCompat.getMediaController(this@MediaActivity)
        if (mediaController != null) {
            mediaController.unregisterCallback(mControllerCallBack)
            MediaControllerCompat.setMediaController(this@MediaActivity, null)
        }
        //停止进度更新
        stopLoop()
        //取消播放状态监听
        mMediaBrowser!!.unsubscribe(mMediaId!!, subscriptionCallBack)
        //断开与媒体服务的链接
        mMediaBrowser!!.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("MediaActivity is destroy")
    }
}