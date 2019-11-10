package com.dming.demo;

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_scan_main.*

class ScanMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_main)
        Toasty.Config.getInstance().apply()
        fullScanBtn.setOnClickListener {
            startActivity(Intent(this, FullScreenActivity::class.java))
        }
        javaScanBtn.setOnClickListener {
            startActivity(Intent(this, JavaCreateActivity::class.java))
        }
        changScanBtn.setOnClickListener {
            startActivity(Intent(this, ChangeViewActivity::class.java))
        }
        testBtn.setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
        }
    }

}
