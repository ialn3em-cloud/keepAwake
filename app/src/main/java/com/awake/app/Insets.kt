package com.awake.app

import android.os.Build
import android.view.View
import android.view.WindowInsets

/**
 * From Android 15 (and always from Android 16) apps draw behind the status and navigation bars.
 * Add their size to this view's own padding so its content stays clear of them.
 */
@Suppress("DEPRECATION")
fun View.padForSystemBars() {
    val left = paddingLeft
    val top = paddingTop
    val right = paddingRight
    val bottom = paddingBottom
    setOnApplyWindowInsetsListener { v, insets ->
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
        } else {
            v.setPadding(
                left + insets.systemWindowInsetLeft, top + insets.systemWindowInsetTop,
                right + insets.systemWindowInsetRight, bottom + insets.systemWindowInsetBottom
            )
        }
        insets
    }
}
