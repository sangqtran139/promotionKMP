package com.ttcn.prm.ui.theme.applier

import com.ttcn.prm.ui.theme.token.ListItemToken

import com.ttcn.prm.databinding.PrmItemChoosePromotionBinding
import com.ttcn.prm.databinding.PrmItemPromotionBinding
import com.ttcn.prm.ui.utils.applyBackgroundColorIfSet
import com.ttcn.prm.ui.utils.applyImageTintIfSet
import com.ttcn.prm.ui.utils.applyRadioStrokeColors
import com.ttcn.prm.ui.utils.applyTextColorIfSet

internal object PromotionListItemApplier {

    fun apply(binding: PrmItemChoosePromotionBinding, token: ListItemToken?) {
        token?.linkTextColor?.let {
            binding.tvDetail.applyTextColorIfSet(it)
            binding.tvArrowDetail.applyImageTintIfSet(it)
        }
        token?.usedBadgeTextColor?.let { binding.txtExpired.applyTextColorIfSet(it) }
        token?.usedBadgeBackgroundColor?.let { binding.txtExpired.applyBackgroundColorIfSet(it) }
        binding.cbUseVoucher.applyRadioStrokeColors(
            unselectedStroke = token?.radioButtonStrokeColor,
            selectedFill = token?.radioButtonSelectedStrokeColor,
        )
    }

    fun apply(binding: PrmItemPromotionBinding, token: ListItemToken?) {
        token?.linkTextColor?.let {
            binding.tvUse.applyTextColorIfSet(it)
            binding.tvArrowDetail.applyImageTintIfSet(it)
        }
        token?.usedBadgeTextColor?.let { binding.txtExpired.applyTextColorIfSet(it) }
        token?.usedBadgeBackgroundColor?.let { binding.txtExpired.applyBackgroundColorIfSet(it) }
    }
}
