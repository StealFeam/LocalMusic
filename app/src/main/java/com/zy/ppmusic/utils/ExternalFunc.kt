package com.zy.ppmusic.utils

import android.content.Context
import android.util.Log
import android.widget.Toast

/**
 * @author y-slience
 * @since 2018/7/11
 */
fun loge(tag: String, str: String) {
    Log.e(tag, str)
}

fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}