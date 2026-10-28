package com.awake.app

import android.animation.ArgbEvaluator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/**
 * Draws each switch model into a given rectangle, for the app, the gallery and the widget alike.
 * [t] goes from 0 (off) to 1 (on) so the change can be animated.
 */
object SwitchArt {

    private val ev = ArgbEvaluator()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private fun mix(t: Float, from: String, to: String) =
        ev.evaluate(t, Color.parseColor(from), Color.parseColor(to)) as Int

    private fun reset(): Paint {
        p.reset()
        p.isAntiAlias = true
        return p
    }

    /** The largest rectangle of the model's shape that fits in [area], centered. */
    fun fit(style: SwitchStyle, area: RectF): RectF {
        var w = area.width()
        var h = w / style.aspect
        if (h > area.height()) {
            h = area.height()
            w = h * style.aspect
        }
        val l = area.centerX() - w / 2f
        val t = area.centerY() - h / 2f
        return RectF(l, t, l + w, t + h)
    }

    /** Draws the model; returns the part that should react to a tap. */
    fun draw(c: Canvas, style: SwitchStyle, r: RectF, t: Float): RectF = when (style) {
        SwitchStyle.CLASSIC -> classic(c, r, t)
        SwitchStyle.GOLD -> gold(c, r, t)
        SwitchStyle.GLASS -> glass(c, r, t)
    }

