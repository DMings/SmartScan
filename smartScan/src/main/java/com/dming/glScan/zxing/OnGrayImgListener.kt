package com.dming.glScan.zxing

import java.nio.ByteBuffer

/**
 * 亮度图片监听
 */
interface OnGrayImgListener {
    fun onGrayImg(width: Int, height: Int, grayByteBuffer: ByteBuffer)
}