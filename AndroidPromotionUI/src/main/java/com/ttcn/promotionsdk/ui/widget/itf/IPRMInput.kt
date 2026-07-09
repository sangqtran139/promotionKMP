package com.ttcn.promotionsdk.ui.widget.itf

import android.content.Context
import androidx.annotation.StringRes
import com.ttcn.promotionsdk.ui.utils.extension.getString
import com.ttcn.promotionsdk.ui.widget.PRMEditText

interface IPRMInput {

  /**
   * Retrieve EditText from current edit field
   */
  fun getInputField(): PRMEditText

  /**
   * Retrieve value from current edit field
   */
  fun value(): String

  fun onTextChanged(newString: String, oldString: String, isSelfUpdate: Boolean) {
    if ((newString != oldString || newString.length != oldString.length) && !"".equals(newString)) {
      showWarning("")
    }
  }

  /**
   * Show bottom warning
   * @param warning
   */
  fun showWarning(warning: String, isError: Boolean = false) {}
  fun showClose(isShow: Boolean ) {}

  /**
   * Show bottom warning
   * @param warningResourceId
   */
  fun showWarning(@StringRes warningResourceId: Int, context: Context, isError: Boolean = false) {
    showWarning(warningResourceId.getString(context), isError)
  }
  fun showWarningNew(warning: String, isError: Boolean = false) {}
  fun showWarningNew(@StringRes warningResourceId: Int, context: Context, isError: Boolean = false) {
    showWarningNew(warningResourceId.getString(context), isError)
  }
  /**
   * Retrieve new cursor position based on information
   * @param selectionStart current cursor position
   * @param phraseLength new text length
   * @param oldLength old text length
   */
  fun getCursorPosition(selectionStart: Int, phraseLength: Int, oldLength: Int): Int {
    var selection = selectionStart + phraseLength - oldLength
    if (phraseLength < oldLength) {
      selection += 1
    } else if (phraseLength > oldLength) {
      selection -= 1
    }
    if (selection < 0) {
      selection = 0
    } else if (selection > phraseLength) {
      selection = phraseLength
    }
    return selection
  }

  fun textChangeListeners() = mutableListOf<(String) -> Unit>()

  fun addTextChangeListener(onTextChanged: (String) -> Unit) {
    textChangeListeners().remove(onTextChanged)
    textChangeListeners().add(onTextChanged)
  }

  fun removeTextChangeListener(onTextChanged: (String) -> Unit) {
    textChangeListeners().remove(onTextChanged)
  }

  fun clearTextChangeListeners() = textChangeListeners().clear()
}