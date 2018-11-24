package com.zy.ppmusic.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.zy.ppmusic.service.MediaService
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author ZhiTouPC
 */
class PlayBack(mMediaService: MediaService) : AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {
    @Volatile
    private var mMediaPlayer: MediaPlayer? = null
    /**
     * 当前播放队列
     */
    @Volatile
    private var mPlayQueue: List<String>? = null
    @Volatile
    private var mCurrentPosition: Int = 0
    var currentMediaId: String? = null
        private set
    private var mCallBack: CallBack? = null
    private val audioManager: AudioManager
    @Volatile
    private var mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK
    @Volatile
    private var mIsAutoStart = false

    /**
     * 是否是用户点击的暂停
     */
    private var mIsUserPause = false
    /**
     * 是否是因为焦点占用被暂停了
     */
    private var mIsPauseCauseAudio = false

    private var mPlayOnFocusGain: Boolean = false
    @Volatile
    var state = PlaybackStateCompat.STATE_NONE
        private set
    private val mContextWeak: WeakReference<MediaService> = WeakReference(mMediaService)

    @Volatile
    var currentStreamPosition: Int  = 0
            get() = mMediaPlayer?.currentPosition ?: mCurrentPosition

    init {
        val context = mMediaService.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun setCallBack(callBack: CallBack) {
        this.mCallBack = callBack
    }

    fun setPlayQueue(queue: List<String>) {
        this.mPlayQueue = queue
    }

    fun setIsUserPause(flag: Boolean) {
        this.mIsUserPause = flag
    }

    fun playMediaIdAutoStart(mediaId: String) {
        this.mIsAutoStart = true
        preparedWithMediaId(mediaId)
    }

    fun preparedWithMediaId(mediaId: String) {
        mPlayOnFocusGain = true
        getAudioFocus()
        val isChanged = !TextUtils.equals(mediaId, currentMediaId)
        if (isChanged) {
            mCurrentPosition = 0
            currentMediaId = mediaId
        }
        if (state == PlaybackStateCompat.STATE_PAUSED && !isChanged && mMediaPlayer != null) {
            configMediaPlayerState()
        } else {
            state = PlaybackStateCompat.STATE_STOPPED
            releasePlayer(true)
            val index = DataProvider.get().getMediaIndex(mediaId)
            PrintLog.d("该歌曲的id====$mediaId")
            PrintLog.d("该歌曲的位置：：：$index")
            createPlayerIfNeed()
            state = PlaybackStateCompat.STATE_BUFFERING
            val path = DataProvider.get().getPath(index)
            if (path.isNullOrEmpty() || !path.isFileExits()) {
                PrintLog.e("preparedWithMediaId error: path is empty or file not exits")
            } else {
                PrintLog.e("preparedWithMediaId: path=$path")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val builder = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .setFlags(AudioManager.FLAG_ALLOW_RINGER_MODES)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                    mMediaPlayer?.setAudioAttributes(builder.build())
                } else {
                    @Suppress("DEPRECATION")
                    mMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                mContextWeak.get()?.applicationContext?.apply {
                    mMediaPlayer?.setDataSource(this,
                            Uri.parse(String.format(Locale.CHINA, "file://%s", path)))
                }
                mMediaPlayer?.prepare()
                mCallBack?.onPlayBackStateChange(state)
                PrintLog.e("onPlay: music init complete index=" + index + " path=" + DataProvider.get().getPath(index))
            }
        }
    }

    private fun configMediaPlayerState() {
        PrintLog.i("configMediaPlayerState: started,$mAudioFocus")
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            //如果没有获取到硬件的播放权限则暂停播放
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                pause()
                mIsPauseCauseAudio = true
            }
        } else {
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer?.setVolume(0.1f, 0.1f)
            } else {
                mMediaPlayer?.setVolume(1.0f, 1.0f)
            }
            //当失去音频的焦点时，如果在播放状态要恢复播放
            if (mPlayOnFocusGain && !mIsUserPause && mIsPauseCauseAudio) {
                if (mMediaPlayer != null && !mMediaPlayer!!.isPlaying) {
                    state = if (mCurrentPosition == mMediaPlayer?.currentPosition) {
                        mMediaPlayer?.start()
                        PlaybackStateCompat.STATE_PLAYING
                    } else {
                        mMediaPlayer?.seekTo(mCurrentPosition)
                        PlaybackStateCompat.STATE_BUFFERING
                    }
                    mIsPauseCauseAudio = false
                }
                mPlayOnFocusGain = false
            }
        }
        mCallBack?.onPlayBackStateChange(state)
    }

    fun pause() {
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                mMediaPlayer?.pause()
                mCurrentPosition = mMediaPlayer!!.currentPosition
            }
            releasePlayer(false)
        }
        state = PlaybackStateCompat.STATE_PAUSED
        mCallBack?.onPlayBackStateChange(state)
    }

    private fun getAudioFocus() {
        val result: Int = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val builder = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            builder.setOnAudioFocusChangeListener(this)
            //设置音频焦点被占用自动暂停
            builder.setWillPauseWhenDucked(true)
            builder.setAcceptsDelayedFocusGain(true)
            audioManager.requestAudioFocus(builder.build())
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        mAudioFocus = if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) AUDIO_FOCUSED else AUDIO_NO_FOCUS_NO_DUCK
    }

    fun play() {
        if (mMediaPlayer == null) {
            return
        }
        if (mAudioFocus != AUDIO_FOCUSED) {
            getAudioFocus()
        }
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            if (mMediaPlayer!!.isPlaying) {
                mMediaPlayer?.pause()
                mCurrentPosition = mMediaPlayer!!.currentPosition
            }
            state = PlaybackStateCompat.STATE_PAUSED
        } else {
            if (state == PlaybackStateCompat.STATE_BUFFERING ||
                    state == PlaybackStateCompat.STATE_PAUSED ||
                    state == PlaybackStateCompat.STATE_CONNECTING) {
                mMediaPlayer?.start()
                setIsUserPause(false)
            }
            state = PlaybackStateCompat.STATE_PLAYING
        }
        mCallBack?.onPlayBackStateChange(state)
    }

    fun seekTo(position: Int, isAutoStart: Boolean) {
        if (mMediaPlayer == null) {
            return
        }
        if (mAudioFocus != AUDIO_FOCUSED) {
            getAudioFocus()
        }
        if (mMediaPlayer!!.isPlaying) {
            mMediaPlayer?.pause()
        }
        PrintLog.print("seek to $position,$isAutoStart")
        mIsAutoStart = isAutoStart
        mMediaPlayer?.seekTo(position)
        state = PlaybackStateCompat.STATE_BUFFERING

        mCallBack?.onPlayBackStateChange(state)
    }

    private fun releasePlayer(releasePlayer: Boolean) {
        if (releasePlayer) {
            mMediaPlayer?.stop()
            mMediaPlayer?.reset()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    fun onSkipToNext(): String? {
        checkPlayQueue()
        if (mPlayQueue!!.isEmpty()) {
            return null
        }
        var queueIndex = mPlayQueue!!.indexOf(currentMediaId)
        if (queueIndex == mPlayQueue!!.size - 1) {
            queueIndex = -1
        }
        val nextMediaId = mPlayQueue!![++queueIndex]
        PrintLog.d("onSkipToNext() called...$queueIndex")
        return nextMediaId
    }

    private fun checkPlayQueue() {
        if (mPlayQueue == null) {
            mPlayQueue = DataProvider.get().pathList
        }
    }

    fun onSkipToPrevious(): String? {
        if (mPlayQueue!!.isEmpty()) {
            return null
        }
        var queueIndex = mPlayQueue?.indexOf(currentMediaId)?:0
        if (queueIndex == 0) {
            queueIndex = mPlayQueue!!.size
        }
        val preMediaId = mPlayQueue!![--queueIndex]
        PrintLog.d("onSkipToPrevious() called...$queueIndex,$preMediaId")
        return preMediaId
    }

    fun stopPlayer() {
        state = PlaybackStateCompat.STATE_STOPPED
        if (mMediaPlayer != null) {
            mMediaPlayer?.reset()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
        mCallBack?.onPlayBackStateChange(state)
    }

    /**
     * 创建播放器或者重置
     */
    private fun createPlayerIfNeed() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setWakeMode(mContextWeak.get()?.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            mMediaPlayer?.setOnCompletionListener(this)
            mMediaPlayer?.setOnPreparedListener(this)
            mMediaPlayer?.setOnErrorListener(this)
            mMediaPlayer?.setOnSeekCompleteListener(this)
        }
    }

    /**
     * OnAudioFocusChangeListener
     *
     * @param focusChange 音频端口焦点状态
     */
    override fun onAudioFocusChange(focusChange: Int) {
        PrintLog.d("onAudioFocusChange() called with: focusChange = [$focusChange]")
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            mAudioFocus = AUDIO_FOCUSED
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            val canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            mAudioFocus = if (canDuck) AUDIO_NO_FOCUS_CAN_DUCK else AUDIO_NO_FOCUS_NO_DUCK
            if (state == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                mPlayOnFocusGain = true
            }
        } else {
            PrintLog.i("onAudioFocusChange: $focusChange")
        }
        configMediaPlayerState()
    }

    /**
     * OnCompletionListener
     *
     * @param mp 是否完成播放
     */
    override fun onCompletion(mp: MediaPlayer) {
        mCallBack?.onCompletion()
    }

    /**
     * OnErrorListener
     * 播放器发生错误
     *
     * @param mp    播放器
     * @param what  错误代码
     * @param extra 错误信息
     * @return 是否处理
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        mCallBack?.onError(what, "errorExtra=$extra")
        return true
    }

    /**
     * OnPreparedListener
     * 准备完成，可以播放了
     *
     * @param mp 播放器
     */
    override fun onPrepared(mp: MediaPlayer) {
        PrintLog.i("onPrepared: called " + mMediaPlayer!!.currentPosition)
        //准备完成
        if (state == PlaybackStateCompat.STATE_BUFFERING) {
            state = PlaybackStateCompat.STATE_CONNECTING
        }

        if (mIsAutoStart) {
            mIsAutoStart = false
            mMediaPlayer?.start()
            state = PlaybackStateCompat.STATE_PLAYING
        }

        mCallBack?.onPlayBackStateChange(state)
    }

    /**
     * 快进到指定位置完成，可以播放了
     *
     * @param mp 播放器
     */
    override fun onSeekComplete(mp: MediaPlayer) {
        mCurrentPosition = mMediaPlayer?.currentPosition ?: 0
        PrintLog.i("onSeekComplete() called=$mCurrentPosition")

        if (state == PlaybackStateCompat.STATE_BUFFERING) {
            state = PlaybackStateCompat.STATE_PAUSED
        }

        if (mIsAutoStart) {
            mMediaPlayer?.start()
            state = PlaybackStateCompat.STATE_PLAYING
            mIsAutoStart = false
        }
        mCallBack?.onPlayBackStateChange(state)
    }

    interface CallBack {
        /**
         * 播放完成
         */
        fun onCompletion()

        /**
         * 播放器状态变化
         *
         * @param state 状态
         */
        fun onPlayBackStateChange(state: Int)

        /**
         * 发生错误
         *
         * @param errorCode 错误码
         * @param error     错误信息
         */
        fun onError(errorCode: Int, error: String)
    }

    companion object {
        /**
         * we don't have audio focus, and can't duck (play at a low volume)
         */
        private const val AUDIO_NO_FOCUS_NO_DUCK = 0
        /**
         * we don't have focus, but can duck (play at a low volume)
         */
        private const val AUDIO_NO_FOCUS_CAN_DUCK = 1
        /**
         * we have full audio focus
         */
        private const val AUDIO_FOCUSED = 2
    }
}
