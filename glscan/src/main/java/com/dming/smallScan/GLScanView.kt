package com.dming.smallScan

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
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
    private var mTop: Float = 0f
    private var mWidth: Float = 0f
    private var mHeight: Float = 0f
    private var mIsUseMinSize: Boolean = false
    private var mIsHasScanLine: Boolean = false
    private var mAnimator: ObjectAnimator? = null
    private var mOnGrayImg: ((width: Int, height: Int, grayByteBuffer: ByteBuffer) -> Unit)? = null
    private var mOnDecodeThreadResult: ((text: String) -> Unit)? = null
    private var mOnUIThreadResult: ((text: String) -> Unit)? = null
    private var mOnScanViewListener: OnScanViewListener? = null
    private var mDecodeOnce: Boolean = false
    private var mCanDecode: Boolean = false
    //
    private var mQRReader: QRCodeReader? = null
    private var mOneReader: MultiFormatOneDReader? = null

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
            val scanLine =
                typedArray.getDrawable(R.styleable.GLScanView_scanLine)
            val scanCrop =
                typedArray.getDrawable(R.styleable.GLScanView_scanCrop)
            val scanBackground = typedArray.getColor(
                R.styleable.GLScanView_scanBackground,
                context.resources.getColor(R.color.scanBackground)
            )
            val addOneDCode = typedArray.getBoolean(R.styleable.GLScanView_addOneDCode, false)
            val onlyOneDCode = typedArray.getBoolean(R.styleable.GLScanView_onlyOneDCode, false)
            mIsUseMinSize = typedArray.getBoolean(R.styleable.GLScanView_useMinSize, false)

            if (addOneDCode || onlyOneDCode) {
                mOneReader = MultiFormatOneDReader(null)
            }
            if (!onlyOneDCode) {
                mQRReader = QRCodeReader()
            }

            typedArray.recycle()
            if (scanLine != null) {
                iv_scan_progress.setImageDrawable(scanLine)
                mIsHasScanLine = true
            }
            if (scanCrop != null) {
                iv_get_img.setImageDrawable(scanCrop)
            }
            mTop = scanTopOffset
            mWidth = scanWidth
            mHeight = scanHeight
            v_left.setBackgroundColor(scanBackground)
            v_top.setBackgroundColor(scanBackground)
            v_right.setBackgroundColor(scanBackground)
            v_bottom.setBackgroundColor(scanBackground)
        }
    }

    private fun changeVieConfigure(
        t: Float,
        ws: Float,
        hs: Float,
        maxWidth: Int,
        maxHeight: Int,
        hasScanLin: Boolean
    ): Rect {
        val viewConfigure =
            ScanLayout.getViewConfigure(t, ws, hs, maxWidth, maxHeight, mIsUseMinSize)
        post {
            DLog.i("scanSize: ${viewConfigure.width()}  scanTopOffset: ${viewConfigure.top}")
            fl_get_img.layoutParams =
                LinearLayout.LayoutParams(viewConfigure.width(), viewConfigure.height())
            v_top.layoutParams =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, viewConfigure.top)
        }
        if (hasScanLin) {
            mAnimator =
                ObjectAnimator.ofFloat(
                    iv_scan_progress,
                    "translationY", 0f, viewConfigure.height().toFloat()
                )
            mAnimator?.let { animator ->
                mAnimator?.cancel()
                animator.duration = 3000
                animator.repeatMode = ValueAnimator.RESTART
                animator.repeatCount = ValueAnimator.INFINITE
                animator.start()
            }
        }
        scannerView.setScannerRect(viewConfigure)
        return viewConfigure
    }

    private fun initView(attrs: AttributeSet?) {
        View.inflate(context, R.layout.layout_gl_qr, this)
        initAttribute(attrs)
        mGLCameraManager.init(context)
        glSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder?) {
                DLog.e("surfaceCreated")
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
                DLog.e("surfaceChanged")
                mCanDecode = true
                val viewConfigure =
                    changeVieConfigure(mTop, mWidth, mHeight, width, height, mIsHasScanLine)
                if (mOnScanViewListener != null) {
                    post { mOnScanViewListener?.onChange(viewConfigure) }
                }
                mGLCameraManager.onSurfaceChanged(width, height)
                mGLCameraManager.changeQRConfigure(mTop, mWidth, mHeight, mIsUseMinSize)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                DLog.e("surfaceDestroyed")
                if (mOnScanViewListener != null) {
                    post { mOnScanViewListener?.onDestroy() }
                }
                mGLCameraManager.surfaceDestroyed()
                mAnimator?.cancel()
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
                    if (this.mOnDecodeThreadResult != null) {
                        if (mDecodeOnce) {
                            mCanDecode = false
                            this.mOnDecodeThreadResult!!(result.text)
                        }
                    }
                    if (this.mOnUIThreadResult != null) {
                        if (mDecodeOnce) {
                            mCanDecode = false
                            post {
                                this.mOnUIThreadResult!!(result.text)
                            }
                        }
                    }
                }
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(context, this)
        glSurfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    private fun decodeBinaryBitmap(binaryBitmap: BinaryBitmap): Result? {
        try {
            return mQRReader?.decode(binaryBitmap)
        } catch (re: ReaderException) {
            // continue
        } finally {
            mQRReader?.reset()
        }
        try {
            return mOneReader?.decode(binaryBitmap)
        } catch (re: ReaderException) {
            // continue
        } finally {
            mOneReader?.reset()
        }
        return null
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

    fun setCropLocationListener(onScanViewListener: OnScanViewListener) {
        this.mOnScanViewListener = onScanViewListener
    }

    fun changeQRConfigure(
        t: Float,
        ws: Float,
        hs: Float
    ) {
        mGLCameraManager.changeQRConfigure(t, ws, hs, mIsUseMinSize)
    }

    fun setFlashLight(on: Boolean): Boolean {
        return mGLCameraManager.setFlashLight(on)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        DLog.e("onDetachedFromWindow")
        mGLCameraManager.destroy()
    }

}