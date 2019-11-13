package com.dming.glScan

import android.graphics.Rect

/**
 * 扫描窗口变化回调，以下生命周期伴随surface回调而来
 */
interface OnScanViewListener {
    /**
     * 扫描窗口创建
     */
    fun onCreate()
    /**
     * 扫描窗口大小改变
     */
    fun onChange(rect: Rect)
    /**
     * 扫描窗口被销毁
     */
    fun onDestroy()
}