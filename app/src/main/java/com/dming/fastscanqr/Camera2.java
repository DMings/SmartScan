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

package com.dming.fastscanqr;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Deprecated
@SuppressWarnings("MissingPermission")
@TargetApi(21)
class Camera2 extends CameraViewImpl {

    private static final String TAG = "Camera2";

    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();
    private Integer mSensorOrientation;

    static {
        INTERNAL_FACINGS.put(Constants.FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(Constants.FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
    }

    private final CameraManager mCameraManager;

    private final CameraDevice.StateCallback mCameraDeviceCallback
            = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            mCallback.onCameraOpened();
            startCaptureSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mCallback.onCameraClosed();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            DLog.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    private final CameraCaptureSession.StateCallback mSessionCallback
            = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (mCamera == null) {
                return;
            }
            mCaptureSession = session;
            updateAutoFocus();
            updateFlash();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                DLog.e(TAG, "Failed to start camera preview because it couldn't access camera "+ e);
            } catch (IllegalStateException e) {
                DLog.e(TAG, "Failed to start camera preview. "+ e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            DLog.e(TAG, "Failed to configure capture session.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            if (mCaptureSession != null && mCaptureSession.equals(session)) {
                mCaptureSession = null;
            }
        }

    };

//    private PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {
//
//        @Override
//        public void onPrecaptureRequired() {
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
//                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
//            setState(STATE_PRECAPTURE);
//            try {
//                mCaptureSession.capture(mPreviewRequestBuilder.build(), this, mBackgroundHandler);
//                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
//                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
//            } catch (CameraAccessException e) {
//                DLog.e(TAG, "Failed to run precapture sequence.", e);
//            }
//        }
//
//        @Override
//        public void onReady() {
////            captureStillPicture();
//        }
//
//    };

    private ReentrantLock mLock = new ReentrantLock();
    private Condition mWaitLock = mLock.newCondition();
    private boolean mDealThreadRun = true;
    private boolean mMakeData = false;

    private final Runnable dealCameraData = new Runnable() {
        @Override
        public void run() {
            while (mDealThreadRun) {
                try {
                    try {
                        mLock.lock();
                        mWaitLock.await();
                        if (mMakeData) {
//                            DLog.i("DMUI","消费: ");
                            mCallback.onDealCameraData(mSensorOrientation);
                            mMakeData = false;
                        } else {
                            DLog.i("DMUI", "没有可消费");
                        }
                    } finally {
                        if (mLock.isHeldByCurrentThread()) {
                            mLock.unlock();
                        }
                    }
                } catch (InterruptedException ie) {
                    DLog.i("DMUi", "deal camera data quit!");
                    mDealThreadRun = false;
                }

            }
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            DLog.i("DMUI", "===onImageAvailable===" + Thread.currentThread());
            try (Image image = reader.acquireNextImage()) {
                if (image != null) {
                    if (image.getPlanes() != null) {
                        Image.Plane[] planes = image.getPlanes();
                        if (planes.length > 0) {
                            ByteBuffer yBuffer = planes[0].getBuffer();
//                            ByteBuffer uvBuffer = planes[1].getBuffer();
//                            ByteBuffer vuBuffer = planes[2].getBuffer();
//                            if (mCameraByte.length == byteBuffer.remaining()) {
//                                byteBuffer.get(mCameraByte);
                            if (mLock.tryLock()) {
                                try {
                                    if (!mMakeData) {
                                        mMakeData = true;
                                        DLog.i("DMUI", "生产: y-getRowStride: " + planes[0].getRowStride() + " getPixelStride: " + planes[0].getPixelStride());
//                                        DLog.i("DMUI", "生产: 111getRowStride: " + planes[1].getRowStride() + " getPixelStride: " + planes[1].getPixelStride());
//                                        mCallback.onCopyCameraData(yBuffer, uvBuffer, vuBuffer, mSensorOrientation, planes[0].getRowStride(), image.getWidth(), image.getHeight());
                                        mCallback.onCopyCamera2Data(yBuffer, mSensorOrientation, planes[0].getRowStride(), image.getHeight(), getRatio());
                                    }
                                    mWaitLock.signal();
                                } finally {
                                    if (mLock.isHeldByCurrentThread()) {
                                        mLock.unlock();
                                    }
                                }
                            }
//                            } else {
//                                DLog.e("DMUI", "planes.length is not same,change");
//                                mCameraByte = new byte[byteBuffer.remaining()];
//                        }
                        } else {
                            DLog.i("DMUI", "planes.length <= 0");
                        }
                    } else {
                        DLog.i("DMUI", "image.getPlanes() null");
                    }
                    image.close();
                } else {
                    DLog.i("DMUI", "image null");
                }
            } catch (Exception ex) {
                DLog.i("DMUI", "Exception->" + ex.toString());
            }
        }

    };


    private String mCameraId;

    private CameraCharacteristics mCameraCharacteristics;

    private CameraDevice mCamera;

    private CameraCaptureSession mCaptureSession;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;

    private HandlerThread mBackgroundThread;

    private Thread mDealThread;

    private Handler mBackgroundHandler;

//    private byte[] mCameraByte = new byte[1];

    private int mFacing;

    private boolean mAutoFocus;

    private int mFlash;

    Camera2(Callback callback, PreviewImpl preview, Context context) {
        super(callback, preview);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mPreview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                startCaptureSession();
            }
        });
    }

    @Override
    boolean start() {
        if (!chooseCameraIdByFacing()) {
            return false;
        }
        DLog.i("DMUI", "============Camera2============");
        makeSureStartBackgroundThread();
        collectCameraInfo();
        startOpeningCamera();
        return true;
    }

    @Override
    void stop() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mPreviewRequestBuilder = null;
        stopBackgroundThread();
    }

    private void makeSureStartBackgroundThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
        if (mDealThread == null) {
            mDealThread = new Thread(dealCameraData);
            mDealThread.start();
        }
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundThread.quitSafely();
            } else {
                mBackgroundThread.quit();
            }
            try {
                if (mBackgroundThread != null) {
                    mBackgroundThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mDealThread != null) {
            mDealThreadRun = false;
            mDealThread.interrupt();
            try {
                mDealThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mDealThread = null;
        mBackgroundThread = null;
        mBackgroundHandler = null;
    }

    @Override
    boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }
        mFacing = facing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    int getFacing() {
        return mFacing;
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (mAutoFocus == autoFocus) {
            return;
        }
        mAutoFocus = autoFocus;
        if (mPreviewRequestBuilder != null) {
            updateAutoFocus();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            null, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    mAutoFocus = !mAutoFocus; // Revert
                }
            }
        }
    }

    @Override
    boolean getAutoFocus() {
        return mAutoFocus;
    }

    @Override
    void setFlash(int flash) {
        if (mFlash == flash) {
            return;
        }
        int saved = mFlash;
        mFlash = flash;
        if (mPreviewRequestBuilder != null) {
            updateFlash();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            null, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    mFlash = saved; // Revert
                }
            }
        }
    }

    @Override
    int getFlash() {
        return mFlash;
    }

