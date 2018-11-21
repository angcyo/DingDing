package com.angcyo.dingding

import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.angcyo.dingding.bean.WordBean
import com.angcyo.lib.L
import com.angcyo.uiview.less.accessibility.*
import com.angcyo.uiview.less.kotlin.*
import com.angcyo.uiview.less.manager.Screenshot
import com.orhanobut.hawk.Hawk
import java.lang.ref.WeakReference

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

        var lastWordBean: WordBean? = null

        const val DING_DING = "com.alibaba.android.rimet"

        var onSearchWordEnd: ((WordBean?) -> Unit)? = null

        /*钉钉网络请求延迟操作时间*/
        var HTTP_DELAY = 2_000L
        /*分享图片延迟*/
        var SHARE_DELAY = 10_000L

        /*是否需要处理无障碍事件*/
        var handEvent = false

        var lastBitmap: WeakReference<Bitmap>? = null

        var onCaptureEnd: ((Bitmap) -> Unit)? = null

        /**正在返回*/
        var isBack = false

        fun searchScreenWords(end: ((WordBean?) -> Unit)? = null) {
            screenshot?.setCaptureDelay(3_00)
            screenshot?.startToShot()
            onSearchWordEnd = end
        }

        fun capture(end: ((Bitmap) -> Unit)? = null) {
            L.i("请求捕捉屏幕...")

            screenshot?.setCaptureDelay(1_000)
            screenshot?.startToShot()
            onCaptureEnd = end
        }
    }

    var filterEven = FilterEven(
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "android.widget.FrameLayout", 2000
    )

    /*双击工作tab的时间*/
    var lastDoubleTime = 0L

    init {
        filterPackageName = DING_DING

        screenshot = Screenshot.capture(context) { bitmap, _ ->
            lastBitmap = WeakReference(bitmap)
            //            OCR.general(bitmap.toBase64())
//            OCR.general_basic(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.general(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.accurate(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
            onCaptureEnd?.invoke(bitmap)
            onCaptureEnd = null
            OCR.general(bitmap.toBase64()) {
                lastWordBean = it
                val oldSearchWordEnd = onSearchWordEnd
                onSearchWordEnd = null
                oldSearchWordEnd?.invoke(it)
            }
        }.setAlwaysCapture(true).setAutoCapture(false).setCaptureDelay(3_00)
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshot?.destroy()
        screenshot = null
    }

    override fun onFilterAccessibilityEvent(accService: BaseAccessibilityService, event: AccessibilityEvent) {
        if (!handEvent) {
            return
        }

        if (isBack) {
            return
        }

        if (isWindowStateChanged(event)) {
            if (isMainActivity(accService, event)) {
                filterEventList.add(filterEven)
            } else {
                filterEventList.remove(filterEven)
            }

            if (isLoginActivity(event)) {
                L.i("钉钉登录界面")
                Tip.show("即将为您登录")

                findNodeById("et_phone_input", accService, event).let {
                    L.i("手机输入:${it.size}")
                    if (it.isEmpty()) {

                    } else {
                        it.first().setNodeText(Hawk.get("ding_user", ""))
                    }
                }

                findNodeById("et_pwd_login", accService, event).let {
                    L.i("密码输入:${it.size}")
                    if (it.isEmpty()) {

                    } else {
                        it.first().setNodeText(Hawk.get("ding_pw", ""))
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
                        val nowTime = nowTime()

                        if ((nowTime - lastDoubleTime) > 1_000) {

                            Tip.show("跳转工作Tab")

                            accService.double(it.toPath())

                            lastDoubleTime = nowTime

                            delay(HTTP_DELAY) {
                                jumpToDingCardActivity(accService)
                            }
                        }
                    }
                }
            } else if ("com.alibaba.lightapp.runtime.activity.CommonWebViewActivity" == event.className) {
                //网页浏览界面 打卡界面就是网页
                Tip.show("检查是否是打卡页面.")

                delay(HTTP_DELAY) {
                    checkDingCardActivity(3) {
                        clickCard(accService)
//                        back(accService)
                    }
                }
            } else {
                if (lastAppIsDingDing()) {
                    L.i("未知的界面")
                    if (!TextUtils.isEmpty(event.className) && event.className.contains("Dialog")) {
                        Tip.show("不支持的对话框")
                    } else {
                        Tip.show("不支持的界面")
                    }
                }
            }
        }
    }


    fun jumpToDingCardActivity(accService: BaseAccessibilityService) {
        if (!lastAppIsDingDing()) {
            L.i("请回到钉钉首页")
            Tip.show("请回到钉钉首页.")
            return
        }
        Tip.show("识别WebView")
        accService.move(Path().apply {
            val realRect = accService.displayRealRect()
            L.i("屏幕大小:$realRect")
            moveTo(realRect.centerX().toFloat(), realRect.height().toFloat() * 4f / 5f)
            lineTo(realRect.centerX().toFloat(), realRect.height().toFloat() * 2f / 5f)
        }, object : GestureCallback() {
            override fun onEnd(gestureDescription: GestureDescription?) {
                super.onEnd(gestureDescription)

                searchScreenWords {
                    it?.let {
                        it.getRectByWord("勤打卡").let {
                            if (it.isEmpty) {
                                delay(HTTP_DELAY) {

                                    if (!lastAppIsDingDing()) {
                                        L.i("请回到钉钉首页")
                                        Tip.show("请回到钉钉首页.")
                                    } else {
                                        jumpToDingCardActivity(accService)
                                    }
                                }
                            } else {
                                L.i("跳转打卡页面")
                                if (lastAppIsDingDing()) {
                                    Tip.show("跳转打卡界面")

                                    accService.touch(it.toPath())
                                } else {
                                    L.i("请回到钉钉首页")
                                    Tip.show("请回到钉钉首页")
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    //检查是否是打卡界面
    fun checkDingCardActivity(count: Int, end: () -> Unit) {
        searchScreenWords {
            it?.let {
                if (!it.getRectByWord("上班打卡").isEmpty
                    || !it.getRectByWord("下班打卡").isEmpty
                    || !it.getRectByWord("更新打卡").isEmpty
                    || !it.getRectByWord("外勤打卡").isEmpty
                ) {
                    end.invoke()
                    return@searchScreenWords
                }

                if (it.getRectByWord("打卡").isEmpty ||
                    it.getRectByWord("统计").isEmpty
                ) {
                    if (count >= 0) {
                        Tip.show("重试${count}检查是否是打卡页面.")
                        checkDingCardActivity(count - 1, end)
                    } else {
                        Tip.show("打卡页面识别失败.")
                    }
                } else {
                    end.invoke()
                }
            }
        }
    }

    /**
     * 开始打卡
     * 1.正在定位中...
     * 2.未到下班时间, 弹出早退打卡对话框
     * 3.定位失败
     * */
    fun clickCard(accService: BaseAccessibilityService, retryCount: Int = 3) {
        if (!lastAppIsDingDing()) {
            L.i("请回到钉钉打卡界面")
            return
        }

        Tip.show("正在OCR识别打卡.")

        searchScreenWords {
            it?.let { wordBean ->
                var haveCard = false
                wordBean.getRectByWord("下班打卡").let {
                    L.i("下班打卡:$it")
                    if (!it.isEmpty) {
                        haveCard = true

                        accService.touch(it.toPath())
                    } else {
                        wordBean.getRectByWord("上班打卡").let {
                            L.i("上班打卡:$it")
                            if (!it.isEmpty) {
                                haveCard = true

                                accService.touch(it.toPath())
                            } else {
                                wordBean.getRectByWord("更新打卡").let {
                                    L.i("更新打卡:$it")
                                    if (!it.isEmpty) {
                                        haveCard = true

                                        accService.touch(it.toPath())
                                    } else {
                                        wordBean.getRectByWord("外勤打卡").let {
                                            L.i("外勤打卡:$it")
                                            if (!it.isEmpty) {
                                                Tip.show("请前往公司再打卡")

                                                shareQQ(accService)

                                                return@searchScreenWords
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (haveCard) {
                    //判断早退
                    Tip.show("确定打卡是否成功.")

                    //打卡结果页识别
                    checkCardReult(accService)
                } else {
                    //没有找到
                    if (retryCount <= 0) {
                        //Tip.show("OCR识别失败")
                        checkCardReult(accService, false)
                    } else {
                        Tip.show("重试$retryCount OCR识别")

                        delay(1_000) {
                            clickCard(accService, retryCount - 1)
                        }
                    }
                }
            }

            if (it == null) {
                Tip.show("OCR接口失败,重试.")

                delay(1_000) {
                    clickCard(accService, retryCount - 1)
                }
            }
        }
    }

    fun checkCardReult(accService: BaseAccessibilityService, isInCardUI: Boolean = true /*是否是打卡界面*/, retry: Int = 3) {
        delay(HTTP_DELAY) {
            searchScreenWords {
                it?.let { wordBean ->
                    wordBean.getRectByWord("上班打卡成功").apply {
                        L.i("上班打卡成功:$this")
                        if (!this.isEmpty) {
                            Tip.show("上班打卡成功.")

                            delay(HTTP_DELAY) {
                                searchScreenWords {
                                    wordBean.getRectByWord("我知道了").let {
                                        if (!it.isEmpty) {
                                            accService.touch(it.toPath())

                                            capture {
                                                Tip.show("分享至QQ")

                                                isBack = true
                                                it.share(accService)

                                                delay(SHARE_DELAY) {
                                                    accService.home()
                                                    DING_DING.startApp(accService)

                                                    //上班任务结束, 等待下班
                                                    back(accService)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            return@searchScreenWords
                        }
                    }

                    wordBean.getRectByWord("下班打卡成功").apply {
                        L.i("下班打卡成功:$this")

                        if (!this.isEmpty) {
                            Tip.show("下班打卡成功.")

                            wordBean.getRectByWord("我知道了").let {
                                if (!it.isEmpty) {
                                    accService.touch(it.toPath())

                                    //结束任务, 等待下一轮
                                    handEvent = false

                                    shareQQ(accService)
                                }
                            }
                            return@searchScreenWords
                        }
                    }

                    wordBean.getRectByWord("确定要打早退卡吗").apply {
                        L.i("确定要打早退卡吗:$this")

                        if (!this.isEmpty) {
                            Tip.show("早退是不可能的.")

                            wordBean.getRectByWord("不打卡").let {
                                if (!it.isEmpty) {
                                    accService.touch(it.toPath())

                                    shareQQ(accService)
                                }
                            }
                            return@searchScreenWords
                        }
                    }

                    wordBean.getRectByWord("更新此次打卡记录").let {
                        L.i("更新此次打卡记录:$it")

                        if (!it.isEmpty) {
                            Tip.show("更新打卡记录.")

                            wordBean.getRectByWord("取消确定").let {
                                if (!it.isEmpty) {
                                    accService.touch(Path().apply {
                                        moveTo((it.right - 10).toFloat(), it.centerY().toFloat())
                                    })

                                    checkCardReult(accService)
                                }
                            }
                            return@searchScreenWords
                        }
                    }

                    //对话框
                    wordBean.getRectByWord("我知道了").let {
                        L.i("我知道了:$it")

                        if (!it.isEmpty) {
                            accService.touch(it.toPath())

                            shareQQ(accService)

                            return@searchScreenWords
                        }
                    }

                    //外勤打卡
                    wordBean.getRectByWord("外勤打卡").let {
                        L.i("外勤打卡:$it")

                        if (!it.isEmpty) {
                            Tip.show("请前往公司再打卡")

                            accService.back()

                            shareQQ(accService)

                            return@searchScreenWords
                        }
                    }

                    //开始人脸识别
                    wordBean.getRectByWord("开始人脸识别").let {
                        L.i("开始人脸识别:$it")

                        if (!it.isEmpty) {
                            Tip.show("不支持虚拟打卡外挂.")

                            accService.back()

                            shareQQ(accService)

                            return@searchScreenWords
                        }
                    }

                    if (retry <= 0) {
                        if (isInCardUI) {
                            Tip.show("弹出了什么鬼?")
                        } else {
                            Tip.show("OCR未识别.")
                        }
                    } else {
                        Tip.show("重试$retry 结果识别")

                        checkCardReult(accService, isInCardUI, retry - 1)
                    }
                }
            }
        }
    }

    fun shareQQ(accService: BaseAccessibilityService) {
        capture {
            Tip.show("分享至QQ")

            isBack = true
            it.share(accService)

            delay(SHARE_DELAY) {
                accService.home()

                delay(1_000) {
                    isBack = true

                    Tip.show("开始回退界面,请等待")

                    DING_DING.startApp(accService)

                    delay(5_000) {
                        back(accService)
                    }
                }
            }
        }
    }

    fun lastAppIsDingDing() = BaseAccessibilityService.lastPackageName == DING_DING

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

    fun back(accService: BaseAccessibilityService) {
        isBack = true
        Tip.show("开始回退钉钉界面")

        delay(300) {
            accService.back()
            delay(360) {
                accService.back()
                delay(360) {
                    accService.back()
                    delay(360) {
                        accService.back()
                        isBack = false

                        Tip.show("回退结束.")

                        delay(1_000) {
                            Tip.show("恭喜,流程结束!.")

                            accService.runMain()
                        }
                    }
                }
            }
        }
    }

//com.alibaba.android.rimet
//com.alibaba.lightapp.runtime.activity.CommonWebViewActivity

//com.alibaba.android.rimet
//com.alibaba.android.user.settings.activity.NewSettingActivity

//com.alibaba.android.rimet
//com.alibaba.android.user.login.SignUpWithPwdActivity
}