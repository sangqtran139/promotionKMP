package com.ttcn.promotionsdk.ui.utils.enum

import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.ui.widget.PRMCardView
import com.ttcn.promotionsdk.ui.utils.extension.retrieveColor

enum class PRMShadowType(
    @ColorRes val colorRes: Int = 0,
    @DimenRes val yOffset: Int = 0,
    @DimenRes val blurRadius: Int = 0,
    @DimenRes val cornerRes: Int = 0
) {
    TokenNone,
    TokenShadowsCard(
        colorRes = R.color.tokenShadowsCardColor,
        yOffset = com.intuit.sdp.R.dimen._3sdp,
        blurRadius = com.intuit.sdp.R.dimen._10sdp,
        cornerRes = com.intuit.sdp.R.dimen._6sdp
    ),
    TokenShadowsBottomTab(
        colorRes = R.color.tokenShadowsCardColor,
        yOffset = com.intuit.sdp.R.dimen._minus3sdp,
        blurRadius = com.intuit.sdp.R.dimen._10sdp
    ),
    TokenShadowsButtonLarge(
        colorRes = R.color.tokenShadowsButtonColor,
        yOffset = com.intuit.sdp.R.dimen._5sdp,
        blurRadius = com.intuit.sdp.R.dimen._11sdp,
        cornerRes = com.intuit.sdp.R.dimen._38sdp
    ),
    TokenShadowsButtonMedium(
        colorRes = R.color.tokenShadowsButtonColor,
        yOffset = com.intuit.sdp.R.dimen._3sdp,
        blurRadius = com.intuit.sdp.R.dimen._10sdp,
        cornerRes = com.intuit.sdp.R.dimen._26sdp
    ),
    TokenShadowsButtonSmall(
        colorRes = R.color.tokenShadowsButtonColor,
        yOffset = com.intuit.sdp.R.dimen._2sdp,
        blurRadius = com.intuit.sdp.R.dimen._8sdp,
        cornerRes = com.intuit.sdp.R.dimen._19sdp
    ),
    TokenShadowsVoucher(
        colorRes = R.color.tokenShadowsVoucher,
        yOffset = com.intuit.sdp.R.dimen._1sdp,
        blurRadius = com.intuit.sdp.R.dimen._5sdp,
        cornerRes = com.intuit.sdp.R.dimen._13sdp
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