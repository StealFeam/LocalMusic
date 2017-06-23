package com.zy.ppmusic.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class PlayService extends Service {
    private static final String TAG = "PlayService";
    public final static int INIT_PLAYER = 0X102;//初始化标志，返回播放信息
    public final static int START_OR_PAUSE = 0X100;
    public final static int PLAY_NEXT = 0X101;

    private List<String> mMusicPath;
    private MediaPlayer player;
    private int currentPosition;
    private boolean isAutoStart = false;

    public PlayService() {

    }

    private static class PlayHandler extends Handler {
        private WeakReference<PlayService> weakReference;
        private Messenger clientMessenger;

        private PlayHandler(PlayService service) {
            weakReference = new WeakReference<PlayService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (weakReference.get() != null) {
                switch (msg.what) {
                    case INIT_PLAYER:
                        weakReference.get().initPlayer(0, 0);
                        break;
                    case START_OR_PAUSE:
                        weakReference.get().startOrPause();
                        break;
                    case PLAY_NEXT:
                        weakReference.get().next();
                        break;
                    default:
                        break;
                }

                Message replyMsg = Message.obtain();
                replyMsg.what = msg.what;
                replyMsg.setData(weakReference.get().getMusicInfo());
                try {
                    if(msg.replyTo != null && clientMessenger == null){
                        clientMessenger = msg.replyTo;
                        msg.replyTo.send(replyMsg);
                    }else{
                        clientMessenger.send(replyMsg);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "handleMessage: 服务器传递消息失败");
                }
            }
            super.handleMessage(msg);
        }
    }

    final Messenger messenger = new Messenger(new PlayHandler(this));

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");
    }


    public Bundle getMusicInfo(){
        Bundle entity = new Bundle();
        entity.putInt("duration",player.getDuration());
        entity.putString("path",mMusicPath.get(currentPosition));
        return entity;
    }

    public void initPlayer(int position, int seekPosition) {
        if (mMusicPath == null || mMusicPath.get(position) == null) {
            Log.d(TAG, "path == null" + mMusicPath + " position " + position);
            return;
        }
        try {
            if (player == null) {
                player = getMediaPlayer(getApplicationContext());
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if (isAutoStart) {
                            mp.start();
                        }
                    }
                });
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Message msg = Message.obtain();
                        msg.what = PLAY_NEXT;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            Log.e(TAG, "自动播放下一首失败--"+e.getMessage() );
                        }
                    }
                });
            }
            currentPosition = position;
            Log.d(TAG, "initPlayer: play path=" + mMusicPath.get(position));
            player.reset();
            player.setDataSource(mMusicPath.get(position));
            player.prepare();
        } catch (IOException e) {
            Log.e(TAG, "initPlayer: error " + e.getMessage());
        }
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

    public void startOrPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.start();
        }
        isAutoStart = true;
    }

    public void next() {
        initPlayer(countPositionInSize(true), 0);
    }

    public void previous() {
        initPlayer(countPositionInSize(false), 0);
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
        Log.e(TAG, "onBind: 收到地址=" + mMusicPath.toString());
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }
}
