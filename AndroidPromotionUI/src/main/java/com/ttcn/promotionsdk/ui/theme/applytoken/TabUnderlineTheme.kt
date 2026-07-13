package com.ttcn.promotionsdk.ui.theme.applytoken

import com.ttcn.promotionsdk.ui.theme.applier.TabLayoutThemeApplier

import com.google.android.material.tabs.TabLayout
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDisplay
import com.ttcn.promotionsdk.ui.theme.toToken

object TabUnderlineTheme {

    fun applyToken(tabs: TabLayout, values: PromotionThemeDisplay.TabUnderlineValues) {
        TabLayoutThemeApplier.apply(tabs, values.toToken())
    }
}
