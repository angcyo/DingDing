package com.angcyo.dingding

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.angcyo.uiview.less.RApplication
import com.yhao.floatwindow.FloatWindow
import com.yhao.floatwindow.Screen

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/14
 */
@SuppressLint("StaticFieldLeak")
object Tip {
    var showTip = true
        set(value) {
            field = value
            if (!value) {
                FloatWindow.destroy()
                view = null
            }
        }

    private var view: View? = null
    fun show(tip: String) {
        if (!showTip) {
            return
        }
        if (view == null) {
            view = FloatWindow.with(RApplication.getApp())
                .setX(Screen.width, 0.8f)
                .setY(Screen.height, 0.8f)
                .setView(R.layout.layout_tip)
                .build(true)
        }
        view?.let {
            it.findViewById<TextView>(R.id.text_view).text = tip
        }
    }
}