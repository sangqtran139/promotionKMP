package com.ttcn.promotionsdk.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.ttcn.promotionsdk.R

abstract class PRMAbstractButton : PRMCardView, IPRMClickEffect {

    /**
     * Holding the state of button [ButtonState]
     */
    var buttonState: ButtonState = ButtonState.IDLE
        set(value) {
            field = value
            enableClickEffect = when (value) {
                ButtonState.IDLE -> {
                    onIdle()
                    isEnabled = true
                    alpha = 1f
                    true
                }

                ButtonState.LOADING -> {
                    onLoading()
                    isEnabled = false
                    alpha = 1f
                    false
                }

                ButtonState.DISABLE -> {
                    onDisable()
                    alpha = getClickAlpha()
                    isEnabled = false
                    false
                }
            }
        }

    override var clickEffectViews: View? = null
    override var enableClickEffect: Boolean = true

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        clickEffectViews = this
        applyClickEffect()
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.PRMAbstractButton).apply {
                buttonState =
                    ButtonState.values()[getInteger(R.styleable.PRMAbstractButton_prmAbtState, 0)]
                recycle()
            }
        }
    }

    open fun onIdle() {
    }

    open fun onLoading() {
    }

    open fun onDisable() {
    }
}

enum class ButtonState { IDLE, LOADING, DISABLE }