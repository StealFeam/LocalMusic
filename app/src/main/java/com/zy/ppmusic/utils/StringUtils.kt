package com.zy.ppmusic.utils

object StringUtils {
    @JvmStatic
    fun ifEmpty(str: CharSequence?): Boolean {
        return str?.isEmpty() != false
    }

    @JvmStatic
    fun ifEmpty(str: String?, emptyReturn: String?): String? =
            if (str == null || str.isEmpty() || "null" == str) {
                emptyReturn
            } else {
                str
            }

    @JvmStatic
    fun ifEquals(str: String?, str1: String?): Boolean = if (ifEmpty(str) || ifEmpty(str1)) {
        false
    } else {
        str == str1
    }
}
