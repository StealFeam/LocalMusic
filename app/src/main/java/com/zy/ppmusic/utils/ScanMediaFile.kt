package com.zy.ppmusic.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import com.zy.ppmusic.App
import kotlinx.coroutines.*

import java.io.File
import java.util.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RecursiveTask
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

    suspend fun scanInternalMedia(c: Context) = coroutineScope {
        FileUtils.getStoragePath(c, isExternalStorage = false)?.let { scanMediaBySystem(it) }
    }

    suspend fun scanExternalMedia(c: Context) = coroutineScope {
        FileUtils.getStoragePath(c, isExternalStorage = true)?.let { scanMediaBySystem(it) }
    }

    private suspend fun scanMediaBySystem(vararg path: String) = suspendCancellableCoroutine<Void> {
        MediaScannerConnection.scanFile(App.instance, path, null) { _, _ ->
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
        logd("run: 扫描开始")
        if (mInnerStoragePath != null) {
            findFileByForkJoin(File(mInnerStoragePath!!))
            logd("run: 扫描内部存储结束")
        }
        if (mExternalStoragePath != null) {
            findFileByForkJoin(File(mExternalStoragePath!!))
            logd("run: 扫描外部存储结束")
        }
        logd("run: 扫描结束 cost=${System.currentTimeMillis() - startTime}")
        l.onComplete(mPathList)
    }

    private fun findFileByForkJoin(file: File) {
        val task = Task(file)
        ForkJoinPool().execute(task)
        mPathList.addAll(task.join())
    }

    class Task(private val file: File) : RecursiveTask<List<String>>() {

        override fun compute(): List<String> {
            val result = mutableListOf<String>()
            if (file.exists() && file.isDirectory) {
                val childFiles = file.listFiles()
                if (childFiles.isNullOrEmpty()) return result
                val tasks = mutableListOf<Task>()
                for (file in childFiles) {
                    tasks.add(Task(file))
                }
                if (tasks.isNotEmpty()) {
                    for (subTask in invokeAll(tasks)) {
                        try {
                            val subResult = subTask.fork().join()
                            if (subResult.isNotEmpty()) {
                                result.addAll(subResult)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                for (type in SupportMediaType.SUPPORT_TYPE) {
                    if (file.name.endsWith(type)) {
                        val size = 1024L * 1024L
                        if (size < file.length()) {
                            logw(file.absolutePath + ",length=" + file.length())
                            result.add(file.absolutePath)
                        }
                    }
                }
            }
            return result
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
