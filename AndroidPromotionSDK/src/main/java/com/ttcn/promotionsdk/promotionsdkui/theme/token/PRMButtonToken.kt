package com.ttcn.promotionsdk.promotionsdkui.theme.token

import androidx.annotation.ColorInt

data class PRMButtonToken(
    @ColorInt val backgroundColor: Int? = null,
    @ColorInt val textColor: Int? = null,
    @ColorInt val shadowColor: Int? = null,
    /** Corner radius in **dp**. */
    val cornerRadius: Float? = null,
)
