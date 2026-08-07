package com.ttcn.promotionsdk.di

import com.ttcn.promotionsdk.config.EmptyPromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single
import io.ktor.client.HttpClient

object NetworkModule {
    internal val module = module {
        single<PromotionRequestContextProvider> {
            get<PromotionSDKConfig>().requestContextProvider
                ?: EmptyPromotionRequestContextProvider()
        }

        single<HttpClient> {
            val config = get<PromotionSDKConfig>()
            PromotionHttpClient.create(
                baseUrl = config.baseUrl,
                requestContextProvider = get(),
                isDebug = config.isDebug,
            )
        }

        single<PromotionApiService> {
            KtorPromotionApiService(client = get())
        }

        single<PromotionRemoteDataSource> {
            PromotionRemoteDataSource(
                apiService = get(),
            )
        }
    }
}
