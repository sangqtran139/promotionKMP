package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.ItemListPromotionApplyBinding
import com.ttcn.promotionsdk.ui.utils.applyDrawableBackgroundTintIfSet
import com.ttcn.promotionsdk.ui.utils.applyTextColorIfSet

object DiscountBadgeApplier {

    fun apply(
        binding: ItemListPromotionApplyBinding,
        token: DiscountBadgeToken?,
        available: Boolean,
    ) {
        if (token == null) return
        if (available) {
            token.availableTextColor?.let { binding.txtName.applyTextColorIfSet(it) }
            val bg = token.availableBackgroundColor
            if (bg != null) {
                binding.root.applyDrawableBackgroundTintIfSet(bg, R.drawable.prm_bg_apply_endow)
            } else {
                binding.root.setBackgroundResource(R.drawable.prm_bg_apply_endow)
            }
        } else {
            token.unavailableTextColor?.let { binding.txtName.applyTextColorIfSet(it) }
            val bg = token.unavailableBackgroundColor
            if (bg != null) {
                binding.root.applyDrawableBackgroundTintIfSet(bg, R.drawable.prm_bg_apply_endow)
            } else {
                binding.root.setBackgroundResource(R.drawable.prm_bg_apply_endow)
            }
        }
    }
}
