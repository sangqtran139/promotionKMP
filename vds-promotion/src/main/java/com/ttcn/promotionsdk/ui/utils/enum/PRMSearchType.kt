package com.ttcn.promotionsdk.ui.utils.enum

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.ttcn.promotionsdk.R

enum class PRMSearchType(
  @DimenRes val iconSizeRes: Int,
  @DrawableRes val iconRes: Int,
  @DrawableRes val searchBackground: Int,
  @DrawableRes val focusBackground: Int,
  @StyleRes val fontStyle: Int,
  @ColorRes val hintColorRes: Int,
  @ColorRes val textColorRes: Int
) {
  BASIC(
    iconSizeRes = R.dimen.text_size_14,
    iconRes = R.drawable.prm_foundations_icon_search_dark,
    searchBackground = R.drawable.prm_views_search_field_basic_background,
    focusBackground = R.drawable.prm_views_search_field_basic_focus_background,
    fontStyle = R.style.fontPRMRegular16,
    hintColorRes = R.color.color_gray_200,
    textColorRes = R.color.color_222222
  ),
  NAVIGATION(
    iconSizeRes = R.dimen.text_size_14,
    iconRes = R.drawable.prm_foundations_icon_search_white,
    searchBackground = R.drawable.prm_views_search_field_navigation_background,
    focusBackground = R.drawable.prm_views_search_field_navigation_background,
    fontStyle = R.style.fontPRMRegular16,
    hintColorRes = R.color.color_gray_200,
    textColorRes = R.color.color_222222
  ),
  BASIC_TRANSPARENT(
    iconSizeRes = R.dimen.text_size_14,
    iconRes = R.drawable.prm_foundations_icon_search_white,
    searchBackground = R.drawable.prm_views_search_field_basic_background_opacity,
    focusBackground = R.drawable.prm_views_search_field_basic_focus_background_opacity,
    fontStyle = R.style.fontPRMRegular16,
    hintColorRes = R.color.color_gray_200,
    textColorRes = R.color.color_222222
  ),
}
