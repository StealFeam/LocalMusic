package com.zy.ppmusic.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

import java.io.File
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * 扫描本地音乐文件
 *
 * @author lengs
 */
class ScanMusicFile private constructor() {
    /**
     * 扫描到的音乐路径集合
     */
    private val mPathList = ArrayList<String>()
    /**
     * 内部存储路径
     */
    private var mInnerStoragePath: String? = null
    /**
     * 外部存储路径
     */
    private var mExternalStoragePath: String? = null

    fun startScan(c: Context,l:OnScanCompleteListener) {
        val context = c.applicationContext
        if (mInnerStoragePath == null) {
            mInnerStoragePath = FileUtils.getStoragePath(context, false)
            mExternalStoragePath = FileUtils.getStoragePath(context, true)
        }
        if (mPathList.size > 0) {
            mPathList.clear()
        }
        Log.d(TAG, "run: 扫描开始")
        if (mInnerStoragePath != null) {
            searchFile(File(mInnerStoragePath))
            Log.e(TAG, "run: 扫描内部存储结束")
        }
        if (mExternalStoragePath != null) {
            searchFile(File(mExternalStoragePath))
            Log.e(TAG, "run: 扫描外部存储结束")
        }
        Log.d(TAG, "run: 扫描结束")
        l.onComplete(mPathList)
    }
    private val dot = "."

    /**
     * 遍历文件目录下的所有文件
     *
     * @param file 需要扫描的文件目录
     */
    private fun searchFile(file: File) {
        if (file.isDirectory && file.listFiles() != null) {
            //过滤android系统目录
            if (file.absolutePath.contains("com.android.")) {
                return
            }
            val items = file.listFiles()
            for (item in items) {
                searchFile(item)
            }
            return
        }
        //过滤没有后缀名的文件
        if (!file.name.contains(dot)) {
            return
        }
        val index = file.name.lastIndexOf(dot)
        val length = file.name.length
        //xxx.x以及xxx.xxxxx格式不支持
        if (index > length - 2 || index < length - 4) {
            return
        }
        //判断文件的类型是否支持
        SupportMediaType.SUPPORT_TYPE.forEach {
            if (file.name.endsWith(it)) {
                val size = 1024L * 1024L
                if (size < file.length()) {
                    Log.w(TAG, file.absolutePath + ",length=" + file.length())
                    mPathList.add(file.absolutePath)
                }
            }
        }
    }

    private object ScanInstance {
        val instance = ScanMusicFile()
    }

    interface OnScanCompleteListener {
        /**
         * 扫描完成
         * @param paths 路径集合
         */
        fun onComplete(paths: ArrayList<String>)
    }

    companion object {
        private val TAG = "ScanMusicFile"
        /**
         * 扫描的数量发生了变化
         */
        private val COUNT_CHANGE = 0X001
        /**
         * 扫描完成
         */
        private val SCAN_COMPLETE = 0X000

        fun get(): ScanMusicFile {
            return ScanInstance.instance
        }
    }
}
