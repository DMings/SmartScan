package com.dming.smallScan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;


/**
 * Created by DMing
 */

public class ScannerView extends View {
    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 90L;
    private Paint mBgPaint;
    private Paint mCornerPaint;
    private Paint mFocusFramePaint;
    private Paint mLaserPaint;

    private int mCornerLength;
    private int mCornerThick;
    private int mFocusLineThick;
    private int scannerAlpha;

    private Rect mScannerRect;
    private RadialGradient mRadialGradient;


    public ScannerView(Context context) {
        this(context, null);
    }

    public ScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 一些初始化操作
     */
    private void init() {

        int tingColor = 0xFF00FF00;
        int bgColor = 0x33000000;

        mBgPaint = new Paint();
        mCornerPaint = new Paint();
        mFocusFramePaint = new Paint();
        mLaserPaint = new Paint();

        mBgPaint.setColor(bgColor);
        mBgPaint.setAntiAlias(true);

        mCornerPaint.setAntiAlias(true);
        mCornerPaint.setColor(tingColor);

        mFocusFramePaint.setAntiAlias(true);
        mFocusFramePaint.setColor(Color.WHITE);

        mLaserPaint.setAntiAlias(true);
        mLaserPaint.setColor(tingColor);

        mCornerLength = 100;
        mCornerThick = 15;
        mFocusLineThick = 1;
        scannerAlpha = 0;
    }

    public void setScannerRect(Rect scannerRect) {
        mScannerRect = scannerRect;
        mRadialGradient = new RadialGradient(mScannerRect.width() / 2,
                10, mScannerRect.width() / 2,
                Color.RED, Color.BLUE, Shader.TileMode.CLAMP);
        mLaserPaint.setShader(mRadialGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mScannerRect == null) return;
        int width = getWidth();
        int height = getHeight();
        canvas.drawRect(0, 0, width, mScannerRect.top, mBgPaint);
        canvas.drawRect(0, mScannerRect.top, mScannerRect.left, mScannerRect.bottom + 1, mBgPaint);
        canvas.drawRect(mScannerRect.right + 1, mScannerRect.top, width, mScannerRect.bottom + 1, mBgPaint);
        canvas.drawRect(0, mScannerRect.bottom + 1, width, height, mBgPaint);
        drawCorner(canvas, mScannerRect);
        drawFocusRect(canvas, mScannerRect);
        drawLaser(canvas, mScannerRect);
        //实现动画效果
        postInvalidateDelayed(ANIMATION_DELAY, mScannerRect.left, mScannerRect.top, mScannerRect.right, mScannerRect.bottom);
    }

    /**
     * 绘制矩形框的四个角
     *
     * @param canvas
     * @param rect
     */
    private void drawCorner(Canvas canvas, Rect rect) {
        if (rect == null) return;
        //绘制左上角
        canvas.drawRect(rect.left, rect.top, rect.left + mCornerLength, rect.top + mCornerThick, mCornerPaint);
        canvas.drawRect(rect.left, rect.top, rect.left + mCornerThick, rect.top + mCornerLength, mCornerPaint);

        //绘制左下角
        canvas.drawRect(rect.left, rect.bottom - mCornerThick, rect.left + mCornerLength, rect.bottom, mCornerPaint);
        canvas.drawRect(rect.left, rect.bottom - mCornerLength, rect.left + mCornerThick, rect.bottom, mCornerPaint);

        //绘制右上角
        canvas.drawRect(rect.right - mCornerLength, rect.top, rect.right, rect.top + mCornerThick, mCornerPaint);
        canvas.drawRect(rect.right - mCornerThick, rect.top, rect.right, rect.top + mCornerLength, mCornerPaint);

        //绘制右下角
        canvas.drawRect(rect.right - mCornerLength, rect.bottom - mCornerThick, rect.right, rect.bottom, mCornerPaint);
        canvas.drawRect(rect.right - mCornerThick, rect.bottom - mCornerLength, rect.right, rect.bottom, mCornerPaint);
    }

    /**
     * 绘制聚焦框
     */
    private void drawFocusRect(Canvas canvas, Rect rect) {
        canvas.drawRect(rect.left + mCornerLength, rect.top, rect.right - mCornerLength, rect.top + mFocusLineThick, mFocusFramePaint);
        canvas.drawRect(rect.right - mFocusLineThick, rect.top + mCornerLength, rect.right, rect.bottom - mCornerLength, mFocusFramePaint);
        canvas.drawRect(rect.left + mCornerLength, rect.bottom - mFocusLineThick, rect.right - mCornerLength, rect.bottom, mFocusFramePaint);
        canvas.drawRect(rect.left, rect.top + mCornerLength, rect.left + mFocusLineThick, rect.bottom - mCornerLength, mFocusFramePaint);
    }

    /**
     * 绘制激光线
     */
    private void drawLaser(Canvas canvas, Rect rect) {
        mLaserPaint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = rect.height() / 2 + rect.top;
        canvas.drawRect(rect.left, middle -5, rect.right, middle + 5, mLaserPaint);
    }


}