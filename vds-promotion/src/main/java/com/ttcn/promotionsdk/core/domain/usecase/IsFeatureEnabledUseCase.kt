package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository

internal class IsFeatureEnabledUseCase(
    private val repository: FeatureFlagRepository,
) {
    operator fun invoke(featureName: String): Boolean = repository.isEnabled(featureName)
}
