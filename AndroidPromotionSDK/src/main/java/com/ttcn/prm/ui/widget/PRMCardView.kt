package com.ttcn.prm.ui.widget

import android.content.Context
import android.util.AttributeSet
import com.ttcn.prm.R
import com.ttcn.prm.ui.utils.enum.PRMShadowType

open class PRMCardView @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet? = null, defStyleInt: Int = 0
) : PRMShadowView(context, attributeSet, defStyleInt) {

    var shadowType = PRMShadowType.TokenShadowsCard
        set(value) {
            field = value
            value.apply(this, shadowLeft, shadowTop, shadowRight, shadowBottom)
        }

    init {
        context.obtainStyledAttributes(attributeSet, R.styleable.PRMCardView).apply {
            shadowType =
                PRMShadowType.values()[getInteger(R.styleable.PRMCardView_prmShadowType, 1)]
            val cornerRadius = getDimension(R.styleable.PRMCardView_prmCvCornerRadius, -1f)
            if (cornerRadius >= 0) {
                shadowType.apply(
                    this@PRMCardView, cornerRadius, cornerRadius, cornerRadius, cornerRadius
                )
            } else {
                val topLeft = getDimension(R.styleable.PRMCardView_prmCvTopLeftRadius, -1f)
                val topRight = getDimension(R.styleable.PRMCardView_prmCvTopRightRadius, -1f)
                val bottomRight = getDimension(R.styleable.PRMCardView_prmCvBottomRightRadius, -1f)
                val bottomLeft = getDimension(R.styleable.PRMCardView_prmCvBottomLeftRadius, -1f)
                shadowType.apply(this@PRMCardView, topLeft, topRight, bottomRight, bottomLeft)
            }
            val shadowMargin = getDimension(R.styleable.PRMCardView_prmCvShadowMargin, -1f)
            if (shadowMargin >= 0) {
                shadowLeft = shadowMargin
                shadowTop = shadowMargin
                shadowRight = shadowMargin
                shadowBottom = shadowMargin
            } else {
                shadowLeft = getDimension(R.styleable.PRMCardView_prmCvShadowMarginLeft, -1f)
                shadowTop = getDimension(R.styleable.PRMCardView_prmCvShadowMarginTop, -1f)
                shadowRight = getDimension(R.styleable.PRMCardView_prmCvShadowMarginRight, -1f)
                shadowBottom = getDimension(R.styleable.PRMCardView_prmCvShadowMarginBottom, -1f)
            }
            recycle()
        }
    }
}