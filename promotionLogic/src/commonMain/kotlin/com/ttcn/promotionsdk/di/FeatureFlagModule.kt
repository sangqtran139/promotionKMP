package com.ttcn.promotionsdk.di

import com.ttcn.promotionsdk.data.remote.FeatureFlagApiService
import com.ttcn.promotionsdk.data.remote.FeatureFlagRemoteDataSource
import com.ttcn.promotionsdk.data.remote.KtorFeatureFlagApiService
import com.ttcn.promotionsdk.data.repository.FeatureFlagRepositoryImpl
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single
import com.ttcn.promotionsdk.domain.repository.FeatureFlagRepository
import com.ttcn.promotionsdk.domain.usecase.FetchFeatureFlagsUseCase
import com.ttcn.promotionsdk.domain.usecase.GetFeatureFlagsUseCase
import com.ttcn.promotionsdk.domain.usecase.GetPromotionFeatureFlagsUseCase
import com.ttcn.promotionsdk.domain.usecase.IsFeatureEnabledUseCase
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureFlagUseCases

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
