package com.ttcn.prm.ui.widget

import android.content.Context
import android.graphics.drawable.GradientDrawable
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
import com.ttcn.prm.R
import com.ttcn.prm.databinding.PrmViewsCoreButtonPrmBinding
import com.ttcn.prm.ui.utils.enum.PRMCoreButtonSize
import com.ttcn.prm.ui.utils.enum.PRMCoreButtonType
import com.ttcn.prm.ui.utils.enum.PRMShadowType
import com.ttcn.prm.ui.theme.token.ButtonToken
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.utils.applyTextColorIfSet
import com.ttcn.prm.ui.utils.extension.getString
import com.ttcn.prm.ui.utils.extension.getText
import com.ttcn.prm.ui.utils.extension.retrieveColor

/**
 * Main theme button with two type [PRMCoreButtonType.PRIMARY] and [PRMCoreButtonType.OUTLINE]
 * By default, [PRMCoreButtonType.PRIMARY] has margin of value [R.dimen.prm_tokenSpacing16] because of shadow layer
 */
class PRMButton : PRMAbstractButton {

    private var viewBinding: PrmViewsCoreButtonPrmBinding? = null
    private var lastAppliedToken: ButtonToken? = null

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
    var typeface: Typeface? = ResourcesCompat.getFont(context, R.font.prm_sf_pro_display_medium)
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
        viewBinding = PrmViewsCoreButtonPrmBinding.inflate(LayoutInflater.from(context), this, true)
        if (attrs == null) {
            updateSize()
            updateType()
            applyToken(PromotionThemeRegistry.buttonToken())
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
        applyToken(PromotionThemeRegistry.buttonToken())
    }

    fun applyToken(token: ButtonToken?) {
        lastAppliedToken = token
        applyTokenInternal(token)
    }

    private fun applyTokenInternal(token: ButtonToken?) {
        if (token == null) return
        token.backgroundColor?.let { color ->
            applyTokenBackground(color, token.cornerRadius)
        }
        token.textColor?.let { viewBinding?.buttonAction?.applyTextColorIfSet(it) }
        token.shadowColor?.let { setShadowColor(it) }
        token.cornerRadius?.let { applyTokenCornerRadius(it) }
        postInvalidate()
    }

    private fun applyTokenBackground(@androidx.annotation.ColorInt color: Int, cornerRadiusDp: Float?) {
        bgColor = color
        val radiusPx = cornerRadiusDp?.let { it * resources.displayMetrics.density }
        viewBinding?.buttonContainer?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            radiusPx?.let { cornerRadius = it }
        }
    }

    private fun applyTokenCornerRadius(radiusDp: Float) {
        val radiusPx = radiusDp * resources.displayMetrics.density
        setCorners(radiusPx, radiusPx, radiusPx, radiusPx, radiusPx)
        val containerBg = viewBinding?.buttonContainer?.background
        when (containerBg) {
            is GradientDrawable -> containerBg.cornerRadius = radiusPx
            null -> {
                tokenBackgroundColor()?.let { applyTokenBackground(it, radiusDp) }
            }
        }
    }

    private fun tokenBackgroundColor(): Int? = lastAppliedToken?.backgroundColor

    private fun reapplyTokenOverridesFromShadow() {
        val token = lastAppliedToken ?: return
        token.shadowColor?.let { setShadowColor(it) }
        token.cornerRadius?.let { applyTokenCornerRadius(it) }
        token.backgroundColor?.let { applyTokenBackground(it, token.cornerRadius) }
        token.textColor?.let { viewBinding?.buttonAction?.applyTextColorIfSet(it) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.buttonToken())
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
                        ContextCompat.getColor(context, R.color.prm_tokenRainbowRedStart),
                        ContextCompat.getColor(context, R.color.prm_tokenRainbowRedEnd)
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
            setShadowColor(context.retrieveColor(R.color.prm_ui_color_tet_2))
        }
        reapplyTokenOverridesFromShadow()
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
        // viewBinding and buttonType are null during super-constructor call — skip background update
        val binding = viewBinding ?: return
        val bgRes = if (!enabled && buttonType == PRMCoreButtonType.PRIMARY) {
            R.drawable.prm_bg_button_primary_disabled
        } else {
            buttonType.backgroundRes
        }
        binding.buttonContainer.setBackgroundResource(bgRes)
    }

    fun resetSlide() {
        viewBinding?.buttonSlide?.setCompleted(completed = false, withAnimation = true)
    }

    interface OnSlideListener {
        fun onSlideCompleteListener(view: PRMSlideButton)

        fun onSlideFailedListener(view: PRMSlideButton, isOutside: Boolean)
    }
}
