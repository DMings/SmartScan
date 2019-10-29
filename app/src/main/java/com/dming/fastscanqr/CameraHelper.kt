package com.dming.fastscanqr

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.ImageView
import com.dming.fastscanqr.utils.EglHelper
import com.dming.fastscanqr.utils.FGLUtils
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock


class CameraHelper {
    private val mCamera = Camera1()

    private val mCameraMatrix = FloatArray(16)
    //
    private lateinit var mGLThread: HandlerThread
    private lateinit var mGLHandler: Handler
    private lateinit var mPreviewFilter: IShader
    private lateinit var mLuminanceFilter: IShader
    private var mTextureId: Int = 0
    private val mEglHelper = EglHelper()
    //
    private lateinit var mPixelThread: HandlerThread
    private lateinit var mPixelHandler: Handler
    private var mIsPixelInitSuccess = false
    private val mPixelLock = ReentrantLock()
    //
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    //
    private var mPixelBuffer: ByteBuffer? = null
//    private var mPixelBitmap: Bitmap? = null
    private val mPixelEglHelper = EglHelper()

    private var mPixelSurface: Surface? = null
    private var mPixelSurfaceTexture: SurfaceTexture? = null
    //
    private var mFrameIds: IntArray? = null
    private var mPixelTexture = -1
    //
    private lateinit var mPixelFilter: IShader
    //
    private var readQRCode: ((width: Int, height: Int, grayByteBuffer: ByteBuffer) -> Unit)? = null
    //
//    private var mTestTexture = -1
    private var mContext: Context? = null

    fun init(context: Context) {
        mContext = context
        mGLThread = HandlerThread("GL")
        mPixelThread = HandlerThread("QR")
        mGLThread.start()
        mGLHandler = Handler(mGLThread.looper)
        mPixelThread.start()
        mPixelHandler = Handler(mPixelThread.looper)
        mGLHandler.post {
            mCamera.init(context)
        }
    }

    fun surfaceCreated(activity: Activity, holder: SurfaceHolder?) {
        mGLHandler.post {
            mEglHelper.initEgl(null, holder!!.surface)
            mTextureId = FGLUtils.createOESTexture()
            mPreviewFilter = PreviewFilter(activity)
            mLuminanceFilter = LuminanceFilter(activity)
            mCamera.open(mTextureId)
            mCamera.getSurfaceTexture()?.setOnFrameAvailableListener {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                if (mFrameIds != null) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameIds!![0])
                    mLuminanceFilter.onDraw(mTextureId, 0, 0, mWidth, mHeight, mCameraMatrix)
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                }
                //
                mPreviewFilter.onDraw(mTextureId, 0, 0, mWidth, mHeight, mCameraMatrix)
                mEglHelper.swapBuffers()
                it.updateTexImage()
                it.getTransformMatrix(mCameraMatrix)
                //
                mPixelLock.tryLock()
                mPixelHandler.post {
                    if (mIsPixelInitSuccess && mFrameIds != null) {
                        mPixelFilter.onDraw(mFrameIds!![1], 0, 0, mWidth, mHeight, null)
                    }
                    mPixelEglHelper.swapBuffers()
                    //
                    try {
                        mPixelLock.lock()
                        val start = System.currentTimeMillis()
                        mPixelBuffer!!.position(0)
                        GLES20.glReadPixels(
                            0,
                            0,
                            mWidth,
                            mHeight,
                            GLES20.GL_RGBA,
                            GLES20.GL_UNSIGNED_BYTE,
                            mPixelBuffer
                        )
//                        DLog.d("mPixelHandler cost time: ${System.currentTimeMillis() - start}")
                        mPixelBuffer?.rewind()
//                        DLog.d("bitmap cost time: ${System.currentTimeMillis() - start}")
                        if (readQRCode != null && mPixelBuffer != null) {
                            this.readQRCode!!(mWidth, mHeight, mPixelBuffer!!)
                        }
                    } finally {
                        mPixelLock.unlock()
                    }
                }
                if (mPixelLock.isHeldByCurrentThread) {
                    mPixelLock.unlock()
                }
            }
            mPixelHandler.post {
                mPixelTexture = FGLUtils.createOESTexture()
                mPixelSurfaceTexture = SurfaceTexture(mPixelTexture)
                mPixelSurface = Surface(mPixelSurfaceTexture)
                mPixelEglHelper.initEgl(mEglHelper.eglContext, mPixelSurface)
                mPixelFilter = PixelFilter(activity)
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
                mPixelSurfaceTexture?.setOnFrameAvailableListener {
                    it.updateTexImage()
                }
            }
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        mGLHandler.post {
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            if (mFrameIds != null) {
                FGLUtils.deleteFBO(mFrameIds)
            }
            mFrameIds = FGLUtils.createFBO(width, height)
            //
            mCamera.surfaceChange(width, height)
            val cameraSize = mCamera.getCameraSize()!!

            mPreviewFilter.onChange(cameraSize.width, cameraSize.height, width, height)
            mLuminanceFilter.onChange(cameraSize.width, cameraSize.height, width, height)
            mPixelFilter.onChange(cameraSize.width, cameraSize.height, width, height)
            //
            mPixelHandler.post {
                mPixelBuffer = ByteBuffer.allocate(mWidth * mHeight * 4)
//                mPixelBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
                mPixelSurfaceTexture?.setDefaultBufferSize(mWidth, mHeight)
                GLES20.glViewport(0, 0, mWidth, mHeight)
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                mIsPixelInitSuccess = true
            }
        }

    }

    fun surfaceDestroyed() {
        mIsPixelInitSuccess = false
        mPixelSurfaceTexture?.setOnFrameAvailableListener(null)
        mPixelHandler.post {
            FGLUtils.deleteTexture(mPixelTexture)
            mPixelFilter.onDestroy()
            mPixelEglHelper.destroyEgl()
            mPixelSurfaceTexture?.release()
            mPixelSurface?.release()
        }
        mGLHandler.post {
            if (mFrameIds != null) {
                FGLUtils.deleteFBO(mFrameIds)
                mFrameIds = null
            }
            FGLUtils.deleteTexture(mTextureId)
            mCamera.close()
            mPreviewFilter.onDestroy()
            mLuminanceFilter.onDestroy()
            mEglHelper.destroyEgl()
        }
    }

    fun destroy() {
        mCamera.release()
        mGLThread.quit()
        mPixelThread.quit()
    }

    fun setParseQRListener(readQRCode: (width: Int, height: Int, grayByteBuffer: ByteBuffer) -> Unit) {
        this.readQRCode = readQRCode
    }

    fun readPixels(imageView: ImageView) {
//        mPixelHandler.post {
//            val start = System.currentTimeMillis()
//            mPixelBuffer!!.position(0)
//            GLES20.glReadPixels(
//                0,
//                0,
//                mWidth,
//                mHeight,
//                GLES20.GL_RGBA,
//                GLES20.GL_UNSIGNED_BYTE,
//                mPixelBuffer
//            )
//            DLog.d("mPixelHandler cost time: ${System.currentTimeMillis() - start}")
//            mPixelBuffer?.rewind()
//            mPixelBitmap!!.copyPixelsFromBuffer(mPixelBuffer)
//            DLog.d("bitmap cost time: ${System.currentTimeMillis() - start}")
//            (imageView.context as Activity).runOnUiThread {
//                imageView.setImageBitmap(mPixelBitmap)
//            }
//        }
    }


}