    /** The model alone on a transparent background, for the widget and the gallery. */
    fun bitmap(style: SwitchStyle, on: Boolean, height: Int): Bitmap {
        val pad = height * 0.06f
        val h = height.toFloat()
        val w = (h - 2 * pad) * style.aspect + 2 * pad
        val bmp = Bitmap.createBitmap(w.toInt(), height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), style, RectF(pad, pad, w - pad, h - pad), if (on) 1f else 0f)
        return bmp
    }

    // the red switch on a white plate with a lamp above it (the original design)
    private fun classic(c: Canvas, plate: RectF, t: Float): RectF {
        val pw = plate.width()
        val ph = plate.height()
        val cx = plate.centerX()
        val top = plate.top
        val pr = pw * 0.07f

        reset().shader = LinearGradient(0f, top, 0f, top + ph,
            Color.parseColor("#F6F7F9"), Color.parseColor("#E1E4E8"), Shader.TileMode.CLAMP)
        p.setShadowLayer(pw * 0.04f, 0f, pw * 0.02f, Color.argb(120, 0, 0, 0))
        c.drawRoundRect(plate, pr, pr, p)

        val iw = pw * 0.36f
        val ih = iw * 0.38f
        val icy = top + ph * 0.265f
        val ir = RectF(cx - iw / 2f, icy - ih / 2f, cx + iw / 2f, icy + ih / 2f)
        reset().color = mix(t, "#5C1A1A", "#FF2B2B")
        if (t > 0f) p.setShadowLayer(iw * 0.35f * t, 0f, 0f, Color.argb((255 * t).toInt(), 255, 40, 40))
        c.drawRoundRect(ir, ih * 0.3f, ih * 0.3f, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = pw * 0.005f
        p.color = Color.parseColor("#7A1515")
        c.drawRoundRect(ir, ih * 0.3f, ih * 0.3f, p)

        val rw = pw * 0.36f
        val rh = rw * 1.85f
        val rcy = top + ph * 0.56f
        val rocker = RectF(cx - rw / 2f, rcy - rh / 2f, cx + rw / 2f, rcy + rh / 2f)
        val rr = rw * 0.08f
        reset().color = Color.parseColor("#C0392B")
        p.setShadowLayer(rw * 0.12f, 0f, rw * 0.06f, Color.argb(110, 0, 0, 0))
        c.drawRoundRect(rocker, rr, rr, p)

        path.reset()
        path.addRoundRect(rocker, rr, rr, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        val topC = mix(t, "#9E1F1B", "#F0463F")
        val botC = mix(t, "#F0463F", "#9E1F1B")
        reset().shader = LinearGradient(0f, rocker.top, 0f, rocker.bottom,
            intArrayOf(topC, topC, botC, botC), floatArrayOf(0f, 0.47f, 0.53f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(rocker, p)
        c.restore()

        reset().style = Paint.Style.STROKE
        p.strokeWidth = pw * 0.006f
        p.color = Color.parseColor("#7A1515")
        c.drawRoundRect(rocker, rr, rr, p)

        // a generous touch area around the rocker
        return RectF(rocker).apply { inset(-rw * 0.4f, -rw * 0.4f) }
    }

    // brushed champagne gold plate with a large rocker and a thin light strip
    private fun gold(c: Canvas, f: RectF, t: Float): RectF {
        val s = f.width()
        fun x(v: Float) = f.left + s * v
        fun y(v: Float) = f.top + s * v

        reset().shader = LinearGradient(f.left, f.top, f.right, f.bottom,
            intArrayOf(Color.parseColor("#E3CC98"), Color.parseColor("#CFB27A"), Color.parseColor("#DCC38D")),
            null, Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(110, 0, 0, 0))
        c.drawRoundRect(f, s * 0.025f, s * 0.025f, p)

        // fine brushing
        reset().strokeWidth = s * 0.004f
        var lx = f.left + s * 0.01f
        var i = 0
        while (lx < f.right) {
            p.color = if (i % 2 == 0) Color.argb(14, 255, 255, 255) else Color.argb(10, 0, 0, 0)
            c.drawLine(lx, f.top + 1, lx, f.bottom - 1, p)
            lx += s * 0.011f
            i++
        }

        // the dark recess the rocker sits in
        val rec = RectF(x(0.115f), y(0.205f), x(0.885f), y(0.785f))
        reset().color = Color.parseColor("#2B1D08")
        c.drawRect(rec, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.008f
        p.color = Color.argb(150, 150, 115, 55)
        c.drawRect(rec, p)

        // off: bottom edge pressed in (gap along the bottom), on: top edge pressed in
        val rTop = y(0.215f + 0.04f * t)
        val rBot = y(0.745f + 0.03f * t)
        val rk = RectF(x(0.125f), rTop, x(0.86f), rBot)
        val light = "#E9D6A8"
        val dark = "#D2B883"
        reset().shader = LinearGradient(0f, rk.top, 0f, rk.bottom,
            mix(t, light, dark), mix(t, dark, light), Shader.TileMode.CLAMP)
        c.drawRect(rk, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.005f
        p.color = Color.argb(90, 255, 245, 215)
        c.drawRect(rk, p)

        // light strip
        val lw = s * 0.1f
        val lh = s * 0.02f
        val ly = rk.top + s * 0.07f
        val led = RectF(rk.centerX() - lw / 2, ly, rk.centerX() + lw / 2, ly + lh)
        reset().color = mix(t, "#5E6A68", "#8FE9FF")
        if (t > 0f) p.setShadowLayer(s * 0.05f * t, 0f, 0f, Color.argb((230 * t).toInt(), 60, 200, 255))
        c.drawRoundRect(led, lh / 2, lh / 2, p)

        return RectF(f)
    }

    // black glass touch panel with a ring that glows warm gold
    private fun glass(c: Canvas, f: RectF, t: Float): RectF {
        val s = f.width()
        val rr = s * 0.08f
        reset().shader = LinearGradient(f.left, f.top, f.right, f.bottom,
            intArrayOf(Color.parseColor("#2B2E33"), Color.parseColor("#0B0C0E"), Color.parseColor("#16181B")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(140, 0, 0, 0))
        c.drawRoundRect(f, rr, rr, p)

        // glare across the top-left corner
        path.reset()
        path.addRoundRect(f, rr, rr, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        reset().shader = LinearGradient(f.left, f.top, f.left + s * 0.5f, f.top + s * 0.5f,
            Color.argb(34, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        val glare = Path().apply {
            moveTo(f.left, f.top)
            lineTo(f.left + s * 0.75f, f.top)
            lineTo(f.left, f.top + s * 0.55f)
            close()
        }
        c.drawPath(glare, p)
        c.restore()
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.006f
        p.color = Color.argb(30, 255, 255, 255)
        c.drawRoundRect(f, rr, rr, p)

        // touch ring
        val cx = f.centerX()
        val cy = f.centerY()
        val ringColor = mix(t, "#4A4D52", "#FFD9A0")
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.013f
        p.color = ringColor
        if (t > 0f) p.setShadowLayer(s * 0.07f * t, 0f, 0f, Color.argb((255 * t).toInt(), 255, 179, 71))
        c.drawCircle(cx, cy, s * 0.16f, p)
        reset().color = mix(t, "#4A4D52", "#FFE2B5")
        if (t > 0f) p.setShadowLayer(s * 0.04f * t, 0f, 0f, Color.argb((255 * t).toInt(), 255, 179, 71))
        c.drawCircle(cx, cy, s * 0.025f, p)

        // status bar at the bottom
        val bw = s * 0.15f
        val bh = s * 0.016f
        val by = f.bottom - s * 0.11f
        reset().color = mix(t, "#2C2E32", "#FFB347")
        if (t > 0f) p.setShadowLayer(s * 0.03f * t, 0f, 0f, Color.argb((255 * t).toInt(), 255, 179, 71))
        c.drawRoundRect(RectF(cx - bw / 2, by, cx + bw / 2, by + bh), bh / 2, bh / 2, p)

        return RectF(f)
    }
}
