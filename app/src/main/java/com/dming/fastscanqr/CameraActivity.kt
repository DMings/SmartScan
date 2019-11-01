package com.dming.fastscanqr

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class CameraActivity : AppCompatActivity() {

    // test
    private var mTestImgBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_back.setOnClickListener {
            onBackPressed()
        }
        glQRView.setResultListener {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "result: $it",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
//        val flashLightBtnSize = TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP, 50f,
//            this.resources.displayMetrics
//        )
        glQRView.setCropLocationListener {
//            val x = (it.left + it.width() / 2).toFloat()
//            val y = (it.top + it.height() / 2).toFloat()
//            btn_flash.x = x - flashLightBtnSize / 2
//            btn_flash.y = y - flashLightBtnSize / 2
            v_test.x = it.left.toFloat()
            v_test.y = it.top.toFloat()
            val lp = v_test.layoutParams
            lp.width = it.width()
            lp.height = it.height()
            v_test.layoutParams = lp
        }
        btn_flash.setOnClickListener {
            if (btn_flash.tag != "on") {
                if (glQRView.setFlashLight(true)) {
                    btn_flash.tag = "on"
                    btn_flash.setImageResource(R.drawable.flashlight_on)
                }
            } else {
                if (glQRView.setFlashLight(false)) {
                    btn_flash.tag = "off"
                    btn_flash.setImageResource(R.drawable.flashlight_off)
                }
            }
        }
//        testGetImg()
    }

//    // test
//    private fun testGetImg() {
//        glQRView.setGrayImgListener { width, height, grayByteBuffer ->
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
