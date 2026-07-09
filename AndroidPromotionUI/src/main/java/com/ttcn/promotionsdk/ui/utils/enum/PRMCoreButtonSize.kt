package com.ttcn.promotionsdk.ui.utils.enum
import androidx.annotation.DimenRes
import com.ttcn.promotionsdk.R

/**
 * Button sizes
 * @param heightRes button height dimens resource
 * @param textSizeRes button text size dimens resource
 * @param paddingRes button padding left and right dimens resources
 * @param loadingSizeRes loading size dimens resources
 */
enum class PRMCoreButtonSize(
    @DimenRes val heightRes: Int,
    @DimenRes val textSizeRes: Int,
    @DimenRes val paddingRes: Int,
    @DimenRes val loadingSizeRes: Int
) {
    SMALL(
        R.dimen.tokenSizing24,
        R.dimen.tokenFontSize12,
        R.dimen.tokenSpacing08,
        R.dimen.tokenSpacing16
    ),
    MEDIUM(
        R.dimen.tokenSizing32,
        R.dimen.tokenFontSize14,
        R.dimen.tokenSpacing16,
        R.dimen.tokenSpacing24
    ),
    LARGE(
        R.dimen.tokenSizing48,
        R.dimen.tokenFontSize18,
        R.dimen.tokenSpacing24,
        R.dimen.tokenSpacing32
    )
}
