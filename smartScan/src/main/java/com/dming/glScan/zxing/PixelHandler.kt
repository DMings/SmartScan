package com.dming.glScan.zxing

import android.os.Handler
import android.os.Looper
import com.dming.glScan.ScannerLayout
import com.dming.glScan.SmartScanParameter
import java.nio.ByteBuffer

/**
 * 读取GL数据线程的Handler,内部保存解码信息，目的在退出时候不join，同时也不出错
 */
class PixelHandler(looper: Looper) : Handler(looper) {

    var buffer: ByteBuffer? = null
    var source: GLRGBLuminanceSource? = null
    var left: Int = 0
    var top: Int = 0
    var width: Int = 0
    var height: Int = 0

    fun setConfigure(
        smartScanParameter: SmartScanParameter,
        maxWidth: Int,
        maxHeight: Int
    ) {
        val viewConfigure =
            ScannerLayout.getViewConfigure(
                smartScanParameter,
                maxWidth,
                maxHeight
            )
        this.left = viewConfigure.left
        this.top = viewConfigure.top
        this.width = viewConfigure.width()
        this.height = viewConfigure.height()
        this.buffer = ByteBuffer.allocateDirect(this.width * this.height * 4) // RGBA
        this.source = GLRGBLuminanceSource(this.width, this.height)
    }

}