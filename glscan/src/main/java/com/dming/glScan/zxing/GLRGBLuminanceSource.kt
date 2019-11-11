package com.dming.glScan.zxing

import com.google.zxing.LuminanceSource

import java.nio.ByteBuffer

/**
 * 由zxing  RGBLuminanceSource 改造过来，用于直接解码亮度数据
 */
class GLRGBLuminanceSource(width: Int, height: Int) : LuminanceSource(width, height) {

    private var mLuminance: ByteArray? = null
    private val mDataWidth: Int = width
    private val mDataHeight: Int = height
    private val mLeft: Int = 0
    private val mTop: Int = 0

    init {
        mLuminance = ByteArray(width * height)
    }

    fun setData(pixels: ByteBuffer) {
        var i = 0
        val srcSize = width * height * 4
        var offset = 0
        while (offset < srcSize) {
            mLuminance!![i++] = pixels.get(offset)
            offset += 4
        }
    }

    override fun getRow(y: Int, rowList: ByteArray?): ByteArray {
        var row = rowList
        require(!(y < 0 || y >= height)) { "Requested row is outside the image: $y" }
        val width = width
        if (row == null || row.size < width) {
            row = ByteArray(width)
        }
        val offset = (y + mTop) * mDataWidth + mLeft
        System.arraycopy(mLuminance!!, offset, row, 0, width)
        return row
    }

    override fun getMatrix(): ByteArray {
        val width = width
        val height = height

        // If the caller asks for the entire underlying image, save the copy and give them the
        // original data. The docs specifically warn that result.length must be ignored.
        if (width == mDataWidth && height == mDataHeight) {
            return mLuminance!!
        }

        val area = width * height
        val matrix = ByteArray(area)
        var inputOffset = mTop * mDataWidth + mLeft

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == mDataWidth) {
            System.arraycopy(mLuminance!!, inputOffset, matrix, 0, area)
            return matrix
        }

        // Otherwise copy one cropped row at a time.
        for (y in 0 until height) {
            val outputOffset = y * width
            System.arraycopy(mLuminance!!, inputOffset, matrix, outputOffset, width)
            inputOffset += mDataWidth
        }
        return matrix
    }

    override fun isCropSupported(): Boolean {
        return true
    }

    override fun crop(mLeft: Int, mTop: Int, width: Int, height: Int): LuminanceSource {
        return GLRGBLuminanceSource(
            width,
            height
        )
    }

}
