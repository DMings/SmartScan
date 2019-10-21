package com.dming.fastscanqr;

interface IShader {

    fun onChange(width: Int, height: Int)

    fun onDraw(textureId: Int, texMatrix: FloatArray, x: Int, y: Int, width: Int, height: Int)

    fun onDraw(
        textureId: Int,
        verMatrix: FloatArray,
        texMatrix: FloatArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )

    fun onDestroy()
}
