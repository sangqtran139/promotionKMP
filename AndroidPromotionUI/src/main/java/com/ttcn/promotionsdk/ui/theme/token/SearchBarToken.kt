package com.ttcn.promotionsdk.ui.theme.token

import androidx.annotation.ColorInt

data class SearchBarToken(
    @ColorInt val borderColor: Int? = null,
    @ColorInt val hintTextColor: Int? = null,
    @ColorInt val textColor: Int? = null,
    @ColorInt val iconColor: Int? = null,
    /** Corner radius in **dp** (same unit as [ButtonToken.cornerRadius]). */
    val cornerRadius: Float? = null,
)
