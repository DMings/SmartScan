package com.dming.fastscanqr

import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer

class PixelHandler(looper: Looper) : Handler(looper) {

    var buffer: ByteBuffer? = null
    var source: GLRGBLuminanceSource? = null
    var left: Int = 0
    var top: Int = 0
    var height: Int = 0
    var width: Int = 0

    fun setConfigure(
        top: Int,
        size: Int,
        maxWidth: Int,
        maxHeight: Int
    ) {
        if(size == 0)return
        this.top = top
        val minSide = if (maxWidth > maxHeight) maxHeight else maxWidth
        if (size > minSide) {
            this.width = minSide
            this.height = minSide
            this.left = 0
        } else {
            this.width = size
            this.height = size
            this.left = (minSide - size) / 2
        }
        this.buffer = ByteBuffer.allocateDirect(this.width * this.height * 4) // RGBA
        this.source = GLRGBLuminanceSource(this.width, this.height)
    }

}