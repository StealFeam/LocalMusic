package com.zy.ppmusic.utils


class StringUtils {
    companion object {
        fun ifEmpty(str: CharSequence?): CharSequence? {
            return ifEmpty(str, "unknown")
        }

        fun ifEmpty(str: CharSequence?, emptyReturn: CharSequence?): CharSequence? {
            return if (str == null || str.isEmpty()) {
                emptyReturn
            } else {
                str
            }
        }
    }
}