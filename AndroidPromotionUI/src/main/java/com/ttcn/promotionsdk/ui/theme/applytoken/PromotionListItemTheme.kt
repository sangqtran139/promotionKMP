package com.ttcn.promotionsdk.ui.theme.applytoken

import com.ttcn.promotionsdk.ui.theme.applier.PromotionListItemApplier

import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDisplay
import com.ttcn.promotionsdk.ui.theme.toToken

object PromotionListItemTheme {

    fun applyToken(binding: ItemChoosePromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }

    fun applyToken(binding: PrmItemPromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }
}
