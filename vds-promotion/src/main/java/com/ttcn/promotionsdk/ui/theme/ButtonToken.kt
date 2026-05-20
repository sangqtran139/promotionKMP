package com.ttcn.promotionsdk.ui.theme

import androidx.annotation.ColorInt

data class ButtonToken(
    @ColorInt val backgroundColor: Int? = null,
    @ColorInt val textColor: Int? = null,
    @ColorInt val shadowColor: Int? = null,
    /** Corner radius in **dp**. */
    val cornerRadius: Float? = null,
)
