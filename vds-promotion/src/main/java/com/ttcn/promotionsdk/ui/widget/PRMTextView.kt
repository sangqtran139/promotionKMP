package com.ttcn.promotionsdk.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.ui.utils.PRMClick
import com.ttcn.promotionsdk.ui.utils.extension.getString

/**
 * Base TextView class for ViettelPay app to replace default [AppCompatTextView]
 */
@Suppress("LeakingThis")
open class PRMTextView : AppCompatTextView, IPRMClickEffect {

    /**
     * Should we prevent clicking too fast
     */
    private var preventFastClick = false

    @StringRes
    var textRes = 0
        set(value) {
            field = value
            if (value != 0) {
                text = value.getString(context)
            }
        }

    override var clickEffectViews: View? = this
    override var enableClickEffect: Boolean = false

    var enableAutoScroll = false
        set(value) {
            field = value
            if (value) {
                setSingleLine()
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
            }
        }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(attrs)
    }

    @SuppressLint("CustomViewStyleable")
    private fun init(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }
        context.obtainStyledAttributes(attrs, R.styleable.PRMTextView).apply {
            textRes = getResourceId(R.styleable.PRMTextView_prmTvText, 0)
            preventFastClick = getBoolean(R.styleable.PRMTextView_prmTvPreventFastClick, false)
            enableClickEffect = getBoolean(R.styleable.PRMTextView_prmTvEnableClickEffect, false)
            enableAutoScroll = getBoolean(R.styleable.PRMTextView_prmTvEnableAutoScroll, false)
            applyClickEffect()
            recycle()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener {
            if (!preventFastClick || PRMClick.check()) {
                l?.onClick(it)
            }
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (enableAutoScroll) {
            if (focused) {
                super.onFocusChanged(focused, direction, previouslyFocusedRect)
            }
        } else {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (enableAutoScroll) {
            if (hasWindowFocus) {
                super.onWindowFocusChanged(hasWindowFocus)
            }
        } else {
            super.onWindowFocusChanged(hasWindowFocus)
        }
    }

    override fun isFocused(): Boolean {
        return if (enableAutoScroll) {
            true
        } else {
            super.isFocused()
        }
    }
}
