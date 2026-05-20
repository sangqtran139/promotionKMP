package com.ttcn.promotionsdk.ui.entry

import android.content.Context
import androidx.annotation.ColorInt
import com.ttcn.promotionsdk.ui.theme.ButtonToken
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDefaults
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDisplay
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.ListItemToken
import com.ttcn.promotionsdk.ui.theme.PromotionThemeConfig
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.SearchBarToken
import com.ttcn.promotionsdk.ui.theme.TabChipToken
import com.ttcn.promotionsdk.ui.theme.TabUnderlineToken

/**
 * Public theme API for host applications.
 * Re-exports token types from the UI theme package.
 */
object PromotionTheme {

    fun configure(config: PromotionThemeConfig) {
        PromotionThemeRegistry.configure(config)
        PromotionSDK.syncThemeConfig(config)
    }

    fun currentConfig(): PromotionThemeConfig? = PromotionThemeRegistry.currentConfig()

    fun clear() {
        PromotionThemeRegistry.configure(null)
        PromotionSDK.syncThemeConfig(null)
    }

    /** SDK-internal default values for theme preview / documentation. */
    fun sdkDefaults(context: Context): PromotionThemeConfig =
        PromotionThemeDefaults.defaultConfig(context)

    fun colorToHex(@ColorInt color: Int): String = PromotionThemeDefaults.colorToHex(color)

    fun pxToDp(context: Context, px: Float): Float = PromotionThemeDefaults.pxToDp(context, px)

    fun loadDisplayDefaults(context: Context): PromotionThemeDisplay.Defaults =
        PromotionThemeDisplay.load(context)

    fun mergeDisplayWithSaved(
        context: Context,
        sdk: PromotionThemeDisplay.Defaults,
        saved: PromotionThemeConfig?,
    ): PromotionThemeDisplay.Defaults =
        PromotionThemeDisplay.mergeWithSaved(context, sdk, saved)
}

typealias ThemeButtonToken = ButtonToken
typealias ThemeSearchBarToken = SearchBarToken
typealias ThemeListItemToken = ListItemToken
typealias ThemeTabChipToken = TabChipToken
typealias ThemeTabUnderlineToken = TabUnderlineToken
typealias ThemeDiscountBadgeToken = DiscountBadgeToken
typealias ThemeConfig = PromotionThemeConfig