//    @Override
//    void setDisplayOrientation(int displayOrientation) {
//        mPreview.setDisplayOrientation(displayOrientation);
//    }

    /**
     * <p>Chooses a camera ID by the specified camera facing ({@link #mFacing}).</p>
     * <p>This rewrites {@link #mCameraId}, {@link #mCameraCharacteristics}, and optionally
     * {@link #mFacing}.</p>
     */
    private boolean chooseCameraIdByFacing() {
        try {
            int internalFacing = INTERNAL_FACINGS.get(mFacing);
            final String[] ids = mCameraManager.getCameraIdList();
            if (ids.length == 0) { // No camera
                throw new RuntimeException("No camera available.");
            }
            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer level = characteristics.get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null ||
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    continue;
                }
                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    throw new NullPointerException("Unexpected state: LENS_FACING null");
                }
                if (internal == internalFacing) {
                    mCameraId = id;
                    mCameraCharacteristics = characteristics;
                    return true;
                }
            }
            // Not found
            mCameraId = ids[0];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            Integer level = mCameraCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null ||
                    level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                return false;
            }
            Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (internal == null) {
                throw new NullPointerException("Unexpected state: LENS_FACING null");
            }
            for (int i = 0, count = INTERNAL_FACINGS.size(); i < count; i++) {
                if (INTERNAL_FACINGS.valueAt(i) == internal) {
                    mFacing = INTERNAL_FACINGS.keyAt(i);
                    return true;
                }
            }
            // The operation can reach here when the only camera device is an external one.
            // We treat it as facing back.
            mFacing = Constants.FACING_BACK;
            return true;
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to get a list of camera devices", e);
        }
    }

    /**
     * <p>Collects some information from {@link #mCameraCharacteristics}.</p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }
        mPreviewSizes.clear();
//        for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
//            int width = size.getWidth();
//            int height = size.getHeight();
//            mPreviewSizes.add(new CameraSize(width, height));
//            DLog.i("DMUI", "Size width: " + width + " height: " + height);
//        }
//        SizeMap mPictureSizes = new SizeMap();
//        for (android.util.Size size : map.getOutputSizes(ImageFormat.YUV_420_888)) {
//            mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
//        }
//        for (AspectRatio ratio : mPreviewSizes.ratios()) { // 删除在捕获不支持的尺寸
//            if (!mPictureSizes.ratios().contains(ratio)) {
//                mPreviewSizes.remove(ratio);
//            }
//        }

        if (mPreviewSizes.isEmpty()) { // 确保有数值
            for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
                int width = size.getWidth();
                int height = size.getHeight();
                mPreviewSizes.add(new CameraSize(width, height));
            }
        }
        mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }


    private void prepareImageReader(CameraSize suitableSize) {
        if (mImageReader != null) {
            mImageReader.close();
        }
        mImageReader = ImageReader.newInstance(suitableSize.getWidth(), suitableSize.getHeight(),
                ImageFormat.YUV_420_888, /* maxImages */ 1);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
    }

    /**
     * <p>Starts a capture session for camera preview.</p>
     * <p>This rewrites {@link #mPreviewRequestBuilder}.</p>
     * <p>The result will be continuously processed in {@link #mSessionCallback}.</p>
     */
    private void startCaptureSession() {
        final int width = mPreview.getWidth();
        final int height = mPreview.getHeight();
        if (!isCameraOpened() || !mPreview.isReady() ||
                width == 0 || height == 0 || mPreviewRequestBuilder != null) {
            return;
        }
        dealCameraSize(mSensorOrientation);
        CameraSize suitableSize = getSuitableSize().getSrcSize();
        prepareImageReader(suitableSize);
        DLog.i( "startCaptureSession " + suitableSize.getWidth() + " -> " + suitableSize.getHeight());
            mPreview.setBufferSize(suitableSize.getWidth(), suitableSize.getHeight());
            Surface surface = mPreview.getSurface();
        try {
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
//            mCamera.createCaptureSession(Collections.singletonList(surface),
                mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    mSessionCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to start camera session");
        }
    }

    /**
     * <p>Starts opening a camera device.</p>
     * <p>The result will be processed in {@link #mCameraDeviceCallback}.</p>
     */
    private void startOpeningCamera() {
        try {
            mCameraManager.openCamera(mCameraId, mCameraDeviceCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to open camera: " + mCameraId, e);
        }
    }

    /**
     * Updates the internal state of auto-focus to {@link #mAutoFocus}.
     */
    private void updateAutoFocus() {
        if (mAutoFocus) {
            int[] modes = mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus is not supported
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        } else {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    /**
     * Updates the internal state of flash to {@link #mFlash}.
     */
    private void updateFlash() {
        switch (mFlash) {
            case Constants.FLASH_OFF:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_ON:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_TORCH:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                break;
            case Constants.FLASH_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_RED_EYE:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} for capturing a still picture.
     */
    private static abstract class PictureCaptureCallback
            extends CameraCaptureSession.CaptureCallback {

        static final int STATE_PREVIEW = 0;
        static final int STATE_LOCKING = 1;
        static final int STATE_LOCKED = 2;
        static final int STATE_PRECAPTURE = 3;
        static final int STATE_WAITING = 4;
        static final int STATE_CAPTURING = 5;

        private int mState;

        PictureCaptureCallback() {
        }

        void setState(int state) {
            mState = state;
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

        private void process(@NonNull CaptureResult result) {
            switch (mState) {
                case STATE_LOCKING: {
                    Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (af == null) {
                        break;
                    }
                    if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setState(STATE_CAPTURING);
                            onReady();
                        } else {
                            setState(STATE_LOCKED);
                            onPrecaptureRequired();
                        }
                    }
                    break;
                }
                case STATE_PRECAPTURE: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ||
                            ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setState(STATE_WAITING);
                    }
                    break;
                }
                case STATE_WAITING: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setState(STATE_CAPTURING);
                        onReady();
                    }
                    break;
                }
            }
        }

        /**
         * Called when it is ready to take a still picture.
         */
        public abstract void onReady();

        /**
         * Called when it is necessary to run the precapture sequence.
         */
        public abstract void onPrecaptureRequired();

    }

}
