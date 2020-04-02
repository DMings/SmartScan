package com.dming.demo

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.dming.glScan.OnScanViewListener
import com.dming.glScan.zxing.OnGrayImgListener
import com.dming.glScan.zxing.OnResultListener
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_test.*
import java.nio.ByteBuffer


class TestActivity : AppCompatActivity() {

    private var mTestImgBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        smartScanView.setOnResultListener(object : OnResultListener {
            override fun onResult(result: Result) {
                Toasty.showText(
                    this@TestActivity,
                    "result: ${result.text}",
                    Toast.LENGTH_SHORT
                )
            }
        })
        smartScanView.setScanViewChangeListener(object : OnScanViewListener {
            override fun onCreate() {

            }

            override fun onChange(rect: Rect) {
                //
                testView.x = rect.left.toFloat()
                testView.y = rect.top.toFloat()
                val lp = testView.layoutParams
                lp.width = rect.width()
                lp.height = rect.height()
                testView.layoutParams = lp
            }

            override fun onDestroy() {

            }

        })
        testGetImg()
    }

    private fun testGetImg() {
        smartScanView.setGrayImgListener(object : OnGrayImgListener {
            override fun onGrayImg(width: Int, height: Int, grayByteBuffer: ByteBuffer) {
                if (mTestImgBitmap == null || mTestImgBitmap!!.width != width ||
                    mTestImgBitmap!!.height != height
                ) {
                    mTestImgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                }
                mTestImgBitmap!!.copyPixelsFromBuffer(grayByteBuffer)
                runOnUiThread {
                    testImg.setImageBitmap(mTestImgBitmap)
                }
            }
        })
    }

}
