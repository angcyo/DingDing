package com.angcyo.dingding

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Message
import com.angcyo.lib.L
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.kotlin.*
import com.angcyo.uiview.less.manager.AlarmBroadcastReceiver
import com.angcyo.uiview.less.manager.RAlarmManager
import com.angcyo.uiview.less.manager.RLocalBroadcastManager
import com.angcyo.uiview.less.manager.Screenshot
import com.angcyo.uiview.less.utils.RUtils
import com.angcyo.uiview.less.utils.Root
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
        const val MSG_GET_CONFIG = 1
        const val CMD_TO_DING_DING = 1
        const val CMD_RESET_TIME = 2
        const val CMD_STOP = 3

        /**获取屏幕截图*/
        const val TASK_SHARE_SHOT = 800
        /**立即执行打卡*/
        const val TASK_JUST_DING = 801
        /**分享测试*/
        const val TASK_SHARE_TEST = 802

        @Deprecated("使用定时器控制")
        var run = false

        //默认上下班时间
        var defaultStartTime: String = "08:45:00"
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
            set(value) {
                field = value
                DingDingInterceptor.handEvent = false
            }

        /**Github 数据缓存 有3分钟, 每2分钟检查一次*/
        const val CHECK_TASK_DELAY = 120_000L
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onHandCommand(command: Int, intent: Intent) {
        super.onHandCommand(command, intent)
        Tip.show("执行命令:$command")

        if (command == CMD_TO_DING_DING) {
            RLocalBroadcastManager.sendBroadcast(
                MainActivity.UPDATE_BOTTOM_TIP,
                Bundle().apply { putString("text", "即将开始打卡...") })

            toDingDing()
        } else if (command == CMD_RESET_TIME) {
            handler.sendEmptyMessage(MSG_CHECK_TIME)
            handler.sendEmptyMessageDelayed(MSG_GET_CONFIG, CHECK_TASK_DELAY)

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

            stopSelf()

            RLocalBroadcastManager.sendBroadcast(
                MainActivity.UPDATE_BOTTOM_TIP,
                Bundle().apply { putString("text", "欢迎下次使用.") })

            Tip.hide()
        } else if (command == TASK_SHARE_SHOT) {
            shareScreenshot()
        } else if (command == TASK_JUST_DING) {
            toDingDing()
        } else if (command == TASK_SHARE_TEST) {
            val testBuilder = StringBuilder()
            testBuilder.append("正在测试的消息:\n")
            val nowTime = nowTime().spiltTime()
            testBuilder.append("${nowTime[0]}-${nowTime[1]}-${nowTime[2]} ")
            testBuilder.append("${nowTime[3]}:${nowTime[4]}:${nowTime[5]}:${nowTime[6]} ")
            testBuilder.append("周${nowTime[7]}\n")
            testBuilder.append("是否是节假日:${OCR.isHoliday()}\n")
            testBuilder.append(Root.initImei())
            shareText(testBuilder.toString())
        }
    }

    //上班打卡的定时任务
    @Deprecated("广播不准, 用handler, 延时")
    var startPendingIntent: PendingIntent? = null
    //下班打卡的定时任务
    @Deprecated("广播不准, 用handler, 延时")
    var endPendingIntent: PendingIntent? = null

    private var startRunnable = Runnable {
        sendBroadcast(
            AlarmBroadcastReceiver.getIntent(
                this,
                TimeAlarmReceiver::class.java,
                TimeAlarmReceiver.RUN
            )
        )
    }

    private var endRunnable = Runnable {
        sendBroadcast(
            AlarmBroadcastReceiver.getIntent(
                this,
                TimeAlarmReceiver::class.java,
                TimeAlarmReceiver.RUN
            )
        )
    }

    fun resetTime() {
        LogFile.log("resetTime() 重置任务时间.")

        isTaskStart = true

        startTime = "08:${nextInt(20, 40)}:${nextInt(0, 59)}"
        endTime = "18:${nextInt(0, 30)}:${nextInt(0, 59)}"
        isStartTimeDo = false
        isEndTimeDo = false
        val spiltTime = nowTime().spiltTime()
        //几号
        lastDay = spiltTime[2]

        RLocalBroadcastManager.sendBroadcast(MainActivity.UPDATE_TIME)
        OCR.month()

        startPendingIntent?.let {
            RAlarmManager.cancel(this, it)
        }

        endPendingIntent?.let {
            RAlarmManager.cancel(this, it)
        }

        removeDelayThread(startRunnable)
        removeDelayThread(endRunnable)

        LogFile.log("设置在: $startTime  $endTime  节假日:${OCR.isHoliday()}")

        if (OCR.isHoliday() && !debugRun) {
            //节假日
        } else {
            val timeSpan = calcTimeSpan()

//            startPendingIntent =
//                    AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java, TimeAlarmReceiver.RUN)
//            endPendingIntent =
//                    AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java, TimeAlarmReceiver.RUN)
//
//            if (debugRun) {
//                RAlarmManager.setDelay(this, 3_000, endPendingIntent!!)
//            } else {
//                val builder = StringBuilder()
//                if (timeSpan[0] > 0) {
//                    val startTimeDelay = timeSpan[0].absoluteValue * 1_000L
//                    builder.append("上班任务定时在 ${RUtils.formatTime(startTimeDelay)} 后.\n")
//                    RAlarmManager.setDelay(this, startTimeDelay, startPendingIntent!!)
//                }
//                if (timeSpan[1] < 0) {
//                    //已经下班, 1秒后 更新打卡
//                    RAlarmManager.setDelay(this, 1_000, endPendingIntent!!)
//                } else {
//                    val endTimeDelay = timeSpan[1].absoluteValue * 1_000L
//                    builder.append("下班任务定时在 ${RUtils.formatTime(endTimeDelay)} 后.")
//                    RAlarmManager.setDelay(this, endTimeDelay, endPendingIntent!!)
//
//                    shareText(builder.toString())
//                }
//            }

            if (debugRun) {
                LogFile.log("3秒后, 测试运行打卡流程.")

                postDelayThread(3_000, startRunnable)
            } else {
                val builder = StringBuilder()
                if (timeSpan[0] > 0) {
                    val startTimeDelay = timeSpan[0].absoluteValue * 1_000L
                    builder.append("上班任务定时在 ${RUtils.formatTime(startTimeDelay)} 后.\n")

                    postDelayThread(startTimeDelay, startRunnable)

                    LogFile.log("上班任务定时在 ${RUtils.formatTime(startTimeDelay)} 后.")
                }
                if (timeSpan[1] < 0) {
                    //已经下班, 1秒后 更新打卡
                    postDelayThread(1_000, endRunnable)

                    LogFile.log("已经下班, 1秒后 更新打卡.")
                } else {
                    val endTimeDelay = timeSpan[1].absoluteValue * 1_000L
                    builder.append("下班任务定时在 ${RUtils.formatTime(endTimeDelay)} 后.")
                    postDelayThread(endTimeDelay, endRunnable)

                    shareText(builder.toString())

                    LogFile.log("下班任务定时在 ${RUtils.formatTime(endTimeDelay)} 后.")
                }
            }
        }

        updateBottomTipBroadcast()
    }

    private fun updateBottomTipBroadcast() {
        val spanBuilder = StringBuilder()

        if (!isTaskStart) {
            spanBuilder.append("等待中...")
            RLocalBroadcastManager.sendBroadcast(
                MainActivity.UPDATE_BOTTOM_TIP,
                Bundle().apply { putString("text", "$spanBuilder") })
            return
        }

        if (OCR.isHoliday()) {
            //节假日
            val nowTime = nowTime().spiltTime()
            spanBuilder.append("${nowTime[0]}-${nowTime[1]}-${nowTime[2]} ")
            spanBuilder.append("周${nowTime[7]}\n")

            spanBuilder.append("今天放假哦 ^_^ T_T")
        } else {
            val timeSpan = calcTimeSpan()

            val formatStartTime = RUtils.formatTime(timeSpan[0].absoluteValue * 1000L)
            if (timeSpan[0] < 0) {
                //超过上班时间
                if (timeSpan[1] < 0) {
                    //超过下班时间
                    spanBuilder.append("今天辛苦咯 ^_^ T_T")
                } else {
                    spanBuilder.append("已上班($defaultStartTime)   $formatStartTime")
                }

            } else {
                spanBuilder.append("距离上班还有($startTime)   $formatStartTime")

                if (timeSpan[0] < 10 * 60) {
                    //10分钟内, tip提示
                    Tip.show("即将上班:$formatStartTime")
                }
            }

            if (timeSpan[1] > 0) {
                val formatTime = RUtils.formatTime(timeSpan[1] * 1000L)

                spanBuilder.append("\n距离下班还有($endTime)   $formatTime")

                if (timeSpan[1] < 10 * 60) {
                    //10分钟内, tip提示
                    Tip.show("即将下班:$formatTime")
                }
            } else {
                spanBuilder.append("\n已下班($endTime)   ${RUtils.formatTime(timeSpan[1].absoluteValue * 1000L)}  更新打卡.")
            }
        }

        RLocalBroadcastManager.sendBroadcast(
            MainActivity.UPDATE_BOTTOM_TIP,
            Bundle().apply { putString("text", "$spanBuilder") })
    }

    /**
     * 返回 正常打卡 范围内的 上下班有效时间间隔. 秒
     * */
    fun calcTimeSpan(): LongArray {
        val spiltTime = nowTime().spiltTime()

        val notTimeString = "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}"

        val defaultStartSpan = TimeUtils.getTimeSpanNoAbs(
            notTimeString,
            defaultStartTime,
            ConstUtils.TimeUnit.SEC,
            "HH:mm:ss"
        )

        val defaultEndSpan = TimeUtils.getTimeSpanNoAbs(
            notTimeString,
            defaultEndTime,
            ConstUtils.TimeUnit.SEC,
            "HH:mm:ss"
        )

        val startSpan = TimeUtils.getTimeSpanNoAbs(
            notTimeString,
            startTime,
            ConstUtils.TimeUnit.SEC,
            "HH:mm:ss"
        )

        val endSpan = TimeUtils.getTimeSpanNoAbs(
            notTimeString,
            endTime,
            ConstUtils.TimeUnit.SEC,
            "HH:mm:ss"
        )

        //负数表示已经上班, 正数表示距离上班的时间差
        var startTime = 0L

        //负数表示已下班, 正数表示距离下班的时间差
        var endTime = 0L
        if (defaultStartSpan > 0) {
            //已经上班了
            startTime = -defaultStartSpan
        } else {
            //还差多少秒上班
            startTime = -startSpan
        }

        //距离下班还有多少秒
        endTime = -endSpan

        return longArrayOf(startTime, endTime)
    }

    /**亮屏和解锁*/
    fun wakeUpAndUnlock() {
        if (MainActivity.activity == null || MainActivity.activity?.get() == null) {
            Screenshot.wakeUpAndUnlock(this)
        } else {
            Screenshot.wakeUpAndUnlock(MainActivity.activity!!.get()!!)
        }
    }

    fun toDingDing() {
        wakeUpAndUnlock()

        DingDingInterceptor.handEvent = true
        RUtils.startApp(this, DingDingInterceptor.DING_DING)
    }

    override fun onHandleMessage(msg: Message): Boolean {
        if (msg.what == MSG_CHECK_TIME) {
            updateBottomTipBroadcast()

            val spiltTime = nowTime().spiltTime()

            //隔天, 重置任务 和定时广播
            if (spiltTime[2] != lastDay) {
                resetTime()

                shareTime()
            }

//            if (debugRun) {
//                shareTime()
//            }

            //心跳提示, 用来提示软件还活着
            if (spiltTime[3] == 7 &&
                (spiltTime[4] % (if (BuildConfig.DEBUG) 10 else 10) == 0) &&
                spiltTime[5] == 0
            ) {
                shareTime(true)
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

            if (OCR.configBean.enable == 0) {
                throw IllegalArgumentException("授权过期,请联系作者.")
            }

            handler.sendEmptyMessageDelayed(MSG_CHECK_TIME, 1_000)
        } else if (msg.what == MSG_GET_CONFIG) {
            OCR.loadConfig()

            handler.sendEmptyMessageDelayed(MSG_GET_CONFIG, CHECK_TASK_DELAY)
        }
        return true
    }

    fun shareTime(heart: Boolean = false) {
        L.i("分享心跳")

        wakeUpAndUnlock()

        val spiltTime = nowTime().spiltTime()

        val shareTextBuilder = StringBuilder()
        shareTextBuilder.append("来自`${RUtils.getAppName(this)}`的提醒:")
        shareTextBuilder.append("\n今天的打卡任务已更新:(${spiltTime[0]}-${spiltTime[1]}-${spiltTime[2]})")
        shareTextBuilder.append("\n上班 $startTime")
        shareTextBuilder.append("\n下班 $endTime")

        if (heart) {
            shareTextBuilder.append("\n助手还活着请放心.")
        }

        val old = DingDingInterceptor.handEvent
        DingDingInterceptor.handEvent = true
        shareTextBuilder.toString().share(this)

        gotoMain(old)

        LogFile.log("心跳:$heart")
    }

    fun shareText(text: String) {
        L.i("分享文本")
        wakeUpAndUnlock()

        val old = DingDingInterceptor.handEvent
        DingDingInterceptor.handEvent = true
        text.share(this)

        gotoMain(old)
    }

    fun shareScreenshot() {
        wakeUpAndUnlock()

        DingDingInterceptor.capture {

            val old = DingDingInterceptor.handEvent
            DingDingInterceptor.handEvent = true

            it.share(this)

            gotoMain(old)
        }
    }

    private fun gotoMain(oldHandEvent: Boolean) {
        mainHandler.postDelayed({
            runMain()
            DingDingInterceptor.handEvent = oldHandEvent
        }, 5_000)
    }
}