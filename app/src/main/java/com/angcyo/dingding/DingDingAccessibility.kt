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
        addInterceptor(DingDingInterceptor())
    }
}