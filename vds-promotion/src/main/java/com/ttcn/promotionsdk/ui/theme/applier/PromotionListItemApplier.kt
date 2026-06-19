package com.ttcn.promotionsdk.ui.theme.applier

import com.ttcn.promotionsdk.ui.theme.token.ListItemToken

import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding
import com.ttcn.promotionsdk.ui.utils.applyBackgroundColorIfSet
import com.ttcn.promotionsdk.ui.utils.applyRadioStrokeColors
import com.ttcn.promotionsdk.ui.utils.applyTextColorIfSet

internal object PromotionListItemApplier {

    fun apply(binding: ItemChoosePromotionBinding, token: ListItemToken?) {
        token?.linkTextColor?.let {
            binding.tvDetail.applyTextColorIfSet(it)
        }
        token?.usedBadgeTextColor?.let { binding.txtExpired.applyTextColorIfSet(it) }
        token?.usedBadgeBackgroundColor?.let { binding.txtExpired.applyBackgroundColorIfSet(it) }
        binding.cbUseVoucher.applyRadioStrokeColors(
            unselectedStroke = token?.radioButtonStrokeColor,
            selectedFill = token?.radioButtonSelectedStrokeColor,
        )
    }

    fun apply(binding: PrmItemPromotionBinding, token: ListItemToken?) {
        token?.linkTextColor?.let { binding.tvUse.applyTextColorIfSet(it) }
        token?.usedBadgeTextColor?.let { binding.txtExpired.applyTextColorIfSet(it) }
        token?.usedBadgeBackgroundColor?.let { binding.txtExpired.applyBackgroundColorIfSet(it) }
    }
}
