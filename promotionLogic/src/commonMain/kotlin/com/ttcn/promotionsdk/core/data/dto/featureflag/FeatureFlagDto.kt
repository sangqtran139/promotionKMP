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

@Serializable
internal data class FeatureFlagRequest(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("userId") val userId: String,
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
