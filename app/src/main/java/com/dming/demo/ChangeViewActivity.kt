package com.dming.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.widget.Toast
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_change_view.*

class ChangeViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_view)
        val oneDP = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f,
            this.resources.displayMetrics
        )
        glScanView.setOnResultListener {
            Toasty.success(this, "result: $it", Toast.LENGTH_SHORT).show()
        }
        qrBtn.setOnClickListener {
            val smartScanParameter = glScanView.getSmartScanParameter()
            smartScanParameter.scanTopOffset = 80 * oneDP
            smartScanParameter.scanWidth = 300 * oneDP
            smartScanParameter.scanHeight = 300 * oneDP
            smartScanParameter.scanLineWidth = 6 * oneDP
            smartScanParameter.onlyOneDCode = false
            smartScanParameter.enableFlashlightBtn = true
            glScanView.updateConfigure(smartScanParameter)
        }
        oneDBtn.setOnClickListener {
            val smartScanParameter = glScanView.getSmartScanParameter()
            smartScanParameter.scanTopOffset = 150 * oneDP
            smartScanParameter.scanWidth = 300 * oneDP
            smartScanParameter.scanHeight = 120 * oneDP
            smartScanParameter.scanLineWidth = 4 * oneDP
            smartScanParameter.onlyOneDCode = true
            smartScanParameter.enableFlashlightBtn = false
            glScanView.updateConfigure(smartScanParameter)
        }
    }

}