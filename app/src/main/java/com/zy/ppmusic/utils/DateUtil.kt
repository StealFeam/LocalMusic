package com.zy.ppmusic.utils

import java.util.*

/**
 * @author ZhiTouPC
 */
class DateUtil {

    fun getTime(formatStr: String, mis: Long?): String {
        val h = mis!! / (60L * 60L * 1000L)
        val m = (mis - h * 60L * 60L * 1000L) / (60L * 1000L)
        val s = mis % (60L * 1000L) / 1000L
        return String.format(Locale.CHINA, formatStr, h, m, s)
    }

    fun getTime(mis: Long?): String {
        val h = mis!! / (60L * 60L * 1000L)
        val m = (mis - h * 60L * 60L * 1000L) / (60L * 1000L)
        val s = mis % (60L * 1000L) / 1000L
        return if (h > 0) {
            String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.CHINA, "%02d:%02d", m, s)
        }
    }

    private object UtilInner {
        val instance = DateUtil()
    }

    companion object {
        fun get(): DateUtil {
            return UtilInner.instance
        }
    }

}
