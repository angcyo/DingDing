package com.angcyo.dingding

import android.view.accessibility.AccessibilityEvent
import com.angcyo.uiview.less.accessibility.BaseAccessibilityService
import com.angcyo.uiview.less.kotlin.runActivity
import com.angcyo.uiview.less.utils.RUtils

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/13
 */
class DingDingAccessibility : BaseAccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        addInterceptor(DingDingInterceptor(this))
        addInterceptor(SystemUIInterceptor())
        addInterceptor(SecurityUIInterceptor())
        addInterceptor(SettingUIInterceptor())
        addInterceptor(ShareQQInterceptor())

        Tip.show("助手已准备")

        runActivity(MainActivity::class.java)
        RUtils.saveToSDCardFolder("run_main", "DingDingAccessibility")
    }

    override fun onDestroy() {
        super.onDestroy()
        Tip.show("助手已断开服务.")

        DingDingInterceptor.screenshot?.destroy()
    }

    override fun checkLastPackageName(event: AccessibilityEvent) {
        super.checkLastPackageName(event)

        //LogFile.acc("lastPackageName:$lastPackageName")
        LogFile.acc(
            "切换到:${AccessibilityEvent.eventTypeToString(event.eventType)}" +
                    "\n主:${rootInActiveWindow.packageName}" +
                    "\n副:${event.packageName}" +
                    "\n类:${event.className} ${event.action}"
        )
    }
}