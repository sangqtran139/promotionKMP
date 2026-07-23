package com.ttcn.promotionsdk.promotionsdkui.theme.applier

import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMListItemToken

import com.ttcn.promotionsdk.databinding.ItemChoosePromotionBinding
import com.ttcn.promotionsdk.databinding.PrmItemPromotionBinding
import com.ttcn.promotionsdk.promotionsdkui.utils.applyBackgroundColorIfSet
import com.ttcn.promotionsdk.promotionsdkui.utils.applyImageTintIfSet
import com.ttcn.promotionsdk.promotionsdkui.utils.applyRadioStrokeColors
import com.ttcn.promotionsdk.promotionsdkui.utils.applyTextColorIfSet

internal object PromotionListItemApplier {

    fun apply(binding: ItemChoosePromotionBinding, token: PRMListItemToken?) {
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

    fun apply(binding: PrmItemPromotionBinding, token: PRMListItemToken?) {
        token?.linkTextColor?.let {
            binding.tvUse.applyTextColorIfSet(it)
            binding.tvArrowDetail.applyImageTintIfSet(it)
        }
        token?.usedBadgeTextColor?.let { binding.txtExpired.applyTextColorIfSet(it) }
        token?.usedBadgeBackgroundColor?.let { binding.txtExpired.applyBackgroundColorIfSet(it) }
    }
}
