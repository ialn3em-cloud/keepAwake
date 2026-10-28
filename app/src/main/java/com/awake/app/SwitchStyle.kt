package com.awake.app

import android.content.Context

/** The switch models the user can pick. Every model but the classic one comes with Pro. */
enum class SwitchStyle(val id: String, val pro: Boolean, val nameRes: Int, val aspect: Float) {
    CLASSIC("classic", false, R.string.style_classic, 0.61f),
    GOLD("gold", true, R.string.style_gold, 1f),
    GLASS("glass", true, R.string.style_glass, 1f);

    companion object {
        private const val KEY = "switch_style"

        fun chosen(context: Context): SwitchStyle {
            val id = prefs(context).getString(KEY, CLASSIC.id)
            return entries.firstOrNull { it.id == id } ?: CLASSIC
        }

        /** What is actually shown: a Pro model falls back to the classic one without Pro (e.g. after a refund). */
        fun current(context: Context): SwitchStyle {
            val s = chosen(context)
            return if (s.pro && !Pro.isUnlocked(context)) CLASSIC else s
        }

        fun choose(context: Context, style: SwitchStyle) {
            prefs(context).edit().putString(KEY, style.id).apply()
            SwitchWidget.refresh(context)
        }

        private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
}
