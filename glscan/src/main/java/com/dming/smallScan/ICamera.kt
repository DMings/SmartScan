package com.dming.smallScan

import android.content.Context
import android.graphics.SurfaceTexture

/**
 * 无论 camera1或者2必须实现这个规则
 */
interface ICamera {
    fun init(context: Context)
    fun open(textureId: Int)
    fun surfaceChange(width: Int, height: Int)
    fun close()
    fun release()
    fun getSurfaceTexture(): SurfaceTexture?
    fun getCameraSize(): CameraSize?
    fun setFlashLight(on: Boolean): Boolean
}