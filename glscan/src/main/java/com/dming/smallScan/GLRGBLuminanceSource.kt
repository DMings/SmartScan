package com.dming.smallScan

/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.zxing.LuminanceSource

import java.nio.ByteBuffer

/**
 * This class is used to help decode images from files which arrive as RGB data from
 * an ARGB pixel array. It does not support rotation.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Betaminos
 */
class GLRGBLuminanceSource(width: Int, height: Int) : LuminanceSource(width, height) {

    private var luminances: ByteArray? = null
    private val dataWidth: Int = width
    private val dataHeight: Int = height
    private val left: Int = 0
    private val top: Int = 0

    init {
        luminances = ByteArray(width * height)
    }

    fun setData(pixels: ByteBuffer) {
        var i = 0
        val srcSize = width * height * 4
        var offset = 0
        while (offset < srcSize) {
            luminances!![i++] = pixels.get(offset)
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
        val offset = (y + top) * dataWidth + left
        System.arraycopy(luminances!!, offset, row, 0, width)
        return row
    }

    override fun getMatrix(): ByteArray {
        val width = width
        val height = height

        // If the caller asks for the entire underlying image, save the copy and give them the
        // original data. The docs specifically warn that result.length must be ignored.
        if (width == dataWidth && height == dataHeight) {
            return luminances!!
        }

        val area = width * height
        val matrix = ByteArray(area)
        var inputOffset = top * dataWidth + left

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == dataWidth) {
            System.arraycopy(luminances!!, inputOffset, matrix, 0, area)
            return matrix
        }

        // Otherwise copy one cropped row at a time.
        for (y in 0 until height) {
            val outputOffset = y * width
            System.arraycopy(luminances!!, inputOffset, matrix, outputOffset, width)
            inputOffset += dataWidth
        }
        return matrix
    }

    override fun isCropSupported(): Boolean {
        return true
    }

    override fun crop(left: Int, top: Int, width: Int, height: Int): LuminanceSource {
        return GLRGBLuminanceSource(
            width,
            height
        )
    }

}
