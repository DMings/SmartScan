package com.dming.smallScan

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View


/**
 * Created by DMing
 */

class ScannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private lateinit var mBgPaint: Paint
    private lateinit var mCornerPaint: Paint
    private lateinit var mLineFramePaint: Paint
    private lateinit var mScanLinePaint: Paint

    private var mCornerSize: Int = 0
    private var mCornerThick: Int = 0
    private var mFrameLineWidth: Int = 0
    private var mScanLineSize: Int = 0
    private var mScanColor: Int = 0

    private var mScannerRect: Rect? = null
    private var mOvalRect: RectF? = null

    private var mAnimator: ValueAnimator? = null
    private var mAnimatorDuration: Long = 3000
    private var mAnimatedFraction: Float = 0f
    private var mScanLineMoveHeight: Float = 0f

    private var mScanLineDrawable: Drawable? = null
    private var mScanCornerDrawable: Drawable? = null

    /**
     * 一些初始化操作
     */
    fun initWithAttribute(typedArray: TypedArray) {
        val oneDP = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f,
            this.resources.displayMetrics
        )
        mScanLineDrawable =
            typedArray.getDrawable(R.styleable.GLScanView_scanLine)
        mScanCornerDrawable =
            typedArray.getDrawable(R.styleable.GLScanView_scanCorner)
        if (mScanCornerDrawable == null) { // 有扫描框图片用图片
            //角长度
            mCornerSize =
                typedArray.getDimension(R.styleable.GLScanView_scanCornerSize, 18 * oneDP).toInt()
            //角宽
            mCornerThick =
                typedArray.getDimension(R.styleable.GLScanView_scanCornerThick, 3 * oneDP).toInt()
        }
        // 扫描线尺寸
        mScanLineSize =
            typedArray.getDimension(R.styleable.GLScanView_scanLineWidth, 6 * oneDP).toInt()
        // 扫描角和扫描框颜色
        mScanColor = typedArray.getColor(
            R.styleable.GLScanView_scanColor,
            context.resources.getColor(R.color.scanColor)
        )
        // 框线宽
        mFrameLineWidth =
            typedArray.getDimension(R.styleable.GLScanView_scanFrameLineWidth, 0f).toInt()
        // 背景色和框线
        val scanBackground = typedArray.getColor(
            R.styleable.GLScanView_scanBackground,
            context.resources.getColor(R.color.scanBackground)
        )
        val scanFrameLineColor = typedArray.getColor(
            R.styleable.GLScanView_scanFrameLineColor,
            context.resources.getColor(R.color.scanBackground)
        )

        mBgPaint = Paint()
        mCornerPaint = Paint()
        mLineFramePaint = Paint()
        mScanLinePaint = Paint()

        mBgPaint.color = scanBackground
        mBgPaint.isAntiAlias = true

        val cornerColor = mScanColor and 0x00FFFFFF or 0xFF000000.toInt()
        mCornerPaint.isAntiAlias = true
        mCornerPaint.color = cornerColor

        mLineFramePaint.isAntiAlias = true
        mLineFramePaint.color = scanFrameLineColor

        mScanLinePaint.isAntiAlias = true

    }

    fun changeScanConfigure(scannerRect: Rect) {
        mScannerRect = scannerRect
        mScannerRect?.let { rect ->
            val ovalRect = RectF(
                0f,
                0f,
                scannerRect.width().toFloat(),
                mScanLineSize.toFloat()
            )
            val statColor = mScanColor and 0x00FFFFFF or 0xAA000000.toInt()
            val endColor = mScanColor and 0x00FFFFFF or 0x10000000
            val linearGradient = LinearGradient(
                ovalRect.left,
                ovalRect.bottom,
                ovalRect.width() / 2,
                ovalRect.bottom,
                endColor, statColor, Shader.TileMode.MIRROR
            )
            mOvalRect = ovalRect
            mScanLinePaint.shader = linearGradient
            if (mAnimator != null) {
                mAnimator!!.removeAllUpdateListeners()
            }
            mAnimator = ValueAnimator.ofFloat(0f, rect.height().toFloat())
            mAnimator!!.addUpdateListener { animation: ValueAnimator ->
                mAnimatedFraction = animation.animatedFraction
                postInvalidateOnAnimation(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom
                )
            }
            mAnimator?.let { animator ->
                animator.cancel()
                animator.duration = mAnimatorDuration
                animator.repeatMode = ValueAnimator.RESTART
                animator.repeatCount = ValueAnimator.INFINITE
                animator.start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBg(canvas) // 背景
        drawFrameLineRect(canvas) // 框边
        //
        if (mScanCornerDrawable != null) { // 四个角
            drawCornerDrawable(canvas)
        } else {
            drawCorner(canvas)
        }
        if (mScanLineDrawable != null) {  // 扫描线
            drawScanLineDrawable(canvas)
        } else {
            drawScanLine(canvas)
        }
    }

    private fun drawBg(canvas: Canvas) {
        mScannerRect?.let {
            canvas.drawRect(
                0f,
                0f,
                width.toFloat(),
                it.top.toFloat(),
                mBgPaint
            )
            canvas.drawRect(
                0f,
                it.top.toFloat(),
                it.left.toFloat(),
                it.bottom.toFloat(),
                mBgPaint
            )
            canvas.drawRect(
                it.right.toFloat(),
                it.top.toFloat(),
                width.toFloat(),
                it.bottom .toFloat(),
                mBgPaint
            )
            canvas.drawRect(
                0f,
                it.bottom .toFloat(),
                width.toFloat(),
                height.toFloat(),
                mBgPaint
            )
        }
    }

    /**
     * 绘制矩形框的四个角
     */
    private fun drawCorner(canvas: Canvas) {
        mScannerRect?.let {
            //绘制左上角
            canvas.drawRect(
                it.left.toFloat(),
                it.top.toFloat(),
                (it.left + mCornerSize).toFloat(),
                (it.top + mCornerThick).toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                it.left.toFloat(),
                it.top.toFloat(),
                (it.left + mCornerThick).toFloat(),
                (it.top + mCornerSize).toFloat(),
                mCornerPaint
            )

            //绘制左下角
            canvas.drawRect(
                it.left.toFloat(),
                (it.bottom - mCornerThick).toFloat(),
                (it.left + mCornerSize).toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                it.left.toFloat(),
                (it.bottom - mCornerSize).toFloat(),
                (it.left + mCornerThick).toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )

            //绘制右上角
            canvas.drawRect(
                (it.right - mCornerSize).toFloat(),
                it.top.toFloat(),
                it.right.toFloat(),
                (it.top + mCornerThick).toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                (it.right - mCornerThick).toFloat(),
                it.top.toFloat(),
                it.right.toFloat(),
                (it.top + mCornerSize).toFloat(),
                mCornerPaint
            )

            //绘制右下角
            canvas.drawRect(
                (it.right - mCornerSize).toFloat(),
                (it.bottom - mCornerThick).toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                (it.right - mCornerThick).toFloat(),
                (it.bottom - mCornerSize).toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )
        }
    }

    /**
     * 绘制矩形框的四个角
     */
    private fun drawCornerDrawable(canvas: Canvas) {
        mScanCornerDrawable?.let { scanCornerDrawable ->
            mScannerRect?.let { rect ->
                scanCornerDrawable.setBounds(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom
                )
            }
            scanCornerDrawable.draw(canvas)
        }
    }

    /**
     * 绘制框
     */
    private fun drawFrameLineRect(canvas: Canvas) {
        mScannerRect?.let {
            canvas.drawRect(
                (it.left + mCornerSize).toFloat(),
                it.top.toFloat(),
                (it.right - mCornerSize).toFloat(),
                (it.top + mFrameLineWidth).toFloat(),
                mLineFramePaint
            )
            canvas.drawRect(
                (it.right - mFrameLineWidth).toFloat(),
                (it.top + mCornerSize).toFloat(),
                it.right.toFloat(),
                (it.bottom - mCornerSize).toFloat(),
                mLineFramePaint
            )
            canvas.drawRect(
                (it.left + mCornerSize).toFloat(),
                (it.bottom - mFrameLineWidth).toFloat(),
                (it.right - mCornerSize).toFloat(),
                it.bottom.toFloat(),
                mLineFramePaint
            )
            canvas.drawRect(
                it.left.toFloat(),
                (it.top + mCornerSize).toFloat(),
                (it.left + mFrameLineWidth).toFloat(),
                (it.bottom - mCornerSize).toFloat(),
                mLineFramePaint
            )
        }
    }

    /**
     * 绘制扫描线
     */
    private fun drawScanLine(canvas: Canvas) {
        mScannerRect?.let {
            canvas.save()
            mScanLineMoveHeight = it.height() - mOvalRect!!.height()
            canvas.translate(
                it.left.toFloat(), it.top + mScanLineMoveHeight * mAnimatedFraction
            )
            canvas.drawOval(mOvalRect!!, mScanLinePaint)
            canvas.restore()
        }
    }

    /**
     * 绘制扫描线 Drawable
     */
    private fun drawScanLineDrawable(canvas: Canvas) {
        mScanLineDrawable?.let { scanLineDrawable ->
            mScannerRect?.let {
                canvas.save()
                mScanLineMoveHeight = it.height() - mOvalRect!!.height()
                canvas.translate(
                    it.left.toFloat(), it.top + mScanLineMoveHeight * mAnimatedFraction
                )
                mOvalRect?.let { ovalRect ->
                    scanLineDrawable.setBounds(
                        ovalRect.left.toInt(),
                        ovalRect.top.toInt(),
                        ovalRect.right.toInt(),
                        ovalRect.bottom.toInt()
                    )
                }
                scanLineDrawable.draw(canvas)
                canvas.restore()
            }
        }
    }

}