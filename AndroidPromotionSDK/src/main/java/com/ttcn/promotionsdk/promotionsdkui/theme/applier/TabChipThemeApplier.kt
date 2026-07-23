package com.ttcn.promotionsdk.promotionsdkui.theme.applier

import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabChipToken

import android.widget.TextView
import com.ttcn.promotionsdk.promotionsdkui.utils.applyBackgroundColorIfSet
import com.ttcn.promotionsdk.promotionsdkui.utils.applyCornerRadiusDp
import com.ttcn.promotionsdk.promotionsdkui.utils.applyTextColorIfSet

internal object TabChipThemeApplier {

    fun applyChip(
        textView: TextView,
        selected: Boolean,
        token: PRMTabChipToken?,
    ) {
        if (token == null) return
        if (selected) {
            token.activeBackgroundColor?.let { textView.applyBackgroundColorIfSet(it) }
            token.activeTextColor?.let { textView.applyTextColorIfSet(it) }
        } else {
            token.inactiveBackgroundColor?.let { textView.applyBackgroundColorIfSet(it) }
            token.inactiveTextColor?.let { textView.applyTextColorIfSet(it) }
        }
        token.cornerRadius?.let { textView.applyCornerRadiusDp(it) }
    }
}
