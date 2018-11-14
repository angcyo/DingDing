package com.angcyo.dingding

import android.content.Intent
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
        addInterceptor(SecurityUIInterceptor())

        Tip.show("助手已准备")

        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        DingDingInterceptor.screenshot?.destroy()
    }
}