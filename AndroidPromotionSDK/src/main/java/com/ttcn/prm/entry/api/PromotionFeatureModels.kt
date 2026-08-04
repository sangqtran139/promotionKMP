package com.ttcn.prm.entry.api

/**
 * DTO công khai của bề mặt **feature flag**. Đối ứng 1-1 với `PromotionFeatureModels.swift` bên iOS:
 * cùng tên type, cùng tên case/field, cùng thứ tự khai báo. Sửa một bên thì sửa cả hai.
 *
 * Vì sao phải có type riêng ở đây thay vì dùng thẳng của lõi? Cùng lý do với `PromotionApiModels.kt`:
 * host chỉ tích hợp `AndroidPromotionSDK`, không có `com.ttcn.promotionsdk.core.*` trên compile
 * classpath — `PromotionFeatureFlag` (hằng chuỗi) và `PromotionFeatureFlags` (data class) của lõi
 * xuất hiện trong chữ ký public thì host **không resolve được**. Ánh xạ lõi ↔ public nằm ở
 * `PromotionFeatureMapper.kt`.
 */

/**
 * Tính năng có thể bị bật/tắt từ xa (kill-switch, không phải A/B test).
 *
 * Dùng enum thay vì chuỗi để host không gõ sai tên cờ; tên cờ thật (`"PROMOTION.VOUCHER_LIST"`…)
 * là chi tiết nội bộ của lõi Kotlin, giữ ở `PromotionFeatureFlag`.
 */
enum class PromotionFeature {
    /** Công tắc tổng. TẮT → mọi tính năng dưới đây đều TẮT, bất kể giá trị riêng. */
    ALL,

    /** Màn "Ưu đãi của tôi" — [com.ttcn.prm.entry.PromotionSDK.openMyPromotion]. */
    VOUCHER_LIST,

    /** Màn "Chi tiết ưu đãi" — [com.ttcn.prm.entry.PromotionSDK.openPromotionDetail]. */
    VOUCHER_DETAIL,

    /** Widget chọn ưu đãi ở màn thanh toán + màn "Chọn ưu đãi". */
    VOUCHER_SELECTION,

    /** Áp voucher vào đơn hàng — [PromotionSDKApi.validateDiscounts]. */
    VOUCHER_APPLY,

    /** Tạo phiên thanh toán — [PromotionSDKApi.createRedemption]. */
    VOUCHER_REDEEM,
}

/**
 * Ảnh chụp **toàn bộ** cờ tại một thời điểm, đọc từ cache (không gọi mạng).
 *
 * Gọi là *snapshot* vì đúng như vậy: giá trị có thể đổi sau lần
 * [com.ttcn.prm.entry.PromotionSDK.refreshFeatureFlags] kế tiếp. Đừng cache lại nó lâu dài — hỏi
 * SDK mỗi khi cần dựng UI.
 *
 * Mọi field đã **áp sẵn công tắc tổng**: [all] TẮT thì các field còn lại đều `false`, host không
 * phải tự nhân hai điều kiện.
 */
data class PromotionFeatureFlagsSnapshot(
    val all: Boolean,
    val voucherList: Boolean,
    val voucherDetail: Boolean,
    val voucherSelection: Boolean,
    val voucherApply: Boolean,
    val voucherRedeem: Boolean,
) {
    /** Tra một [PromotionFeature] trên chính snapshot này (không hỏi lại SDK). */
    fun isEnabled(feature: PromotionFeature): Boolean = when (feature) {
        PromotionFeature.ALL -> all
        PromotionFeature.VOUCHER_LIST -> voucherList
        PromotionFeature.VOUCHER_DETAIL -> voucherDetail
        PromotionFeature.VOUCHER_SELECTION -> voucherSelection
        PromotionFeature.VOUCHER_APPLY -> voucherApply
        PromotionFeature.VOUCHER_REDEEM -> voucherRedeem
    }

    companion object {
        /**
         * Mặc định **fail-open**: chưa `initialize()` hoặc chưa có cache → coi như bật hết.
         * SDK không tự khoá tính năng chỉ vì chưa gọi được API lần nào.
         */
        @JvmField
        val AllEnabled = PromotionFeatureFlagsSnapshot(
            all = true,
            voucherList = true,
            voucherDetail = true,
            voucherSelection = true,
            voucherApply = true,
            voucherRedeem = true,
        )
    }
}
