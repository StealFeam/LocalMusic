package com.zy.ppmusic.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.zy.ppmusic.service.MediaService;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * @author ZhiTouPC
 */
public class PlayBack implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {
    private static final String TAG = "PlayBack";

    /**
     * we don't have audio focus, and can't duck (play at a low volume)
     */
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    /**
     * we don't have focus, but can duck (play at a low volume)
     */
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    /**
     * we have full audio focus
     */
    private static final int AUDIO_FOCUSED = 2;

    private MediaPlayer mMediaPlayer;
    private final MediaService mMediaService;
    /**
     * 当前播放队列
     */
    private volatile List<String> mPlayQueue;
    private volatile int mCurrentPosition;
    private volatile String mCurrentMediaId;
    private int mCurrentIndex;
    private CallBack mCallBack;
    private AudioManager audioManager;
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;

    private boolean mIsAutoStart = false;
    /**
     * 是否是用户点击的暂停
     */
    private boolean mIsUserPause = false;
    /**
     * 是否是因为焦点占用被暂停了
     */
    private boolean mIsPauseCauseAudio = false;

    private boolean mPlayOnFocusGain;
    private int mState = PlaybackStateCompat.STATE_NONE;
    private Random mRandom;

    public interface CallBack {
        /**
         * 播放完成
         */
        void onCompletion();

        /**
         * 播放器状态变化
         * @param state 状态
         */
        void onPlayBackStateChange(int state);

        /**
         * 发生错误
         * @param errorCode 错误码
         * @param error 错误信息
         */
        void onError(int errorCode, String error);
    }

    public PlayBack(MediaService mMediaService) {
        this.mMediaService = mMediaService;
        Context context = mMediaService.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setCallBack(CallBack callBack) {
        this.mCallBack = callBack;
    }

    public void setPlayQueue(List<String> queue) {
        this.mPlayQueue = queue;
    }

    public void setIsUserPause(boolean flag) {
        this.mIsUserPause = flag;
    }

    public void preparedWithMediaId(String mediaId) {
        mPlayOnFocusGain = true;
        getAudioFocus();
        boolean isChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (isChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }
        if (mState == PlaybackStateCompat.STATE_PAUSED && !isChanged && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_STOPPED;
            releasePlayer(true);
            int index = DataTransform.getInstance().getMediaIndex(mediaId);
            createPlayerIfNeed();
            mState = PlaybackStateCompat.STATE_BUFFERING;
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                String path = DataTransform.getInstance().getPath(index);
                if (TextUtils.isEmpty(path)) {
                    Log.e(TAG, "preparedWithMediaId error: path is null");
                } else {
                    Log.e(TAG, "preparedWithMediaId: path=" + path);
                    mMediaPlayer.setDataSource(path);
                    mMediaPlayer.prepare();
                    if (mCallBack != null) {
                        mCallBack.onPlayBackStateChange(mState);
                    }
                    Log.e(TAG, "onPlay: music init complete index=" + index + " path=" + DataTransform.getInstance().getPath(index));
                }
            } catch (IOException e) {
                Log.e(TAG, "preparedWithMediaId error: " + e.getMessage());
                if (mCallBack != null) {
                    mCallBack.onError(0, e.getMessage());
                }
            }
        }
    }

