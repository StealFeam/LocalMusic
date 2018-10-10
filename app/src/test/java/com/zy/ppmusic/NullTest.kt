package com.zy.ppmusic

import org.junit.Test

/**
 * @author y-slience
 * @since 2018/8/20
 */
class NullTest {
    @Test
    fun testNull(){
        var a = ""
        a?.apply {
            println("不为空的")
        }?:apply {
            println("a是为空的")
        }

        a?:return

        if(a == null){
            println("a是空的")
        }
    }
}