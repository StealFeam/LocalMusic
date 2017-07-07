package com.zy.ppmusic.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.zy.ppmusic.MediaActivity;
import com.zy.ppmusic.R;
import com.zy.ppmusic.utils.PlayBack;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.util.ArrayList;
import java.util.List;

public class MediaService extends MediaBrowserServiceCompat {
    private static final String TAG = "MediaService";
    private MediaSessionCompat sessionCompat;
    private PlaybackStateCompat.Builder mStateBuilder;
    private PlayBack mPlayBack;
    private List<String> mPlayQueue;
    private NotificationManagerCompat mNotificationManager;
    private MediaSessionCompat.QueueItem mCurrentMedia;
    private AudioBecomingNoisyReceiver audioBecomingNoisyReceiver;
    private boolean mServiceStarted;

    private Handler mDelayedStopHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: " + msg.toString());
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        if (sessionCompat == null) {
            sessionCompat = new MediaSessionCompat(this, TAG);
        }
        setSessionToken(sessionCompat.getSessionToken());
        mStateBuilder = new PlaybackStateCompat.Builder();
        mStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE);
        sessionCompat.setPlaybackState(mStateBuilder.build());
        sessionCompat.setCallback(new MediaSessionCallBack());
        sessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Intent it = new Intent(getApplicationContext(), MediaActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, it, PendingIntent.FLAG_UPDATE_CURRENT);
        sessionCompat.setSessionActivity(pendingIntent);

        mPlayBack = new PlayBack(this);
        mPlayBack.setCallBack(new PlayBack.CallBack() {
            @Override
            public void onCompletion() {
                handleStopRequest();
            }

            @Override
            public void onPlayBackStateChange(int state) {
                onPlayStateChange(0, null);
            }

            @Override
            public void onError(int errorCode, String error) {
                onPlayStateChange(errorCode, error);
            }
        });

        mNotificationManager = NotificationManagerCompat.from(this);
        audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver(this);
    }

    private void handleStopRequest() {

    }

    private void onPlayStateChange(int errorCode, String error) {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayBack != null) {
            position = mPlayBack.getCurrentStreamPosition();
        }
        long playbackActions = PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        if (mPlayBack != null && mPlayBack.isPlaying()) {
            playbackActions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(playbackActions);
        int state = mPlayBack.getState();
        if (error != null) {
            stateBuilder.setErrorMessage(errorCode, error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());
        if (mCurrentMedia != null) {
            stateBuilder.setActiveQueueItemId(mCurrentMedia.getQueueId());
        }
        sessionCompat.setPlaybackState(stateBuilder.build());
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            postNotification();
            audioBecomingNoisyReceiver.register();
        } else {
            if (state == PlaybackStateCompat.STATE_PAUSED) {
                postNotification();
            } else {
                mNotificationManager.cancel(412);
            }
            stopForeground(false);
            audioBecomingNoisyReceiver.unregister();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KeyEvent keyEvent = MediaButtonReceiver.handleIntent(sessionCompat, intent);
        if (keyEvent != null) {
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    mPlayBack.onSkipToNext();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    mPlayBack.onSkipToPrevious();
                    break;
                default:
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUId, @Nullable Bundle bundle) {
        return new BrowserRoot(clientPackageName, null);
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren() called with: s = [" + s + "], result = [" + result + "]");
        if (s.equals(getPackageName())) {
            result.detach();
            ScanMusicFile.getInstance().scanMusicFile(this).setOnScanComplete(new ScanMusicFile.OnScanComplete() {
                @Override
                protected void onComplete(ArrayList<String> paths) {
                    mPlayQueue = paths;
                    mPlayBack.setPlayQueue(mPlayQueue);
                    Log.d(TAG, "onComplete() called with: paths = [" + paths + "]");
                    List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
                    for (int i = 0; i < paths.size(); i++) {
                        MediaMetadataCompat mediaMetadataCompat = ScanMusicFile.getInstance().
                                getMusicById(String.valueOf(paths.get(i).hashCode()));
                        if (mediaMetadataCompat != null) {
                            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                                    mediaMetadataCompat.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                            mediaItems.add(mediaItem);
                        } else {
                            Log.e(TAG, "onComplete: null is " + paths.get(i));
                        }
                    }
                    result.sendResult(mediaItems);
                }
            });
        }
    }

    public void postNotification() {
        Notification notification;
        MediaControllerCompat controller = sessionCompat.getController();//获取媒体控制器
        MediaMetadataCompat metadata = controller.getMetadata();//media信息
        PlaybackStateCompat playbackState = controller.getPlaybackState();//播放状态
        if (metadata == null || playbackState == null) {
            Log.e(TAG, "postNotification: " + (metadata == null) + "," + (playbackState == null));
            return;
        }
        MediaDescriptionCompat descriptionCompat = metadata.getDescription();
        Bitmap iconBitmap = null;
        if (descriptionCompat != null) {
            iconBitmap = descriptionCompat.getIconBitmap();
        }
        if (iconBitmap == null) {
            iconBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Log.e(TAG, "postNotification: playing=" + mPlayBack.isPlaying());
        NotificationCompat.Action action = mPlayBack.isPlaying()
                ? new NotificationCompat.Action(R.drawable.ic_pause, "暂停",
                MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(), PlaybackStateCompat.ACTION_PAUSE))
                : new NotificationCompat.Action(R.drawable.ic_play, "播放",
                MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(), PlaybackStateCompat.ACTION_PLAY));
        //添加上一首图标和监听
        builder.addAction(R.drawable.ic_previous, "上一首", MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(),
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        //添加暂停或者播放图标和监听
        builder.addAction(action);
        //添加下一首图标和监听
        builder.addAction(R.drawable.ic_next, "下一首", MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(),
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        builder.setStyle(new NotificationCompat.MediaStyle()
                .setMediaSession(sessionCompat.getSessionToken())
                .setShowCancelButton(true)
                .setShowActionsInCompactView(1, 2)//缩小版显示第几个图标 上面添加了三个，所以对应的index为0，1，2
        );

        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setShowWhen(false)
                .setContentIntent(controller.getSessionActivity())//设置点击需要响应的activity
                .setLargeIcon(iconBitmap)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (descriptionCompat != null) {
            Log.e(TAG, "postNotification: title=" + descriptionCompat.getTitle() + ",subTitle=" + descriptionCompat.getSubtitle());
            builder.setContentTitle(descriptionCompat.getTitle());
            builder.setContentText(descriptionCompat.getSubtitle());
        } else {
            Log.e(TAG, "postNotification: description is null");
        }
        builder.setOngoing(true);
        notification = builder.build();
        mNotificationManager.notify(412, notification);
        startForeground(412, notification);
    }


    private final class MediaSessionCallBack extends MediaSessionCompat.Callback {

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(TAG, "onPlayFromMediaId() called with: mediaId = [" + mediaId + "], extras = [" + extras + "]");
            //设置媒体信息
            MediaMetadataCompat track = ScanMusicFile.getInstance().getMusicById(mediaId);
            if (track != null) {
                mCurrentMedia = new MediaSessionCompat.QueueItem(track.getDescription(), track.hashCode());
            }
            sessionCompat.setMetadata(track);

            mPlayBack.onPlay(mediaId);

            onPlayStateChange(0, null);

            handlePlayRequest();
        }

        @Override
        public void onPrepare() {
            super.onPrepare();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            Log.d(TAG, "onPlay() called");
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            super.onPause();
            mPlayBack.play();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.d(TAG, "onSkipToNext() called");
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.d(TAG, "onSkipToPrevious() called");
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    }


    public void handlePlayRequest() {
        if (mCurrentMedia == null) {
            return;
        }

        if (!mServiceStarted) {
            startService(new Intent(getApplicationContext(), MediaService.class));
            mServiceStarted = true;
        }

        if (!sessionCompat.isActive()) {
            sessionCompat.setActive(true);
        }

        mPlayBack.play();
    }


    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
        private final Context context;
        private boolean mIsRegistered = false;
        private IntentFilter mAudioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        private AudioBecomingNoisyReceiver(Context context) {
            this.context = context.getApplicationContext();
        }

        public void register() {
            if (!mIsRegistered) {
                context.registerReceiver(this, mAudioNoisyIntentFilter);
                mIsRegistered = true;
            }
        }

        public void unregister() {
            if (mIsRegistered) {
                context.unregisterReceiver(this);
                mIsRegistered = false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                handlePauseRequest();
            }
        }
    }

    private void handlePauseRequest() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sessionCompat.release();
    }
}
