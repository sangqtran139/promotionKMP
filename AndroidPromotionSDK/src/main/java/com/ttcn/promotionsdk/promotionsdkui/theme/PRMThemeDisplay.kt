package com.ttcn.promotionsdk.promotionsdkui.theme

import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMButtonToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMDiscountBadgeToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMListItemToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMSearchBarToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabChipToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabUnderlineToken

import android.content.Context
import com.ttcn.promotionsdk.promotionsdkui.theme.applytoken.PRMTokenColorParser

/**
 * Hex-string display model for theme preview UI.
 * Converts to [PRMSDKTheme] / token types for SDK apply.
 */
object PRMThemeDisplay {

    data class Defaults(
        val button: ButtonValues = ButtonValues(),
        val searchBar: SearchBarValues = SearchBarValues(),
        val listItem: ListItemValues = ListItemValues(),
        val tabChip: TabChipValues = TabChipValues(),
        val tabUnderline: TabUnderlineValues = TabUnderlineValues(),
        val discountBadge: DiscountBadgeValues = DiscountBadgeValues(),
    )

    data class ButtonValues(
        val backgroundColor: String? = null,
        val textColor: String? = null,
        val shadowColor: String? = null,
        val cornerRadius: Float? = null,
    )

    data class SearchBarValues(
        val borderColor: String? = null,
        val hintTextColor: String? = null,
        val textColor: String? = null,
        val iconColor: String? = null,
        val cornerRadius: Float? = null,
    )

    data class ListItemValues(
        val linkTextColor: String? = null,
        val usedBadgeTextColor: String? = null,
        val usedBadgeBackgroundColor: String? = null,
        val radioButtonStrokeColor: String? = null,
        val radioButtonSelectedStrokeColor: String? = null,
    )

    data class TabChipValues(
        val activeBackgroundColor: String? = null,
        val inactiveBackgroundColor: String? = null,
        val activeTextColor: String? = null,
        val inactiveTextColor: String? = null,
        val cornerRadius: Float? = null,
    )

    data class TabUnderlineValues(
        val indicatorColor: String? = null,
        val activeTextColor: String? = null,
        val inactiveTextColor: String? = null,
        val backgroundColor: String? = null,
    )

    data class DiscountBadgeValues(
        val availableTextColor: String? = null,
        val unavailableTextColor: String? = null,
        val availableBackgroundColor: String? = null,
        val unavailableBackgroundColor: String? = null,
        val actionTextColor: String? = null,
    )

    fun load(context: Context): Defaults =
        fromConfig(PromotionThemeDefaults.theme(context), context)

    fun mergeWithSaved(context: Context, sdk: Defaults, saved: PRMSDKTheme?): Defaults {
        if (saved == null) return sdk
        val savedDisplay = fromConfig(saved, context)
        return Defaults(
            button = sdk.button.merge(savedDisplay.button),
            searchBar = sdk.searchBar.merge(savedDisplay.searchBar),
            listItem = sdk.listItem.merge(savedDisplay.listItem),
            tabChip = sdk.tabChip.merge(savedDisplay.tabChip),
            tabUnderline = sdk.tabUnderline.merge(savedDisplay.tabUnderline),
            discountBadge = sdk.discountBadge.merge(savedDisplay.discountBadge),
        )
    }

    fun themeFromDisplayValues(display: Defaults, sdk: Defaults): PRMSDKTheme =
        PRMSDKTheme(
            buttonToken = display.button.toToken().takeIf { display.button != sdk.button },
            searchBarToken = display.searchBar.toToken()
                .takeIf { display.searchBar != sdk.searchBar },
            listItemToken = display.listItem.toToken().takeIf { display.listItem != sdk.listItem },
            tabChipToken = display.tabChip.toToken().takeIf { display.tabChip != sdk.tabChip },
            tabUnderlineToken = resolveTabUnderlineToken(display.tabUnderline, sdk.tabUnderline),
            discountBadgeToken = display.discountBadge.toToken()
                .takeIf { display.discountBadge != sdk.discountBadge },
        )

