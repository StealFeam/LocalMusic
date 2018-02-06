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

    private var serviceWeakReference: WeakReference<MediaService>? = null

    init {
        this.serviceWeakReference = WeakReference(service)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (serviceWeakReference!!.get() != null) {
            serviceWeakReference!!.get()?.updatePositionToSession()
        }
    }

}

