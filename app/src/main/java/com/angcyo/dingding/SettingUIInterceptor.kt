package com.angcyo.dingding

import android.view.accessibility.AccessibilityEvent
import com.angcyo.uiview.less.accessibility.AccessibilityInterceptor
import com.angcyo.uiview.less.accessibility.BaseAccessibilityService
import com.angcyo.uiview.less.accessibility.click

/**
 * 电池忽略弹窗
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/13
 */
class SettingUIInterceptor : AccessibilityInterceptor() {
    companion object {
        var touch = false
    }

    init {
        filterPackageName = "com.android.settings"
    }

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)

        if (isWindowStateChanged(event) /*&& touch*/) {
            findNodeByText("允许", accService, event).let {
                //                L.i("系统界面2:${it.size}")

                if (it.isNotEmpty()) {
                    it.last().click()
                }
                touch = false
            }
        }
    }
}