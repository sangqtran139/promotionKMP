package com.ttcn.promotionsdk.ui.utils

import android.text.Editable
import android.text.TextWatcher
import com.ttcn.promotionsdk.ui.widget.itf.IPRMInput

internal object PRMViewsDataProvider {

  internal fun IPRMInput.getInputTextWatcher(): TextWatcher {
    return object : TextWatcher {

      /** Prevent [afterTextChanged] trigger when we update text content */
      private var isSelfUpdate = false
      private var oldString = ""

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        if (!isSelfUpdate) {
          oldString = s.toString()
        }
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      }

      override fun afterTextChanged(s: Editable?) {
        val newString = s.toString()
        textChangeListeners().forEach { it(newString) }
        onTextChanged(newString, oldString, false)
        if (isSelfUpdate) {
          return
        }
        isSelfUpdate = true
        onTextChanged(newString, oldString, true)
        isSelfUpdate = false
      }
    }
  }
}
