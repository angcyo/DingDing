package com.angcyo.dingding

import com.orhanobut.hawk.Hawk
import okhttp3.ResponseBody
import retrofit2.http.*
import rx.Observable

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2018/11/13
 */
interface Api {

    @POST("https://aip.baidubce.com/oauth/2.0/token")
    fun getToken(
        @Query("grant_type") grant_type: String = "client_credentials",
        @Query("client_id") client_id: String = Hawk.get("baidu_ak", "vGcIcmO6OWnPcBBv9TzZryiD"),
        @Query("client_secret") client_secret: String = Hawk.get("baidu_sk", "Aa8lePlFQ8cp1py9GZUrrdkZGEyY2Tln")
    ): Observable<ResponseBody>


    /**通用文字识别*/
    @POST("https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    @FormUrlEncoded
    fun general_basic(
        @Query("access_token") access_token: String,
        @Field("image") image: String /*Base64后的图片*/
    ): Observable<ResponseBody>


    /**通用文字识别（含位置信息版）*/
    @POST("https://aip.baidubce.com/rest/2.0/ocr/v1/general")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    @FormUrlEncoded
    fun general(
        @Query("access_token") access_token: String,
        @Field("image") image: String /*Base64后的图片*/
    ): Observable<ResponseBody>

    /**通用文字识别（含位置高精度版）*/
    @POST("https://aip.baidubce.com/rest/2.0/ocr/v1/accurate")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    @FormUrlEncoded
    fun accurate(
        @Query("access_token") access_token: String,
        @Field("image") image: String /*Base64后的图片*/
    ): Observable<ResponseBody>

    @GET("https://raw.githubusercontent.com/angcyo/RHttp/master/json/ding_ding_config.json")
    fun config(): Observable<ResponseBody>
}