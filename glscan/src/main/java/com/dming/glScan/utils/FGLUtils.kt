package com.dming.glScan.utils

import android.opengl.GLES11Ext
import android.opengl.GLES20

import javax.microedition.khronos.opengles.GL10

/**
 * GL工具类
 */
object FGLUtils {

    /**
     * 创建FBO
     */
    fun createFBO(width: Int, height: Int): IntArray? {
        val mFrameBuffer = IntArray(1)
        val mFrameBufferTexture = IntArray(1)
        GLES20.glGenFramebuffers(1, mFrameBuffer, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0])
        GLES20.glGenTextures(1, mFrameBufferTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTexture[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, mFrameBufferTexture[0], 0
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffer, 0)
            GLES20.glDeleteTextures(1, mFrameBufferTexture, 0)
            DLog.e("create framebuffer failed")
            return null
        }
        //        DLog.i("Java create framebuffer success: (" +
        //                width + ", " + height + "), FB: " + mFrameBuffer[0] + " , Tex: " + mFrameBufferTexture[0]);
        return intArrayOf(mFrameBuffer[0], mFrameBufferTexture[0])
    }

    /**
     * 删除FBO
     */
    fun deleteFBO(ids: IntArray) {
        GLES20.glDeleteFramebuffers(1, intArrayOf(ids[0]), 0)
        GLES20.glDeleteTextures(1, intArrayOf(ids[1]), 0)
    }

    /**
     * 创建OES纹理
     */
    fun createOESTexture(): Int {
        val tex = IntArray(1)
        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0)
        //将此纹理绑定到外部纹理上
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        //设置纹理过滤参数
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        //解除纹理绑定
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return tex[0]
    }

    /**
     * 创建普通纹理
     */
    fun createTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return tex[0]
    }

    /**
     * 删除纹理
     */
    fun deleteTexture(textureId: Int) {
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    /**
     * 检测错误，一般用于debug
     */
    fun glCheckErr() {
        val err = GLES20.glGetError()
        DLog.i("checkErr: $err")
    }

    /**
     * 检测错误，一般用于debug
     */
    fun glCheckErr(tag: String) {
        val err = GLES20.glGetError()
        if (err != 0) {
            DLog.i("$tag > checkErr: $err")
        }
    }

    /**
     * 检测错误，一般用于debug
     */
    fun glCheckErr(tag: Int) {
        val err = GLES20.glGetError()
        DLog.i("$tag > checkErr: $err")
    }

}
