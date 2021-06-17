package com.study.mwlee.realestate.network

import com.study.mwlee.realestate.BuildConfig
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface RetrofitService {

    @GET("getRTMSDataSvcAptRent")
    fun getAptRent(
        @Query("serviceKey") serviceKey: String,
        @Query("LAWD_CD") lawdCd: String,
        @Query("DEAL_YMD") dealYmd: String
    ): Call<AptResponse>

    @GET("getRTMSDataSvcAptTrade")
    fun getAptTrade(
        @Query("serviceKey") serviceKey: String,
        @Query("LAWD_CD") lawdCd: String,
        @Query("DEAL_YMD") dealYmd: String
    ): Call<AptResponse>

    @Headers(
        "X-NCP-APIGW-API-KEY-ID:${BuildConfig.KEY_NAVER_CLIENT_ID}",
        "X-NCP-APIGW-API-KEY:${BuildConfig.KEY_NAVER_CLIENT_SECRET}"
    )
    @GET("geocode")
    fun getGeocoding(
        @Query("query") query: String
    ): Call<GeocodingResponse>

}