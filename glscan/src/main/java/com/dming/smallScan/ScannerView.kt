package com.dming.smallScan

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


/**
 * Created by DMing
 */

class ScannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private lateinit var mBgPaint: Paint
    private lateinit var mCornerPaint: Paint
    private lateinit var mLineFramePaint: Paint
    private lateinit var mLaserPaint: Paint

    private var mCornerSize: Int = 0
    private var mCornerThickWidth: Int = 0
    private var mCornerLineWidth: Int = 0
    private var mLaserSize: Int = 0

    private var mScannerRect: Rect? = null
    private var mOvalRect: RectF? = null

    private var mAnimator: ValueAnimator? = null
    private var mAnimatedFraction: Float = 0f
    private var mLaserMoveHeight: Float = 0f

    init {
        init()
    }

    /**
     * 一些初始化操作
     */
    private fun init() {

        val tingColor = -0xff0100
        val bgColor = 0x33000000

        mBgPaint = Paint()
        mCornerPaint = Paint()
        mLineFramePaint = Paint()
        mLaserPaint = Paint()

        mBgPaint.color = bgColor
        mBgPaint.isAntiAlias = true

        mCornerPaint.isAntiAlias = true
        mCornerPaint.color = tingColor

        mLineFramePaint.isAntiAlias = true
        mLineFramePaint.color = Color.WHITE

        mLaserPaint.isAntiAlias = true
        mLaserPaint.color = tingColor

        mCornerSize = 100
        mCornerThickWidth = 15
        mCornerLineWidth = 1
        mLaserSize = 20
    }

    fun changeScanConfigure(scannerRect: Rect) {
        mScannerRect = scannerRect
        mScannerRect?.let { rect ->

            val ovalRect  = RectF(
                0f,
               0f,
                scannerRect.width().toFloat(),
                mLaserSize.toFloat()
            )
            val linearGradient = LinearGradient(
                ovalRect.left,
                ovalRect.bottom,
                ovalRect.width() / 2,
                ovalRect.bottom,
                0x1000ff00,-0x55ff0100,  Shader.TileMode.MIRROR
            )
            mOvalRect = ovalRect
            mLaserPaint.shader = linearGradient
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
                animator.duration = 3000
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
        val width = width
        val height = height
        mScannerRect?.let {
            canvas.drawRect(0f, 0f, width.toFloat(), it.top.toFloat(), mBgPaint)
            canvas.drawRect(
                0f,
                it.top.toFloat(),
                it.left.toFloat(),
                (it.bottom + 1).toFloat(),
                mBgPaint
            )
            canvas.drawRect(
                (it.right + 1).toFloat(),
                it.top.toFloat(),
                width.toFloat(),
                (it.bottom + 1).toFloat(),
                mBgPaint
            )
            canvas.drawRect(
                0f,
                (it.bottom + 1).toFloat(),
                width.toFloat(),
                height.toFloat(),
                mBgPaint
            )
            drawCorner(canvas, mScannerRect)
            drawCornerLineRect(canvas, it)
            drawLaser(canvas, it)
        }
    }

    /**
     * 绘制矩形框的四个角
     */
    private fun drawCorner(canvas: Canvas, rect: Rect?) {
        rect?.let {
            //绘制左上角
            canvas.drawRect(
                it.left.toFloat(),
                it.top.toFloat(),
                (it.left + mCornerSize).toFloat(),
                (it.top + mCornerThickWidth).toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                it.left.toFloat(),
                it.top.toFloat(),
                (it.left + mCornerThickWidth).toFloat(),
                (it.top + mCornerSize).toFloat(),
                mCornerPaint
            )

            //绘制左下角
            canvas.drawRect(
                it.left.toFloat(),
                (it.bottom - mCornerThickWidth).toFloat(),
                (it.left + mCornerSize).toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                it.left.toFloat(),
                (it.bottom - mCornerSize).toFloat(),
                (it.left + mCornerThickWidth).toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )

            //绘制右上角
            canvas.drawRect(
                (it.right - mCornerSize).toFloat(),
                it.top.toFloat(),
                it.right.toFloat(),
                (it.top + mCornerThickWidth).toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                (it.right - mCornerThickWidth).toFloat(),
                it.top.toFloat(),
                it.right.toFloat(),
                (it.top + mCornerSize).toFloat(),
                mCornerPaint
            )

            //绘制右下角
            canvas.drawRect(
                (it.right - mCornerSize).toFloat(),
                (it.bottom - mCornerThickWidth).toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )
            canvas.drawRect(
                (it.right - mCornerThickWidth).toFloat(),
                (it.bottom - mCornerSize).toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat(),
                mCornerPaint
            )
        }
    }

    /**
     * 绘制框
     */
    private fun drawCornerLineRect(canvas: Canvas, rect: Rect) {
        canvas.drawRect(
            (rect.left + mCornerSize).toFloat(),
            rect.top.toFloat(),
            (rect.right - mCornerSize).toFloat(),
            (rect.top + mCornerLineWidth).toFloat(),
            mLineFramePaint
        )
        canvas.drawRect(
            (rect.right - mCornerLineWidth).toFloat(),
            (rect.top + mCornerSize).toFloat(),
            rect.right.toFloat(),
            (rect.bottom - mCornerSize).toFloat(),
            mLineFramePaint
        )
        canvas.drawRect(
            (rect.left + mCornerSize).toFloat(),
            (rect.bottom - mCornerLineWidth).toFloat(),
            (rect.right - mCornerSize).toFloat(),
            rect.bottom.toFloat(),
            mLineFramePaint
        )
        canvas.drawRect(
            rect.left.toFloat(),
            (rect.top + mCornerSize).toFloat(),
            (rect.left + mCornerLineWidth).toFloat(),
            (rect.bottom - mCornerSize).toFloat(),
            mLineFramePaint
        )
    }

    /**
     * 绘制激光线
     */
    private fun drawLaser(canvas: Canvas, rect: Rect) {
        canvas.save()
        mLaserMoveHeight = rect.height() - mOvalRect!!.height()
        canvas.translate(
            rect.left.toFloat(), rect.top + mLaserMoveHeight * mAnimatedFraction
        )
        canvas.drawOval(mOvalRect!!, mLaserPaint)
        canvas.restore()
    }

}