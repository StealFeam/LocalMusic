package com.zy.ppmusic.utils

import android.os.AsyncTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * @author stealfeam
 * @since 2018/6/29
 */
object TaskPool : CoroutineDispatcher(){
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(block)
    }

    fun execute(runnable: Runnable){
        AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable)
    }

    fun executeSyc(runnable: Runnable){
        AsyncTask.SERIAL_EXECUTOR.execute(runnable)
    }
}