package com.angcyo.dingding

import android.graphics.Point
import android.view.accessibility.AccessibilityEvent
import com.angcyo.dingding.SystemUIInterceptor.Companion.navigatorBarHeight
import com.angcyo.uiview.less.accessibility.*
import com.angcyo.uiview.less.utils.ScreenUtil.density

/**
 * 后台启动应用, 会弹窗
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/13
 */
class SecurityUIInterceptor : AccessibilityInterceptor() {
    companion object {
        var touch = false
    }

    init {
        filterPackageName = "com.miui.securitycenter"
    }

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)

        if (isWindowStateChanged(event) /*&& touch*/) {
            findNodeByText("允许", accService, event).let {
                if (it.isNotEmpty()) {
                    it.last().click()
                } else {
                    //需要考虑导航栏
                    if (isWindowStateChanged(event) && touch) {
                        val realSize = accService.displayRealSize()

                        accService.touch(
                            Point(
                                (realSize.x * 3f / 4f).toInt(),
                                (realSize.y - navigatorBarHeight - 60 * density()).toInt()
                            )
                        )
                    }
                }
                touch = false
            }
        }
    }
}