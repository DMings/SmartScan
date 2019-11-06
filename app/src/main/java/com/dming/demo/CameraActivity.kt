package com.dming.demo

import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import com.dming.smallScan.OnScanViewListener
import kotlinx.android.synthetic.main.activity_main.*


class CameraActivity : AppCompatActivity() {

    // test
//    private var mTestImgBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_back.setOnClickListener {
            onBackPressed()
        }
        glScanView.setOnResultOnceListener {
            Toast.makeText(this, "result: $it", Toast.LENGTH_SHORT).show()
        }
        v_test.setOnClickListener { glScanView.startDecode() }
        val flashLightBtnSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 40f,
            this.resources.displayMetrics
        )
        glScanView.setCornerLocationListener(object : OnScanViewListener {
            override fun onCreate() {
                btn_flash.visibility = View.VISIBLE
                val padding = (flashLightBtnSize / 4).toInt()
                btn_flash.setPadding(padding, padding, padding, padding)
            }

            override fun onChange(rect: Rect) {
                val x = (rect.left + rect.width() / 2).toFloat()
                btn_flash.x = x - flashLightBtnSize / 2
                btn_flash.y = rect.bottom - flashLightBtnSize * 3 / 2
                //
//                v_test.x = rect.left.toFloat()
//                v_test.y = rect.top.toFloat()
//                val lp = v_test.layoutParams
//                lp.width = rect.width()
//                lp.height = rect.height()
//                v_test.layoutParams = lp
            }

            override fun onDestroy() {

            }

        })
        btn_flash.setOnClickListener {
            if (btn_flash.tag != "on") {
                if (glScanView.setFlashLight(true)) {
                    btn_flash.tag = "on"
                    btn_flash.setImageResource(R.drawable.flashlight_on)
                }
            } else {
                if (glScanView.setFlashLight(false)) {
                    btn_flash.tag = "off"
                    btn_flash.setImageResource(R.drawable.flashlight_off)
                }
            }
        }
//        testGetImg()
    }
//    glScanView.setViewConfigure(0.2f,0.7f,0.3f,false)
//    glScanView.setDecodeConfigure(true)
    // test
//    private fun testGetImg() {
//        glScanView.setGrayImgListener { width, height, grayByteBuffer ->
//            if (mTestImgBitmap == null || mTestImgBitmap!!.width != width ||
//                mTestImgBitmap!!.height != height
//            ) {
//                mTestImgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//            }
//            mTestImgBitmap!!.copyPixelsFromBuffer(grayByteBuffer)
//            runOnUiThread {
//                testImg.setImageBitmap(mTestImgBitmap)
//            }
//        }
//    }

}
