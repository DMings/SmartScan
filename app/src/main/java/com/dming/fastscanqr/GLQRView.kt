package com.dming.fastscanqr

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.TypedValue
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
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

    companion object {
        private const val TOP: Float = 100f
        private const val SIZE_PERCENT: Float = 0.65f
    }

    private val mCameraHelper: CameraHelper = CameraHelper()
    private val mReader = QRCodeReader()
    private var mTop: Int = 0
    private var mSize: Int = 0
    private var mAnimator: ObjectAnimator? = null
    // test
//    private lateinit var mTestImgBitmap: Bitmap

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
        afterInitQRView()
        initEvent()
    }

    private fun afterInitQRView() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outSize = Point()
        wm.defaultDisplay.getRealSize(outSize)
        mSize = (outSize.x * SIZE_PERCENT).toInt()
        mTop = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TOP,
            context.resources.displayMetrics
        ).toInt()
        fl_get_img.layoutParams = LinearLayout.LayoutParams(mSize, mSize)
        v_top.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mTop)
        mAnimator = ObjectAnimator.ofFloat(iv_scan_progress, "translationY", 0f, mSize.toFloat())
        mAnimator?.let {
            it.duration = 3000
            it.repeatMode = ValueAnimator.RESTART
            it.repeatCount = ValueAnimator.INFINITE
            it.start()
        }
    }

    private fun initView() {
        View.inflate(context, R.layout.layout_gl_qr, this)
        mCameraHelper.init(context)
        glSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder?) {
                DLog.e("surfaceCreated")
                mCameraHelper.surfaceCreated(context, holder)
                mAnimator?.start()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                DLog.e("surfaceChanged")
                mCameraHelper.onSurfaceChanged(width, height)
                mCameraHelper.changeQRConfigure(mTop, mSize)
                // test
//                mTestImgBitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                DLog.e("surfaceDestroyed")
                mCameraHelper.surfaceDestroyed()
                mAnimator?.cancel()
            }

        })
    }

    private fun initEvent() {
        mCameraHelper.setParseQRListener { width: Int, height: Int, source: GLRGBLuminanceSource, grayByteBuffer: ByteBuffer ->
            try {
                // test
//                mTestImgBitmap.copyPixelsFromBuffer(grayByteBuffer)
//                post {
//                    testImg.setImageBitmap(mTestImgBitmap)
//                }
                val start = System.currentTimeMillis()
                source.setData(grayByteBuffer)
                val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                val result = mReader.decode(binaryBitmap)// 开始解析
                DLog.i("width: $width height: $height decode cost time: ${System.currentTimeMillis() - start}  result: ${result.text}")
                post {
                    Toast.makeText(
                        context,
                        "width: $width height: $height decode cost time: ${System.currentTimeMillis() - start}  result: ${result.text}",
                        Toast.LENGTH_SHORT
                    ).show()
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
        btn_flash.setOnClickListener {
            if (btn_flash.tag != "on") {
                if (mCameraHelper.setFlashLight(true)) {
                    btn_flash.tag = "on"
                    btn_flash.setImageResource(R.drawable.flashlight_on)
                }
            } else {
                if (mCameraHelper.setFlashLight(false)) {
                    btn_flash.tag = "off"
                    btn_flash.setImageResource(R.drawable.flashlight_off)
                }
            }
        }
    }

    fun changeQRConfigure(
        top: Int,
        size: Int
    ) {
        mCameraHelper.changeQRConfigure(top, size)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        DLog.e("onDetachedFromWindow")
        mCameraHelper.destroy()
    }

}