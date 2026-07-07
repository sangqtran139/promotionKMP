package com.ttcn.promotionsdk.ui.theme.applier

import com.ttcn.promotionsdk.ui.theme.token.TabUnderlineToken

import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import com.google.android.material.tabs.TabLayout
import com.ttcn.promotionsdk.R

internal object TabLayoutThemeApplier {

    fun apply(tabs: TabLayout, token: TabUnderlineToken?) {
        if (token == null) return

        token.indicatorColor?.let { color ->
            tabs.setSelectedTabIndicatorColor(color)
            tabs.setSelectedTabIndicator(createIndicatorDrawable(tabs, color))
        }

        val inactive = token.inactiveTextColor
        val active = token.activeTextColor
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

    }

    private fun createIndicatorDrawable(
        tabs: TabLayout,
        @ColorInt color: Int,
    ): GradientDrawable {
        val indicatorHeight = tabs.resources.getDimensionPixelSize(R.dimen.view_size_2)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            setSize(-1, indicatorHeight)
        }
    }
}
