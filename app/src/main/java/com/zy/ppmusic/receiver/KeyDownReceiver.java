package com.zy.ppmusic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by ZhiTouPC on 2017/6/21.
 */

public class KeyDownReceiver extends BroadcastReceiver{
    private static final String TAG = "KeyDownReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)){
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            switch (event.getKeyCode()){
                case KeyEvent.KEYCODE_MEDIA_NEXT:{
                    Log.e(TAG, "onReceive: 下一首");
                }
                case KeyEvent.KEYCODE_MEDIA_PAUSE:{
                    Log.e(TAG, "暂停");
                }
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:{
                    Log.e(TAG, "播放或者暂停");
                }
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:{
                    Log.e(TAG, "上一首");
                }
                case KeyEvent.KEYCODE_VOLUME_DOWN:{
                    Log.e(TAG, "音量减");
                }
                case KeyEvent.KEYCODE_VOLUME_UP:{
                    Log.e(TAG, "音量加");
                }
            }
        }
    }
}
