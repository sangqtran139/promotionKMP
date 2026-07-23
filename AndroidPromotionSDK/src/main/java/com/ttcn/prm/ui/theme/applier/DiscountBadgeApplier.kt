package com.ttcn.prm.ui.theme.applier

import com.ttcn.prm.ui.theme.token.DiscountBadgeToken

import com.ttcn.prm.R
import com.ttcn.prm.databinding.ItemListPromotionApplyBinding
import com.ttcn.prm.ui.utils.applyDrawableBackgroundTintIfSet
import com.ttcn.prm.ui.utils.applyTextColorIfSet

internal object DiscountBadgeApplier {

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
