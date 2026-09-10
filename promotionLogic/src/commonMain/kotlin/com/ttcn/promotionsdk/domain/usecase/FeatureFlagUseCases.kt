package com.ttcn.promotionsdk.domain.usecase

import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.domain.model.featureflag.FeatureFlag
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.domain.repository.FeatureFlagRepository

public class FetchFeatureFlagsUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    public constructor() : this(get<FeatureFlagRepository>())

    public suspend operator fun invoke(): Unit = repository.fetchFlags()
}

public class IsFeatureEnabledUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    public constructor() : this(get<FeatureFlagRepository>())

    public operator fun invoke(featureName: String): Boolean = repository.isEnabled(featureName)
}

public class GetFeatureFlagsUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    public constructor() : this(get<FeatureFlagRepository>())

    public operator fun invoke(featureNames: List<String>): List<FeatureFlag> =
        featureNames.map { FeatureFlag(name = it, enabled = repository.isEnabled(it)) }
}

public class GetPromotionFeatureFlagsUseCase internal constructor(
    private val repository: FeatureFlagRepository,
) {
    public constructor() : this(get<FeatureFlagRepository>())

    public operator fun invoke(): PromotionFeatureFlags = repository.getPromotionFeatureFlags()
}

/**
 * Facade gom bốn use case cờ tính năng, song song với [PromotionUseCases].
 * Dựng thẳng sau khi `PromotionContainer.initialize(...)`: `PromotionFeatureFlagUseCases()`.
 *
 * [refresh] không ném lỗi: gọi API thất bại thì giữ nguyên cờ đang cache.
 */
public class PromotionFeatureFlagUseCases internal constructor(
    private val fetchFeatureFlags: FetchFeatureFlagsUseCase,
    private val isFeatureEnabled: IsFeatureEnabledUseCase,
    private val getFeatureFlags: GetFeatureFlagsUseCase,
    private val getPromotionFeatureFlags: GetPromotionFeatureFlagsUseCase,
) {
    public constructor() : this(
        FetchFeatureFlagsUseCase(),
        IsFeatureEnabledUseCase(),
        GetFeatureFlagsUseCase(),
        GetPromotionFeatureFlagsUseCase(),
    )

    public suspend fun refresh(): Unit = fetchFeatureFlags()

    public fun isEnabled(featureName: String): Boolean = isFeatureEnabled(featureName)

    public fun flagsOf(featureNames: List<String>): List<FeatureFlag> = getFeatureFlags(featureNames)

    public fun all(): PromotionFeatureFlags = getPromotionFeatureFlags()
}
