package com.dming.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast

class LauncherActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && packageManager.checkPermission(
                Manifest.permission.CAMERA,
                packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 666)
        } else {
            startActivity(Intent(this, ScanMainActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 666 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, ScanMainActivity::class.java))
            finish()
        }else {
            Toast.makeText(this, "请授予摄像头权限，否则无法正常使用", Toast.LENGTH_LONG).show()
        }
    }
}

