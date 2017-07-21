package com.zy.ppmusic.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.zy.ppmusic.ui.MediaActivity;
import com.zy.ppmusic.data.db.DBManager;
import com.zy.ppmusic.entity.MusicDbEntity;
import com.zy.ppmusic.entity.MusicInfoEntity;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.FileUtils;
import com.zy.ppmusic.utils.NotificationUtils;
import com.zy.ppmusic.utils.PlayBack;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MediaService extends MediaBrowserServiceCompat {
    private static final String TAG = "MediaService";
    /*-------------------play action--------------------------*/
    //播放指定id
    public static final String ACTION_PLAY_WITH_ID = "PLAY_WITH_ID";
    //缓冲指定id
    public static final String ACTION_PREPARED_WITH_ID = "PREPARED_WITH_ID";
    //播放或者暂停
    public static final String ACTION_PLAY_OR_PAUSE = "PLAY_OR_PAUSE";
    //初始化播放器
    public static final String ACTION_PLAY_INIT = "PLAY_INIT";
    //快进
    public static final String ACTION_SEEK_TO = "SEEK_TO";

    /*-------------------play action end--------------------------*/

    //获取参数
    public static final String ACTION_PARAM = "ACTION_PARAM";


    /*-------------------command action--------------------------*/
    //获取播放位置
    public static final String COMMAND_POSITION = "COMMAND_POSITION";
    //获取播放位置 resultCode
    public static final int COMMAND_POSITION_CODE = 0x001;

    //更新播放列表
    public static final String COMMAND_UPDATE_QUEUE = "COMMAND_UPDATE_QUEUE";
    /*-------------------command action end--------------------------*/


    //通知的id
    public static final int NOTIFY_ID = 412;

    private final String CACHE_MEDIA_LIST = "CACHE_MEDIA_LIST";
    private final String CACHE_QUEUE_LIST = "CACHE_QUEUE_LIST";

    private MediaSessionCompat mMediaSessionCompat;
    private PlaybackStateCompat.Builder mPlayBackStateBuilder;
    private PlayBack mPlayBack;
    private List<String> mPlayQueueMediaId;
    private NotificationManagerCompat mNotificationManager;
    private MediaSessionCompat.QueueItem mCurrentMedia;
    private AudioBecomingNoisyReceiver mAudioReceiver;
    private boolean mServiceStarted;
    private boolean isAutoContinuedPlay = true;//播放完成是否继续下一首
    private List<MediaBrowserCompat.MediaItem> mMediaItemList = new ArrayList<>();
    private List<MediaSessionCompat.QueueItem> mQueueItemList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        if (mMediaSessionCompat == null) {
            mMediaSessionCompat = new MediaSessionCompat(this, TAG);
        }
        mPlayQueueMediaId = new ArrayList<>();
        setSessionToken(mMediaSessionCompat.getSessionToken());
        mPlayBackStateBuilder = new PlaybackStateCompat.Builder();
        mPlayBackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        mMediaSessionCompat.setPlaybackState(mPlayBackStateBuilder.build());
        mMediaSessionCompat.setCallback(new MediaSessionCallBack());
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSessionCompat.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);

        if (!mServiceStarted) {
            startService(new Intent(getBaseContext(), MediaService.class));
            mServiceStarted = true;
        }

        if (!mMediaSessionCompat.isActive()) {
            mMediaSessionCompat.setActive(true);
        }

        Intent it = new Intent(getBaseContext(), MediaActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 1, it, PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSessionCompat.setSessionActivity(pendingIntent);

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
        mAudioReceiver = new AudioBecomingNoisyReceiver(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //也可以在这接收通知按钮的event事件
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return START_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUId, @Nullable Bundle bundle) {
        return new BrowserRoot(clientPackageName, bundle);
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "service onLoadChildren() called with: s = [" + s + "], result = [" + result + "]");
        if (s.equals(getPackageName())) {
            //如果当前队列为空时
            if (mMediaItemList.size() == 0) {
                result.detach();
                Object localData = FileUtils.readObject(getCacheDir().getAbsolutePath());
                if (localData != null) {
                    DataTransform.getInstance().transFormData((List<MusicInfoEntity>) localData);
                    mMediaItemList = DataTransform.getInstance().getMediaItemList();
                    mQueueItemList = DataTransform.getInstance().getQueueItemList();
                    mPlayQueueMediaId = DataTransform.getInstance().getMediaIdList();
                    mPlayBack.setPlayQueue(mPlayQueueMediaId);
                    mMediaSessionCompat.setQueue(mQueueItemList);
                    result.sendResult(mMediaItemList);
                } else {
                    ScanMusicFile.getInstance().scanMusicFile(this).setOnScanComplete(new ScanMusicFile.OnScanComplete() {
                        @Override
                        protected void onComplete(ArrayList<String> paths) {
                            DataTransform.getInstance().transFormData(getApplicationContext(),paths);
                            mMediaItemList = DataTransform.getInstance().getMediaItemList();
                            mQueueItemList = DataTransform.getInstance().getQueueItemList();
                            mPlayQueueMediaId = DataTransform.getInstance().getMediaIdList();
                            mPlayBack.setPlayQueue(mPlayQueueMediaId);
                            mMediaSessionCompat.setQueue(mQueueItemList);
                            result.sendResult(mMediaItemList);

                            FileUtils.saveObject(DataTransform.getInstance().getMusicInfoEntities(),
                                    getCacheDir().getAbsolutePath());
                        }
                    });
                }
            } else {//不为空直接替换列表
                result.sendResult(mMediaItemList);
            }
        }
    }

    /**
     * 停止播放
     *
     * @param isNeedEnd 是否需要停止播放
     */
    private void handleStopRequest(boolean isNeedEnd) {
        Log.d(TAG, "handleStopRequest() called with: isNeedEnd = [" + isNeedEnd + "]");

        if (isNeedEnd) {
            savePlayingRecord();
        }
        if (!isNeedEnd) {
            //判断重复模式，单曲重复，列表重复，列表播放
            switch (mMediaSessionCompat.getController().getRepeatMode()) {
                //列表重复：自动下一首
                case PlaybackStateCompat.REPEAT_MODE_ALL:
                    if (isAutoContinuedPlay) {
                        onMediaChange(mPlayBack.onSkipToNext());
                        handlePlayOrPauseRequest();
                    }
                    break;
                //单曲重复：重复当前的歌曲
                case PlaybackStateCompat.REPEAT_MODE_ONE:
                    onMediaChange(mCurrentMedia.getDescription().getMediaId());
                    handlePlayOrPauseRequest();
                    break;
                //列表播放：判断是否播放到列表的最后
                case PlaybackStateCompat.REPEAT_MODE_NONE:
                    int position = mQueueItemList.indexOf(mCurrentMedia);
                    Log.d(TAG, "handleStopRequest() query index=" + position);
                    if (position < (mQueueItemList.size() - 1)) {
                        onMediaChange(mPlayBack.onSkipToNext());
                        handlePlayOrPauseRequest();
                    } else {
                        onMediaChange(null);
                        Log.e(TAG, "handleStopRequest: 已播放到最后一首曲目");
                    }
                    break;
                default:
                    break;
            }
        } else {
            mNotificationManager.cancel(NOTIFY_ID);
            mMediaSessionCompat.setActive(false);
            stopForeground(true);
            System.exit(0);
        }
    }

    /**
     * 保存播放记录到本地
     */
    public void savePlayingRecord() {
        MusicDbEntity dbEntity = new MusicDbEntity();
        dbEntity.setLastMediaId(mPlayBack.getCurrentMediaId());
        dbEntity.setLastMediaPath(mCurrentMedia.getDescription().getMediaUri().getPath());
        dbEntity.setLastPlayAuthor(String.valueOf(mCurrentMedia.getDescription().getSubtitle()));
        dbEntity.setLastPlayedPosition(mPlayBack.getCurrentStreamPosition());
        dbEntity.setLastPlayIndex(mPlayQueueMediaId.indexOf(mPlayBack.getCurrentMediaId()));
        dbEntity.setLastPlayName(String.valueOf(mCurrentMedia.getDescription().getTitle()));
        DBManager.getInstance().initDb(getApplicationContext()).deleteAll();
        DBManager.getInstance().insetEntity(dbEntity);
    }

    /**
     * 当播放状态发生改变时
     *
     * @param errorCode 错误代码
     * @param error 错误描述
     */
    private void onPlayStateChange(int errorCode, String error) {
        Log.d(TAG, "onPlayStateChange() called with: " + mPlayBack.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayBack != null) {
            position = mPlayBack.getCurrentStreamPosition();
        }
        long playbackActions = PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        if (mPlayBack.isPlaying()) {
            playbackActions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(playbackActions);
        int state = mPlayBack.getState();
        if (error != null) {
            stateBuilder.setErrorMessage(errorCode, error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());
        if (mCurrentMedia != null) {
            stateBuilder.setActiveQueueItemId(mCurrentMedia.getQueueId());
        }
        mMediaSessionCompat.setPlaybackState(stateBuilder.build());

        Notification notification = NotificationUtils.postNotify(this, mMediaSessionCompat,
                mPlayBack.isPlaying());
        startForeground(NOTIFY_ID, notification);

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            mAudioReceiver.register();
        } else {
            mAudioReceiver.unregister();
        }
    }

    /**
     * 播放曲目发生变化时
     *
     * @param mediaId 曲目id
     */
    public void onMediaChange(String mediaId) {
        if (mediaId != null) {
            //设置媒体信息
            MediaMetadataCompat track = DataTransform.getInstance().getMetadataCompatList().get(mediaId);
            if (track != null) {
                mMediaSessionCompat.setMetadata(track);
            }
            mCurrentMedia = mQueueItemList.get(DataTransform.getInstance().getMediaIndex(mediaId));

            mPlayBack.preparedWithMediaId(mediaId);
        } else {
            mPlayBack.stopPlayer();
        }
    }

    /**
     * 更新列表
     */
    private void updateQueue(){
        mMediaItemList = DataTransform.getInstance().getMediaItemList();
        mQueueItemList = DataTransform.getInstance().getQueueItemList();
        mPlayQueueMediaId = DataTransform.getInstance().getMediaIdList();
        mPlayBack.setPlayQueue(mPlayQueueMediaId);
        mMediaSessionCompat.setQueue(mQueueItemList);
        //覆盖本地缓存
        FileUtils.saveObject(DataTransform.getInstance().getMusicInfoEntities(),
                getCacheDir().getAbsolutePath());
    }

    /**
     * 响应Activity的调用
     * getController.transportControls.
     */
    private final class MediaSessionCallBack extends MediaSessionCompat.Callback {

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(TAG, "onPlayFromMediaId() called with: mediaId = [" + mediaId + "]");
            if (extras != null) {
                String action = extras.getString(ACTION_PARAM);
                Log.d(TAG, "onPlayFromMediaId: extra=" + action);
                if (ACTION_PLAY_OR_PAUSE.equals(action)) {//开始暂停请求
                    handlePlayOrPauseRequest();
                } else if (ACTION_PREPARED_WITH_ID.equals(action)) {//缓冲请求
                    onMediaChange(mediaId);
                } else if (ACTION_PLAY_WITH_ID.equals(action)) {//播放指定id请求
                    //如果和当前的mediaId相同则视为暂停或播放操作，不同则替换曲目
                    if (!Objects.equals(mediaId, mCurrentMedia != null ?
                            mCurrentMedia.getDescription().getMediaId() : null)) {
                        onMediaChange(mediaId);
                    }
                    handlePlayOrPauseRequest();
                } else if (ACTION_PLAY_INIT.equals(action)) {//初始化播放器，如果本地有播放记录，取播放记录，没有就初始化穿过来的media
                    List<MusicDbEntity> entity = DBManager.getInstance().initDb(getApplicationContext()).getEntity();
                    if (entity.size() > 0) {
                        onMediaChange(entity.get(0).getLastMediaId());
                        mPlayBack.seekTo(entity.get(0).getLastPlayedPosition(),false);
                    } else {
                        onMediaChange(mediaId);
                    }
                }else if(ACTION_SEEK_TO.equals(action)){
                    int seekPosition = extras.getInt("position");
                    mPlayBack.seekTo(seekPosition,true);
                }else {
                    onMediaChange(mediaId);
                }
            }
        }

        @Override
        public void onPrepare() {
            super.onPrepare();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            Log.d(TAG, "onPlay() called");
            handlePlayOrPauseRequest();
        }

        @Override
        public void onPause() {
            super.onPause();
            mPlayBack.pause();
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext() called");
            onMediaChange(mPlayBack.onSkipToNext());
            handlePlayOrPauseRequest();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.d(TAG, "onSkipToPrevious() called");
            onMediaChange(mPlayBack.onSkipToPrevious());
            handlePlayOrPauseRequest();
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent ke = mediaButtonEvent.getParcelableExtra("android.intent.extra.KEY_EVENT");
            Bundle extras = mediaButtonEvent.getExtras();
            Log.e(TAG, "onMediaButtonEvent: extra=" + extras.toString());
            switch (ke.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    onMediaChange(mPlayBack.onSkipToNext());
                    handlePlayOrPauseRequest();
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    handleStopRequest(true);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    onMediaChange(mPlayBack.onSkipToPrevious());
                    handlePlayOrPauseRequest();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    handlePlayOrPauseRequest();
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            Bundle extra = new Bundle();
            switch (command) {
                case COMMAND_POSITION:
                    extra.putInt("position", mPlayBack.getCurrentStreamPosition());
                    cb.send(COMMAND_POSITION_CODE, extra);
                    break;
                case COMMAND_UPDATE_QUEUE:
                    updateQueue();
                    break;
                default:
                    System.out.println("onCommand no match");
                    super.onCommand(command, extras, cb);
                    break;
            }

        }
    }

    /**
     * 处理播放或者暂停请求
     */
    public void handlePlayOrPauseRequest() {
        if (mCurrentMedia == null) {
            return;
        }
        //如果状态不为暂停或者播放，则重置媒体信息
        if(mPlayBack.getState() != PlaybackStateCompat.ACTION_PAUSE
                && mPlayBack.getState() != PlaybackStateCompat.STATE_PLAYING){
            onMediaChange(String.valueOf(mCurrentMedia.getQueueId()));
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
                Log.e(TAG, "拔出耳机了");
                handlePlayOrPauseRequest();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called with: intent = [" + intent + "]");
        return super.onBind(intent);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        Log.d(TAG, "unbindService() called with: conn = [" + conn + "]");
    }

    @Override
    public void onLoadItem(String itemId, @NonNull Result<MediaBrowserCompat.MediaItem> result) {
        super.onLoadItem(itemId, result);
        Log.d(TAG, "onLoadItem() called with: itemId = [" + itemId + "], result = [" + result + "]");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        mMediaSessionCompat.release();
    }
}