    private fun resolveTabUnderlineToken(
        display: TabUnderlineValues,
        sdk: TabUnderlineValues,
    ): PRMTabUnderlineToken? {
        if (display == sdk) return null
        val displayToken = display.toToken()
        val sdkToken = sdk.toToken()
        return PRMTabUnderlineToken(
            indicatorColor = displayToken.indicatorColor ?: sdkToken.indicatorColor,
            activeTextColor = displayToken.activeTextColor ?: sdkToken.activeTextColor,
            inactiveTextColor = displayToken.inactiveTextColor ?: sdkToken.inactiveTextColor,
            backgroundColor = displayToken.backgroundColor ?: sdkToken.backgroundColor,
        )
    }

    private fun fromConfig(config: PRMSDKTheme, context: Context?): Defaults {
        fun hex(@androidx.annotation.ColorInt color: Int?) =
            color?.let { PromotionThemeDefaults.colorToHex(it) }

        return Defaults(
            button = ButtonValues(
                backgroundColor = hex(config.buttonToken?.backgroundColor),
                textColor = hex(config.buttonToken?.textColor),
                shadowColor = hex(config.buttonToken?.shadowColor),
                cornerRadius = config.buttonToken?.cornerRadius,
            ),
            searchBar = SearchBarValues(
                borderColor = hex(config.searchBarToken?.borderColor),
                hintTextColor = hex(config.searchBarToken?.hintTextColor),
                textColor = hex(config.searchBarToken?.textColor),
                iconColor = hex(config.searchBarToken?.iconColor),
                cornerRadius = config.searchBarToken?.cornerRadius,
            ),
            listItem = ListItemValues(
                linkTextColor = hex(config.listItemToken?.linkTextColor),
                usedBadgeTextColor = hex(config.listItemToken?.usedBadgeTextColor),
                usedBadgeBackgroundColor = hex(config.listItemToken?.usedBadgeBackgroundColor),
                radioButtonStrokeColor = hex(config.listItemToken?.radioButtonStrokeColor),
                radioButtonSelectedStrokeColor = hex(config.listItemToken?.radioButtonSelectedStrokeColor),
            ),
            tabChip = TabChipValues(
                activeBackgroundColor = hex(config.tabChipToken?.activeBackgroundColor),
                inactiveBackgroundColor = hex(config.tabChipToken?.inactiveBackgroundColor),
                activeTextColor = hex(config.tabChipToken?.activeTextColor),
                inactiveTextColor = hex(config.tabChipToken?.inactiveTextColor),
                cornerRadius = config.tabChipToken?.cornerRadius,
            ),
            tabUnderline = TabUnderlineValues(
                indicatorColor = hex(config.tabUnderlineToken?.indicatorColor),
                activeTextColor = hex(config.tabUnderlineToken?.activeTextColor),
                inactiveTextColor = hex(config.tabUnderlineToken?.inactiveTextColor),
                backgroundColor = hex(config.tabUnderlineToken?.backgroundColor),
            ),
            discountBadge = DiscountBadgeValues(
                availableTextColor = hex(config.discountBadgeToken?.availableTextColor),
                unavailableTextColor = hex(config.discountBadgeToken?.unavailableTextColor),
                availableBackgroundColor = hex(config.discountBadgeToken?.availableBackgroundColor),
                unavailableBackgroundColor = hex(config.discountBadgeToken?.unavailableBackgroundColor),
                actionTextColor = hex(config.discountBadgeToken?.actionTextColor),
            ),
        )
    }

    private fun ButtonValues.merge(other: ButtonValues) = copy(
        backgroundColor = other.backgroundColor ?: backgroundColor,
        textColor = other.textColor ?: textColor,
        shadowColor = other.shadowColor ?: shadowColor,
        cornerRadius = other.cornerRadius ?: cornerRadius,
    )

    private fun SearchBarValues.merge(other: SearchBarValues) = copy(
        borderColor = other.borderColor ?: borderColor,
        hintTextColor = other.hintTextColor ?: hintTextColor,
        textColor = other.textColor ?: textColor,
        iconColor = other.iconColor ?: iconColor,
        cornerRadius = other.cornerRadius ?: cornerRadius,
    )

    private fun ListItemValues.merge(other: ListItemValues) = copy(
        linkTextColor = other.linkTextColor ?: linkTextColor,
        usedBadgeTextColor = other.usedBadgeTextColor ?: usedBadgeTextColor,
        usedBadgeBackgroundColor = other.usedBadgeBackgroundColor ?: usedBadgeBackgroundColor,
        radioButtonStrokeColor = other.radioButtonStrokeColor ?: radioButtonStrokeColor,
        radioButtonSelectedStrokeColor = other.radioButtonSelectedStrokeColor
            ?: radioButtonSelectedStrokeColor,
    )

