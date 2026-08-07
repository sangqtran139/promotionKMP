package com.ttcn.prm.ui.utils.enum


import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import com.ttcn.prm.R

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
        textColorRes = R.color.prm_tokenWhite,
        paddingRes = R.dimen.prm_tokenSpacing16,
        loadingColorRes = R.color.prm_tokenWhite
    ),
    OUTLINE(
        backgroundRes = R.drawable.prm_foundations_button_outline_background,
        textColorRes = R.color.prm_tokenRedTheme,
        paddingRes = 0,
        loadingColorRes = R.color.prm_tokenRedTheme
    ),
    WHITE_SOLID(
        backgroundRes = R.drawable.prm_foundations_button_white_solid_background,
        textColorRes = R.color.prm_tokenDark100,
        paddingRes = 0,
        loadingColorRes = R.color.prm_tokenDark100
    ),
    TET(
        backgroundRes = R.drawable.prm_foundations_button_tet_background,
        textColorRes = R.color.prm_tokenWhite,
        paddingRes = R.dimen.prm_tokenSpacing16,
        loadingColorRes = R.color.prm_tokenWhite
    ),
    WHITE_OUTLINE(
        backgroundRes = R.drawable.prm_foundations_button_white_outline_background,
        textColorRes = R.color.prm_tokenWhite,
        paddingRes = 0,
        loadingColorRes = R.color.prm_tokenWhite
    ),
    VER68(
        backgroundRes = R.drawable.prm_foundations_button_dark_solid_background,
        textColorRes = R.color.prm_tokenWhite,
        paddingRes = R.dimen.prm_tokenSpacing16,
        loadingColorRes = R.color.prm_tokenWhite
    ),
    SLIDE(
        backgroundRes = R.drawable.prm_foundations_button_primary_normal,
        textColorRes = R.color.prm_tokenWhite,
        paddingRes = R.dimen.prm_tokenSpacing16,
        loadingColorRes = R.color.prm_tokenWhite
    )
}