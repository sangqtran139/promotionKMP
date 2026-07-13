package com.ttcn.promotionsdk.ui.theme.applytoken

import com.ttcn.promotionsdk.ui.theme.applier.DiscountBadgeApplier

import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDisplay
import com.ttcn.promotionsdk.ui.theme.toToken

internal object DiscountBadgeTheme {

    fun applyToken(
        binding: ItemListPromotionApplyBinding,
        values: PromotionThemeDisplay.DiscountBadgeValues,
        available: Boolean,
    ) {
        DiscountBadgeApplier.apply(binding, values.toToken(), available)
    }
}
