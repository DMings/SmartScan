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


@Suppress("DEPRECATION")
class CameraHelper {
    //
    private var mCameraId: Int = 0
    private var mCamera: Camera? = null
    private lateinit var mCameraParameters: Camera.Parameters
    private val mCameraInfo = Camera.CameraInfo()
    private val mPreviewSizes: MutableList<CameraSize> = ArrayList()
    private lateinit var mSurfaceTexture: SurfaceTexture
    private val mCameraMatrix = FloatArray(16)
    //
    private lateinit var mGLThread: HandlerThread
    private lateinit var mGLHandler: Handler
    private lateinit var mCameraFilter: CameraFilter
    private var mTextureId: Int = 0
    private val mEglHelper = EglHelper()
    //
    private lateinit var mPixelThread: HandlerThread
    private lateinit var mPixelHandler: Handler
    private val qrLock = ReentrantLock()
    //
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    //
    private var mPixelBuffer: ByteBuffer? = null
    private var mPixelBitmap: Bitmap? = null
    private val mPixelEglHelper = EglHelper()
//    private lateinit var mPixelFilter: CameraFilter

    private var mPixelSurface: Surface? = null
    private var mPixelSurfaceTexture: SurfaceTexture? = null
    //
    private var mFrameIds = IntArray(2)
    private var mPixelTexture = -1
    //
//    private lateinit var mImgFilter: ImgFilter

    fun init() {
        mGLThread = HandlerThread("GL")
        mPixelThread = HandlerThread("QR")
        mGLThread.start()
        mGLHandler = Handler(mGLThread.looper)
        mPixelThread.start()
        mPixelHandler = Handler(mPixelThread.looper)
        mGLHandler.post {
            chooseCamera()
        }
    }

