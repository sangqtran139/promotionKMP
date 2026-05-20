package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding

object DiscountBadgeTheme {

    fun applyToken(
        binding: ItemListPromotionApplyBinding,
        values: PromotionThemeDisplay.DiscountBadgeValues,
        available: Boolean,
    ) {
        DiscountBadgeApplier.apply(binding, values.toToken(), available)
    }
}
