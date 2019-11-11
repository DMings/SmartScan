package com.dming.glScan.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import com.dming.glScan.SmartScanParameter
import com.dming.glScan.filter.IShader
import com.dming.glScan.filter.LuminanceFilter
import com.dming.glScan.filter.PixelFilter
import com.dming.glScan.filter.PreviewFilter
import com.dming.glScan.utils.EglHelper
import com.dming.glScan.utils.FGLUtils
import com.dming.glScan.zxing.GLRGBLuminanceSource
import com.dming.glScan.zxing.PixelHandler
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock

/**
 * 控制camera绘制到GL的管理类
 */
class GLCameraManager {
    private val mCamera = Camera1()
    private val mCameraMatrix = FloatArray(16)
    // GL绘制线程
    private lateinit var mGLThread: HandlerThread
    private lateinit var mGLHandler: Handler
    private lateinit var mPreviewFilter: IShader
    private lateinit var mLuminanceFilter: IShader
    private var mTextureId: Int = 0
    private val mEglHelper = EglHelper()
    // 像素读取，解码线程
    private lateinit var mPixelThread: HandlerThread
    private lateinit var mPixelHandler: PixelHandler
    private var mIsPixelCreate = false
    private val mPixelLock = ReentrantLock()
    //
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    //
    private val mPixelEglHelper = EglHelper()

    private var mPixelSurface: Surface? = null
    private var mPixelSurfaceTexture: SurfaceTexture? = null
    //
    private var mFrameIds: IntArray? = null
    private var mPixelTexture = -1
    private lateinit var mPixelFilter: IShader
    //
    private var mScale: Float = 1.0f

    private var onReadScanData: ((
        width: Int, height: Int,
        source: GLRGBLuminanceSource, grayByteBuffer: ByteBuffer
    ) -> Unit)? = null

    /**
     * 初始化GL绘制线程、scan读取像素解码线程，
     * 获取摄像头信息
     */
    fun init(context: Context) {
        mGLThread = HandlerThread("GL")
        mPixelThread = HandlerThread("SCAN")
        mGLThread.start()
        mGLHandler = Handler(mGLThread.looper)
        mPixelThread.start()
        mPixelHandler = PixelHandler(mPixelThread.looper)
        mGLHandler.post {
            mCamera.init(context)
        }
    }

