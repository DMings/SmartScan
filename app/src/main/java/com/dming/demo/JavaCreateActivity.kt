package com.dming.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.dming.glScan.SmartScanParameter
import com.dming.glScan.SmartScanView
import com.dming.glScan.zxing.OnResultListener
import com.google.zxing.Result

class JavaCreateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frameLayout = FrameLayout(this)
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        frameLayout.layoutParams = params
        setContentView(frameLayout)
        val smartScanView = SmartScanView(this)
        frameLayout.addView(smartScanView)

        smartScanView.setOnResultOnceListener(object : OnResultListener {
            override fun onResult(result: Result) {
                Toasty.showText(
                    this@JavaCreateActivity,
                    "result: ${result.text}",
                    Toast.LENGTH_SHORT
                )
                finish()
            }
        })

        val smartScanParameter = SmartScanParameter()

//        val oneDP = TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP, 1f,
//            this.resources.displayMetrics
//        )

        smartScanParameter.apply {
            //            this.scanWidth = scanWidth
//            this.scanHeight = scanHeight
            this.scanPercentWidth = 0.65f
            this.scanPercentHeight = 0.65f
//            this.scanTopOffset = scanTopOffset
            this.scanPercentTopOffset = 0.2f
//            this.scanLine = scanLine
//            this.scanCorner = scanCorner
//            this.scanBackgroundColor = 0x33000000
//            this.addOneDCode = addOneDCode
//            this.onlyOneDCode = onlyOneDCode
//            this.scanMustSquare = true
//            this.scanCornerSize = 18 * oneDP
//            this.scanCornerThick = 3 * oneDP
//            this.scanLineWidth = 6 * oneDP
//            this.scanFrameLineWidth = scanFrameLineWidth
//            this.scanFrameLineColor = scanFrameLineColor
//            this.scanColor = 0xFF00ff00.toInt()
//            this.disableScale = disableScale
//            this.enableBeep = enableBeep
//            this.enableVibrate = enableVibrate
            this.enableFlashlightBtn = true
            this.scanCorner = resources.getDrawable(R.drawable.smart_scan_corner_bg)
        }
        smartScanView.init(smartScanParameter)
    }

}