package com.awake.app

import android.app.Activity
import android.view.View

/** Free build: the main screen is just the switch. The pro build has its own ProEntry. */
object ProEntry {
    fun wrap(activity: Activity, switchView: View): View = switchView
    fun refresh(root: View) {}
}
