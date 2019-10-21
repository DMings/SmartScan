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
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import com.dming.fastscanqr.utils.DLog

@TargetApi(21)
class Camera2 : BaseCamera(){

    private var mCameraManager: CameraManager? = null

    private val mCameraDeviceCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            mCamera = camera
                        startCaptureSession();
        }

        override fun onClosed(camera: CameraDevice) {}

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            mCamera = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            DLog.e("onError: " + camera.id + " (" + error + ")")
            mCamera!!.close()
            mCamera = null
        }

    }

    private val mSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            if (mCamera == null) {
                return
            }
            mCaptureSession = session
            updateAutoFocus()
            try {
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
            if (mCaptureSession != null && mCaptureSession == session) {
                mCaptureSession = null
            }
        }

    }

    private var mCameraId: String? = null

    private var mCameraCharacteristics: CameraCharacteristics? = null

    private var mCamera: CameraDevice? = null

    private var mCaptureSession: CameraCaptureSession? = null

    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    private var mAutoFocus: Boolean = false

    fun init(context: Context) {
        mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun start(): Boolean {
        if (!chooseCameraIdByFacing()) {
            return false
        }
        collectCameraInfo()
        startOpeningCamera()
        return true
    }

    fun stop() {
        if (mCaptureSession != null) {
            mCaptureSession!!.close()
            mCaptureSession = null
        }
        if (mCamera != null) {
            mCamera!!.close()
            mCamera = null
        }
        mPreviewRequestBuilder = null
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
                    continue
                }
                val internal = characteristics.get(CameraCharacteristics.LENS_FACING)
                    ?: throw NullPointerException("Unexpected state: LENS_FACING null")
                if (internal == internalFacing) {
                    mCameraId = id
                    mCameraCharacteristics = characteristics
                    return true
                }
            }
            // Not found
            mCameraId = ids[0]
            mCameraCharacteristics = mCameraManager!!.getCameraCharacteristics(mCameraId!!)
            val level = mCameraCharacteristics!!.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
            )
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                return false
            }
            val internal = mCameraCharacteristics!!.get(CameraCharacteristics.LENS_FACING)
                ?: throw NullPointerException("Unexpected state: LENS_FACING null")
            return true
        } catch (e: CameraAccessException) {
            throw RuntimeException("Failed to get a list of camera devices", e)
        }

    }

    private fun collectCameraInfo() {
        val map = mCameraCharacteristics!!.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )?: throw IllegalStateException("Failed to get configuration map: " + mCameraId!!)
        mPreviewSizes.clear()

        map.getOutputSizes(Surface::class.java).forEach {
            mPreviewSizes.add(CameraSize(it.width, it.height))
        }
        val mSensorOrientation =
            mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        getDealCameraSize(1,1,mSensorOrientation)
    }

    private fun startCaptureSession(surface: Surface) {
        //        final int viewWidth = mPreview.getWidth();
        //        final int viewHeight = mPreview.getHeight();
        //        if (mCamera != null ||
        //                width == 0 || height == 0 || mPreviewRequestBuilder != null) {
        //            return;
        //        }
        //        dealCameraSize(mSensorOrientation);
        //        CameraSize suitableSize = getSuitableSize().getSrcSize();
        //        prepareImageReader(suitableSize);
        //        DLog.i( "startCaptureSession " + suitableSize.getWidth() + " -> " + suitableSize.getHeight());
        //            mPreview.setBufferSize(suitableSize.getWidth(), suitableSize.getHeight());
        //            Surface surface = mPreview.getSurface();
        try {
            mPreviewRequestBuilder = mCamera!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(surface)
            mCamera!!.createCaptureSession(
                listOf(surface),
                mSessionCallback, null
            )
        } catch (e: CameraAccessException) {
            throw RuntimeException("Failed to start camera session")
        }

    }

    @SuppressLint("MissingPermission")
    private fun startOpeningCamera() {
        try {
            mCameraManager!!.openCamera(mCameraId!!, mCameraDeviceCallback, null)
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
            if (modes == null || modes.size == 0 ||
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
