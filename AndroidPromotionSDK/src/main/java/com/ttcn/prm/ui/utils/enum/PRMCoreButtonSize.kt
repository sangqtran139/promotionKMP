package com.ttcn.prm.ui.utils.enum
import androidx.annotation.DimenRes
import com.ttcn.prm.R

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
        R.dimen.prm_tokenSizing24,
        R.dimen.prm_tokenFontSize12,
        R.dimen.prm_tokenSpacing08,
        R.dimen.prm_tokenSpacing16
    ),
    MEDIUM(
        R.dimen.prm_tokenSizing32,
        R.dimen.prm_tokenFontSize14,
        R.dimen.prm_tokenSpacing16,
        R.dimen.prm_tokenSpacing24
    ),
    LARGE(
        R.dimen.prm_tokenSizing48,
        R.dimen.prm_tokenFontSize18,
        R.dimen.prm_tokenSpacing24,
        R.dimen.prm_tokenSpacing32
    )
}
