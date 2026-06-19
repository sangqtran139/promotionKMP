package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.ui.theme.applier.TabLayoutThemeApplier

import com.google.android.material.tabs.TabLayout

object TabUnderlineTheme {

    fun applyToken(tabs: TabLayout, values: PromotionThemeDisplay.TabUnderlineValues) {
        TabLayoutThemeApplier.apply(tabs, values.toToken())
    }
}
