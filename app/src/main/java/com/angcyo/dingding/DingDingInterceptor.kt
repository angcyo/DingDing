package com.angcyo.dingding

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.angcyo.lib.L
import com.angcyo.uiview.less.accessibility.*
import com.angcyo.uiview.less.kotlin.toBase64
import com.angcyo.uiview.less.manager.Screenshot

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/10/31
 */
class DingDingInterceptor(context: Context) : AccessibilityInterceptor() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var screenshot: Screenshot? = null

        var lastBitmap: Bitmap? = null
    }

    /**是否需要处理截屏数据*/
    var handlerEvent = false

    init {
        filterPackageName = "com.alibaba.android.rimet"

        screenshot = Screenshot.capture(context) { bitmap, _ ->
            //            OCR.general(bitmap.toBase64())
//            OCR.general_basic(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.general(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.accurate(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
            if (handlerEvent) {
                OCR.general(bitmap.toBase64())
            } else {
                lastBitmap = bitmap
            }
        }.setAlwaysCapture(true).setCaptureDelay(1_000)
    }

    override fun onAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        super.onAccessibilityEvent(accService, event)
        //L.i("切换到")
        if (isWindowStateChanged(event)) {
            if (isLoginActivity(event)) {
                L.i("钉钉登录界面")

                findNodeById("et_phone_input", accService, event).let {
                    L.i("手机输入:${it.size}")
                    if (it.isEmpty()) {

                    } else {
                        it.first().setNodeText("18575683884")
                    }
                }

                findNodeById("et_pwd_login", accService, event).let {
                    L.i("密码输入:${it.size}")
                    if (it.isEmpty()) {

                    } else {
                        it.first().setNodeText("")
                    }
                }

                findNodeById("btn_next", accService, event).let {
                    L.i("登录:${it.size}")
                    if (it.isEmpty()) {

                    } else {
                        it.first().click()
                    }
                }
            } else if (isMainActivity(accService, event)) {
                L.i("钉钉首页界面")

                findBottomRect(accService, findRectByText("工作", accService, event)).let {
                    L.i("工作:$it")
                    if (!it.isEmpty) {
                        accService.touch(it.toPath())

                        delay(600) {
                            accService.move(Path().apply {
                                val realRect = accService.displayRealRect()
                                moveTo(realRect.centerX().toFloat(), realRect.centerY().toFloat())
                                lineTo(realRect.centerX().toFloat(), realRect.centerY().toFloat() - 400)
                            })
                        }
                    }
                }
            }
        }
    }

    fun isLoginActivity(event: AccessibilityEvent): Boolean {
        return "com.alibaba.android.user.login.SignUpWithPwdActivity" == event.className
    }

    fun isMainActivity(accService: BaseAccessibilityService, event: AccessibilityEvent): Boolean {
        var result = false
        if ("android.widget.FrameLayout" == event.className) {
            findNodeByText("消息", accService, event).let {
                L.i("消息:${it.size}")
            }
            findNodeByText("DING", accService, event).let {
                L.i("DING:${it.size}")
            }
            findNodeByText("工作", accService, event).let {
                L.i("工作:${it.size}")
            }
            findNodeByText("通讯录", accService, event).let {
                L.i("通讯录:${it.size}")
            }
            findNodeByText("我的", accService, event).let {
                L.i("我的:${it.size}")
            }
            findNodeById("bottom_tab", accService, event).let {
                L.i("bottom_tab:${it.size}")
                result = it.isNotEmpty()
            }
        }
        return result
    }

    //com.alibaba.android.rimet
    //com.alibaba.lightapp.runtime.activity.CommonWebViewActivity

    //com.alibaba.android.rimet
    //com.alibaba.android.user.settings.activity.NewSettingActivity

    //com.alibaba.android.rimet
    //com.alibaba.android.user.login.SignUpWithPwdActivity
}