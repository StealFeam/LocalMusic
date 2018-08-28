package com.zy.ppmusic.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zy.ppmusic.service.MediaService
import java.lang.ref.WeakReference

/**
 * @author ZhiTouPC
 * @date 2018/1/11
 * @des 循环接受 LoopService 的广播
 */
class LoopReceiver(service: MediaService) : BroadcastReceiver() {

    private val serviceWeakReference: WeakReference<MediaService>? by lazy {
        WeakReference(service)
    }

    override fun onReceive(context: Context, intent: Intent) {
        serviceWeakReference?.get()?.updatePositionToSession()
    }

}

