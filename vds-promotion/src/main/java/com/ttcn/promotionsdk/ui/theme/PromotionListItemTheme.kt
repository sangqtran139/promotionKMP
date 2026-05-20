package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding

object PromotionListItemTheme {

    fun applyToken(binding: ItemChoosePromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }

    fun applyToken(binding: PrmItemPromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }
}
