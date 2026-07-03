package com.ttcn.promotionsdk.core.di

import android.content.Context
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.config.SdkEnvironment
import com.ttcn.promotionsdk.core.data.repository.FeatureFlagRepositoryImpl
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository
import com.ttcn.promotionsdk.core.domain.usecase.GetFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetPromotionFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.IsFeatureEnabledUseCase
import io.getunleash.android.DefaultUnleash
import io.getunleash.android.UnleashConfig

object FeatureFlagModule {

    private const val STAGING_PROXY_URL = "http://web-staging.digital.vn/ffm/api/frontend"
    private const val STAGING_CLIENT_KEY =
        "promotion:staging.61382fc42684d08088d6c768be552eb37955fd78ecdda1962c1e2f71"

    // TODO: fill PROD credentials when available
    private const val PROD_PROXY_URL = ""
    private const val PROD_CLIENT_KEY = ""

    internal val module = module {
        single<DefaultUnleash> {
            val context = get<Context>()
            val sdkConfig = get<PromotionSDKConfig>()
            val (proxyUrl, clientKey) = when (sdkConfig.environment) {
                SdkEnvironment.STAGING -> STAGING_PROXY_URL to STAGING_CLIENT_KEY
                SdkEnvironment.PROD -> PROD_PROXY_URL to PROD_CLIENT_KEY
            }
            DefaultUnleash(
                androidContext = context,
                unleashConfig = UnleashConfig.newBuilder(appName = "promotion")
                    .proxyUrl(proxyUrl)
                    .clientKey(clientKey)
                    .build(),
            ).also { it.start() }
        }

        single<FeatureFlagRepository> {
            FeatureFlagRepositoryImpl(unleash = get())
        }

        single { IsFeatureEnabledUseCase(repository = get()) }
        single { GetFeatureFlagsUseCase(repository = get()) }
        single { GetPromotionFeatureFlagsUseCase(repository = get()) }
    }
}
