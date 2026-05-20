package com.ttcn.promotionsdk.ui.theme

import com.google.android.material.tabs.TabLayout

object TabLayoutThemeApplier {

    fun apply(tabs: TabLayout, token: TabUnderlineToken?) {
        token?.indicatorColor?.let { tabs.setSelectedTabIndicatorColor(it) }
        val inactive = token?.inactiveTextColor
        val active = token?.activeTextColor
        if (inactive != null || active != null) {
            val current = tabs.tabTextColors
            val defaultInactive = current?.defaultColor ?: 0
            val defaultActive = current?.getColorForState(
                intArrayOf(android.R.attr.state_selected),
                defaultInactive,
            ) ?: defaultInactive
            tabs.setTabTextColors(
                inactive ?: defaultInactive,
                active ?: defaultActive,
            )
        }
        token?.backgroundColor?.let { tabs.setBackgroundColor(it) }
    }
}
