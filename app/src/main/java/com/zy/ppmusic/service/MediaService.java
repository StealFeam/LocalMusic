package com.zy.ppmusic.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.zy.ppmusic.callback.AudioNoisyCallBack;
import com.zy.ppmusic.callback.TimeTikCallBack;
import com.zy.ppmusic.data.db.DataBaseManager;
import com.zy.ppmusic.entity.MusicDbEntity;
import com.zy.ppmusic.mvp.view.MediaActivity;
import com.zy.ppmusic.receiver.AudioBecomingNoisyReceiver;
import com.zy.ppmusic.receiver.LoopReceiver;
import com.zy.ppmusic.utils.Constant;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.FileUtils;
import com.zy.ppmusic.utils.LocalCacheMediaLoader;
import com.zy.ppmusic.utils.NotificationUtils;
import com.zy.ppmusic.utils.PlayBack;
import com.zy.ppmusic.utils.PrintLog;
import com.zy.ppmusic.utils.ScanMusicFile;
import com.zy.ppmusic.utils.StringUtils;
import com.zy.ppmusic.utils.TimerUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zy.ppmusic.utils.NotificationUtils.NOTIFY_ID;

/**
 * @author ZhiTouPC
 */
public class MediaService extends MediaBrowserServiceCompat {
    /**
     * 播放指定id
     */
    public static final String ACTION_PLAY_WITH_ID = "PLAY_WITH_ID";
    /*-------------------play action--------------------------*/
    /**
     * 缓冲指定id
     */
    public static final String ACTION_PREPARED_WITH_ID = "PREPARED_WITH_ID";
    /**
     * 初始化播放器
     */
    public static final String ACTION_PLAY_INIT = "PLAY_INIT";
    /**
     * 快进
     */
    public static final String ACTION_SEEK_TO = "SEEK_TO";
    /**
     * 获取参数
     */
    public static final String ACTION_PARAM = "ACTION_PARAM";

    /*-------------------play action end--------------------------*/
    /**
     * 快进
     */
    public static final String SEEK_TO_POSITION_PARAM = "SEEK_TO_POSITION_PARAM";

    /*-------------------command action--------------------------*/
    /**
     * 开启循环
     */
    public static final String COMMAND_START_LOOP = "COMMAND_START_LOOP";

    /**
     * 关闭循环
     */
    public static final String COMMAND_STOP_LOOP = "COMMAND_STOP_LOOP";
    /**
     * 获取播放位置
     */
    public static final String COMMAND_POSITION = "COMMAND_POSITION";
    /**
     * 获取播放位置 resultCode
     */
    public static final int COMMAND_POSITION_CODE = 0x001;
    /**
     * 更新播放列表
     */
    public static final String COMMAND_UPDATE_QUEUE = "COMMAND_UPDATE_QUEUE";
    /**
     * 更新播放列表resultCode
     */
    public static final int COMMAND_UPDATE_QUEUE_CODE = 0x002;
    /*-------------------command action end--------------------------*/
    /*-------------------custom action start--------------------------*/
    /**
     * 播放列表为空，本地未搜索到曲目
     */
    public static final String ERROR_PLAY_QUEUE_EVENT = "ERROR_PLAY_QUEUE_EVENT";
    /**
     * 加载中...
     */
    public static final String LOADING_QUEUE_EVENT = "LOADING_QUEUE_EVENT";
    /**
     * 加载完成...
     */
    public static final String LOAD_COMPLETE_EVENT = "LOAD_COMPLETE_EVENT";
    /**
     * 加载本地缓存位置...
     */
    public static final String LOCAL_CACHE_POSITION_EVENT = "LOCAL_CACHE_POSITION_EVENT";
    /**
     * 更新播放位置...
     */
    public static final String UPDATE_POSITION_EVENT = "UPDATE_POSITION_EVENT";

