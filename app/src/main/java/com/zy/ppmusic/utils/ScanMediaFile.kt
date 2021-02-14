package com.zy.ppmusic.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import com.zy.ppmusic.App
import kotlinx.coroutines.*

import java.io.File
import java.util.*
import kotlin.coroutines.resume

/**
 * 扫描本地音乐文件
 *
 * @author StealFeam
 */
class ScanMediaFile private constructor() {
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
    private val dot = "."

    suspend fun scanInternalMedia(c: Context) = coroutineScope {
        FileUtils.getStoragePath(c, isExternalStorage = false)?.let { scanMediaBySystem(it) }
    }

    suspend fun scanExternalMedia(c: Context) = coroutineScope {
        FileUtils.getStoragePath(c, isExternalStorage = true)?.let { scanMediaBySystem(it) }
    }

    private suspend fun scanMediaBySystem(vararg path: String) = suspendCancellableCoroutine<Void> {
        MediaScannerConnection.scanFile(App.instance!!, path, null) { _, _ ->
            it.resume(Void)
        }
    }

    fun startScan(c: Context, l: OnScanCompleteListener) {
        val context = c.applicationContext
        if (mInnerStoragePath == null) {
            mInnerStoragePath = FileUtils.getStoragePath(context, false)
            mExternalStoragePath = FileUtils.getStoragePath(context, true)
        }
        if (mPathList.size > 0) {
            mPathList.clear()
        }
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "run: 扫描开始")
        if (mInnerStoragePath != null) {
            searchFile(File(mInnerStoragePath!!))
            Log.e(TAG, "run: 扫描内部存储结束")
        }
        if (mExternalStoragePath != null) {
            searchFile(File(mExternalStoragePath!!))
            Log.e(TAG, "run: 扫描外部存储结束")
        }
        Log.d(TAG, "run: 扫描结束 cost=${System.currentTimeMillis() - startTime}")
        l.onComplete(mPathList)
    }

    /**
     * 遍历文件目录下的所有文件
     *
     * @param file 需要扫描的文件目录
     */
    private fun searchFile(file: File) {
        if (file.isDirectory && file.listFiles()?.isNullOrEmpty() == false) {
            // 过滤android系统目录
            if (file.absolutePath.contains("com.android.")) {
                return
            }
            // 过滤隐藏文件
            if (file.isHidden) return
            // 过滤以点开头的文件夹或文件
            if (file.absolutePath.startsWith(dot)) return
            file.listFiles { _, name ->
                Constant.FILTER_DIRS.none { segment -> name.splitToSequence("/").any { it == segment } }
            }?.filter {
                //过滤没有后缀名的文件
                val index = it.name.lastIndexOf(dot)
                val length = it.name.length
                //xxx.x以及xxx.xxxxx格式不支持
                index <= 0 || index > length - 2 || index < length - 4
            }?.forEach {
                searchFile(it)
            }
            return
        }
        println(file.absolutePath)
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
        val instance = ScanMediaFile()
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

        fun get(): ScanMediaFile {
            return ScanInstance.instance
        }
    }
}
