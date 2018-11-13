package com.angcyo.dingding

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.angcyo.dingding.DingDingInterceptor.Companion.screenshot
import com.angcyo.lib.L
import com.angcyo.uiview.less.accessibility.Permission
import com.angcyo.uiview.less.accessibility.navigatorBarHeight
import com.angcyo.uiview.less.base.BaseAppCompatActivity


class MainActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//        )

        setContentView(R.layout.activity_main)


//        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setSupportActionBar(viewHolder.v(R.id.toolbar))

        viewHolder.view(R.id.fab).setOnClickListener { view ->
            //startActivity(Intent(this, TestActivity::class.java))

            SystemUIInterceptor.navigatorBarHeight = navigatorBarHeight()
            SystemUIInterceptor.touch = true

            L.i("height:${navigatorBarHeight()}")

            if (Permission.check(this)) {
                screenshot?.startCapture(this, 909)
            }
        }

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 900)

//        screenshot = Screenshot.capture(this) { bitmap, filePath ->
//            Log.i(
//                "angcyo",
//                "${bitmap.allocationByteCount.toLong()}   ${Formatter.formatFileSize(
//                    this@MainActivity,
//                    bitmap.allocationByteCount.toLong()
//                )}"
//            )
//            findViewById<ImageView>(R.id.image_view).setImageBitmap(bitmap)
//
//            findViewById<ImageView>(R.id.image_view2).setImageBitmap(bitmap.toBytes()?.toBitmap())
//
////            OCR.general(bitmap.toBase64())
//            OCR.general_basic(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.general(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.accurate(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//        }.setAlwaysCapture(false).setCaptureDelay(1_000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screenshot?.onActivityResult(resultCode, data)
    }
}
