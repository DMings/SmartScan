package com.dming.fastscanqr.utils

import android.util.Log
import com.dming.fastscanqr.BuildConfig


/**
 * 简单日志类，方便
 * Created by DMing on 2019/9/19.
 */

class DLog {
    companion object {
        private val TAG = "DMUI"

        fun d(msg: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, msg)
        }

        fun i(msg: String) {
            if (BuildConfig.DEBUG) Log.i(TAG, msg)
        }

        fun e(msg: String) {
            if (BuildConfig.DEBUG) Log.e(TAG, msg)
        }

        fun d(tagNull: String, msg: String) {
            d(msg)
        }

        fun i(tagNull: String, msg: String) {
            i(msg)
        }

        fun e(tagNull: String, msg: String) {
            e(msg)
        }
    }
}