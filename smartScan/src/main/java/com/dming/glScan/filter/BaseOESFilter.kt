package com.dming.glScan.filter

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import com.dming.glScan.R
import com.dming.glScan.utils.ShaderHelper
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 绘制cameraOES数据基类
 */
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
    //
    var texH = 1f
    var texW = 1f

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
    }

    override fun onChange(imgWidth: Int, imgHeight: Int, width: Int, height: Int) {
//        DLog.i("BaseOESFilter 000 imgWidth: $imgWidth  - imgHeight: $imgHeight >>> width: $width  - height: $height")
        var imgHRatio = 1f
        var imgWRatio = 1f
        if (imgWidth > imgHeight) {
            imgHRatio = 1f * imgHeight / imgWidth
        } else {
            imgWRatio = 1f * imgWidth / imgHeight
        }
        texH = imgHRatio
        texW = imgWRatio
//        DLog.i("BaseOESFilter 111 texH: $texH  - texW: $texW ")
        var ratio: Float
        if (width > height) {
            ratio = 1f * height / width
            texW = imgWRatio * ratio
            texH = imgHRatio
        } else {
            ratio = 1f * width / height
            texH = imgHRatio * ratio
            texW = imgWRatio
        }
//        DLog.i("BaseOESFilter 222 texH: $texH  - texW: $texW ratio:$ratio")
        if (texW > texH) {
            ratio = 1f / texH
            texH = 1f
            texW *= ratio
        } else {
            ratio = 1f / texW
            texW = 1f
            texH *= ratio
        }
//        DLog.i("BaseOESFilter 333 texH: $texH  - texW: $texW")
        //
//        Matrix.setIdentityM(mMvpMatrix, 0)
//        Matrix.scaleM(mMvpMatrix, 0, texW, texH, 1f)
    }

    override fun setScaleMatrix(scale: Float) {
        Matrix.setIdentityM(mMvpMatrix, 0)
        Matrix.scaleM(mMvpMatrix, 0, texW, texH, 1f)
        Matrix.scaleM(mMvpMatrix, 0, scale, scale, 1f)
    }

    override fun onDraw(
        textureId: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        texMatrix: FloatArray?
    ) {
        onDraw(textureId, x, y, width, height, mMvpMatrix, texMatrix ?: mMvpMatrix)
    }

    override fun onDraw(
        textureId: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        verMatrix: FloatArray?,
        texMatrix: FloatArray?
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
