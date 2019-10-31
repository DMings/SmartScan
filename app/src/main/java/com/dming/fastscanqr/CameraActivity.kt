package com.dming.fastscanqr

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        glQRView.changeQRConfigure()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
