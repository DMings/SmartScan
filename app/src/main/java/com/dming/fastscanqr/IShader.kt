package com.dming.fastscanqr;

interface IShader {

    fun onChange(width: Int, height: Int)

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
