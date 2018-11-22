package com.angcyo.dingding

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.angcyo.lib.L
import com.angcyo.uiview.less.accessibility.*
import com.angcyo.uiview.less.utils.T_
import com.orhanobut.hawk.Hawk

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/15
 */
class ShareQQInterceptor : AccessibilityInterceptor() {

    companion object {
        const val QQ = "com.tencent.mobileqq"
    }

    init {
        filterPackageNameList.add("android")
        filterPackageNameList.add(QQ)
        filterPackageNameList.add("com.huawei.android.internal.app")
    }

    /**是否点了转发*/
    var isForwardClick = false

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)

        if (!DingDingInterceptor.handEvent) {
            return
        }

        if (BuildConfig.DEBUG) {
            if (event.packageName == "android") {
                BaseAccessibilityService.logNodeInfo(accService.rootInActiveWindow)
                BaseAccessibilityService.logNodeInfo(event.source)
            }
        }

        if (isWindowStateChanged(event)) {
            if (event.packageName == "android" || event.packageName == "com.huawei.android.internal.app") {

                isForwardClick = false

                LogFile.log("弹窗包名: ${event.packageName} ")

                delay(5_00) {
                    //分享选择框
                    findNodeByText("发送给好友", accService, event).let {
                        L.i("发送给好友:${it.size}")

                        LogFile.log("发送给好友 找到${it.size}")

                        delay(5_00) {
                            it.firstOrNull()?.let {
                                LogFile.log("坐标: ${it.toRect()}")

                                accService.touch(it.toRect().toPath())
                            }

                            if (it.isEmpty()) {
                                if (DingDingService.isTaskStart || DingDingService.debugRun) {
                                    LogFile.log("正在使用OCR识别 发送给好友")

                                    DingDingInterceptor.searchScreenWords {
                                        it?.let { wordBean ->
                                            wordBean.getRectByWord("发送给好友", "好友").let { rect ->
                                                if (rect.isEmpty) {
                                                    LogFile.log("OCR未找到 发送给好友")

                                                    T_.error("无法识别QQ分享")
                                                    accService.back()
                                                } else {

                                                    LogFile.log("找到 $rect .touch()")

                                                    accService.touch(rect.toPath())
                                                }
                                            }
                                        }

                                        if (it == null) {
                                            T_.error("COR请求失败")
                                            LogFile.log("COR请求失败, back()")

                                            accService.back()
                                        }
                                    }
                                } else {
                                    if (Build.MODEL == "OPPO A83") {
                                        T_.error("不支持 OPPO A83")
                                    } else {
                                        Tip.show("请先安装QQ")
                                    }
                                    accService.back()
                                }
                            }
                        }
                    }
                }
            } else if (event.packageName == "com.tencent.mobileqq") {
                LogFile.log("分享给好友 ${event.packageName} ${event.className}")

                //手机QQ
                if (event.className == "com.tencent.mobileqq.activity.ForwardRecentActivity") {
                    //寻找好友
                    val qqUsers = Hawk.get("share_qq", "angcyo")
                    val splitUser = qqUsers.split(" ")

                    LogFile.log("分享给好友 $qqUsers")

                    shareToUser(splitUser, 0, accService, event)

                } else if (event.className == "android.app.Dialog" && isForwardClick) {
                    findNodeByText("发送", accService, event).let {
                        L.i("发送:${it.size}")
                        LogFile.log("发送 按钮 ${it.size}")

                        it.lastOrNull()?.let {
                            isForwardClick = false
                            //accService.touch(it.toRect().toPath())
                            it.click()
                        }
                    }
                } else {
                    isForwardClick = false
                }
            }
        }
    }

    fun shareToUser(users: List<String>, index: Int, accService: BaseAccessibilityService, event: AccessibilityEvent) {
        if (users.size > index) {
            findNodeByText(users[index], accService, event).let {
                LogFile.log("查找QQ会话 ${users[index]} ${it.size}")

                if (it.firstOrNull() == null) {
                    shareToUser(users, index + 1, accService, event)
                } else {
                    it.firstOrNull()?.let {
                        isForwardClick = true
                        accService.touch(it.toRect().toPath())

                        LogFile.log("点击QQ会话 ${it.toRect()}")
                    }
                }
            }
        }
    }
}