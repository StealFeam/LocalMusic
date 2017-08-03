package com.zy.ppmusic.ui

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.ResultReceiver
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.widget.PopupWindowCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.LoopModelAdapter
import com.zy.ppmusic.adapter.MainMenuAdapter
import com.zy.ppmusic.adapter.PlayQueueAdapter
import com.zy.ppmusic.bl.BlScanActivity
import com.zy.ppmusic.contract.IMediaActivityContract
import com.zy.ppmusic.entity.MainMenuEntity
import com.zy.ppmusic.presenter.MediaPresenterImpl
import com.zy.ppmusic.service.MediaService
import java.lang.ref.WeakReference

/**
 * 与MediaService两种通信方式：
 *      1.MediaController.transportControls.playFromMediaId(String, Bundle);//只发送消息（最好与播放器状态相关）
 *      2.SessionCompat.sendCommand(String,Bundle,ResultReceiver);//需要获取结果
 *
 */
class MediaActivity : AppCompatActivity(), IMediaActivityContract.IView {
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

    private var mMediaBrowser: MediaBrowserCompat? = null
    var mMediaId: String? = null
    var mMediaController: MediaControllerCompat? = null
    var mPlayQueueList: MutableList<MediaSessionCompat.QueueItem>? = null
    var ivNextAction: ImageView? = null
    var ivPreviousAction: ImageView? = null
    var ivPlayAction: ImageView? = null
    var ivModelAction: ImageView? = null
    var ivMenuAction: ImageView? = null
    var tvDisPlayName: TextView? = null
    var tvSubName: TextView? = null
    var mQueueRecycler: RecyclerView? = null
    var mBottomQueueDialog: BottomSheetDialog? = null
    var mBottomQueueAdapter: PlayQueueAdapter? = null
    var mCurrentMediaIdStr: String? = null
    var mProgressSeekBar: SeekBar? = null
    var endPosition: Long = 0//结束位置
    var stepPosition: Long = 0//自增量
    var startPosition: Long = 0//起始位置
    var percent: Float = 0f//当前percent
    var mLooperHandler: LoopHandler? = null//循环处理
    var mResultReceive: MediaResultReceive? = null//媒体播放进度处理
    var mIsTrackingBar: Boolean? = false//是否正在拖动进度条
    var mFunctionRecycler: RecyclerView? = null
    var mPresenter: IMediaActivityContract.IPresenter? = null
    var mLoadingDialog: AppCompatDialog? = null
    var mLoopModelRecycler: RecyclerView? = null
    var mLoopModelAdapter: LoopModelAdapter? = null
    var mLoopModelPopWindow: PopupWindow? = null
    var mLoopModelContentView: View? = null

    /*
     * 实现自循环1s后请求播放器播放的位置
     * controllerCompat 媒体控制器
     * receiver     接收消息回调
     *
     * kotlin静态内部类
     */
    class LoopHandler(controllerCompat: MediaControllerCompat, receiver: ResultReceiver) : Handler() {
        val mMediaController: WeakReference<MediaControllerCompat> = WeakReference<MediaControllerCompat>(controllerCompat)

