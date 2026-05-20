package com.ttcn.promotionsdk.ui.theme

import com.google.android.material.tabs.TabLayout

object TabUnderlineTheme {

    fun applyToken(tabs: TabLayout, values: PromotionThemeDisplay.TabUnderlineValues) {
        TabLayoutThemeApplier.apply(tabs, values.toToken())
    }
}
