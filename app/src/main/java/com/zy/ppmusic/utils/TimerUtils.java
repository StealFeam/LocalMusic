package com.zy.ppmusic.utils;

import android.content.Context;
import android.os.CountDownTimer;

import com.zy.ppmusic.callback.TimeTikCallBack;

/**
 * @author ZhiTouPC
 * @date 2018/1/19
 */

public class TimerUtils extends CountDownTimer{
    private TimeTikCallBack mTikCallBack;
    /**
     * @param millisInFuture    The number of millis in the future from the call
     *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
     *                          is called.
     * @param countDownInterval The interval along the way to receive
     *                          {@link #onTick(long)} callbacks.
     */
    public TimerUtils(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
    }

    public void startTik(TimeTikCallBack callBack){
        this.mTikCallBack = callBack;
        start();
    }

    public void stopTik(){
        cancel();
        this.mTikCallBack = null;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        tik(millisUntilFinished);
    }

    @Override
    public void onFinish() {
        tik(0);
    }

    private void tik(long mis){
        if (mTikCallBack != null) {
            mTikCallBack.onTik(mis);
        }
    }
}
