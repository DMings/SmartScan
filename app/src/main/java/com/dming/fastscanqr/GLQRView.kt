package com.dming.fastscanqr

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.dming.fastscanqr.utils.DLog
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.android.synthetic.main.layout_gl_qr.view.*
import java.nio.ByteBuffer


class GLQRView : FrameLayout {

    private val mGLCameraManager = GLCameraManager()
    private val mReader = QRCodeReader()
    private var mTop: Float = 0f
    private var mSize: Float = 0f
    private var mIsHasScanLine: Boolean = false
    private var mAnimator: ObjectAnimator? = null
    private var onGrayImg: ((width: Int, height: Int, grayByteBuffer: ByteBuffer) -> Unit)? = null
    private var onResult: ((text: String) -> Unit)? = null
    private var onCropLocation: ((rect: Rect) -> Unit)? = null

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

    private fun initAttribute(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.GLQRView)
            var scanSize = typedArray.getFloat(R.styleable.GLQRView_scanPercentSize, 0f)
            var scanTopOffset = typedArray.getFloat(R.styleable.GLQRView_scanPercentTopOffset, 0f)
            DLog.i("111scanSize: $scanSize  scanTopOffset: $scanTopOffset")
            scanSize = if (scanSize == 0f) {
                typedArray.getDimension(R.styleable.GLQRView_scanSize, 0f)
            } else {
                scanSize
            }
            scanTopOffset = if (scanTopOffset == 0f) {
                typedArray.getDimension(R.styleable.GLQRView_scanTopOffset, 0f)
            } else {
                scanTopOffset
            }
            DLog.i("222scanSize: $scanSize  scanTopOffset: $scanTopOffset")
            val scanLine =
                typedArray.getDrawable(R.styleable.GLQRView_scanLine)
            val scanCrop =
                typedArray.getDrawable(R.styleable.GLQRView_scanCrop)
            val scanBackground = typedArray.getColor(
                R.styleable.GLQRView_scanBackground,
                context.resources.getColor(R.color.scanBackground)
            )
            typedArray.recycle()
            if (scanLine != null) {
                iv_scan_progress.setImageDrawable(scanLine)
                mIsHasScanLine = true
            }
            if (scanCrop != null) {
                iv_get_img.setImageDrawable(scanCrop)
            }
            mTop = scanTopOffset
            mSize = scanSize
            v_left.setBackgroundColor(scanBackground)
            v_top.setBackgroundColor(scanBackground)
            v_right.setBackgroundColor(scanBackground)
            v_bottom.setBackgroundColor(scanBackground)
        }
    }

    private fun changeVieConfigure(
        top: Float,
        size: Float,
        maxWidth: Int,
        maxHeight: Int,
        hasScanLin: Boolean
    ): Rect {
        val viewConfigure =
            GLCameraManager.getViewConfigure(top, size, maxWidth, maxHeight)
        post {
            DLog.i("333scanSize: ${viewConfigure.width()}  scanTopOffset: ${viewConfigure.top}")
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
        return viewConfigure
    }

    private fun initView(attrs: AttributeSet?) {
        View.inflate(context, R.layout.layout_gl_qr, this)
        initAttribute(attrs)
        mGLCameraManager.init(context)
        glSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder?) {
                DLog.e("surfaceCreated")
                mGLCameraManager.surfaceCreated(context, holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                DLog.e("surfaceChanged")
                val viewConfigure = changeVieConfigure(mTop, mSize, width, height, mIsHasScanLine)
                if (onCropLocation != null) {
                    post {
                        onCropLocation!!(viewConfigure)
                    }
                }
                mGLCameraManager.onSurfaceChanged(width, height)
                mGLCameraManager.changeQRConfigure(mTop, mSize)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                DLog.e("surfaceDestroyed")
                mGLCameraManager.surfaceDestroyed()
                mAnimator?.cancel()
            }

        })
    }

    private fun initEvent() {
        mGLCameraManager.setParseQRListener { width: Int, height: Int, source: GLRGBLuminanceSource, grayByteBuffer: ByteBuffer ->
            try {
                if (this.onGrayImg != null) {
                    this.onGrayImg!!(width, height, grayByteBuffer)
                }
                val start = System.currentTimeMillis()
                source.setData(grayByteBuffer)
                val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                val result = mReader.decode(binaryBitmap)// 开始解析
                DLog.i("width: $width height: $height decode cost time: ${System.currentTimeMillis() - start}  result: ${result.text}")
                if (this.onResult != null && result != null) {
                    this.onResult!!(result.text)
                }
            } catch (e: NotFoundException) {
//                e.printStackTrace()
            } catch (e: ChecksumException) {
//                e.printStackTrace()
            } catch (e: FormatException) {
//                e.printStackTrace()
            } finally {
                mReader.reset()
            }
        }
    }

    fun setGrayImgListener(
        onGrayImg: (
            width: Int, height: Int, grayByteBuffer: ByteBuffer
        ) -> Unit
    ) {
        this.onGrayImg = onGrayImg
    }

    fun setResultListener(onResult: (text: String) -> Unit) {
        this.onResult = onResult
    }

    fun setCropLocationListener(onCropLocation: ((rect: Rect) -> Unit)) {
        this.onCropLocation = onCropLocation
    }

    fun changeQRConfigure(
        top: Float,
        size: Float
    ) {
        mGLCameraManager.changeQRConfigure(top, size)
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