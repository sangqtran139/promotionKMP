package com.ttcn.promotionsdk.promotionsdkui.theme

import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMButtonToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMDiscountBadgeToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMListItemToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMSearchBarToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabChipToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabUnderlineToken

import android.content.Context
import androidx.annotation.ColorInt
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.promotionsdkui.utils.enum.PRMCoreButtonType
import com.ttcn.promotionsdk.promotionsdkui.widget.PRMSearchType
import com.ttcn.promotionsdk.promotionsdkui.utils.extension.retrieveColor

/**
 * Internal SDK defaults extracted from component source / resources.
 * Used by theme preview and when resolving unset token properties.
 */
internal object PromotionThemeDefaults {

    /**
     * Defaults aligned with [PRMButton] PRIMARY (common CTA in promotion screens).
     * Background is gradient in SDK; token stores gradient end color [R.color.tokenRainbowRedEnd].
     * Corner radius follows foundation button drawables ([R.dimen.tokenBorderRadius24] → _19sdp).
     * Note: sdp scales on device (e.g. ~25dp on a wide screen); that is the physical radius to apply via [PRMButtonToken].
     */
    fun button(context: Context) = PRMButtonToken(
        backgroundColor = context.retrieveColor(R.color.tokenRainbowRedEnd),
        textColor = context.retrieveColor(PRMCoreButtonType.PRIMARY.textColorRes),
        shadowColor = context.retrieveColor(R.color.tokenShadowsButtonColor),
        cornerRadius = pxToDp(context, context.resources.getDimension(R.dimen.tokenBorderRadius24))
    )

    fun searchBar(context: Context) = PRMSearchBarToken(
        borderColor = context.retrieveColor(R.color.tokenDark10),
        hintTextColor = context.retrieveColor(PRMSearchType.BASIC.hintColorRes),
        textColor = context.retrieveColor(PRMSearchType.BASIC.textColorRes),
        iconColor = context.retrieveColor(R.color.tokenDark40),
        cornerRadius = pxToDp(context, context.resources.getDimension(R.dimen.tokenBorderRadius08)),
    )

    fun listItem(context: Context) = PRMListItemToken(
        linkTextColor = context.retrieveColor(R.color.color_EE0033),
        usedBadgeTextColor = context.retrieveColor(R.color.tokenDark100),
        usedBadgeBackgroundColor = context.retrieveColor(R.color.color_f4f4f4),
        radioButtonStrokeColor = context.retrieveColor(R.color.tokenDark40),
        radioButtonSelectedStrokeColor = context.retrieveColor(R.color.color_EE0033),
    )

    fun tabChip(context: Context) = PRMTabChipToken(
        activeBackgroundColor = context.retrieveColor(R.color.color_4e4e4e),
        inactiveBackgroundColor = context.retrieveColor(R.color.color_f4f4f4),
        activeTextColor = context.retrieveColor(R.color.white),
        inactiveTextColor = context.retrieveColor(R.color.color_7A7A7A),
        cornerRadius = pxToDp(context, context.resources.getDimension(R.dimen.view_size_7)),
    )

    fun tabUnderline(context: Context) = PRMTabUnderlineToken(
        indicatorColor = context.retrieveColor(R.color.color_red_EE0033),
        activeTextColor = context.retrieveColor(R.color.black),
        inactiveTextColor = context.retrieveColor(R.color.color_7a7a7a),
        backgroundColor = context.retrieveColor(R.color.color_FBFBFB),
    )

    fun discountBadge(context: Context) = PRMDiscountBadgeToken(
        availableTextColor = context.retrieveColor(R.color.tokenPineBlue100),
        unavailableTextColor = context.retrieveColor(R.color.color_7A7A7A),
        availableBackgroundColor = context.retrieveColor(R.color.tokenPineBlue10),
        unavailableBackgroundColor = context.retrieveColor(R.color.color_f4f4f4),
        actionTextColor = context.retrieveColor(R.color.color_EE0033),
    )

    fun theme(context: Context) = PRMSDKTheme(
        buttonToken = button(context),
        searchBarToken = searchBar(context),
        listItemToken = listItem(context),
        tabChipToken = tabChip(context),
        tabUnderlineToken = tabUnderline(context),
        discountBadgeToken = discountBadge(context),
    )

    fun colorToHex(@ColorInt color: Int): String = ThemeHex.format(color)

    fun pxToDp(context: Context, px: Float): Float =
        px / context.resources.displayMetrics.density
}
