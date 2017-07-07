package com.zy.ppmusic.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.zy.ppmusic.IMusicInterface;
import com.zy.ppmusic.IOnMusicChangeListener;
import com.zy.ppmusic.IPlayerStateChangeListener;
import com.zy.ppmusic.MediaActivity;
import com.zy.ppmusic.MusicInfoEntity;
import com.zy.ppmusic.R;
import com.zy.ppmusic.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class PlayService extends Service {
    private static final String TAG = "PlayService";
    //播放器状态结束
    public final static String LOCAL_NAME = "Local_Path";//本地存储文件目录的名称

    private final static String ACTION = "com.zy.media.service.PlayService";

    private List<String> mMusicPath;
    private MediaPlayer player;
    private int currentPosition;
    private boolean isAutoStart = false;
    private RemoteCallbackList<IPlayerStateChangeListener> callbackList = new RemoteCallbackList<>();
    private RemoteCallbackList<IOnMusicChangeListener> musicChangeList = new RemoteCallbackList<>();
    private int mPlayerState;
    private boolean isServiceStarted = false;
    private PlaybackStateCompat.Builder mStateBuilder;
    private NotificationManagerCompat notificationManagerCompat;

    public PlayService() {

    }

    private Binder binder = new IMusicInterface.Stub() {
        @Override
        public void initPlayer(int position, int seek) throws RemoteException {
            PlayService.this.initPlayer(position,seek);
        }

        @Override
        public void playOrPause() throws RemoteException {
            startOrPause();
        }

        @Override
        public void next() throws RemoteException {
            PlayService.this.next();
        }

        @Override
        public void previous() throws RemoteException {
            PlayService.this.previous();
        }

        @Override
        public com.zy.ppmusic.MusicInfoEntity getMusicInfo() throws RemoteException {
            return PlayService.this.getMusicInfo();
        }

        @Override
        public void registerListener(IPlayerStateChangeListener l) throws RemoteException {
            callbackList.register(l);
        }

        @Override
        public void unregisterListener(IPlayerStateChangeListener l) throws RemoteException {
            callbackList.unregister(l);
        }

        @Override
        public void registerMusicChange(IOnMusicChangeListener l) throws RemoteException {
            musicChangeList.register(l);
        }

        @Override
        public void unregisterMusicChange(IOnMusicChangeListener l) throws RemoteException {
            musicChangeList.unregister(l);
        }
    };
    private MediaSessionCompat sessionCompat;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");

        if (sessionCompat == null) {
            sessionCompat = new MediaSessionCompat(this,TAG);
        }
        mStateBuilder = new PlaybackStateCompat.Builder();
        mStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE);
        sessionCompat.setPlaybackState(mStateBuilder.build());
        sessionCompat.setCallback(new PlaySessionBack());
        sessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS|
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Intent it = new Intent(getApplicationContext(), MediaActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),1,it,PendingIntent.FLAG_UPDATE_CURRENT);
        sessionCompat.setSessionActivity(pendingIntent);


        notificationManagerCompat = NotificationManagerCompat.from(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: "+intent.getAction() );
        KeyEvent event1 = MediaButtonReceiver.handleIntent(sessionCompat, intent);
        Log.d(TAG, "onStartCommand() called");
        if (event1 != null) {
            Log.e(TAG, "onStartCommand: "+event1.getKeyCode());
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private int releaseAudioFocus () {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.v(TAG, "releaseAudioFocus by ");
        return audioManager.abandonAudioFocus(mAfListener);
    }


    private int requestAudioFocus () {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.v(TAG, "requestAudioFocus by ");
        return audioManager.requestAudioFocus(
                mAfListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private AudioManager.OnAudioFocusChangeListener mAfListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.v(TAG, "onAudioFocusChange = " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                if (player.isPlaying()) {
                    player.pause();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (!player.isPlaying()) {
                    player.start();
                }
            }
        }
    };


    public MusicInfoEntity getMusicInfo() {
        int currentPlayPosition = 0;
        if(player != null && player.isPlaying()){
            player.pause();
            currentPlayPosition = player.getCurrentPosition();
        }
        return new MusicInfoEntity(getMusicName(currentPosition),
                player.getDuration(),getMusicPath(currentPosition), currentPlayPosition,currentPosition);
    }

    public String getMusicName(int position) {
        String path = getMusicPath(position);
        if (path != null) {
            return path.substring((path.lastIndexOf("/") + 1), path.lastIndexOf("."));
        } else {
            return null;
        }
    }

    public String getMusicPath(int position) {
        if (position >= 0 && mMusicPath != null && position < mMusicPath.size()) {
            return mMusicPath.get(position);
        } else {
            return null;
        }
    }

    public void initPlayer(int position, int seekPosition) {
        if (mMusicPath == null) {
            mMusicPath = getLocalPath();
            Log.d(TAG, "initPlayer: localPath="+mMusicPath);
        }
        if (mMusicPath.get(position) == null) {
            Log.d(TAG, "path == null" + mMusicPath + " position " + position);
            return;
        }

        try {
            if (player == null) {
                Log.d(TAG, "initPlayer: player start init");
                player = getMediaPlayer(getBaseContext());
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setVolume(1.0f,1.0f);
                Log.d(TAG, "initPlayer: player init complete");
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if (isAutoStart) {
                            Log.d(TAG, "onPrepared: player started");
                            mp.start();
                        }
                        MusicInfoEntity musicInfo = getMusicInfo();
                        buildNotify(musicInfo);
                        notifyMusicChange();
                    }
                });
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        next();
                    }
                });
            }
            currentPosition = position;
            Log.d(TAG, "initPlayer: play path=" + mMusicPath.get(position));
            player.reset();
            player.setDataSource(mMusicPath.get(position));
            player.prepareAsync();
            Log.d(TAG, "initPlayer: prepared complete");
        } catch (IOException e) {
            Log.e(TAG, "initPlayer: error " + e.getMessage());
        }
    }

    private List<String> getLocalPath() {
        Object result = FileUtils.readDataFromFile(getCacheDir().getAbsolutePath()+ File.separator+String.valueOf(LOCAL_NAME.hashCode()));
        if ((result != null) ) {
            return (List<String>) result;
        }
        return null;
    }

    /**
     * 通知音乐播放文件变化
     */
    public void notifyMusicChange(){
        int size = musicChangeList.beginBroadcast();
        for(int i=0;i<size;i++){
            IOnMusicChangeListener l = musicChangeList.getBroadcastItem(i);
            try {
                l.onMusicChange(getMusicInfo());
            } catch (RemoteException e) {
                musicChangeList.finishBroadcast();
                e.printStackTrace();
            }
        }
        musicChangeList.finishBroadcast();
    }

    /**
     * 通知播放器状态变化
     */
    public void notifyPlayerStateChange(){
        long playBackActions = PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        if(isPlay()){
            playBackActions|=PlaybackStateCompat.ACTION_PAUSE;
        }
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        builder.setActions(playBackActions);
        builder.setState(mPlayerState,currentPosition,1.0f, SystemClock.elapsedRealtime());
        sessionCompat.setPlaybackState(builder.build());
//        buildNotify(getMusicInfo());
        int size = callbackList.beginBroadcast();
        for(int i=0;i<size;i++){
            IPlayerStateChangeListener l = callbackList.getBroadcastItem(i);
            try {
                l.onPlayerStateChange(mPlayerState);
            } catch (RemoteException e) {
                callbackList.finishBroadcast();
                e.printStackTrace();
            }
        }
        callbackList.finishBroadcast();
    }

    private MediaPlayer getMediaPlayer(Context context) {
        MediaPlayer mediaplayer = new MediaPlayer();
        try {
            @SuppressLint("PrivateApi") Class<?> cMediaTimeProvider = Class.forName("android.media.MediaTimeProvider");
            @SuppressLint("PrivateApi") Class<?> cSubtitleController = Class.forName("android.media.SubtitleController");

            @SuppressLint("PrivateApi") Class<?> iSubtitleControllerAnchor = Class.forName("android.media.SubtitleController$Anchor");
            @SuppressLint("PrivateApi") Class<?> iSubtitleControllerListener = Class.forName("android.media.SubtitleController$Listener");

            Constructor constructor = cSubtitleController.getConstructor(Context.class, cMediaTimeProvider,
                    iSubtitleControllerListener);

            Object subtitleInstance = constructor.newInstance(context, null, null);

            Field f = cSubtitleController.getDeclaredField("mHandler");

            f.setAccessible(true);
            try {
                f.set(subtitleInstance, new Handler());
            } catch (IllegalAccessException e) {
                return mediaplayer;
            } finally {
                f.setAccessible(false);
            }

            Method setsubtitleanchor = mediaplayer.getClass().getMethod("setSubtitleAnchor", cSubtitleController, iSubtitleControllerAnchor);

            setsubtitleanchor.invoke(mediaplayer, subtitleInstance, null);
            //Log.e("", "subtitle is setted :p");
        } catch (Exception e) {

        }

        return mediaplayer;
    }

    public void buildNotify(MusicInfoEntity entity){
        Log.d(TAG, "buildNotify() called with: entity = [" + entity + "]");
        Notification notification;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        if(!sessionCompat.isActive()){
            sessionCompat.setActive(true);
        }
        NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();
        mediaStyle.setMediaSession(sessionCompat.getSessionToken());
        mediaStyle.setShowActionsInCompactView(2,1,0);
        builder.setStyle(mediaStyle);

        NotificationCompat.Action action = isPlay()
                ? new NotificationCompat.Action(
                R.mipmap.pause, "暂停",
                MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(),
                        PlaybackStateCompat.ACTION_PAUSE))
                : new NotificationCompat.Action(R.mipmap.play, "播放",
                MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(),
                        PlaybackStateCompat.ACTION_PLAY));
        builder.addAction(R.mipmap.previous,"上一首",MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(),
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        builder.addAction(action);
        builder.addAction(R.mipmap.next,"下一首",MediaButtonReceiver.buildMediaButtonPendingIntent(getBaseContext(),
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

//        if(isPlay()){
//            builder.addAction(generateAction(R.mipmap.pause,"暂停",PlaybackStateCompat.ACTION_PAUSE));
//        }
        builder.setContentTitle(entity.getName());
        builder.setContentText(entity.getPath());
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round));
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notification = builder.build();
        notificationManagerCompat.notify(412,notification);
        if(isPlay()){
            startForeground(412,notification);
        }else{
            if(mPlayerState == PlaybackStateCompat.STATE_STOPPED){
                notificationManagerCompat.cancel(412);
            }
            stopForeground(false);
        }
        if(!isServiceStarted){
            startService(new Intent(getApplicationContext(),PlayService.class));
            isServiceStarted = true;
        }
        Log.d(TAG, "buildNotify() called end");
    }


    private NotificationCompat.Action generateAction( int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), PlayService.class);
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action( icon, title, pendingIntent);
    }


    public boolean isPlay() {
        return player != null && player.isPlaying();
    }

    public void startOrPause() {
        if (player == null) {
            mPlayerState = PlaybackStateCompat.STATE_ERROR;
            Log.e(TAG, "startOrPause: player is null");
            notifyPlayerStateChange();
            return;
        }

        if (player.isPlaying()) {
            Log.e(TAG, "startOrPause: stop play");
            player.pause();
            mPlayerState = PlaybackStateCompat.STATE_PAUSED;
        } else {
            Log.e(TAG, "startOrPause: start play");
            player.start();
            if(player.isPlaying()){
                mPlayerState = PlaybackStateCompat.STATE_PLAYING;
            }
        }
        notifyPlayerStateChange();
        isAutoStart = true;
    }

    public void next() {
        initPlayer(countPositionInSize(true), 0);
        isAutoStart = true;
    }

    public void previous() {
        initPlayer(countPositionInSize(false), 0);
        isAutoStart = true;
    }

    /**
     * 控制position的范围
     */
    private int countPositionInSize(boolean isUp) {
        if (isUp) {
            return (1 + currentPosition) >= mMusicPath.size() ? 0 : ++currentPosition;
        } else {
            return (1 - currentPosition) < 0 ? (mMusicPath.size() - 1) : --currentPosition;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mMusicPath = intent.getStringArrayListExtra("paths");
        if (mMusicPath != null) {
            savePathToLocal(mMusicPath);
            Log.e(TAG, "onBind: 收到地址=" + mMusicPath.toString());
        }
        return binder;
    }

    private void savePathToLocal(List<String> mMusicPath) {
        FileUtils.writeDataToFile(mMusicPath,getCacheDir().getAbsolutePath(),String.valueOf(LOCAL_NAME.hashCode()));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called with: intent = [" + intent + "]");
        return false;
    }

    private final class PlaySessionBack extends MediaSessionCompat.Callback{
        @Override
        public void onPlay() {
            super.onPlay();
            Log.e(TAG, "onPlay: called");
            startOrPause();
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {





            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    }

    public final static class SimpleMediaButtonReceiver extends BroadcastReceiver{
        private static final String TAG = "SimpleMediaButtonReceiv";
        public SimpleMediaButtonReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                Log.e(TAG, "onReceive: action="+intent.getAction() );
                KeyEvent event = intent.getParcelableExtra("android.intent.extra.KEY_EVENT");
                Log.e(TAG, "onReceive: keyEvent="+event.toString() );
                switch (event.getKeyCode()){
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        Log.e(TAG, "onReceive: play started");
                        break;
                }
                Intent it = new Intent(ACTION);
                it.setPackage(context.getPackageName());
                PackageManager packageManager = context.getPackageManager();
                List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(it, 0);
                if(!resolveInfos.isEmpty()){
                    ResolveInfo info = resolveInfos.get(0);
                    ComponentName componentName = new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
                    intent.setComponent(componentName);
                    context.startService(intent);
                }

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseAudioFocus();
        Log.d(TAG, "onDestroy() called");
    }
}
