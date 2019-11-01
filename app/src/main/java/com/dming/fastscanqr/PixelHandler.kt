package com.dming.fastscanqr

import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer

class PixelHandler(looper: Looper) : Handler(looper) {

    var buffer: ByteBuffer? = null
    var source: GLRGBLuminanceSource? = null
    var left: Int = 0
    var top: Int = 0
    var width: Int = 0
    var height: Int = 0

    fun setConfigure(
        top: Float,
        size: Float,
        maxWidth: Int,
        maxHeight: Int
    ) {
        val viewConfigure = GLCameraManager.getViewConfigure(top, size, maxWidth, maxHeight)
        this.left = viewConfigure.left
        this.top = viewConfigure.top
        this.width = viewConfigure.width()
        this.height = viewConfigure.height()
        this.buffer = ByteBuffer.allocateDirect(this.width * this.height * 4) // RGBA
        this.source = GLRGBLuminanceSource(this.width, this.height)
    }

}