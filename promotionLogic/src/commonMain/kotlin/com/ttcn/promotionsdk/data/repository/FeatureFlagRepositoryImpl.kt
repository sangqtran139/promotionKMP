package com.ttcn.promotionsdk.data.repository

import com.ttcn.promotionsdk.common.promotionWarn
import com.ttcn.promotionsdk.data.dto.featureflag.toPromotionFeatureFlags
import com.ttcn.promotionsdk.data.local.FeatureFlagLocalDataSource
import com.ttcn.promotionsdk.data.remote.FeatureFlagRemoteDataSource
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.domain.repository.FeatureFlagRepository
import kotlin.concurrent.Volatile

internal class FeatureFlagRepositoryImpl(
    private val remoteDataSource: FeatureFlagRemoteDataSource,
    private val localDataSource: FeatureFlagLocalDataSource,
) : FeatureFlagRepository {

    @Volatile
    private var cachedFlags: PromotionFeatureFlags = localDataSource.load()

    /**
     * Lỗi gọi API bị nuốt có chủ đích: giữ nguyên cờ đang cache thay vì khoá tính năng. Nuốt nhưng
     * **có log** ([promotionWarn]) — bản trước im hoàn toàn, mà đây lại là đường duy nhất nạp
     * kill-switch: parse hỏng hay mạng chết thì mọi cờ lặng lẽ về mặc định bật-hết, không ai có manh
     * mối nào để lần. Một dòng log là đủ để người tích hợp biết chỗ mà nhìn.
     *
     * Request **không mang định danh khách** (`userId`/`sessionId` đã bỏ khỏi `FeatureFlagRequest`
     * cùng lúc với `customerId` ở bề mặt public) — server lấy từ JWT `sub`. Hệ quả: Unleash không
     * rollout theo % user được, chỉ bật/tắt toàn bộ. Khi vertical FeatureFlag chốt nguồn định danh
     * thì nối lại ở đây.
     */
    override suspend fun fetchFlags() {
        runCatching { remoteDataSource.getFeatureFlags() }
            .onFailure {
                promotionWarn("Nạp feature flag thất bại (${it::class.simpleName}) — giữ cờ đang cache.")
            }
            .getOrNull()
            ?.let { response ->
                val flags = response.toPromotionFeatureFlags()
                localDataSource.save(flags)
                cachedFlags = flags
            }
    }

    override fun isEnabled(featureName: String): Boolean = cachedFlags.isEnabled(featureName)

    /**
     * Trả bản đã chuẩn hoá, **không** phải [cachedFlags] thô: instance này thoát ra tới host qua
     * `PromotionFeatureFlagUseCases.all()`, mà ngoài đó không ai bị ép gọi [PromotionFeatureFlags.isEnabled]
     * — đọc thẳng `.voucherList` là chuyện tự nhiên. Chuẩn hoá ở đây thì đọc kiểu nào cũng đúng luật.
     *
     * Cache vẫn giữ giá trị thô để bật lại `ENABLE_ALL` là các cờ con trở về đúng giá trị riêng.
     */
    override fun getPromotionFeatureFlags(): PromotionFeatureFlags = cachedFlags.normalized()
}
