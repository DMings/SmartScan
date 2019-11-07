package com.dming.smallScan

import android.graphics.Rect

/**
 * 扫描view的窗口定位计算
 */
class ScanLayoutLocation {

    companion object {
        fun getViewConfigure(
            t: Float,
            ws: Float,
            hs: Float,
            maxWidth: Int,
            maxHeight: Int,
            scanMustSquare: Boolean
        ): Rect {
            val left: Int
            val top: Int
            var height: Int
            var width: Int
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
            val tt = if (t <= 1) maxHeight * t else t
            width = if (ww > maxWidth) maxWidth else ww
            height = if (hh > maxHeight) maxHeight else hh
            if (scanMustSquare) { // 用最小的边
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