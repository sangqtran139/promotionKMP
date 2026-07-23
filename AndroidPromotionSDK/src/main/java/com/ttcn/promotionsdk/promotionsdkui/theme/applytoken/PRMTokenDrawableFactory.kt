package com.ttcn.promotionsdk.promotionsdkui.theme.applytoken

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt

object PRMTokenDrawableFactory {

    fun roundedRect(
        @ColorInt color: Int,
        cornerRadiusDp: Float,
        context: Context,
    ): GradientDrawable {
        val radiusPx = cornerRadiusDp * context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radiusPx
        }
    }
}
