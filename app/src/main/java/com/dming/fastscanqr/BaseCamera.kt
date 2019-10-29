package com.dming.fastscanqr

import com.dming.fastscanqr.utils.DLog
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

open class BaseCamera {

    protected val mPreviewSizes: MutableList<CameraSize> = ArrayList()
    protected var viewWidth: Int = 0
    protected var viewHeight: Int = 0
    protected var mCameraSize: CameraSize = CameraSize(0,0)

    protected fun dealCameraSize(width: Int, height: Int, rotation: Int) {
        val lessThanView = ArrayList<CameraSize>()
        DLog.i("getDealCameraSize width>  $width height>> $height rotation: $rotation")
        for (size in mPreviewSizes) {
//            DLog.i("preview size:$size")
            if (rotation == 90 || rotation == 270) { // normal
                lessThanView.add(CameraSize(size.height, size.width, size)) // ?
            } else { // 0 180
                lessThanView.add(CameraSize(size.width, size.height, size))
            }
        }
        var cSize: CameraSize? = null
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
        DLog.i("suitableSize>$cSize")
        mCameraSize =  cSize
    }

}