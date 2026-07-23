package com.ttcn.promotionsdk.promotionsdkui.utils.enum


import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import com.ttcn.promotionsdk.R

/**
 * @property backgroundRes button background drawable resource
 * @property textColorRes color resource of text
 * @property paddingRes surround padding, main purpose for shadow layer
 * @property loadingColorRes color resource of loading indicator
 */
enum class PRMCoreButtonType(
    @DrawableRes val backgroundRes: Int,
    @ColorRes val textColorRes: Int,
    @DimenRes var paddingRes: Int,
    @ColorRes val loadingColorRes: Int
) {
    PRIMARY(
        backgroundRes = R.drawable.prm_foundations_button_primary_normal,
        textColorRes = R.color.tokenWhite,
        paddingRes = R.dimen.tokenSpacing16,
        loadingColorRes = R.color.tokenWhite
    ),
    OUTLINE(
        backgroundRes = R.drawable.prm_foundations_button_outline_background,
        textColorRes = R.color.tokenRedTheme,
        paddingRes = 0,
        loadingColorRes = R.color.tokenRedTheme
    ),
    WHITE_SOLID(
        backgroundRes = R.drawable.prm_foundations_button_white_solid_background,
        textColorRes = R.color.tokenDark100,
        paddingRes = 0,
        loadingColorRes = R.color.tokenDark100
    ),
    TET(
        backgroundRes = R.drawable.prm_foundations_button_tet_background,
        textColorRes = R.color.tokenWhite,
        paddingRes = R.dimen.tokenSpacing16,
        loadingColorRes = R.color.tokenWhite
    ),
    WHITE_OUTLINE(
        backgroundRes = R.drawable.prm_foundations_button_white_outline_background,
        textColorRes = R.color.tokenWhite,
        paddingRes = 0,
        loadingColorRes = R.color.tokenWhite
    ),
    VER68(
        backgroundRes = R.drawable.prm_foundations_button_dark_solid_background,
        textColorRes = R.color.tokenWhite,
        paddingRes = R.dimen.tokenSpacing16,
        loadingColorRes = R.color.tokenWhite
    ),
    SLIDE(
        backgroundRes = R.drawable.prm_foundations_button_primary_normal,
        textColorRes = R.color.tokenWhite,
        paddingRes = R.dimen.tokenSpacing16,
        loadingColorRes = R.color.tokenWhite
    )
}