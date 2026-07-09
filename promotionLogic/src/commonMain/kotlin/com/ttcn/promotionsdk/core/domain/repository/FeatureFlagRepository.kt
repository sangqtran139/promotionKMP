package com.ttcn.promotionsdk.core.domain.repository

import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags

internal interface FeatureFlagRepository {
    suspend fun fetchFlags()
    fun isEnabled(featureName: String): Boolean
    fun getPromotionFeatureFlags(): PromotionFeatureFlags
}
