package com.zy.ppmusic.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import com.zy.ppmusic.App
import com.zy.ppmusic.R
import com.zy.ppmusic.callback.AudioNoisyCallBack
import com.zy.ppmusic.callback.TimeTikCallBack
import com.zy.ppmusic.entity.MusicDbEntity
import com.zy.ppmusic.mvp.view.MediaActivity
import com.zy.ppmusic.receiver.AudioBecomingNoisyReceiver
import com.zy.ppmusic.receiver.LoopReceiver
import com.zy.ppmusic.utils.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author ZhiTouPC
 */
class MediaService : MediaBrowserServiceCompat() {
    /**
     * 保持后台运行且与前台进行通信
     */
    private var mMediaSessionCompat: MediaSessionCompat? = null
    /**
     * 播放器controller
     */
    private var mPlayBack: PlayBack? = null
    /**
     * 当前播放的媒体
     */
    @Volatile
    private var mCurrentMedia: MediaSessionCompat.QueueItem? = null
    /**
     * 音频监听
     */
    private var mAudioReceiver: AudioBecomingNoisyReceiver? = null
    /**
     * 倒计时
     */
    private var mCountDownTimer: TimerUtils? = null
    /**
     * 更新当前播放的媒体信息
     */
    private var mUpdateRunnable: UpdateRunnable? = null
    /**
     * 更新播放列表
     */
    private var mUpdateQueueRunnable: UpdateQueueRunnable? = null
    /**
     * 错误曲目数量
     * 当无法播放曲目数量和列表数量相同时销毁播放器避免循环
     */
    private var mErrorTimes: Int = 0

    private var receiver: LoopReceiver? = null

    private val mHandler = Handler(Looper.getMainLooper())

    private val filter by lazy {
        IntentFilter(LoopService.ACTION)
    }

