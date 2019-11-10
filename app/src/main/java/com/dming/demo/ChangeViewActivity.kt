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
            val glScanParameter = glScanView.getGLScanParameter()
            glScanParameter.scanTopOffset = 80 * oneDP
            glScanParameter.scanWidth = 300 * oneDP
            glScanParameter.scanHeight = 300 * oneDP
            glScanParameter.scanLineWidth = 6 * oneDP
            glScanParameter.onlyOneDCode = false
            glScanParameter.enableFlashlightBtn = true
            glScanView.updateConfigure(glScanParameter)
        }
        oneDBtn.setOnClickListener {
            val glScanParameter = glScanView.getGLScanParameter()
            glScanParameter.scanTopOffset = 150 * oneDP
            glScanParameter.scanWidth = 300 * oneDP
            glScanParameter.scanHeight = 80 * oneDP
            glScanParameter.scanLineWidth = 4 * oneDP
            glScanParameter.onlyOneDCode = true
            glScanParameter.enableFlashlightBtn = false
            glScanView.updateConfigure(glScanParameter)
        }
    }

}