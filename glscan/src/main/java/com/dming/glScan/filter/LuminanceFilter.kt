package com.dming.glScan.filter

import android.content.Context
import com.dming.glScan.R

/**
 * 以亮度，黑白的形式绘制OES数据，为解码提前做准备
 */
class LuminanceFilter(context: Context):
    BaseOESFilter(context, R.raw.luminance_frg)