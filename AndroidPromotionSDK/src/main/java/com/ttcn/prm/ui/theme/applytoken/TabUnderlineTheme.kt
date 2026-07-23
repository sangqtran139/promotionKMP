package com.ttcn.prm.ui.theme.applytoken

import com.ttcn.prm.ui.theme.applier.TabLayoutThemeApplier

import com.google.android.material.tabs.TabLayout
import com.ttcn.prm.ui.theme.PromotionThemeDisplay
import com.ttcn.prm.ui.theme.toToken

object TabUnderlineTheme {

    fun applyToken(tabs: TabLayout, values: PromotionThemeDisplay.TabUnderlineValues) {
        TabLayoutThemeApplier.apply(tabs, values.toToken())
    }
}
