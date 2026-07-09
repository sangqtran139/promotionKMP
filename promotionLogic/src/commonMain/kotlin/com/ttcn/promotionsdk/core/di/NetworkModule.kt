package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.config.EmptyPromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single
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