    /**
     * surface创建，创建EGL环境、OES纹理，打开摄像头，设置纹理、监听器，
     * 当数据来的时候获取旋转矩阵，用于矫正纹理旋转角度；图像数据来到分两步绘制，
     * 第一步以亮度的方式绘制到FBO中，第二步以正常的方式绘制到预览界面；
     * 在解码线程中，绘制FBO中的纹理数据，然后再读取像素数据（这里有种方式是直接
     * 读取FBO纹理中的数据，我也尝试过读取，但是实际上速度却比绘制后读取更慢，我也不知道为什么）
     */
    fun surfaceCreated(context: Context, holder: SurfaceHolder?) {
        mGLHandler.post {
            mEglHelper.initEgl(null, holder!!.surface)
            mTextureId = FGLUtils.createOESTexture()
            mPreviewFilter = PreviewFilter(context)
            mLuminanceFilter = LuminanceFilter(context)
            mCamera.open(mTextureId)
            mCamera.getSurfaceTexture()?.setOnFrameAvailableListener {
                it.getTransformMatrix(mCameraMatrix)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                mFrameIds?.let { frameIds ->
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameIds[0])
                    mLuminanceFilter.setScaleMatrix(mScale)
                    mLuminanceFilter.onDraw(mTextureId, 0, 0, mWidth, mHeight, mCameraMatrix)
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                }
                //
                mPreviewFilter.setScaleMatrix(mScale)
                mPreviewFilter.onDraw(mTextureId, 0, 0, mWidth, mHeight, mCameraMatrix)
                mEglHelper.swapBuffers()
                it.updateTexImage()
                //
                readScanDataFromGL()
            }
            mPixelHandler.post {
                mPixelTexture = FGLUtils.createOESTexture()
                mPixelSurfaceTexture = SurfaceTexture(mPixelTexture)
                mPixelSurface = Surface(mPixelSurfaceTexture)
                mPixelEglHelper.initEgl(mEglHelper.eglContext, mPixelSurface)
                mPixelFilter = PixelFilter(context)
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
                mPixelSurfaceTexture?.setOnFrameAvailableListener {
                    it.updateTexImage()
                }
            }
        }
    }

    /**
     * surface大小改变，重置scale比例，重新创建FBO，配置GL着色器
     */
    fun onSurfaceChanged(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        mScale = 1.0f
        mGLHandler.post {
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            mFrameIds?.let {
                FGLUtils.deleteFBO(it)
            }
            mFrameIds = FGLUtils.createFBO(width, height)
            //
            mCamera.surfaceChange(width, height)
            val cameraSize = mCamera.getCameraSize()
//            DLog.i("cameraSize  width: ${cameraSize.width} height: ${cameraSize.height}")
            mPreviewFilter.onChange(cameraSize.width, cameraSize.height, width, height)
            mLuminanceFilter.onChange(cameraSize.width, cameraSize.height, width, height)
            //
            mPixelHandler.post {
                mPixelFilter.onChange(cameraSize.width, cameraSize.height, mWidth, mHeight)
                mPixelSurfaceTexture?.setDefaultBufferSize(mWidth, mHeight)
                GLES20.glViewport(0, 0, mWidth, mHeight)
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                mIsPixelCreate = true
            }
        }
    }

    /**
     * surface关闭，释放资源，删除纹理
     */
    fun surfaceDestroyed() {
        mIsPixelCreate = false
        mPixelHandler.buffer = null
        mPixelSurfaceTexture?.setOnFrameAvailableListener(null)
        mPixelHandler.post {
            FGLUtils.deleteTexture(mPixelTexture)
            mPixelFilter.onDestroy()
            mPixelEglHelper.destroyEgl()
            mPixelSurfaceTexture?.release()
            mPixelSurface?.release()
        }
        mGLHandler.post {
            mFrameIds?.let {
                FGLUtils.deleteFBO(it)
            }
            mFrameIds = null
            FGLUtils.deleteTexture(mTextureId)
            mCamera.close()
            mPreviewFilter.onDestroy()
            mLuminanceFilter.onDestroy()
            mEglHelper.destroyEgl()
        }
    }

    /**
     * 资源释放，camera关闭，GL线程退出，解码线程推出
     */
    fun destroy() {
        mCamera.release()
        mGLThread.quit()
        mPixelThread.quit()
        mGLThread.join()
//        mPixelThread.join()
    }

    /**
     * 缩放变化调用
     */
    fun onScaleChange(scale: Float) {
        mScale *= scale
        if (mScale < 1.0f) {
            mScale = 1.0f
        } else if (mScale > 3.0f) {
            mScale = 3.0f
        }
    }

    /**
     * 扫描配置变化调用
     */
    fun changeScanConfigure(smartScanParameter: SmartScanParameter) {
        mPixelHandler.post {
            mPixelHandler.setConfigure(smartScanParameter, mWidth, mHeight)
        }
    }

    /**
     * 从GL读取预览框数据
     */
    private fun readScanDataFromGL() {
        mPixelLock.tryLock()
        mPixelHandler.post {
            if (mIsPixelCreate) {
                if (mFrameIds != null) {
                    mPixelFilter.onDraw(mFrameIds!![1], 0, 0, mWidth, mHeight, null)
                }
                mPixelEglHelper.swapBuffers()
                //
                try {
                    mPixelLock.lock()
                    if (mPixelHandler.width != 0 && mPixelHandler.height != 0) {
                        mPixelHandler.buffer?.let { byteBuffer ->
                            //                        val start = System.currentTimeMillis()
                            byteBuffer.position(0)
                            GLES20.glReadPixels(
                                mPixelHandler.left,
                                mPixelHandler.top,
                                mPixelHandler.width,
                                mPixelHandler.height,
                                GLES20.GL_RGBA,
                                GLES20.GL_UNSIGNED_BYTE,
                                byteBuffer
                            )
//                    DLog.d("mPixelHandler cost time: ${System.currentTimeMillis() - start}")
                            byteBuffer.rewind()
                            if (onReadScanData != null && mPixelHandler.source != null) {
                                onReadScanData!!(
                                    mPixelHandler.width,
                                    mPixelHandler.height,
                                    mPixelHandler.source!!,
                                    byteBuffer
                                )
                            }
//                    DLog.d("qr cost time: ${System.currentTimeMillis() - start}")
                        }
                    }
                } finally {
                    mPixelLock.unlock()
                }
            }
        }
        if (mPixelLock.isHeldByCurrentThread) {
            mPixelLock.unlock()
        }
    }

    /**
     * 监听扫码窗口的数据
     */
    fun setOnReadScanDateListener(
        onReadScanData: (
            width: Int, height: Int,
            source: GLRGBLuminanceSource, grayByteBuffer: ByteBuffer
        ) -> Unit
    ) {
        this.onReadScanData = onReadScanData
    }

    /**
     * 设置闪光灯开启与否
     */
    fun setFlashLight(on: Boolean): Boolean {
        return mCamera.setFlashLight(on)
    }

}