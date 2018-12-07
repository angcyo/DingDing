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

        /**打卡命中按钮提示*/
        val accCardStringBuilder = StringBuilder()

        /**登录次数, 打卡期内, 只登陆2次*/
        var loginCount
            get() = Hawk.get("login_count", 0)
            set(value) {
                Hawk.put("login_count", value)
            }

        var lastIsLoginActivity = false
    }

    var filterEven = FilterEven(
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "android.widget.FrameLayout", 2000
    )

    /*双击工作tab的时间*/
    var lastDoubleTime = 0L

    var isCheckCardUI = false

    val workTabPath = Path()

    init {
        filterPackageName = DING_DING

        screenshot = Screenshot.capture(context) { bitmap, _ ->
            L.i("拿到屏幕截图:$bitmap")
            lastBitmap = WeakReference(bitmap)
            //            OCR.general(bitmap.toBase64())
//            OCR.general_basic(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.general(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
//            OCR.accurate(BitmapFactory.decodeFile("/sdcard/test.jpg").toBase64())
            onCaptureEnd?.invoke(bitmap)
            onCaptureEnd = null
            OCR.ocr(bitmap.toBase64()) {
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
                lastIsLoginActivity = true

                val currentLoginCount = DingDingInterceptor.loginCount
                if (currentLoginCount >= 2) {
                    Tip.show("登录次数超限")
                    LogFile.log("登录次数超限:$currentLoginCount")
                    return
                }

                Tip.show("即将为您登录")
                LogFile.log("即将为您登录")

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

                        DingDingInterceptor.loginCount = currentLoginCount + 1
                    }
                }
            } else if (isMainActivity(accService, event)) {
                lastIsLoginActivity = false

                L.i("钉钉首页界面")
                findBottomRect(accService, findRectByText("工作", accService, event)).let {
                    L.i("工作:$it")

                    LogFile.touch("find 工作 Tab $it")

                    if (!it.isEmpty) {
                        val nowTime = nowTime()

                        if ((nowTime - lastDoubleTime) > 2_000) {

                            Tip.show("跳转工作Tab")

                            LogFile.acc("双击:$it")
                            L.i("双击:$it")
                            accService.double(it.toPath().apply {
                                workTabPath.set(this)
                            })

                            lastDoubleTime = nowTime

                            delay(HTTP_DELAY) {
                                jumpToDingCardActivity(accService)
                            }
                        }
                    }
                }
            } else if ("com.alibaba.lightapp.runtime.activity.CommonWebViewActivity" == event.className) {
                lastIsLoginActivity = false

                //网页浏览界面 打卡界面就是网页
                Tip.show("检查是否是打卡页面.")

                LogFile.acc("检查是否是打卡页面.")

                delay(HTTP_DELAY) {
                    if (isCheckCardUI) {
                        LogFile.acc("已经在check card ui.")
                    } else {
                        checkDingCardActivity(3) {
                            //                        if (Build.MODEL == "OPPO A83") {
//                            //oppo 手机分享图片 经常不成功. 杀掉QQ, 再分享提高成功率
//                            accService.kill(ShareQQInterceptor.QQ)
//                        }
                            isCheckCardUI = true
                            DingDingInterceptor.accCardStringBuilder.clear()
                            clickCard(accService)
//                        back(accService)
                        }
                    }
                }
            } else {
                if (lastAppIsDingDing()) {
                    L.i("未知的界面") //不支持的界面
                    if (!TextUtils.isEmpty(event.className) && event.className.contains("Dialog")) {
                        Tip.show("${event.className}")
                    } else {
                        Tip.show("${event.className}")
                    }
                }
            }
        }
    }


    fun jumpToDingCardActivity(accService: BaseAccessibilityService, retryCount: Int = 3) {
        if (lastIsLoginActivity) {
            LogFile.acc("被踢出登录界面.")
            return
        }

        if (!lastAppIsDingDing()) {
            L.i("请回到钉钉首页1 last:${BaseAccessibilityService.lastPackageName}")
            Tip.show("请回到钉钉首页.")

            LogFile.acc("请回到钉钉首页1 last:${BaseAccessibilityService.lastPackageName}")
            return
        }
        if (retryCount <= 0) {
            //重试失败, 回到顶部
            Tip.show("重试失败, 回到顶部")

            LogFile.acc("重试失败, 回到顶部")

            accService.double(workTabPath)

            delay(HTTP_DELAY) {
                jumpToDingCardActivity(accService)
            }
            return
        }

        Tip.show("识别WebView $retryCount")
        accService.move(Path().apply {
            val realRect = accService.displayRealRect()
            val y1 = realRect.height().toFloat() * 3f / 4f
            val y2 = realRect.height().toFloat() * 1f / 4f

            L.i("屏幕大小:$realRect  开始滚动:$y1 -> $y2")
            moveTo(realRect.centerX().toFloat(), y1)
            lineTo(realRect.centerX().toFloat(), y2)

            LogFile.acc("屏幕大小:$realRect  开始滚动:$y1 -> $y2")

            Tip.show("开始滚动:$y1 -> $y2")

            LogFile.touch("屏幕大小:$realRect  开始滚动:$y1 -> $y2")
        }, object : GestureCallback() {
            override fun onEnd(gestureDescription: GestureDescription?) {
                super.onEnd(gestureDescription)

                LogFile.acc("滚动结束")
                L.i("滚动结束")
                LogFile.touch("滚动结束")

                searchScreenWords {
                    it?.let {
                        Tip.show("识别 `考勤打卡`")

                        it.getRectByWord("勤打卡").let {

                            LogFile.acc("查找 `勤打卡` 坐标. $it")
                            LogFile.touch("查找 `勤打卡` 坐标. $it")

                            if (it.isEmpty) {
                                delay(HTTP_DELAY) {
                                    if (!lastAppIsDingDing()) {
                                        L.i("请回到钉钉首页2 last:${BaseAccessibilityService.lastPackageName}")
                                        Tip.show("请回到钉钉首页.")

                                        LogFile.acc("请回到钉钉首页2 last:${BaseAccessibilityService.lastPackageName}")
                                    } else {
                                        jumpToDingCardActivity(accService, retryCount - 1)
                                    }
                                }
                            } else {
                                L.i("跳转打卡页面")
                                if (lastAppIsDingDing()) {
                                    Tip.show("跳转打卡界面")

                                    accService.touch(it.toPath())
                                } else {
                                    L.i("请回到钉钉首页3, last:${BaseAccessibilityService.lastPackageName}")
                                    Tip.show("请回到钉钉首页")

                                    LogFile.acc("请回到钉钉首页3 last:${BaseAccessibilityService.lastPackageName}")
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
            L.i("请回到钉钉打卡界面 ${BaseAccessibilityService.lastPackageName}")
            LogFile.log("请回到钉钉打卡界面 ${BaseAccessibilityService.lastPackageName}")

            isCheckCardUI = false
            return
        }

        Tip.show("正在OCR识别打卡.")

        searchScreenWords {
            it?.let { wordBean ->
                var haveCard = false
                var isGoWork = false //上班打卡,成功后 对话框会翻转..延迟到翻转后
                wordBean.getRectByWord("下班打卡").let {
                    L.i("下班打卡:$it")
                    LogFile.touch("下班打卡:$it")
                    accCardStringBuilder.append("下班打卡:$it\n")

                    if (!it.isEmpty) {
                        haveCard = true

                        accService.touch(it.toPath())
                    } else {
                        wordBean.getRectByWord("上班打卡").let {
                            L.i("上班打卡:$it")
                            LogFile.touch("上班打卡:$it")
                            accCardStringBuilder.append("上班打卡:$it\n")

                            if (!it.isEmpty) {
                                haveCard = true
                                isGoWork = true

                                accService.touch(it.toPath())
                            } else {
                                wordBean.getRectByWord("更新打卡").let {
                                    L.i("更新打卡:$it")

                                    LogFile.touch("更新打卡:$it")

                                    accCardStringBuilder.append("更新打卡:$it\n")

                                    if (!it.isEmpty) {
                                        haveCard = true

                                        accService.touch(it.toPath())
                                    } else {
                                        wordBean.getRectByWord("外勤打卡").let {
                                            L.i("外勤打卡:$it")
                                            LogFile.touch("外勤打卡:$it")

                                            accCardStringBuilder.append("外勤打卡:$it\n")

                                            if (!it.isEmpty) {
                                                Tip.show("请前往公司再打卡")

                                                shareQQ(accService)

                                                isCheckCardUI = false
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
                    Tip.show("确定打卡是否成功.")

                    //打卡结果页识别
                    if (isGoWork) {
                        delay(3_000) {
                            checkCardReult(accService)
                        }
                    } else {
                        checkCardReult(accService)
                    }
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
                        LogFile.touch("上班打卡成功:$this")

                        if (!this.isEmpty) {
                            Tip.show("上班打卡成功.")

                            //上班打卡成功, 后dialog会自动翻转
                            delay(HTTP_DELAY) {
                                searchScreenWords {
                                    wordBean.getRectByWord("我知道了").let {

                                        LogFile.touch("我知道了:$it")

                                        if (!it.isEmpty) {
                                            accService.touch(it.toPath())

                                            capture {
                                                Tip.show("分享至QQ")

                                                isBack = true
                                                it.share(accService, true)

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
                            isCheckCardUI = false
                            return@searchScreenWords
                        }
                    }

                    wordBean.getRectByWord("下班打卡成功").apply {
                        L.i("下班打卡成功:$this")
                        LogFile.touch("下班打卡成功:$this")

                        if (!this.isEmpty) {
                            Tip.show("下班打卡成功.")

                            wordBean.getRectByWord("我知道了").let {

                                LogFile.touch("我知道了:$it")

                                if (!it.isEmpty) {
                                    accService.touch(it.toPath())

                                    //结束任务, 等待下一轮
                                    handEvent = false

                                    shareQQ(accService)
                                }
                            }
                            isCheckCardUI = false
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
                            isCheckCardUI = false
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
                            isCheckCardUI = false
                            return@searchScreenWords
                        }
                    }

                    //对话框
                    wordBean.getRectByWord("我知道了").let {
                        L.i("我知道了:$it")

                        LogFile.touch("对话框 我知道了:$it")

                        if (!it.isEmpty) {
                            accService.touch(it.toPath())

                            shareQQ(accService)

                            isCheckCardUI = false
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

                            isCheckCardUI = false
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

                            isCheckCardUI = false
                            return@searchScreenWords
                        }
                    }

                    //上班打卡 分享对话框 取消分享
                    wordBean.getRectByWord("取消分享").let {
                        LogFile.touch("对话框 取消分享:$it")

                        if (!it.isEmpty) {
                            accService.touch(it.toPath())

                            shareQQ(accService)

                            isCheckCardUI = false
                            return@searchScreenWords
                        }
                    }

                    if (retry <= 0) {
                        if (isInCardUI) {
                            Tip.show("弹出了什么鬼?")
                        } else {
                            Tip.show("OCR未识别.")
                        }
                        isCheckCardUI = false
                    } else {
                        Tip.show("重试$retry 结果识别")

                        checkCardReult(accService, isInCardUI, retry - 1)
                    }
                }
                if (it == null) {
                    isCheckCardUI = false
                }
            }
        }
    }

    fun shareQQ(accService: BaseAccessibilityService) {

        DingDingService.onStartShare(accService)

        capture {
            Tip.show("分享至QQ")

            LogFile.share("${this.javaClass.simpleName} 分享至QQ")

            isBack = true
            it.share(accService, true, !DingDingService.isSpecialModel())

            delay(SHARE_DELAY) {
                accService.home()

                delay(1_000) {
                    isBack = true

                    Tip.show("开始回退界面,请等待")

                    LogFile.share("${this.javaClass.simpleName} 开始回退界面")

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
//            findNodeByText("消息", accService, event).let {
//                L.i("消息:${it.size}")
//            }
//            findNodeByText("DING", accService, event).let {
//                L.i("DING:${it.size}")
//            }
//            findNodeByText("工作", accService, event).let {
//                L.i("工作:${it.size}")
//            }
//            findNodeByText("通讯录", accService, event).let {
//                L.i("通讯录:${it.size}")
//            }
//            findNodeByText("我的", accService, event).let {
//                L.i("我的:${it.size}")
//            }
            findNodeById("bottom_tab", accService, event).let {
                L.i("bottom_tab:${it.size}")
                result = it.isNotEmpty()
            }
        }
        return result
    }

    fun back(accService: BaseAccessibilityService) {
        handEvent = false
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

                            LogFile.log("恭喜,流程结束!.")

                            isCheckCardUI = false
                            DingDingService.isStartTimeDo = false
                            DingDingService.isEndTimeDo = false
                            accService.runMain()

                            DingDingService.share(accService, "$accCardStringBuilder\n打卡结束, 请登录钉钉查看准确结果.")

                            delay(2_000) {
                                Tip.show("恭喜,流程结束!.")
                            }
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