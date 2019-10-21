package com.dming.fastscanqr

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix

import com.dming.fastscanqr.utils.ShaderHelper

import java.nio.FloatBuffer
import java.nio.ShortBuffer

open class BaseOESFilter(mContext: Context, frgId: Int) : IShader {

    private var mIndexSB: ShortBuffer
    private var mTexFB: FloatBuffer
    private var mPosFB: FloatBuffer

    private var mProgram: Int = 0
    private var mPosition: Int = 0
    private var mTextureCoordinate: Int = 0
    private var mImageOESTexture: Int = 0
    private var uMvpMatrix: Int = 0
    private var uTexMatrix: Int = 0
    private var mMvpMatrix = FloatArray(16)

    init {
        mIndexSB = ShaderHelper.arrayToShortBuffer(VERTEX_INDEX)
        mPosFB = ShaderHelper.arrayToFloatBuffer(VERTEX_POS)
        mTexFB = ShaderHelper.arrayToFloatBuffer(TEX_VERTEX)
        mProgram = ShaderHelper.loadProgram(mContext, R.raw.process_ver, frgId)
        mPosition = GLES20.glGetAttribLocation(mProgram, "inputPosition")
        mTextureCoordinate = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate")
        mImageOESTexture = GLES20.glGetUniformLocation(mProgram, "inputImageOESTexture")
        uMvpMatrix = GLES20.glGetUniformLocation(mProgram, "inputMatrix")
        uTexMatrix = GLES20.glGetUniformLocation(mProgram, "uTexMatrix")
        Matrix.setIdentityM(mMvpMatrix, 0)
    }

    override fun onChange(width: Int, height: Int) {}

    override fun onDraw(
        textureId: Int,
        texMatrix: FloatArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        onDraw(textureId, mMvpMatrix, texMatrix, x, y, width, height)
    }

    override fun onDraw(
        textureId: Int,
        verMatrix: FloatArray,
        texMatrix: FloatArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        GLES20.glUseProgram(mProgram)
        GLES20.glEnableVertexAttribArray(mPosition)
        GLES20.glVertexAttribPointer(
            mPosition, 3,
            GLES20.GL_FLOAT, false, 0, mPosFB
        )
        GLES20.glEnableVertexAttribArray(mTextureCoordinate)
        GLES20.glVertexAttribPointer(
            mTextureCoordinate, 2,
            GLES20.GL_FLOAT, false, 0, mTexFB
        )
        GLES20.glUniformMatrix4fv(uMvpMatrix, 1, false, verMatrix, 0)
        GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(mImageOESTexture, 0)
        GLES20.glViewport(x, y, width, height)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, VERTEX_INDEX.size,
            GLES20.GL_UNSIGNED_SHORT, mIndexSB
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glDisableVertexAttribArray(mPosition)
        GLES20.glDisableVertexAttribArray(mTextureCoordinate)
        GLES20.glUseProgram(0)
    }

    override fun onDestroy() {
        GLES20.glDeleteProgram(mProgram)
    }

    companion object {
        private val VERTEX_INDEX = shortArrayOf(0, 1, 3, 2, 3, 1)
        private val VERTEX_POS =
            floatArrayOf(-1f, 1.0f, 0f, -1f, -1.0f, 0f, 1f, -1.0f, 0f, 1f, 1.0f, 0f)
        val TEX_VERTEX = floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f)
    }

}
