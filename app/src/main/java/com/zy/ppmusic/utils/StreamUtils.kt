package com.zy.ppmusic.utils

import java.io.Closeable
import java.io.IOException

/**
 * @author ZhiTouPC
 */
object StreamUtils {
    @JvmStatic
    fun closeIo(vararg closeable: Closeable?) {
        for (item in closeable) {
            if (item != null) {
                try {
                    item.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }
}