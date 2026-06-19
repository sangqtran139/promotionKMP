package com.ttcn.promotionsdk.ui.theme.applier

import com.ttcn.promotionsdk.ui.theme.token.TabChipToken

import android.widget.TextView
import com.ttcn.promotionsdk.ui.utils.applyBackgroundColorIfSet
import com.ttcn.promotionsdk.ui.utils.applyCornerRadiusDp
import com.ttcn.promotionsdk.ui.utils.applyTextColorIfSet

internal object TabChipThemeApplier {

    fun applyChip(
        textView: TextView,
        selected: Boolean,
        token: TabChipToken?,
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
