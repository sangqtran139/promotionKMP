package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository

internal class FetchFeatureFlagsUseCase(
    private val repository: FeatureFlagRepository,
) {
    suspend operator fun invoke() = repository.fetchFlags()
}
