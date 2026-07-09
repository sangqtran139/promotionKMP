package com.ttcn.promotionsdk.ui.utils.extension

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.app.Activity
import android.graphics.Rect
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Show the soft input from view
 */
fun View.showPRMSoftInput() {
    showSoftInput(0)
}

private const val TAG_ON_GLOBAL_LAYOUT_LISTENER = -8

fun Activity.showPRMSoftInput() {
    if (!isSoftInputVisible()) {
        toggleSoftInput()
    }
}

/**
 * Show the soft input from view
 *
 * @param flags Provides additional operating flags.  Currently may be 0 or have the [InputMethodManager.SHOW_IMPLICIT] bit set.
 */
fun View.showSoftInput(flags: Int) {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.let { manager ->
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        manager.showSoftInput(
            this, flags,
            object : ResultReceiver(Handler()) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    if (resultCode == InputMethodManager.RESULT_UNCHANGED_HIDDEN ||
                        resultCode == InputMethodManager.RESULT_HIDDEN
                    ) {
                        context.toggleSoftInput()
                    }
                }
            }
        )
        manager.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }
}

/**
 * Hide the soft input from view
 */
fun View.hideSoftInput() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.let { manager ->
        manager.hideSoftInputFromWindow(windowToken, 0)
    }
}

/**
 * Hide the soft input from window
 */
fun Window.hideSoftInput() {
    currentFocus?.let {
        val focusView = decorView.findViewWithTag<View?>("keyboardTagView")
        val view = if (focusView == null) {
            val editText = EditText(context)
            editText.tag = "keyboardTagView"
            (decorView as ViewGroup).addView(editText, 0, 0)
            editText
        } else {
            focusView
        }
        view.hideSoftInput()
    }
}

/**
 * Hide the soft input from activity
 */
fun Activity.hideSoftInput() {
    window.hideSoftInput()
}

/**
 * Toggle the soft input display or not.
 */
fun Context.toggleSoftInput() {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.let { manager ->
        manager.toggleSoftInput(0, 0)
    }
}

private var decorViewDelta = 0

private fun Window.decorViewInvisibleHeight(): Int {
    val rect = Rect()
    decorView.getWindowVisibleDisplayFrame(rect)
    val delta = abs(decorView.bottom - rect.bottom)
    return if (delta <= navBarHeight() + statusBarHeight()) {
        decorViewDelta = delta
        0
    } else {
        delta - decorViewDelta
    }
}

fun Activity.isSoftInputVisible(): Boolean {
    return window.decorViewInvisibleHeight() > 0
}

/**
 * Register soft input changed listener from activity
 *
 * @param onChanged The soft input changed listener.
 */
fun Activity.registerSoftInputChanged(onChanged: (Int) -> Unit) {
    window.registerSoftInputChanged(onChanged)
}

/**
 * Register soft input changed listener from window
 *
 * @param onChanged The soft input changed listener.
 */
fun Window.registerSoftInputChanged(onChanged: (Int) -> Unit) {
    if (attributes.flags.and(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) != 0) {
        clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
    val contentView = findViewById<FrameLayout>(android.R.id.content)
    var previousHeight = decorViewInvisibleHeight()
    val onGlobalLayoutListener = OnGlobalLayoutListener {
        val height = decorViewInvisibleHeight()
        if (previousHeight != height) {
            onChanged(height)
            previousHeight = height
        }
    }
    contentView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    contentView.setTag(TAG_ON_GLOBAL_LAYOUT_LISTENER, onGlobalLayoutListener)
}

fun Window.unregisterSoftInputChanged() {
    val contentView = findViewById<FrameLayout>(android.R.id.content)
    val tag = contentView.getTag(TAG_ON_GLOBAL_LAYOUT_LISTENER)
    if (tag is OnGlobalLayoutListener) {
        contentView.viewTreeObserver.removeOnGlobalLayoutListener(tag)
    }
}
