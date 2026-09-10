package com.ttcn.prm.ui.theme.applier

import com.ttcn.prm.ui.theme.token.PRMTabChipToken

import android.widget.TextView
import com.ttcn.prm.ui.utils.applyBackgroundColorIfSet
import com.ttcn.prm.ui.utils.applyCornerRadiusDp
import com.ttcn.prm.ui.utils.applyTextColorIfSet

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
