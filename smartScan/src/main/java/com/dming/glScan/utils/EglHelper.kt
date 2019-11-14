package com.dming.glScan.utils

import android.opengl.EGL14
import android.view.Surface
import javax.microedition.khronos.egl.*

/**
 * EGL环境帮助类
 */
class EglHelper {
    private var mEgl: EGL10? = null
    private var mEglDisplay: EGLDisplay? = null
    var eglContext: EGLContext? = null
        private set
    private var mEglSurface: EGLSurface? = null

    private val mRedSize = 8
    private val mGreenSize = 8
    private val mBlueSize = 8
    private val mAlphaSize = 8
    private val mDepthSize = 8
    private val mStencilSize = 8
    private val mRenderType = 4


    /**
     * 初始化EGL环境
     */
    fun initEgl(eglContext: EGLContext?, surface: Surface?) {
        //1. 得到Egl实例
        mEgl = EGLContext.getEGL() as EGL10
        mEgl?.let { egl ->

            //2. 得到默认的显示设备（就是窗口）
            mEglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
                throw RuntimeException("eglGetDisplay failed")
            }
            //3. 初始化默认显示设备
            val version = IntArray(2)
            if (!egl.eglInitialize(mEglDisplay, version)) {
                throw RuntimeException("eglInitialize failed")
            }

            //4. 设置显示设备的属性
            val attributeList = intArrayOf(
                EGL10.EGL_RED_SIZE,
                mRedSize,
                EGL10.EGL_GREEN_SIZE,
                mGreenSize,
                EGL10.EGL_BLUE_SIZE,
                mBlueSize,
                EGL10.EGL_ALPHA_SIZE,
                mAlphaSize,
                EGL10.EGL_DEPTH_SIZE,
                mDepthSize,
                EGL10.EGL_STENCIL_SIZE,
                mStencilSize,
                EGL10.EGL_RENDERABLE_TYPE,
                mRenderType, //egl版本  2.0
                EGL10.EGL_NONE
            )


            val numConfig = IntArray(1)
            require(
                egl.eglChooseConfig(
                    mEglDisplay, attributeList, null, 1,
                    numConfig
                )
            ) { "eglChooseConfig failed" }
            val numConfigs = numConfig[0]
            require(numConfigs > 0) { "No configs match configSpec" }

            //5. 从系统中获取对应属性的配置
//        val configs = arrayOfNulls<EGLConfig>(numConfigs)
            val configs = Array<EGLConfig>(numConfigs) {
                val e = object : EGLConfig() {}
                e
            }
            require(
                egl.eglChooseConfig(
                    mEglDisplay, attributeList, configs, numConfigs,
                    numConfig
                )
            ) { "eglChooseConfig#2 failed" }
//        require(
//            configs as? Array<EGLConfig> != null
//        ) {
//            "configs wrong"
//        }
            var eglConfig = chooseConfig(egl, mEglDisplay, configs)
            if (eglConfig == null) {
                eglConfig = configs[0]
            }

            //6. 创建EglContext
            val contextAttr = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
            if (eglContext == null) {
                this.eglContext =
                    egl.eglCreateContext(mEglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttr)
            } else {
                this.eglContext =
                    egl.eglCreateContext(mEglDisplay, eglConfig, eglContext, contextAttr)
            }

            //7. 创建渲染的Surface
            mEglSurface = if (surface == null) {
                EGL10.EGL_NO_SURFACE
            } else {
                egl.eglCreateWindowSurface(mEglDisplay, eglConfig, surface, null)
            }
            //8. 绑定EglContext和Surface到显示设备中
            if (mEglDisplay != null && mEglSurface != null && this.eglContext != null) {
                if (!egl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, this.eglContext)) {
                    throw RuntimeException("eglMakeCurrent fail")
                }
            }
        }
    }

    //9. 刷新数据，显示渲染场景
    fun swapBuffers(): Boolean {
        return if (mEgl != null) {
            mEgl?.eglSwapBuffers(mEglDisplay, mEglSurface)
            true
        } else {
            false
        }
    }

    /**
     * 释放EGL
     */
    fun destroyEgl() {
        mEgl?.let { egl ->
            if (mEglSurface != null && mEglSurface !== EGL10.EGL_NO_SURFACE) {
                egl.eglMakeCurrent(
                    mEglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT
                )

                egl.eglDestroySurface(mEglDisplay, mEglSurface)
                mEglSurface = null
            }
            if (eglContext != null) {
                egl.eglDestroyContext(mEglDisplay, eglContext)
                eglContext = null
            }
            if (mEglDisplay != null) {
                egl.eglTerminate(mEglDisplay)
                mEglDisplay = null
            }
            mEgl = null
        }
    }

    private fun chooseConfig(
        egl: EGL10, display: EGLDisplay?,
        configs: Array<EGLConfig>
    ): EGLConfig? {
        for (config in configs) {
            val d = findConfigAttribute(
                egl, display, config,
                EGL10.EGL_DEPTH_SIZE
            )
            val s = findConfigAttribute(
                egl, display, config,
                EGL10.EGL_STENCIL_SIZE
            )
            if (d >= mDepthSize && s >= mStencilSize) {
                val r = findConfigAttribute(
                    egl, display, config,
                    EGL10.EGL_RED_SIZE
                )
                val g = findConfigAttribute(
                    egl, display, config,
                    EGL10.EGL_GREEN_SIZE
                )
                val b = findConfigAttribute(
                    egl, display, config,
                    EGL10.EGL_BLUE_SIZE
                )
                val a = findConfigAttribute(
                    egl, display, config,
                    EGL10.EGL_ALPHA_SIZE
                )
                if (r == mRedSize && g == mGreenSize
                    && b == mBlueSize && a == mAlphaSize
                ) {
                    return config
                }
            }
        }
        return null
    }

    private fun findConfigAttribute(
        egl: EGL10, display: EGLDisplay?,
        config: EGLConfig, attribute: Int, defaultValue: Int = 0
    ): Int {
        val value = IntArray(1)
        return if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
            value[0]
        } else defaultValue
    }
}