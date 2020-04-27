package com.dming.glScan.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES20
import android.os.HandlerThread
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
    private var mGLHandler: CameraHandler? = null
    private var mPreviewFilter: IShader? = null
    private var mLuminanceFilter: IShader? = null
    private var mTextureId: Int = 0
    private val mEglHelper = EglHelper()
    // 像素读取，解码线程
    private var mPixelThread: HandlerThread? = null
    private var mPixelHandler: PixelHandler? = null
    private val mPixelLock = ReentrantLock()
    //
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    //
    private val mPixelEglHelper = EglHelper()
    private var mPixelSurfaceTexture: SurfaceTexture? = null
    //
    private var mFrameIds: IntArray? = null
    private var mPixelTexture = -1
    private var mPixelFilter: IShader? = null
    //
    private var mScale: Float = 1.0f

    private var mOnReadScanData: ((
        width: Int, height: Int,
        source: GLRGBLuminanceSource, grayByteBuffer: ByteBuffer
    ) -> Unit)? = null

    /**
     * 初始化GL绘制线程、scan读取像素解码线程，
     * 获取摄像头信息
     */
    fun init(context: Context) {
        mPixelThread = HandlerThread("SCAN")
        mGLHandler = CameraHandler.instance
        mGLHandler?.create()
        mPixelThread?.let {
            it.start()
            mPixelHandler = PixelHandler(it.looper)
        }
        CameraHandler.instance.post {
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
//      DLog.i("surfaceCreated")
        mGLHandler?.post {
            //          DLog.i("surfaceCreated mGLHandler post")
            mEglHelper.initEgl(null, holder!!.surface)
            if (mEglHelper.isEGLCreate()) {
                mTextureId = FGLUtils.createOESTexture()
                mPreviewFilter = PreviewFilter(context)
                mLuminanceFilter = LuminanceFilter(context)
                mCamera.open(mTextureId)
                mCamera.getSurfaceTexture()?.setOnFrameAvailableListener {
                    mCamera.getSurfaceTexture()?.let { surfaceTexture ->
                        surfaceTexture.getTransformMatrix(mCameraMatrix)
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                        mFrameIds?.let { frameIds ->
                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameIds[0])
                            mLuminanceFilter?.setScaleMatrix(mScale)
                            mLuminanceFilter?.onDraw(
                                mTextureId,
                                0,
                                0,
                                mWidth,
                                mHeight,
                                mCameraMatrix
                            )
                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                        }
                        //
                        mPreviewFilter?.setScaleMatrix(mScale)
                        mPreviewFilter?.onDraw(mTextureId, 0, 0, mWidth, mHeight, mCameraMatrix)
                        mEglHelper.swapBuffers()
                        surfaceTexture.updateTexImage()
                        //
                        readScanDataFromGL()
                    }
                }
                mPixelTexture = FGLUtils.createOESTexture()
                mPixelHandler?.post {
                    //              DLog.i("surfaceCreated mPixelHandler post")
                    mPixelSurfaceTexture = SurfaceTexture(mPixelTexture)
                    mPixelEglHelper.initEgl(mEglHelper.eglContext, mPixelSurfaceTexture)
                    if (mPixelEglHelper.isEGLCreate()) {
                        mPixelFilter = PixelFilter(context)
                        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
                        mPixelSurfaceTexture?.setOnFrameAvailableListener {
                            mPixelSurfaceTexture?.updateTexImage()
                        }
                    }
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
//      DLog.i("onSurfaceChanged")
        mGLHandler?.post {
            //          DLog.i("onSurfaceChanged mGLHandler post")
            if (mEglHelper.isEGLCreate()) {
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
//          DLog.i("cameraSize  width: ${cameraSize.width} height: ${cameraSize.height}")
                mPreviewFilter?.onChange(cameraSize.width, cameraSize.height, width, height)
                mLuminanceFilter?.onChange(cameraSize.width, cameraSize.height, width, height)
                //
                mPixelHandler?.post {
                    if (mPixelEglHelper.isEGLCreate()) {
//                  DLog.i("onSurfaceChanged mPixelHandler post")
                        mPixelFilter?.onChange(
                            cameraSize.width,
                            cameraSize.height,
                            mWidth,
                            mHeight
                        )
                        mPixelSurfaceTexture?.setDefaultBufferSize(mWidth, mHeight)
                        GLES20.glViewport(0, 0, mWidth, mHeight)
                        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    }
                }
            }
        }
    }

    /**
     * surface关闭，释放资源，删除纹理
     */
    fun surfaceDestroyed() {
//        DLog.i("surfaceDestroyed")
        mPixelSurfaceTexture?.setOnFrameAvailableListener(null)
        mPixelHandler?.buffer = null
        mPixelSurfaceTexture = null
        mPixelHandler?.post {
            if (mPixelEglHelper.isEGLCreate()) {
//                  DLog.i("surfaceDestroyed pixelHandler post")
                if (mPixelTexture != -1) {
                    FGLUtils.deleteTexture(mPixelTexture)
                    mPixelTexture = -1
                }
                mPixelFilter?.onDestroy()
                mPixelEglHelper.destroyEgl()
                mPixelSurfaceTexture?.release()
            }
        }
        mGLHandler?.post {
            //          DLog.i("surfaceDestroyed mGLHandler post")
            mCamera.close()
            if (mEglHelper.isEGLCreate()) {
                mFrameIds?.let {
                    FGLUtils.deleteFBO(it)
                }
                mFrameIds = null
                FGLUtils.deleteTexture(mTextureId)
                mPreviewFilter?.onDestroy()
                mLuminanceFilter?.onDestroy()
                mEglHelper.destroyEgl()
            }
        }
    }

    /**
     * 资源释放，camera关闭，GL线程退出，解码线程推出
     */
    fun destroy() {
//        DLog.i("destroy >>>")
        mOnReadScanData = null
        // 保证post执行完退出
        mGLHandler?.destroy {
            mCamera.release()
        }
        mPixelHandler?.post {
            mPixelThread?.quit()
        }
//        mGLThread?.join() // GL线程一般不会堵塞，要等待结束，不然，再进入页面请求GL会出问题
//        mPixelThread.join() // 解码线程耗时严重，不能随意join
        mGLHandler = null
        mPixelHandler = null
        mPixelThread = null
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
        mPixelHandler?.post {
            mPixelHandler?.setConfigure(smartScanParameter, mWidth, mHeight)
        }
    }

    /**
     * 从GL读取预览框数据
     */
    private fun readScanDataFromGL() {
        mPixelLock.tryLock() // 如果正在处理数据，就会获取锁失败，这时正常情况
        mPixelHandler?.post {
            mPixelHandler?.let { pixelHandler ->
                if (mPixelEglHelper.isEGLCreate()) {
                    if (mFrameIds != null) {
                        mPixelFilter?.onDraw(mFrameIds!![1], 0, 0, mWidth, mHeight, null)
                    }
                    //
                    try {
                        mPixelLock.lock()
                        if (pixelHandler.width != 0 && pixelHandler.height != 0) {
                            pixelHandler.buffer?.let { byteBuffer ->
                                byteBuffer.position(0)
                                GLES20.glReadPixels(
                                    pixelHandler.left,
                                    pixelHandler.top,
                                    pixelHandler.width,
                                    pixelHandler.height,
                                    GLES20.GL_RGBA,
                                    GLES20.GL_UNSIGNED_BYTE,
                                    byteBuffer
                                )
                                byteBuffer.rewind()
                                mOnReadScanData?.let {
                                    if (pixelHandler.source != null) {
                                        it(
                                            pixelHandler.width,
                                            pixelHandler.height,
                                            pixelHandler.source!!,
                                            byteBuffer
                                        )
                                    }
                                }
                            }
                        }
                    } finally {
                        mPixelLock.unlock()
                    }
                    mPixelEglHelper.swapBuffers()
                }
            }
        }
        if (mPixelLock.isHeldByCurrentThread) { // 如果锁定就释放锁
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
        mOnReadScanData = onReadScanData
    }

    /**
     * 设置闪光灯开启与否
     */
    fun setFlashLight(on: Boolean): Boolean {
        return mCamera.setFlashLight(on)
    }

    /**
     * 获取camera对象
     */
    fun getCamera(): Camera? {
        return mCamera.getCamera()
    }

}