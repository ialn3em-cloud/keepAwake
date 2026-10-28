package com.awake.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Four round buttons under the switch: 10, 15 and 30 minutes, and "always".
 * The chosen one glows red. [onPick] gets the minutes (0 = always).
 */
class TimerChips(context: Context) : LinearLayout(context) {

    var onPick: ((Int) -> Unit)? = null

    private val options = intArrayOf(10, 15, 30, 0)
    private val chips = mutableListOf<TextView>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, dp(8), 0, dp(8))
        for (minutes in options) {
            val chip = TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 12f
                setLineSpacing(0f, 0.9f)
                text = label(minutes)
                contentDescription = if (minutes == 0) {
                    context.getString(R.string.timer_always_a11y)
                } else {
                    context.resources.getQuantityString(R.plurals.timer_minutes_a11y, minutes, minutes)
                }
                setOnClickListener { onPick?.invoke(minutes) }
            }
            chips.add(chip)
            addView(chip, LayoutParams(dp(64), dp(64)).apply { marginStart = dp(6); marginEnd = dp(6) })
        }
    }

    fun select(minutes: Int) {
        options.forEachIndexed { i, m ->
            val on = m == minutes
            chips[i].isSelected = on
            chips[i].setTextColor(if (on) Color.WHITE else Color.parseColor("#C9CDD2"))
            chips[i].background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(if (on) "#C0392B" else "#26282C"))
                setStroke(dp(if (on) 2 else 1), Color.parseColor(if (on) "#F0463F" else "#3A3D42"))
            }
            chips[i].elevation = if (on) dp(6).toFloat() else 0f
        }
    }

    // "⏱" on top, the number in bold, the unit underneath
    private fun label(minutes: Int): CharSequence {
        val number = if (minutes == 0) "∞" else minutes.toString()
        val unit = context.getString(if (minutes == 0) R.string.timer_always else R.string.timer_min)
        val sb = SpannableStringBuilder("⏱\n")
        sb.setSpan(RelativeSizeSpan(0.75f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val start = sb.length
        sb.append(number)
        sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(RelativeSizeSpan(1.45f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val unitStart = sb.length
        sb.append("\n").append(unit)
        sb.setSpan(RelativeSizeSpan(0.8f), unitStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
