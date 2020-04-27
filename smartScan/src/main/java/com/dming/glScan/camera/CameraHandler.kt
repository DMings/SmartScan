package com.dming.glScan.camera

import android.os.Handler
import android.os.HandlerThread

/**
 * @author DMing
 * @date 2020/4/27.
 * description:
 */
class CameraHandler {

    companion object {
        val instance: CameraHandler by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CameraHandler()
        }
    }

    private val lock = Object()
    private var isCreate = false
    private var mGLThread: HandlerThread? = null
    private var mGLHandler: Handler? = null

    /**
     * 只能在UI线程中运行
     */
    fun create() {
        synchronized(lock) {
            if (isCreate) { // destroy 尚未调用
                lock.wait() // 等待
            }
        }
        mGLThread = HandlerThread("GL")
        mGLThread?.let {
            it.start()
            mGLHandler = Handler(it.looper)
        }
        isCreate = true
    }

    fun post(runnable: () -> Unit) {
        mGLHandler?.post(runnable)
    }

    /**
     * 只能在UI线程中运行
     */
    fun destroy(runnable: () -> Unit) {
        if (mGLHandler == null) {
            isCreate = false
        } else {
            mGLHandler?.post {
                runnable()
                mGLThread?.quit()
                synchronized(lock) {
                    isCreate = false
                    lock.notify()
                }
            }
        }
    }

}