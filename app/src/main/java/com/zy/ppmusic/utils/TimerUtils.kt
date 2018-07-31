package com.zy.ppmusic.utils

import android.os.CountDownTimer

import com.zy.ppmusic.callback.TimeTikCallBack

/**
 * @author ZhiTouPC
 * @date 2018/1/19
 */

class TimerUtils
/**
 * @param millisInFuture    The number of millis in the future from the call
 * to [.start] until the countdown is done and [.onFinish]
 * is called.
 * @param countDownInterval The interval along the way to receive
 * [.onTick] callbacks.
 */
(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {
    private var mTikCallBack: TimeTikCallBack? = null

    fun startTik(callBack: TimeTikCallBack) {
        this.mTikCallBack = callBack
        start()
    }

    fun stopTik() {
        cancel()
        this.mTikCallBack = null
    }

    override fun onTick(millisUntilFinished: Long) {
        tik(millisUntilFinished)
    }

    override fun onFinish() {
        tik(0)
    }

    private fun tik(mis: Long) {
        if (mTikCallBack != null) {
            mTikCallBack!!.onTik(mis)
        }
    }
}
