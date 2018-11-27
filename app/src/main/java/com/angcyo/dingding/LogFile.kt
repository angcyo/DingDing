package com.angcyo.dingding

import com.angcyo.uiview.less.utils.RUtils

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/20
 */
object LogFile {
    fun log(data: String) {
        RUtils.saveToSDCard("run_log.log", data)
    }

    fun timeTick(data: String = "tick") {
        RUtils.saveToSDCard("time_tick.log", data)
    }

    fun threadTick(data: String = "tick") {
        RUtils.saveToSDCard("thread_tick.log", data)
    }

    fun ocr(data: String) {
        RUtils.saveToSDCard("ocr.log", data)
    }

    fun acc(data: String) {
        RUtils.saveToSDCard("acc.log", data)
    }

    fun http(data: String) {
        RUtils.saveToSDCard("http.log", data)
    }

    fun share(data: String) {
        RUtils.saveToSDCard("share.log", data)
    }

    fun touch(data: String) {
        RUtils.saveToSDCard("touch.log", data)
    }
}