package com.dming.fastscanqr

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_back.setOnClickListener {
            onBackPressed()
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
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
