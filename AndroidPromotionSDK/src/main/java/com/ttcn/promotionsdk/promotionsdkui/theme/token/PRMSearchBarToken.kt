package com.ttcn.promotionsdk.promotionsdkui.theme.token

import androidx.annotation.ColorInt

data class PRMSearchBarToken(
    @ColorInt val borderColor: Int? = null,
    @ColorInt val hintTextColor: Int? = null,
    @ColorInt val textColor: Int? = null,
    @ColorInt val iconColor: Int? = null,
    /** Corner radius in **dp** (same unit as [PRMButtonToken.cornerRadius]). */
    val cornerRadius: Float? = null,
)
