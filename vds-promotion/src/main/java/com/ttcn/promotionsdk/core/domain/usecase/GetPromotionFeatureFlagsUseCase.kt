package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository

internal class GetPromotionFeatureFlagsUseCase(
    private val repository: FeatureFlagRepository,
) {
    operator fun invoke(): PromotionFeatureFlags = repository.getPromotionFeatureFlags()
}
