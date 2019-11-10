package com.dming.demo

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.dming.smallScan.OnScanViewListener
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_full_screen.*


class TestActivity : AppCompatActivity() {

    private var mTestImgBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        glScanView.setOnResultListener {
            Toasty.success(this, "result: $it", Toast.LENGTH_SHORT).show()
        }
        v_test.setOnClickListener { glScanView.startDecode() }
        glScanView.setCornerLocationListener(object : OnScanViewListener {
            override fun onCreate() {

            }

            override fun onChange(rect: Rect) {
                //
                v_test.x = rect.left.toFloat()
                v_test.y = rect.top.toFloat()
                val lp = v_test.layoutParams
                lp.width = rect.width()
                lp.height = rect.height()
                v_test.layoutParams = lp
            }

            override fun onDestroy() {

            }

        })
        testGetImg()
    }

    private fun testGetImg() {
        glScanView.setGrayImgListener { width, height, grayByteBuffer ->
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
    }

}
