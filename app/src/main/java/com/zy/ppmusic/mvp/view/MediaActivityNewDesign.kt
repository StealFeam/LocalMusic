package com.zy.ppmusic.mvp.view

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialog
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.MainMenuAdapter
import com.zy.ppmusic.adapter.MediaInfoAdapter
import com.zy.ppmusic.adapter.PlayQueueAdapter
import com.zy.ppmusic.entity.MainMenuEntity
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.presenter.MediaPresenterImpl
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.DataTransform
import com.zy.ppmusic.widget.BorderTextView
import com.zy.ppmusic.widget.EasyTintView
import com.zy.ppmusic.widget.TimBackGroundDrawable
import kotlinx.android.synthetic.main.activity_media_new_design.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

/**
 * 与MediaService两种通信方式：
 *      1.MediaController.transportControls.playFromMediaId(String, Bundle);//只发送消息（最好与播放器状态相关）
 *      2.SessionCompat.sendCommand(String,Bundle,ResultReceiver);//需要获取结果
 */
class MediaActivityNewDesign : AppCompatActivity(), IMediaActivityContract.IView {
    private var mMediaBrowser: MediaBrowserCompat? = null
    private var mMainMenuAdapter: MainMenuAdapter? = null
    private var mMediaId: String? = null
    private var mMediaController: MediaControllerCompat? = null//媒体控制器
    private var mPlayQueueList: MutableList<MediaSessionCompat.QueueItem>? = null//播放列表与service同步
    private var mBottomQueueAdapter: PlayQueueAdapter? = null//播放列表的适配器
    private var mCurrentMediaIdStr: String? = null//当前播放的媒体id
    private var endPosition: Long = 0//结束位置
    private var stepPosition: Long = 0//自增量
    private var startPosition: Long = 0//起始位置
    private var percent: Float = 0f//当前percent
    private var mLooperHandler: LoopHandler? = null//循环处理
    private var mResultReceive: MediaResultReceive? = null//媒体播放进度处理

    private var mPresenter: IMediaActivityContract.IPresenter? = null
    private var mLoadingDialog: AppCompatDialog? = null//加载中的dialog

