package com.ttcn.promotionsdk.ui.utils.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.ViewsCoreButtonPrmBinding
import com.ttcn.promotionsdk.ui.utils.enum.PRMCoreButtonSize
import com.ttcn.promotionsdk.ui.utils.enum.PRMCoreButtonType
import com.ttcn.promotionsdk.ui.utils.enum.PRMShadowType
import com.ttcn.promotionsdk.ui.utils.extension.getString
import com.ttcn.promotionsdk.ui.utils.extension.getText
import com.ttcn.promotionsdk.ui.utils.extension.retrieveColor

/**
 * Main theme button with two type [PRMCoreButtonType.PRIMARY] and [PRMCoreButtonType.OUTLINE]
 * By default, [PRMCoreButtonType.PRIMARY] has margin of value [R.dimen.tokenSpacing16] because of shadow layer
 */
class PRMButton : PRMAbstractButton {

    private var viewBinding: ViewsCoreButtonPrmBinding? = null

    var onSlideListener: OnSlideListener? = null

    @StringRes
    var textRes: Int = 0
        set(value) {
            field = value
            if (value != 0) {
                viewBinding?.buttonAction?.textRes = value
                viewBinding?.buttonSlide?.text = value.getString(context)
            }
        }

    @StringRes
    var prefixIconRes: Int = 0
        set(value) {
            field = value
            if (value != 0) {
                viewBinding?.imagePrefixIcon?.visibility = View.VISIBLE
                viewBinding?.imagePrefixIcon?.setImageResource(value)
                updateSize()
            }
        }

    /**
     * Text label of the button
     */
    var text: String = ""
        set(value) {
            field = value
            textRes = 0
            viewBinding?.buttonAction?.text = value
            viewBinding?.buttonSlide?.text = value
        }

    /**
     * Size of the button
     */
    private var coreButtonSize: PRMCoreButtonSize = PRMCoreButtonSize.SMALL
        set(value) {
            field = value
            updateSize()
        }

    /**
     * Type of the button
     */
    var buttonType: PRMCoreButtonType = PRMCoreButtonType.PRIMARY
        set(value) {
            field = value
            updateType()
        }

    /**
     * Type of the buttonText
     */
    var typeface: Typeface? = ResourcesCompat.getFont(context, R.font.sf_pro_display_medium)
        set(value) {
            field = value
            if (value != null) {
                viewBinding?.buttonAction?.typeface = value
            }
        }

    /**
     * Apply shadow padding for [PRMCoreButtonType.OUTLINE], [PRMCoreButtonType.WHITE_SOLID] and [PRMCoreButtonType.TET] like [PRMCoreButtonType.PRIMARY]
     */
    var withShadowPadding: Boolean = false

