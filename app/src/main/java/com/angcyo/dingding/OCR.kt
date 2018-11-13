package com.angcyo.dingding

import com.angcyo.dingding.bean.TokenBean
import com.angcyo.http.Http
import com.angcyo.http.HttpSubscriber
import com.angcyo.uiview.less.utils.T_

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/13
 */
object OCR {
    var tokenBean: TokenBean? = null
    var isIng = false

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
    fun general_basic(image: String) {
//        if (isIng) {
//            return
//        }
        getToken {
            isIng = true
            Http.create(Api::class.java)
                .general_basic(it.access_token, image)
                .compose(Http.transformerBean(String::class.java))
                .subscribe(object : HttpSubscriber<String>() {
                    override fun onEnd(data: String?, error: Throwable?) {
                        super.onEnd(data, error)
                        isIng = false
                        error?.let {
                            T_.error(it.message)
                        }
                    }
                })
        }
    }

    @Synchronized
    fun general(image: String) {
//        if (isIng) {
//            return
//        }
        getToken {
            isIng = true
            Http.create(Api::class.java)
                .general(it.access_token, image)
                .compose(Http.transformerBean(String::class.java))
                .subscribe(object : HttpSubscriber<String>() {
                    override fun onEnd(data: String?, error: Throwable?) {
                        super.onEnd(data, error)
                        isIng = false
                        error?.let {
                            T_.error(it.message)
                        }
                    }
                })
        }
    }

    @Synchronized
    fun accurate(image: String) {
//        if (isIng) {
//            return
//        }
        getToken {
            isIng = true
            Http.create(Api::class.java)
                .accurate(it.access_token, image)
                .compose(Http.transformerBean(String::class.java))
                .subscribe(object : HttpSubscriber<String>() {
                    override fun onEnd(data: String?, error: Throwable?) {
                        super.onEnd(data, error)
                        isIng = false
                        error?.let {
                            T_.error(it.message)
                        }
                    }
                })
        }
    }
}