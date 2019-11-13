package com.dming.glScan

import android.graphics.Rect

/**
 * 扫描view的窗口定位计算
 */
class ScannerLayout {

    companion object {
        fun getViewConfigure(
            smartScanParameter: SmartScanParameter,
            maxWidth: Int,
            maxHeight: Int
        ): Rect {
            val left: Int
            val top: Int
            var height: Int
            var width: Int
            val ts = if (smartScanParameter.scanTopOffset > 0) smartScanParameter.scanTopOffset else
                smartScanParameter.scanPercentTopOffset
            val ws = if (smartScanParameter.scanWidth > 0) smartScanParameter.scanWidth else
                smartScanParameter.scanPercentWidth
            val hs = if (smartScanParameter.scanHeight > 0) smartScanParameter.scanHeight else
                smartScanParameter.scanPercentHeight

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
            if (smartScanParameter.scanMustSquare == true) { // 用最小的边
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