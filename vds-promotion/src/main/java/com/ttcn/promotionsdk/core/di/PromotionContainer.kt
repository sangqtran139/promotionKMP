package com.ttcn.promotionsdk.core.di

import android.content.Context
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.data.remote.PromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.remote.RetrofitClient
import com.ttcn.promotionsdk.core.di.internal.SdkDi
import com.ttcn.promotionsdk.core.di.internal.get
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
            UseCaseModule.module,
            FeatureFlagModule.module,
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

    internal val promotionApiService: PromotionApiService
        get() = get()

    internal val promotionRemoteDataSource: PromotionRemoteDataSource
        get() = get()

    internal val promotionRepository: PromotionRepository
        get() = get()

    val requestContextProvider: PromotionRequestContextProvider
        get() = get()
}
