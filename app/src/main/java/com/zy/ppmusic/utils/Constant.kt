package com.zy.ppmusic.utils

import com.zy.ppmusic.App

/**
 * @author y-slience
 * @date 2018/6/11
 */
object Constant {
    var IS_STARTED = false
    //本地存储通知栏样式选择
    const val LOCAL_CHOOSE_FILE = "LOCAL_CHOOSE_STYLE"
    const val CHOOSE_STYLE_EXTRA = "CHOOSE_STYLE_EXTRA"
    const val LOCAL_STYLE_NAME = "LOCAL_STYLE_ID"
    val CACHE_FILE_PATH = "${App.getInstance().cacheDir.absolutePath}/pathlist.obj"
}