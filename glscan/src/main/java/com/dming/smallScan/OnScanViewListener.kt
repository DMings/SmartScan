package com.dming.smallScan

import android.graphics.Rect

/**
 * 扫描窗口变化回调
 */
interface OnScanViewListener {
    fun onCreate()
    fun onChange(rect: Rect)
    fun onDestroy()
}