package com.ttcn.prm.ui.widget

import android.content.Context
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.widget.addTextChangedListener
import com.ttcn.prm.R
import com.ttcn.prm.databinding.PrmViewsSearchFieldBinding
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.theme.token.PRMSearchBarToken
import com.ttcn.prm.ui.utils.applyCornerRadiusDp
import com.ttcn.prm.ui.utils.applyImageTintIfSet
import com.ttcn.prm.ui.utils.applyStrokeColorIfSet
import com.ttcn.prm.ui.utils.extension.hideSoftInput
import com.ttcn.prm.ui.utils.extension.retrieveColor
import com.ttcn.prm.ui.utils.extension.setFont
import com.ttcn.prm.ui.utils.extension.showPRMSoftInput
import com.ttcn.prm.ui.widget.itf.IPRMInput

/**
 * Search field
 */
internal class PRMSearchField @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs), View.OnFocusChangeListener, IPRMInput {

    val viewBinding = PrmViewsSearchFieldBinding.inflate(LayoutInflater.from(context), this, true)

    private var onFocusListener: ((Boolean) -> Unit)? = null

    fun setOnFocusListener(listener: (Boolean) -> Unit) {
        onFocusListener = listener
    }

    var hint: String = ""
        set(value) {
            field = value
            viewBinding.searchInput.hint = value
        }

    var searchType: PRMSearchType = PRMSearchType.BASIC
        set(value) {
            field = value
            updateInputType()
        }

    @ColorRes
    var hintColor: Int = 0
        set(value) {
            field = value
            if (hintColor != 0) {
                viewBinding.searchInput.setHintTextColor(context.retrieveColor(value))
            }
        }

    var maxLength: Int = 0
        set(value) {
            field = value
            if (maxLength != 0) {
                viewBinding.searchInput.filters = arrayOf<InputFilter>(LengthFilter(maxLength))
            }
        }

    var padding: Int = 0
        set(value) {
            field = value
            if (padding != 0) {
                viewBinding.searchInput.setPadding(
                    resources.getDimension(R.dimen.prm_30sdp).toInt(),
                    padding,
                    0,
                    padding
                )
            }
        }

    var onTextChangeListener: ((String) -> Unit)? = null


    private val textChangeListeners = mutableListOf<(String) -> Unit>()

    private var previousKeyword: String = ""

    private var lastAppliedToken: PRMSearchBarToken? = null

    init {
        viewBinding.searchInput.onFocusChangeListener = this
        viewBinding.buttonClear.setOnClickListener { viewBinding.searchInput.setText("") }
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.PRMSearchField).apply {
                hint = getString(R.styleable.PRMSearchField_prmSfHint).toString()
                val type = getInteger(R.styleable.PRMSearchField_prmSvType, 0)
                searchType = PRMSearchType.values()[type % 3]
                if (type == 2) {
                    viewBinding.buttonSearch.visibility = View.VISIBLE
                }
                recycle()
            }
        }
        viewBinding.searchInput.addTextChangedListener {
            if (it.isNullOrBlank()) {
                viewBinding.buttonClear.visibility = INVISIBLE
            } else {
                viewBinding.buttonClear.visibility = VISIBLE
            }

            val newKeyword = if (it.isNullOrBlank()) "" else it.toString().trim()

            if (previousKeyword == newKeyword) {
                return@addTextChangedListener
            }

            previousKeyword = newKeyword
            onTextChangeListener?.invoke(newKeyword)
        }
        applyToken(PromotionThemeRegistry.searchBarToken())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.searchBarToken())
    }

    fun applyToken(token: PRMSearchBarToken?) {
        lastAppliedToken = token
        applyTokenInternal(token)
    }

    private fun applyTokenInternal(token: PRMSearchBarToken?) {
        if (token == null) return
        token.borderColor?.let { viewBinding.searchInput.applyStrokeColorIfSet(it) }
        token.hintTextColor?.let { viewBinding.searchInput.setHintTextColor(it) }
        token.textColor?.let { viewBinding.searchInput.setTextColor(it) }
        token.iconColor?.let { color ->
            viewBinding.searchIcon.applyImageTintIfSet(color)
            viewBinding.buttonClear.applyImageTintIfSet(color)
            viewBinding.buttonSearch.applyImageTintIfSet(color)
        }
        token.cornerRadius?.let { viewBinding.searchInput.applyCornerRadiusDp(it) }
    }

    private fun updateInputType() {
        val currentText = viewBinding.searchInput.text.toString().trimStart()
        searchType.apply {
            viewBinding.searchIcon.setImageResource(iconRes)
            val iconSize = resources.getDimension(iconSizeRes).toInt()
            val params = viewBinding.searchIcon.layoutParams
            params.width = iconSize
            params.height = iconSize
            viewBinding.searchIcon.layoutParams = params
            if (currentText.isEmpty()) {
                viewBinding.buttonClear.visibility = View.INVISIBLE
            } else {
                viewBinding.buttonClear.visibility = View.VISIBLE
            }
            viewBinding.searchInput.apply {
                viewBinding.searchInput.setFont(fontStyle)
                setTextColor(context.retrieveColor(textColorRes))
                setHintTextColor(context.retrieveColor(hintColorRes))
                setBackgroundResource(if (hasFocus()) focusBackground else searchBackground)
                if (searchType == PRMSearchType.NAVIGATION) {
                    val currentBackground = background
                    currentBackground.alpha = resources.getInteger(R.integer.prm_tokenOpacity08)
                    background = currentBackground
                }
                val leftPadding =
                    iconSize + 2 * resources.getDimension(R.dimen.prm_6sdp).toInt()
                val rightPadding = viewBinding.buttonClear.layoutParams.width
                setPadding(leftPadding, paddingTop, rightPadding, paddingBottom)
            }
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.searchBarToken())
    }

    override fun value(): String {
        return viewBinding.searchInput.toString().trimStart()
    }

    override fun getInputField(): PRMEditText {
        return viewBinding.searchInput
    }


    fun setOnSearchActionListener(listener: OnClickListener?) {
        viewBinding.buttonSearch.setOnClickListener(listener)
    }

    fun setOnDoneKeyboardListener(listener: OnClickListener?) {
        viewBinding.searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideSoftInput()
                listener?.onClick(this)
                true
            }
            false
        }
    }

    override fun textChangeListeners(): MutableList<(String) -> Unit> = textChangeListeners

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) {
            v.showPRMSoftInput()
        }
        updateInputType()

        onFocusListener?.invoke(hasFocus)
    }
}
