package com.ttcn.promotionsdk.domain.model.featureflag

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
     * Hỏi thẳng `ENABLE_ALL` trả về chính [enableAll]. Tên cờ lạ trả `false`.
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

    /**
     * Bản đã **áp sẵn công tắc tổng**: đọc thẳng field cũng cho kết quả giống hệt [isEnabled].
     * Bất biến `normalized().voucherList == isEnabled(VOUCHER_LIST)`, đúng cho cả sáu cờ.
     *
     * **Không** dùng cho bản đem đi lưu cache — `FeatureFlagLocalDataSource` giữ giá trị thô để
     * server bật lại `ENABLE_ALL` thì các cờ con trở về đúng giá trị riêng thay vì kẹt `false`.
     */
    fun normalized(): PromotionFeatureFlags = if (enableAll) this else AllDisabled

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

        /** Kết quả của [normalized] khi công tắc tổng tắt — tắt tổng là tắt hết, không ngoại lệ. */
        val AllDisabled = PromotionFeatureFlags(
            enableAll = false,
            voucherApply = false,
            voucherRedeem = false,
            voucherSelection = false,
            voucherDetail = false,
            voucherList = false,
        )
    }
}
