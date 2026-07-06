package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.ui.theme.token.ButtonToken
import com.ttcn.promotionsdk.ui.theme.token.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.token.ListItemToken
import com.ttcn.promotionsdk.ui.theme.token.SearchBarToken
import com.ttcn.promotionsdk.ui.theme.token.TabChipToken
import com.ttcn.promotionsdk.ui.theme.token.TabUnderlineToken

data class PromotionSDKTheme(
    val buttonToken: ButtonToken? = null,
    val searchBarToken: SearchBarToken? = null,
    val listItemToken: ListItemToken? = null,
    val tabChipToken: TabChipToken? = null,
    val tabUnderlineToken: TabUnderlineToken? = null,
    val discountBadgeToken: DiscountBadgeToken? = null,
) {
    companion object {
        fun from(config: PromotionThemeConfig) = PromotionSDKTheme(
            buttonToken = config.buttonToken,
            searchBarToken = config.searchBarToken,
            listItemToken = config.listItemToken,
            tabChipToken = config.tabChipToken,
            tabUnderlineToken = config.tabUnderlineToken,
            discountBadgeToken = config.discountBadgeToken,
        )
    }
}

internal fun PromotionSDKTheme.toThemeConfig() = PromotionThemeConfig(
    buttonToken = buttonToken,
    searchBarToken = searchBarToken,
    listItemToken = listItemToken,
    tabChipToken = tabChipToken,
    tabUnderlineToken = tabUnderlineToken,
    discountBadgeToken = discountBadgeToken,
)
