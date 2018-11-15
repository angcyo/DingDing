package com.angcyo.dingding

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.text.TextUtils
import com.angcyo.dingding.DingDingInterceptor.Companion.screenshot
import com.angcyo.uiview.less.accessibility.Permission
import com.angcyo.uiview.less.base.BaseAppCompatActivity
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.kotlin.share
import com.angcyo.uiview.less.manager.AlarmBroadcastReceiver
import com.angcyo.uiview.less.manager.RAlarmManager
import com.angcyo.uiview.less.manager.RLocalBroadcastManager
import com.angcyo.uiview.less.manager.Screenshot
import com.angcyo.uiview.less.utils.RUtils
import com.angcyo.uiview.less.utils.T_
import com.orhanobut.hawk.Hawk
import java.lang.ref.WeakReference


class MainActivity : BaseAppCompatActivity() {

    companion object {
        const val UPDATE_BOTTOM_TIP = "update"
        const val UPDATE_TIME = "update_time"
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
        viewHolder.exV(R.id.share_qq_view).setInputText(Hawk.get("share_qq", "angcyo"))
        viewHolder.exV(R.id.baidu_ak_view).setInputText(Hawk.get("baidu_ak", ""))
        viewHolder.exV(R.id.baidu_sk_view).setInputText(Hawk.get("baidu_sk", ""))
        //viewHolder.exV(R.id.ding_pw_view).requestFocus()

        viewHolder.click(R.id.start_button) {
            if (BuildConfig.DEBUG) {
                Hawk.put("baidu_ak", "vGcIcmO6OWnPcBBv9TzZryiD")
                Hawk.put("baidu_sk", "Aa8lePlFQ8cp1py9GZUrrdkZGEyY2Tln")
            }

            val ak = "${viewHolder.tv(R.id.baidu_ak_view).text}"
            val sk = "${viewHolder.tv(R.id.baidu_sk_view).text}"
            if (TextUtils.isEmpty(ak) || TextUtils.isEmpty(sk)) {
                T_.error("请申请百度云OCR")
                return@click
            }

            if (DingDingService.isTaskStart) {
                //停止挂机
                BaseService.start(this, DingDingService::class.java, DingDingService.CMD_STOP)
            } else {
                //开始挂机
                Hawk.put("ding_user", "${viewHolder.tv(R.id.ding_user_view).text}")
                Hawk.put("ding_pw", "${viewHolder.tv(R.id.ding_pw_view).text}")
                Hawk.put("share_qq", "${viewHolder.tv(R.id.share_qq_view).text}")
                Hawk.put("baidu_ak", "$ak")
                Hawk.put("baidu_sk", "$sk")

                if (Permission.check(this)) {
                    screenshot?.startCapture(this, 909)
                } else {
                    return@click
                }
            }

            DingDingService.isTaskStart = !DingDingService.isTaskStart

            updateTipTextView()
        }

        viewHolder.cb(R.id.close_float_box, !Tip.showTip) { _, isChecked ->
            Tip.showTip = !isChecked
        }

        viewHolder.cb(R.id.debug_box, DingDingService.debugRun) { _, isChecked ->
            DingDingService.debugRun = isChecked
        }

        RLocalBroadcastManager
            .instance()
            .registerBroadcast(
                hashCode(),
                RLocalBroadcastManager.OnBroadcastReceiver { _, intent, action ->
                    if (action == UPDATE_BOTTOM_TIP) {
                        updateBottomTipTextView(
                            intent.getBundleExtra(RLocalBroadcastManager.KEY_EXTRA).getString("text") ?: ""
                        )
                    } else if (action == UPDATE_TIME) {
                        updateTipTextView()
                    }
                }, UPDATE_BOTTOM_TIP, UPDATE_TIME
            )

        viewHolder.click(R.id.test_button) {
            T_.show("请锁屏.")
            viewHolder.postDelay(5_000) {
                Screenshot.wakeUpAndUnlock(this)
//                viewHolder.post {
//                    DingDingInterceptor.DING_DING.startApp(this)
//                }
            }
//            RAlarmManager.setDelay(
//                this,
//                10_000L,
//                AlarmBroadcastReceiver.getPendingIntent(this, AlarmBroadcastReceiver::class.java)
//            )
//            RUtils.killMyAllProcess(this)
        }

        if (BuildConfig.DEBUG) {
            viewHolder.click(R.id.bottom_tip_text_view) {
                RUtils.saveView(viewHolder.itemView).share(this)
            }
        }

//        RAlarmManager.setDelay(
//            this,
//            3_000L,
//            AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java)
//        )
//
//        RAlarmManager.setDelay(
//            this,
//            4_000L,
//            AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java)
//        )
    }

    override fun onResume() {
        super.onResume()
        ignoreBatteryOptimization()

        updateTipTextView()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.clear()
        activity = null
        RLocalBroadcastManager.instance().unregisterBroadcast(hashCode())
    }

    fun updateTipTextView() {
        val builder = StringBuilder()
        val km: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardSecure) {
            builder.append("取消锁屏密码,可以自动`亮屏``解锁`\n")
        }
        builder.append("输入`钉钉手机号和密码`,可以自动登录钉钉")

        if (DingDingService.isTaskStart) {
            viewHolder.tv(R.id.start_button).text = "停止挂机"
            builder.append("\n本次上班打卡预设时间:${DingDingService.startTime}")
            builder.append("\n本次下班打卡预设时间:${DingDingService.endTime}")
        } else {
            viewHolder.tv(R.id.start_button).text = "开始挂机"
        }

        builder.append("\n请将程序添加到`电池优化`白名单.")
        builder.append("\n请取消程序的后台配置限制(如果有)")

        viewHolder.tv(R.id.tip_text_view).text = builder
    }

    fun updateBottomTipTextView(text: String) {
        val bottomBuilder = StringBuilder()
        bottomBuilder.append(text)
        viewHolder.tv(R.id.bottom_tip_text_view).text = bottomBuilder
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screenshot?.onActivityResult(resultCode, data) {
            BaseService.start(this, DingDingService::class.java, DingDingService.CMD_RESET_TIME)
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
        //super.onBackPressed()
    }
}
