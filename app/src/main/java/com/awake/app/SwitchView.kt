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
import android.view.animation.LinearInterpolator

class SwitchView(context: Context) : View(context) {

    var onToggle: (() -> Unit)? = null

    /** Time left on the timer, shown instead of the hint ("14:52"), or null when there is no timer. */
    var countdown: String? = null
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    // mechanical position, light brightness, and finger pressure
    private var m = 0f
    private var g = 0f
    private var press = 0f
    private var mechAnim: ValueAnimator? = null
    private var lightAnim: ValueAnimator? = null
    private var pressAnim: ValueAnimator? = null
    var isOn = false
        private set
    private val hit = RectF()
    private val lastPlate = RectF()
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

    /**
     * Like a real switch: the rocker snaps over with a small bounce, the light comes on a moment
     * after the contact closes (each model in its own way: flicker, blink, sweep), and fades out
     * gently when it is turned off.
     */
    fun setOn(on: Boolean, animate: Boolean) {
        isOn = on
        updateDescription()
        val target = if (on) 1f else 0f
        mechAnim?.cancel()
        lightAnim?.cancel()
        if (!animate) {
            m = target
            g = target
            invalidate()
            return
        }
        mechAnim = ValueAnimator.ofFloat(m, target).apply {
            // a quick snap that settles at once, like a real rocker; no bounce
            duration = 150
            interpolator = DecelerateInterpolator(2.2f)
            addUpdateListener {
                m = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        lightAnim = if (on) {
            val (profile, length) = SwitchArt.ignition(style)
            ValueAnimator.ofFloat(0f, 1f).apply {
                startDelay = 90
                duration = length
                interpolator = LinearInterpolator()
                addUpdateListener {
                    g = SwitchArt.sample(profile, it.animatedValue as Float)
                    invalidate()
                }
                start()
            }
        } else {
            ValueAnimator.ofFloat(g, 0f).apply {
                duration = 340
                interpolator = DecelerateInterpolator(1.6f)
                addUpdateListener {
                    g = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    private fun animatePress(to: Float) {
        pressAnim?.cancel()
        pressAnim = ValueAnimator.ofFloat(press, to).apply {
            duration = if (to > 0f) 70 else 140
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                press = it.animatedValue as Float
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
        lastPlate.set(plate)
        hit.set(SwitchArt.draw(c, style, plate, m, g, press))

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
            MotionEvent.ACTION_DOWN -> {
                if (hit.contains(e.x, e.y)) animatePress(1f)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animatePress(0f)
                return true
            }
            MotionEvent.ACTION_UP -> {
                animatePress(0f)
                if (hit.contains(e.x, e.y)) {
                    if (style == SwitchStyle.FACTORY) {
                        // green turns it on, red turns it off; pressing the one already down does nothing
                        val wantOn = e.y < SwitchArt.factorySplit(lastPlate)
                        if (wantOn != isOn) performClick()
                    } else {
                        performClick()
                    }
                }
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
