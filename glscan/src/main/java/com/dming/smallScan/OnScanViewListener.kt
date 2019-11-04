package com.dming.smallScan

import android.graphics.Rect

interface OnScanViewListener {
    fun onCreate()
    fun onChange(rect: Rect)
    fun onDestroy()
}