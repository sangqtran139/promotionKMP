// vds-promotion/src/main/java/com/ttcn/promotionsdk/core/di/PromotionContainer.kt
package com.ttcn.promotionsdk.core.di

import android.content.Context
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.data.remote.PromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.remote.RetrofitClient
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

object PromotionContainer {

    @Volatile
    private var applicationContext: Context? = null

    @Volatile
    private var config: PromotionSDKConfig? = null

    fun init(context: Context, config: PromotionSDKConfig) {
        applicationContext = context.applicationContext
        this.config = config
        SdkDi.getInstance().start(
            context = context.applicationContext,
            config = config,
            NetworkModule.module,
            RepositoryModule.module,
        )
    }

    fun clear() {
        RetrofitClient.clear()
        SdkDi.getInstance().clear()
        applicationContext = null
        config = null
    }

    fun isInitialized(): Boolean = applicationContext != null

    fun requireConfig(): PromotionSDKConfig = requireNotNull(config) {
        "PromotionSDKConfig is unavailable. Call PromotionSDK.init() first."
    }

    val promotionApiService: PromotionApiService
        get() = get()

    val promotionRemoteDataSource: PromotionRemoteDataSource
        get() = get()

    val promotionRepository: PromotionRepository
        get() = get()

    val requestContextProvider: PromotionRequestContextProvider
        get() = get()
}
