package com.angcyo.dingding

import android.content.Intent
import android.os.Bundle
import android.os.Message
import com.angcyo.lib.L
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.kotlin.nextInt
import com.angcyo.uiview.less.kotlin.nowTime
import com.angcyo.uiview.less.kotlin.spiltTime
import com.angcyo.uiview.less.manager.RLocalBroadcastManager
import com.angcyo.uiview.less.manager.Screenshot
import com.angcyo.uiview.less.utils.RUtils
import com.angcyo.uiview.less.utils.utilcode.utils.ConstUtils
import com.angcyo.uiview.less.utils.utilcode.utils.TimeUtils
import kotlin.math.absoluteValue

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/14
 */
class DingDingService : BaseService() {

    companion object {
        const val MSG_CHECK_TIME = 0
        var run = false
            set(value) {
                field = value
                if (!value) {
                    DingDingInterceptor.handEvent = false
                }
            }

        //默认上下班时间
        var defaultStartTime: String = "08:44:00"
        var defaultEndTime: String = "18:01:10"

        /**随机产生上下班时间*/
        //上下班时间
        var startTime: String = "08:30:18"
        var endTime: String = "18:10:10"

        //是否触发了上班打卡
        var isStartTimeDo = false

        //是否触发了下班打卡, 一天执行一次
        var isEndTimeDo = false

        fun resetTime() {
            startTime = "08:${nextInt(20, 40)}:${nextInt(0, 59)}"
            endTime = "18:${nextInt(0, 30)}:${nextInt(0, 59)}"
            isStartTimeDo = false
            isEndTimeDo = false
            val spiltTime = nowTime().spiltTime()
            //几号
            lastDay = spiltTime[2]
        }

        /**那一号, 隔天需要重置打卡时间*/
        var lastDay = 0

        /**
         * 跳过上下班时间判断
         * */
        var debugRun = true
    }

    override fun onCreate() {
        super.onCreate()

        handler.sendEmptyMessage(MSG_CHECK_TIME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        L.i("$intent $flags $startId")
        return super.onStartCommand(intent, flags, startId)
    }

    fun toDingDing() {
        if (MainActivity.activity == null || MainActivity.activity?.get() == null) {
            Screenshot.wakeUpAndUnlock(this)
        } else {
            Screenshot.wakeUpAndUnlock(MainActivity.activity!!.get()!!)
        }

        DingDingInterceptor.handEvent = true
        RUtils.startApp(this, DingDingInterceptor.DING_DING)
    }

    override fun onHandleMessage(msg: Message): Boolean {
        if (msg.what == MSG_CHECK_TIME) {
            val spiltTime = nowTime().spiltTime()

            if (spiltTime[2] != lastDay) {
                resetTime()

                RLocalBroadcastManager.sendBroadcast(MainActivity.UPDATE_TIME)
            }

            if (run) {

                val logTime = "当前时间:${spiltTime[0]}-${spiltTime[1]}-${spiltTime[2]} " +
                        "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}'${spiltTime[6]}"
                L.i(logTime)
                RLocalBroadcastManager.sendBroadcast(MainActivity.UPDATE, Bundle().apply { putString("text", logTime) })

                val defaultStartSpan = TimeUtils.getTimeSpanNoAbs(
                    "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}",
                    defaultStartTime,
                    ConstUtils.TimeUnit.SEC,
                    "HH:mm:ss"
                )

                val defaultEndSpan = TimeUtils.getTimeSpanNoAbs(
                    "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}",
                    defaultEndTime,
                    ConstUtils.TimeUnit.SEC,
                    "HH:mm:ss"
                )

                if (defaultStartSpan > 0 && !debugRun) {
                    //已经过了上班时间
                } else {
                    //上班时间
                    if (isStartTimeDo) {
                        //已经在执行
                    } else {
                        val startSpan = TimeUtils.getTimeSpanNoAbs(
                            "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}",
                            startTime,
                            ConstUtils.TimeUnit.SEC,
                            "HH:mm:ss"
                        )

                        val msg = "距离上班$startTime 还差:${startSpan.absoluteValue} 秒"
                        L.i(msg)

                        if (startSpan >= 0 || debugRun) {
                            //上班打卡
                            isStartTimeDo = true

                            RLocalBroadcastManager.sendBroadcast(
                                MainActivity.UPDATE,
                                Bundle().apply { putString("text", "$logTime\n$startTime 开始上班打卡") })

                            toDingDing()
                        } else {
                            RLocalBroadcastManager.sendBroadcast(
                                MainActivity.UPDATE,
                                Bundle().apply { putString("text", "$logTime\n$msg") })
                        }
                    }
                }

                if (defaultEndSpan > 0 /*&& !debugRun 触发一次就行*/) {
                    //到了下班时间
                    if (isEndTimeDo) {
                        //已在执行
                    } else {
                        val endSpan = TimeUtils.getTimeSpanNoAbs(
                            "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}",
                            endTime,
                            ConstUtils.TimeUnit.SEC,
                            "HH:mm:ss"
                        )

                        val msg = "距离下班$endTime 还差:${endSpan.absoluteValue} 秒"
                        L.i(msg)

                        if (endSpan >= 0 || debugRun) {
                            isEndTimeDo = true

                            RLocalBroadcastManager.sendBroadcast(
                                MainActivity.UPDATE,
                                Bundle().apply { putString("text", "$logTime\n$endTime 开始下班打卡") })

                            toDingDing()
                        } else {
                            RLocalBroadcastManager.sendBroadcast(
                                MainActivity.UPDATE,
                                Bundle().apply { putString("text", "$logTime\n$msg") })
                        }
                    }
                } else {
                    //还没到下班时间
                }
            }
            handler.sendEmptyMessageDelayed(MSG_CHECK_TIME, 5_000)
        }
        return true
    }
}