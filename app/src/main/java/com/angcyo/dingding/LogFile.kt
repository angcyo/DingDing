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
}