    private void configMediaPlayerState() {
        Log.e(TAG, "configMediaPlayerState: started," + mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            //如果没有获取到硬件的播放权限则暂停播放
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                Log.e(TAG, "config:paused");
                pause();
                mIsPauseCauseAudio = true;
            }
        } else {
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(0.2f, 0.2f);
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                }
            }
            //当失去音频的焦点时，如果在播放状态要恢复播放
            if (mPlayOnFocusGain && !mIsUserPause && mIsPauseCauseAudio) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                    mIsPauseCauseAudio = false;
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }

    public void pause() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }
            releasePlayer(false);
        }
        mState = PlaybackStateCompat.STATE_PAUSED;
        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }

    private void getAudioFocus() {
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) ? AUDIO_FOCUSED : AUDIO_NO_FOCUS_NO_DUCK;
    }

    public int getState() {
        return mState;
    }

    public int getCurrentStreamPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    public boolean isPlaying() {
        return mState == PlaybackStateCompat.STATE_PLAYING ||
                (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    public void play() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mAudioFocus != AUDIO_FOCUSED) {
            getAudioFocus();
        }
        Log.w(TAG, "play: called");
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }
            mState = PlaybackStateCompat.STATE_PAUSED;
        } else {
            if (mState == PlaybackStateCompat.STATE_BUFFERING ||
                    mState == PlaybackStateCompat.STATE_PAUSED ||
                    mState == PlaybackStateCompat.STATE_CONNECTING) {
                mMediaPlayer.start();
                setIsUserPause(false);
            }
            mState = PlaybackStateCompat.STATE_PLAYING;
        }

        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }


    public void seekTo(int position, boolean isAutoStart) {
        if (mMediaPlayer == null) {
            return;
        }
        if (mAudioFocus != AUDIO_FOCUSED) {
            getAudioFocus();
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        System.out.println("seek to " + position + "," + isAutoStart);
        mIsAutoStart = isAutoStart;
        mMediaPlayer.seekTo(position);
        mState = PlaybackStateCompat.STATE_BUFFERING;

        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }

    private void releasePlayer(boolean releasePlayer) {
        if (mMediaPlayer != null && releasePlayer) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

    }

    public String getCurrentMediaId() {
        return mCurrentMediaId;
    }

    public int getCurrentIndex() {
        return mPlayQueue.indexOf(mCurrentMediaId);
    }

    public int getMediaIndex(String mediaId) {
        return mPlayQueue.indexOf(mediaId);
    }

    public String onSkipToNext() {
        checkPlayQueue();
        int queueIndex = mPlayQueue.indexOf(mCurrentMediaId);
        if (queueIndex == (mPlayQueue.size() - 1)) {
            queueIndex = -1;
        }
        String nextMediaId = mPlayQueue.get(++queueIndex);
        Log.d(TAG, "onSkipToNext() called..." + queueIndex);
        return nextMediaId;
    }

    public String randomIndex() {
        checkPlayQueue();
        if (mRandom == null) {
            mRandom = new Random();
        }
        mCurrentIndex = mPlayQueue.indexOf(mCurrentMediaId);
        int index;
        while (true) {
            index = mRandom.nextInt(mPlayQueue.size());
            if (index >= 0 && index < mPlayQueue.size() && index != mCurrentIndex) {
                break;
            }
        }
        Log.w(TAG, "randomIndex: " + index);
        return mPlayQueue.get(index);
    }

    private void checkPlayQueue() {
        if (mPlayQueue == null) {
            mPlayQueue = DataTransform.getInstance().getPathList();
        }
    }

    public String onSkipToPrevious() {
        int queueIndex = mPlayQueue.indexOf(mCurrentMediaId);
        if (queueIndex == 0) {
            queueIndex = mPlayQueue.size();
        }
        String preMediaId = mPlayQueue.get(--queueIndex);
        Log.d(TAG, "onSkipToPrevious() called..." + queueIndex + "," + preMediaId);
        return preMediaId;
    }

    public void stopPlayer() {
        mMediaService.stopForeground(false);
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }

    /**
     * 创建播放器或者重置
     */
    private void createPlayerIfNeed() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(mMediaService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        }
    }

    /**
     * OnAudioFocusChangeListener
     *
     * @param focusChange 音频端口焦点状态
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange() called with: focusChange = [" + focusChange + "]");
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            mAudioFocus = AUDIO_FOCUSED;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;
            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                mPlayOnFocusGain = true;
            }
        } else {
            Log.e(TAG, "onAudioFocusChange: " + focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * OnCompletionListener
     *
     * @param mp 是否完成播放
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mCallBack != null) {
            mCallBack.onCompletion();
        }
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
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mCallBack != null) {
            mCallBack.onError(what, "errorExtra=" + extra);
        }
        return true;
    }

    /**
     * OnPreparedListener
     * 准备完成，可以播放了
     *
     * @param mp 播放器
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.e(TAG, "onPrepared: called " + mMediaPlayer.getCurrentPosition());
        //准备完成
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            mState = PlaybackStateCompat.STATE_CONNECTING;
        }
        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }

    /**
     * 快进到指定位置完成，可以播放了
     *
     * @param mp 播放器
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mCurrentPosition = mp.getCurrentPosition();
        Log.d(TAG, "onSeekComplete() called=" + mCurrentPosition);

        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            mState = PlaybackStateCompat.STATE_PAUSED;
        }

        if (mIsAutoStart) {
            mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
            mIsAutoStart = false;
        }
        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }
}
