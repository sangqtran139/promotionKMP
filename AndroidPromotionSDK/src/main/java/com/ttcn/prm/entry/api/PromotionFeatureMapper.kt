package com.ttcn.prm.entry.api

import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlags as CoreFeatureFlags

/**
 * Ranh giới chuyển đổi cho feature flag: lõi Kotlin ↔ DTO public. Cùng vai trò với phần map ở cuối
 * `PromotionSDKApi`, tách file vì bên feature flag chỉ có ánh xạ, không có lời gọi mạng.
 *
 * `internal` — host không thấy, và cũng không được thấy: cả hai đầu vào của nó
 * ([PromotionFeatureFlag], [CoreFeatureFlags]) đều nằm ngoài compile classpath của host.
 * Đối ứng `PromotionSDKImpl.flagName(for:)` / `toSnapshot()` bên iOS.
 */

/** Tên cờ thật của lõi Kotlin cho một [PromotionFeature]. */
internal fun PromotionFeature.flagName(): String = when (this) {
    PromotionFeature.ALL -> PromotionFeatureFlag.ENABLE_ALL
    PromotionFeature.VOUCHER_LIST -> PromotionFeatureFlag.VOUCHER_LIST
    PromotionFeature.VOUCHER_DETAIL -> PromotionFeatureFlag.VOUCHER_DETAIL
    PromotionFeature.VOUCHER_SELECTION -> PromotionFeatureFlag.VOUCHER_SELECTION
    PromotionFeature.VOUCHER_APPLY -> PromotionFeatureFlag.VOUCHER_APPLY
    PromotionFeature.VOUCHER_REDEEM -> PromotionFeatureFlag.VOUCHER_REDEEM
}

/**
 * Đọc qua [CoreFeatureFlags.isEnabled] chứ **không** đọc thẳng field: công tắc tổng `ENABLE_ALL`
 * chỉ được áp bên trong hàm đó (`if (!enableAll) return false`), field thô thì không. Đọc thẳng
 * field sẽ trả `voucherList = true` ngay cả khi công tắc tổng đang tắt — host ẩn nhầm/hiện nhầm.
 * Một nguồn sự thật cho luật "tắt tổng là tắt hết".
 */
internal fun CoreFeatureFlags.toSnapshot() = PromotionFeatureFlagsSnapshot(
    all = isEnabled(PromotionFeatureFlag.ENABLE_ALL),
    voucherList = isEnabled(PromotionFeatureFlag.VOUCHER_LIST),
    voucherDetail = isEnabled(PromotionFeatureFlag.VOUCHER_DETAIL),
    voucherSelection = isEnabled(PromotionFeatureFlag.VOUCHER_SELECTION),
    voucherApply = isEnabled(PromotionFeatureFlag.VOUCHER_APPLY),
    voucherRedeem = isEnabled(PromotionFeatureFlag.VOUCHER_REDEEM),
)
