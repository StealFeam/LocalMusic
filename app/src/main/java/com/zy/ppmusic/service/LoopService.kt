package com.zy.ppmusic.service

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.zy.ppmusic.utils.PrintLog

/**
 * @author stealfeam
 * @date 2017/12/28
 */
class LoopService : IntentService(TAG) {
    private var loopStart = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PrintLog.i("loop start ... ")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent?) {
        PrintLog.e("onHandleIntent....." + (intent?.toString() ?: "intent为空"))
        while (loopStart) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                    Intent(ACTION))
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
    }

    override fun stopService(name: Intent): Boolean {
        loopStart = false
        PrintLog.e("stopService....." + name.toString())
        return super.stopService(name)
    }

    override fun onDestroy() {
        if (loopStart) {
            loopStart = false
            stopSelf()
        }
        super.onDestroy()
        PrintLog.i("loop destroy ... $loopStart")
    }

    companion object {
        private const val TAG = "LoopService"
        val ACTION: String = LoopService::class.java.simpleName
    }
}
