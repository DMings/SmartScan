package com.dming.fastscanqr

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.dming.fastscanqr.utils.DLog
import com.dming.fastscanqr.utils.ShaderHelper
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class PixelFilter(mContext: Context) : IShader {

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
    private var mTestTexMatrix = FloatArray(16)
    var texH = 1f
    var texW = 1f

    init {
        mIndexSB = ShaderHelper.arrayToShortBuffer(VERTEX_INDEX)
        mPosFB = ShaderHelper.arrayToFloatBuffer(VERTEX_POS)
        mTexFB = ShaderHelper.arrayToFloatBuffer(TEX_VERTEX)
        mProgram = ShaderHelper.loadProgram(mContext, R.raw.process_ver, R.raw.img_frg)
        mPosition = GLES20.glGetAttribLocation(mProgram, "inputPosition")
        mTextureCoordinate = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate")
        mImageOESTexture = GLES20.glGetUniformLocation(mProgram, "inputImageOESTexture")
        uMvpMatrix = GLES20.glGetUniformLocation(mProgram, "inputMatrix")
        uTexMatrix = GLES20.glGetUniformLocation(mProgram, "uTexMatrix")
        Matrix.setIdentityM(mMvpMatrix, 0)
        Matrix.setIdentityM(mTestTexMatrix, 0)
    }

    override fun onChange(imgWidth: Int, imgHeight: Int, width: Int, height: Int) {
//        val imgRatio = 1.0f * imgHeight / imgWidth
//        val ratio = 1.0f * height / width
//        var imgH = 1.0f
//        var imgW = 1.0f
//        var h = 1.0f
//        var w = 1.0f
//        //
//        if (imgRatio >= 1) {
//            imgH = imgRatio
//        } else {
//            imgW = 1.0f / imgRatio
//        }
//        if (ratio >= 1) {
//            h = ratio
//        } else {
//            w = 1.0f / ratio
//        }
//        val texH = imgH / h
//        val texW = imgW / w

//        DLog.i("onChange000 imgWidth: $imgWidth  - imgHeight: $imgHeight >>> width: $width  - height: $height")

        var imgHRatio = 1f
        var imgWRatio = 1f
        if (imgWidth > imgHeight) {
            imgHRatio = 1f * imgHeight / imgWidth
        } else {
            imgWRatio = 1f * imgWidth / imgHeight
        }
//        DLog.i("111 imgHRatio: $imgHRatio  - imgWRatio: $imgWRatio")
        var ratio = 0f
        if (width > height) {
            ratio = 1f * height / width
            texW = imgWRatio * ratio
            texH = imgHRatio
        } else {
            ratio = 1f * width / height
            texH = imgHRatio * ratio
            texW = imgWRatio
        }
//        DLog.i("222 texH: $texH  - texW: $texW ratio:$ratio")
        if (texW > texH) {
            ratio = 1f / texH
            texH = 1f
            texW *= ratio
        } else {
            ratio = 1f / texW
            texW = 1f
            texH *= ratio
        }
//        DLog.i("333 texH: $texH  - texW: $texW")
//        var who = 1
//        if(imgHeight > imgWidth && height > width){
//            val imgRatio = 1f * imgHeight / imgWidth
//            val ratio = 1f * width / height
//            texH = imgRatio * ratio
//            who = 1
//        }else if(imgWidth > imgHeight && width > height){
//            val imgRatio = 1f * imgWidth / imgHeight
//            val ratio = 1f * height / width
//            texW = imgRatio * ratio
//            who = 2
//        } else if(imgHeight > imgWidth && width > height){
//            val imgRatio = 1f * imgHeight / imgWidth
//            val ratio = 1f * width / height
//            texH = ratio * imgRatio
//            who = 3
//        } else {
//            val imgRatio = 1f * imgWidth / imgHeight
//            val ratio = 1f * height / width
//            texW = imgRatio * ratio
//            who = 4
//        }
//        DLog.i("onChange333 texH: $texH  - texW: $texW who: $who")
//        //
//        mPosFB = ShaderHelper.arrayToFloatBuffer(
//            floatArrayOf(
//                -texW, texH, 0f,
//                -texW, -texH, 0f,
//                texW, -texH, 0f,
//                texW, texH, 0f
//            )
//        )
    }

    override fun onDraw(
        textureId: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        texMatrix: FloatArray?
    ) {

//        Matrix.setRotateM(mMvpMatrix,0,90f,0f,0f,1f)
        Matrix.setIdentityM(mMvpMatrix, 0)
        Matrix.scaleM(mMvpMatrix, 0, texW, texH, 1f)

        onDraw(textureId, x, y, width, height, mMvpMatrix, texMatrix)
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(mImageOESTexture, 0)
        GLES20.glViewport(x, y, width, height)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, VERTEX_INDEX.size,
            GLES20.GL_UNSIGNED_SHORT, mIndexSB
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
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
//        val TEX_VERTEX = floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
    }

}
