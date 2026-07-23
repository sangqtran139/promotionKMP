package com.ttcn.promotionsdk.promotionsdkui.theme.applytoken

import com.ttcn.promotionsdk.promotionsdkui.theme.applier.PromotionListItemApplier

import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding
import com.ttcn.promotionsdk.promotionsdkui.theme.PRMThemeDisplay
import com.ttcn.promotionsdk.promotionsdkui.theme.toToken

object PRMListItemTheme {

    fun applyToken(binding: ItemChoosePromotionBinding, values: PRMThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }

    fun applyToken(binding: PrmItemPromotionBinding, values: PRMThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }
}
