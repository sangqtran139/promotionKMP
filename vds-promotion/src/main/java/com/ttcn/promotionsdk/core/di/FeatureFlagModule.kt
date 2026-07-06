package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single
import com.ttcn.promotionsdk.core.domain.usecase.FetchFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetPromotionFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.IsFeatureEnabledUseCase

object FeatureFlagModule {

    internal val module = module {
        single { FetchFeatureFlagsUseCase(repository = get()) }
        single { IsFeatureEnabledUseCase(repository = get()) }
        single { GetFeatureFlagsUseCase(repository = get()) }
        single { GetPromotionFeatureFlagsUseCase(repository = get()) }
    }
}
