package com.dming.glScan.zxing

import com.google.zxing.Result

/**
 * 结果监听
 */
interface OnResultListener {
    fun onResult(result: Result)
}
