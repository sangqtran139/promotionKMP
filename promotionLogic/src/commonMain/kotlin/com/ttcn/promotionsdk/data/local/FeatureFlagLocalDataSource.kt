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
        return PromotionFeatureFlags(
            enableAll = storage.getBoolean(PromotionFeatureFlag.ENABLE_ALL),
            voucherApply = storage.getBoolean(PromotionFeatureFlag.VOUCHER_APPLY),
            voucherRedeem = storage.getBoolean(PromotionFeatureFlag.VOUCHER_REDEEM),
            voucherSelection = storage.getBoolean(PromotionFeatureFlag.VOUCHER_SELECTION),
            voucherDetail = storage.getBoolean(PromotionFeatureFlag.VOUCHER_DETAIL),
            voucherList = storage.getBoolean(PromotionFeatureFlag.VOUCHER_LIST),
        )
    }

    fun hasCache(): Boolean = storage.contains(KEY_HAS_CACHE)

    private companion object {
        const val KEY_HAS_CACHE = "feature_flag_has_cache"
    }
}
