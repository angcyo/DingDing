package com.angcyo.dingding

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Message
import com.angcyo.lib.L
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.kotlin.nextInt
import com.angcyo.uiview.less.kotlin.nowTime
import com.angcyo.uiview.less.kotlin.spiltTime
import com.angcyo.uiview.less.manager.AlarmBroadcastReceiver
import com.angcyo.uiview.less.manager.RAlarmManager
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
        const val CMD_TO_DING_DING = 1
        const val CMD_RESET_TIME = 2
        const val CMD_STOP = 3

        @Deprecated("使用定时器控制")
        var run = false

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

        /**那一号, 隔天需要重置打卡时间*/
        var lastDay = 0

        /**
         * 跳过上下班时间判断
         * */
        var debugRun = false

        /**任务已开始*/
        var isTaskStart = false
    }

    override fun onCreate() {
        super.onCreate()

        handler.sendEmptyMessage(MSG_CHECK_TIME)
    }

    override fun onHandCommand(command: Int, intent: Intent) {
        super.onHandCommand(command, intent)
        if (command == CMD_TO_DING_DING) {
            RLocalBroadcastManager.sendBroadcast(
                MainActivity.UPDATE_BOTTOM_TIP,
                Bundle().apply { putString("text", "即将开始打卡...") })

            toDingDing()
        } else if (command == CMD_RESET_TIME) {
            resetTime()
        } else if (command == CMD_STOP) {
            isTaskStart = false

            startPendingIntent?.let {
                RAlarmManager.cancel(this, it)
            }
            startPendingIntent = null

            endPendingIntent?.let {
                RAlarmManager.cancel(this, it)
            }
            endPendingIntent = null

            RLocalBroadcastManager.sendBroadcast(MainActivity.UPDATE_TIME)
        }
    }

    //上班打卡的定时任务
    var startPendingIntent: PendingIntent? = null
    //下班打卡的定时任务
    var endPendingIntent: PendingIntent? = null

    fun resetTime() {
        isTaskStart = true

        startTime = "08:${nextInt(20, 40)}:${nextInt(0, 59)}"
        endTime = "18:${nextInt(0, 30)}:${nextInt(0, 59)}"
        isStartTimeDo = false
        isEndTimeDo = false
        val spiltTime = nowTime().spiltTime()
        //几号
        lastDay = spiltTime[2]

        RLocalBroadcastManager.sendBroadcast(MainActivity.UPDATE_TIME)

        startPendingIntent?.let {
            RAlarmManager.cancel(this, it)
        }

        endPendingIntent?.let {
            RAlarmManager.cancel(this, it)
        }

        val timeSpan = calcTimeSpan()

        startPendingIntent = AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java)
        endPendingIntent = AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java)

        if (debugRun) {
            RAlarmManager.setDelay(this, 3_000, endPendingIntent!!)
        } else {
            RAlarmManager.setDelay(this, timeSpan[0], startPendingIntent!!)
            RAlarmManager.setDelay(this, timeSpan[1], endPendingIntent!!)
        }

        updateBottomTip()
    }

    fun updateBottomTip() {
        val spanBuilder = StringBuilder()

        if (!isTaskStart) {
            spanBuilder.append("等待中...")
            RLocalBroadcastManager.sendBroadcast(
                MainActivity.UPDATE_BOTTOM_TIP,
                Bundle().apply { putString("text", "$spanBuilder") })
            return
        }

        val timeSpan = calcTimeSpan()

        if (timeSpan[0] > 0) {
            spanBuilder.append("距离上班还有($startTime)   ${RUtils.formatTime(timeSpan[0] * 1000L)}")
        } else {
            spanBuilder.append("已上班($defaultStartTime)   ${RUtils.formatTime(timeSpan[0].absoluteValue * 1000L)}")
        }

        if (timeSpan[1] > 0) {
            spanBuilder.append("\n距离下班还有($endTime)   ${RUtils.formatTime(timeSpan[1] * 1000L)}")
        }

        RLocalBroadcastManager.sendBroadcast(
            MainActivity.UPDATE_BOTTOM_TIP,
            Bundle().apply { putString("text", "$spanBuilder") })
    }

    /**
     * 返回 正常打卡 范围内的 上下班有效时间间隔
     * */
    fun calcTimeSpan(): LongArray {
        val spiltTime = nowTime().spiltTime()

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

        val startSpan = TimeUtils.getTimeSpanNoAbs(
            "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}",
            startTime,
            ConstUtils.TimeUnit.SEC,
            "HH:mm:ss"
        )

        val endSpan = TimeUtils.getTimeSpanNoAbs(
            "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}",
            endTime,
            ConstUtils.TimeUnit.SEC,
            "HH:mm:ss"
        )

        var startTime = 0L
        var endTime = 0L
        if (defaultStartSpan > 0) {
            //已经上班了
            startTime = -defaultStartSpan
        } else {
            //还差多少秒上班
//            if (startSpan) {
//            }
            startTime = if (startSpan > 0) 1_000 /*已经到了时间,1秒后打卡*/ else startSpan.absoluteValue
        }

        //距离下班还有多少秒
        endTime = if (endSpan > 0) 1_000 /*已经到了时间,1秒后打卡*/ else endSpan.absoluteValue

        return longArrayOf(startTime, endTime)
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
            updateBottomTip()

            val spiltTime = nowTime().spiltTime()

            //隔天, 重置任务 和定时广播
            if (spiltTime[2] != lastDay) {
                resetTime()
            }

            /**采用定时广播的方式实现*/
            if (run) {

                val logTime = "当前时间:${spiltTime[0]}-${spiltTime[1]}-${spiltTime[2]} " +
                        "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}'${spiltTime[6]}"
                L.i(logTime)
                RLocalBroadcastManager.sendBroadcast(
                    MainActivity.UPDATE_BOTTOM_TIP,
                    Bundle().apply { putString("text", logTime) })

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
                                MainActivity.UPDATE_BOTTOM_TIP,
                                Bundle().apply { putString("text", "$logTime\n$startTime 开始上班打卡") })

                            toDingDing()
                        } else {
                            RLocalBroadcastManager.sendBroadcast(
                                MainActivity.UPDATE_BOTTOM_TIP,
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
                                MainActivity.UPDATE_BOTTOM_TIP,
                                Bundle().apply { putString("text", "$logTime\n$endTime 开始下班打卡") })

                            toDingDing()
                        } else {
                            RLocalBroadcastManager.sendBroadcast(
                                MainActivity.UPDATE_BOTTOM_TIP,
                                Bundle().apply { putString("text", "$logTime\n$msg") })
                        }
                    }
                } else {
                    //还没到下班时间
                }
            }
            handler.sendEmptyMessageDelayed(MSG_CHECK_TIME, 1_000)
        }
        return true
    }
}