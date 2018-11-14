package com.angcyo.dingding

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.angcyo.dingding.DingDingInterceptor.Companion.screenshot
import com.angcyo.uiview.less.accessibility.Permission
import com.angcyo.uiview.less.base.BaseAppCompatActivity
import com.angcyo.uiview.less.base.BaseService
import com.orhanobut.hawk.Hawk
import java.lang.ref.WeakReference


class MainActivity : BaseAppCompatActivity() {

    companion object {
        var activity: WeakReference<Activity>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = WeakReference(this)

//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//        )

        setContentView(R.layout.activity_main)


//        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setSupportActionBar(viewHolder.v(R.id.toolbar))

//        viewHolder.view(R.id.fab).setOnClickListener { view ->
//            //startActivity(Intent(this, TestActivity::class.java))
//
//            if (BuildConfig.DEBUG) {
//                viewHolder.postDelay(3000) {
//                    L.w("点亮屏幕")
//                    Screenshot.wakeUpAndUnlock(this)
//                }
//                return@setOnClickListener
//            }
//
//            SystemUIInterceptor.navigatorBarHeight = navigatorBarHeight()
//            SystemUIInterceptor.touch = true
//
//            L.i("height:${navigatorBarHeight()}")
//
//            if (Permission.check(this)) {
//                screenshot?.startCapture(this, 909)
//            }
//        }

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

        viewHolder.exV(R.id.ding_user_view).setInputText(Hawk.get("ding_user", ""))
        viewHolder.exV(R.id.ding_pw_view).setInputText(Hawk.get("ding_pw", ""))
        //viewHolder.exV(R.id.ding_pw_view).requestFocus()

        viewHolder.click(R.id.start_button) {
            if (DingDingService.run) {
                //停止挂机
            } else {
                //开始挂机
                Hawk.put("ding_user", "${viewHolder.tv(R.id.ding_user_view).text}")
                Hawk.put("ding_pw", "${viewHolder.tv(R.id.ding_pw_view).text}")

                if (Permission.check(this)) {
                    DingDingService.resetTime()
                    screenshot?.startCapture(this, 909)
                }
            }

            DingDingService.run = !DingDingService.run

            updateTipTextView()
        }

        viewHolder.cb(R.id.close_float_box).setOnCheckedChangeListener { buttonView, isChecked ->
            Tip.showTip = !isChecked
        }
        viewHolder.cb(R.id.close_float_box).isChecked = !Tip.showTip
    }

    override fun onResume() {
        super.onResume()

        updateTipTextView()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.clear()
        activity = null
    }

    fun updateTipTextView() {
        val builder = StringBuilder()
        val km: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardSecure) {
            builder.append("取消锁屏密码,可以自动`亮屏``解锁`\n")
        }
        builder.append("输入`钉钉手机号和密码`,可以自动登录钉钉")

        if (DingDingService.run) {
            viewHolder.tv(R.id.start_button).text = "停止挂机"
            builder.append("\n本次上班打卡预设时间:${DingDingService.startTime}")
            builder.append("\n本次下班打卡预设时间:${DingDingService.endTime}")
        } else {
            viewHolder.tv(R.id.start_button).text = "开始挂机"
        }

        viewHolder.tv(R.id.tip_text_view).text = builder
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screenshot?.onActivityResult(resultCode, data) {
            BaseService.start(this, DingDingService::class.java)
        }
    }
}
