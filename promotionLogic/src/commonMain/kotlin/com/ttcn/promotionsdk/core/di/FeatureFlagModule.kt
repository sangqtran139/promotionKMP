package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.data.remote.FeatureFlagApiService
import com.ttcn.promotionsdk.core.data.remote.FeatureFlagRemoteDataSource
import com.ttcn.promotionsdk.core.data.remote.KtorFeatureFlagApiService
import com.ttcn.promotionsdk.core.data.repository.FeatureFlagRepositoryImpl
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository
import com.ttcn.promotionsdk.core.domain.usecase.FetchFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetPromotionFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.IsFeatureEnabledUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureFlagUseCases

object FeatureFlagModule {
    internal val module = module {
        single<FeatureFlagApiService> { KtorFeatureFlagApiService(client = get()) }
        single { FeatureFlagRemoteDataSource(apiService = get()) }

        single<FeatureFlagRepository> {
            FeatureFlagRepositoryImpl(
                remoteDataSource = get(),
                localDataSource = get(),
            )
        }

        single { FetchFeatureFlagsUseCase(repository = get()) }
        single { IsFeatureEnabledUseCase(repository = get()) }
        single { GetFeatureFlagsUseCase(repository = get()) }
        single { GetPromotionFeatureFlagsUseCase(repository = get()) }

        single {
            PromotionFeatureFlagUseCases(
                fetchFeatureFlags = get(),
                isFeatureEnabled = get(),
                getFeatureFlags = get(),
                getPromotionFeatureFlags = get(),
            )
        }
    }
}
