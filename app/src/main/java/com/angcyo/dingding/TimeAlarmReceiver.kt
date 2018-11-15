package com.angcyo.dingding

import android.content.Context
import android.content.Intent
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.manager.AlarmBroadcastReceiver

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/15
 */
class TimeAlarmReceiver : AlarmBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent, action: String) {
        if (ACTION_ALARM.equals(action, ignoreCase = true)) {
            BaseService.start(context, DingDingService::class.java, DingDingService.CMD_TO_DING_DING)
        }
    }
}