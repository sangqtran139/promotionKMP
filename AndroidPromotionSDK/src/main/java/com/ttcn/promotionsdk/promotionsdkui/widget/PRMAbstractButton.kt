package com.ttcn.promotionsdk.promotionsdkui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.ttcn.promotionsdk.R

abstract class PRMAbstractButton : PRMCardView, IPRMClickEffect {

    /**
     * Holding the state of button [PRMButtonState]
     */
    var buttonState: PRMButtonState = PRMButtonState.IDLE
        set(value) {
            field = value
            enableClickEffect = when (value) {
                PRMButtonState.IDLE -> {
                    onIdle()
                    isEnabled = true
                    alpha = 1f
                    true
                }

                PRMButtonState.LOADING -> {
                    onLoading()
                    isEnabled = false
                    alpha = 1f
                    false
                }

                PRMButtonState.DISABLE -> {
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
                    PRMButtonState.values()[getInteger(R.styleable.PRMAbstractButton_prmAbtState, 0)]
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

enum class PRMButtonState { IDLE, LOADING, DISABLE }