package com.dming.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.widget.Toast
import com.dming.glScan.zxing.OnResultListener
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_change_view.*

class ChangeViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_view)
        val oneDP = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f,
            this.resources.displayMetrics
        )
        smartScanView.setOnResultListener(object : OnResultListener {
            override fun onResult(result: Result) {
                Toasty.showText(
                    this@ChangeViewActivity,
                    "result: ${result.text}",
                    Toast.LENGTH_SHORT
                )
            }
        })
        qrBtn.setOnClickListener {
            val smartScanParameter = smartScanView.getSmartScanParameter()
            smartScanParameter.scanTopOffset = 80 * oneDP
            smartScanParameter.scanWidth = 300 * oneDP
            smartScanParameter.scanHeight = 300 * oneDP
            smartScanParameter.scanLineWidth = 6 * oneDP
            smartScanParameter.onlyOneDCode = false
            smartScanParameter.enableFlashlightBtn = true
            smartScanView.updateConfigure(smartScanParameter)
        }
        oneDBtn.setOnClickListener {
            val smartScanParameter = smartScanView.getSmartScanParameter()
            smartScanParameter.scanTopOffset = 150 * oneDP
            smartScanParameter.scanWidth = 300 * oneDP
            smartScanParameter.scanHeight = 120 * oneDP
            smartScanParameter.scanLineWidth = 4 * oneDP
            smartScanParameter.onlyOneDCode = true
            smartScanParameter.enableFlashlightBtn = false
            smartScanView.updateConfigure(smartScanParameter)
        }
    }

}