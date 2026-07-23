package com.ttcn.prm.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import com.ttcn.prm.R

/**
 * Base EditText class for ViettelPay app to replace default [AppCompatEditText]
 *
 */
class PRMEditText : AppCompatEditText {

  var preventKeyboardDismiss = false

  var canEdit = true
    set(value) {
      field = value
      isFocusable = value
      isFocusableInTouchMode = value
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
    if (attrs == null) {
      return
    }
//    context.obtainStyledAttributes(attrs, R.styleable.PRMEditText).apply {
//      preventKeyboardDismiss = getBoolean(R.styleable.PRMEditText_prmEtPreventKeyboardDismiss, false)
//      recycle()
//    }
  }

  override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
//    if (preventKeyboardDismiss) {
//      return true
//    }
    return super.onKeyPreIme(keyCode, event)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
//    if (!canEdit) return false
    return super.onTouchEvent(event)
  }
}
