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
import com.angcyo.uiview.less.kotlin.*
import com.angcyo.uiview.less.manager.AlarmBroadcastReceiver
import com.angcyo.uiview.less.manager.RLocalBroadcastManager
import com.angcyo.uiview.less.manager.Screenshot
import com.angcyo.uiview.less.utils.RDialog
import com.angcyo.uiview.less.utils.Root
import com.angcyo.uiview.less.utils.T_
import com.angcyo.uiview.less.widget.CharInputFilter
import com.orhanobut.hawk.Hawk
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat


class MainActivity : BaseAppCompatActivity() {

    companion object {
        const val UPDATE_BOTTOM_TIP = "update"
        const val UPDATE_TIME = "update_time"

        //软件禁止使用
        const val SOFT_ENABLE = "SOFT_ENABLE"

        //本设备停止使用
        const val SOFT_STOP = "SOFT_STOP"
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
        viewHolder.exV(R.id.share_qq_view).setInputText(Hawk.get("share_qq", "我的电脑"))
        viewHolder.exV(R.id.baidu_ak_view).setInputText(Hawk.get("baidu_ak", ""))
        viewHolder.exV(R.id.baidu_sk_view).setInputText(Hawk.get("baidu_sk", ""))
        updateDelayTime()

        //viewHolder.exV(R.id.ding_pw_view).requestFocus()

        viewHolder.click(R.id.start_button) {
            if (BuildConfig.DEBUG) {
                Hawk.put("baidu_ak", "vGcIcmO6OWnPcBBv9TzZryiD")
                Hawk.put("baidu_sk", "Aa8lePlFQ8cp1py9GZUrrdkZGEyY2Tln")
            }

            if (!OCR.checkRun()) {
                OCR.loadConfig()
                return@click
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

                viewHolder.enable(R.id.test_button, true)
                viewHolder.enable(R.id.test_share_button, true)
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
                configTime(true)

                currentFocus?.clearFocus()

//                if (BuildConfig.DEBUG) {
//                    return@click
//                }

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

        viewHolder.cb(R.id.keep_box, false, "keep_on") { _, isChecked ->
            window.decorView.keepScreenOn = isChecked
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
                    } else if (action == SOFT_STOP) {
                        RDialog.tip(this, "未授权的设备.")
                    } else if (action == SOFT_ENABLE) {
                        RDialog.tip(this, "软件已停止使用.")
                    }
                }, UPDATE_BOTTOM_TIP, UPDATE_TIME, SOFT_ENABLE, SOFT_STOP
            )

        viewHolder.click(R.id.test_button) {
            currentFocus?.clearFocus()

            L.w("开始测试唤醒")
            T_.show("请锁屏.")
            val runnable = Runnable {
                L.w("开始唤醒屏幕  do...")

                Screenshot.wakeUpAndUnlock(this, true) {
                    DingDingInterceptor.DING_DING.startApp(this)

                    viewHolder.postDelay(3 * 1000L) {
                        runMain()
                    }
                }
            }

            viewHolder.postDelay(if (BuildConfig.DEBUG) 10 * 1_000L else 5_000L) {
                L.w("开始唤醒屏幕")
                viewHolder.post(runnable)
            }
//            RAlarmManager.setDelay(
//                this,
//                10_000L,
//                AlarmBroadcastReceiver.getPendingIntent(this, AlarmBroadcastReceiver::class.java)
//            )
//            RUtils.killMyAllProcess(this)
        }

        viewHolder.click(R.id.test_share_button) {
            currentFocus?.clearFocus()

            BaseService.start(RApplication.getApp(), DingDingService::class.java, DingDingService.TASK_SHARE_TEST)
        }

        if (BuildConfig.DEBUG) {
            viewHolder.tv(R.id.bottom_tip_text_view).text = "打卡助手为您服务!"
            viewHolder.click(R.id.bottom_tip_text_view) {
                //DingDingInterceptor.handEvent = true
                //RUtils.saveView(viewHolder.itemView).share(this)
                //"分享文本测试".share(this)

                //Screenshot.wakeUpAndUnlock(this, false)
            }
        }

        viewHolder.tv(R.id.uuid_text_view).text = Root.initImei()
        viewHolder.click(R.id.uuid_text_view) {
            Root.initImei().copy()
            T_.show("UUID 已复制" + BuildConfig.FLAVOR)
        }

        registerReceiver(TimeAlarmReceiver(), AlarmBroadcastReceiver.getIntentFilter().apply {
            addAction(TimeAlarmReceiver.RUN)
        })

        configTime()

        OCR.loadConfig()
    }

