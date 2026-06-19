package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.ui.theme.token.ButtonToken
import com.ttcn.promotionsdk.ui.theme.token.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.token.ListItemToken
import com.ttcn.promotionsdk.ui.theme.token.SearchBarToken
import com.ttcn.promotionsdk.ui.theme.token.TabChipToken
import com.ttcn.promotionsdk.ui.theme.token.TabUnderlineToken

internal object PromotionThemeRegistry {

    @Volatile
    private var config: PromotionThemeConfig? = null

    fun configure(themeConfig: PromotionThemeConfig?) {
        config = themeConfig
    }

    fun currentConfig(): PromotionThemeConfig? = config

    fun buttonToken(): ButtonToken? = config?.buttonToken

    fun searchBarToken(): SearchBarToken? = config?.searchBarToken

    fun listItemToken(): ListItemToken? = config?.listItemToken

    fun tabChipToken(): TabChipToken? = config?.tabChipToken

    fun tabUnderlineToken(): TabUnderlineToken? = config?.tabUnderlineToken

    fun discountBadgeToken(): DiscountBadgeToken? = config?.discountBadgeToken
}
