package com.ttcn.promotionsdk.data.local

import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlags

internal class FeatureFlagLocalDataSource(private val storage: PromotionPreferences) {

    fun save(flags: PromotionFeatureFlags) {
        storage.putBoolean(KEY_HAS_CACHE, true)
        storage.putBoolean(PromotionFeatureFlag.ENABLE_ALL, flags.enableAll)
        storage.putBoolean(PromotionFeatureFlag.VOUCHER_APPLY, flags.voucherApply)
        storage.putBoolean(PromotionFeatureFlag.VOUCHER_REDEEM, flags.voucherRedeem)
        storage.putBoolean(PromotionFeatureFlag.VOUCHER_SELECTION, flags.voucherSelection)
        storage.putBoolean(PromotionFeatureFlag.VOUCHER_DETAIL, flags.voucherDetail)
        storage.putBoolean(PromotionFeatureFlag.VOUCHER_LIST, flags.voucherList)
    }

    fun load(): PromotionFeatureFlags {
        if (!storage.contains(KEY_HAS_CACHE)) return PromotionFeatureFlags.AllEnabled
        // `default = true` ở mọi khoá: cache cũ được ghi bởi bản SDK trước có thể thiếu khoá (vd
        // thêm cờ mới), và khoá thiếu KHÔNG được phép tắt tính năng — cùng luật với mapper.
        return PromotionFeatureFlags(
            enableAll = storage.getBoolean(PromotionFeatureFlag.ENABLE_ALL, default = true),
            voucherApply = storage.getBoolean(PromotionFeatureFlag.VOUCHER_APPLY, default = true),
            voucherRedeem = storage.getBoolean(PromotionFeatureFlag.VOUCHER_REDEEM, default = true),
            voucherSelection = storage.getBoolean(PromotionFeatureFlag.VOUCHER_SELECTION, default = true),
            voucherDetail = storage.getBoolean(PromotionFeatureFlag.VOUCHER_DETAIL, default = true),
            voucherList = storage.getBoolean(PromotionFeatureFlag.VOUCHER_LIST, default = true),
        )
    }

    fun hasCache(): Boolean = storage.contains(KEY_HAS_CACHE)

    private companion object {
        const val KEY_HAS_CACHE = "feature_flag_has_cache"
    }
}
