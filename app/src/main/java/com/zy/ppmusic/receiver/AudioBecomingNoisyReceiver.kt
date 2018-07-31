package com.zy.ppmusic.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log

import com.zy.ppmusic.callback.AudioNoisyCallBack

/**
 * @author ZhiTouPC
 * 当手机来电话或者播放其他的媒体时
 */
class AudioBecomingNoisyReceiver(context: Context) : BroadcastReceiver() {
    private val mContext: Context = context.applicationContext
    private var mIsRegistered = false
    private val mAudioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var mCallBack: AudioNoisyCallBack? = null

    fun register(callBack: AudioNoisyCallBack) {
        if (!mIsRegistered) {
            this.mCallBack = callBack
            mContext.registerReceiver(this, mAudioNoisyIntentFilter)
            mIsRegistered = true
        }
    }

    fun unregister() {
        if (mIsRegistered) {
            this.mCallBack = null
            mContext.unregisterReceiver(this)
            mIsRegistered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        //当收到耳机被拔出时暂停播放
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
            Log.e(TAG, "拔出耳机了")
            if (mCallBack != null) {
                mCallBack!!.comingNoisy()
            }
        }
    }

    companion object {
        private val TAG = "AudioBecomingReceiver"
    }
}