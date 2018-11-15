package com.angcyo.dingding

import android.graphics.Point
import android.view.accessibility.AccessibilityEvent
import com.angcyo.uiview.less.accessibility.*
import com.angcyo.uiview.less.utils.ScreenUtil.density

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/13
 */
class SystemUIInterceptor : AccessibilityInterceptor() {
    companion object {
        var navigatorBarHeight = 0
        var touch = false
    }

    init {
        filterPackageName = "com.android.systemui"
    }

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)

        if (!DingDingInterceptor.handEvent) {
            return
        }

        if (isWindowStateChanged(event) /*&& touch*/) {
            //无法拿到
//            findNodeById("button1", accService, event).let {
//                L.i("系统界面1:${it.size}")
//
//                if (it.isNotEmpty()) {
//                    val text = it.first().text
//                    text
//                } else {
//                    val a = ""
//                }
//            }

            findNodeByText("立即开始", accService, event).let {
                //                L.i("系统界面2:${it.size}")

                if (it.isNotEmpty()) {
                    it.first().click()
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