    private fun TabChipValues.merge(other: TabChipValues) = copy(
        activeBackgroundColor = other.activeBackgroundColor ?: activeBackgroundColor,
        inactiveBackgroundColor = other.inactiveBackgroundColor ?: inactiveBackgroundColor,
        activeTextColor = other.activeTextColor ?: activeTextColor,
        inactiveTextColor = other.inactiveTextColor ?: inactiveTextColor,
        cornerRadius = other.cornerRadius ?: cornerRadius,
    )

    private fun TabUnderlineValues.merge(other: TabUnderlineValues) = copy(
        indicatorColor = other.indicatorColor ?: indicatorColor,
        activeTextColor = other.activeTextColor ?: activeTextColor,
        inactiveTextColor = other.inactiveTextColor ?: inactiveTextColor,
        backgroundColor = other.backgroundColor ?: backgroundColor,
    )

    private fun DiscountBadgeValues.merge(other: DiscountBadgeValues) = copy(
        availableTextColor = other.availableTextColor ?: availableTextColor,
        unavailableTextColor = other.unavailableTextColor ?: unavailableTextColor,
        availableBackgroundColor = other.availableBackgroundColor ?: availableBackgroundColor,
        unavailableBackgroundColor = other.unavailableBackgroundColor ?: unavailableBackgroundColor,
        actionTextColor = other.actionTextColor ?: actionTextColor,
    )
}

fun PRMThemeDisplay.ButtonValues.toToken(): PRMButtonToken = PRMButtonToken(
    backgroundColor = PRMTokenColorParser.parse(backgroundColor),
    textColor = PRMTokenColorParser.parse(textColor),
    shadowColor = PRMTokenColorParser.parse(shadowColor),
    cornerRadius = cornerRadius,
)

fun PRMThemeDisplay.SearchBarValues.toToken(): PRMSearchBarToken = PRMSearchBarToken(
    borderColor = PRMTokenColorParser.parse(borderColor),
    hintTextColor = PRMTokenColorParser.parse(hintTextColor),
    textColor = PRMTokenColorParser.parse(textColor),
    iconColor = PRMTokenColorParser.parse(iconColor),
    cornerRadius = cornerRadius,
)

fun PRMThemeDisplay.ListItemValues.toToken(): PRMListItemToken = PRMListItemToken(
    linkTextColor = PRMTokenColorParser.parse(linkTextColor),
    usedBadgeTextColor = PRMTokenColorParser.parse(usedBadgeTextColor),
    usedBadgeBackgroundColor = PRMTokenColorParser.parse(usedBadgeBackgroundColor),
    radioButtonStrokeColor = PRMTokenColorParser.parse(radioButtonStrokeColor),
    radioButtonSelectedStrokeColor = PRMTokenColorParser.parse(radioButtonSelectedStrokeColor),
)

fun PRMThemeDisplay.TabChipValues.toToken(): PRMTabChipToken = PRMTabChipToken(
    activeBackgroundColor = PRMTokenColorParser.parse(activeBackgroundColor),
    inactiveBackgroundColor = PRMTokenColorParser.parse(inactiveBackgroundColor),
    activeTextColor = PRMTokenColorParser.parse(activeTextColor),
    inactiveTextColor = PRMTokenColorParser.parse(inactiveTextColor),
    cornerRadius = cornerRadius,
)

fun PRMThemeDisplay.TabUnderlineValues.toToken(): PRMTabUnderlineToken = PRMTabUnderlineToken(
    indicatorColor = PRMTokenColorParser.parse(indicatorColor),
    activeTextColor = PRMTokenColorParser.parse(activeTextColor),
    inactiveTextColor = PRMTokenColorParser.parse(inactiveTextColor),
    backgroundColor = PRMTokenColorParser.parse(backgroundColor),
)

fun PRMThemeDisplay.DiscountBadgeValues.toToken(): PRMDiscountBadgeToken = PRMDiscountBadgeToken(
    availableTextColor = PRMTokenColorParser.parse(availableTextColor),
    unavailableTextColor = PRMTokenColorParser.parse(unavailableTextColor),
    availableBackgroundColor = PRMTokenColorParser.parse(availableBackgroundColor),
    unavailableBackgroundColor = PRMTokenColorParser.parse(unavailableBackgroundColor),
    actionTextColor = PRMTokenColorParser.parse(actionTextColor),
)
