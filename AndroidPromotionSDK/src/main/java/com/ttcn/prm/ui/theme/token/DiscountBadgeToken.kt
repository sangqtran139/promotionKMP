package com.ttcn.prm.ui.theme.token

import androidx.annotation.ColorInt

data class DiscountBadgeToken(
    @ColorInt val availableTextColor: Int? = null,
    @ColorInt val unavailableTextColor: Int? = null,
    @ColorInt val availableBackgroundColor: Int? = null,
    @ColorInt val unavailableBackgroundColor: Int? = null,
    @ColorInt val actionTextColor: Int? = null,
)
