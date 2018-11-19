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
import com.angcyo.lib.L
import com.angcyo.uiview.less.RApplication
import com.angcyo.uiview.less.accessibility.Permission
import com.angcyo.uiview.less.base.BaseAppCompatActivity
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.kotlin.copy
import com.angcyo.uiview.less.kotlin.nowTime
import com.angcyo.uiview.less.kotlin.spiltTime
import com.angcyo.uiview.less.manager.RLocalBroadcastManager
import com.angcyo.uiview.less.manager.Screenshot
import com.angcyo.uiview.less.utils.Root
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
        setContentView(R.layout.activity_main)
        setSupportActionBar(viewHolder.v(R.id.toolbar))

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 900)

        viewHolder.exV(R.id.ding_user_view).setInputText(Hawk.get("ding_user", ""))
        viewHolder.exV(R.id.ding_pw_view).setInputText(Hawk.get("ding_pw", ""))
        viewHolder.exV(R.id.share_qq_view).setInputText(Hawk.get("share_qq", "默认电脑"))
        viewHolder.exV(R.id.baidu_ak_view).setInputText(Hawk.get("baidu_ak", ""))
        viewHolder.exV(R.id.baidu_sk_view).setInputText(Hawk.get("baidu_sk", ""))
        updateDelayTime()

        //viewHolder.exV(R.id.ding_pw_view).requestFocus()

        viewHolder.click(R.id.start_button) {
            if (BuildConfig.DEBUG) {
                Hawk.put("baidu_ak", "vGcIcmO6OWnPcBBv9TzZryiD")
                Hawk.put("baidu_sk", "Aa8lePlFQ8cp1py9GZUrrdkZGEyY2Tln")
            }

            val ak = "${viewHolder.tv(R.id.baidu_ak_view).text.trim()}"
            val sk = "${viewHolder.tv(R.id.baidu_sk_view).text.trim()}"
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

                Hawk.put(
                    "http_delay",
                    "${(viewHolder.tv(R.id.http_delay_view).text.toString().toIntOrNull() ?: 2) * 1000L}"
                )

                updateDelayTime()

                DingDingInterceptor.handEvent = true

                if (Permission.check(this)) {
                    screenshot?.startCapture(this, 909)
                } else {
                    return@click
                }
            }
        }

        viewHolder.cb(R.id.close_float_box, Tip.showTip, "show_tip") { _, isChecked ->
            Tip.showTip = isChecked
        }

        viewHolder.cb(R.id.holiday_box, OCR.jumpHoliday, "holiday_box", null)

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

        viewHolder.click(R.id.test_share_button) {
            BaseService.start(RApplication.getApp(), DingDingService::class.java, DingDingService.TASK_SHARE_TEST)
        }

        if (BuildConfig.DEBUG) {
            viewHolder.tv(R.id.bottom_tip_text_view).text = "打卡助手为您服务!"
            viewHolder.click(R.id.bottom_tip_text_view) {
                //DingDingInterceptor.handEvent = true
                //RUtils.saveView(viewHolder.itemView).share(this)
                //"分享文本测试".share(this)

                Screenshot.wakeUpAndUnlock(this, false)
            }
        }

        viewHolder.tv(R.id.uuid_text_view).text = Root.initImei()
        viewHolder.click(R.id.uuid_text_view) {
            Root.initImei().copy()
            T_.show("已复制")
        }
    }

    override fun onResume() {
        super.onResume()
        ignoreBatteryOptimization()

        updateTipTextView()

        viewHolder.tv(R.id.device_tip_text_view).text = Root.device_info(this)

        val spiltTime = nowTime().spiltTime()
        L.i("今天周:${spiltTime[7]} 节假日:${OCR.isHoliday()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.clear()
        activity = null
        RLocalBroadcastManager.instance().unregisterBroadcast(hashCode())
    }

    fun updateDelayTime() {
        DingDingInterceptor.HTTP_DELAY = Hawk.get("http_delay", "${DingDingInterceptor.HTTP_DELAY}").toLong()
        viewHolder.exV(R.id.http_delay_view).setInputText("${DingDingInterceptor.HTTP_DELAY / 1000L}")
    }

    fun updateTipTextView() {
        val builder = StringBuilder()
        val km: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardSecure) {
            builder.append("取消锁屏密码,可以自动`亮屏``解锁(8.0)`\n")
        }
        builder.append("输入`钉钉手机号和密码`,可以自动登录钉钉")

        if (DingDingService.isTaskStart) {
            viewHolder.tv(R.id.start_button).text = "停止挂机"
//            builder.append("\n本次上班打卡预设时间:${DingDingService.startTime}")
//            builder.append("\n本次下班打卡预设时间:${DingDingService.endTime}")

            Tip.show("助手挂机中...")
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
        if (resultCode != -1) {
            DingDingService.isTaskStart = false
            updateTipTextView()
        }
        screenshot?.onActivityResult(resultCode, data) {
            DingDingInterceptor.handEvent = false

            DingDingService.isTaskStart = !DingDingService.isTaskStart

            updateTipTextView()

            BaseService.start(this, DingDingService::class.java, DingDingService.CMD_RESET_TIME)
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
        //super.onBackPressed()
    }
}
