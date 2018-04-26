package com.zy.ppmusic.utils


class StringUtils {
    companion object {
        private fun ifEmpty(str: CharSequence?): Boolean = str?.isEmpty() != false

        fun ifEmpty(str: String?, emptyReturn: String?): String? =
                if (str == null || str.isEmpty() || "null" == str) {
                    emptyReturn
                } else {
                    str
                }

        fun ifEquals(str: String?, str1: String?): Boolean = if (ifEmpty(str) || ifEmpty(str1)) {
            false
        } else {
            str == str1
        }
    }

}
