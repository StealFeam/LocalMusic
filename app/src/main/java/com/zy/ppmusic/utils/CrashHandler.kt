package com.zy.ppmusic.utils

import android.content.Context
import android.content.Intent
import android.os.Process

import com.zy.ppmusic.mvp.view.ErrorActivity

/**
 * @author stealfeam
 */
class CrashHandler(private val mContext: Context) : Thread.UncaughtExceptionHandler {

    fun attach() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * Method invoked when the given thread terminates due to the
     * given uncaught exception.
     *
     * Any exception thrown by this method will be ignored by the
     * Java Virtual Machine.
     *
     * @param t the thread
     * @param e the exception
     */
    override fun uncaughtException(t: Thread, e: Throwable?) {
        if (e == null) {
            return
        }
        e.printStackTrace()
        val it = Intent(mContext, ErrorActivity::class.java)
        it.putExtra(ErrorActivity.ERROR_INFO, e)
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        mContext.startActivity(it)
        Process.killProcess(Process.myPid())
    }
}
