package com.dming.fastscanqr

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
class GLRGBLuminanceSource : LuminanceSource {

    private var luminances: ByteArray? = null
    private val dataWidth: Int
    private val dataHeight: Int
    private val left: Int
    private val top: Int

    constructor(width: Int, height: Int, pixels: ByteBuffer) : super(width, height) {

        dataWidth = width
        dataHeight = height
        left = 0
        top = 0

        val srcSize = width * height * 4
        var i = 0
        if(luminances == null){
            luminances = ByteArray( width * height)
        }else{
            if(luminances!!.size != width * height){
                luminances = ByteArray( width * height)
            }
        }
        var offset = 0
        while (offset < srcSize) {
            luminances!![i++] = pixels.get(offset)
            offset += 4
        }
    }

    private constructor(
        pixels: ByteArray,
        dataWidth: Int,
        dataHeight: Int,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ) : super(width, height) {
        require(!(left + width > dataWidth || top + height > dataHeight)) { "Crop rectangle does not fit within image data." }
        this.luminances = pixels
        this.dataWidth = dataWidth
        this.dataHeight = dataHeight
        this.left = left
        this.top = top
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        var row = row
        require(!(y < 0 || y >= height)) { "Requested row is outside the image: $y" }
        val width = width
        if (row == null || row.size < width) {
            row = ByteArray(width)
        }
        val offset = (y + top) * dataWidth + left
        System.arraycopy(luminances, offset, row, 0, width)
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
            System.arraycopy(luminances, inputOffset, matrix, 0, area)
            return matrix
        }

        // Otherwise copy one cropped row at a time.
        for (y in 0 until height) {
            val outputOffset = y * width
            System.arraycopy(luminances, inputOffset, matrix, outputOffset, width)
            inputOffset += dataWidth
        }
        return matrix
    }

    override fun isCropSupported(): Boolean {
        return true
    }

    override fun crop(left: Int, top: Int, width: Int, height: Int): LuminanceSource {
        return GLRGBLuminanceSource(
            luminances!!,
            dataWidth,
            dataHeight,
            this.left + left,
            this.top + top,
            width,
            height
        )
    }

}
