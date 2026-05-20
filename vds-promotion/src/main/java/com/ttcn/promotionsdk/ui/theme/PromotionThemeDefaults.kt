package com.ttcn.promotionsdk.ui.theme

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.ui.utils.enum.PRMCoreButtonSize
import com.ttcn.promotionsdk.ui.utils.enum.PRMCoreButtonType
import com.ttcn.promotionsdk.ui.utils.enum.PRMShadowType
import com.ttcn.promotionsdk.ui.utils.enum.PRMSearchType
import com.ttcn.promotionsdk.ui.utils.extension.retrieveColor

/**
 * Internal SDK defaults extracted from component source / resources.
 * Used by theme preview and when resolving unset token properties.
 */
internal object PromotionThemeDefaults {

    /**
     * Defaults aligned with [PRMButton] PRIMARY + LARGE (common CTA in promotion screens).
     * Background is gradient in SDK; token stores gradient end color [R.color.tokenRainbowRedEnd].
     * Corner radius matches [PRMShadowType.TokenShadowsButtonLarge] (in dp).
     */
    fun button(context: Context) = ButtonToken(
        backgroundColor = context.retrieveColor(R.color.tokenRainbowRedEnd),
        textColor = context.retrieveColor(PRMCoreButtonType.PRIMARY.textColorRes),
        shadowColor = context.retrieveColor(R.color.tokenShadowsButtonColor),
        cornerRadius = buttonCornerRadiusDp(context, PRMCoreButtonSize.LARGE),
    )

    fun buttonCornerRadiusDp(context: Context, size: PRMCoreButtonSize = PRMCoreButtonSize.LARGE): Float {
        val shadowType = when (size) {
            PRMCoreButtonSize.SMALL -> PRMShadowType.TokenShadowsButtonSmall
            PRMCoreButtonSize.MEDIUM -> PRMShadowType.TokenShadowsButtonMedium
            PRMCoreButtonSize.LARGE -> PRMShadowType.TokenShadowsButtonLarge
        }
        return pxToDp(context, context.resources.getDimension(shadowType.cornerRes))
    }

    fun searchBar(context: Context) = SearchBarToken(
        borderColor = context.retrieveColor(R.color.tokenDark10),
        hintTextColor = context.retrieveColor(PRMSearchType.BASIC.hintColorRes),
        textColor = context.retrieveColor(PRMSearchType.BASIC.textColorRes),
        iconColor = context.retrieveColor(R.color.tokenDark40),
        cornerRadius = pxToDp(context, context.resources.getDimension(R.dimen.tokenBorderRadius08)),
    )

    fun listItem(context: Context) = ListItemToken(
        linkTextColor = context.retrieveColor(R.color.color_EE0033),
        usedBadgeTextColor = context.retrieveColor(R.color.tokenDark100),
        usedBadgeBackgroundColor = context.retrieveColor(R.color.color_f4f4f4),
        radioButtonStrokeColor = context.retrieveColor(R.color.tokenDark40),
        radioButtonSelectedStrokeColor = context.retrieveColor(R.color.color_EE0033),
    )

    fun tabChip(context: Context) = TabChipToken(
        activeBackgroundColor = context.retrieveColor(R.color.color_4e4e4e),
        inactiveBackgroundColor = context.retrieveColor(R.color.color_f4f4f4),
        activeTextColor = context.retrieveColor(R.color.white),
        inactiveTextColor = context.retrieveColor(R.color.color_7A7A7A),
        cornerRadius = pxToDp(context, context.resources.getDimension(R.dimen.view_size_7)),
    )

    fun tabUnderline(context: Context) = TabUnderlineToken(
        indicatorColor = context.retrieveColor(R.color.color_red_EE0033),
        activeTextColor = context.retrieveColor(R.color.black),
        inactiveTextColor = context.retrieveColor(R.color.color_7a7a7a),
        backgroundColor = context.retrieveColor(R.color.color_FBFBFB),
    )

    fun discountBadge(context: Context) = DiscountBadgeToken(
        availableTextColor = context.retrieveColor(R.color.tokenPineBlue100),
        unavailableTextColor = context.retrieveColor(R.color.color_7A7A7A),
        availableBackgroundColor = context.retrieveColor(R.color.tokenPineBlue10),
        unavailableBackgroundColor = context.retrieveColor(R.color.color_f4f4f4),
        actionTextColor = context.retrieveColor(R.color.color_EE0033),
    )

    fun defaultConfig(context: Context) = PromotionThemeConfig(
        buttonToken = button(context),
        searchBarToken = searchBar(context),
        listItemToken = listItem(context),
        tabChipToken = tabChip(context),
        tabUnderlineToken = tabUnderline(context),
        discountBadgeToken = discountBadge(context),
    )

    @ColorInt
    fun colorToHex(@ColorInt color: Int): String =
        if (Color.alpha(color) != 0xFF) {
            String.format("#%08X", color)
        } else {
            String.format("#%06X", 0xFFFFFF and color)
        }

    fun pxToDp(context: Context, px: Float): Float =
        px / context.resources.displayMetrics.density
}
