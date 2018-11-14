package com.angcyo.dingding

import com.angcyo.uiview.less.accessibility.BaseAccessibilityService

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

        Tip.show("助手已准备")
    }

    override fun onDestroy() {
        super.onDestroy()
        DingDingInterceptor.screenshot?.destroy()
    }
}