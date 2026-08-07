package com.ttcn.promotionsdk.domain.repository

import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlags

internal interface FeatureFlagRepository {
    suspend fun fetchFlags()
    fun isEnabled(featureName: String): Boolean
    fun getPromotionFeatureFlags(): PromotionFeatureFlags
}
