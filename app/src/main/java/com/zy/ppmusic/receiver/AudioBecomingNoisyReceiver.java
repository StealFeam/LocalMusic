package com.zy.ppmusic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import com.zy.ppmusic.callback.AudioNoisyCallBack;

/**
 * @author ZhiTouPC
 * 当手机来电话或者播放其他的媒体时
 */
public class AudioBecomingNoisyReceiver extends BroadcastReceiver {
    private static final String TAG = "AudioBecomingRecei";
    private final Context context;
    private boolean mIsRegistered = false;
    private IntentFilter mAudioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private AudioNoisyCallBack mCallBack;

    public AudioBecomingNoisyReceiver(Context context) {
        this.context = context.getApplicationContext();
    }

    public void register(AudioNoisyCallBack callBack) {
        if (!mIsRegistered) {
            this.mCallBack = callBack;
            context.registerReceiver(this, mAudioNoisyIntentFilter);
            mIsRegistered = true;
        }
    }

    public void unregister() {
        if (mIsRegistered) {
            this.mCallBack = null;
            context.unregisterReceiver(this);
            mIsRegistered = false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //当收到耳机被拔出时暂停播放
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            Log.e(TAG, "拔出耳机了");
            if (mCallBack != null) {
                mCallBack.comingNoisy();
            }
        }
    }
}