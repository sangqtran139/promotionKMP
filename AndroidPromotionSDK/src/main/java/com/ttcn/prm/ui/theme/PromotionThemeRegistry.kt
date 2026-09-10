package com.ttcn.prm.ui.theme

import com.ttcn.prm.ui.theme.token.PRMButtonToken
import com.ttcn.prm.ui.theme.token.PRMDiscountBadgeToken
import com.ttcn.prm.ui.theme.token.PRMListItemToken
import com.ttcn.prm.ui.theme.token.PRMSearchBarToken
import com.ttcn.prm.ui.theme.token.PRMTabChipToken
import com.ttcn.prm.ui.theme.token.PRMTabUnderlineToken

internal object PromotionThemeRegistry {

    @Volatile
    private var config: PromotionSDKTheme? = null

    fun configure(themeConfig: PromotionSDKTheme?) {
        config = themeConfig
    }

    fun currentConfig(): PromotionSDKTheme? = config

    fun buttonToken(): PRMButtonToken? = config?.buttonToken

    fun searchBarToken(): PRMSearchBarToken? = config?.searchBarToken

    fun listItemToken(): PRMListItemToken? = config?.listItemToken

    fun tabChipToken(): PRMTabChipToken? = config?.tabChipToken

    fun tabUnderlineToken(): PRMTabUnderlineToken? = config?.tabUnderlineToken

    fun discountBadgeToken(): PRMDiscountBadgeToken? = config?.discountBadgeToken
}
