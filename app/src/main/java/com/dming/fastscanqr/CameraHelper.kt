package com.dming.fastscanqr

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.dming.fastscanqr.utils.DLog
import com.dming.fastscanqr.utils.EglHelper
import com.dming.fastscanqr.utils.FGLUtils
import java.io.IOException
import java.util.*

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
    private val mHandlerThread = HandlerThread("GL")
    private lateinit var mHandler: Handler
    private lateinit var mCameraFilter: CameraFilter
    private var mTextureId: Int = 0
    private val mEglHelper = EglHelper()
    //
    private var width: Int = 0
    private var height: Int = 0

    fun init() {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }

    fun surfaceCreated(activity: Activity, holder: SurfaceHolder?) {
        mHandler.post {
            mEglHelper.initEgl(null, holder!!.surface)
            mEglHelper.glBindThread()
            mTextureId = FGLUtils.createOESTexture()
            mSurfaceTexture = SurfaceTexture(mTextureId)
            mCameraFilter = CameraFilter(activity)
            mSurfaceTexture.setOnFrameAvailableListener {
                it.updateTexImage()
                it.getTransformMatrix(mCameraMatrix)
                mEglHelper.swapBuffers()
                mCameraFilter.onDraw(mTextureId, mCameraMatrix, 0, 0, width, height)
            }
            chooseCamera()
            openCamera()
            if (mCamera != null) {
                try {
                    mCamera?.setPreviewTexture(mSurfaceTexture)
                } catch (e: IOException) {
                }
                setCameraDisplayOrientation(activity, mCamera!!, mCameraInfo)
                adjustCameraParameters(activity)
            }
        }
    }

    fun onSurfaceChanged(holder: SurfaceHolder?, width: Int, height: Int) {
        this@CameraHelper.width = width
        this@CameraHelper.height = height
        mHandler.post {
            mCameraFilter.onChange(width, height)
        }
    }

    fun surfaceDestroyed() {
        mHandler.post {
            FGLUtils.deleteTexture(mTextureId)
            releaseCamera()
            mCameraFilter.onDestroy()
            mEglHelper.destroyEgl()
            mSurfaceTexture.release()
        }
    }

    fun destroy() {
        mHandlerThread.quit()
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