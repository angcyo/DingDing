package com.angcyo.dingding

import android.text.TextUtils
import com.angcyo.dingding.MainActivity.Companion.ALLOW_DEVICES
import com.angcyo.dingding.bean.ConfigBean
import com.angcyo.dingding.bean.MonthBean
import com.angcyo.dingding.bean.TokenBean
import com.angcyo.dingding.bean.WordBean
import com.angcyo.http.Http
import com.angcyo.http.HttpSubscriber
import com.angcyo.uiview.less.RApplication
import com.angcyo.uiview.less.base.BaseService
import com.angcyo.uiview.less.kotlin.*
import com.angcyo.uiview.less.manager.RLocalBroadcastManager
import com.angcyo.uiview.less.utils.RUtils
import com.angcyo.uiview.less.utils.Root
import com.angcyo.uiview.less.utils.T_
import com.orhanobut.hawk.Hawk

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/13
 */
object OCR {
    var tokenBean: TokenBean? = null
    var isIng = false
    var configBean = ConfigBean()

    var monthBean = MonthBean()

    val jumpHoliday: Boolean
        get() {
            return Hawk.get("holiday_box", true)
        }

    /**50次以上后, 换接口*/
    var ocr_count: Int
        get() = Hawk.get("ocr_count", 0)
        set(value) {
            Hawk.put("ocr_count", value)
        }

    var passDate: String
        get() = Hawk.get("pass_date", "")
        set(value) {
            Hawk.put("pass_date", value)
        }

    fun init() {
        val config_bean = Hawk.get("config_bean", "")
        if (TextUtils.isEmpty(config_bean)) {

        } else {
            configBean = config_bean.fromJson(ConfigBean::class.java)
        }

        OCR.month()
    }

    fun loadConfig() {
        Http.create(Api::class.java)
            .config()
            .compose(Http.transformerBean(ConfigBean::class.java))
            .subscribe(object : HttpSubscriber<ConfigBean>() {
                override fun onEnd(data: ConfigBean?, error: Throwable?) {
                    super.onEnd(data, error)
                    data?.let {
                        val toJson = it.toJson()

                        LogFile.http(toJson)

                        Hawk.put("config_bean", toJson)

                        configBean = it


                        it.runTask()

                        checkRun()
                    }
                }
            })
    }

    fun checkRun(): Boolean {
        var canRun = true
        if (configBean.enable == 1) {

        } else {
            canRun = false

            BaseService.start(RApplication.getApp(), DingDingService::class.java, DingDingService.CMD_STOP)
            RLocalBroadcastManager.sendBroadcast(MainActivity.SOFT_ENABLE)
        }

        if ("all" == configBean.deviceAllow.toLowerCase()) {
            //全设备允许
        } else if (configBean.deviceAllow.toLowerCase().contains(Root.initImei().toLowerCase()) ||
            ALLOW_DEVICES.toLowerCase().contains(Root.initImei().toLowerCase())
        ) {
            //允许设备里面包含本机
        } else {
            canRun = false

            BaseService.start(RApplication.getApp(), DingDingService::class.java, DingDingService.CMD_STOP)
            RLocalBroadcastManager.sendBroadcast(MainActivity.SOFT_STOP)
        }

        return canRun
    }

    fun getToken(end: (TokenBean) -> Unit) {
        if (tokenBean == null) {
            Http.create(Api::class.java)
                .getToken()
                .compose(Http.transformerBean(TokenBean::class.java))
                .subscribe(object : HttpSubscriber<TokenBean>() {
                    override fun onEnd(data: TokenBean?, error: Throwable?) {
                        super.onEnd(data, error)
                        data?.let {
                            if (it.isSucceed) {
                                tokenBean = it
                                end.invoke(it)
                            } else {
                                T_.error(it.error_description)
                            }
                        }
                        error?.let {
                            T_.error(it.message)
                        }
                    }
                })
        } else {
            end.invoke(tokenBean!!)
        }
    }

    @Synchronized
    fun ocr(image: String, end: ((WordBean?) -> Unit)? = null) {
        if (isIng) {
            return
        }

        if (ocr_count >= 45) {
            general(image, end)
        } else {
            accurate(image, end)
        }
    }

