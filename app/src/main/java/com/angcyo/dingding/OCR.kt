package com.angcyo.dingding

import com.angcyo.dingding.bean.ConfigBean
import com.angcyo.dingding.bean.TokenBean
import com.angcyo.dingding.bean.WordBean
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
    var configBean = ConfigBean()

    fun loadConfig() {
        Http.create(Api::class.java)
            .config()
            .compose(Http.transformerBean(ConfigBean::class.java))
            .subscribe(object : HttpSubscriber<ConfigBean>() {
                override fun onEnd(data: ConfigBean?, error: Throwable?) {
                    super.onEnd(data, error)
                    data?.let {
                        configBean = it
                    }
                }
            })
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
                        end?.invoke(data)
                    }
                })
        }
    }
}