package com.zy.ppmusic

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    var currentPosition = 3
    var index = 4
    @Test
    fun addition_isCorrect() {
        println(countPositionInSize(true))
    }

    private fun countPositionInSize(isUp: Boolean): Int {
        if (isUp) {
            return if ((currentPosition+1) >= index) 0 else ++currentPosition
        } else {
            return if ((currentPosition-1) < 0) index - 1 else --currentPosition
        }
    }
}
