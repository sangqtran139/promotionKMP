package com.ttcn.prm.ui.utils.extension

import android.content.res.Resources

/**
 * Return the status bar's height.
 *
 * @return the status bar's height
 */
fun statusBarHeight(): Int {
    val resources: Resources = Resources.getSystem()
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    return resources.getDimensionPixelSize(resourceId)
}

/**
 * Return the navigation bar's height.
 *
 * @return the navigation bar's height
 */
fun navBarHeight(): Int {
    val res = Resources.getSystem()
    val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId != 0) {
        res.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}
