package com.zy.ppmusic.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import android.util.Log

import com.zy.ppmusic.callback.AudioNoisyCallBack
import com.zy.ppmusic.utils.PrintLog

/**
 * @author stealfeam
 * 当手机来电话或者播放其他的媒体时
 */
class AudioBecomingNoisyReceiver(private val context: Context) : BroadcastReceiver() {
    @Volatile
    private var mIsRegistered = false
    private val mAudioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var mCallBack: AudioNoisyCallBack? = null

    @Synchronized
    fun register(callBack: AudioNoisyCallBack) {
        if (!mIsRegistered) {
            this.mCallBack = callBack
            context.registerReceiver(this, mAudioNoisyIntentFilter)
            mIsRegistered = true
        }
    }

    @Synchronized
    fun unregister() {
        if (mIsRegistered) {
            this.mCallBack = null
            context.unregisterReceiver(this)
            mIsRegistered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        //当收到耳机被拔出时暂停播放
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
            Log.e(TAG, "拔出耳机了")
            mCallBack?.comingNoisy()
        }
    }

    companion object {
        private val TAG = "AudioBecomingReceiver"
    }
}