    /**
     * 开始倒计时
     */
    public static final String ACTION_COUNT_DOWN_TIME = "ACTION_COUNT_DOWN_TIME";
    /**
     * 倒计时结束
     */
    public static final String ACTION_COUNT_DOWN_END = "ACTION_COUNT_DOWN_END";
    /**
     * 停止倒计时
     */
    public static final String ACTION_STOP_COUNT_DOWN = "ACTION_STOP_COUNT_DOWN";

    /**
     * -------------------custom action end--------------------------
     */
    private static final String TAG = "MediaService";
    /**
     * 保持后台运行且与前台进行通信
     */
    private MediaSessionCompat mMediaSessionCompat;
    /**
     * 播放器controller
     */
    private PlayBack mPlayBack;
    /**
     * 媒体id列表
     */
    private List<String> mPlayQueueMediaId;
    private List<MediaBrowserCompat.MediaItem> mMediaItemList = new ArrayList<>();
    private List<MediaSessionCompat.QueueItem> mQueueItemList = new ArrayList<>();
    /**
     * 当前播放的媒体
     */
    private volatile MediaSessionCompat.QueueItem mCurrentMedia;
    /**
     * 音频监听
     */
    private AudioBecomingNoisyReceiver mAudioReceiver;
    /**
     * 倒计时
     */
    private TimerUtils mCountDownTimer;
    /**
     * 线程池
     */
    private ExecutorService mBackgroundPool;
    /**
     * 更新当前播放的媒体信息
     */
    private UpdateRunnable mUpdateRunnable;
    /**
     * 更新播放列表
     */
    private UpdateQueueRunnable mUpdateQueueRunnable;
    /**
     * 错误曲目数量
     * 当无法播放曲目数量和列表数量相同时销毁播放器避免循环
     */
    private int mErrorTimes;
    private IntentFilter filter = new IntentFilter(LoopService.ACTION);
    private LoopReceiver receiver;
    private Handler mHandler = new Handler();
    private AudioNoisyCallBack audioCallBack = new AudioNoisyCallBack() {
        @Override
        public void comingNoisy() {
            handlePlayOrPauseRequest();
        }
    };
    /**
     * 倒计时监听
     */
    private TimeTikCallBack timeTikCallBack = new TimeTikCallBack() {
        @Override
        public void onTik(long mis) {
            //如果页面绑定时
            if (mMediaSessionCompat.getController() != null) {
                if (mis != 0) {
                    Bundle bundle = new Bundle();
                    bundle.putLong(ACTION_COUNT_DOWN_TIME, mis);
                    mMediaSessionCompat.sendSessionEvent(ACTION_COUNT_DOWN_TIME, bundle);
                } else {
                    if (mMediaSessionCompat.getController() != null) {
                        mMediaSessionCompat.sendSessionEvent(ACTION_COUNT_DOWN_END, null);
                    }
                    handleStopRequest(true);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        PrintLog.e("onCreate----------");
        if (!Constant.INSTANCE.getIS_STARTED()) {
            PrintLog.e("启动Service。。。。。");
            stopSelf();
            startService(new Intent(getBaseContext(), MediaService.class));
            Constant.INSTANCE.setIS_STARTED(true);
        }
        if (mMediaSessionCompat == null) {
            mMediaSessionCompat = new MediaSessionCompat(this, TAG);
        }
        mPlayQueueMediaId = new ArrayList<>();
        setSessionToken(mMediaSessionCompat.getSessionToken());
        PlaybackStateCompat.Builder mPlayBackStateBuilder = new PlaybackStateCompat.Builder();
        mPlayBackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        mMediaSessionCompat.setPlaybackState(mPlayBackStateBuilder.build());
        mMediaSessionCompat.setCallback(new MediaSessionCallBack());
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS |
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        mMediaSessionCompat.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);

        if (!mMediaSessionCompat.isActive()) {
            mMediaSessionCompat.setActive(true);
        }

        Intent it = new Intent(getBaseContext(), MediaActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 1, it,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSessionCompat.setSessionActivity(pendingIntent);

        mAudioReceiver = new AudioBecomingNoisyReceiver(this);

        mBackgroundPool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, TAG);
            }
        });
    }


    @Override
    public IBinder onBind(Intent intent) {
        PrintLog.e("onBind-----" + intent.toString());
        return super.onBind(intent);
    }

    private void initPlayBack() {
        if (mPlayBack != null) {
            return;
        }
        mPlayBack = new PlayBack(this);
        mPlayBack.setCallBack(new PlayBack.CallBack() {
            @Override
            public void onCompletion() {
                if (mErrorTimes != 0) {
                    mErrorTimes = 0;
                }
                handleStopRequest(false);
            }

            @Override
            public void onPlayBackStateChange(int state) {
                onPlayStateChange();
            }

            @Override
            public void onError(int errorCode, String error) {
                mErrorTimes++;
                if (mErrorTimes < mMediaItemList.size()) {
                    onMediaChange(mPlayBack.onSkipToNext(), true);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        initPlayBack();
        //也可以在这接收通知按钮的event事件
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return START_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUId, @Nullable Bundle bundle) {
        Log.d(TAG, "onGetRoot() called with: clientPackageName = [" + clientPackageName + "], clientUId = [" + clientUId + "], bundle = [" + bundle + "]");
        if (clientPackageName.equals(getPackageName())) {
            return new BrowserRoot(clientPackageName, bundle);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "service onLoadChildren() called with: s = [" + s + "], result = [" + result + "]");
        if (s.equals(getPackageName())) {
            if (mMediaItemList.size() == 0) {
                result.detach();
                ArrayList<MediaBrowserCompat.MediaItem> list = DataTransform.getInstance().getMediaItemList();
                if (list != null) {
                    mMediaItemList = list;
                    result.sendResult(list);
                    PrintLog.print("load list size ... " + mMediaItemList.size());
                } else {
                    mBackgroundPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            LocalCacheMediaLoader localCacheMediaLoader = new LocalCacheMediaLoader();
                            List<String> mediaPaths = localCacheMediaLoader.getMediaPaths();
                            if (mediaPaths != null) {
                                PrintLog.e("加载本地数据了");
                                mMediaItemList = DataTransform.getInstance().getMediaItemList();
                                result.sendResult(mMediaItemList);
                            } else {
                                ScanMusicFile.getInstance().setOnScanComplete(new ScanMusicFile.AbstractOnScanComplete() {
                                    @Override
                                    protected void onComplete(ArrayList<String> paths) {
                                        PrintLog.e("扫描本地数据了");
                                        DataTransform.getInstance().transFormData(getApplicationContext(), paths);
                                        result.sendResult(mMediaItemList);
                                    }
                                }).scanMusicFile(getApplicationContext());
                            }
                        }
                    });
                }
            } else {
                result.sendResult(mMediaItemList);
            }
            if (mUpdateQueueRunnable == null) {
                mUpdateQueueRunnable = new UpdateQueueRunnable(this);
            }
            mBackgroundPool.submit(mUpdateQueueRunnable);
        }
    }

    /**
     * 停止播放
     *
     * @param isNeedEnd 是否需要停止播放
     */
    private void handleStopRequest(boolean isNeedEnd) {
        Log.d(TAG, "handleStopRequest() called with: isNeedEnd = [" + isNeedEnd + "]");
        if (!isNeedEnd) {
            changeMediaByMode(true, true);
        } else {
            if (mPlayBack.isPlaying()) {
                handlePlayOrPauseRequest();
            }
            savePlayingRecord();
            if (mUpdateQueueRunnable != null) {
                mBackgroundPool.submit(mUpdateQueueRunnable);
            }
            NotificationUtils.cancelNotify(this, NotificationUtils.NOTIFY_ID);
            mMediaSessionCompat.setActive(false);
            mMediaSessionCompat.release();
            mAudioReceiver.unregister();
            stopForeground(true);
            stopSelf();
        }
    }

    /**
     * 通过列表模式决定下一个播放的媒体
     *
     * @param isNext     true下一首曲目，false上一首曲目
     * @param isComplete 调用是否来自歌曲播放完成
     */
    private void changeMediaByMode(boolean isNext, boolean isComplete) {
        if (mPlayBack == null) {
            Log.e(TAG, "changeMediaByMode: playback is null");
            PrintLog.d("尝试启动界面");
            startService(new Intent(getApplicationContext(), MediaService.class));
            Constant.INSTANCE.setIS_STARTED(false);
            return;
        }
        Log.e(TAG, "changeMediaByMode: " + mMediaSessionCompat.getController().getRepeatMode());
        //判断重复模式，单曲重复，随机播放，列表播放
        switch (mMediaSessionCompat.getController().getRepeatMode()) {
            //随机播放：自动下一首  ----暂改为列表循环
            case PlaybackStateCompat.REPEAT_MODE_ALL:
                onMediaChange(mPlayBack.onSkipToNext(), true);
                break;
            //单曲重复：重复当前的歌曲
            case PlaybackStateCompat.REPEAT_MODE_ONE:
                onMediaChange(mCurrentMedia.getDescription().getMediaId(), true);
                break;
            //列表播放：判断是否播放到列表的最后
            case PlaybackStateCompat.REPEAT_MODE_NONE:
                if (isNext) {
                    int position = mQueueItemList.indexOf(mCurrentMedia);
                    //如果不是当前歌曲播放完成自动调用的话，就直接播放下一首
                    if (!isComplete || position < (mQueueItemList.size() - 1)) {
                        onMediaChange(mPlayBack.onSkipToNext(), true);
                    } else {
                        onMediaChange(mPlayQueueMediaId.get(mPlayQueueMediaId.size() - 1), false);
                        Log.e(TAG, "handleStopRequest: 已播放到最后一首曲目");
                    }
                } else {
                    onMediaChange(mPlayBack.onSkipToPrevious(), true);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 保存播放记录到本地
     */
    public void savePlayingRecord() {
        //当前没有播放曲目
        if (mCurrentMedia == null) {
            return;
        }
        MusicDbEntity cacheEntity = new MusicDbEntity();
        cacheEntity.setLastMediaId(mPlayBack.getCurrentMediaId());
        if (mCurrentMedia != null && mCurrentMedia.getDescription() != null) {
            if (mCurrentMedia.getDescription().getMediaUri() != null) {
                cacheEntity.setLastMediaPath(mCurrentMedia.getDescription().getMediaUri().getPath());
            }
            cacheEntity.setLastPlayAuthor(String.valueOf(mCurrentMedia.getDescription().getSubtitle()));
            cacheEntity.setLastPlayName(String.valueOf(mCurrentMedia.getDescription().getTitle()));
        }
        cacheEntity.setLastPlayedPosition(mPlayBack.getCurrentStreamPosition());
        cacheEntity.setLastPlayIndex(mPlayQueueMediaId.indexOf(mPlayBack.getCurrentMediaId()));
        //删除已有的记录
        DataBaseManager.getInstance().initDb(getApplicationContext()).deleteAll();
        DataBaseManager.getInstance().insetEntity(cacheEntity);
    }

    /**
     * 当播放状态发生改变时
     */
    private void onPlayStateChange() {
        initPlayBack();
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

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());
        if (mCurrentMedia != null) {
            stateBuilder.setActiveQueueItemId(mCurrentMedia.getQueueId());
        }
        mMediaSessionCompat.setPlaybackState(stateBuilder.build());
        Notification notification = NotificationUtils.createNotify(this, mMediaSessionCompat, mPlayBack.isPlaying());
        if (notification != null) {
            startForeground(NOTIFY_ID, notification);
        }
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            mAudioReceiver.register(audioCallBack);
        } else {
            mAudioReceiver.unregister();
        }
    }

    /**
     * 播放曲目发生变化时
     *
     * @param mediaId                曲目id
     * @param shouldPlayWhenPrepared 是否需要准备完成后播放
     */
    public void onMediaChange(String mediaId, boolean shouldPlayWhenPrepared) {
        onMediaChange(mediaId, shouldPlayWhenPrepared, 0);
    }

    private void removeQueueItemByDes(MediaDescriptionCompat des) {
        int index = getIndexByDes(des);
        removeQueueItemAt(index);
    }

    private int getIndexByDes(MediaDescriptionCompat des) {
        for (int i = 0; i < mQueueItemList.size(); i++) {
            MediaSessionCompat.QueueItem queueItem = mQueueItemList.get(i);
            if (queueItem != null && des != null) {
                if (StringUtils.Companion.ifEquals(queueItem.getDescription().getMediaId(),
                        des.getMediaId())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 移除列表中的item
     *
     * @param removeIndex 要移除item的位置
     */
    public void removeQueueItemAt(int removeIndex) {
        if (removeIndex == -1) {
            Log.e(TAG, "removeQueueItemAt: the index is " + removeIndex);
            return;
        }
        Log.e(TAG, "removeQueueItemAt: " + removeIndex);
        int state = mPlayBack.getState();
        if (mPlayBack.isPlaying()) {
            mPlayBack.pause();
        }
        //如果删除的是当前播放的歌曲，则播放新的曲目
        if (mPlayBack.getCurrentIndex() == removeIndex) {
            DataTransform.getInstance().removeItem(getApplicationContext(), removeIndex);
            mBackgroundPool.submit(mUpdateQueueRunnable);
            if (mPlayQueueMediaId.size() > 0) {
                //删除的是前列表倒数第二个曲目的时候直接播放替代的曲目
                if (removeIndex <= mPlayQueueMediaId.size() - 1) {
                    onMediaChange(mPlayQueueMediaId.get(removeIndex), state == PlaybackStateCompat.STATE_PLAYING);
                } else {//删除的是前列表最后一个曲目播放列表的第一个曲目
                    onMediaChange(mPlayQueueMediaId.get(0), state == PlaybackStateCompat.STATE_PLAYING);
                }
            } else {
                mPlayBack.stopPlayer();
            }
        } else {//如果不是当前曲目，不能影响当前播放,记录下播放进度，更新列表后继续播放
            int currentIndex = mPlayBack.getCurrentIndex();
            int position = mPlayBack.getCurrentStreamPosition();
            DataTransform.getInstance().removeItem(getApplicationContext(), removeIndex);
            mBackgroundPool.submit(mUpdateQueueRunnable);
            if (currentIndex < removeIndex) {
                onMediaChange(mPlayQueueMediaId.get(currentIndex),
                        state == PlaybackStateCompat.STATE_PLAYING, position);
            } else {
                onMediaChange(mPlayQueueMediaId.get(currentIndex - 1),
                        state == PlaybackStateCompat.STATE_PLAYING, position);
            }
        }
    }

    /**
     * 处理播放或者暂停请求
     */
    public void handlePlayOrPauseRequest() {
        if (mCurrentMedia == null) {
            startActivity(new Intent(getApplicationContext(), MediaActivity.class));
            Constant.INSTANCE.setIS_STARTED(false);
            return;
        }
        if (mPlayBack == null) {
            PrintLog.e("播放器未初始化");
            return;
        }
        mPlayBack.play();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called with: intent = [" + intent + "]");
        //TODO 如果前台退出绑定，且没有播放媒体就让服务停止
//        if (mPlayBack != null && !mPlayBack.isPlaying()) {
//            handleStopRequest(true);
//        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        NotificationUtils.cancelAllNotify(this);
        DataBaseManager.getInstance().closeConn();
        mAudioReceiver.unregister();
        mMediaSessionCompat.release();
        if (mCountDownTimer != null) {
            mCountDownTimer.stopTik();
            mCountDownTimer = null;
        }
        if (mPlayBack != null) {
            mPlayBack.stopPlayer();
            mPlayBack = null;
        }
    }

    public void stopLoop() {
        if (receiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        }
        stopService(new Intent(this, LoopService.class));
    }

    private void startLoop() {
        if (receiver == null) {
            receiver = new LoopReceiver(this);
        }
        stopLoop();
        startService(new Intent(this, LoopService.class));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    /**
     * 进度更新到界面
     */
    public void updatePositionToSession() {
        try {
            if (mMediaSessionCompat.getController() == null) {
                stopLoop();
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putInt(MediaService.UPDATE_POSITION_EVENT, mPlayBack.getCurrentStreamPosition());
            mMediaSessionCompat.sendSessionEvent(MediaService.UPDATE_POSITION_EVENT, bundle);
        } catch (Exception e) {
            stopLoop();
        }
    }

    /**
     * 播放曲目发生变化时
     *
     * @param mediaId                曲目id
     * @param shouldPlayWhenPrepared 是否需要准备完成后播放
     */
    public void onMediaChange(String mediaId, boolean shouldPlayWhenPrepared, int shouldSeekToPosition) {
        if (mediaId != null && !DataTransform.getInstance().getPathList().isEmpty()) {
            if (mUpdateRunnable == null) {
                mUpdateRunnable = new UpdateRunnable(this);
            }
            mUpdateRunnable.setMediaId(mediaId);
            mUpdateRunnable.setSeekToPosition(shouldSeekToPosition);
            mUpdateRunnable.isPlayWhenPrepared(shouldPlayWhenPrepared);
            mBackgroundPool.submit(mUpdateRunnable);
        } else {
            mPlayBack.stopPlayer();
        }
    }

    private static class UpdateRunnable extends Thread {
        private WeakReference<MediaService> mWeakService;
        private boolean isShouldPlay;
        private int seekToPosition;
        private String mediaId;

        private UpdateRunnable(MediaService service) {
            this.mWeakService = new WeakReference<>(service);
        }

        private void setMediaId(String mediaId) {
            this.mediaId = mediaId;
        }

        private void isPlayWhenPrepared(boolean flag) {
            this.isShouldPlay = flag;
        }

        private void setSeekToPosition(int position) {
            this.seekToPosition = position;
        }

        @Override
        public void run() {
            if (mWeakService.get() == null) {
                return;
            }
            final MediaService mediaService = mWeakService.get();
            final String mediaId = this.mediaId;
            if (mediaId == null || mediaService.mPlayBack == null) {
                return;
            }
            //TODO 设置媒体信息
            MediaMetadataCompat track = DataTransform.getInstance().getMetadataCompatList().get(mediaId);
            //TODO 触发MediaControllerCompat.Callback->onMetadataChanged方法
            if (track != null) {
                mediaService.mMediaSessionCompat.setMetadata(track);
            }
            int index = DataTransform.getInstance().getMediaIndex(mediaId);
            mediaService.mCurrentMedia = mediaService.mQueueItemList.get(index);
            if (seekToPosition > 0) {
                mediaService.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mediaService.mPlayBack.preparedWithMediaId(mediaId);
                        mediaService.mPlayBack.seekTo(seekToPosition, isShouldPlay);
                    }
                });
                Bundle extra = new Bundle();
                extra.putInt(LOCAL_CACHE_POSITION_EVENT, seekToPosition);
                mediaService.mMediaSessionCompat.sendSessionEvent(LOCAL_CACHE_POSITION_EVENT, extra);
            } else {
                mediaService.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isShouldPlay) {
                            mediaService.mPlayBack.playMediaIdAutoStart(mediaId);
                        } else {
                            mediaService.mPlayBack.preparedWithMediaId(mediaId);
                        }
                    }
                });

            }
        }
    }

    /**
     * 更新播放列表
     */
    private static class UpdateQueueRunnable implements Runnable {
        private WeakReference<MediaService> mWeakService;

        private UpdateQueueRunnable(MediaService mWeakService) {
            this.mWeakService = new WeakReference<>(mWeakService);
        }

        @Override
        public void run() {
            if (mWeakService.get() != null) {
                updateQueue(mWeakService.get());
            }
        }

        /**
         * 更新列表
         */
        private void updateQueue(MediaService mService) {
            mService.mMediaItemList = DataTransform.getInstance().getMediaItemList();
            Log.e(TAG, "updateQueue: size ... " + mService.mMediaItemList.size());

            mService.mQueueItemList = DataTransform.getInstance().getQueueItemList();
            mService.mPlayQueueMediaId = DataTransform.getInstance().getMediaIdList();
            mService.mPlayBack.setPlayQueue(mService.mPlayQueueMediaId);
            mService.mMediaSessionCompat.setQueue(mService.mQueueItemList);

            //覆盖本地缓存
            FileUtils.saveObject(DataTransform.getInstance().getMusicInfoEntities(),
                    mService.getCacheDir().getAbsolutePath());
        }
    }

    /**
     * 响应Activity的调用
     * getController.transportControls.
     */
    private class MediaSessionCallBack extends MediaSessionCompat.Callback {
        private long mLastDownTime;
        private long mHeadSetDownTime;

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(TAG, "onPlayFromMediaId() called with: mediaId = [" + mediaId + "]");
            if (extras != null) {
                String action = extras.getString(ACTION_PARAM);
                Log.d(TAG, "onPlayFromMediaId: extra=" + action);
                //TODO 缓冲请求
                if (ACTION_PREPARED_WITH_ID.equals(action)) {
                    String noneMediaId = "-1";
                    if (noneMediaId.equals(mediaId)) {
                        if (mPlayQueueMediaId != null && mPlayQueueMediaId.size() > 0) {
                            onMediaChange(mPlayQueueMediaId.get(0), false);
                        } else {
                            mMediaSessionCompat.sendSessionEvent(ERROR_PLAY_QUEUE_EVENT, null);
                        }
                    } else {
                        onMediaChange(mediaId, false);
                    }
                    //TODO 播放指定id请求
                } else if (ACTION_PLAY_WITH_ID.equals(action)) {
                    //TODO 如果和当前的mediaId相同则视为暂停或播放操作，不同则替换曲目
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if (!Objects.equals(mediaId, mCurrentMedia != null ?
                                mCurrentMedia.getDescription().getMediaId() : null)) {
                            onMediaChange(mediaId, true);
                            return;
                        }
                    } else {
                        if (mCurrentMedia != null && !mediaId.equals(mCurrentMedia.getDescription().getMediaId())) {
                            onMediaChange(mediaId, true);
                            return;
                        }
                    }
                    handlePlayOrPauseRequest();
                    //TODO 初始化播放器，如果本地有播放记录，取播放记录，没有就初始化穿过来的media
                } else if (ACTION_PLAY_INIT.equals(action)) {
                    List<MusicDbEntity> entityRecordList = DataBaseManager.getInstance()
                            .initDb(getApplicationContext()).getEntity();
                    if (entityRecordList.size() > 0) {
                        final int seekPosition = entityRecordList.get(0).getLastPlayedPosition();
                        onMediaChange(entityRecordList.get(0).getLastMediaId(), false, seekPosition);
                    } else {
                        onMediaChange(mediaId, false);
                    }
                } else if (ACTION_SEEK_TO.equals(action)) {
                    int seekPosition = extras.getInt(SEEK_TO_POSITION_PARAM);
                    mPlayBack.seekTo(seekPosition, true);
                    Log.e(TAG, "onPlayFromMediaId: " + seekPosition);
                } else {
                    PrintLog.i("unknown event");
                }
            }
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
            Log.d(TAG, "onPause() called");
            mPlayBack.pause();
            mPlayBack.setIsUserPause(true);
        }

        @Override
        public void onStop() {
            super.onStop();
            handleStopRequest(true);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
            try {
                String mediaId = DataTransform.getInstance().getMediaIdList().get((int) id);
                onMediaChange(mediaId, true);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext() called");
            changeMediaByMode(true, false);
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious() called");
            changeMediaByMode(false, false);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent notificationKeyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (notificationKeyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                Log.w(TAG, "onMediaButtonEvent: up=" + notificationKeyEvent.getAction() +
                        ",code=" + notificationKeyEvent.getKeyCode());
                switch (notificationKeyEvent.getKeyCode()) {
                    //点击下一首
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        changeMediaByMode(true, false);
                        break;
                    //点击关闭
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        handleStopRequest(true);
                        break;
                    //点击上一首（目前没有）
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        changeMediaByMode(false, false);
                        break;
                    //有线耳机 暂支持下一首
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        if (mLastDownTime != notificationKeyEvent.getDownTime()) {
                            mHeadSetDownTime = notificationKeyEvent.getEventTime();
                            mLastDownTime = notificationKeyEvent.getDownTime();
                        }
                        break;
                    //点击播放按钮
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        Log.e(TAG, "onMediaButtonEvent: 点击了播放按钮");
                        handlePlayOrPauseRequest();
                        break;
                    default:
                        break;
                }
            } else {
                //TODO 如果有线耳机上的播放键长按了300毫秒以上，则视为下一首
                if (notificationKeyEvent.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK &&
                        notificationKeyEvent.getAction() == KeyEvent.ACTION_UP) {
                    long secondMis = 300;
                    PrintLog.d("按钮抬起走了这里");
                    if (notificationKeyEvent.getEventTime() - mHeadSetDownTime > secondMis) {
                        changeMediaByMode(true, false);
                        PrintLog.d("之后下一首");
                    } else {
                        PrintLog.d("之后暂停或者播放");
                        handlePlayOrPauseRequest();
                    }
                }
                Log.w(TAG, "onMediaButtonEvent: action=" + notificationKeyEvent.getAction() +
                        ",code=" + notificationKeyEvent.getKeyCode());
            }
            return true;
        }

        @Override
        public void onCommand(String command, Bundle reqExtra, ResultReceiver cb) {
            final Bundle resultExtra = new Bundle();
            switch (command) {
                case COMMAND_POSITION:
                    resultExtra.putInt("position", mPlayBack.getCurrentStreamPosition());
                    if (cb != null) {
                        cb.send(COMMAND_POSITION_CODE, resultExtra);
                    }
                    break;
                case COMMAND_UPDATE_QUEUE:
                    savePlayingRecord();
                    mBackgroundPool.submit(mUpdateQueueRunnable);
                    List<MusicDbEntity> entity = DataBaseManager.getInstance()
                            .initDb(getApplicationContext()).getEntity();
                    if (entity.size() > 0) {
                        String lastMediaId = entity.get(0).getLastMediaId();
                        if (!DataTransform.getInstance().getMediaIdList().contains(lastMediaId)) {
                            onMediaChange(mPlayQueueMediaId.get(0), false);
                        } else {
                            onMediaChange(lastMediaId, false, entity.get(0).getLastPlayedPosition());
                        }
                    } else {
                        onMediaChange(mPlayQueueMediaId.get(0), false);
                    }
                    if (cb != null) {
                        cb.send(COMMAND_UPDATE_QUEUE_CODE, resultExtra);
                    }
                    break;
                //TODO 开始循环获取当前播放位置
                case COMMAND_START_LOOP:
                    startLoop();
                    break;
                //TODO 结束获取当前播放位置
                case COMMAND_STOP_LOOP:
                    stopLoop();
                    break;
                default:
                    PrintLog.print("onCommand no match");
                    super.onCommand(command, reqExtra, cb);
                    break;
            }
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            super.onSetRepeatMode(repeatMode);
            mMediaSessionCompat.setRepeatMode(repeatMode);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            super.onRemoveQueueItem(description);
            Log.d(TAG, "onRemoveQueueItem() called with: description = [" + description + "]");
            removeQueueItemByDes(description);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            switch (action) {
                //TODO 开始倒计时
                case ACTION_COUNT_DOWN_TIME:
                    if (mCountDownTimer != null) {
                        mCountDownTimer.stopTik();
                        mCountDownTimer = null;
                    }
                    mCountDownTimer = new TimerUtils(extras.getLong(ACTION_COUNT_DOWN_TIME), 1000);
                    mCountDownTimer.startTik(timeTikCallBack);
                    break;
                //TODO 结束倒计时
                case ACTION_STOP_COUNT_DOWN:
                    if (mCountDownTimer != null) {
                        mCountDownTimer.stopTik();
                        mCountDownTimer = null;
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
