package com.dming.glScan

import android.graphics.Rect

/**
 * 扫描窗口变化回调，以下生命周期伴随surface回调而来
 */
interface OnScanViewListener {
    /**
     * View创建
     */
    fun onCreate()
    /**
     * View大小该表
     */
    fun onChange(rect: Rect)
    /**
     * View被移除
     */
    fun onDestroy()
}