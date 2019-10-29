package com.dming.fastscanqr

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.widget.ImageView
import com.dming.fastscanqr.utils.DLog
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer


class CameraActivity : AppCompatActivity() {

//    companion object {
//        val DECODE_HINTS: Map<DecodeHintType, Any> = mapOf(
//            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
//        )
//    }

    private val mCameraHelper: CameraHelper = CameraHelper()
    private val mReader = QRCodeReader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCameraHelper.init(this)
        glSurfaceView.holder.addCallback(object : Callback {

            override fun surfaceCreated(holder: SurfaceHolder?) {
                DLog.i("surfaceCreated")
                mCameraHelper.surfaceCreated(this@CameraActivity, holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                DLog.i("surfaceChanged")
                mCameraHelper.onSurfaceChanged(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                DLog.i("surfaceDestroyed")
                mCameraHelper.surfaceDestroyed()
            }

        })
        mCameraHelper.setParseQRListener { width: Int, height: Int, grayByteBuffer: ByteBuffer ->
            try {
                val start = System.currentTimeMillis()
                val source = GLRGBLuminanceSource(width, height, grayByteBuffer)
                val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                val result = mReader.decode(binaryBitmap)// 开始解析
                DLog.i("width: $width height: $height decode cost time: ${System.currentTimeMillis() - start}  result: ${result.text}")
            } catch (e: NotFoundException) {
//                e.printStackTrace()
            } catch (e: ChecksumException) {
                e.printStackTrace()
            } catch (e: FormatException) {
                e.printStackTrace()
            }
        }
        showQRImg.setOnClickListener {
            mCameraHelper.readPixels(it as ImageView)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        DLog.i("onConfigurationChanged: ${newConfig!!.orientation}")
    }

//        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//            fullBtn.setImageResource(R.drawable.ic_button_zoom);
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        }else {
//            fullBtn.setImageResource(R.drawable.ic_button_full);
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
//        baseLayout.getViewTreeObserver()
//            .addOnGlobalLayoutListener(ViewTreeObserver.OnGlobalLayoutListener {
//                val width = baseLayout.getMeasuredWidth()
//                val height = baseLayout.getMeasuredHeight()
//                if (width != widthPixels || height != heightPixels) {
//                    widthPixels = width
//                    heightPixels = height
//                    //                    DLog.i("baseLayout Width: "+baseLayout.getMeasuredWidth()+" Height: "+baseLayout.getMeasuredHeight());
//                    dnPlayer.onConfigurationChanged(widthPixels, heightPixels)
//                }
//            })
//    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraHelper.destroy()
    }

}
