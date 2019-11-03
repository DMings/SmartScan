package com.dming.smallScan;

interface IShader {

    fun onChange(imgWidth: Int, imgHeight: Int,width: Int, height: Int)

    fun setScaleMatrix(scale:Float)

    fun onDraw(textureId: Int, x: Int, y: Int, width: Int, height: Int, texMatrix: FloatArray?)

    fun onDraw(
        textureId: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        verMatrix: FloatArray?,
        texMatrix: FloatArray?
    )

    fun onDestroy()
}
