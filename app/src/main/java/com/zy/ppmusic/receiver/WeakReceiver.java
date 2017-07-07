package com.zy.ppmusic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.zy.ppmusic.MusicInfoEntity;
import com.zy.ppmusic.service.PlayService;

public class WeakReceiver extends BroadcastReceiver {
    private static final String TAG = "WeakReceiver";
    public static String ACTION = "com.zy.ppmusic.receiver.WeakReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: 收到消息");
        if(intent.getAction().equals(ACTION)){
            Log.i(TAG, "onReceive: 启动service");
            MusicInfoEntity musicInfo = intent.getParcelableExtra("musicInfo");
            Intent it = new Intent(context, PlayService.class);
            it.putExtra("musicInfo",musicInfo);
            context.startService(it);
        }
    }
}
