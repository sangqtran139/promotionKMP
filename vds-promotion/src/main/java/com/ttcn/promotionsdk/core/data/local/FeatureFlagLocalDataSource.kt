package com.ttcn.promotionsdk.core.data.local

import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags

internal class FeatureFlagLocalDataSource(private val storage: SharedPrefStorage) {

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
        if (!storage.contains(KEY_HAS_CACHE)) return allEnabled()
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

    private fun allEnabled() = PromotionFeatureFlags(
        enableAll = true,
        voucherApply = true,
        voucherRedeem = true,
        voucherSelection = true,
        voucherDetail = true,
        voucherList = true,
    )

    companion object {
        private const val KEY_HAS_CACHE = "feature_flag_has_cache"
    }
}
