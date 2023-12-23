package com.zy.ppmusic.utils

import android.os.Environment
import com.zy.ppmusic.App

/**
 * @author stealfeam
 * @date 2018/6/11
 */
object Constant {
    var IS_STARTED = false
    //本地存储通知栏样式选择
    const val LOCAL_CHOOSE_FILE = "LOCAL_CHOOSE_STYLE"
    const val CHOOSE_STYLE_EXTRA = "CHOOSE_STYLE_EXTRA"
    const val LOCAL_STYLE_NAME = "LOCAL_STYLE_ID"
    val CACHE_FILE_PATH = "${App.instance.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}/pathlist.obj"
    const val SP_APP_ATTACH_TIME = "app_attach_time"
}
