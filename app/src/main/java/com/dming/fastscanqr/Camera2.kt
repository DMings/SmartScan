/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dming.fastscanqr

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.view.Surface
import com.dming.fastscanqr.utils.DLog

@TargetApi(21)
class Camera2 : BaseCamera(), ICamera {

    private var mCameraId: String? = null
    private var mCameraCharacteristics: CameraCharacteristics? = null
    private var mCamera: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    private var mAutoFocus: Boolean = false
    private var mSurface: Surface? = null
    private var mSurfaceTexture: SurfaceTexture? = null

    override fun init(context: Context) {
        mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        chooseCameraIdByFacing()
    }

    override fun open(textureId: Int) {
        collectCameraInfo()
        startOpeningCamera()
        mSurfaceTexture = SurfaceTexture(textureId)
        mSurface = Surface(mSurfaceTexture)
    }

    override fun surfaceChange(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        if (mCamera != null) {
            startCaptureSession(width, height)
        }
    }

    override fun close() {
        mCaptureSession?.close()
        mCaptureSession = null
        mCamera?.close()
        mSurfaceTexture?.release()
        mSurface?.release()
        mSurfaceTexture = null
        mSurface = null
        mCamera = null
        mPreviewRequestBuilder = null
    }

    override fun release() {
    }

    override fun getSurfaceTexture(): SurfaceTexture? {
        return mSurfaceTexture
    }

    override fun getCameraSize(): CameraSize? {
        return mCameraSize
    }

    private var mCameraManager: CameraManager? = null

    private val mSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            DLog.i("mSessionCallback >>>onConfigured")
            if (mCamera == null) {
                return
            }
            mCaptureSession = session
            updateAutoFocus()
            try {
                DLog.i("setRepeatingRequest")
                mCaptureSession!!.setRepeatingRequest(mPreviewRequestBuilder!!.build(), null, null)
            } catch (e: CameraAccessException) {
                DLog.e("Failed to start camera preview because it couldn't access camera $e")
            } catch (e: IllegalStateException) {
                DLog.e("Failed to start camera preview. $e")
            }

        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            DLog.e("Failed to configure capture session.")
        }

        override fun onClosed(session: CameraCaptureSession) {
            DLog.e("mSessionCallback onClosed")
            if (mCaptureSession != null && mCaptureSession == session) {
                mCaptureSession = null
            }
        }

    }

    private fun chooseCameraIdByFacing(): Boolean {
        try {
            val internalFacing = CameraCharacteristics.LENS_FACING_BACK
            val ids = mCameraManager!!.cameraIdList
            if (ids.isEmpty()) { // No camera
                throw RuntimeException("No camera available.")
            }
            for (id in ids) {
                val characteristics = mCameraManager!!.getCameraCharacteristics(id)
                val level = characteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                )
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    DLog.i("INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY")
                }
                val internal = characteristics.get(CameraCharacteristics.LENS_FACING)
                    ?: throw NullPointerException("Unexpected state: LENS_FACING null")
                if (internal == internalFacing) {
                    mCameraId = id
                    mCameraCharacteristics = characteristics
                    return true
                }
            }
            return false
        } catch (e: CameraAccessException) {
            throw RuntimeException("Failed to get a list of camera devices", e)
        }

    }

    private fun collectCameraInfo() {
        val map = mCameraCharacteristics!!.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        ) ?: throw IllegalStateException("Failed to get configuration map: " + mCameraId!!)
        mPreviewSizes.clear()
        map.getOutputSizes(SurfaceTexture::class.java).forEach {
            mPreviewSizes.add(CameraSize(it.width, it.height))
        }
    }

    private fun startCaptureSession(width: Int, height: Int) {
        DLog.i("startCaptureSession $width $height")
        val mSensorOrientation =
            mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        mCameraSize = getDealCameraSize(width, height, mSensorOrientation)
        mSurfaceTexture?.setDefaultBufferSize(mCameraSize!!.width, mCameraSize!!.height)
        try {
            mPreviewRequestBuilder = mCamera!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(mSurface!!)
            DLog.i("createCaptureSession >>>")
            mCamera!!.createCaptureSession(listOf(mSurface), mSessionCallback, null)
            DLog.i("createCaptureSession >>> finish")
        } catch (e: Exception) {
            DLog.i("CameraAccessException: ${e.cause}")
            throw RuntimeException("Failed to start camera session")
        }

    }

    @SuppressLint("MissingPermission")
    private fun startOpeningCamera() {
        try {
            mCameraManager!!.openCamera(mCameraId!!, object : CameraDevice.StateCallback() {

                override fun onOpened(camera: CameraDevice) {
                    DLog.i("startOpeningCamera onOpened")
                    mCamera = camera
                    if (mSurface != null && viewWidth != 0 && viewHeight != 0) {
                        startCaptureSession(viewWidth, viewHeight)
                    }
                }

                override fun onClosed(camera: CameraDevice) {
                    DLog.i("startOpeningCamera onClosed")
                }

                override fun onDisconnected(camera: CameraDevice) {
                    DLog.i("startOpeningCamera onDisconnected")
                    camera.close()
                    mCamera = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    DLog.e("startOpeningCamera onError: " + camera.id + " (" + error + ")")
                    mCamera!!.close()
                    mCamera = null
                }

            }, null)
        } catch (e: CameraAccessException) {
            throw RuntimeException("Failed to open camera: " + mCameraId!!, e)
        }

    }

    private fun updateAutoFocus() {
        if (mAutoFocus) {
            val modes = mCameraCharacteristics!!.get(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
            )
            // Auto focus is not supported
            if (modes == null || modes.isEmpty() ||
                modes.size == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF
            ) {
                mAutoFocus = false
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
            } else {
                mPreviewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }
        } else {
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
        }
    }

}
