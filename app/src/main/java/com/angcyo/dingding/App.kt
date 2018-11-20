package com.angcyo.dingding

import com.angcyo.uiview.less.RApplication

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/14
 */
class App : RApplication() {
    override fun onInit() {
        super.onInit()
        OCR.init()
    }
}