package com.dming.fastscanqr

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.Surface
import com.dming.fastscanqr.utils.DLog
import java.io.IOException

@Suppress("DEPRECATION")
class Camera1 : BaseCamera(), ICamera {
    private var mCameraId: Int = 0
    private var mCamera: Camera? = null
    private lateinit var mCameraParameters: Camera.Parameters
    private val mCameraInfo = Camera.CameraInfo()
    private var mSurfaceTexture: SurfaceTexture? = null

    override fun init(context: Context) {
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

    override fun open(textureId: Int) {
        DLog.i("mCameraId: $mCameraId")
        mSurfaceTexture = SurfaceTexture(textureId)
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
                mCamera?.setPreviewTexture(mSurfaceTexture)
            } catch (e: IOException) {
            }
            setCameraDisplayOrientation(mCamera!!, mCameraInfo)
        }
        DLog.d("openCamera cost time: ${System.currentTimeMillis() - start}")
    }

    override fun surfaceChange(surface: Surface,width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        mSurfaceTexture?.setDefaultBufferSize(width, height)
        adjustCameraParameters(width, height)
    }

    override fun close() {
        mSurfaceTexture?.setOnFrameAvailableListener(null)
        mCamera?.release()
        mSurfaceTexture?.release()
        mSurfaceTexture = null
    }

    override fun release() {
        //
    }

    override fun getSurfaceTexture(): SurfaceTexture? {
        return mSurfaceTexture
    }

    private fun adjustCameraParameters(width: Int, height: Int) {
        Camera.getCameraInfo(mCameraId, mCameraInfo)
        val suitableSize = getDealCameraSize(width, height, mCameraInfo.orientation)
        val size = suitableSize!!.srcSize
        mCamera?.stopPreview()
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


}