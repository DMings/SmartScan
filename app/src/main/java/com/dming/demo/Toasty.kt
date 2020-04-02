package com.dming.demo;

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.annotation.IntDef
import android.widget.Toast

/**
 * @author DMing
 * @date 2020/4/2.
 * description:
 */
class Toasty {

    @IntDef(value = [Toast.LENGTH_SHORT, Toast.LENGTH_LONG])
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Duration

    companion object {

        private var mToast: Toast? = null
        private var mHandler: Handler? = null

        @Synchronized
        fun showText(context: Context, text: String, @Duration duration: Int) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                showTextInner(context, text, duration)
            } else {
                if (mHandler == null) {
                    mHandler = Handler(Looper.getMainLooper())
                }
                mHandler?.post {
                    showTextInner(context, text, duration)
                }
            }
        }

        @Synchronized
        fun clear() {
            mToast?.cancel()
            mToast = null
            mHandler = null
        }

        private fun showTextInner(context: Context, text: String, @Duration duration: Int) {
            mToast?.cancel()
            mToast = Toast.makeText(
                context.applicationContext,
                text,
                duration
            )
            mToast?.show()
        }
    }

}
