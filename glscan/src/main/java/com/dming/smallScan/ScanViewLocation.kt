package com.dming.smallScan

import android.graphics.Rect

/**
 * 扫描view的窗口定位计算
 */
class ScanViewLocation {

    companion object {
        fun getViewConfigure(
            glScanParameter: GLScanParameter,
            maxWidth: Int,
            maxHeight: Int
        ): Rect {
            val left: Int
            val top: Int
            var height: Int
            var width: Int
            val ts = if (glScanParameter.scanTopOffset > 0) glScanParameter.scanTopOffset else
                glScanParameter.scanPercentTopOffset
            val ws = if (glScanParameter.scanWidth > 0) glScanParameter.scanWidth else
                glScanParameter.scanPercentWidth
            val hs = if (glScanParameter.scanHeight > 0) glScanParameter.scanHeight else
                glScanParameter.scanPercentHeight

            val ww = if (ws == 0f) {
                maxWidth
            } else {
                (if (ws <= 1) maxWidth * ws else ws).toInt()
            }
            val hh = if (hs == 0f) {
                maxHeight
            } else {
                (if (hs <= 1) maxHeight * hs else hs).toInt()
            }
            val tt = if (ts <= 1) maxHeight * ts else ts
            width = if (ww > maxWidth) maxWidth else ww
            height = if (hh > maxHeight) maxHeight else hh
            if (glScanParameter.scanMustSquare) { // 用最小的边
                if (width > height) {
                    width = height
                } else {
                    height = width
                }
            }
            left = (maxWidth - width) / 2
            top = if (maxWidth > maxHeight) { // 横屏  高度的居中
                (maxHeight - height) / 2
            } else {
                if (tt + height > maxHeight) maxHeight - height else tt.toInt()
            }
            return Rect(left, top, left + width, top + height)
        }
    }
}