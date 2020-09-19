package com.zy.ppmusic.utils

import android.util.Log
import android.widget.Toast
import com.zy.ppmusic.App
import java.io.File

/**
 * @author stealfeam
 * @since 2018/7/11
 */
fun loge(tag: String, str: String) {
    Log.e(tag, str)
}

fun toast(msg: String) {
    Toast.makeText(App.appBaseContext, msg, Toast.LENGTH_SHORT).show()
}

fun String.isFileExits(): Boolean {
    if (this.isNotEmpty()) {
        return File(this).exists()
    }
    return false
}

typealias Void = Unit
