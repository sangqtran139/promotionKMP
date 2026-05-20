package com.ttcn.promotionsdk.ui.theme

import android.content.Context
import com.ttcn.promotionsdk.ui.utils.extension.TokenColorParser

/**
 * Hex-string display model for theme preview UI.
 * Converts to [PromotionThemeConfig] / token types for SDK apply.
 */
object PromotionThemeDisplay {

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

    fun load(context: Context): Defaults = fromConfig(PromotionThemeDefaults.defaultConfig(context), context)

    fun mergeWithSaved(context: Context, sdk: Defaults, saved: PromotionThemeConfig?): Defaults {
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

    fun configFromDisplayValues(display: Defaults, sdk: Defaults): PromotionThemeConfig =
        PromotionThemeConfig(
            buttonToken = display.button.toToken().takeIf { display.button != sdk.button },
            searchBarToken = display.searchBar.toToken().takeIf { display.searchBar != sdk.searchBar },
            listItemToken = display.listItem.toToken().takeIf { display.listItem != sdk.listItem },
            tabChipToken = display.tabChip.toToken().takeIf { display.tabChip != sdk.tabChip },
            tabUnderlineToken = display.tabUnderline.toToken().takeIf { display.tabUnderline != sdk.tabUnderline },
            discountBadgeToken = display.discountBadge.toToken().takeIf { display.discountBadge != sdk.discountBadge },
        )

    private fun fromConfig(config: PromotionThemeConfig, context: Context?): Defaults {
        fun hex(@androidx.annotation.ColorInt color: Int?) =
            color?.let { PromotionThemeDefaults.colorToHex(it) }

        fun radiusDp(px: Float?) =
            if (px != null && context != null) PromotionThemeDefaults.pxToDp(context, px) else px

        return Defaults(
            button = ButtonValues(
                backgroundColor = hex(config.buttonToken?.backgroundColor),
                textColor = hex(config.buttonToken?.textColor),
                shadowColor = hex(config.buttonToken?.shadowColor),
                cornerRadius = radiusDp(config.buttonToken?.cornerRadius),
            ),
            searchBar = SearchBarValues(
                borderColor = hex(config.searchBarToken?.borderColor),
                hintTextColor = hex(config.searchBarToken?.hintTextColor),
                textColor = hex(config.searchBarToken?.textColor),
                iconColor = hex(config.searchBarToken?.iconColor),
                cornerRadius = radiusDp(config.searchBarToken?.cornerRadius),
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
                cornerRadius = radiusDp(config.tabChipToken?.cornerRadius),
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
        radioButtonSelectedStrokeColor = other.radioButtonSelectedStrokeColor ?: radioButtonSelectedStrokeColor,
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

fun PromotionThemeDisplay.ButtonValues.toToken(): ButtonToken = ButtonToken(
    backgroundColor = TokenColorParser.parse(backgroundColor),
    textColor = TokenColorParser.parse(textColor),
    shadowColor = TokenColorParser.parse(shadowColor),
    cornerRadius = cornerRadius,
)

fun PromotionThemeDisplay.SearchBarValues.toToken(): SearchBarToken = SearchBarToken(
    borderColor = TokenColorParser.parse(borderColor),
    hintTextColor = TokenColorParser.parse(hintTextColor),
    textColor = TokenColorParser.parse(textColor),
    iconColor = TokenColorParser.parse(iconColor),
    cornerRadius = cornerRadius,
)

fun PromotionThemeDisplay.ListItemValues.toToken(): ListItemToken = ListItemToken(
    linkTextColor = TokenColorParser.parse(linkTextColor),
    usedBadgeTextColor = TokenColorParser.parse(usedBadgeTextColor),
    usedBadgeBackgroundColor = TokenColorParser.parse(usedBadgeBackgroundColor),
    radioButtonStrokeColor = TokenColorParser.parse(radioButtonStrokeColor),
    radioButtonSelectedStrokeColor = TokenColorParser.parse(radioButtonSelectedStrokeColor),
)

fun PromotionThemeDisplay.TabChipValues.toToken(): TabChipToken = TabChipToken(
    activeBackgroundColor = TokenColorParser.parse(activeBackgroundColor),
    inactiveBackgroundColor = TokenColorParser.parse(inactiveBackgroundColor),
    activeTextColor = TokenColorParser.parse(activeTextColor),
    inactiveTextColor = TokenColorParser.parse(inactiveTextColor),
    cornerRadius = cornerRadius,
)

fun PromotionThemeDisplay.TabUnderlineValues.toToken(): TabUnderlineToken = TabUnderlineToken(
    indicatorColor = TokenColorParser.parse(indicatorColor),
    activeTextColor = TokenColorParser.parse(activeTextColor),
    inactiveTextColor = TokenColorParser.parse(inactiveTextColor),
    backgroundColor = TokenColorParser.parse(backgroundColor),
)

fun PromotionThemeDisplay.DiscountBadgeValues.toToken(): DiscountBadgeToken = DiscountBadgeToken(
    availableTextColor = TokenColorParser.parse(availableTextColor),
    unavailableTextColor = TokenColorParser.parse(unavailableTextColor),
    availableBackgroundColor = TokenColorParser.parse(availableBackgroundColor),
    unavailableBackgroundColor = TokenColorParser.parse(unavailableBackgroundColor),
    actionTextColor = TokenColorParser.parse(actionTextColor),
)
