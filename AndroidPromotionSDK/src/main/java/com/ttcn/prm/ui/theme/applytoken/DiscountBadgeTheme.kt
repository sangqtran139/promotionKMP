package com.ttcn.prm.ui.theme.applytoken

import com.ttcn.prm.ui.theme.applier.DiscountBadgeApplier

import com.ttcn.prm.databinding.ItemListPromotionApplyBinding
import com.ttcn.prm.ui.theme.PromotionThemeDisplay
import com.ttcn.prm.ui.theme.toToken

internal object DiscountBadgeTheme {

    fun applyToken(
        binding: ItemListPromotionApplyBinding,
        values: PromotionThemeDisplay.DiscountBadgeValues,
        available: Boolean,
    ) {
        DiscountBadgeApplier.apply(binding, values.toToken(), available)
    }
}
