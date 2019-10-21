package com.dming.fastscanqr

import android.graphics.SurfaceTexture

interface ICamera {
    fun init()
    fun open(surfaceTexture: SurfaceTexture)
    fun surfaceChange(width: Int, height: Int)
    fun close()
    fun release()
}