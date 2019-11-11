package com.dming.glScan.utils

import android.util.Log
import com.dming.glScan.BuildConfig


/**
 * 简单日志类，方便
 */

class DLog {
    companion object {
        private val TAG = "DMUI"

        @JvmStatic
        fun d(msg: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, msg)
        }

        @JvmStatic
        fun i(msg: String) {
            if (BuildConfig.DEBUG) Log.i(TAG, msg)
        }

        @JvmStatic
        fun e(msg: String) {
            if (BuildConfig.DEBUG) Log.e(TAG, msg)
        }

        @JvmStatic
        fun d(tagNull: String, msg: String) {
            d(msg)
        }

        @JvmStatic
        fun i(tagNull: String, msg: String) {
            i(msg)
        }

        @JvmStatic
        fun e(tagNull: String, msg: String) {
            e(msg)
        }
    }
}