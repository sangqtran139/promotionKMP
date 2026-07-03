package com.ttcn.promotionsdk.core.domain.model.featureflag

data class PromotionFeatureFlags(
    val enableAll: Boolean,
    val voucherApply: Boolean,
    val voucherRedeem: Boolean,
    val voucherSelection: Boolean,
    val voucherDetail: Boolean,
    val voucherList: Boolean,
) {
    fun isEnabled(flag: String): Boolean {
        if (!enableAll) return false
        return when (flag) {
            PromotionFeatureFlag.VOUCHER_APPLY -> voucherApply
            PromotionFeatureFlag.VOUCHER_REDEEM -> voucherRedeem
            PromotionFeatureFlag.VOUCHER_SELECTION -> voucherSelection
            PromotionFeatureFlag.VOUCHER_DETAIL -> voucherDetail
            PromotionFeatureFlag.VOUCHER_LIST -> voucherList
            else -> false
        }
    }
}
