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

        /**是否点了转发*/
        var isForwardClick = false
    }

    init {
        filterPackageNameList.add("android")
        filterPackageNameList.add(QQ)
        filterPackageNameList.add("com.huawei.android.internal.app")
//        filterPackageNameList.add("com.angcyo.dingding")
    }

    var lastIsForwardRecentActivity = false

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)

//        if (BuildConfig.DEBUG) {
//            if (event.packageName == "com.tencent.mobileqq" || event.packageName == "com.angcyo.dingding") {
//                L.e("------------------------------------------------start")
//                BaseAccessibilityService.logNodeInfo(accService.rootInActiveWindow)
//                BaseAccessibilityService.logNodeInfo(event.source)
//                L.e("------------------------------------------------end")
//            }
//        }

        if (!DingDingInterceptor.handEvent) {
            return
        }

        if (BuildConfig.DEBUG) {
//            if (event.packageName == "android") {
//            BaseAccessibilityService.logNodeInfo(accService.rootInActiveWindow)
//            BaseAccessibilityService.logNodeInfo(event.source)
//            }
        }

        if (isWindowStateChanged(event)) {
            LogFile.share("弹窗包名:\n${accService.rootInActiveWindow?.packageName}\n${event.packageName}\n${event.className}")

            if (event.packageName == "android" || event.packageName == "com.huawei.android.internal.app") {

                isForwardClick = false

                delay(5_00) {
                    //分享选择框
                    findNodeByText("发送给好友", accService, event).let {
                        L.i("发送给好友:${it.size}")

                        LogFile.share("发送给好友 按钮找到${it.size}")

                        delay(5_00) {
                            it.firstOrNull()?.let {
                                LogFile.share("touch坐标: ${it.toRect()}")

                                accService.touch(it.toRect().toPath())
                            }

                            if (it.isEmpty()) {
                                if (DingDingService.isTaskStart || DingDingService.debugRun) {
                                    LogFile.share("正在使用OCR识别 发送给好友")

                                    DingDingInterceptor.searchScreenWords {
                                        it?.let { wordBean ->
                                            wordBean.getRectByWord("发送给好友", "好友").let { rect ->
                                                if (rect.isEmpty) {
                                                    LogFile.share("OCR未找到 发送给好友")

                                                    T_.error("无法识别QQ分享")
                                                    accService.back()
                                                } else {

                                                    LogFile.share("找到 $rect .touch()")

                                                    accService.touch(rect.toPath())
                                                }
                                            }
                                        }

                                        if (it == null) {
                                            T_.error("COR请求失败")
                                            LogFile.share("COR请求失败, back()")

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
                if (lastIsForwardRecentActivity) {
                    if (event.className == "com.tencent.mobileqq.activity.SplashActivity") {
                        LogFile.share("已分享.")
                        return
                    }
                }

                lastIsForwardRecentActivity = event.className == "com.tencent.mobileqq.activity.ForwardRecentActivity"

                LogFile.share("QQ界面 分享给好友 ${event.packageName} ${event.className} $isForwardClick")

                //手机QQ
                if (event.className == "com.tencent.mobileqq.activity.ForwardRecentActivity" ||
                    event.className == "com.tencent.connect.common.AssistActivity" ||
                    event.className == "com.tencent.mobileqq.activity.SplashActivity"
                ) {
                    Tip.show(event.className.substring(event.className.lastIndexOf('.')))

                    isForwardClick = false

                    //寻找好友
                    val qqUsers = Hawk.get("share_qq", "angcyo")
                    val splitUser = qqUsers.split(" ")

                    LogFile.share("分享给好友 $qqUsers")

                    shareToUser(splitUser, 0, accService, event)

                } else if (event.className == "android.app.Dialog" && isForwardClick) {
                    findNodeByText("发送", accService, event).let {
                        L.i("发送:${it.size}")
                        LogFile.share("touch 发送 按钮 ${it.size}")

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

//        if (BuildConfig.DEBUG) {
//            BaseAccessibilityService.logNodeInfo(accService.rootInActiveWindow)
//            BaseAccessibilityService.logNodeInfo(event.source)
//        }

//        delay(1_000) {
        if (users.size > index && !isForwardClick) {
            findNodeByText(users[index], accService, event).let {
                L.w("查找QQ会话 ${users[index]} ${it.size} $isForwardClick")

                LogFile.share("查找QQ会话 ${users[index]} ${it.size}")

                if (it.firstOrNull() == null) {
                    shareToUser(users, index + 1, accService, event)
                } else {
                    it.firstOrNull()?.let {
                        isForwardClick = true

                        L.w("点击:${it.toRect()} $isForwardClick")
                        accService.touch(it.toRect().toPath())

                        LogFile.share("点击QQ会话 ${it.toRect()}")
                    }
                }
            }
        }
//        }
    }
}