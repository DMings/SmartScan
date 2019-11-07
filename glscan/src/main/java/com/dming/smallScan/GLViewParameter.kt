package com.dming.smallScan

import android.graphics.drawable.Drawable

/**
 * 定义GLScanView传入参数类
 */
class GLViewParameter {
    var scanWidth: Float = 0f
    var scanHeight: Float = 0f
    var scanPercentWidth: Float = 0f
    var scanPercentHeight: Float = 0f
    var scanTopOffset: Float = 0f
    var scanPercentTopOffset: Float = 0f
    var scanLine: Drawable? = null
    var scanCorner: Drawable? = null
    var scanBackgroundColor: Int = 0
    var addOneDCode: Boolean = false
    var onlyOneDCode: Boolean = false
    var scanMustSquare: Boolean = false
    var scanCornerSize: Float = 0f
    var scanCornerThick: Float = 0f
    var scanLineWidth: Float = 0f
    var scanFrameLineWidth: Float = 0f
    var scanFrameLineColor: Int = 0
    var scanColor: Int = 0
    var disableScale: Boolean = false
    var enableBeep: Boolean = false
    var enableVibrate: Boolean = false
    var enableFlashlightBtn: Boolean = false
}