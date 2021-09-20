package com.zy.ppmusic.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.session.MediaButtonReceiver
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
import kotlinx.coroutines.*
import kotlin.system.exitProcess

/**
 * @author stealfeam
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
                onPlayStateChange(state)
            }

            override fun onError(errorCode: Int, error: String) {
                mErrorTimes++
                if (mErrorTimes < DataProvider.get().mediaIdList.size) {
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
        return Service.START_NOT_STICKY
    }

    override fun onGetRoot(clientPackageName: String, clientUId: Int, bundle: Bundle?): BrowserRoot? {
        Log.d(TAG, "onGetRoot() called with: clientPackageName = [" + clientPackageName +
                "], clientUId = [" + clientUId + "], bundle = [" + bundle + "]")
        return if (clientPackageName == packageName) {
            BrowserRoot(MY_ROOT_ID, bundle)
        } else {
            BrowserRoot(EMPTY_ROOT_ID, bundle)
        }
    }

    override fun onLoadChildren(s: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        Log.d(TAG, "service onLoadChildren() called with: s = [$s], result = [$result]")
        if (s == MY_ROOT_ID) {
            result.detach()
            if (DataProvider.get().mediaItemList.isNotEmpty()) {
                result.sendResult(DataProvider.get().mediaItemList)
                PrintLog.print("load list size ... ${DataProvider.get().mediaItemList.size}")
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    val job = async(Dispatchers.IO) {
                        DataProvider.get().loadData(false)
                        return@async DataProvider.get().mediaItemList
                    }
                    val list = job.await()
                    result.sendResult(list)
                }
            }
            updateQueue()
        } else {
            result.sendResult(null)
        }
    }

    /**
     * 是否想要停止播放
     * 1.当前曲目播放完成
     * 2.想要终止当前程序
     * @param isNeedEnd 是否需要停止播放
     */
    private fun handleStopRequest(isNeedEnd: Boolean) {
        Log.d(TAG, "handleStopRequest() called with: isNeedEnd = [$isNeedEnd]")
        if (!isNeedEnd) {
            changeMediaByMode(isNext = true, true)
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
            Process.killProcess(Process.myPid())
            exitProcess(-1)
        }
    }

    private var lastChangeMis = 0L

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
                this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(this)
            }
            Constant.IS_STARTED = false
            return
        }
        Log.e(TAG, "changeMediaByMode: $isNext---$isComplete")
        //判断重复模式，单曲重复，随机播放，列表播放
        when (mMediaSessionCompat?.controller?.repeatMode) {
            //暂改为列表循环
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                onMediaChange(mPlayBack?.onSkipToNext(), true)
            }
            //单曲重复
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                onMediaChange(mCurrentMedia?.description?.mediaId, true)
            }
            //列表播放
            PlaybackStateCompat.REPEAT_MODE_NONE -> if (isNext) {
                val position = DataProvider.get().getQueueItemList().indexOf(mCurrentMedia)
                //如果不是当前歌曲播放完成自动调用的话，就直接播放下一首
                if (!isComplete || position < DataProvider.get().getQueueItemList().size - 1) {
                    onMediaChange(mPlayBack!!.onSkipToNext(), true)
                } else {
                    onMediaChange(DataProvider.get().mediaIdList[DataProvider.get().mediaIdList.size - 1],
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
        App.instance?.databaseManager?.apply {
            deleteAll()
            insetEntity(buildCacheEntity())
        }
    }

    private fun buildCacheEntity(): MusicDbEntity? {
        mCurrentMedia?.let {
            return MusicDbEntity().apply {
                if (it.description != null) {
                    lastMediaId = it.description.mediaId
                    lastMediaPath = it.description.mediaUri?.path
                    lastPlayAuthor = it.description.subtitle.toString()
                    lastPlayName = it.description.title.toString()
                }
                lastPlayedPosition = mPlayBack?.currentStreamPosition?.toLong() ?: 0
                lastPlayIndex = DataProvider.get().mediaIdList.indexOf(this.lastMediaId)
            }
        }
        return null
    }

    /**
     * 当播放状态发生改变时
     */
    private fun onPlayStateChange(state: Int) {
        initPlayBack()
        PrintLog.d("onPlayStateChange() called with: " + mPlayBack!!.state)
        val position = mPlayBack?.currentStreamPosition?.toLong() ?: 0
        var playbackActions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackActions = playbackActions or PlaybackStateCompat.ACTION_PAUSE
            mAudioReceiver?.register(audioCallBack)
        } else {
            mAudioReceiver?.unregister()
        }

        val stateBuilder = PlaybackStateCompat.Builder().setActions(playbackActions)
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())
        if (mCurrentMedia != null) {
            stateBuilder.setActiveQueueItemId(mCurrentMedia!!.queueId)
        }

        GlobalScope.launch(Dispatchers.Main) {
            mMediaSessionCompat?.setPlaybackState(stateBuilder.build())
            postNotifyByStyle(state)
        }
    }

    private fun removeQueueItemByDes(des: MediaDescriptionCompat?) {
        val index = getIndexByDes(des)
        removeQueueItemAt(index)
    }

    private fun getIndexByDes(des: MediaDescriptionCompat?): Int {
        for (i in DataProvider.get().getQueueItemList().indices) {
            val queueItem = DataProvider.get().getQueueItemList()[i]
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
            updateQueue()
            return
        }
        Log.e(TAG, "removeQueueItemAt: $removeIndex")
        //如果删除的是当前播放的歌曲，则播放新的曲目
        if (DataProvider.get().getQueueItemList().indexOf(mCurrentMedia) == removeIndex) {
            val state = mPlayBack?.state
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                mPlayBack?.pause()
            }
            DataProvider.get().removeQueueAt(removeIndex)
            updateQueue()
            if (DataProvider.get().mediaIdList.isNotEmpty()) {
                //删除的是前列表倒数第二个曲目的时候直接播放替代的曲目
                if (removeIndex <= DataProvider.get().mediaIdList.size - 1) {
                    onMediaChange(DataProvider.get().mediaIdList[removeIndex], state == PlaybackStateCompat.STATE_PLAYING)
                } else {
                    //删除的是前列表最后一个曲目播放列表的第一个曲目
                    onMediaChange(DataProvider.get().mediaIdList[0], state == PlaybackStateCompat.STATE_PLAYING)
                }
            } else {
                mPlayBack?.stopPlayer()
            }
        } else {
            DataProvider.get().removeQueueAt(removeIndex)
            updateQueue()
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
                    this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
        App.instance?.databaseManager?.closeConn()
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
        GlobalScope.launch(Dispatchers.IO) {
            val bundle = Bundle()
            bundle.putInt(UPDATE_POSITION_EVENT, mPlayBack!!.currentStreamPosition)
            mMediaSessionCompat?.sendSessionEvent(UPDATE_POSITION_EVENT, bundle)
        }
    }

    /**
     * 播放曲目发生变化时
     *
     * @param mediaId                曲目id
     * @param shouldPlayWhenPrepared 是否需要准备完成后播放
     */
    fun onMediaChange(mediaId: String?, shouldPlayWhenPrepared: Boolean, shouldSeekToPosition: Long = 0) {
        if (DataProvider.get().pathList.isNotEmpty()) {
            if (System.currentTimeMillis() - lastChangeMis < 800) {
                return
            } else {
                lastChangeMis = System.currentTimeMillis()
            }
            PrintLog.d("mediaId-----$mediaId")
            GlobalScope.launch(Dispatchers.Default) {
                updateTask(mediaId, shouldPlayWhenPrepared, shouldSeekToPosition)
            }
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


    private fun updateTask(mediaId: String?, isShouldPlay: Boolean, seekToPosition: Long) {
        if (mediaId.isNullOrEmpty()) {
            PrintLog.e("mediaId is $mediaId")
            return
        }
        mPlayBack ?: initPlayBack()
        //设置媒体信息
        val track = DataProvider.get().metadataCompatList[mediaId]
        // 触发MediaControllerCompat.Callback->onMetadataChanged方法
        mMediaSessionCompat?.setMetadata(track)
        PrintLog.e("update track is $track")
        val index = DataProvider.get().getMediaIndex(mediaId)
        if (index < 0) return
        mCurrentMedia = DataProvider.get().getQueueItemList()[index]
        if (seekToPosition > 0) {
            val extra = Bundle()
            extra.putLong(LOCAL_CACHE_POSITION_EVENT, seekToPosition)
            mMediaSessionCompat?.sendSessionEvent(LOCAL_CACHE_POSITION_EVENT, extra)
        }
        mPlayBack?.preparedWithMediaId(mediaId,seekToPosition.toInt(), isShouldPlay)
    }

    private fun updateQueue() {
        mPlayBack?.setPlayQueue(DataProvider.get().mediaIdList)
        mMediaSessionCompat?.setQueue(DataProvider.get().queueItemList)
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
                        if (DataProvider.get().getMediaIdList().isNotEmpty()) {
                            onMediaChange(DataProvider.get().getMediaIdList()[0], false)
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
                    GlobalScope.launch(Dispatchers.Main) {
                        val job = async(Dispatchers.IO) {
                            return@async App.instance!!.databaseManager!!.entity
                        }
                        val cacheEntity = job.await()
                        if (cacheEntity != null) {
                            PrintLog.d("缓存的进度---${cacheEntity.lastPlayedPosition}")
                            onMediaChange(cacheEntity.lastMediaId, false, cacheEntity.lastPlayedPosition)
                        } else {
                            onMediaChange(mediaId, false)
                        }
                    }
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
            val mediaId = DataProvider.get().mediaIdList[id.toInt()]
            PrintLog.e("准备跳转的id--------${DataProvider.get().getMetadataItem(mediaId)?.description?.title}")
            onMediaChange(mediaId, true)
        }

        override fun onSkipToNext() {
            Log.d(TAG, "onSkipToNext() called")
            changeMediaByMode(isNext = true, false)
        }

        override fun onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious() called")
            changeMediaByMode(isNext = false, false)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val keyEvent = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return true
            when (keyEvent.action) {
                KeyEvent.ACTION_DOWN -> {
                    Log.w(TAG, "onMediaButtonEvent: up=" + keyEvent.action +
                            ",code=" + keyEvent.keyCode)
                    when (keyEvent.keyCode) {
                        //点击下一首
                        KeyEvent.KEYCODE_MEDIA_NEXT -> changeMediaByMode(isNext = true, false)
                        //点击关闭
                        KeyEvent.KEYCODE_MEDIA_STOP -> handleStopRequest(true)
                        //点击上一首（目前没有）
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> changeMediaByMode(isNext = false, false)
                        //点击播放按钮
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            Log.e(TAG, "onMediaButtonEvent: 点击了播放按钮")
                            handlePlayOrPauseRequest()
                        }
                    }
                }
                KeyEvent.ACTION_UP -> {
                    //如果有线耳机上的播放键快速点击两次且时间在300毫秒以内，则视为下一首
                    if (keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                        PrintLog.d("按钮抬起走了这里")
                        loge(keyEvent.toString())
                        mHandler.postDelayed(playRunnable, 300)
                        if (mLastHeadSetEventTime > 0 && (keyEvent.eventTime - mLastHeadSetEventTime) < 300) {
                            mHandler.removeCallbacks(playRunnable)
                            changeMediaByMode(isNext = true, false)
                        }
                        mLastHeadSetEventTime = keyEvent.eventTime
                    }
                    Log.w(TAG, "onMediaButtonEvent: action=" + keyEvent.action + ",code=" + keyEvent.keyCode)
                }
                else -> loge(keyEvent.toString())
            }
            return true
        }

        override fun onCommand(command: String?, reqExtra: Bundle?, cb: ResultReceiver?) {
            when (command) {
                COMMAND_POSITION -> {
                    val resultExtra = Bundle()
                    resultExtra.putInt(EXTRA_POSITION, mPlayBack?.currentStreamPosition ?: 0)
                    cb?.send(COMMAND_POSITION_CODE, resultExtra)
                }
                COMMAND_UPDATE_QUEUE -> {
                    //是否是前台扫描媒体操作
                    val isForce = reqExtra?.getBoolean(EXTRA_SCAN_COMPLETE) ?: false
                    GlobalScope.launch(Dispatchers.Main) {
                        val job = async(Dispatchers.Default) {
                            savePlayingRecord()
                            mPlayBack?.setPlayQueue(DataProvider.get().mediaIdList)
                            mMediaSessionCompat?.setQueue(DataProvider.get().queueItemList)
                            cb?.send(COMMAND_UPDATE_QUEUE_CODE, Bundle())
                            if (isForce) {
                                //读取本地数据库
                                return@async App.instance!!.databaseManager!!.entity
                            }
                            return@async MusicDbEntity()
                        }
                        if (isForce) {
                            val cacheEntity = job.await()
                            if (cacheEntity != null) {
                                val lastMediaId = cacheEntity.lastMediaId
                                if (!DataProvider.get().mediaIdList.contains(lastMediaId)) {
                                    onMediaChange(DataProvider.get().getMediaIdList()[0], false)
                                } else {
                                    onMediaChange(lastMediaId, false, cacheEntity.lastPlayedPosition)
                                }
                            } else {
                                onMediaChange(DataProvider.get().getMediaIdList()[0], false)
                            }
                        }
                    }

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
         * 发送当前播放进度
         */
        const val EXTRA_POSITION = "position"
        /**
         * 扫描完成后的更新列表
         */
        const val EXTRA_SCAN_COMPLETE = "scan_complete"

        /**
         * -------------------custom action end--------------------------
         */
        private const val TAG = "MediaService"


        private const val MY_ROOT_ID = "my_root_id"
        private const val EMPTY_ROOT_ID = "empty_root_id"
    }

}

