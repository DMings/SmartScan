package com.dming.smallScan

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import com.dming.smallScan.utils.DLog
import com.google.zxing.BinaryBitmap
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.oned.MultiFormatOneDReader
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.android.synthetic.main.layout_gl_qr.view.*
import java.nio.ByteBuffer

@Suppress("UNUSED")
class GLScanView : FrameLayout, ScaleGestureDetector.OnScaleGestureListener {

    private val mGLCameraManager = GLCameraManager()
    private var mScanTop: Float = 0f
    private var mScanWidth: Float = 0f
    private var mScanHeight: Float = 0f
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mScanMustSquare: Boolean = false
    private var mOnGrayImg: ((width: Int, height: Int, grayByteBuffer: ByteBuffer) -> Unit)? = null
    private var mOnDecodeThreadResult: ((text: String) -> Unit)? = null
    private var mOnUIThreadResult: ((text: String) -> Unit)? = null
    private var mOnScanViewListener: OnScanViewListener? = null
    private var mDecodeOnce: Boolean = false
    private var mCanDecode: Boolean = false
    //
    private var mQRReader: QRCodeReader? = null
    private var mOneReader: MultiFormatOneDReader? = null
    //
    private var mBeepVibrateManager: BeepVibrateManager? = null
    private var mFlashLightBtnSize: Int = 0
    private var mDisableScale: Boolean = false

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs)
        initEvent()
    }

    @Suppress("DEPRECATION")
    private fun initAttribute(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.GLScanView)
            //
            var scanWidth = typedArray.getFloat(R.styleable.GLScanView_scanPercentWidth, 0f)
            scanWidth = if (scanWidth == 0f) {
                typedArray.getDimension(R.styleable.GLScanView_scanWidth, 0f)
            } else {
                scanWidth
            }
            //
            var scanHeight = typedArray.getFloat(R.styleable.GLScanView_scanPercentHeight, 0f)
            scanHeight = if (scanHeight == 0f) {
                typedArray.getDimension(R.styleable.GLScanView_scanHeight, 0f)
            } else {
                scanHeight
            }
            // <<----
            var scanTopOffset = typedArray.getFloat(R.styleable.GLScanView_scanPercentTopOffset, 0f)
            scanTopOffset = if (scanTopOffset == 0f) {
                typedArray.getDimension(R.styleable.GLScanView_scanTopOffset, 0f)
            } else {
                scanTopOffset
            }
            mScanMustSquare = typedArray.getBoolean(R.styleable.GLScanView_scanMustSquare, true)

            val enableFlashlightBtn =
                typedArray.getBoolean(R.styleable.GLScanView_enableFlashlightBtn, false)
            mDisableScale = typedArray.getBoolean(R.styleable.GLScanView_disableScale, false)

            val enableBeep = typedArray.getBoolean(R.styleable.GLScanView_enableBeep, false)
            val enableVibrate = typedArray.getBoolean(R.styleable.GLScanView_enableVibrate, false)
            if ((enableBeep || enableVibrate) && context is Activity) {
                mBeepVibrateManager =
                    BeepVibrateManager(context as Activity, enableBeep, enableVibrate)
            }

            val addOneDCode = typedArray.getBoolean(R.styleable.GLScanView_addOneDCode, false)
            val onlyOneDCode = typedArray.getBoolean(R.styleable.GLScanView_onlyOneDCode, false)

            if (addOneDCode || onlyOneDCode) {
                mOneReader = MultiFormatOneDReader(null)
            }
            if (!onlyOneDCode) {
                mQRReader = QRCodeReader()
            }
            scannerView.initWithAttribute(typedArray)
            flashlightInit(enableFlashlightBtn)
            typedArray.recycle()
            mScanTop = scanTopOffset
            mScanWidth = scanWidth
            mScanHeight = scanHeight
        }
    }

    private fun initView(attrs: AttributeSet?) {
        View.inflate(context, R.layout.layout_gl_qr, this)
        initAttribute(attrs)
        mGLCameraManager.init(context)
        glSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder?) {
                if (mOnScanViewListener != null) {
                    post { mOnScanViewListener?.onCreate() }
                }
                mGLCameraManager.surfaceCreated(context, holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                mCanDecode = true
                mViewWidth = width
                mViewHeight = height
                mGLCameraManager.onSurfaceChanged(mViewWidth, mViewHeight)
                changeScanConfigure()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                if (mOnScanViewListener != null) {
                    post { mOnScanViewListener?.onDestroy() }
                }
                mGLCameraManager.surfaceDestroyed()
            }

        })
    }

    private fun initEvent() {
        mGLCameraManager.setParseQRListener { width: Int, height: Int, source: GLRGBLuminanceSource, grayByteBuffer: ByteBuffer ->
            if (this.mOnGrayImg != null) {
                this.mOnGrayImg!!(width, height, grayByteBuffer)
            }
            if (mCanDecode && (this.mOnDecodeThreadResult != null || this.mOnUIThreadResult != null)) {
                val start = System.currentTimeMillis()
                source.setData(grayByteBuffer)
                val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                val result = decodeBinaryBitmap(binaryBitmap)
                if (result != null) {
                    DLog.i("width: $width height: $height decode cost time: ${System.currentTimeMillis() - start}  result: ${result.text}")
                    if (mDecodeOnce) {
                        mCanDecode = false
                        mBeepVibrateManager?.playBeepSoundAndVibrate()
                        this.mOnDecodeThreadResult?.let { onDecodeThreadResult ->
                            onDecodeThreadResult(result.text)
                        }
                        post {
                            this.mOnUIThreadResult?.let { onUIThreadResult ->
                                onUIThreadResult(result.text)
                            }
                        }
                    } else {
                        this.mOnDecodeThreadResult?.let { onDecodeThreadResult ->
                            onDecodeThreadResult(result.text)
                        }
                        post {
                            this.mOnUIThreadResult?.let { onUIThreadResult ->
                                onUIThreadResult(result.text)
                            }
                        }
                    }
                }
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(context, this)
        glSurfaceView.setOnTouchListener { _, event ->
            if (mDisableScale) {
                false
            } else {
                scaleGestureDetector.onTouchEvent(event)
            }
        }
    }

    private fun decodeBinaryBitmap(binaryBitmap: BinaryBitmap): Result? {
        try {
            val result = mQRReader?.decode(binaryBitmap)
            if (result != null) {
                return result
            }
        } catch (re: ReaderException) {
            // continue
        } finally {
            mQRReader?.reset()
        }
        try {
            val result = mOneReader?.decode(binaryBitmap)
            if (result != null) {
                return result
            }
        } catch (re: ReaderException) {
            // continue
        } finally {
            mOneReader?.reset()
        }
        return null
    }

    private fun changeScanConfigure() {
        val viewConfigure = ScanLayoutLocation.getViewConfigure(
            mScanTop,
            mScanWidth,
            mScanHeight,
            mViewWidth,
            mViewHeight,
            mScanMustSquare
        )
        // 改变扫描框
        scannerView.changeScanConfigure(viewConfigure)
        // 改变读取像素
        mGLCameraManager.changeScanConfigure(
            mScanTop,
            mScanWidth,
            mScanHeight,
            mScanMustSquare
        )
        // 回调变化
        if (mOnScanViewListener != null) {
            post { mOnScanViewListener?.onChange(viewConfigure) }
        }
        // 改变闪光灯按钮
        onFlashlightLayoutChange(viewConfigure)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        mGLCameraManager.onScaleChange(detector.scaleFactor)
        return true
    }

    @Suppress("unused")
    fun setGrayImgListener(
        mOnGrayImg: (
            width: Int, height: Int, grayByteBuffer: ByteBuffer
        ) -> Unit
    ) {
        this.mOnGrayImg = mOnGrayImg
    }

    fun setOnResultInThreadListener(mOnResult: (text: String) -> Unit) {
        this.mOnDecodeThreadResult = mOnResult
        this.mDecodeOnce = false
    }

    fun setOnResultOnceInThreadListener(mOnResult: (text: String) -> Unit) {
        this.mOnDecodeThreadResult = mOnResult
        this.mDecodeOnce = true
    }

    fun setOnResultListener(mOnResult: (text: String) -> Unit) {
        this.mOnUIThreadResult = mOnResult
        this.mDecodeOnce = false
    }

    fun setOnResultOnceListener(mOnResult: (text: String) -> Unit) {
        this.mOnUIThreadResult = mOnResult
        this.mDecodeOnce = true
    }

    fun startDecode() {
        mCanDecode = true
    }

    fun stopDecode() {
        mCanDecode = false
    }

    fun setViewConfigure(
        topOffset: Float,
        scanWidth: Float,
        scanHeight: Float,
        scanMustSquare: Boolean
    ) {
        mScanTop = topOffset
        mScanWidth = scanWidth
        mScanHeight = scanHeight
        mScanMustSquare = scanMustSquare
        changeScanConfigure()
    }

    fun setDecodeConfigure(
        onlyOneDCode: Boolean,
        addOneDCode: Boolean = false
    ) {
        mOneReader = if (addOneDCode || onlyOneDCode) {
            MultiFormatOneDReader(null)
        } else {
            null
        }
        mQRReader = if (!onlyOneDCode) {
            QRCodeReader()
        } else {
            null
        }
    }

    fun setCornerLocationListener(onScanViewListener: OnScanViewListener) {
        this.mOnScanViewListener = onScanViewListener
    }

    @Suppress("unused")
    fun setFlashLight(on: Boolean): Boolean {
        return mGLCameraManager.setFlashLight(on)
    }

    private fun flashlightInit(enableFlashlightBtn: Boolean) {
        if (enableFlashlightBtn) {
            mFlashLightBtnSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50f,
                this.resources.displayMetrics
            ).toInt()
            btn_flash.visibility = View.VISIBLE
            val padding = (mFlashLightBtnSize / 5)
            btn_flash.setPadding(padding, padding, padding, padding)
            val layoutParams = btn_flash.layoutParams
            layoutParams.width = mFlashLightBtnSize
            layoutParams.height = mFlashLightBtnSize
            btn_flash.layoutParams = layoutParams
            btn_flash.setOnClickListener {
                if (btn_flash.tag != "on") {
                    if (setFlashLight(true)) {
                        btn_flash.tag = "on"
                        btn_flash.setImageResource(R.drawable.smart_scan_flashlight_on)
                    }
                } else {
                    if (setFlashLight(false)) {
                        btn_flash.tag = "off"
                        btn_flash.setImageResource(R.drawable.smart_scan_flashlight_off)
                    }
                }
            }
        } else {
            mFlashLightBtnSize = 0
        }
    }

    private fun onFlashlightLayoutChange(rect: Rect) {
        if (mFlashLightBtnSize > 0) {
            val x = (rect.left + rect.width() / 2).toFloat()
            btn_flash.x = x - mFlashLightBtnSize / 2
            btn_flash.y = rect.bottom - mFlashLightBtnSize * 1.2f
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mGLCameraManager.destroy()
        mBeepVibrateManager?.close()
    }

}