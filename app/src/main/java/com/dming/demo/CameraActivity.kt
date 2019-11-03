package com.dming.demo

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class CameraActivity : AppCompatActivity() {

    private var mToast: Toast? = null
    // test
    private var mTestImgBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_back.setOnClickListener {
            onBackPressed()
        }
        glQRView.setResultOnThreadListener {

        }
        val flashLightBtnSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 40f,
            this.resources.displayMetrics
        )
        val padding = (flashLightBtnSize / 4).toInt()
        btn_flash.setPadding(padding, padding, padding, padding)
        glQRView.setCropLocationListener {
            btn_flash.visibility = View.VISIBLE
            val x = (it.left + it.width() / 2).toFloat()
            btn_flash.x = x - flashLightBtnSize / 2
            btn_flash.y = it.bottom - flashLightBtnSize * 3 / 2
            //
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
