package com.ttcn.promotionsdk.promotionsdkui.widget

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.annotation.FloatRange

internal interface IPRMClickEffect {

    var clickEffectViews: View?
    var enableClickEffect: Boolean

    @SuppressLint("ClickableViewAccessibility")
    fun applyClickEffect() {
        clickEffectViews?.applyClickEffect(enableClickEffect, getClickAlpha())
    }

    fun getClickAlpha() = 0.5f
}

@SuppressLint("ClickableViewAccessibility")
fun View.applyClickEffect(
    isEnable: Boolean = true,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.5f
) {
    setOnTouchListener { v, event ->
        if (!v.isEnabled) return@setOnTouchListener false
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isEnable) {
                    v.alpha = alpha
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                v.alpha = 1f
            }
        }
        false
    }
}
