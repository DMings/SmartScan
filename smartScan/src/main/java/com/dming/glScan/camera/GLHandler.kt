package com.dming.glScan.camera;

import android.os.Handler
import android.os.Looper

class GLHandler(looper: Looper) : Handler(looper) {
    var isDead = false
}
