package com.dming.glScan.camera

import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * camera基类
 */
open class BaseCamera {

    protected val mPreviewSizes: MutableList<CameraSize> = ArrayList()
    protected var mViewWidth: Int = 0
    protected var mViewHeight: Int = 0
    protected var mCameraSize: CameraSize =
        CameraSize(0, 0)

    /**
     * 根据旋转角度，找到最合适的尺寸，这里在匹配角度情况下，采用各边长差值绝对值的平方和方式获取
     */
    protected fun dealCameraSize(width: Int, height: Int, rotation: Int) {
        val lessThanView = ArrayList<CameraSize>()
//        DLog.i("dealCameraSize width>  $width height>> $height rotation: $rotation")
        for (size in mPreviewSizes) {
            if (rotation == 90 || rotation == 270) { // normal
                lessThanView.add(CameraSize(size.height, size.width, size))
            } else { // 0 180
                lessThanView.add(CameraSize(size.width, size.height, size))
            }
        }
        var cSize: CameraSize? = null
        var diffMinValue = Int.MAX_VALUE
        for (size in lessThanView) {
            val diffWidth = abs(width - size.width) // 差值绝对值
            val diffHeight = abs(height - size.height) // 差值绝对值
            val diffValue = diffWidth * diffWidth + diffHeight * diffHeight // 平方和
            if (diffValue < diffMinValue) {  // 找出差值最小的数
                diffMinValue = diffValue
                cSize = size
            }
        }
        if (cSize == null) {
            cSize = lessThanView[0]
        }
        mCameraSize =  cSize
    }

}