    private val audioCallBack = object : AudioNoisyCallBack {
        override fun comingNoisy() {
            handlePlayOrPauseRequest()
        }
    }
    /**
     * 倒计时监听
     */
    private val timeTikCallBack = object : TimeTikCallBack {

        override fun onTik(mis: Long) {
            mMediaSessionCompat?.apply {
                //如果页面绑定时
                if (this.controller != null) {
                    if (mis != 0L) {
                        val bundle = Bundle()
                        bundle.putLong(ACTION_COUNT_DOWN_TIME, mis)
                        this.sendSessionEvent(ACTION_COUNT_DOWN_TIME, bundle)
                    } else {
                        this.sendSessionEvent(ACTION_COUNT_DOWN_END, null)
                        handleStopRequest(true)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        PrintLog.e("onCreate----------")
        if (!Constant.IS_STARTED && applicationContext != null) {
            PrintLog.e("启动Service。。。。。")
            stopSelf()
            try {
                startService(Intent(applicationContext, MediaService::class.java))
            } catch (e: Exception) {
                PrintLog.e("启动失败")
            }
            Constant.IS_STARTED = true
        }
        if (mMediaSessionCompat == null) {
            mMediaSessionCompat = MediaSessionCompat(this, TAG)
        }
        sessionToken = mMediaSessionCompat!!.sessionToken
        val mPlayBackStateBuilder = PlaybackStateCompat.Builder()
        mPlayBackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        mMediaSessionCompat?.setPlaybackState(mPlayBackStateBuilder.build())
        mMediaSessionCompat?.setCallback(MediaSessionCallBack())
        mMediaSessionCompat?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

        mMediaSessionCompat?.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
        mMediaSessionCompat?.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)

        val mediaIntent = Intent(applicationContext, MediaActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 1, mediaIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        mMediaSessionCompat?.setMediaButtonReceiver(pendingIntent)
        mMediaSessionCompat?.setSessionActivity(pendingIntent)
        mAudioReceiver = AudioBecomingNoisyReceiver(this)

    }

    private fun initPlayBack() {
        if (mPlayBack != null) {
            return
        }
        mPlayBack = PlayBack(this)
        mPlayBack?.setCallBack(object : PlayBack.CallBack {
            override fun onCompletion() {
                if (mErrorTimes != 0) {
                    mErrorTimes = 0
                }
                handleStopRequest(false)
            }

            override fun onPlayBackStateChange(state: Int) {
                if (isMainLooper()) {
                    TaskPool.executeSyc(Runnable {
                        onPlayStateChange(state)
                    })
                    return
                }
                onPlayStateChange(state)
            }

            private fun isMainLooper(): Boolean {
                return Looper.myLooper() == Looper.getMainLooper()
            }

            override fun onError(errorCode: Int, error: String) {
                mErrorTimes++
                if (mErrorTimes < DataTransform.get().mediaIdList.size) {
                    onMediaChange(mPlayBack!!.onSkipToNext(), true)
                } else {
                    toast("无可播放媒体，请重新扫描")
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = ["
                + flags + "], startId = [" + startId + "]")
        initPlayBack()
        //也可以在这接收通知按钮的event事件
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent)
        return Service.START_STICKY
    }

    override fun onGetRoot(clientPackageName: String, clientUId: Int, bundle: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
        Log.d(TAG, "onGetRoot() called with: clientPackageName = [" + clientPackageName +
                "], clientUId = [" + clientUId + "], bundle = [" + bundle + "]")
        return if (clientPackageName == packageName) {
            MediaBrowserServiceCompat.BrowserRoot(MY_ROOT_ID, bundle)
        } else {
            MediaBrowserServiceCompat.BrowserRoot(EMPTY_ROOT_ID, bundle)
        }
    }

    override fun onLoadChildren(s: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
        Log.d(TAG, "service onLoadChildren() called with: s = [$s], result = [$result]")
        if (s == MY_ROOT_ID) {
            result.detach()
            if (DataTransform.get().mediaItemList.isNotEmpty()) {
                result.sendResult(DataTransform.get().mediaItemList)
                PrintLog.print("load list size ... ${DataTransform.get().mediaItemList.size}")
            } else {
                TaskPool.executeSyc(Runnable {
                    val localCacheMediaLoader = LocalCacheMediaLoader()
                    if (localCacheMediaLoader.mediaPaths.isNotEmpty()) {
                        PrintLog.e("加载本地数据了")
                        result.sendResult(DataTransform.get().mediaItemList)
                    } else {
                        ScanMusicFile.get().setOnScanComplete(object : ScanMusicFile.AbstractOnScanComplete() {
                            override fun onComplete(paths: ArrayList<String>) {
                                PrintLog.e("扫描本地数据了")
                                DataTransform.get().transFormData(applicationContext, paths)
                                result.sendResult(DataTransform.get().mediaItemList)
                            }
                        }).scanMusicFile(applicationContext)
                    }
                })
            }
            if (mUpdateQueueRunnable == null) {
                mUpdateQueueRunnable = UpdateQueueRunnable(this)
            }
            TaskPool.executeSyc(mUpdateQueueRunnable!!)
        } else {
            result.sendResult(null)
        }
    }

    /**
     * 停止播放
     *
     * @param isNeedEnd 是否需要停止播放
     */
    private fun handleStopRequest(isNeedEnd: Boolean) {
        Log.d(TAG, "handleStopRequest() called with: isNeedEnd = [$isNeedEnd]")
        if (!isNeedEnd) {
            changeMediaByMode(true, true)
        } else {
            if (mPlayBack!!.state == PlaybackStateCompat.STATE_PLAYING) {
                handlePlayOrPauseRequest()
            }
            savePlayingRecord()
            NotificationUtils.cancelAllNotify(applicationContext)
            mMediaSessionCompat?.isActive = false
            mMediaSessionCompat?.release()
            mAudioReceiver?.unregister()
            stopForeground(true)
            stopSelf()
            android.os.Process.killProcess(Process.myPid())
            System.exit(-1)
        }
    }

    /**
     * 通过列表模式决定下一个播放的媒体
     *
     * @param isNext     true下一首曲目，false上一首曲目
     * @param isComplete 调用是否来自歌曲播放完成
     */
    private fun changeMediaByMode(isNext: Boolean, isComplete: Boolean) {
        if (mPlayBack == null) {
            PrintLog.e("changeMediaByMode: playback is null")
            PrintLog.d("尝试启动界面")
            Intent(applicationContext, MediaActivity::class.java).apply {
                this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
            }
            Constant.IS_STARTED = false
            return
        }
        Log.e(TAG, "changeMediaByMode: $isNext---$isComplete")
        //判断重复模式，单曲重复，随机播放，列表播放
        when (mMediaSessionCompat!!.controller.repeatMode) {
            //暂改为列表循环
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                onMediaChange(mPlayBack!!.onSkipToNext(), true)
            }
            //单曲重复
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                onMediaChange(mCurrentMedia!!.description.mediaId, true)
            }
            //列表播放
            PlaybackStateCompat.REPEAT_MODE_NONE -> if (isNext) {
                val position = DataTransform.get().getQueueItemList().indexOf(mCurrentMedia)
                //如果不是当前歌曲播放完成自动调用的话，就直接播放下一首
                if (!isComplete || position < DataTransform.get().getQueueItemList().size - 1) {
                    onMediaChange(mPlayBack!!.onSkipToNext(), true)
                } else {
                    onMediaChange(DataTransform.get().mediaIdList[DataTransform.get().mediaIdList.size - 1],
                            false)
                    Log.e(TAG, "handleStopRequest: 已播放到最后一首曲目")
                }
            } else {
                onMediaChange(mPlayBack!!.onSkipToPrevious(), true)
            }
            else -> {
                PrintLog.e("未知的重复模式")
            }
        }
    }

    /**
     * 保存播放记录到本地
     * 1、在关闭时调用需要在主线程上及时保存，不然很可能会被杀死导致保存失败
     * 2、扫描完成后的更新列表时需要保存当前的播放进度，这时需要在子线程中更新
     */
    private fun savePlayingRecord() {
        PrintLog.d("开始保存记录-----")
        App.getInstance().databaseManager.apply {
            deleteAll()
            insetEntity(buildCacheEntity())
        }
    }

    private fun buildCacheEntity(): MusicDbEntity {
        return MusicDbEntity().apply {
            mCurrentMedia?.description?.also {
                lastMediaId = it.mediaId
                lastMediaPath = it.mediaUri?.path
                lastPlayAuthor = it.subtitle.toString()
                lastPlayName = it.title.toString()
            }
            lastPlayedPosition = mPlayBack?.currentStreamPosition?.toLong() ?: 0
            lastPlayIndex = DataTransform.get().mediaIdList.indexOf(this.lastMediaId)
        }
    }

    /**
     * 当播放状态发生改变时
     */
    private fun onPlayStateChange(state: Int) {
        initPlayBack()
        PrintLog.d("onPlayStateChange() called with: " + mPlayBack!!.state)
        val position = mPlayBack?.currentStreamPosition?.toLong()
                ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
        var playbackActions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackActions = playbackActions or PlaybackStateCompat.ACTION_PAUSE
            mAudioReceiver?.register(audioCallBack)
        } else {
            mAudioReceiver?.unregister()
        }

        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(playbackActions)

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())
        if (mCurrentMedia != null) {
            stateBuilder.setActiveQueueItemId(mCurrentMedia!!.queueId)
        }

        mMediaSessionCompat?.setPlaybackState(stateBuilder.build())

        postNotifyByStyle(state)
    }

    private fun removeQueueItemByDes(des: MediaDescriptionCompat?) {
        val index = getIndexByDes(des)
        removeQueueItemAt(index)
    }

    private fun getIndexByDes(des: MediaDescriptionCompat?): Int {
        for (i in DataTransform.get().getQueueItemList().indices) {
            val queueItem = DataTransform.get().getQueueItemList()[i]
            if (des != null) {
                if (StringUtils.ifEquals(queueItem.description.mediaId, des.mediaId)) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * 移除列表中的item
     *
     * @param removeIndex 要移除item的位置
     */
    private fun removeQueueItemAt(removeIndex: Int) {
        if (removeIndex == -1) {
            Log.e(TAG, "removeQueueItemAt: the index is $removeIndex")
            return
        }
        Log.e(TAG, "removeQueueItemAt: $removeIndex")
        //如果删除的是当前播放的歌曲，则播放新的曲目
        if (DataTransform.get().getMediaIndex(mPlayBack?.currentMediaId) == removeIndex) {
            val state = mPlayBack?.state
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                mPlayBack?.pause()
            }
            DataTransform.get().removeItem(removeIndex)
            TaskPool.executeSyc(mUpdateQueueRunnable!!)
            if (DataTransform.get().mediaIdList.isNotEmpty()) {
                //删除的是前列表倒数第二个曲目的时候直接播放替代的曲目
                if (removeIndex <= DataTransform.get().mediaIdList.size - 1) {
                    onMediaChange(DataTransform.get().mediaIdList[removeIndex], state == PlaybackStateCompat.STATE_PLAYING)
                } else {//删除的是前列表最后一个曲目播放列表的第一个曲目
                    onMediaChange(DataTransform.get().mediaIdList[0], state == PlaybackStateCompat.STATE_PLAYING)
                }
            } else {
                mPlayBack?.stopPlayer()
            }
        } else {//如果不是当前曲目，不能影响当前播放,记录下播放进度，更新列表后继续播放
            DataTransform.get().removeItem(removeIndex)
            TaskPool.executeSyc(mUpdateQueueRunnable!!)
        }
    }

    /**
     * 处理播放或者暂停请求
     */
    fun handlePlayOrPauseRequest() {
        if (mCurrentMedia == null) {
            if (mMediaSessionCompat!!.sessionToken != null) {
                initPlayBack()
            } else {
                Intent(applicationContext, MediaActivity::class.java).apply {
                    this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(this)
                }
                Constant.IS_STARTED = false
                PrintLog.d("由后台启动界面")
            }
            return
        }
        if (mPlayBack == null) {
            PrintLog.e("播放器未初始化")
            return
        }
        mMediaSessionCompat?.isActive = true
        mPlayBack?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
        NotificationUtils.cancelAllNotify(this)
        App.getInstance().databaseManager.closeConn()
        mAudioReceiver?.unregister()
        mMediaSessionCompat?.release()
        if (mCountDownTimer != null) {
            mCountDownTimer?.stopTik()
            mCountDownTimer = null
        }
        if (mPlayBack != null) {
            mPlayBack?.stopPlayer()
            mPlayBack = null
        }
    }

    fun stopLoop() {
        if (receiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver!!)
        }
        stopService(Intent(this, LoopService::class.java))
    }

    private fun startLoop() {
        if (receiver == null) {
            receiver = LoopReceiver(this)
        }
        stopLoop()
        startService(Intent(this, LoopService::class.java))
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver!!, filter)
    }

    /**
     * 进度更新到界面
     */
    fun updatePositionToSession() {
        mMediaSessionCompat ?: return
        mMediaSessionCompat!!.controller ?: apply {
            stopLoop()
            return
        }
        TaskPool.executeSyc(Runnable {
            val bundle = Bundle()
            bundle.putInt(MediaService.UPDATE_POSITION_EVENT, mPlayBack!!.currentStreamPosition)
            mMediaSessionCompat?.sendSessionEvent(MediaService.UPDATE_POSITION_EVENT, bundle)
        })
    }

    /**
     * 播放曲目发生变化时
     *
     * @param mediaId                曲目id
     * @param shouldPlayWhenPrepared 是否需要准备完成后播放
     */
    fun onMediaChange(mediaId: String?, shouldPlayWhenPrepared: Boolean, shouldSeekToPosition: Long = 0) {
        if (DataTransform.get().pathList.isNotEmpty()) {
            PrintLog.d("mediaId-----$mediaId")
            if (mUpdateRunnable == null) {
                mUpdateRunnable = UpdateRunnable(this)
            }
            mUpdateRunnable?.setMediaId(mediaId)
            mUpdateRunnable?.setSeekToPosition(shouldSeekToPosition)
            mUpdateRunnable?.isPlayWhenPrepared(shouldPlayWhenPrepared)
            TaskPool.executeSyc(mUpdateRunnable!!)
        } else {
            mPlayBack?.stopPlayer()
        }
    }

    /**
     * 根据类型选择指定的通知样式
     */
    private fun postNotifyByStyle(state: Int) {
        val style = NotificationUtils.getNotifyStyle(applicationContext)
        val notification: Notification? = if (style == R.id.rb_choose_system) {
            NotificationUtils.createSystemNotify(this@MediaService,
                    mMediaSessionCompat, state == PlaybackStateCompat.STATE_PLAYING)
        } else {
            NotificationUtils.createCustomNotify(this@MediaService,
                    mMediaSessionCompat, state == PlaybackStateCompat.STATE_PLAYING)
        }
        notification?.also {
            startForeground(NotificationUtils.NOTIFY_ID, it)
        }
    }

    private class UpdateRunnable constructor(service: MediaService) : Runnable {
        private val mWeakService: WeakReference<MediaService> = WeakReference(service)
        private var isShouldPlay: Boolean = false
        private var seekToPosition: Long = 0
        private var mediaId: String? = null

        fun setMediaId(mediaId: String?) {
            this.mediaId = mediaId
        }

        fun isPlayWhenPrepared(flag: Boolean) {
            this.isShouldPlay = flag
        }

        fun setSeekToPosition(position: Long) {
            this.seekToPosition = position
        }

        override fun run() {
            val mediaService = mWeakService.get()
            if (mediaId.isNullOrEmpty()) {
                PrintLog.e("mediaId is $mediaId,service weak $mediaService")
                return
            }
            mediaService ?: return
            mediaService.mPlayBack?:mediaService.initPlayBack()
            //设置媒体信息
            val track = DataTransform.get().metadataCompatList[mediaId]
            //触发MediaControllerCompat.Callback->onMetadataChanged方法
            if (track != null) {
                mediaService.mMediaSessionCompat?.setMetadata(track)
            } else {
                println("----track is null----")
            }

            val index = DataTransform.get().getMediaIndex(mediaId!!)
            mediaService.mCurrentMedia = DataTransform.get().getQueueItemList()[index]
            if (seekToPosition > 0) {
                val extra = Bundle()
                extra.putLong(LOCAL_CACHE_POSITION_EVENT, seekToPosition)
                mediaService.mMediaSessionCompat?.sendSessionEvent(LOCAL_CACHE_POSITION_EVENT, extra)
                mediaService.mPlayBack?.preparedWithMediaId(mediaId!!)
                mediaService.mPlayBack?.seekTo(seekToPosition.toInt(), isShouldPlay)
            } else {
                if (isShouldPlay) {
                    PrintLog.e("准备自动播放id")
                    mediaService.mPlayBack?.playMediaIdAutoStart(mediaId!!)
                } else {
                    PrintLog.e("准备播放id")
                    mediaService.mPlayBack?.preparedWithMediaId(mediaId!!)
                }
            }
        }
    }

    /**
     * 更新播放列表
     */
    private class UpdateQueueRunnable constructor(mWeakService: MediaService) : Runnable {
        private val mWeakService: WeakReference<MediaService> by lazy {
            WeakReference(mWeakService)
        }

        override fun run() {
            Log.e(TAG, "updateQueue: size ... ${DataTransform.get().mediaIdList.size}")

            mWeakService.get()?.mPlayBack?.setPlayQueue(DataTransform.get().mediaIdList)
            mWeakService.get()?.mMediaSessionCompat?.setQueue(DataTransform.get().queueItemList)

            //覆盖本地缓存
            FileUtils.saveObject(DataTransform.get().musicInfoEntities!!,
                    Constant.CACHE_FILE_PATH)
        }

    }

    /**
     * 响应Activity的调用
     * getController.transportControls.
     */
    private inner class MediaSessionCallBack : MediaSessionCompat.Callback() {
        private var mLastHeadSetEventTime: Long = 0

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            Log.d(TAG, "onPlayFromMediaId() called with: mediaId = [$mediaId]")
            if (extras != null) {
                val action = extras.getString(ACTION_PARAM)
                Log.d(TAG, "onPlayFromMediaId: extra=$action")
                // 缓冲请求
                if (ACTION_PREPARED_WITH_ID == action) {
                    val noneMediaId = "-1"
                    if (noneMediaId == mediaId) {
                        if (DataTransform.get().getMediaIdList().isNotEmpty()) {
                            onMediaChange(DataTransform.get().getMediaIdList()[0], false)
                        } else {
                            mMediaSessionCompat?.sendSessionEvent(ERROR_PLAY_QUEUE_EVENT, null)
                        }
                    } else {
                        onMediaChange(mediaId, false)
                    }
                    // 播放指定id请求
                } else if (ACTION_PLAY_WITH_ID == action) {
                    // 如果和当前的mediaId相同则视为暂停或播放操作，不同则替换曲目
                    if (mCurrentMedia != null && mediaId != mCurrentMedia!!.description.mediaId) {
                        onMediaChange(mediaId, true)
                        PrintLog.d("播放指定曲目")
                        return
                    }
                    handlePlayOrPauseRequest()
                    //初始化播放器，如果本地有播放记录，取播放记录，没有就初始化穿过来的media
                } else if (ACTION_PLAY_INIT == action) {
                    TaskPool.executeSyc(Runnable {
                        val entityRecordList = App.getInstance().databaseManager.entity
                        if (entityRecordList.size > 0) {
                            val seekPosition = entityRecordList[0].lastPlayedPosition
                            mHandler.post {
                                onMediaChange(entityRecordList[0].lastMediaId, false, seekPosition)
                            }
                        } else {
                            mHandler.post {
                                onMediaChange(mediaId, false)
                            }
                        }
                    })
                } else if (ACTION_SEEK_TO == action) {
                    val seekPosition = extras.getInt(SEEK_TO_POSITION_PARAM)
                    mPlayBack?.seekTo(seekPosition, true)
                    Log.e(TAG, "onPlayFromMediaId: $seekPosition")
                } else {
                    PrintLog.i("unknown event")
                }
            } else {
                PrintLog.e("未知的播放id")
            }
        }

        override fun onPlay() {
            super.onPlay()
            Log.d(TAG, "onPlay() called")
            handlePlayOrPauseRequest()
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "onPause() called")
            mPlayBack?.pause()
            mPlayBack?.setIsUserPause(true)
        }

        override fun onStop() {
            super.onStop()
            handleStopRequest(true)
        }

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)
            val mediaId = DataTransform.get().mediaIdList[id.toInt()]
            PrintLog.e("准备跳转的id--------${DataTransform.get().getMetadataItem(mediaId)?.description?.title}")
            onMediaChange(mediaId, true)
        }

