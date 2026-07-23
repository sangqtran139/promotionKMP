package com.ttcn.prm.ui.utils

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.ttcn.prm.R

fun TextView.applyTextColorIfSet(@ColorInt color: Int?) {
    color?.let { setTextColor(it) }
}

fun View.applyBackgroundColorIfSet(@ColorInt color: Int?) {
    color?.let {
        val drawable = (background as? GradientDrawable)?.mutate() as? GradientDrawable
            ?: GradientDrawable().also { bg -> background = bg }
        drawable.setColor(it)
    }
}

/** Tints a vector/shape background while keeping its original silhouette (e.g. voucher badge). */
fun View.applyDrawableBackgroundTintIfSet(
    @ColorInt color: Int?,
    @DrawableRes drawableRes: Int,
) {
    color ?: return
    val drawable = ContextCompat.getDrawable(context, drawableRes)?.mutate() ?: return
    DrawableCompat.setTint(drawable, color)
    background = drawable
}

fun View.applyCornerRadiusDp(radiusDp: Float?) {
    radiusDp ?: return
    val radiusPx = radiusDp * resources.displayMetrics.density
    when (val bg = background) {
        is GradientDrawable -> {
            bg.cornerRadius = radiusPx
        }
        is LayerDrawable -> {
            for (i in 0 until bg.numberOfLayers) {
                (bg.getDrawable(i) as? GradientDrawable)?.cornerRadius = radiusPx
            }
        }
        else -> {
            background = GradientDrawable().apply {
                cornerRadius = radiusPx
                setColor(0x00000000)
            }
        }
    }
}

fun ImageView.applyImageTintIfSet(@ColorInt color: Int?) {
    color?.let { tint ->
        drawable?.let { d ->
            val wrapped = DrawableCompat.wrap(d.mutate())
            DrawableCompat.setTint(wrapped, tint)
            setImageDrawable(wrapped)
        }
    }
}

fun View.applyStrokeColorIfSet(@ColorInt strokeColor: Int?, strokeWidthDp: Float = 1f) {
    strokeColor ?: return
    val strokeWidthPx = (strokeWidthDp * resources.displayMetrics.density).toInt()
    val drawable = (background as? GradientDrawable)?.mutate() as? GradientDrawable
        ?: GradientDrawable().also { background = it }
    drawable.setStroke(strokeWidthPx, strokeColor)
}

/**
 * Applies list-item checkbox tokens while preserving [R.drawable.prm_bg_checkbox_use_voucher] layout:
 * checked = oval fill + white tick; unchecked = oval stroke only.
 */
fun AppCompatRadioButton.applyRadioStrokeColors(
    @ColorInt unselectedStroke: Int?,
    @ColorInt selectedFill: Int?,
) {
    if (unselectedStroke == null && selectedFill == null) return

    val ctx = context
    val defaultSelectedFill = ContextCompat.getColor(ctx, R.color.color_EE0033)
    val defaultUnselectedStroke = ContextCompat.getColor(ctx, R.color.tokenDark40)
    val checkedFill = selectedFill ?: defaultSelectedFill
    val uncheckedStroke = unselectedStroke ?: defaultUnselectedStroke
    val strokeWidthPx = (1f * resources.displayMetrics.density).toInt().coerceAtLeast(1)

    val tickDrawable = ContextCompat.getDrawable(ctx, R.drawable.prm_ic_tick_white)?.mutate()
    val tickWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
    val tickHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)

    val checkedBackground = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(checkedFill)
    }
    val checkedLayer = if (tickDrawable != null) {
        LayerDrawable(arrayOf(checkedBackground, tickDrawable)).apply {
            setLayerGravity(1, Gravity.CENTER)
            setLayerWidth(1, tickWidth)
            setLayerHeight(1, tickHeight)
        }
    } else {
        checkedBackground
    }

    val uncheckedBackground = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setStroke(strokeWidthPx, uncheckedStroke)
    }

    background = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_checked), checkedLayer)
        addState(intArrayOf(), uncheckedBackground)
    }
}
