package com.ttcn.prm.ui.theme.applytoken

import com.ttcn.prm.ui.theme.applier.PromotionListItemApplier

import com.ttcn.prm.databinding.PrmItemChoosePromotionBinding
import com.ttcn.prm.databinding.PrmItemPromotionBinding
import com.ttcn.prm.ui.theme.PromotionThemeDisplay
import com.ttcn.prm.ui.theme.toToken

object PromotionListItemTheme {

    fun applyToken(binding: PrmItemChoosePromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }

    fun applyToken(binding: PrmItemPromotionBinding, values: PromotionThemeDisplay.ListItemValues) {
        PromotionListItemApplier.apply(binding, values.toToken())
    }
}
