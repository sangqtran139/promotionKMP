package com.ttcn.promotionsdk.core.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.ttcn.promotionsdk.core.config.EmptyPromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.data.remote.ApiInterceptor
import com.ttcn.promotionsdk.core.data.remote.PromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.remote.RetrofitClient

object NetworkModule {
    internal val module = module {
        single<PromotionRequestContextProvider> {
            get<PromotionSDKConfig>().requestContextProvider
                ?: EmptyPromotionRequestContextProvider()
        }

        single<ApiInterceptor> {
            ApiInterceptor(
                requestContextProvider = get(),
            )
        }

        single<PromotionApiService> {
            val isDebug = (get<Context>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            RetrofitClient.promotionApiService(
                baseUrl = get<PromotionSDKConfig>().baseUrl,
                apiInterceptor = get(),
                isDebug = isDebug,
            )
        }

        single<PromotionRemoteDataSource> {
            PromotionRemoteDataSource(
                apiService = get(),
            )
        }
    }
}
