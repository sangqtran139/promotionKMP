package com.ttcn.promotionsdk.core.data.repository

import com.ttcn.promotionsdk.core.data.dto.featureflag.toPromotionFeatureFlags
import com.ttcn.promotionsdk.core.data.local.FeatureFlagLocalDataSource
import com.ttcn.promotionsdk.core.data.remote.FeatureFlagRemoteDataSource
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository
import kotlin.concurrent.Volatile

internal class FeatureFlagRepositoryImpl(
    private val remoteDataSource: FeatureFlagRemoteDataSource,
    private val localDataSource: FeatureFlagLocalDataSource,
) : FeatureFlagRepository {

    @Volatile
    private var cachedFlags: PromotionFeatureFlags = localDataSource.load()

    /**
     * Lỗi gọi API bị nuốt có chủ đích: giữ nguyên cờ đang cache thay vì khoá tính năng.
     *
     * Request **không mang định danh khách** (`userId`/`sessionId` đã bỏ khỏi `FeatureFlagRequest`
     * cùng lúc với `customerId` ở bề mặt public) — server lấy từ JWT `sub`. Hệ quả: Unleash không
     * rollout theo % user được, chỉ bật/tắt toàn bộ. Khi vertical FeatureFlag chốt nguồn định danh
     * thì nối lại ở đây.
     */
    override suspend fun fetchFlags() {
        runCatching {
            remoteDataSource.getFeatureFlags()
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
