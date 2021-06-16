package com.study.mwlee.realestate.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitService {

    @GET("getRTMSDataSvcAptRent")
    fun getAptRent(
        @Query("serviceKey") serviceKey: String,
        @Query("LAWD_CD") lawdCd: String,
        @Query("DEAL_YMD") dealYmd: String
    ): Call<AptTradeResponse>

    @GET("getRTMSDataSvcAptTrade")
    fun getAptTrade(
        @Query("serviceKey") serviceKey: String,
        @Query("LAWD_CD") lawdCd: String,
        @Query("DEAL_YMD") dealYmd: String
    ): Call<AptTradeResponse>

}