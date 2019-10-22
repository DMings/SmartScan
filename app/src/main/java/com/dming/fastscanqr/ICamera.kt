package com.dming.fastscanqr

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface

interface ICamera {
    fun init(context: Context)
    fun open(textureId: Int)
    fun surfaceChange(surface: Surface, width: Int, height: Int)
    fun close()
    fun release()
    fun getSurfaceTexture(): SurfaceTexture?
}