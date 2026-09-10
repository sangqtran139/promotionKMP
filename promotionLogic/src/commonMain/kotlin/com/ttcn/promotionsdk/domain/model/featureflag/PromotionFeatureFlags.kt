package com.ttcn.promotionsdk.domain.model.featureflag

public data class FeatureFlag(
    val name: String,
    val enabled: Boolean,
)

public object PromotionFeatureFlag {
    public const val ENABLE_ALL: String = "PROMOTION.ENABLE_ALL"
    public const val VOUCHER_APPLY: String = "PROMOTION.VOUCHER_APPLY"
    public const val VOUCHER_REDEEM: String = "PROMOTION.VOUCHER_REDEEM"
    public const val VOUCHER_SELECTION: String = "PROMOTION.VOUCHER_SELECTION"
    public const val VOUCHER_DETAIL: String = "PROMOTION.VOUCHER_DETAIL"
    public const val VOUCHER_LIST: String = "PROMOTION.VOUCHER_LIST"
}

public data class PromotionFeatureFlags(
    val enableAll: Boolean,
    val voucherApply: Boolean,
    val voucherRedeem: Boolean,
    val voucherSelection: Boolean,
    val voucherDetail: Boolean,
    val voucherList: Boolean,
) {
    /**
     * [PromotionFeatureFlag.ENABLE_ALL] là công tắc tổng: tắt nó thì mọi cờ con đều tắt.
     * Hỏi thẳng `ENABLE_ALL` trả về chính [enableAll]. Tên cờ lạ trả `true` — xem nhánh `else`.
     */
    public fun isEnabled(flag: String): Boolean {
        if (!enableAll) return false
        return when (flag) {
            PromotionFeatureFlag.ENABLE_ALL -> enableAll
            PromotionFeatureFlag.VOUCHER_APPLY -> voucherApply
            PromotionFeatureFlag.VOUCHER_REDEEM -> voucherRedeem
            PromotionFeatureFlag.VOUCHER_SELECTION -> voucherSelection
            PromotionFeatureFlag.VOUCHER_DETAIL -> voucherDetail
            PromotionFeatureFlag.VOUCHER_LIST -> voucherList
            // Tên cờ SDK chưa biết ⇒ BẬT. Server chưa từng trả `false` cho nó, nên không có căn cứ
            // để tắt. Cùng luật với mapper: chỉ `enabled: false` mới tắt.
            else -> true
        }
    }

    /**
     * Bản đã **áp sẵn công tắc tổng**: đọc thẳng field cũng cho kết quả giống hệt [isEnabled].
     * Bất biến `normalized().voucherList == isEnabled(VOUCHER_LIST)`, đúng cho cả sáu cờ.
     *
     * **Không** dùng cho bản đem đi lưu cache — `FeatureFlagLocalDataSource` giữ giá trị thô để
     * server bật lại `ENABLE_ALL` thì các cờ con trở về đúng giá trị riêng thay vì kẹt `false`.
     */
    public fun normalized(): PromotionFeatureFlags = if (enableAll) this else AllDisabled

    public companion object {
        /** Mặc định khi chưa có cache: bật hết, để SDK không tự khoá tính năng lúc chưa gọi được API. */
        public val AllEnabled: PromotionFeatureFlags = PromotionFeatureFlags(
            enableAll = true,
            voucherApply = true,
            voucherRedeem = true,
            voucherSelection = true,
            voucherDetail = true,
            voucherList = true,
        )

        /** Kết quả của [normalized] khi công tắc tổng tắt — tắt tổng là tắt hết, không ngoại lệ. */
        public val AllDisabled: PromotionFeatureFlags = PromotionFeatureFlags(
            enableAll = false,
            voucherApply = false,
            voucherRedeem = false,
            voucherSelection = false,
            voucherDetail = false,
            voucherList = false,
        )
    }
}