        override fun onSkipToNext() {
            Log.d(TAG, "onSkipToNext() called")
            changeMediaByMode(true, false)
        }

        override fun onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious() called")
            changeMediaByMode(false, false)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val keyEvent = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            when {
                keyEvent.action == KeyEvent.ACTION_DOWN -> {
                    Log.w(TAG, "onMediaButtonEvent: up=" + keyEvent.action +
                            ",code=" + keyEvent.keyCode)
                    when (keyEvent.keyCode) {
                        //点击下一首
                        KeyEvent.KEYCODE_MEDIA_NEXT -> changeMediaByMode(true, false)
                        //点击关闭
                        KeyEvent.KEYCODE_MEDIA_STOP -> handleStopRequest(true)
                        //点击上一首（目前没有）
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> changeMediaByMode(false, false)
                        //点击播放按钮
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            Log.e(TAG, "onMediaButtonEvent: 点击了播放按钮")
                            handlePlayOrPauseRequest()
                        }
                        else -> {
                        }
                    }
                }
                keyEvent.action == KeyEvent.ACTION_UP -> {
                    //如果有线耳机上的播放键快速点击两次且时间在300毫秒以内，则视为下一首
                    if (keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                        PrintLog.d("按钮抬起走了这里")
                        loge(keyEvent.toString())
                        mHandler.postDelayed(playRunnable, 300)
                        if (mLastHeadSetEventTime > 0 && (keyEvent.eventTime - mLastHeadSetEventTime) < 300) {
                            mHandler.removeCallbacks(playRunnable)
                            changeMediaByMode(true, false)
                        }
                        loge("last=$mLastHeadSetEventTime,current=${keyEvent.eventTime}")
                        mLastHeadSetEventTime = keyEvent.eventTime

                        //                    if (keyEvent.eventTime - mLastHeadSetEventTime > secondMis) {
                        //                        changeMediaByMode(true, false)
                        //                        PrintLog.d("之后下一首")
                        //                    } else {
                        //                        PrintLog.d("之后暂停或者播放")
                        //                        handlePlayOrPauseRequest()
                        //                    }
                    }
                    Log.w(TAG, "onMediaButtonEvent: action=" + keyEvent.action +
                            ",code=" + keyEvent.keyCode)
                }
                else -> loge(keyEvent.toString())
            }
            return true
        }

        override fun onCommand(command: String?, reqExtra: Bundle?, cb: ResultReceiver?) {
            when (command) {
                COMMAND_POSITION -> {
                    val resultExtra = Bundle()
                    resultExtra.putInt("position", mPlayBack?.currentStreamPosition ?: 0)
                    cb?.send(COMMAND_POSITION_CODE, resultExtra)
                }
                COMMAND_UPDATE_QUEUE -> {
                    TaskPool.executeSyc(Runnable {
                        savePlayingRecord()
                    })
                    TaskPool.executeSyc(mUpdateQueueRunnable!!)
                    TaskPool.executeSyc(Runnable {
                        val entity = App.getInstance().databaseManager.entity
                        if (entity.size > 0) {
                            val lastMediaId = entity[0].lastMediaId
                            mHandler.post {
                                if (!DataTransform.get().mediaIdList.contains(lastMediaId)) {
                                    onMediaChange(DataTransform.get().getMediaIdList()[0], false)
                                } else {
                                    onMediaChange(lastMediaId, false, entity[0].lastPlayedPosition)
                                }
                            }
                        } else {
                            mHandler.post {
                                onMediaChange(DataTransform.get().getMediaIdList()[0], false)
                            }
                        }
                        cb?.send(COMMAND_UPDATE_QUEUE_CODE, Bundle())
                    })
                }
                //开始循环获取当前播放位置
                COMMAND_START_LOOP -> startLoop()
                //结束获取当前播放位置
                COMMAND_STOP_LOOP -> stopLoop()
                COMMAND_CHANGE_NOTIFY_STYLE -> {
                    val style = reqExtra!!.getInt(Constant.CHOOSE_STYLE_EXTRA, 0)
                    NotificationUtils.setNotifyStyle(style)
                    postNotifyByStyle(mPlayBack?.state ?: PlaybackStateCompat.STATE_NONE)
                }
                else -> {
                    PrintLog.print("onCommand no match")
                    super.onCommand(command, reqExtra, cb)
                }
            }
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            mMediaSessionCompat?.setRepeatMode(repeatMode)
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
            super.onRemoveQueueItem(description)
            Log.d(TAG, "onRemoveQueueItem() called with: description = [$description]")
            removeQueueItemByDes(description)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
            when (action) {
                //开始倒计时
                ACTION_COUNT_DOWN_TIME -> {
                    if (mCountDownTimer != null) {
                        mCountDownTimer?.stopTik()
                        mCountDownTimer = null
                    }
                    mCountDownTimer = TimerUtils(extras!!.getLong(ACTION_COUNT_DOWN_TIME), 1000)
                    mCountDownTimer?.startTik(timeTikCallBack)
                }
                //结束倒计时
                ACTION_STOP_COUNT_DOWN -> if (mCountDownTimer != null) {
                    mCountDownTimer?.stopTik()
                    mCountDownTimer = null
                }
            }
        }
    }

    private val playRunnable by lazy {
        DoublePlayRunnable()
    }


    inner class DoublePlayRunnable : Runnable {
        override fun run() {
            handlePlayOrPauseRequest()
        }
    }

    companion object {
        /**
         * 播放指定id
         */
        const val ACTION_PLAY_WITH_ID = "PLAY_WITH_ID"
        /*-------------------play action--------------------------*/
        /**
         * 缓冲指定id
         */
        const val ACTION_PREPARED_WITH_ID = "PREPARED_WITH_ID"
        /**
         * 初始化播放器
         */
        const val ACTION_PLAY_INIT = "PLAY_INIT"
        /**
         * 快进
         */
        const val ACTION_SEEK_TO = "SEEK_TO"
        /**
         * 获取参数
         */
        const val ACTION_PARAM = "ACTION_PARAM"

        /*-------------------play action end--------------------------*/
        /**
         * 快进
         */
        const val SEEK_TO_POSITION_PARAM = "SEEK_TO_POSITION_PARAM"

        /*-------------------command action--------------------------*/
        /**
         * 开启循环
         */
        const val COMMAND_START_LOOP = "COMMAND_START_LOOP"

        /**
         * 关闭循环
         */
        const val COMMAND_STOP_LOOP = "COMMAND_STOP_LOOP"
        /**
         * 获取播放位置
         */
        const val COMMAND_POSITION = "COMMAND_POSITION"
        /**
         * 获取播放位置 resultCode
         */
        const val COMMAND_POSITION_CODE = 0x001
        /**
         * 更新播放列表
         */
        const val COMMAND_UPDATE_QUEUE = "COMMAND_UPDATE_QUEUE"
        /**
         * 更新播放列表resultCode
         */
        const val COMMAND_UPDATE_QUEUE_CODE = 0x002
        /**
         * 修改通知的样式
         */
        const val COMMAND_CHANGE_NOTIFY_STYLE = "COMMAND_CHANGE_NOTIFY_STYLE"
        /*-------------------command action end--------------------------*/
        /*-------------------custom action start--------------------------*/
        /**
         * 播放列表为空，本地未搜索到曲目
         */
        const val ERROR_PLAY_QUEUE_EVENT = "ERROR_PLAY_QUEUE_EVENT"
        /**
         * 加载中...
         */
        const val LOADING_QUEUE_EVENT = "LOADING_QUEUE_EVENT"
        /**
         * 加载完成...
         */
        const val LOAD_COMPLETE_EVENT = "LOAD_COMPLETE_EVENT"
        /**
         * 加载本地缓存位置...
         */
        const val LOCAL_CACHE_POSITION_EVENT = "LOCAL_CACHE_POSITION_EVENT"
        /**
         * 更新播放位置...
         */
        const val UPDATE_POSITION_EVENT = "UPDATE_POSITION_EVENT"

        const val RESET_SESSION_EVENT = "reset_session_event"

        /**
         * 开始倒计时
         */
        const val ACTION_COUNT_DOWN_TIME = "ACTION_COUNT_DOWN_TIME"
        /**
         * 倒计时结束
         */
        const val ACTION_COUNT_DOWN_END = "ACTION_COUNT_DOWN_END"
        /**
         * 停止倒计时
         */
        const val ACTION_STOP_COUNT_DOWN = "ACTION_STOP_COUNT_DOWN"

        /**
         * -------------------custom action end--------------------------
         */
        private const val TAG = "MediaService"


        private const val MY_ROOT_ID = "my_root_id"
        private const val EMPTY_ROOT_ID = "empty_root_id"
    }

}

