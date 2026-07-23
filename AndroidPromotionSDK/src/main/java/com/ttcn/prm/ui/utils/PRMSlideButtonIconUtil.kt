package com.ttcn.prm.ui.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import com.ttcn.prm.ui.widget.PRMSlideButton

internal object PRMSlideButtonIconUtil {

    @SuppressLint("UseCompatLoadingForDrawables")
    internal fun loadIconCompat(context: Context, value: Int): Drawable {
        return context.resources.getDrawable(value, context.theme)
    }

    internal fun tintIconCompat(icon: Drawable, color: Int) {
        icon.setTint(color)
    }

    internal fun startIconAnimation(icon: Drawable) {
        (icon as? AnimatedVectorDrawable)?.start()
    }

    internal fun stopIconAnimation(icon: Drawable) {
        (icon as? AnimatedVectorDrawable)?.stop()
    }

    /**
     * Creates a [ValueAnimator] to animate the icon.
     * Fades if not AVD, otherwise starts the AVD animation once.
     */
    fun createIconAnimator(
        view: PRMSlideButton,
        icon: Drawable,
        listener: ValueAnimator.AnimatorUpdateListener,
    ): ValueAnimator {
        return if (icon !is AnimatedVectorDrawable) {
            // Fallback: fade animation
            ValueAnimator.ofInt(0, 255).apply {
                addUpdateListener(listener)
                addUpdateListener {
                    icon.alpha = it.animatedValue as Int
                    view.invalidate()
                }
            }
        } else {
            // AVD animation
            var startedOnce = false
            ValueAnimator.ofInt(0).apply {
                addUpdateListener(listener)
                addUpdateListener {
                    if (!startedOnce) {
                        startIconAnimation(icon)
                        view.invalidate()
                        startedOnce = true
                    }
                }
            }
        }
    }
}