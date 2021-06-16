package com.study.mwlee.realestate.network

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory


object RetrofitClient {

    val service: RetrofitService

    init {
        // for debugging
        val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.NONE
        }
        val client: OkHttpClient = OkHttpClient.Builder().apply {
            this.addInterceptor(interceptor)
        }.build()

        service = Retrofit.Builder()
            .baseUrl("http://openapi.molit.go.kr:8081/OpenAPI_ToolInstallPackage/service/rest/RTMSOBJSvc/")
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(
                TikXmlConverterFactory.create(
                    TikXml.Builder().exceptionOnUnreadXml(false).build()
                )
            )
            .client(client)
            .build()
            .create(RetrofitService::class.java)
    }
}