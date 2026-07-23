package com.ttcn.promotionsdk.promotionsdkui.theme.applytoken

import com.ttcn.promotionsdk.promotionsdkui.theme.applier.DiscountBadgeApplier

import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding
import com.ttcn.promotionsdk.promotionsdkui.theme.PRMThemeDisplay
import com.ttcn.promotionsdk.promotionsdkui.theme.toToken

internal object DiscountBadgeTheme {

    fun applyToken(
        binding: ItemListPromotionApplyBinding,
        values: PRMThemeDisplay.DiscountBadgeValues,
        available: Boolean,
    ) {
        DiscountBadgeApplier.apply(binding, values.toToken(), available)
    }
}
