package com.dming.fastscanqr

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.Surface
import com.dming.fastscanqr.utils.DLog
import java.io.IOException
import java.util.*
import kotlin.math.abs

@Suppress("DEPRECATION")
class Camera1 : ICamera {

    private var mCameraId: Int = 0
    private var mCamera: Camera? = null
    private lateinit var mCameraParameters: Camera.Parameters
    private val mCameraInfo = Camera.CameraInfo()
    private val mPreviewSizes: MutableList<CameraSize> = ArrayList()
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    override fun init() {
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

    override fun open(surfaceTexture: SurfaceTexture) {
        val start = System.currentTimeMillis()
        mCamera = Camera.open(mCameraId)
        mCameraParameters = mCamera!!.parameters
        mPreviewSizes.clear()
        for (size in mCameraParameters.supportedPreviewSizes) {
            //            DLog.i("size->" + size.width + " " + size.height);
            mPreviewSizes.add(CameraSize(size.width, size.height))
        }
        if (mCamera != null) {
            try {
                mCamera?.setPreviewTexture(surfaceTexture)
            } catch (e: IOException) {
            }
            setCameraDisplayOrientation(mCamera!!, mCameraInfo)
//            adjustCameraParameters()
        }
        DLog.d("openCamera cost time: ${System.currentTimeMillis() - start}")
    }

    override fun surfaceChange(width: Int, height: Int) {
        //
        adjustCameraParameters()
    }

    override fun close() {
        mCamera?.release()
    }

    override fun release() {
        //
    }

    private fun adjustCameraParameters() {
        val suitableSize = getDealCameraSize(mCameraInfo.orientation)
        val size = suitableSize!!.srcSize
        mCameraParameters.setPreviewSize(size.width, size.height)
        setAutoFocusInternal(true)
        mCamera?.parameters = mCameraParameters
        mCamera?.startPreview()
    }


    private fun setCameraDisplayOrientation(
        camera: Camera,
        info: Camera.CameraInfo
    ) {
        val rotation = mCameraInfo.orientation
//        val rotation = activity.windowManager.defaultDisplay.rotation
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
        return if (mCamera != null) {
            val modes = mCameraParameters.supportedFocusModes
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_FIXED
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_INFINITY
            } else {
                mCameraParameters.focusMode = modes[0]
            }
            true
        } else {
            false
        }
    }

    private fun getDealCameraSize(rotation: Int): CameraSize? {
        val greaterThanView = TreeSet<CameraSize>()
        val lessThanView = ArrayList<CameraSize>()
//        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val point = Point()
//        wm.defaultDisplay.getSize(point)
//        if (point.x in 701..799) {
//            point.x = 720
//            point.y = 1280
//        } else if (point.x in 1001..1099) {
//            point.x = 1080
//            point.y = 1920
//        }
//        val viewWidth = point.x
//        val viewHeight = point.y
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
                val diffWidth = abs(viewWidth - size.width)
                val diffHeight = abs(viewHeight - size.height)
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