    private var mBorderTextView: BorderTextView? = null
    private var mPageAdapter: MediaInfoAdapter? = null

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
            when (msg?.what) {
                0 -> { //开始循环
                    if (mMediaController.get() != null) {
                        this.removeCallbacksAndMessages(null)
                        this.removeMessages(0)
                        mMediaController.get()?.sendCommand(MediaService.COMMAND_POSITION, null, mReceiver.get())
                        if (mMediaController.get()?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
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
//                    control_display_time_tv.text = DateUtil.getInstance().getTime(position)
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
                    println("MediaResultReceive other result....$resultCode," + resultData.toString())
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_new_design)
        if (supportActionBar != null) {
            supportActionBar?.elevation = 0f
        }

        createEmptyData()

        val drawable = TimBackGroundDrawable()
        drawable.setDrawableColor(ContextCompat.getColor(this, R.color.colorTheme))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            media_title_tint.background = drawable
        } else {
            @Suppress("DEPRECATION")
            media_title_tint.setBackgroundDrawable(drawable)
        }

        mPresenter = MediaPresenterImpl(this)

        val dataList = ArrayList<MainMenuEntity>()
        dataList.add(MainMenuEntity(getString(R.string.scan_local_music), R.drawable.ic_search_music))
        dataList.add(MainMenuEntity(getString(R.string.bluetooth_manager), R.drawable.ic_bl_manager))
        dataList.add(MainMenuEntity(getString(R.string.time_to_close), R.drawable.ic_time_clock))
        mMainMenuAdapter = MainMenuAdapter(dataList)
        recycler_media_menu.adapter = mMainMenuAdapter
        recycler_media_menu.layoutManager = GridLayoutManager(this, 3)

        mMainMenuAdapter?.setListener { _, position ->
            //view,position
            when (position) {
                0 -> {//刷新播放列表
                    showMsg(getString(R.string.start_scanning_the_local_file))
                    mPresenter?.refreshQueue(applicationContext, true)
                }
                1 -> {//蓝牙管理
                    val intent = Intent(this@MediaActivityNewDesign, BlScanActivity::class.java)
                    startActivity(intent)
                }
                2 -> {//定时关闭

                }
            }
        }

    }

    private fun createEmptyData() {
        mPageAdapter = MediaInfoAdapter(supportFragmentManager)
        vp_media_play_info.adapter = mPageAdapter
        vp_media_play_info.setPageMarginDrawable(ColorDrawable(ContextCompat.getColor(applicationContext, R.color.colorAccent)))
        vp_media_play_info.addOnPageChangeListener(mPageChangeListener)
    }

    private val mPageChangeListener = object : ViewPager.OnPageChangeListener {
        var mSelectedPosition = 0

        override fun onPageScrollStateChanged(state: Int) {
            if(state == 0 && mSelectedPosition >= 0){
                val mediaId = DataTransform.getInstance().mediaIdList[mSelectedPosition]
                if(mCurrentMediaIdStr != null && mCurrentMediaIdStr!! == mediaId){
                    return
                }
                val extra = Bundle()
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
                mMediaController?.transportControls?.playFromMediaId(mediaId, extra)
                mSelectedPosition = -1
            }
        }

        override fun onPageScrolled(position: Int, positionOffset: Float,
                                    positionOffsetPixels: Int) {
        }

        override fun onPageSelected(position: Int) {
            mSelectedPosition = position
        }
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
            mMediaController?.sendCommand(MediaService.COMMAND_UPDATE_QUEUE, null, mResultReceive)
        } else {
            showMsg(getString(R.string.no_media_searched))
            mMediaController?.transportControls?.playFromMediaId("-1", null)
        }
        println("------------- refresh Queue .......refreshQueue.............")
        mPageAdapter?.setPathList(mPathList!!)

    }

    /**
     * 首次加载完成
     */
    override fun loadFinished() {
        println("------------- refresh Queue ......loadFinished..............")
        mPageAdapter?.setPathList(DataTransform.getInstance().pathList)

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
        mPresenter?.refreshQueue(this@MediaActivityNewDesign, false)
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
        println("showLoading。。。。。")
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
        println("hideLoading。。。。。")
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
            println("connected service....")
            mMediaId = mMediaBrowser?.root
            mMediaBrowser?.subscribe(mMediaId!!, subscriptionCallBack)
            mMediaController = MediaControllerCompat(this@MediaActivityNewDesign, mMediaBrowser!!.sessionToken)
            mResultReceive = MediaResultReceive(Handler())
            if (mLooperHandler == null) {
                mLooperHandler = LoopHandler(mMediaController!!, mResultReceive!!)
            }
            MediaControllerCompat.setMediaController(this@MediaActivityNewDesign, mMediaController)
            mMediaController?.registerCallback(mControllerCallBack)
            loadMode()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended....")
            mMediaBrowser?.unsubscribe(mMediaBrowser!!.root, subscriptionCallBack)
            val mediaController = MediaControllerCompat.getMediaController(this@MediaActivityNewDesign)
            if (mediaController != null) {
                mediaController.unregisterCallback(mControllerCallBack)
                MediaControllerCompat.setMediaController(this@MediaActivityNewDesign, null)
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed.....")
            mMediaBrowser?.connect()
        }
    }

