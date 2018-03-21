package com.zy.ppmusic

import org.junit.Test
import java.nio.charset.Charset
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    private var currentPosition = 3
    private var index = 4
    @Test
    fun addition_isCorrect() {
        println(countPositionInSize(true))
    }

    @Test
    fun testMapValue() {
        val list = ArrayList<String>()
        list.add("ssss")
        val s = String("ssss".toByteArray(Charset.forName("UTF-8")))

        println("index=" + list.indexOf(s))
    }


    @Test
    fun testTime() {
        val time = 7195992L
        val h = time/(60L * 60L * 1000L)
        val m = (time/(60L * 1000L)) - h * 60L
        val s = (time%60000L)/1000L
        println(String.format(Locale.CHINA, "%dh:%2dm:%2ds", h, m, s))
    }

    private fun countPositionInSize(isUp: Boolean): Int {
        return if (isUp) {
            if ((currentPosition + 1) >= index) 0 else ++currentPosition
        } else {
            if ((currentPosition - 1) < 0) index - 1 else --currentPosition
        }
    }


}
