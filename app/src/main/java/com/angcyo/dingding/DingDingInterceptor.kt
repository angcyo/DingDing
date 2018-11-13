package com.angcyo.dingding

import android.view.accessibility.AccessibilityEvent
import com.angcyo.uiview.less.accessibility.AccessibilityInterceptor
import com.angcyo.uiview.less.accessibility.BaseAccessibilityService

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/10/31
 */
class DingDingInterceptor : AccessibilityInterceptor() {
    init {
        filterPackageName = "com.alibaba.android.rimet"
    }

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)
        //L.i("切换到")
    }

    fun isLoginActivity(event: AccessibilityEvent): Boolean {
        return "com.alibaba.android.user.login" == event.className
    }
}