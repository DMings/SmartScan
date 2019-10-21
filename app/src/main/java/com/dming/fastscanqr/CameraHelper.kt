package com.dming.fastscanqr

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.ImageView
import com.dming.fastscanqr.utils.DLog
import com.dming.fastscanqr.utils.EglHelper
import com.dming.fastscanqr.utils.FGLUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs


class CameraHelper {
    private val mCamera = Camera1()

    private lateinit var mSurfaceTexture: SurfaceTexture
    private val mCameraMatrix = FloatArray(16)
    //
    private lateinit var mGLThread: HandlerThread
    private lateinit var mGLHandler: Handler
    private lateinit var mPreviewFilter: PreviewFilter
    private lateinit var mLuminanceFilter: LuminanceFilter
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
    private var mPixelBitmap: Bitmap? = null
    private val mPixelEglHelper = EglHelper()

    private var mPixelSurface: Surface? = null
    private var mPixelSurfaceTexture: SurfaceTexture? = null
    //
    private var mFrameIds = IntArray(2)
    private var mPixelTexture = -1
    //
    private lateinit var mPixelFilter: PixelFilter

    fun init() {
        mGLThread = HandlerThread("GL")
        mPixelThread = HandlerThread("QR")
        mGLThread.start()
        mGLHandler = Handler(mGLThread.looper)
        mPixelThread.start()
        mPixelHandler = Handler(mPixelThread.looper)
        mGLHandler.post {
            mCamera.init()
        }
    }

    fun surfaceCreated(activity: Activity, holder: SurfaceHolder?) {
        mGLHandler.post {
            mEglHelper.initEgl(null, holder!!.surface)
            mTextureId = FGLUtils.createOESTexture()
            mSurfaceTexture = SurfaceTexture(mTextureId)
            mPreviewFilter = PreviewFilter(activity)
            mLuminanceFilter = LuminanceFilter(activity)
            mSurfaceTexture.setOnFrameAvailableListener {
                it.updateTexImage()
                it.getTransformMatrix(mCameraMatrix)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameIds[0])
                mLuminanceFilter.onDraw(mTextureId, mCameraMatrix, 0, 0, mWidth, mHeight)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                //
                mPreviewFilter.onDraw(mTextureId, mCameraMatrix, 0, 0, mWidth, mHeight)
                mEglHelper.swapBuffers()
                //
                mPixelHandler.post {
                    if (mIsPixelInitSuccess) {
                        mPixelFilter.onDraw(mFrameIds[1], mCameraMatrix, 0, 0, mWidth, mHeight)
                    }
                    mPixelEglHelper.swapBuffers()
                }
            }
            mCamera.open(mSurfaceTexture)
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
            mSurfaceTexture.setDefaultBufferSize(width, height)
            GLES20.glViewport(0, 0, mWidth, mHeight)
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            mFrameIds = FGLUtils.createFBO(width, height)
            //
            mCamera.surfaceChange(width, height)
            //
            mPixelHandler.post {
                mPixelBuffer = ByteBuffer.allocate(width * height * 4)
                mPixelBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
                mPixelSurfaceTexture?.setDefaultBufferSize(width, height)
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
        mSurfaceTexture.setOnFrameAvailableListener(null)
        mPixelHandler.post {
            FGLUtils.deleteTexture(mPixelTexture)
            mPixelFilter.onDestroy()
            mPixelEglHelper.destroyEgl()
            mPixelSurfaceTexture?.release()
            mPixelSurface?.release()
        }
        mGLHandler.post {
            FGLUtils.deleteFBO(mFrameIds)
            FGLUtils.deleteTexture(mTextureId)
            mCamera.close()
            mPreviewFilter.onDestroy()
            mLuminanceFilter.onDestroy()
            mEglHelper.destroyEgl()
            mSurfaceTexture.release()
        }
    }

    fun destroy() {
        mCamera.release()
        mGLThread.quit()
        mPixelThread.quit()
    }

    fun readPixels(imageView: ImageView) {
        mPixelHandler.post {
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
            DLog.d("mPixelHandler cost time: ${System.currentTimeMillis() - start}")
            mPixelBuffer?.rewind()
            mPixelBitmap!!.copyPixelsFromBuffer(mPixelBuffer)
            DLog.d("bitmap cost time: ${System.currentTimeMillis() - start}")
            (imageView.context as Activity).runOnUiThread {
                imageView.setImageBitmap(mPixelBitmap)
            }
        }
    }


}