    fun surfaceCreated(activity: Activity, holder: SurfaceHolder?) {
        mGLHandler.post {
            mEglHelper.initEgl(null, holder!!.surface)
            mTextureId = FGLUtils.createOESTexture()
            mSurfaceTexture = SurfaceTexture(mTextureId)
            mCameraFilter = CameraFilter(activity)
            GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f)
            mSurfaceTexture.setOnFrameAvailableListener {
                it.updateTexImage()
                it.getTransformMatrix(mCameraMatrix)
//                GLES20.glViewport(0, 0, mWidth, mHeight)
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameIds[0])
//                mCameraFilter.onDraw(mTextureId, mCameraMatrix, 0, 0, mWidth, mHeight)
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                //
                mCameraFilter.onDraw(mTextureId, mCameraMatrix, 0, 0, mWidth, mHeight)
                mEglHelper.swapBuffers()
                //
                mPixelHandler.post {
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//                    mImgFilter.onDraw(mFrameIds[1], mCameraMatrix, 0, 0, mWidth, mHeight)
                    mPixelEglHelper.swapBuffers()
                }

            }
            openCamera()
            if (mCamera != null) {
                try {
                    mCamera?.setPreviewTexture(mSurfaceTexture)
                } catch (e: IOException) {
                }
                setCameraDisplayOrientation(activity, mCamera!!, mCameraInfo)
                adjustCameraParameters(activity)
            }
            mPixelHandler.post {
                mPixelTexture = FGLUtils.createOESTexture()
                mPixelSurfaceTexture = SurfaceTexture(mPixelTexture)
                mPixelSurface = Surface(mPixelSurfaceTexture)
                mPixelEglHelper.initEgl(null, mPixelSurface)
//                mImgFilter = ImgFilter(activity)
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
                mPixelSurfaceTexture?.setOnFrameAvailableListener {
                    it.updateTexImage()
                }
            }
        }
    }

    fun onSurfaceChanged(holder: SurfaceHolder?, width: Int, height: Int) {
        mWidth = width
        mHeight = height
        mPixelBuffer = ByteBuffer.allocate(width * height * 4)
        mPixelBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        mGLHandler.post {
            mSurfaceTexture.setDefaultBufferSize(width, height)
            mFrameIds = FGLUtils.createFBO(width, height)
        }

    }

    fun surfaceDestroyed() {
        mPixelHandler.post {
            mPixelSurfaceTexture?.setOnFrameAvailableListener(null)
            FGLUtils.deleteTexture(mPixelTexture)
//            mImgFilter.onDestroy()
            mPixelEglHelper.destroyEgl()
            mPixelSurfaceTexture?.release()
            mPixelSurface?.release()
        }
        mGLHandler.post {
            FGLUtils.deleteFBO(mFrameIds)
            mSurfaceTexture.setOnFrameAvailableListener(null)
            FGLUtils.deleteTexture(mTextureId)
            releaseCamera()
            mCameraFilter.onDestroy()
            mEglHelper.destroyEgl()
            mSurfaceTexture.release()
        }
    }

    fun destroy() {
        mGLThread.quit()
        mPixelThread.quit()
    }

    fun readPixels(imageView: ImageView) {
        mPixelHandler.post {
            val start = System.currentTimeMillis()
            mPixelBuffer!!.position(0)
//            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameIds[0])
            GLES20.glReadPixels(
                0,
                0,
                mWidth,
                mHeight,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                mPixelBuffer
            )
//            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            DLog.d("mPixelHandler cost time: ${System.currentTimeMillis() - start}")
//            mPixelBuffer?.rewind()
            mPixelBitmap!!.copyPixelsFromBuffer(mPixelBuffer)
            DLog.d("bitmap cost time: ${System.currentTimeMillis() - start}")
            (imageView.context as Activity).runOnUiThread {
                imageView.setImageBitmap(mPixelBitmap)
            }
        }
    }

    private fun chooseCamera() {
        var i = 0
        val count = Camera.getNumberOfCameras()
        while (i < count) {
            Camera.getCameraInfo(i, mCameraInfo)
            if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCameraId = i
                return
            }
            i++
        }
        mCameraId = -1
    }

    private fun openCamera() {
        if (mCamera != null) {
            releaseCamera()
        }
        mCamera = Camera.open(mCameraId)
        mCameraParameters = mCamera!!.parameters
        mPreviewSizes.clear()
        for (size in mCameraParameters.supportedPreviewSizes) {
            //            DLog.i("size->" + size.width + " " + size.height);
            mPreviewSizes.add(CameraSize(size.width, size.height))
        }

    }

    private fun adjustCameraParameters(context: Context) {
        val suitableSize = getDealCameraSize(context, mCameraInfo.orientation)
        val size = suitableSize!!.srcSize
        mCameraParameters.setPreviewSize(size.width, size.height)
        setAutoFocusInternal(true)
        mCamera?.parameters = mCameraParameters
        mCamera?.startPreview()
    }

    private fun releaseCamera() {
        mCamera?.release()
    }

    private fun setCameraDisplayOrientation(
        activity: Activity,
        camera: Camera,
        info: Camera.CameraInfo
    ) {
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360   // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        DLog.i("result: $result")
        camera.setDisplayOrientation(result)
    }

    private fun setAutoFocusInternal(autoFocus: Boolean): Boolean {
        if (mCamera != null) {
            val modes = mCameraParameters.getSupportedFocusModes()
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED)
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY)
            } else {
                mCameraParameters.setFocusMode(modes.get(0))
            }
            return true
        } else {
            return false
        }
    }

    private fun getDealCameraSize(context: Context, rotation: Int): CameraSize? {
        val greaterThanView = TreeSet<CameraSize>()
        val lessThanView = ArrayList<CameraSize>()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getSize(point)
        if (point.x in 701..799) {
            point.x = 720
            point.y = 1280
        } else if (point.x in 1001..1099) {
            point.x = 1080
            point.y = 1920
        }
        val viewWidth = point.x
        val viewHeight = point.y
        DLog.i("viewWidth>  $viewWidth viewHeight>> $viewHeight")
        for (size in mPreviewSizes) {
            if (rotation == 90 || rotation == 270) { // width > height normal
                if (size.width >= viewHeight && size.height >= viewWidth) {
                    greaterThanView.add(CameraSize(size.height, size.width, size))
                } else {
                    lessThanView.add(CameraSize(size.height, size.width, size))
                }
            } else { // width < height normal  0 180
                if (size.width >= viewWidth && size.height >= viewHeight) {
                    greaterThanView.add(CameraSize(size.width, size.height, size))
                } else {
                    lessThanView.add(CameraSize(size.width, size.height, size))
                }
            }
        }
        var cSize: CameraSize? = null
        if (greaterThanView.size > 0) {
            cSize = greaterThanView.first()
        } else {
            var diffMinValue = Integer.MAX_VALUE
            for (size in lessThanView) {
                val diffWidth = Math.abs(viewWidth - size.width)
                val diffHeight = Math.abs(viewHeight - size.height)
                val diffValue = diffWidth + diffHeight
                if (diffValue < diffMinValue) {  // 找出差值最小的数
                    diffMinValue = diffValue
                    cSize = size
                }
            }
            if (cSize == null) {
                cSize = lessThanView[0]
            }
        }
        DLog.i("suitableSize>" + cSize!!.toString())
        return cSize
    }

}