        val mReceiver: WeakReference<ResultReceiver> = WeakReference<ResultReceiver>(receiver)

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg!!.what) {
                0 -> { //开始循环
                    if (mMediaController.get() != null) {
                        mMediaController.get()!!.sendCommand(MediaService.COMMAND_POSITION, null, mReceiver.get()!!)
                        this.sendEmptyMessageDelayed(0, 1000)
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
                    percent = ((startPosition * 1.0f) / endPosition * 1.0f)
                    if (mIsTrackingBar!!.not()) {
                        mProgressSeekBar!!.progress = (percent * 100f).toInt()
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

        ivNextAction = findViewById(R.id.control_action_next) as ImageView
        ivPlayAction = findViewById(R.id.control_action_play_pause) as ImageView
        ivMenuAction = findViewById(R.id.control_action_show_queue) as ImageView
        ivPreviousAction = findViewById(R.id.control_action_previous) as ImageView
        ivModelAction = findViewById(R.id.control_action_loop_model) as ImageView

        mFunctionRecycler = findViewById(R.id.more_function_recycle) as RecyclerView

        mPresenter = MediaPresenterImpl(this)

        val dataList = ArrayList<MainMenuEntity>()
        dataList.add(MainMenuEntity("扫描音乐", R.drawable.ic_search_music))
        dataList.add(MainMenuEntity("蓝牙管理", R.drawable.ic_bl_manager))
        val adapter = MainMenuAdapter(dataList)
        mFunctionRecycler!!.adapter = adapter
        mFunctionRecycler!!.layoutManager = GridLayoutManager(this, 2)

        adapter.setListener { _, position ->
            //view,position
            when (position) {
                0 -> {//刷新播放列表
                    showMsg("开始扫描本地文件")
                    mPresenter!!.refreshQueue(applicationContext)
                }
                1 -> {
                    val intent = Intent(this@MediaActivity, BlScanActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        mProgressSeekBar = findViewById(R.id.control_display_progress) as SeekBar
        mProgressSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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
                mLoopModelPopWindow = PopupWindow(this@MediaActivity)
                mLoopModelContentView = layoutInflater.inflate(R.layout.layout_loop_model, null)
                mLoopModelPopWindow!!.contentView = mLoopModelContentView
                mLoopModelPopWindow!!.width = ViewGroup.LayoutParams.WRAP_CONTENT
                mLoopModelPopWindow!!.height = ViewGroup.LayoutParams.WRAP_CONTENT
                mLoopModelPopWindow!!.isOutsideTouchable = true
                mLoopModelPopWindow!!.setBackgroundDrawable(ContextCompat.getDrawable(this@MediaActivity,R.drawable.layout_shape_round))
                mLoopModelRecycler = mLoopModelContentView!!.findViewById(R.id.pop_loop_model_recycler) as RecyclerView
                mLoopModelAdapter = LoopModelAdapter()
                mLoopModelAdapter!!.setListener { view, position ->
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
                    mLoopModelPopWindow!!.dismiss()
                }
                mLoopModelRecycler!!.adapter = mLoopModelAdapter
                mLoopModelRecycler!!.layoutManager = LinearLayoutManager(this@MediaActivity)
            }
            //提前计算contentView的宽和高
            mLoopModelPopWindow!!.contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            //分别计算横坐标和纵坐标的偏移
            val offsetY = -mLoopModelPopWindow!!.contentView.measuredHeight - ivModelAction!!.measuredHeight;
            val offsetX = -ivModelAction!!.measuredWidth / 2
            PopupWindowCompat.showAsDropDown(mLoopModelPopWindow!!, ivModelAction!!, offsetX, offsetY, Gravity.BOTTOM)
        })
        //播放按钮监听
        ivPlayAction!!.setOnClickListener({
            //初始化的时候点击的按钮直接播放当前的media
            val extra = Bundle()
            if (mMediaController!!.playbackState.state == PlaybackStateCompat.STATE_NONE) {
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PREPARED_WITH_ID)
            } else {
                extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID)
            }
            if (mCurrentMediaIdStr != null) {
                mMediaController!!.transportControls.playFromMediaId(mCurrentMediaIdStr, extra)
            } else {
                println("播放列表为空")
                mMediaController!!.transportControls.playFromMediaId("-1", extra)
            }
        })
        //播放列表监听
        ivMenuAction!!.setOnClickListener({
            mBottomQueueDialog = BottomSheetDialog(this@MediaActivity)
            val contentView = LayoutInflater.from(this@MediaActivity).inflate(R.layout.play_queue_layout, null)
            mBottomQueueDialog!!.setContentView(contentView)
            mQueueRecycler = contentView.findViewById(R.id.control_queue_recycle) as RecyclerView
            mBottomQueueAdapter = PlayQueueAdapter(mPlayQueueList)
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
                run {
                    val dialog = AlertDialog.Builder(this@MediaActivity)
                    dialog.setTitle("确认删除吗")
                    dialog.setMessage("此操作会同时删除本地文件")
                    dialog.setPositiveButton("删除") { _, which ->
                        mMediaController!!.removeQueueItemAt(position)
                        mPlayQueueList!!.removeAt(position)
                        mBottomQueueAdapter!!.setData(mPlayQueueList)
                        mBottomQueueAdapter!!.notifyItemRemoved(position)
                    }
                    dialog.setNegativeButton("取消") { dialog, which ->
                        dialog.cancel()
                        dialog.dismiss()
                    }
                    dialog.create().show()
                }
            }
            mQueueRecycler!!.adapter = mBottomQueueAdapter
            mQueueRecycler!!.layoutManager = LinearLayoutManager(this)
            mBottomQueueDialog!!.show()
        })
    }

    /**
     * IView
     * 刷新列表的回调
     */
    override fun refreshQueue(mPathList: java.util.ArrayList<String>?) {
        if (mPathList != null && mPathList.size > 0) {
            showMsg("扫描到" + mPathList.size + "首曲目")
            mMediaController!!.sendCommand(MediaService.COMMAND_UPDATE_QUEUE, null, mResultReceive)
        } else {
            showMsg("未扫描到曲目")
            mMediaController!!.transportControls.playFromMediaId("-1", null)
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceComponentName = ComponentName(this, MediaService::class.java)
        mMediaBrowser = MediaBrowserCompat(this, serviceComponentName, mConnectionCallBack, null)
        mMediaBrowser!!.connect()
    }

    override fun onRestart() {
        super.onRestart()
        if (mMediaBrowser != null) {
            mMediaBrowser!!.connect()
        }
        startLoop()
    }

    override fun onResume() {
        super.onResume()
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
    val subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback() {
        //service加载完成列表回调
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            println("onChildrenLoaded.....activity..."+children)
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
                        mLooperHandler!!.sendEmptyMessage(0)
                        setMediaInfo(displayTitle, subTitle)
                    }
                    handlePlayState(mMediaController!!.playbackState.state)
                } else {
                    setMediaInfo(children[0].mediaId as String, children[0].description.subtitle as String)
                    mCurrentMediaIdStr = children[0].description.mediaId
                    val extra = Bundle()
                    extra.putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_INIT)
                    mMediaController!!.transportControls.playFromMediaId(mCurrentMediaIdStr, extra)
                }
            }else{
                setMediaInfo(getString(R.string.app_name),getString(R.string.app_name))
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
    val mControllerCallBack = object : MediaControllerCompat.Callback() {
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
                println("endPosition=$endPosition,step=$stepPosition")
                setMediaInfo(displayTitle, subTitle)
            }
        }

