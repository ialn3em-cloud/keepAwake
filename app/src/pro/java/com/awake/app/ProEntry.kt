package com.awake.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Pro build: the switch with an "Automatic apps" card under it.
 * The card is filled red while the feature is on, and dark with a red outline while it is off.
 */
object ProEntry {

    private const val TAG_CARD = "auto_card"

    fun wrap(activity: Activity, switchView: View): View {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1B1C1F"))
            // keep the card above the navigation bar
            padForSystemBars()
        }
        // the switch takes the space left above the card, so the two never overlap
        root.addView(switchView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val card = LinearLayout(activity).apply {
            tag = TAG_CARD
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 14), dp(activity, 11), dp(activity, 14), dp(activity, 11))
            elevation = dp(activity, 6).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener {
                activity.startActivity(Intent(activity, AutoAppsActivity::class.java))
            }
        }

        val icon = TextView(activity).apply {
            text = "⚡"
            textSize = 17f
            gravity = Gravity.CENTER
        }
        card.addView(icon, LinearLayout.LayoutParams(dp(activity, 34), dp(activity, 34)))

        val texts = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 10), 0, dp(activity, 10), 0)
        }
        texts.addView(TextView(activity).apply {
            setText(R.string.auto_title)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        })
        val statusRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dot = View(activity)
        statusRow.addView(dot, LinearLayout.LayoutParams(dp(activity, 8), dp(activity, 8)).apply {
            marginEnd = dp(activity, 6)
        })
        statusRow.addView(TextView(activity).apply { textSize = 12f })
        texts.addView(statusRow)
        card.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        // "›" points forward; in Arabic forward is to the left, so it is mirrored to "‹"
        card.addView(TextView(activity).apply {
            text = "›"
            textSize = 22f
            textDirection = View.TEXT_DIRECTION_LOCALE
        })

        root.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 24))
        })
        refresh(root)
        return root
    }

    /** Show the current state; called when the main screen comes back into view. */
    fun refresh(root: View) {
        val card = root.findViewWithTag<LinearLayout>(TAG_CARD) ?: return
        val context = card.context
        val count = WatcherService.chosenApps(context).size
        val on = WatcherService.prefs(context).getBoolean(WatcherService.KEY_ENABLED, false) && count > 0

        val red = Color.parseColor("#C0392B")
        val textColor = if (on) Color.WHITE else Color.parseColor("#E6E8EB")
        card.background = GradientDrawable().apply {
            cornerRadius = dp(context, 18).toFloat()
            if (on) {
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                colors = intArrayOf(Color.parseColor("#D24435"), Color.parseColor("#B03226"))
            } else {
                setColor(Color.parseColor("#26282C"))
                setStroke(dp(context, 2), red)
            }
        }

        val icon = card.getChildAt(0) as TextView
        icon.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (on) Color.argb(46, 255, 255, 255) else Color.argb(51, 192, 57, 43))
        }

        val texts = card.getChildAt(1) as LinearLayout
        (texts.getChildAt(0) as TextView).setTextColor(textColor)
        val statusRow = texts.getChildAt(1) as LinearLayout
        statusRow.getChildAt(0).background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(if (on) "#7CFFB2" else "#8B9097"))
        }
        (statusRow.getChildAt(1) as TextView).apply {
            text = if (on) {
                context.resources.getQuantityString(R.plurals.auto_status_on, count, count)
            } else {
                context.getString(R.string.auto_status_off)
            }
            setTextColor(textColor)
            alpha = 0.85f
        }
        (card.getChildAt(2) as TextView).setTextColor(textColor)

        card.contentDescription = context.getString(R.string.auto_title) + ", " + (statusRow.getChildAt(1) as TextView).text
    }

    private fun dp(context: android.content.Context, v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()
}
