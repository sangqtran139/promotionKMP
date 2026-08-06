package com.ttcn.promotionsdk.core.data.dto.featureflag

import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeatureFlagItemResponse(
    @SerialName("flagName") val flagName: String,
    @SerialName("enabled") val enabled: Boolean,
)

/**
 * Body gọi feature flag. **Không còn `userId` / `sessionId`** — SDK không nhận định danh khách từ
 * host nữa; server tự lấy từ JWT `sub` như mọi API promotion khác.
 *
 * `encodeDefaults = true` ở [PromotionHttpClient] nên body luôn là `{"properties":{}}`, không phải `{}`.
 */
@Serializable
internal data class FeatureFlagRequest(
    @SerialName("properties") val properties: Map<String, String> = emptyMap(),
)

internal fun List<FeatureFlagItemResponse>.toPromotionFeatureFlags(): PromotionFeatureFlags {
    val map = associate { it.flagName to it.enabled }
    return PromotionFeatureFlags(
        enableAll = map[PromotionFeatureFlag.ENABLE_ALL] ?: false,
        voucherApply = map[PromotionFeatureFlag.VOUCHER_APPLY] ?: false,
        voucherRedeem = map[PromotionFeatureFlag.VOUCHER_REDEEM] ?: false,
        voucherSelection = map[PromotionFeatureFlag.VOUCHER_SELECTION] ?: false,
        voucherDetail = map[PromotionFeatureFlag.VOUCHER_DETAIL] ?: false,
        voucherList = map[PromotionFeatureFlag.VOUCHER_LIST] ?: false,
    )
}
