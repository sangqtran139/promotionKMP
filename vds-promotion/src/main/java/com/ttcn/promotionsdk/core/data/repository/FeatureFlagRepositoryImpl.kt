package com.ttcn.promotionsdk.core.data.repository

import io.getunleash.android.DefaultUnleash
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.core.domain.repository.FeatureFlagRepository

internal class FeatureFlagRepositoryImpl(
    private val unleash: DefaultUnleash,
) : FeatureFlagRepository {

    override fun isEnabled(featureName: String): Boolean = unleash.isEnabled(featureName)

    override fun getPromotionFeatureFlags(): PromotionFeatureFlags = PromotionFeatureFlags(
        enableAll = unleash.isEnabled(PromotionFeatureFlag.ENABLE_ALL),
        voucherApply = unleash.isEnabled(PromotionFeatureFlag.VOUCHER_APPLY),
        voucherRedeem = unleash.isEnabled(PromotionFeatureFlag.VOUCHER_REDEEM),
        voucherSelection = unleash.isEnabled(PromotionFeatureFlag.VOUCHER_SELECTION),
        voucherDetail = unleash.isEnabled(PromotionFeatureFlag.VOUCHER_DETAIL),
        voucherList = unleash.isEnabled(PromotionFeatureFlag.VOUCHER_LIST),
    )
}
