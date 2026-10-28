package com.awake.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * The switch models: a live preview on top (tap it to see it switch), the models in a grid,
 * and one button that applies the picked model, or offers Pro when it is locked.
 */
class StylesActivity : Activity() {

    private val bg = Color.parseColor("#1B1C1F")
    private val fg = Color.parseColor("#E6E8EB")
    private val card = Color.parseColor("#26282C")
    private val red = Color.parseColor("#F0463F")

    private lateinit var preview: SwitchView
    private lateinit var action: TextView
    private val cards = mutableMapOf<SwitchStyle, LinearLayout>()
    private var picked = SwitchStyle.CLASSIC
    private val proListener: () -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picked = SwitchStyle.current(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            padForSystemBars()
        }
        root.addView(TextView(this).apply {
            setText(R.string.styles_title)
            textSize = 22f
            setTextColor(fg)
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            setText(R.string.styles_hint)
            textSize = 13f
            setTextColor(Color.parseColor("#8B9097"))
            setPadding(0, dp(4), 0, dp(4))
        })

        preview = SwitchView(this).apply {
            style = picked
            setOn(true, false)
            // the preview only plays the switch; it does not touch the real one
            onToggle = {
                setOn(!isOn, true)
                Haptics.click(this@StylesActivity)
            }
        }
        root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        SwitchStyle.entries.chunked(3).forEach { rowStyles ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowStyles.forEach { style ->
                val c = buildCard(style)
                cards[style] = c
                row.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                })
            }
            // keep cards the same width when the last row is not full
            repeat(3 - rowStyles.size) {
                row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            }
            grid.addView(row)
        }
        root.addView(grid)

        action = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setOnClickListener { act() }
        }
        root.addView(action, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        })

        setContentView(root)
        render()
    }

    override fun onResume() {
        super.onResume()
        Pro.addListener(proListener)
        Pro.start(this)
        render()
    }

    override fun onPause() {
        Pro.removeListener(proListener)
        super.onPause()
    }

    private fun buildCard(style: SwitchStyle) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(6), dp(8), dp(6), dp(8))
        val box = FrameLayout(context)
        box.addView(ImageView(context).apply {
            setImageBitmap(SwitchArt.bitmap(style, true, dp(84)))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(84)))
        box.addView(TextView(context).apply {
            tag = "badge"
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(6), dp(1), dp(6), dp(1))
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START))
        addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(TextView(context).apply {
            setText(style.nameRes)
            textSize = 12f
            setTextColor(fg)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })
        contentDescription = getString(style.nameRes)
        setOnClickListener {
            picked = style
            preview.style = style
            preview.setOn(true, false)
            render()
        }
    }

    private fun render() {
        val unlocked = Pro.isUnlocked(this)
        val current = SwitchStyle.current(this)
        cards.forEach { (style, c) ->
            val sel = style == picked
            c.background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(card)
                setStroke(dp(2), if (sel) red else Color.TRANSPARENT)
            }
            val badge = c.findViewWithTag<TextView>("badge")
            when {
                !style.pro -> badge.setBadge(R.string.styles_free, "#1E8E3E", Color.WHITE)
                unlocked -> badge.setBadge(R.string.styles_pro, "#F5B700", bg)
                else -> badge.setBadge(R.string.styles_locked, "#F5B700", bg)
            }
        }

        val locked = picked.pro && !unlocked
        action.text = when {
            picked == current -> getString(R.string.styles_in_use)
            locked -> Pro.price?.let { getString(R.string.pro_buy_price, it) } ?: getString(R.string.pro_buy)
            else -> getString(R.string.styles_use)
        }
        action.isEnabled = picked != current
        action.alpha = if (picked == current) 0.5f else 1f
        action.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(Color.parseColor("#D24435"), Color.parseColor("#B03226"))
        }
    }

    private fun act() {
        if (picked.pro && !Pro.isUnlocked(this)) {
            if (!Pro.buy(this)) Toast.makeText(this, R.string.pro_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        SwitchStyle.choose(this, picked)
        Haptics.click(this)
        render()
    }

    private fun TextView.setBadge(text: Int, color: String, textColor: Int) {
        setText(text)
        setTextColor(textColor)
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor(color))
        }
    }

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}
