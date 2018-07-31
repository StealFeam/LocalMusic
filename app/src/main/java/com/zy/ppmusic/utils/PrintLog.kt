package com.zy.ppmusic.utils

import android.util.Log

import com.zy.ppmusic.BuildConfig

import java.util.Locale

/**
 * @author ZhiTouPC
 * @date 2017/12/27
 */

object PrintLog {
    private var mClassName: String? = null
    private var mMethodName: String? = null
    private var mLineNumber: Int = 0
    private var mDetailInfo: String? = null

    private fun generateInfo(elements: Array<StackTraceElement>) {
        mClassName = elements[1].className
        mMethodName = elements[1].methodName
        mLineNumber = elements[1].lineNumber
        mDetailInfo = String.format(Locale.CHINA, "%s : %s : %s \n", mClassName, mMethodName, mLineNumber)
        println("--**--")
    }

    fun print(msg: Any) {
        generateInfo(Throwable().stackTrace)
        println(mDetailInfo!! + msg)
    }

    fun e(msg: String) {
        generateInfo(Throwable().stackTrace)
        Log.e(mClassName, buildLogMsg(msg))
    }

    fun w(msg: String) {
        generateInfo(Throwable().stackTrace)
        Log.w(mClassName, buildLogMsg(msg))
    }

    fun i(msg: String) {
        generateInfo(Throwable().stackTrace)
        Log.i(mClassName, buildLogMsg(msg))
    }

    fun d(msg: String) {
        generateInfo(Throwable().stackTrace)
        Log.d(mClassName, buildLogMsg(msg))
    }

    private fun buildLogMsg(msg: String?): String {
        return if (BuildConfig.DEBUG) {
            String.format(Locale.CHINA, "%s -> %d \n %s", mMethodName, mLineNumber, msg ?: "empty")
        } else {
            "-------------------------------"
        }
    }
}

fun loge(msg:String){
    PrintLog.e(msg)
}

fun logd(msg:String){
    PrintLog.d(msg)
}

fun logi(msg:String){
    PrintLog.i(msg)
}

fun logw(msg:String){
    PrintLog.w(msg)
}
