package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.domain.model.featureflag.FeatureFlag
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository

class FetchFeatureFlagsUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    constructor() : this(get<FeatureFlagRepository>())

    suspend operator fun invoke() = repository.fetchFlags()
}

class IsFeatureEnabledUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    constructor() : this(get<FeatureFlagRepository>())

    operator fun invoke(featureName: String): Boolean = repository.isEnabled(featureName)
}

class GetFeatureFlagsUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    constructor() : this(get<FeatureFlagRepository>())

    operator fun invoke(featureNames: List<String>): List<FeatureFlag> =
        featureNames.map { FeatureFlag(name = it, enabled = repository.isEnabled(it)) }
}

class GetPromotionFeatureFlagsUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    constructor() : this(get<FeatureFlagRepository>())

    operator fun invoke(): PromotionFeatureFlags = repository.getPromotionFeatureFlags()
}

/**
 * Facade gom bốn use case cờ tính năng, song song với [PromotionUseCases].
 * Dựng thẳng sau khi `PromotionContainer.initialize(...)`: `PromotionFeatureFlagUseCases()`.
 *
 * [refresh] không ném lỗi: gọi API thất bại thì giữ nguyên cờ đang cache.
 */
class PromotionFeatureFlagUseCases internal constructor(
    private val fetchFeatureFlags: FetchFeatureFlagsUseCase,
    private val isFeatureEnabled: IsFeatureEnabledUseCase,
    private val getFeatureFlags: GetFeatureFlagsUseCase,
    private val getPromotionFeatureFlags: GetPromotionFeatureFlagsUseCase,
) {
    constructor() : this(
        FetchFeatureFlagsUseCase(),
        IsFeatureEnabledUseCase(),
        GetFeatureFlagsUseCase(),
        GetPromotionFeatureFlagsUseCase(),
    )

    suspend fun refresh() = fetchFeatureFlags()

    fun isEnabled(featureName: String): Boolean = isFeatureEnabled(featureName)

    fun flagsOf(featureNames: List<String>): List<FeatureFlag> = getFeatureFlags(featureNames)

    fun all(): PromotionFeatureFlags = getPromotionFeatureFlags()
}