    private fun loadMode() {
        if (mPresenter != null && mMediaController != null) {
            //设置循环模式
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
                if (mMediaController?.playbackState?.state != PlaybackStateCompat.STATE_NONE) {
                    val metadata = mMediaController?.metadata
                    //请求获取当前播放位置
                    if (metadata != null) {
                        mCurrentMediaIdStr = metadata.description.mediaId
                        endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        stepPosition = endPosition / 100L
                        startPosition = 0
                        mLooperHandler?.sendEmptyMessage(0)
                        //更新时间
                        vp_media_play_info.currentItem = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
                    }
                    handlePlayState(mMediaController!!.playbackState!!.state)
                } else {
                    mCurrentMediaIdStr = children[0].description.mediaId
                    vp_media_play_info.currentItem = 0
                    val extra = Bundle()
                    extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_INIT)
                    mMediaController?.transportControls?.playFromMediaId(mCurrentMediaIdStr, extra)
                }
            } else {
                //初始化时间
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
                endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                stepPosition = endPosition / 100L
                startPosition = 0
                println("endPosition=$endPosition,step=$stepPosition")
                if (mCurrentMediaIdStr != null) {
                    val indexOfQueue = DataTransform.getInstance().getMediaIndex(mCurrentMediaIdStr)
                    println("index。。is $indexOfQueue")
                    vp_media_play_info.setCurrentItem(indexOfQueue,true)
                    if(mBottomQueueAdapter != null){
                        mBottomQueueAdapter?.selectIndex = indexOfQueue
                        mBottomQueueAdapter?.notifyDataSetChanged()
                    }
                }
                //更新时间
            }
        }

        //播放列表变化回调
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            if (queue?.size == 0) {
                //更新时间
                return
            }
            if (mBottomQueueAdapter != null) {
                mBottomQueueAdapter?.setData(queue)
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
                println("onPlaybackStateChanged....." + state?.state)
                println("position=" + state?.position + ",buffer=" + state?.bufferedPosition)
                startPosition = state!!.position
                percent = ((startPosition * 1.0f) / endPosition * 1.0f)
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
                        mBorderTextView = BorderTextView(this@MediaActivityNewDesign)
                    }
                    val h = mis / (60L * 60L * 1000L)
                    val m = (mis / (60L * 1000L)) - h * 60L
                    val s = (mis % (60L * 1000L)) / 1000L
                    val formatStr: String
                    if (h > 0) {
                        formatStr = getString(R.string.format_string_time_count_down_with_hour)
                        mBorderTextView?.show(media_title_tint, String.format(Locale.CHINA, formatStr, h, m, s))
                    } else {
                        formatStr = getString(R.string.format_string_time_count_down_no_hour)
                        mBorderTextView?.show(media_title_tint, String.format(Locale.CHINA, formatStr, m, s))
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
                    println("onSessionEvent....." + event + "," + extras.toString())
                }
            }
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
            //更新播放
        } else {
            startLoop()
            //更新暂停
        }
    }

    private fun startLoop() {
        if (mLooperHandler != null) {
            mLooperHandler?.removeCallbacksAndMessages(null)
            mLooperHandler = null
        }
        if (mMediaController != null && mResultReceive != null) {
            mLooperHandler = LoopHandler(mMediaController!!, mResultReceive!!)
            mLooperHandler?.sendEmptyMessage(0)
        }
    }

    private fun stopLoop() {
        if (mLooperHandler != null) {
            mLooperHandler?.removeCallbacksAndMessages(null)
            mLooperHandler = null
        }
    }

    fun showMsg(msg: String) {
        EasyTintView.makeText(media_title_tint, msg, EasyTintView.TINT_SHORT).show()
    }


    /**
     * 断开媒体服务
     */
    fun disConnectService() {
        //释放控制器
        val mediaController = MediaControllerCompat.getMediaController(this@MediaActivityNewDesign)
        if (mediaController != null) {
            mediaController.unregisterCallback(mControllerCallBack)
            MediaControllerCompat.setMediaController(this@MediaActivityNewDesign, null)
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
        println("onStop called")
        disConnectService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mPresenter != null) {
            mPresenter?.destroyView()
        }
        vp_media_play_info.removeOnPageChangeListener(mPageChangeListener)
        if (mLoadingDialog != null) {
            mLoadingDialog?.dismiss()
            mLoadingDialog?.cancel()
        }
        println("MediaActivity is destroy")
    }
}
