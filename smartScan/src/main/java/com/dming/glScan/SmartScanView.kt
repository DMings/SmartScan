package com.dming.glScan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.dming.glScan.camera.GLCameraManager
import com.dming.glScan.zxing.GLRGBLuminanceSource
import com.dming.glScan.zxing.OnGrayImgListener
import com.dming.glScan.zxing.OnResultListener
import com.google.zxing.BinaryBitmap
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.oned.MultiFormatOneDReader
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.android.synthetic.main.layout_gl_qr.view.*
import java.nio.ByteBuffer


/**
 * 扫码View核心类
 */
class SmartScanView : FrameLayout, ScaleGestureDetector.OnScaleGestureListener {

    private var mSmartScanParameter = SmartScanParameter()
    private val mGLCameraManager = GLCameraManager()
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mOnGrayImg: OnGrayImgListener? = null
    //    private var mOnDecodeThreadResult: ((text: String) -> Unit)? = null
    private var mOnDecodeThreadResult: OnResultListener? = null
    private var mOnUIThreadResult: OnResultListener? = null
    private var mOnScanViewListener: OnScanViewListener? = null
    private var mDecodeOnce: Boolean = false
    private var mCanDecode: Boolean = false
    //
    private var mQRReader: QRCodeReader? = null
    private var mOneReader: MultiFormatOneDReader? = null
    //
    private var mBeepVibrateManager: BeepVibrateManager? = null
    private var mFlashLightBtnSize: Int = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        if (context is Activity) {
            val activity = context as Activity;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(activity, "请授予摄像头权限，否则无法正常使用", Toast.LENGTH_LONG).show()
                    return
                }
            }
        }
        if (attrs != null) {
            handleAttribute(attrs)
            init(mSmartScanParameter)
        }
    }

    /**
     * 设置默认（缺失）参数
     */
    private fun setDefaultParameter(smartScanParameter: SmartScanParameter) {
        mSmartScanParameter = smartScanParameter
        val oneDP = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f,
            this.resources.displayMetrics
        )
        if (mSmartScanParameter.scanCornerSize == 0f) {
            mSmartScanParameter.scanCornerSize = 18 * oneDP
        }
        if (mSmartScanParameter.scanCornerThick == 0f) {
            mSmartScanParameter.scanCornerThick = 3 * oneDP
        }
        if (mSmartScanParameter.scanLineWidth == 0f) {
            mSmartScanParameter.scanLineWidth = 5 * oneDP
        }
        if (mSmartScanParameter.scanMustSquare == null) {
            mSmartScanParameter.scanMustSquare = true
        }
        if (mSmartScanParameter.scanLineTime == null) {
            mSmartScanParameter.scanLineTime = 3000
        }
        if (mSmartScanParameter.scanBackgroundColor == null) {
            mSmartScanParameter.scanBackgroundColor =
                context.resources.getColor(R.color.smartScanBackgroundColor)
        }
        if (mSmartScanParameter.scanFrameLineColor == null) {
            mSmartScanParameter.scanFrameLineColor =
                context.resources.getColor(R.color.smartScanFrameColor)
        }
        if (mSmartScanParameter.scanLineColor == null) {
            mSmartScanParameter.scanLineColor =
                context.resources.getColor(R.color.smartScanColor)
        }
        if (mSmartScanParameter.scanCornerColor == null) {
            mSmartScanParameter.scanCornerColor =
                context.resources.getColor(R.color.smartScanColor)
        }
        if (mSmartScanParameter.scanFrameLineWidth == null) {
            mSmartScanParameter.scanFrameLineWidth = 1f
        }
    }

    /**
     * 当在xml创建的时候获取attribute中属性
     */
    private fun handleAttribute(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.SmartScanView)
            val scanPercentWidth =
                typedArray.getFloat(R.styleable.SmartScanView_scanPercentWidth, 0f)
            val scanWidth = typedArray.getDimension(R.styleable.SmartScanView_scanWidth, 0f)
            val scanPercentHeight =
                typedArray.getFloat(R.styleable.SmartScanView_scanPercentHeight, 0f)
            val scanHeight = typedArray.getDimension(R.styleable.SmartScanView_scanHeight, 0f)
            val scanPercentTopOffset =
                typedArray.getFloat(R.styleable.SmartScanView_scanPercentTopOffset, 0f)
            val scanTopOffset = typedArray.getDimension(R.styleable.SmartScanView_scanTopOffset, 0f)
            val scanMustSquare =
                typedArray.getBoolean(R.styleable.SmartScanView_scanMustSquare, true)

            val enableFlashlightBtn =
                typedArray.getBoolean(R.styleable.SmartScanView_enableFlashlightBtn, false)
            val disableScale = typedArray.getBoolean(R.styleable.SmartScanView_disableScale, false)

            val enableBeep = typedArray.getBoolean(R.styleable.SmartScanView_enableBeep, false)
            val enableVibrate =
                typedArray.getBoolean(R.styleable.SmartScanView_enableVibrate, false)

            val addOneDCode = typedArray.getBoolean(R.styleable.SmartScanView_addOneDCode, false)
            val onlyOneDCode = typedArray.getBoolean(R.styleable.SmartScanView_onlyOneDCode, false)

            val scanLine =
                typedArray.getDrawable(R.styleable.SmartScanView_scanLine)
            val scanCorner =
                typedArray.getDrawable(R.styleable.SmartScanView_scanCorner)
            var scanCornerSize = 0f
            var scanCornerThick = 0f
            if (scanCorner == null) { // 有扫描框图片用图片
                //角长度
                scanCornerSize =
                    typedArray.getDimension(R.styleable.SmartScanView_scanCornerSize, 0f)
                //角宽
                scanCornerThick =
                    typedArray.getDimension(R.styleable.SmartScanView_scanCornerThick, 0f)
            }
            // 扫描线尺寸
            val scanLineWidth =
                typedArray.getDimension(R.styleable.SmartScanView_scanLineWidth, 0f)
            // 扫描线时间
            val scanLineTime =
                typedArray.getInt(R.styleable.SmartScanView_scanLineTime, 3000)
            // 扫描线颜色
            val scanLineColor = typedArray.getColor(
                R.styleable.SmartScanView_scanLineColor,
                context.resources.getColor(R.color.smartScanColor)
            )
            // 扫描角颜色
            val scanCornerColor = typedArray.getColor(
                R.styleable.SmartScanView_scanCornerColor,
                context.resources.getColor(R.color.smartScanColor)
            )
            // 框线宽
            val scanFrameLineWidth =
                typedArray.getDimension(R.styleable.SmartScanView_scanFrameLineWidth, 1f)
            // 背景色
            val scanBackgroundColor = typedArray.getColor(
                R.styleable.SmartScanView_scanBackgroundColor,
                context.resources.getColor(R.color.smartScanBackgroundColor)
            )
            // 扫描框线
            val scanFrameLineColor = typedArray.getColor(
                R.styleable.SmartScanView_scanFrameLineColor,
                context.resources.getColor(R.color.smartScanFrameColor)
            )
            typedArray.recycle()
            mSmartScanParameter.apply {
                this.scanWidth = scanWidth
                this.scanHeight = scanHeight
                this.scanPercentWidth = scanPercentWidth
                this.scanPercentHeight = scanPercentHeight
                this.scanTopOffset = scanTopOffset
                this.scanPercentTopOffset = scanPercentTopOffset
                this.scanLine = scanLine
                this.scanCorner = scanCorner
                this.scanBackgroundColor = scanBackgroundColor
                this.addOneDCode = addOneDCode
                this.onlyOneDCode = onlyOneDCode
                this.scanMustSquare = scanMustSquare
                this.scanCornerSize = scanCornerSize
                this.scanCornerThick = scanCornerThick
                this.scanLineWidth = scanLineWidth
                this.scanLineTime = scanLineTime
                this.scanFrameLineWidth = scanFrameLineWidth
                this.scanFrameLineColor = scanFrameLineColor
                this.scanLineColor = scanLineColor
                this.scanCornerColor = scanCornerColor
                this.disableScale = disableScale
                this.enableBeep = enableBeep
                this.enableVibrate = enableVibrate
                this.enableFlashlightBtn = enableFlashlightBtn
            }
        }
    }

    /**
     * 传入属性进行初始化
     */
    fun init(smartScanParameter: SmartScanParameter) {
        setDefaultParameter(smartScanParameter)
        initView()
        initEvent()
    }

    /**
     * 初始化view部分
     */
    private fun initView() {
        View.inflate(context, R.layout.layout_gl_qr, this)
        mGLCameraManager.init(context)
        initFlashlight()
        updateConfigure(mSmartScanParameter)
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

    /**
     * 初始化事件部分
     */
    private fun initEvent() {
        mGLCameraManager.setOnReadScanDateListener { width: Int, height: Int, source: GLRGBLuminanceSource, grayByteBuffer: ByteBuffer ->
            this.mOnGrayImg?.onGrayImg(width, height, grayByteBuffer)
            if (mCanDecode && (this.mOnDecodeThreadResult != null || this.mOnUIThreadResult != null)) {
//                val start = System.currentTimeMillis()
                source.setData(grayByteBuffer)
                val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                val result = decodeBinaryBitmap(binaryBitmap)
                if (result != null) {
//                    DLog.i("width: $width height: $height decode cost time: ${System.currentTimeMillis() - start}  result: ${result.text}")
                    if (mDecodeOnce) {
                        mCanDecode = false
                        mBeepVibrateManager?.playBeepSoundAndVibrate()
                        this.mOnDecodeThreadResult?.onResult(result)
                        post {
                            this.mOnUIThreadResult?.onResult(result)
                        }
                    } else {
                        this.mOnDecodeThreadResult?.onResult(result)
                        post {
                            this.mOnUIThreadResult?.onResult(result)
                        }
                    }
                }
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(context, this)
        glSurfaceView.setOnTouchListener { _, event ->
            if (mSmartScanParameter.disableScale) {
                false
            } else {
                scaleGestureDetector.onTouchEvent(event)
            }
        }
    }

    /**
     * 解码二值化的图像
     */
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

    /**
     * 改变扫码view的配置
     */
    private fun changeScanConfigure() {
        val viewConfigure = ScannerLayout.getViewConfigure(
            mSmartScanParameter,
            mViewWidth,
            mViewHeight
        )
        // 改变扫描框
        scannerView.changeScanConfigure(mSmartScanParameter, viewConfigure)
        // 改变读取像素
        mGLCameraManager.changeScanConfigure(mSmartScanParameter)
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

    /**
     * 设置获取亮度（灰度）图片监听
     */
    fun setGrayImgListener(onGrayImg: OnGrayImgListener) {
        this.mOnGrayImg = onGrayImg
    }

    /**
     * 从解码线程中监听解码结果，会不断的触发，可使用stopDecode()关闭
     */
    fun setOnResultInThreadListener(onResultListener: OnResultListener) {
        this.mOnDecodeThreadResult = onResultListener
        this.mDecodeOnce = false
    }

    /**
     * 从解码线程中监听解码结果，成功后会停止，可使用startDecode()开启
     */
    fun setOnResultOnceInThreadListener(onResultListener: OnResultListener) {
        this.mOnDecodeThreadResult = onResultListener
        this.mDecodeOnce = true
    }

    /**
     * 从UI线程中监听解码结果，会不断的触发，可使用stopDecode()关闭
     */
    fun setOnResultListener(onResultListener: OnResultListener) {
        this.mOnUIThreadResult = onResultListener
        this.mDecodeOnce = false
    }

    /**
     * 从UI线程中监听解码结果，成功后会停止，可使用startDecode()开启
     */
    fun setOnResultOnceListener(onResultListener: OnResultListener) {
        this.mOnUIThreadResult = onResultListener
        this.mDecodeOnce = true
    }

    /**
     * 开启解码
     */
    fun startDecode() {
        mCanDecode = true
    }

    /**
     * 关闭解码
     */
    fun stopDecode() {
        mCanDecode = false
    }

    /**
     * 监听smart的扫码窗口改变，自定义窗口极奇重要的监听
     */
    fun setScanViewChangeListener(onScanViewListener: OnScanViewListener) {
        this.mOnScanViewListener = onScanViewListener
    }

    /**
     * 控制闪光灯开启关闭
     */
    fun setFlashLight(on: Boolean): Boolean {
        return mGLCameraManager.setFlashLight(on)
    }

    /**
     * 初始化内部的闪光灯按钮大小、位置
     */
    private fun initFlashlight() {
        mFlashLightBtnSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 50f,
            this.resources.displayMetrics
        ).toInt()
        val padding = (mFlashLightBtnSize / 5)
        flashBtn.setPadding(padding, padding, padding, padding)
        val layoutParams = flashBtn.layoutParams
        layoutParams.width = mFlashLightBtnSize
        layoutParams.height = mFlashLightBtnSize
        flashBtn.layoutParams = layoutParams
        flashBtn.setOnClickListener {
            if (flashBtn.tag != "on") {
                if (setFlashLight(true)) {
                    flashBtn.tag = "on"
                    flashBtn.setImageResource(R.drawable.smart_scan_flashlight_on)
                }
            } else {
                if (setFlashLight(false)) {
                    flashBtn.tag = "off"
                    flashBtn.setImageResource(R.drawable.smart_scan_flashlight_off)
                }
            }
        }
    }

    /**
     * 更新闪光灯显示与否
     */
    private fun updateFlashlight(enableFlashlightBtn: Boolean) {
        if (enableFlashlightBtn) {
            flashBtn.visibility = View.VISIBLE
        } else {
            flashBtn.visibility = View.GONE
        }
    }

    /**
     * 更新闪光灯位置
     */
    private fun onFlashlightLayoutChange(rect: Rect) {
        if (mFlashLightBtnSize > 0) {
            val x = (rect.left + rect.width() / 2).toFloat()
            flashBtn.x = x - mFlashLightBtnSize / 2
            flashBtn.y = rect.bottom - mFlashLightBtnSize * 1.2f
        }
    }

    /**
     * 改变smartScanView的配置，如窗口大小，解码配置等等等
     */
    fun updateConfigure(smartScanParameter: SmartScanParameter) {
        setDefaultParameter(smartScanParameter)
        changeScanConfigure()
        mOneReader = if (smartScanParameter.addOneDCode || smartScanParameter.onlyOneDCode) {
            MultiFormatOneDReader(null)
        } else {
            null
        }
        mQRReader = if (!smartScanParameter.onlyOneDCode) {
            QRCodeReader()
        } else {
            null
        }
        updateFlashlight(smartScanParameter.enableFlashlightBtn)
        if ((smartScanParameter.enableBeep || smartScanParameter.enableVibrate) && context is Activity) {
            mBeepVibrateManager =
                BeepVibrateManager(
                    context as Activity
                )
        }
        mBeepVibrateManager?.updateConfigure(
            smartScanParameter.enableBeep,
            smartScanParameter.enableVibrate
        )
    }

    /**
     * 获取现在的配置，一般结合updateConfigure(smartScanParameter: SmartScanParameter) 使用
     */
    fun getSmartScanParameter(): SmartScanParameter {
        return mSmartScanParameter
    }

    /**
     * 获取camera对象
     */
    fun getCamera(): Camera? {
        return mGLCameraManager.getCamera()
    }

    /**
     * view被移除的资源释放
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mGLCameraManager.destroy()
        mBeepVibrateManager?.close()
    }

}