package com.ttcn.promotionsdk.promotionsdkui.theme

import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMButtonToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMDiscountBadgeToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMListItemToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMSearchBarToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabChipToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabUnderlineToken

internal object PromotionThemeRegistry {

    @Volatile
    private var config: PRMSDKTheme? = null

    fun configure(themeConfig: PRMSDKTheme?) {
        config = themeConfig
    }

    fun currentConfig(): PRMSDKTheme? = config

    fun buttonToken(): PRMButtonToken? = config?.buttonToken

    fun searchBarToken(): PRMSearchBarToken? = config?.searchBarToken

    fun listItemToken(): PRMListItemToken? = config?.listItemToken

    fun tabChipToken(): PRMTabChipToken? = config?.tabChipToken

    fun tabUnderlineToken(): PRMTabUnderlineToken? = config?.tabUnderlineToken

    fun discountBadgeToken(): PRMDiscountBadgeToken? = config?.discountBadgeToken
}
