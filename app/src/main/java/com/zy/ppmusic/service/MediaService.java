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
import android.util.SparseArray;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.zy.ppmusic.MediaActivity;
import com.zy.ppmusic.R;
import com.zy.ppmusic.utils.PlayBack;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.util.ArrayList;
import java.util.List;

public class MediaService extends MediaBrowserServiceCompat {
    private static final String TAG = "MediaService";
    //通知的id
    private final int NOTIFY_ID = 412;

    private MediaSessionCompat sessionCompat;
    private PlaybackStateCompat.Builder mStateBuilder;
    private PlayBack mPlayBack;
    private List<String> mPlayQueueMediaId;
    private NotificationManagerCompat mNotificationManager;
    private MediaSessionCompat.QueueItem mCurrentMedia;
    private AudioBecomingNoisyReceiver audioBecomingNoisyReceiver;
    private boolean mServiceStarted;
    private boolean isAutoContinuedPlay = true;//播放完成是否继续下一首
    private SparseArray<MediaSessionCompat.QueueItem> mQueueValueArray;

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
        mPlayQueueMediaId = new ArrayList<>();
        mQueueValueArray = new SparseArray<>();
        setSessionToken(sessionCompat.getSessionToken());
        mStateBuilder = new PlaybackStateCompat.Builder();
        mStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        sessionCompat.setPlaybackState(mStateBuilder.build());
        sessionCompat.setCallback(new MediaSessionCallBack());
        sessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        sessionCompat.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);

        Intent it = new Intent(getApplicationContext(), MediaActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, it, PendingIntent.FLAG_UPDATE_CURRENT);
        sessionCompat.setSessionActivity(pendingIntent);

        mPlayBack = new PlayBack(this);
        mPlayBack.setCallBack(new PlayBack.CallBack() {
            @Override
            public void onCompletion() {
                handleStopRequest(false);
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

    /**
     * 停止播放
     *
     * @param isNeedEnd 是否需要停止播放
     */
    private void handleStopRequest(boolean isNeedEnd) {
        mPlayBack.stopPlayer();
        if (!isNeedEnd) {
            //判断重复模式，单曲重复，列表重复，列表播放
            switch (sessionCompat.getController().getRepeatMode()) {
                //列表重复：自动下一首
                case PlaybackStateCompat.REPEAT_MODE_ALL:
                    if (isAutoContinuedPlay) {
                        onMediaChange(mPlayBack.onSkipToNext());
                    }
                    break;
                //单曲重复：重复当前的歌曲
                case PlaybackStateCompat.REPEAT_MODE_ONE:
                    onMediaChange(mCurrentMedia.getDescription().getMediaId());
                    break;
                //列表播放：判断是否播放到列表的最后
                case PlaybackStateCompat.REPEAT_MODE_NONE:
                    List<MediaSessionCompat.QueueItem> queue = sessionCompat.getController().getQueue();
                    int position = queue.indexOf(mCurrentMedia);
                    Log.d(TAG, "handleStopRequest() query index=" + position);
                    if (position < (queue.size() - 1)) {
                        onMediaChange(mPlayBack.onSkipToNext());
                    } else {
                        Log.e(TAG, "handleStopRequest: 已播放到最后一首曲目");
                    }
                    break;
                default:

                    break;
            }
        } else {
            mNotificationManager.cancel(NOTIFY_ID);
            sessionCompat.setActive(false);
            stopForeground(true);
            System.exit(0);
        }
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
            postRemoteNotification();
            audioBecomingNoisyReceiver.register();
        } else {
            if (state == PlaybackStateCompat.STATE_PAUSED) {
                postRemoteNotification();
            } else {
                mNotificationManager.cancel(NOTIFY_ID);
            }
            stopForeground(false);
            audioBecomingNoisyReceiver.unregister();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //也可以在这接收通知按钮的event事件
        MediaButtonReceiver.handleIntent(sessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUId, @Nullable Bundle bundle) {
        return new BrowserRoot(clientPackageName, null);
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "service onLoadChildren() called with: s = [" + s + "], result = [" + result + "]");
        if (s.equals(getPackageName())) {
            result.detach();
            ScanMusicFile.getInstance().scanMusicFile(this).setOnScanComplete(new ScanMusicFile.OnScanComplete() {
                @Override
                protected void onComplete(ArrayList<String> paths) {
                    Log.d(TAG, "onComplete() called with: paths = [" + paths + "]");
                    List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
                    List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
                    for (int i = 0; i < paths.size(); i++) {
                        String mediaId = String.valueOf(paths.get(i).hashCode());
                        MediaMetadataCompat mediaMetadataCompat = ScanMusicFile.getInstance().
                                getMusicById(mediaId);
                        if (mediaMetadataCompat != null) {
                            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                                    mediaMetadataCompat.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                            mediaItems.add(mediaItem);
                            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(
                                    mediaMetadataCompat.getDescription(), Long.parseLong(mediaId));
                            queueItems.add(queueItem);
                            mQueueValueArray.append(i, queueItem);
                            mPlayQueueMediaId.add(mediaId);
                        } else {
                            Log.e(TAG, "onComplete: null is " + paths.get(i));
                        }
                    }
                    mPlayBack.setPlayQueue(mPlayQueueMediaId);
                    sessionCompat.setQueue(queueItems);
                    result.sendResult(mediaItems);
                }
            });
        }
    }

    public void postRemoteNotification() {
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
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notify_copy_layout);
        remoteViews.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap);
        if(mPlayBack.isPlaying()){
            remoteViews.setImageViewResource(R.id.notify_action_play_pause,R.drawable.ic_pause);
        }else{
            remoteViews.setImageViewResource(R.id.notify_action_play_pause,R.drawable.ic_play);
        }
        if (descriptionCompat != null) {
            Log.e(TAG, "postNotification: title=" + descriptionCompat.getTitle() + ",subTitle=" + descriptionCompat.getSubtitle());
            remoteViews.setTextViewText(R.id.notify_display_title, descriptionCompat.getTitle());
            remoteViews.setTextViewText(R.id.notify_display_sub_title, descriptionCompat.getSubtitle());
        } else {
            Log.e(TAG, "postNotification: description is null");
        }
        remoteViews.setOnClickPendingIntent(R.id.notify_action_play_pause, MediaButtonReceiver.
                buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_PLAY_PAUSE));
        remoteViews.setOnClickPendingIntent(R.id.notify_action_next, MediaButtonReceiver.
                buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        remoteViews.setOnClickPendingIntent(R.id.notify_action_close, MediaButtonReceiver.
                buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_STOP));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_small_notify);
        builder.setContentIntent(controller.getSessionActivity());

        notification = builder.build();
        notification.contentView = remoteViews;
        mNotificationManager.notify(NOTIFY_ID, notification);
        startForeground(NOTIFY_ID, notification);
    }

    public void onMediaChange(String mediaId) {
        //设置媒体信息
        MediaMetadataCompat track = ScanMusicFile.getInstance().getMusicById(mediaId);
        if (track != null) {
            sessionCompat.setMetadata(track);
        }
        mCurrentMedia = mQueueValueArray.valueAt(mPlayQueueMediaId.indexOf(mediaId));

        mPlayBack.onPlay(mediaId);

        onPlayStateChange(0, null);

        handlePlayRequest();
    }

    /**
     * 响应Activity的调用
     * getController.transportControls.
     */
    private final class MediaSessionCallBack extends MediaSessionCompat.Callback {

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(TAG, "onPlayFromMediaId() called with: mediaId = [" + mediaId + "], extras = [" + extras + "]");
            onMediaChange(mediaId);
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
            onMediaChange(mPlayBack.onSkipToNext());
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.d(TAG, "onSkipToPrevious() called");
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent ke = mediaButtonEvent.getParcelableExtra("android.intent.extra.KEY_EVENT");
            Bundle extras = mediaButtonEvent.getExtras();
            Log.e(TAG, "onMediaButtonEvent: extra=" + extras.toString());
            switch (ke.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    onMediaChange(mPlayBack.onSkipToNext());
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    handleStopRequest(true);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    /**
     * 处理播放请求
     */
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
            //当收到耳机被拔出时暂停播放
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
