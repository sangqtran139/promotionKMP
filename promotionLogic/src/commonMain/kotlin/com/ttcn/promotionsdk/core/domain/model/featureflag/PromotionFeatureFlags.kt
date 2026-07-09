package com.ttcn.promotionsdk.core.domain.model.featureflag

data class FeatureFlag(
    val name: String,
    val enabled: Boolean,
)

object PromotionFeatureFlag {
    const val ENABLE_ALL = "PROMOTION.ENABLE_ALL"
    const val VOUCHER_APPLY = "PROMOTION.VOUCHER_APPLY"
    const val VOUCHER_REDEEM = "PROMOTION.VOUCHER_REDEEM"
    const val VOUCHER_SELECTION = "PROMOTION.VOUCHER_SELECTION"
    const val VOUCHER_DETAIL = "PROMOTION.VOUCHER_DETAIL"
    const val VOUCHER_LIST = "PROMOTION.VOUCHER_LIST"
}

data class PromotionFeatureFlags(
    val enableAll: Boolean,
    val voucherApply: Boolean,
    val voucherRedeem: Boolean,
    val voucherSelection: Boolean,
    val voucherDetail: Boolean,
    val voucherList: Boolean,
) {
    /**
     * [PromotionFeatureFlag.ENABLE_ALL] là công tắc tổng: tắt nó thì mọi cờ con đều tắt.
     *
     * Hỏi thẳng `ENABLE_ALL` phải trả về chính [enableAll]. Trước đây nó không có nhánh riêng nên
     * rơi vào `else -> false` — tức là **luôn `false`**, kể cả khi công tắc tổng đang bật.
     */
    fun isEnabled(flag: String): Boolean {
        if (!enableAll) return false
        return when (flag) {
            PromotionFeatureFlag.ENABLE_ALL -> enableAll
            PromotionFeatureFlag.VOUCHER_APPLY -> voucherApply
            PromotionFeatureFlag.VOUCHER_REDEEM -> voucherRedeem
            PromotionFeatureFlag.VOUCHER_SELECTION -> voucherSelection
            PromotionFeatureFlag.VOUCHER_DETAIL -> voucherDetail
            PromotionFeatureFlag.VOUCHER_LIST -> voucherList
            else -> false
        }
    }

    companion object {
        /** Mặc định khi chưa có cache: bật hết, để SDK không tự khoá tính năng lúc chưa gọi được API. */
        val AllEnabled = PromotionFeatureFlags(
            enableAll = true,
            voucherApply = true,
            voucherRedeem = true,
            voucherSelection = true,
            voucherDetail = true,
            voucherList = true,
        )
    }
}
