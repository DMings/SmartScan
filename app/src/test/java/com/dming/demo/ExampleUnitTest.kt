package com.dming.demo

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
        val imgWidth = 100
        val imgHeight = 300
        val width = 200
        val height = 100
        //
        var texH = 1f
        var texW = 1f
        var who = 1
        if(imgHeight > imgWidth && height > width){
            val imgRatio = 1f * imgHeight / imgWidth
            val ratio = 1f * width / height
            texH = imgRatio * ratio
            who = 1
        }else if(imgWidth > imgHeight && width > height){
            val imgRatio = 1f * imgWidth / imgHeight
            val ratio = 1f * height / width
            texW = imgRatio * ratio
            who = 2
        } else if(imgHeight > imgWidth && width > height){
            val imgRatio = 1f * imgHeight / imgWidth
            val ratio = 1f * width / height
            texH = ratio * imgRatio
            who = 3
        } else {
            val imgRatio = 1f * imgWidth / imgHeight
            val ratio = 1f * height / width
            texW = imgRatio * ratio
            who = 4
        }
        println("onChange333 texH: $texH  - texW: $texW who: $who")
    }
}
