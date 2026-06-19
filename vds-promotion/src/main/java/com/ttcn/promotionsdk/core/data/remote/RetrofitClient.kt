package com.ttcn.promotionsdk.core.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    fun promotionApiService(baseUrl: String, apiInterceptor: ApiInterceptor, isDebug: Boolean = false): PromotionApiService {
        return instance(baseUrl, apiInterceptor, isDebug).create(PromotionApiService::class.java)
    }

    fun clear() {
        retrofit = null
    }

    private fun instance(baseUrl: String, apiInterceptor: ApiInterceptor, isDebug: Boolean): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(baseUrl, apiInterceptor, isDebug).also { retrofit = it }
        }
    }

    private fun buildRetrofit(baseUrl: String, apiInterceptor: ApiInterceptor, isDebug: Boolean): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(buildOkHttpClient(apiInterceptor, isDebug))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun buildOkHttpClient(apiInterceptor: ApiInterceptor, isDebug: Boolean): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(apiInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }
}
