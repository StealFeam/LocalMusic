package com.zy.ppmusic.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.zy.ppmusic.mvp.view.MediaActivity;
import com.zy.ppmusic.utils.PrintOut;

/**
 * @author ZhiTouPC
 * @date 2017/12/28
 */

public class LoopService extends IntentService{
    private static final String TAG = "LoopService";
    public static final String ACTION = LoopService.class.getSimpleName();

    public LoopService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
        PrintOut.i("loop start ... ");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        for(;;){
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                    new Intent(MediaActivity.LoopReceiver.Companion.getACTION()));
            try {
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PrintOut.i("loop stop ... ");
    }
}
