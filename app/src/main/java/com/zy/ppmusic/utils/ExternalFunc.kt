package com.zy.ppmusic.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.zy.ppmusic.App

/**
 * @author y-slience
 * @since 2018/7/11
 */
fun loge(tag: String, str: String) {
    Log.e(tag, str)
}

fun toast(msg: String) {
    Toast.makeText(App.getAppBaseContext(), msg, Toast.LENGTH_SHORT).show()
}