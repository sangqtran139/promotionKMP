package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.ui.theme.applier.DiscountBadgeApplier

import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding

internal object DiscountBadgeTheme {

    fun applyToken(
        binding: ItemListPromotionApplyBinding,
        values: PromotionThemeDisplay.DiscountBadgeValues,
        available: Boolean,
    ) {
        DiscountBadgeApplier.apply(binding, values.toToken(), available)
    }
}
