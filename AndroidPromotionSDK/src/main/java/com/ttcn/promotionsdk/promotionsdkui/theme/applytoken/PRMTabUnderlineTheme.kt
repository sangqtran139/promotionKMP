package com.ttcn.promotionsdk.promotionsdkui.theme.applytoken

import com.ttcn.promotionsdk.promotionsdkui.theme.applier.TabLayoutThemeApplier

import com.google.android.material.tabs.TabLayout
import com.ttcn.promotionsdk.promotionsdkui.theme.PRMThemeDisplay
import com.ttcn.promotionsdk.promotionsdkui.theme.toToken

object PRMTabUnderlineTheme {

    fun applyToken(tabs: TabLayout, values: PRMThemeDisplay.TabUnderlineValues) {
        TabLayoutThemeApplier.apply(tabs, values.toToken())
    }
}
