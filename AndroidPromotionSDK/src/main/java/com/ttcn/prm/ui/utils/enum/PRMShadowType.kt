package com.ttcn.prm.ui.utils.enum

import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import com.ttcn.prm.R
import com.ttcn.prm.ui.widget.PRMCardView
import com.ttcn.prm.ui.utils.extension.retrieveColor

internal enum class PRMShadowType(
    @ColorRes val colorRes: Int = 0,
    @DimenRes val yOffset: Int = 0,
    @DimenRes val blurRadius: Int = 0,
    @DimenRes val cornerRes: Int = 0
) {
    TokenNone,
    TokenShadowsCard(
        colorRes = R.color.prm_tokenShadowsCardColor,
        yOffset = R.dimen.prm_3sdp,
        blurRadius = R.dimen.prm_10sdp,
        cornerRes = R.dimen.prm_6sdp
    ),
    TokenShadowsBottomTab(
        colorRes = R.color.prm_tokenShadowsCardColor,
        yOffset = R.dimen.prm_minus3sdp,
        blurRadius = R.dimen.prm_10sdp
    ),
    TokenShadowsButtonLarge(
        colorRes = R.color.prm_tokenShadowsButtonColor,
        yOffset = R.dimen.prm_5sdp,
        blurRadius = R.dimen.prm_11sdp,
        cornerRes = R.dimen.prm_38sdp
    ),
    TokenShadowsButtonMedium(
        colorRes = R.color.prm_tokenShadowsButtonColor,
        yOffset = R.dimen.prm_3sdp,
        blurRadius = R.dimen.prm_10sdp,
        cornerRes = R.dimen.prm_26sdp
    ),
    TokenShadowsButtonSmall(
        colorRes = R.color.prm_tokenShadowsButtonColor,
        yOffset = R.dimen.prm_2sdp,
        blurRadius = R.dimen.prm_8sdp,
        cornerRes = R.dimen.prm_19sdp
    ),
    TokenShadowsVoucher(
        colorRes = R.color.prm_tokenShadowsVoucher,
        yOffset = R.dimen.prm_1sdp,
        blurRadius = R.dimen.prm_5sdp,
        cornerRes = R.dimen.prm_13sdp
    );

    fun apply(
        cardView: PRMCardView,
        topLeftRadius: Float = -1f,
        topRightRadius: Float = -1f,
        bottomRightRadius: Float = -1f,
        bottomLeftRadius: Float = -1f
    ) {
        if (this == TokenNone) {
            cardView.bgColor = Color.TRANSPARENT
            cardView.setShadowColor(Color.TRANSPARENT)
            cardView.shadowLeft = 0f
            cardView.shadowTop = 0f
            cardView.shadowRight = 0f
            cardView.shadowBottom = 0f
            cardView.shadowSpace = 0f
        } else {
            val dY = cardView.resources.getDimension(yOffset)
            cardView.shadowDy = dY
            val radius = cardView.resources.getDimension(blurRadius)
            cardView.setShadowColor(cardView.context.retrieveColor(colorRes))
            cardView.setShadowBlur(radius)
            cardView.shadowSpace = radius
            radius.let {
                cardView.shadowLeft = if (this == TokenShadowsBottomTab) 0f else it
                cardView.shadowTop = it - dY
                cardView.shadowRight = if (this == TokenShadowsBottomTab) 0f else it
                cardView.shadowBottom = if (this == TokenShadowsBottomTab) 0f else it + dY.toInt()
            }
            if (this == TokenShadowsBottomTab || this == TokenShadowsCard) {
                val corner = if (cornerRes == 0) 0f else cardView.resources.getDimension(cornerRes)
                cardView.setCorners(
                    if (topLeftRadius >= 0) topLeftRadius else corner,
                    if (topRightRadius >= 0) topRightRadius else corner,
                    if (bottomRightRadius >= 0) bottomRightRadius else corner,
                    if (bottomLeftRadius >= 0) bottomLeftRadius else corner
                )
            } else {
                val corner = cardView.resources.getDimension(cornerRes)
                cardView.setCorners(corner, corner, corner, corner)
            }
            if (this == TokenShadowsVoucher) {
                cardView.elevation = 0f
            }
        }
    }

    fun applyOnlyPadding(cardView: PRMCardView) {
        cardView.setShadowColor(Color.TRANSPARENT)
        val dY = cardView.resources.getDimension(yOffset)
        cardView.shadowDy = dY
        val radius = cardView.resources.getDimension(blurRadius)
        cardView.setShadowBlur(radius)
        cardView.shadowSpace = radius
        cardView.shadowLeft = radius
        cardView.shadowTop = radius - dY
        cardView.shadowRight = radius
        cardView.shadowBottom = radius + dY.toInt()
        val corner = cardView.resources.getDimension(cornerRes)
        cardView.setCorners(corner, corner, corner, corner)
    }
}