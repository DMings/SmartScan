package com.dming.fastscanqr

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.Surface
import android.view.WindowManager
import com.dming.fastscanqr.utils.DLog


@Suppress("DEPRECATION")
class Camera1 : BaseCamera(), ICamera {
    private var mCameraId: Int = 0
    private var mCamera: Camera? = null
    private lateinit var mCameraParameters: Camera.Parameters
    private val mCameraInfo = Camera.CameraInfo()
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mContext: Context? = null

    override fun init(context: Context) {
        mContext = context
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
            mPreviewSizes.add(CameraSize(size.width, size.height))
        }
        if (mCamera != null) {
            mCamera?.setPreviewTexture(mSurfaceTexture)
            setCameraDisplayOrientation(mCamera!!, mCameraInfo)
        }
        DLog.d("openCamera cost time: ${System.currentTimeMillis() - start}")
    }

    override fun surfaceChange(width: Int, height: Int) {
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

    override fun getCameraSize(): CameraSize? {
        return mCameraSize
    }

    private fun adjustCameraParameters(width: Int, height: Int) {
        val rotation =
            if (mContext != null)
                (mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay.rotation
            else 0
        Camera.getCameraInfo(mCameraId, mCameraInfo)
        mCameraSize = getDealCameraSize(width, height, rotation)
        val size = mCameraSize!!.srcSize
        mCamera?.stopPreview()
        mCameraParameters.setPreviewSize(size.width, size.height)
        setAutoFocusInternal()
        mCamera?.let {
            it.parameters = mCameraParameters
            it.startPreview()
        }
    }


    private fun setCameraDisplayOrientation(
        camera: Camera,
        info: Camera.CameraInfo
    ) {
//        val rotation = mCameraInfo.orientation
        val rotation =
            if (mContext != null)
                (mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay.rotation
            else 0
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

    private fun setAutoFocusInternal(): Boolean {
        return if (mCamera != null) {
            val modes = mCameraParameters.supportedFocusModes
            when {
                modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ->
                    mCameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                modes.contains(Camera.Parameters.FOCUS_MODE_FIXED) ->
                    mCameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_FIXED
                modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY) ->
                    mCameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_INFINITY
                else -> mCameraParameters.focusMode = modes[0]
            }
            true
        } else {
            false
        }
    }


}