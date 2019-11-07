package com.dming.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import com.dming.smallScan.GLScanView
import com.dming.smallScan.GLViewParameter

class CreateViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val framelayout = FrameLayout(this)
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        framelayout.layoutParams = params
        setContentView(framelayout)
        val glScanView = GLScanView(this)
        framelayout.addView(glScanView)
        val gLViewParameter = GLViewParameter()

        val oneDP = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f,
            this.resources.displayMetrics
        )

        gLViewParameter.apply {
//            this.scanWidth = scanWidth
//            this.scanHeight = scanHeight
            this.scanPercentWidth = 0.65f
            this.scanPercentHeight = 0.65f
//            this.scanTopOffset = scanTopOffset
            this.scanPercentTopOffset = 0.2f
//            this.scanLine = scanLine
//            this.scanCorner = scanCorner
            this.scanBackgroundColor = 0x33000000
//            this.addOneDCode = addOneDCode
//            this.onlyOneDCode = onlyOneDCode
            this.scanMustSquare = true
            this.scanCornerSize = 18 * oneDP
            this.scanCornerThick = 3 * oneDP
            this.scanLineWidth = 6 * oneDP
//            this.scanFrameLineWidth = scanFrameLineWidth
//            this.scanFrameLineColor = scanFrameLineColor
            this.scanColor = 0xFF00ff00.toInt()
//            this.disableScale = disableScale
//            this.enableBeep = enableBeep
//            this.enableVibrate = enableVibrate
            this.enableFlashlightBtn = true
        }
        glScanView.initWithParameter(gLViewParameter)
    }

}