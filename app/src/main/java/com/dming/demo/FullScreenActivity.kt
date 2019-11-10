package com.dming.demo

import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.dming.smallScan.OnScanViewListener
import com.gyf.immersionbar.ImmersionBar
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_full_screen.*


class FullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)
        ImmersionBar.with(this).titleBarMarginTop(btn_back).init()
        btn_back.setOnClickListener {
            onBackPressed()
        }
        glScanView.setOnResultOnceListener {
            Toasty.success(this, "result: $it", Toast.LENGTH_LONG).show()
            finish()
        }
        v_test.setOnClickListener { glScanView.startDecode() }
        glScanView.setCornerLocationListener(object : OnScanViewListener {
            override fun onCreate() {

            }

            override fun onChange(rect: Rect) {

            }

            override fun onDestroy() {

            }

        })
    }

}