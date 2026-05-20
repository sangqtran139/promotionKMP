package com.ttcn.promotionsdk.ui.theme

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
