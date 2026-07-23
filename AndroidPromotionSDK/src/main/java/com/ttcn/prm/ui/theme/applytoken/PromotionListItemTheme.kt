package com.ttcn.prm.ui.theme.applytoken

import com.ttcn.prm.ui.theme.applier.PromotionListItemApplier

import com.ttcn.prm.databinding.ItemChoosePromotionBinding
import com.ttcn.prm.databinding.PrmItemPromotionBinding
import com.ttcn.prm.ui.theme.PromotionThemeDisplay
import com.ttcn.prm.ui.theme.toToken

object PromotionListItemTheme {

    fun applyToken(binding: ItemChoosePromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }

    fun applyToken(binding: PrmItemPromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }
}
