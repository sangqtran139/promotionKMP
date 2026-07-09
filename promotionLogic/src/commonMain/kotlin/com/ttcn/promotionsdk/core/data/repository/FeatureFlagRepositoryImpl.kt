package com.ttcn.promotionsdk.core.data.repository

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.dto.featureflag.toPromotionFeatureFlags
import com.ttcn.promotionsdk.core.data.local.FeatureFlagLocalDataSource
import com.ttcn.promotionsdk.core.data.remote.FeatureFlagRemoteDataSource
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository
import kotlin.concurrent.Volatile

internal class FeatureFlagRepositoryImpl(
    private val remoteDataSource: FeatureFlagRemoteDataSource,
    private val localDataSource: FeatureFlagLocalDataSource,
    private val contextProvider: PromotionRequestContextProvider,
) : FeatureFlagRepository {

    @Volatile
    private var cachedFlags: PromotionFeatureFlags = localDataSource.load()

    /** Lỗi gọi API bị nuốt có chủ đích: giữ nguyên cờ đang cache thay vì khoá tính năng. */
    override suspend fun fetchFlags() {
        runCatching {
            remoteDataSource.getFeatureFlags(
                sessionId = "",
                userId = contextProvider.getCustomerId().orEmpty(),
            )
        }
            .getOrNull()
            ?.let { response ->
                val flags = response.toPromotionFeatureFlags()
                localDataSource.save(flags)
                cachedFlags = flags
            }
    }

    override fun isEnabled(featureName: String): Boolean = cachedFlags.isEnabled(featureName)

    override fun getPromotionFeatureFlags(): PromotionFeatureFlags = cachedFlags
}
