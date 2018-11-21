package com.angcyo.dingding

import android.content.Context
import android.content.Intent
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.kotlin.runMain
import com.angcyo.uiview.less.manager.AlarmBroadcastReceiver

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/15
 */
class TimeAlarmReceiver : AlarmBroadcastReceiver() {

    companion object {
        const val RUN = "com.angcyo.ding.run"
    }

    override fun onReceive(context: Context, intent: Intent, action: String) {
        if (RUN.equals(action, ignoreCase = true)) {
            Tip.show("定时:开始打卡")
            BaseService.start(context, DingDingService::class.java, DingDingService.CMD_TO_DING_DING)
        } else if (Intent.ACTION_SCREEN_OFF.equals(action, ignoreCase = true)) {
            if (DingDingService.isTaskStart) {
                context.runMain()
            }
        }
    }
}