package com.zy.ppmusic.utils

import android.os.AsyncTask

/**
 * @author stealfeam
 * @since 2018/6/29
 */
object TaskPool{
    fun executeSyc(runnable: Runnable){
        AsyncTask.SERIAL_EXECUTOR.execute(runnable)
    }
}