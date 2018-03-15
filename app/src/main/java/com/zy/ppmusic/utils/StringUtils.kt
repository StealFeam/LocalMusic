package com.zy.ppmusic.utils


class StringUtils {
    companion object {
        private fun ifEmpty(str: CharSequence?): Boolean = str?.isEmpty() ?: true

        fun ifEmpty(str: String?, emptyReturn: String?): String? {
            return if (str == null || str.isEmpty() || "null" == str) {
                emptyReturn
            } else {
                str
            }
        }

        fun ifEquals(str: String?, str1: String?): Boolean {
            return if (ifEmpty(str) || ifEmpty(str1)) {
                false
            } else {
                str == str1
            }
        }
    }

}