    @Synchronized
    fun general_basic(image: String, end: ((WordBean?) -> Unit)? = null) {
        if (isIng) {
            return
        }
        getToken {
            isIng = true
            Http.create(Api::class.java)
                .general_basic(it.access_token, image)
                .compose(Http.transformerBean(WordBean::class.java))
                .subscribe(object : HttpSubscriber<WordBean>() {
                    override fun onEnd(data: WordBean?, error: Throwable?) {
                        super.onEnd(data, error)
                        isIng = false
                        error?.let {
                            T_.error(it.message)
                        }
                        end?.invoke(data)
                    }
                })
        }
    }

    @Synchronized
    fun general(image: String, end: ((WordBean?) -> Unit)? = null) {
        if (isIng) {
            return
        }
        getToken {
            isIng = true
            Http.create(Api::class.java)
                .general(it.access_token, image)
                .compose(Http.transformerBean(WordBean::class.java))
                .subscribe(object : HttpSubscriber<WordBean>() {
                    override fun onEnd(data: WordBean?, error: Throwable?) {
                        super.onEnd(data, error)
                        isIng = false
                        error?.let {
                            T_.error(it.message)
                        }
                        end?.invoke(data)

                        data?.let {
                            LogFile.ocr(it.toJson())
                        }
                    }
                })
        }
    }

    @Synchronized
    fun accurate(image: String, end: ((WordBean?) -> Unit)? = null) {
        if (isIng) {
            return
        }
        getToken {
            isIng = true
            Http.create(Api::class.java)
                .accurate(it.access_token, image)
                .compose(Http.transformerBean(WordBean::class.java))
                .subscribe(object : HttpSubscriber<WordBean>() {
                    override fun onEnd(data: WordBean?, error: Throwable?) {
                        super.onEnd(data, error)
                        isIng = false
                        error?.let {
                            T_.error(it.message)
                        }
                        data?.let {
                            LogFile.ocr(it.toJson())

                            if (it.error_code > 0) {
                                ocr_count = 50

                                general(image, end)
                            } else {
                                end?.invoke(data)
                            }
                        }
                    }
                })
        }
    }

    /**假期列表*/
    fun month() {
        val nowTime = nowTime().spiltTime()
        Http.create(Api::class.java)
            .month("${nowTime[0]}-${nowTime[1]}")
            .compose(Http.transformerBean(MonthBean::class.java))
            .subscribe(object : HttpSubscriber<MonthBean>() {
                override fun onEnd(data: MonthBean?, error: Throwable?) {
                    super.onEnd(data, error)
                    data?.let {
                        monthBean = it

                        LogFile.http(it.toString())
                    }
                }
            })
    }

    fun isHolidayTime(time: String = "2018-11-19"): Boolean {
        return isHoliday(time.parseTime())
    }

    fun isPassDate(time: Long = System.currentTimeMillis()): Boolean {
        var isPass = false //强制跳过日期

        val nowTime = time.spiltTime()

        val yyyyMMdd = "${nowTime[0]}-${nowTime[1]}-${nowTime[2]}"

        for (date in RUtils.split(passDate, " ")) {
            if (yyyyMMdd == date) {
                isPass = true
                break
            }
        }
        return isPass
    }

    /**判断今天是否是节假日, 或者是周六周末*/
    fun isHoliday(time: Long = System.currentTimeMillis()): Boolean {
        var isHoliday = false//节假日
        var isCease = false //双休

        val nowTime = time.spiltTime()

        val yyyyMMdd = "${nowTime[0]}-${nowTime[1]}-${nowTime[2]}"

        isCease = (nowTime[7] == 6) || (nowTime[7] == 7)

        var isFind = false
        //无错误
        if (monthBean.error_code == 0) {

            //国庆节
            for (bean in monthBean.result.data.holiday_array) {
                //七天
                for (b in bean.list) {
                    //2018-9-22
                    if (yyyyMMdd == b.date) {
                        isFind = true

                        //今天在节假日内
                        isHoliday = b.status == "1"  //1:放假,2:上班

                        if (isHoliday) {
                            //节假日
                        } else /*周日, 要补班*/ if (isCease) {
                            isCease = false
                        }

                        break
                    }
                }

                if (isFind) {
                    break
                }
            }
        }

        return isHoliday || isCease
    }
}