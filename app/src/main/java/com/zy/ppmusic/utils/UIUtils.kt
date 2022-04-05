package com.zy.ppmusic.utils

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.zy.ppmusic.App
import com.zy.ppmusic.R

/**
 * @author stealfeam
 * @date 2017/10/19
 */
fun dp2px(context: Context, dp: Int): Int {
    val density = context.resources.displayMetrics.density
    return (dp * density + 0.5f).toInt()
}
fun px2dp(context: Context, px: Int): Int {
    val density = context.resources.displayMetrics.density
    return (px / density + 0.5f).toInt()
}
fun getString(id: Int): String {
    return App.instance!!.resources.getString(id)
}
fun getColor(id: Int): Int {
    return ContextCompat.getColor(App.instance!!, id)
}

var themeColor: Int = getColor(R.color.colorTheme)

var View.isVisible: Boolean
    set(value) {
        visibility = if (value) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }
    get() = visibility == View.VISIBLE

fun <T> lazy2(initializer: () -> T): Lazy<T> = lazy<T>(LazyThreadSafetyMode.NONE, initializer)

