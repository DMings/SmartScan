package com.dming.fastscanqr

import com.dming.fastscanqr.utils.DLog
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

open class BaseCamera {

    protected val mPreviewSizes: MutableList<CameraSize> = ArrayList()
    protected var viewWidth: Int = 0
    protected var viewHeight: Int = 0

    protected fun getDealCameraSize(width: Int, height: Int, rotation: Int): CameraSize {
        val greaterThanView = TreeSet<CameraSize>()
        val lessThanView = ArrayList<CameraSize>()
        DLog.i("getDealCameraSize width>  $width height>> $height")
        for (size in mPreviewSizes) {
            if (rotation == 90 || rotation == 270) { // width > height normal
                if (size.width >= height && size.height >= width) {
                    greaterThanView.add(CameraSize(size.height, size.width, size))
                } else {
                    lessThanView.add(CameraSize(size.height, size.width, size))
                }
            } else { // width < height normal  0 180
                if (size.width >= width && size.height >= height) {
                    greaterThanView.add(CameraSize(size.width, size.height, size))
                } else {
                    lessThanView.add(CameraSize(size.width, size.height, size))
                }
            }
        }
        var cSize: CameraSize? = null
        if (greaterThanView.size > 0) {
            cSize = greaterThanView.first()
        } else {
            var diffMinValue = Float.MAX_VALUE
            for (size in lessThanView) {
                val diffWidth = abs(width - size.width)
                val diffHeight = abs(height - size.height)
                val diffValue = sqrt(0.0f + diffWidth * diffWidth + diffHeight * diffHeight)
                if (diffValue < diffMinValue) {  // 找出差值最小的数
                    diffMinValue = diffValue
                    cSize = size
                }
            }
            if (cSize == null) {
                cSize = lessThanView[0]
            }
        }
        DLog.i("suitableSize>" + cSize!!.toString())
        return cSize
    }

}