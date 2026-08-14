package com.ttcn.prm.ui.theme.applier

import androidx.core.content.ContextCompat
import com.ttcn.prm.ui.theme.token.DiscountBadgeToken

import com.ttcn.prm.R
import com.ttcn.prm.databinding.PrmItemListPromotionApplyBinding
import com.ttcn.prm.ui.utils.applyDrawableBackgroundTintIfSet
import com.ttcn.prm.ui.utils.applyTextColorIfSet

/**
 * `available=true` → nền/chữ xanh mint (`prm_tokenPineBlue10`/`prm_tokenPineBlue100`, như voucher
 * khả dụng). `available=false` → nền/chữ xám, đồng bộ nút "+N" (`prm_color_f4f4f4`/`prm_color_4e4e4e`)
 * thay vì chỉ hạ alpha như trước. Token host set thì override theo field, thiếu field nào thì rơi về
 * màu mặc định này.
 */
internal object DiscountBadgeApplier {

    fun apply(
        binding: PrmItemListPromotionApplyBinding,
        token: DiscountBadgeToken?,
        available: Boolean,
    ) {
        val context = binding.root.context
        val textColor = (if (available) token?.availableTextColor else token?.unavailableTextColor)
            ?: ContextCompat.getColor(
                context,
                if (available) R.color.prm_tokenPineBlue100 else R.color.prm_color_4e4e4e,
            )
        val bgColor = (if (available) token?.availableBackgroundColor else token?.unavailableBackgroundColor)
            ?: ContextCompat.getColor(
                context,
                if (available) R.color.prm_tokenPineBlue10 else R.color.prm_color_f4f4f4,
            )

        binding.txtName.applyTextColorIfSet(textColor)
        binding.root.applyDrawableBackgroundTintIfSet(bgColor, R.drawable.prm_bg_apply_endow)
    }
}
