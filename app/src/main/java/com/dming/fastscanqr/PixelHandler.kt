package com.dming.fastscanqr

import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer

class PixelHandler(looper: Looper) : Handler(looper) {

    var buffer: ByteBuffer? = null
    var height: Int = 0
    var width: Int = 0

    fun setByteBuffer(width: Int, height: Int) {
        buffer = ByteBuffer.allocateDirect(width * height * 4)
    }

}