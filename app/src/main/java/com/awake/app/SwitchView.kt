package com.awake.app

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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
    private val rocker = RectF()
    private val path = Path()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ev = ArgbEvaluator()
    private val bg = Color.parseColor("#1B1C1F")

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

        val ph = minOf(h * 0.8f, w * 0.86f / 0.61f)
        val pw = ph * 0.61f
        val left = (w - pw) / 2f
        val top = (h - ph) / 2f
        val cx = w / 2f
        val plate = RectF(left, top, left + pw, top + ph)
        val pr = pw * 0.07f

        // plate
        p.reset()
        p.isAntiAlias = true
        p.shader = LinearGradient(
            0f, top, 0f, top + ph,
            Color.parseColor("#F6F7F9"), Color.parseColor("#E1E4E8"), Shader.TileMode.CLAMP
        )
        p.setShadowLayer(pw * 0.04f, 0f, pw * 0.02f, Color.argb(120, 0, 0, 0))
        c.drawRoundRect(plate, pr, pr, p)

        // indicator light
        val iw = pw * 0.36f
        val ih = iw * 0.38f
        val icy = top + ph * 0.265f
        val ir = RectF(cx - iw / 2f, icy - ih / 2f, cx + iw / 2f, icy + ih / 2f)
        p.reset()
        p.isAntiAlias = true
        p.color = ev.evaluate(t, Color.parseColor("#5C1A1A"), Color.parseColor("#FF2B2B")) as Int
        if (t > 0f) {
            p.setShadowLayer(iw * 0.35f * t, 0f, 0f, Color.argb((255 * t).toInt(), 255, 40, 40))
        }
        c.drawRoundRect(ir, ih * 0.3f, ih * 0.3f, p)
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = pw * 0.005f
        p.color = Color.parseColor("#7A1515")
        c.drawRoundRect(ir, ih * 0.3f, ih * 0.3f, p)

        // rocker
        val rw = pw * 0.36f
        val rh = rw * 1.85f
        val rcy = top + ph * 0.56f
        rocker.set(cx - rw / 2f, rcy - rh / 2f, cx + rw / 2f, rcy + rh / 2f)
        val rr = rw * 0.08f

        p.reset()
        p.isAntiAlias = true
        p.color = Color.parseColor("#C0392B")
        p.setShadowLayer(rw * 0.12f, 0f, rw * 0.06f, Color.argb(110, 0, 0, 0))
        c.drawRoundRect(rocker, rr, rr, p)

        path.reset()
        path.addRoundRect(rocker, rr, rr, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        val light = Color.parseColor("#F0463F")
        val dark = Color.parseColor("#9E1F1B")
        val topC = ev.evaluate(t, dark, light) as Int
        val botC = ev.evaluate(t, light, dark) as Int
        p.reset()
        p.isAntiAlias = true
        p.shader = LinearGradient(
            0f, rocker.top, 0f, rocker.bottom,
            intArrayOf(topC, topC, botC, botC),
            floatArrayOf(0f, 0.47f, 0.53f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(rocker, p)
        c.restore()

        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = pw * 0.006f
        p.color = Color.parseColor("#7A1515")
        c.drawRoundRect(rocker, rr, rr, p)

        // label
        p.reset()
        p.isAntiAlias = true
        p.color = Color.parseColor("#8B9097")
        p.textSize = ph * 0.035f
        p.textAlign = Paint.Align.CENTER
        val label = countdown?.let { context.getString(R.string.timer_left, it) }
            ?: context.getString(if (isOn) R.string.hint_on else R.string.hint_off)
        c.drawText(label, cx, top + ph * 0.9f, p)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val slop = rocker.width() * 0.4f
                val hit = RectF(rocker)
                hit.inset(-slop, -slop)
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