        //播放列表变化回调
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            if(queue!!.size == 0){
                setMediaInfo(getString(R.string.app_name),getString(R.string.app_name))
                return
            }
            if (mBottomQueueAdapter != null) {
                mBottomQueueAdapter!!.setData(queue)
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
                handlePlayState(state.state)
            })
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            println("onSessionEvent....." + event + "," + extras.toString())
            when (event) {
                MediaService.ERROR_PLAY_QUEUE_EVENT -> {
                    showMsg("播放列表为空，未发现曲目")
                }
                MediaService.LOADING_QUEUE_EVENT -> {
                    showMsg("加载列表中...")
                    showLoading()
                }
                MediaService.LOAD_COMPLETE_EVENT -> {
                    showMsg("加载完成....")
                    hideLoading()
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
        mLooperHandler = LoopHandler(mMediaController!!, mResultReceive!!)
        mLooperHandler!!.sendEmptyMessage(0)
    }

    private fun stopLoop() {
        if (mLooperHandler != null) {
            mLooperHandler!!.removeCallbacksAndMessages(null)
            mLooperHandler = null
        }
    }

    fun showMsg(msg: String) {
        Snackbar.make(ivModelAction!!,msg,Snackbar.LENGTH_SHORT).show()
    }

    /**
     * 更新媒体信息
     */
    fun setMediaInfo(displayTitle: String, subTitle: String) {
        tvDisPlayName!!.text = displayTitle
        tvSubName!!.text = subTitle
    }

    override fun onPause() {
        super.onPause()
        println("onPause called")
    }

    override fun onStop() {
        super.onStop()
        println("onStop called")
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
        mMediaBrowser = null
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