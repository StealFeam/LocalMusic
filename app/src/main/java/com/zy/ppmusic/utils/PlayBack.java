package com.zy.ppmusic.utils;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.media.AudioAttributesCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.zy.ppmusic.entity.MusicInfoEntity;
import com.zy.ppmusic.service.MediaService;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

/**
 * @author ZhiTouPC
 */
public class PlayBack implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {
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
    /**
     * 当前播放队列
     */
    private List<String> mPlayQueue;
    private int mCurrentPosition;
    private String mCurrentMediaId;
    private CallBack mCallBack;
    private AudioManager audioManager;
    private volatile int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;

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
    private WeakReference<MediaService> mContextWeak;


    public PlayBack(MediaService mMediaService) {
        this.mContextWeak = new WeakReference<>(mMediaService);
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

    public void playMediaIdAutoStart(String mediaId) {
        this.mIsAutoStart = true;
        preparedWithMediaId(mediaId);
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
            try {
                String path = DataTransform.getInstance().getPath(index);
                if (TextUtils.isEmpty(path)) {
                    PrintOut.e("preparedWithMediaId error: path is empty");
                } else {
                    PrintOut.e("preparedWithMediaId: path=" + path);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AudioAttributes.Builder builder = new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setLegacyStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.setAudioAttributes(builder.build());
                    } else {
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    }
                    mMediaPlayer.setDataSource(mContextWeak.get(), Uri.parse(String.format(Locale.CHINA, "file://%s", path)));

                    mMediaPlayer.prepare();
                    if (mCallBack != null) {
                        mCallBack.onPlayBackStateChange(mState);
                    }
                    PrintOut.e("onPlay: music init complete index=" + index + " path=" + DataTransform.getInstance().getPath(index));
                }
            } catch (IOException e) {
                PrintOut.e("preparedWithMediaId error: " + e.getMessage());
                if (mCallBack != null) {
                    mCallBack.onError(0, e.getMessage());
                }
            }
        }
    }

    private void configMediaPlayerState() {
        PrintOut.i("configMediaPlayerState: started," + mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            //如果没有获取到硬件的播放权限则暂停播放
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                PrintOut.e("config:paused");
                pause();
                mIsPauseCauseAudio = true;
            }
        } else {
            if (mMediaPlayer != null) {
                if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                    mMediaPlayer.setVolume(0.2f, 0.2f);
                } else {
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
        int result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder builder = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN);
            builder.setOnAudioFocusChangeListener(this);
            result = audioManager.requestAudioFocus(builder.build());
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
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
        PrintOut.print("seek to " + position + "," + isAutoStart);
        mIsAutoStart = isAutoStart;
        mMediaPlayer.seekTo(position);
        mState = PlaybackStateCompat.STATE_BUFFERING;

        if (mCallBack != null) {
            mCallBack.onPlayBackStateChange(mState);
        }
    }

    private void releasePlayer(boolean releasePlayer) {
        if (mMediaPlayer != null && releasePlayer) {
            mMediaPlayer.stop();
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

    public String onSkipToNext() {
        checkPlayQueue();
        if (mPlayQueue.isEmpty()) {
            return null;
        }
        int queueIndex = mPlayQueue.indexOf(mCurrentMediaId);
        if (queueIndex == (mPlayQueue.size() - 1)) {
            queueIndex = -1;
        }
        String nextMediaId = mPlayQueue.get(++queueIndex);
        PrintOut.d("onSkipToNext() called..." + queueIndex);
        return nextMediaId;
    }

    private void checkPlayQueue() {
        if (mPlayQueue == null) {
            mPlayQueue = DataTransform.getInstance().getPathList();
        }
    }

    public String onSkipToPrevious() {
        if (mPlayQueue.isEmpty()) {
            return null;
        }
        int queueIndex = mPlayQueue.indexOf(mCurrentMediaId);
        if (queueIndex == 0) {
            queueIndex = mPlayQueue.size();
        }
        String preMediaId = mPlayQueue.get(--queueIndex);
        PrintOut.d("onSkipToPrevious() called..." + queueIndex + "," + preMediaId);
        return preMediaId;
    }

    public void stopPlayer() {
        mContextWeak.get().stopForeground(false);
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
            mMediaPlayer.setWakeMode(mContextWeak.get().getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
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
        PrintOut.d("onAudioFocusChange() called with: focusChange = [" + focusChange + "]");
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
            PrintOut.i("onAudioFocusChange: " + focusChange);
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
        PrintOut.i("onPrepared: called " + mMediaPlayer.getCurrentPosition());
        //准备完成
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            mState = PlaybackStateCompat.STATE_CONNECTING;
        }

        if (mIsAutoStart) {
            mIsAutoStart = false;
            mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
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
        mCurrentPosition = mMediaPlayer.getCurrentPosition();
        PrintOut.i("onSeekComplete() called=" + mCurrentPosition);

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

    public interface CallBack {
        /**
         * 播放完成
         */
        void onCompletion();

        /**
         * 播放器状态变化
         *
         * @param state 状态
         */
        void onPlayBackStateChange(int state);

        /**
         * 发生错误
         *
         * @param errorCode 错误码
         * @param error     错误信息
         */
        void onError(int errorCode, String error);
    }
}
