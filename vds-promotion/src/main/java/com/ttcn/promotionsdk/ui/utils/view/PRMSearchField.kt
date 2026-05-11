package com.ttcn.promotionsdk.ui.utils.view

import android.content.Context
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.databinding.ViewsSearchFieldBinding
import com.ttcn.promotionsdk.ui.utils.PRMViewsDataProvider.getInputTextWatcher
import com.ttcn.promotionsdk.ui.utils.enum.PRMSearchType
import com.ttcn.promotionsdk.ui.utils.extension.getText
import com.ttcn.promotionsdk.ui.utils.extension.replaceAccents
import com.ttcn.promotionsdk.ui.utils.extension.retrieveColor
import com.ttcn.promotionsdk.ui.utils.extension.setFont
import com.ttcn.promotionsdk.ui.utils.extension.showPRMSoftInput
import com.ttcn.promotionsdk.ui.utils.view.itf.IPRMInput

/**
 * Search field
 */
class PRMSearchField @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs), View.OnFocusChangeListener, IPRMInput {

    val viewBinding = ViewsSearchFieldBinding.inflate(LayoutInflater.from(context), this, true)

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
                    resources.getDimension(R.dimen.tokenSpacing24).toInt(), padding, 0, padding
                )
            }
        }

    var onTextChangeListener: ((String) -> Unit)? = null

    private val textWatcher by lazy { getInputTextWatcher() }

    private val textChangeListeners = mutableListOf<(String) -> Unit>()

    var allowVietnamese: Boolean = false

    init {
        viewBinding.searchInput.removeTextChangedListener(textWatcher)
        viewBinding.searchInput.addTextChangedListener(textWatcher)
        viewBinding.searchInput.onFocusChangeListener = this
        viewBinding.buttonClear.setOnClickListener { viewBinding.searchInput.setText("") }
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.PRMSearchField).apply {
                hint = getString(R.styleable.PRMSearchField_prmSfHint).getText()
                val type = getInteger(R.styleable.PRMSearchField_prmSvType, 0)
                searchType = PRMSearchType.values()[type % 3]
                if (type == 2) {
                    viewBinding.buttonSearch.visibility = VISIBLE
                }
                allowVietnamese = getBoolean(R.styleable.PRMSearchField_prmSfAllowVietnamese, false)
                recycle()
            }
        }
    }

    private fun updateInputType() {
        val currentText = viewBinding.searchInput.text.toString().trimStart()
        searchType.apply {
            if (currentText.isEmpty()) {
                viewBinding.buttonSearch.setTextColor(context.retrieveColor(R.color.color_222222))
                viewBinding.buttonClear.visibility = INVISIBLE
            } else {
                viewBinding.buttonClear.visibility = VISIBLE
                viewBinding.buttonSearch.setTextColor(context.retrieveColor(R.color.color_EE0033))
            }
            viewBinding.searchInput.apply {
                viewBinding.searchInput.setFont(fontStyle)
                setTextColor(context.retrieveColor(textColorRes))
                setHintTextColor(context.retrieveColor(hintColorRes))
                setBackgroundResource(if (hasFocus()) focusBackground else searchBackground)
            }
        }
    }

    override fun value(): String {
        return viewBinding.searchInput.toString().trimStart()
    }

    override fun getInputField(): PRMEditText {
        return viewBinding.searchInput
    }

    override fun onTextChanged(newString: String, oldString: String, isSelfUpdate: Boolean) {
        var phrase = ""
        if (isSelfUpdate) {
            phrase = updateContent(newString)
            if (phrase != newString) {
                val length = viewBinding.searchInput.text.toString().length
                viewBinding.searchInput.text?.replace(0, length, phrase)
            }
            if (phrase.isEmpty()) {
                viewBinding.buttonSearch.setTextColor(context.retrieveColor(R.color.color_222222))
                viewBinding.buttonClear.visibility = INVISIBLE
            } else {
                viewBinding.buttonSearch.setTextColor(context.retrieveColor(R.color.color_EE0033))
                viewBinding.buttonClear.visibility = VISIBLE
            }
            val currentSelection = viewBinding.searchInput.selectionStart
            val selection = getCursorPosition(currentSelection, phrase.length, oldString.length)
            viewBinding.searchInput.setSelection(selection)
            onTextChangeListener?.invoke(phrase)
        }
        super.onTextChanged(phrase, oldString, isSelfUpdate)
    }

    fun setOnSearchActionListener(listener: OnClickListener?) {
        viewBinding.buttonSearch.setOnClickListener(listener)
    }

    override fun textChangeListeners(): MutableList<(String) -> Unit> = textChangeListeners

    private fun updateContent(phrase: String): String {
        var text = phrase.trimStart().replace("\\s+".toRegex(), " ")
        if (maxLength > 0 && text.length > maxLength) {
            text = text.substring(0, maxLength)
        }
        return if (allowVietnamese) text else text.replaceAccents()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) {
            v.showPRMSoftInput()
        }
        updateInputType()
    }
}
