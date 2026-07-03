package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.model.featureflag.FeatureFlag
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository

internal class GetFeatureFlagsUseCase(
    private val repository: FeatureFlagRepository,
) {
    operator fun invoke(featureNames: List<String>): List<FeatureFlag> =
        featureNames.map { FeatureFlag(name = it, enabled = repository.isEnabled(it)) }
}
