package com.zy.ppmusic.utils

import android.os.AsyncTask
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Runnable
import kotlin.coroutines.experimental.CoroutineContext

/**
 * @author y-slience
 * @since 2018/6/29
 */
object TaskPool :CoroutineDispatcher(){
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(block)
    }

    fun execute(runnable: Runnable){
        AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable)
    }
}