    override fun onResume() {
        super.onResume()

        updateTipTextView()

        viewHolder.tv(R.id.device_tip_text_view).text = Root.device_info(this)

        val spiltTime = nowTime().spiltTime()
        L.i("今天周:${spiltTime[7]} 节假日:${OCR.isHoliday()}")

//        if (BuildConfig.DEBUG) {
//            L.i(Http.map("a:2", "b:3").toJson())
//        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        ignoreBatteryOptimization()
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
        } else {
            viewHolder.tv(R.id.start_button).text = "开始挂机"
        }

        builder.append("\n*请保持屏幕常亮, 提高成功率")
        builder.append("\n*锁屏前, 请将程序切换至前台")
        builder.append("\n请取消锁屏密码,可以自动亮屏并解锁(如果满足)")
        builder.append("\n请把锁屏时间设置成10分钟以上")

        viewHolder.tv(R.id.tip_text_view).text = builder
    }

    fun updateBottomTipTextView(text: String) {
        val bottomBuilder = StringBuilder()
        bottomBuilder.append(text)
        viewHolder.tv(R.id.bottom_tip_text_view).text = bottomBuilder
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val mediaProjection = screenshot?.onActivityResult(resultCode, data) {
            DingDingInterceptor.handEvent = false

            DingDingService.isTaskStart = true

            updateTipTextView()

            BaseService.start(this, DingDingService::class.java, DingDingService.CMD_RESET_TIME)
        }

        if (mediaProjection != null) {
            DingDingService.isTaskStart = false
            updateTipTextView()

            viewHolder.enable(R.id.test_button, false)
            viewHolder.enable(R.id.test_share_button, false)
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
        //super.onBackPressed()
    }

    //上下班时间
    fun configTime(save: Boolean = false) {
        if (save) {
            val startTime = viewHolder.tv(R.id.edit_start_time).text
            val endTime = viewHolder.tv(R.id.edit_end_time).text
            val delayTime = viewHolder.tv(R.id.edit_random_time).text

            val df = SimpleDateFormat("HH:mm")
            if (TextUtils.isEmpty(startTime)) {
            } else {
                try {
                    df.parse("$startTime")
                    DingDingService.defaultStartTime = "$startTime"
                } catch (e: Exception) {
                }
            }
            if (TextUtils.isEmpty(endTime)) {
            } else {
                try {
                    df.parse("$endTime")
                    DingDingService.defaultEndTime = "$endTime"
                } catch (e: Exception) {
                }
            }
            if (TextUtils.isEmpty(delayTime)) {
            } else {
                if (delayTime.toString().toInt() < 4) {
                    DingDingService.defaultDelayTime = "4"
                } else {
                    DingDingService.defaultDelayTime = "$delayTime"
                }
            }

            configTime()
        } else {
            viewHolder.exV(R.id.edit_start_time).setInputText(DingDingService.defaultStartTime)
            viewHolder.exV(R.id.edit_end_time).setInputText(DingDingService.defaultEndTime)
            viewHolder.exV(R.id.edit_random_time).setInputText(DingDingService.defaultDelayTime)

            viewHolder.exV(R.id.edit_start_time).setFilter(CharInputFilter().apply {
                setFilterModel(CharInputFilter.MODEL_NUMBER)
                setMaxInputLength(5)
                addFilterCallback { _, c, _, _, _, _ ->
                    c == ':'
                }
            })

            viewHolder.exV(R.id.edit_end_time).setFilter(CharInputFilter().apply {
                setFilterModel(CharInputFilter.MODEL_NUMBER)
                setMaxInputLength(5)
                addFilterCallback { _, c, _, _, _, _ ->
                    c == ':'
                }
            })

            viewHolder.exV(R.id.edit_random_time).setFilter(CharInputFilter().apply {
                setFilterModel(CharInputFilter.MODEL_NUMBER)
                setMaxInputLength(2)
            })
        }
    }
}
