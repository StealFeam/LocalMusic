package com.zy.ppmusic.utils

import android.content.Context
import android.support.v4.content.ContextCompat
import com.zy.ppmusic.App

/**
 * @author ZhiTouPC
 * @date 2017/10/19
 */

object UiUtils {
    fun dp2px(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    fun px2dp(context: Context, px: Int): Int {
        val density = context.resources.displayMetrics.density
        return (px / density + 0.5f).toInt()
    }

    fun getString(id: Int): String {
        return App.getInstance().resources.getString(id)
    }

    fun getColor(id: Int): Int {
        return ContextCompat.getColor(App.getInstance(), id)
    }
}