    /**
     * Type of the button
     */
    var paddingStartEnd: Int = 0
        set(value) {
            field = value
            updatePaddingButton()
        }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        viewBinding = ViewsCoreButtonPrmBinding.inflate(LayoutInflater.from(context), this, true)
        if (attrs == null) {
            return
        }
        context.obtainStyledAttributes(attrs, R.styleable.PRMButton).apply {
            val textValue = TypedValue()
            getValue(R.styleable.PRMButton_prmBtText, textValue)
            if (textValue.type == TypedValue.TYPE_REFERENCE) {
                textRes = getResourceId(R.styleable.PRMButton_prmBtText, 0)
            } else {
                text = getString(R.styleable.PRMButton_prmBtText).getText()
            }
            withShadowPadding = getBoolean(R.styleable.PRMButton_prmBtWithShadowPadding, false)
            coreButtonSize = PRMCoreButtonSize.values()[getInt(R.styleable.PRMButton_prmBtSize, 0)]
            buttonType = PRMCoreButtonType.values()[getInt(R.styleable.PRMButton_prmBtType, 0)]
            prefixIconRes = getResourceId(R.styleable.PRMButton_prmBtPrefixIcon, 0)
            if (prefixIconRes != 0) {
                viewBinding?.imagePrefixIcon?.visibility = View.VISIBLE
            } else {
                viewBinding?.imagePrefixIcon?.visibility = View.GONE
            }
            recycle()
        }
    }

    override fun onIdle() {
        viewBinding?.buttonAction?.visibility = View.VISIBLE
        viewBinding?.loadingIndicator?.visibility = View.INVISIBLE
    }

    override fun onLoading() {
        viewBinding?.buttonAction?.visibility = View.INVISIBLE
        viewBinding?.loadingIndicator?.visibility = View.VISIBLE
    }

    override fun onDisable() {

    }

    /**
     * Resize button based on [PRMCoreButtonSize]
     */
    private fun updateSize() {
        viewBinding?.buttonAction?.let {

            if (viewBinding != null && viewBinding!!.imagePrefixIcon != null) {
                if (viewBinding!!.imagePrefixIcon.visibility != View.VISIBLE) {
                    val padding = resources.getDimensionPixelSize(coreButtonSize.paddingRes)
                    it.setPadding(padding, 0, padding, 0)
                }
            } else {
                val padding = resources.getDimensionPixelSize(coreButtonSize.paddingRes)
                it.setPadding(padding, 0, padding, 0)
            }


            it.layoutParams = it.layoutParams.apply {
                height = resources.getDimensionPixelSize(coreButtonSize.heightRes)
            }
            it.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(coreButtonSize.textSizeRes)
            )
        }
        viewBinding?.buttonSlide?.let {
            it.layoutParams = it.layoutParams.apply {
                height = resources.getDimensionPixelSize(coreButtonSize.heightRes)
            }
        }
        viewBinding?.loadingIndicator?.let {
            it.layoutParams = viewBinding?.loadingIndicator?.layoutParams?.apply {
                val loadingSize = resources.getDimensionPixelSize(coreButtonSize.loadingSizeRes)
                width = loadingSize
                height = loadingSize
            }
        }
        updateShadow()
    }

    /**
     * Change styles and shadow based on [PRMCoreButtonType]
     */
    private fun updateType() {
        if (buttonType == PRMCoreButtonType.SLIDE) {
            viewBinding?.buttonSlide?.isAnimateCompletion = false
            viewBinding?.buttonSlide?.viewTreeObserver?.addOnPreDrawListener(object :
                ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewBinding?.buttonSlide?.viewTreeObserver?.removeOnPreDrawListener(this)
                    viewBinding?.buttonSlide?.setGradientOuter(
                        ContextCompat.getColor(context, R.color.tokenRainbowRedStart),
                        ContextCompat.getColor(context, R.color.tokenRainbowRedEnd)
                    )
                    return true
                }
            })
            viewBinding?.buttonSlide?.onSlideCompleteListener =
                object : PRMSlideButton.OnSlideCompleteListener {
                    override fun onSlideComplete(view: PRMSlideButton) {
                        onSlideListener?.onSlideCompleteListener(view)
                    }
                }
            viewBinding?.buttonSlide?.onSlideUserFailedListener =
                object : PRMSlideButton.OnSlideUserFailedListener {
                    override fun onSlideFailed(view: PRMSlideButton, isOutside: Boolean) {
                        onSlideListener?.onSlideFailedListener(view, isOutside)
                    }
                }
        }
        viewBinding?.buttonSlide?.isVisible = buttonType == PRMCoreButtonType.SLIDE
        viewBinding?.layoutButtonText?.isVisible = buttonType != PRMCoreButtonType.SLIDE
        viewBinding?.buttonContainer?.setBackgroundResource(buttonType.backgroundRes)
        viewBinding?.buttonAction?.setTextColor(context.retrieveColor(buttonType.textColorRes))
        viewBinding?.loadingIndicator?.setColorFilter(context.retrieveColor(buttonType.loadingColorRes))
        updateShadow()
    }

    private fun updateShadow() {
        val shadow =
            if (buttonType == PRMCoreButtonType.PRIMARY || buttonType == PRMCoreButtonType.TET || buttonType == PRMCoreButtonType.SLIDE) {
                when (coreButtonSize) {
                    PRMCoreButtonSize.SMALL -> PRMShadowType.TokenShadowsButtonSmall
                    PRMCoreButtonSize.MEDIUM -> PRMShadowType.TokenShadowsButtonMedium
                    PRMCoreButtonSize.LARGE -> PRMShadowType.TokenShadowsButtonLarge
                }
            } else {
                PRMShadowType.TokenNone
            }
        if (buttonType != PRMCoreButtonType.PRIMARY || buttonType != PRMCoreButtonType.SLIDE) {
            if (withShadowPadding) {
                when (coreButtonSize) {
                    PRMCoreButtonSize.SMALL -> PRMShadowType.TokenShadowsButtonSmall
                    PRMCoreButtonSize.MEDIUM -> PRMShadowType.TokenShadowsButtonMedium
                    PRMCoreButtonSize.LARGE -> PRMShadowType.TokenShadowsButtonLarge
                }.applyOnlyPadding(this)
            } else {
                shadow.apply(this)
            }
        } else {
            shadow.apply(this)
        }

        if (buttonType == PRMCoreButtonType.TET) {
            setShadowColor(context.retrieveColor(R.color.ui_color_tet_2))
        }
    }

    private fun updatePaddingButton() {
        viewBinding?.buttonAction?.updatePadding(paddingStartEnd, 0, paddingStartEnd, 0)
    }

//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        viewBinding?.loadingIndicator?.let { loadingIndicator ->
//            loadingAnimation = please(duration = 1500L, interpolator = LinearInterpolator()) {
//                animate(loadingIndicator) toBe {
//                    toBeRotated(360f)
//                    withEndAction {
//                        setPercent(0f)
//                        start()
//                    }
//                }
//            }
//            loadingAnimation?.start()
//        }
//    }

    override fun onDetachedFromWindow() {
//        loadingAnimation?.cancel()
        super.onDetachedFromWindow()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding?.buttonAction?.isEnabled = enabled
        viewBinding?.buttonSlide?.isEnabled = enabled
    }

    fun resetSlide() {
        viewBinding?.buttonSlide?.setCompleted(completed = false, withAnimation = true)
    }

    interface OnSlideListener {
        fun onSlideCompleteListener(view: PRMSlideButton)

        fun onSlideFailedListener(view: PRMSlideButton, isOutside: Boolean)
    }
}
