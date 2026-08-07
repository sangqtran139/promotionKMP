package com.ttcn.prm.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.ttcn.prm.R
import com.ttcn.prm.ui.utils.PRMClick

/**
 * Custom class to handle everything with image view
 */
internal class PRMImageView : AppCompatImageView, IPRMClickEffect {

    /**
     * Should we prevent clicking too fast
     */
    private var preventFastClick = false

    override var clickEffectViews: View? = this
    override var enableClickEffect: Boolean = false

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(attrs)
    }

    @SuppressLint("CustomViewStyleable")
    private fun init(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }
        context.obtainStyledAttributes(attrs, R.styleable.PRMImageView).apply {
            enableClickEffect = getBoolean(R.styleable.PRMImageView_prmIvEnableClickEffect, false)
            preventFastClick = getBoolean(R.styleable.PRMImageView_prmIvPreventFastClick, false)
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
}
