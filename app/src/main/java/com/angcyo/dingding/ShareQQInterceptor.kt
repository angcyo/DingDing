package com.angcyo.dingding

import android.view.accessibility.AccessibilityEvent
import com.angcyo.lib.L
import com.angcyo.uiview.less.accessibility.*
import com.orhanobut.hawk.Hawk

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/15
 */
class ShareQQInterceptor : AccessibilityInterceptor() {
    init {
        filterPackageNameList.add("android")
        filterPackageNameList.add("com.tencent.mobileqq")
    }

    /**是否点了转发*/
    var isForwardClick = false

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)
        if (isWindowStateChanged(event)) {
            if (event.packageName == "android") {
                //分享选择框
                findNodeByText("发送给好友", accService, event).let {
                    L.i("发送给好友:${it.size}")
                    it.firstOrNull()?.let {
                        accService.touch(it.toRect().toPath())
                    }
                }
            } else if (event.packageName == "com.tencent.mobileqq") {
                //手机QQ
                if (event.className == "com.tencent.mobileqq.activity.ForwardRecentActivity") {
                    //寻找好友
                    val qq = Hawk.get("share_qq", "angcyo")
                    findNodeByText(qq, accService, event).let {
                        L.i("$qq:${it.size}")
                        it.firstOrNull()?.let {
                            isForwardClick = true
                            accService.touch(it.toRect().toPath())
                        }
                    }
                } else if (event.className == "android.app.Dialog" && isForwardClick) {
                    findNodeByText("发送", accService, event).let {
                        L.i("发送:${it.size}")
                        it.lastOrNull()?.let {
                            isForwardClick = false
                            //accService.touch(it.toRect().toPath())
                            it.click()
                        }
                    }
                } else {
                    isForwardClick = true
                }
            }
        }
    }
}