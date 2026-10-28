package com.awake.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator

class SwitchView(context: Context) : View(context) {

    var onToggle: (() -> Unit)? = null

    /** Time left on the timer, shown instead of the hint ("14:52"), or null when there is no timer. */
    var countdown: String? = null
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private var t = 0f
    var isOn = false
        private set
    private var anim: ValueAnimator? = null
    private val hit = RectF()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Color.parseColor("#1B1C1F")

    /** Which model to draw. */
    var style: SwitchStyle = SwitchStyle.CLASSIC
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isFocusable = true
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateDescription()
    }

    // read by TalkBack: "Screen Switch, switch, on/off"
    private fun updateDescription() {
        contentDescription = context.getString(R.string.app_name)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            stateDescription = context.getString(if (isOn) R.string.a11y_on else R.string.a11y_off)
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = true
        info.isChecked = isOn
    }

    fun setOn(on: Boolean, animate: Boolean) {
        isOn = on
        updateDescription()
        val target = if (on) 1f else 0f
        anim?.cancel()
        if (!animate) {
            t = target
            invalidate()
            return
        }
        anim = ValueAnimator.ofFloat(t, target).apply {
            duration = 160
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                t = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(bg)

        // the classic plate carries its label inside; the square models get it underneath
        val labelInside = style == SwitchStyle.CLASSIC
        val area = if (labelInside) {
            RectF(w * 0.07f, h * 0.1f, w * 0.93f, h * 0.9f)
        } else {
            RectF(w * 0.12f, h * 0.08f, w * 0.88f, h * 0.82f)
        }
        val plate = SwitchArt.fit(style, area)
        hit.set(SwitchArt.draw(c, style, plate, t))

        p.reset()
        p.isAntiAlias = true
        p.color = Color.parseColor("#8B9097")
        p.textAlign = Paint.Align.CENTER
        val label = countdown?.let { context.getString(R.string.timer_left, it) }
            ?: context.getString(if (isOn) R.string.hint_on else R.string.hint_off)
        if (labelInside) {
            p.textSize = plate.height() * 0.035f
            c.drawText(label, plate.centerX(), plate.top + plate.height() * 0.9f, p)
        } else {
            p.textSize = minOf(plate.width() * 0.055f, h * 0.035f)
            c.drawText(label, plate.centerX(), plate.bottom + p.textSize * 2.2f, p)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                if (hit.contains(e.x, e.y)) performClick()
                return true
            }
        }
        return super.onTouchEvent(e)
    }

    // TalkBack's double tap also arrives here
    override fun performClick(): Boolean {
        super.performClick()
        onToggle?.invoke()
        return true
    }
}
