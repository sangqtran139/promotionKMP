package com.ttcn.promotionsdk.ui.theme

import androidx.annotation.ColorInt

data class TabChipToken(
    @ColorInt val activeBackgroundColor: Int? = null,
    @ColorInt val inactiveBackgroundColor: Int? = null,
    @ColorInt val activeTextColor: Int? = null,
    @ColorInt val inactiveTextColor: Int? = null,
    /** Corner radius in **dp**. */
    val cornerRadius: Float? = null,
)
