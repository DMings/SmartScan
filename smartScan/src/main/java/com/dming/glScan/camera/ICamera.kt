package com.dming.glScan.camera

import android.content.Context
import android.graphics.SurfaceTexture

/**
 * 无论 camera1或者2必须实现这个规则
 */
interface ICamera {
    // 初始化
    fun init(context: Context)
    // 开启摄像头
    fun open(textureId: Int)
    // surface大小改变
    fun surfaceChange(width: Int, height: Int)
    // 摄像头关闭
    fun close()
    // 释放某些资源
    fun release()
    // 获取绘制到的SurfaceTexture
    fun getSurfaceTexture(): SurfaceTexture?
    // 获取摄像头尺寸
    fun getCameraSize(): CameraSize?
    // 控制闪光灯
    fun setFlashLight(on: Boolean): Boolean
}