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
import com.orhanobut.hawk.Hawk
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

        @Volatile
        var threadRun = false

        const val DEFAULT_PATTERN = "HH:mm"
        const val DEFAULT_PATTERN_CALC_SPAN = "HH:mm:ss"

        //默认上下班时间
        var defaultStartTime: String
            get() = Hawk.get("defaultStartTime", "08:45")
            set(value) {
                Hawk.put("defaultStartTime", value)
            }

        var defaultEndTime: String
            get() = Hawk.get("defaultEndTime", "18:01")
            set(value) {
                Hawk.put("defaultEndTime", value)
            }

        var defaultDelayTime: String
            get() = Hawk.get("defaultDelayTime", "30")
            set(value) {
                Hawk.put("defaultDelayTime", value)
            }

        /**随机产生上下班时间*/
        //上下班时间
        var startTime: String = "08:30:00"
        var endTime: String = "18:10:00"

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
        var CHECK_TIME_DELAY = if (BuildConfig.DEBUG) 10_000L else 1_000L

        var lastHitTime = 0L
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
            resetTime()

            threadRun = true
            ThreadTick().start()

            Tip.show("助手挂机中...")

            handler.sendEmptyMessageDelayed(MSG_CHECK_TIME, CHECK_TIME_DELAY)
            handler.sendEmptyMessageDelayed(MSG_GET_CONFIG, CHECK_TASK_DELAY)
        } else if (command == CMD_STOP) {
            threadRun = false
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
        if (isStartTimeDo) {
            return@Runnable
        }
        isStartTimeDo = true

        LogFile.log("startRunnable run.")

        BaseService.start(this, DingDingService::class.java, DingDingService.CMD_TO_DING_DING)

        sendBroadcast(
            AlarmBroadcastReceiver.getIntent(
                this,
                TimeAlarmReceiver::class.java,
                TimeAlarmReceiver.RUN
            )
        )
    }

    private var endRunnable = Runnable {
        if (isEndTimeDo) {
            return@Runnable
        }
        isEndTimeDo = true

        LogFile.log("endRunnable run.")

        BaseService.start(this, DingDingService::class.java, DingDingService.CMD_TO_DING_DING)

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

        val delayMillis = defaultDelayTime.toInt() * 60 * 1000L
        val delayStart = nextInt(defaultDelayTime.toInt()) * 60 * 1000L
        val delayEnd = nextInt(defaultDelayTime.toInt()) * 60 * 1000L

        startTime = (defaultStartTime.toMillis(DEFAULT_PATTERN) - delayMillis + delayStart)
            .toTime(DEFAULT_PATTERN_CALC_SPAN)
        endTime = (defaultEndTime.toMillis(DEFAULT_PATTERN) + delayMillis - delayEnd)
            .toTime(DEFAULT_PATTERN_CALC_SPAN)

        //startTime = "08:${nextInt(20, 40)}:${nextInt(0, 59)}"
        //endTime = "18:${nextInt(0, 30)}:${nextInt(0, 59)}"

        isStartTimeDo = false
        isEndTimeDo = false
        val spiltTime = nowTime().spiltTime()
        //几号
        lastDay = spiltTime[2]

        LogFile.log("设置在: $startTime  $endTime  节假日:${OCR.isHoliday()}")

        RLocalBroadcastManager.sendBroadcast(MainActivity.UPDATE_TIME)
        OCR.month()

        updateBroadcast(true)
        updateBottomTipBroadcast()
    }

    private fun updateBroadcast(shareText: Boolean = false) {
        L.v("更新任务定时器. 跳过: (${isStartTimeDo || isEndTimeDo})")
        LogFile.log("更新任务定时器.  跳过: (${isStartTimeDo || isEndTimeDo})")

        if (isStartTimeDo || isEndTimeDo) {
            return
        }

        if (Screenshot.isScreenOn(this)) {

        } else {
            runMain()
        }

        wakeUpAndUnlock(Runnable {
            LogFile.log("更新任务 唤醒成功.")
            Tip.show("唤醒成功.")
        })

        startPendingIntent?.let {
            RAlarmManager.cancel(this, it)
        }

        endPendingIntent?.let {
            RAlarmManager.cancel(this, it)
        }

        removeDelayThread(startRunnable)
        removeDelayThread(endRunnable)

        if (OCR.isHoliday() && !debugRun) {
            //节假日
        } else {
            val timeSpan = calcTimeSpan()

            startPendingIntent =
                    AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java, TimeAlarmReceiver.RUN)
            endPendingIntent =
                    AlarmBroadcastReceiver.getPendingIntent(this, TimeAlarmReceiver::class.java, TimeAlarmReceiver.RUN)
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
                    builder.append("上班任务($startTime)定时在 ${RUtils.formatTime(startTimeDelay)} 后.\n")

                    RAlarmManager.setDelay(this, startTimeDelay, startPendingIntent!!)
                    postDelayThread(startTimeDelay, startRunnable)

                    if (shareText) {
                        LogFile.log("上班任务($startTime)定时在 ${RUtils.formatTime(startTimeDelay)} 后.")
                    }
                }
                if (timeSpan[1] < 0) {
                    //已经下班, 1秒后 更新打卡
                    postDelayThread(1_000, endRunnable)

                    if (shareText) {
                        LogFile.log("已经下班, 1秒后 更新打卡.")
                    }
                } else {
                    val endTimeDelay = timeSpan[1].absoluteValue * 1_000L
                    builder.append("下班任务($endTime)定时在 ${RUtils.formatTime(endTimeDelay)} 后.")

                    RAlarmManager.setDelay(this, endTimeDelay, endPendingIntent!!)
                    postDelayThread(endTimeDelay, endRunnable)

                    if (shareText) {
                        if (timeSpan[0] > 60 || timeSpan[1] > 60) {
                            //短时间就执行的任务, 不执行分享
                            shareText(builder.toString())
                        }
                        LogFile.log("下班任务($endTime)定时在 ${RUtils.formatTime(endTimeDelay)} 后.")
                    }
                }
            }
        }

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
            defaultStartTime.toMillis(DEFAULT_PATTERN).toTime(DEFAULT_PATTERN_CALC_SPAN),
            ConstUtils.TimeUnit.SEC,
            DEFAULT_PATTERN_CALC_SPAN
        )

        val defaultEndSpan = TimeUtils.getTimeSpanNoAbs(
            notTimeString,
            defaultEndTime.toMillis(DEFAULT_PATTERN).toTime(DEFAULT_PATTERN_CALC_SPAN),
            ConstUtils.TimeUnit.SEC,
            DEFAULT_PATTERN_CALC_SPAN
        )

        val startSpan = TimeUtils.getTimeSpanNoAbs(
            notTimeString,
            startTime,
            ConstUtils.TimeUnit.SEC,
            DEFAULT_PATTERN_CALC_SPAN
        )

        val endSpan = TimeUtils.getTimeSpanNoAbs(
            notTimeString,
            endTime,
            ConstUtils.TimeUnit.SEC,
            DEFAULT_PATTERN_CALC_SPAN
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
    fun wakeUpAndUnlock(succeededAction: Runnable) {
        LogFile.log("唤醒屏幕: ${MainActivity.activity}")

        if (MainActivity.activity == null || MainActivity.activity?.get() == null) {
            Screenshot.wakeUpAndUnlock(this, true, succeededAction)
        } else {
            LogFile.log("唤醒屏幕do: ${MainActivity.activity!!.get()!!}")

            Screenshot.wakeUpAndUnlock(MainActivity.activity!!.get()!!, true, succeededAction)
        }
    }

    fun toDingDing() {
        wakeUpAndUnlock(Runnable {
            DingDingInterceptor.handEvent = true
            RUtils.startApp(this, DingDingInterceptor.DING_DING)
        })
    }

    override fun onHandleMessage(msg: Message): Boolean {
        if (msg.what == MSG_CHECK_TIME) {
            updateBottomTipBroadcast()

            val spiltTime = nowTime().spiltTime()

            //隔天, 重置任务 和定时广播
            if (spiltTime[2] != lastDay) {
                OCR.ocr_count = 0

                resetTime()

                shareTime()
            }

//            if (debugRun) {
//                shareTime()
//            }

            val startTimeLong = startTime.toMillis(DEFAULT_PATTERN).spiltTime()
            val endTimeLong = endTime.toMillis(DEFAULT_PATTERN).spiltTime()

            //心跳提示, 用来提示软件还活着
            if (spiltTime[3] in (startTimeLong[3] - 1..startTimeLong[3]) ||
                spiltTime[3] in (endTimeLong[3] - 1..endTimeLong[3])
            ) {
                val nowTime = nowTime()
                if (nowTime - lastHitTime >= 30 * 60 * 1000L) {
                    //30分钟通知一次

                    //上下班打卡快到时, 唤醒屏幕. 增加 handler延迟的命中率
                    updateBroadcast()
                    //shareTime(true)
                    Tip.show("Ready 请保持屏幕常亮.")

                    lastHitTime = nowTime
                }
                //gotoMain(DingDingInterceptor.handEvent)
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
                    DEFAULT_PATTERN
                )

                val defaultEndSpan = TimeUtils.getTimeSpanNoAbs(
                    "${spiltTime[3]}:${spiltTime[4]}:${spiltTime[5]}",
                    defaultEndTime,
                    ConstUtils.TimeUnit.SEC,
                    DEFAULT_PATTERN
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
                            DEFAULT_PATTERN
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
                            DEFAULT_PATTERN
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

            LogFile.timeTick()

            handler.sendEmptyMessageDelayed(MSG_CHECK_TIME, CHECK_TIME_DELAY)
        } else if (msg.what == MSG_GET_CONFIG) {
            OCR.loadConfig()

            handler.sendEmptyMessageDelayed(MSG_GET_CONFIG, CHECK_TASK_DELAY)
        }
        return true
    }

    fun shareTime(heart: Boolean = false) {
        L.i("分享心跳")

        if (isStartTimeDo || isEndTimeDo) {
            return
        }

        wakeUpAndUnlock(Runnable {
            val spiltTime = nowTime().spiltTime()

            val shareTextBuilder = StringBuilder()
            shareTextBuilder.append("来自`${RUtils.getAppName(this)}`的提醒:")
            if (!heart) {
                shareTextBuilder.append("\n今天的打卡任务已更新:(${spiltTime[0]}-${spiltTime[1]}-${spiltTime[2]})")
            }
            shareTextBuilder.append("\n上班 $startTime")
            shareTextBuilder.append("\n下班 $endTime")

            if (heart) {
                shareTextBuilder.append("\n助手还活着请放心.")
            }

            if (isStartTimeDo || isEndTimeDo) {

            } else {
                shareText(shareTextBuilder.toString())
            }

            LogFile.log("心跳:$heart")
            LogFile.log("上班 $startTime  下班 $endTime")
        })
    }

    fun shareText(text: String) {
        if (isStartTimeDo || isEndTimeDo) {
            return
        }

        L.i("分享文本")

        LogFile.log("分享文本:$text")

        wakeUpAndUnlock(Runnable {

            if (isStartTimeDo || isEndTimeDo) {

            } else {
                val old = DingDingInterceptor.handEvent
                DingDingInterceptor.handEvent = true
                text.share(this)

                gotoMain(old)
            }
        })
    }

    fun shareScreenshot() {
        if (isStartTimeDo || isEndTimeDo) {
            return
        }

        wakeUpAndUnlock(Runnable {
            DingDingInterceptor.capture {

                val old = DingDingInterceptor.handEvent
                DingDingInterceptor.handEvent = true

                it.share(this)

                gotoMain(old)
            }
        })
    }

    private fun gotoMain(oldHandEvent: Boolean) {
        mainHandler.postDelayed({

            if (isStartTimeDo || isEndTimeDo) {
                return@postDelayed
            }

            runMain()
            DingDingInterceptor.handEvent = oldHandEvent
        }, 5_000)
    }

    override fun onDestroy() {
        super.onDestroy()
        LogFile.log("打卡服务:onDestroy")
        threadRun = false
    }

    class ThreadTick : Thread() {
        override fun run() {
            super.run()

            while (threadRun) {
                LogFile.threadTick()
                sleep(1_000)
            }
        }
    }
}