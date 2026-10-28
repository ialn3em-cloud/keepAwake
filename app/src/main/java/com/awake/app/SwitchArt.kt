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
 * Two values drive it, like a real switch: [m] is the mechanical position (0 off, 1 on),
 * [g] is how bright its light is (0 dark, 1 fully lit). [press] is the finger resting on it:
 * the plate never moves, only the part you push sinks a little into it.
 */
object SwitchArt {

    private val ev = ArgbEvaluator()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private fun mix(t: Float, from: String, to: String) =
        ev.evaluate(t.coerceIn(0f, 1f), Color.parseColor(from), Color.parseColor(to)) as Int

    private fun alpha(v: Float, max: Int) = (max * v.coerceIn(0f, 1f)).toInt()

    /** A lit edge along the top and a dark one along the bottom, so a flat shape reads as a solid piece. */
    private fun bevel(c: Canvas, r: RectF, radius: Float, w: Float, light: Int = 110, dark: Int = 70) {
        reset().style = Paint.Style.STROKE
        p.strokeWidth = w
        p.shader = LinearGradient(0f, r.top, 0f, r.bottom,
            intArrayOf(Color.argb(light, 255, 255, 255), Color.TRANSPARENT, Color.argb(dark, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        val e = RectF(r).apply { inset(w / 2, w / 2) }
        c.drawRoundRect(e, radius, radius, p)
    }

    /** The soft sheen of a polished or glossy surface across its upper part. */
    private fun gloss(c: Canvas, r: RectF, radius: Float, strength: Float) {
        path.reset()
        path.addRoundRect(r, radius, radius, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        reset().shader = LinearGradient(0f, r.top, 0f, r.top + r.height() * 0.55f,
            Color.argb(alpha(strength, 255), 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(r.left, r.top, r.right, r.top + r.height() * 0.55f, p)
        c.restore()
    }

    /** Light from a lamp falling on the surface around it, clipped to [clip]. */
    private fun spill(c: Canvas, clip: RectF, radius: Float, cx: Float, cy: Float, r: Float, rgb: Int, strength: Float) {
        if (strength <= 0f) return
        path.reset()
        path.addRoundRect(clip, radius, radius, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        reset().shader = android.graphics.RadialGradient(cx, cy, r,
            Color.argb(alpha(strength, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb)),
            Color.argb(0, Color.red(rgb), Color.green(rgb), Color.blue(rgb)), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        c.restore()
    }

    /**
     * How each model's light comes on, as (time 0..1, brightness) pairs, and how long it takes:
     * a lamp that flickers, an LED that blinks twice, a ring that sweeps round, a glow that breathes in.
     */
    fun ignition(style: SwitchStyle): Pair<FloatArray, Long> = when (style) {
        SwitchStyle.CLASSIC -> floatArrayOf(0f, 0f, .1f, .75f, .18f, .12f, .3f, .9f, .4f, .35f, .55f, 1f, 1f, 1f) to 520L
        SwitchStyle.GOLD, SwitchStyle.FACTORY ->
            floatArrayOf(0f, 0f, .12f, 1f, .24f, .08f, .38f, 1f, .5f, .15f, .62f, 1f, 1f, 1f) to 460L
        SwitchStyle.OAK -> floatArrayOf(0f, 0f, .15f, .6f, .25f, .3f, .45f, .9f, .6f, .72f, 1f, 1f) to 480L
        SwitchStyle.GLASS -> floatArrayOf(0f, 0f, .45f, .85f, .65f, .62f, 1f, 1f) to 560L
        SwitchStyle.ROUND, SwitchStyle.BLACK -> floatArrayOf(0f, 0f, .3f, .55f, .6f, .85f, 1f, 1f) to 460L
        SwitchStyle.MARBLE, SwitchStyle.KIDS -> floatArrayOf(0f, 0f, .4f, .7f, 1f, 1f) to 360L
    }

    /** Brightness at [f] (0..1) along a profile from [ignition]. */
    fun sample(profile: FloatArray, f: Float): Float {
        var i = 0
        while (i + 3 < profile.size && profile[i + 2] < f) i += 2
        val (t0, v0, t1, v1) = listOf(profile[i], profile[i + 1], profile[i + 2], profile[i + 3])
        if (t1 <= t0) return v1
        return v0 + (v1 - v0) * ((f - t0) / (t1 - t0)).coerceIn(0f, 1f)
    }

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
    fun draw(c: Canvas, style: SwitchStyle, r: RectF, m: Float, g: Float, press: Float = 0f): RectF {
        this.press = press
        return drawStyle(c, style, r, m, g)
    }

    private var press = 0f

    private fun drawStyle(c: Canvas, style: SwitchStyle, r: RectF, m: Float, g: Float): RectF = when (style) {
        SwitchStyle.CLASSIC -> classic(c, r, m, g)
        SwitchStyle.GOLD -> gold(c, r, m, g)
        SwitchStyle.GLASS -> glass(c, r, m, g)
        SwitchStyle.ROUND -> round(c, r, m, g)
        SwitchStyle.FACTORY -> factory(c, r, m, g)
        SwitchStyle.MARBLE -> marble(c, r, m, g)
        SwitchStyle.KIDS -> kids(c, r, m, g)
        SwitchStyle.BLACK -> black(c, r, m, g)
        SwitchStyle.OAK -> oak(c, r, m, g)
    }

    /** On the factory model, a tap above this line is the green (on) button, below it the red (off) one. */
    fun factorySplit(r: RectF) = r.top + r.width() * 156f / 170f

    /** The model alone on a transparent background, for the widget and the gallery. */
    fun bitmap(style: SwitchStyle, on: Boolean, height: Int): Bitmap {
        val pad = height * 0.06f
        val h = height.toFloat()
        val w = (h - 2 * pad) * style.aspect + 2 * pad
        val bmp = Bitmap.createBitmap(w.toInt(), height, Bitmap.Config.ARGB_8888)
        val v = if (on) 1f else 0f
        draw(Canvas(bmp), style, RectF(pad, pad, w - pad, h - pad), v, v)
        return bmp
    }

    // the red switch on a white plate with a lamp above it (the original design)
    private fun classic(c: Canvas, plate: RectF, m: Float, g: Float): RectF {
        val pw = plate.width()
        val ph = plate.height()
        val cx = plate.centerX()
        val top = plate.top
        val pr = pw * 0.07f

        reset().shader = LinearGradient(0f, top, 0f, top + ph,
            Color.parseColor("#F6F7F9"), Color.parseColor("#E1E4E8"), Shader.TileMode.CLAMP)
        p.setShadowLayer(pw * 0.04f, 0f, pw * 0.02f, Color.argb(120, 0, 0, 0))
        c.drawRoundRect(plate, pr, pr, p)
        bevel(c, plate, pr, pw * 0.012f, light = 200, dark = 40)

        val iw = pw * 0.36f
        val ih = iw * 0.38f
        val icy = top + ph * 0.265f
        val ir = RectF(cx - iw / 2f, icy - ih / 2f, cx + iw / 2f, icy + ih / 2f)
        // the lamp's red light falls on the white plate around it
        spill(c, plate, pr, cx, icy, pw * 0.5f, Color.rgb(255, 60, 50), g * 0.28f)
        reset().color = mix(g, "#5C1A1A", "#FF2B2B")
        if (g > 0f) p.setShadowLayer(iw * 0.35f * g, 0f, 0f, Color.argb(alpha(g, 255), 255, 40, 40))
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
        val topC = mix(m, "#9E1F1B", "#F0463F")
        val botC = mix(m, "#F0463F", "#9E1F1B")
        reset().shader = LinearGradient(0f, rocker.top, 0f, rocker.bottom,
            intArrayOf(topC, topC, botC, botC), floatArrayOf(0f, 0.47f, 0.53f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(rocker, p)
        c.restore()
        // glossy plastic: the raised half catches the light
        val raised = if (m > 0.5f) RectF(rocker.left, rocker.top, rocker.right, rocker.centerY())
            else RectF(rocker.left, rocker.centerY(), rocker.right, rocker.bottom)
        gloss(c, raised, rr, 0.22f)
        bevel(c, rocker, rr, rw * 0.035f, light = 90, dark = 60)

        reset().style = Paint.Style.STROKE
        p.strokeWidth = pw * 0.006f
        p.color = Color.parseColor("#7A1515")
        c.drawRoundRect(rocker, rr, rr, p)

        // a generous touch area around the rocker
        return RectF(rocker).apply { inset(-rw * 0.4f, -rw * 0.4f) }
    }

    // brushed champagne gold plate with a large rocker and a thin light strip
    private fun gold(c: Canvas, f: RectF, m: Float, g: Float): RectF {
        val s = f.width()
        fun x(v: Float) = f.left + s * v
        fun y(v: Float) = f.top + s * v

        reset().shader = LinearGradient(f.left, f.top, f.right, f.bottom,
            intArrayOf(Color.parseColor("#E3CC98"), Color.parseColor("#CFB27A"), Color.parseColor("#DCC38D")),
            null, Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(110, 0, 0, 0))
        c.drawRoundRect(f, s * 0.025f, s * 0.025f, p)
        bevel(c, f, s * 0.025f, s * 0.01f, light = 120, dark = 60)

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
        val rec = RectF(x(0.14f), y(0.215f), x(0.86f), y(0.785f))
        reset().color = Color.parseColor("#2B1D08")
        c.drawRect(rec, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.007f
        p.color = Color.argb(140, 150, 115, 55)
        c.drawRect(rec, p)

        // the rocker fills the recess; it tilts on a pivot across its middle, so one half faces up
        // to the light and the other leans away (off: top half up, on: bottom half up)
        val gap = s * 0.006f
        val rk = RectF(rec.left + gap, rec.top + gap, rec.right - gap, rec.bottom - gap)
        val mid = rk.centerY()
        val upper = RectF(rk.left, rk.top, rk.right, mid)
        val lower = RectF(rk.left, mid, rk.right, rk.bottom)
        reset().shader = LinearGradient(0f, upper.top, 0f, upper.bottom,
            mix(m, "#F0DFB4", "#D2B985"), mix(m, "#E7D2A3", "#DBC392"), Shader.TileMode.CLAMP)
        c.drawRect(upper, p)
        reset().shader = LinearGradient(0f, lower.top, 0f, lower.bottom,
            mix(m, "#DCC593", "#E7D2A3"), mix(m, "#D1B886", "#F0DFB4"), Shader.TileMode.CLAMP)
        c.drawRect(lower, p)

        // fine brushing on the rocker too
        reset().strokeWidth = s * 0.003f
        var bx = rk.left + s * 0.01f
        var bi = 0
        while (bx < rk.right) {
            p.color = if (bi % 2 == 0) Color.argb(10, 255, 255, 255) else Color.argb(7, 0, 0, 0)
            c.drawLine(bx, rk.top, bx, rk.bottom, p)
            bx += s * 0.011f
            bi++
        }

        // the pivot crease, a thin shadow where the low edge dips into the recess, darker sides
        reset().strokeWidth = s * 0.004f
        p.color = Color.argb(80, 255, 250, 230)
        c.drawLine(rk.left, mid - s * 0.002f, rk.right, mid - s * 0.002f, p)
        p.color = Color.argb(35, 0, 0, 0)
        c.drawLine(rk.left, mid + s * 0.002f, rk.right, mid + s * 0.002f, p)
        val sh = s * 0.02f
        val down = (1f - m).coerceIn(0f, 1f)
        reset().shader = LinearGradient(0f, rk.bottom - sh, 0f, rk.bottom,
            Color.TRANSPARENT, Color.argb(alpha(down, 190), 43, 29, 8), Shader.TileMode.CLAMP)
        c.drawRect(rk.left, rk.bottom - sh, rk.right, rk.bottom, p)
        reset().shader = LinearGradient(0f, rk.top, 0f, rk.top + sh,
            Color.argb(alpha(1f - down, 190), 43, 29, 8), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(rk.left, rk.top, rk.right, rk.top + sh, p)
        val sw = s * 0.012f
        reset().shader = LinearGradient(rk.left, 0f, rk.left + sw, 0f,
            Color.argb(40, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(rk.left, rk.top, rk.left + sw, rk.bottom, p)
        reset().shader = LinearGradient(rk.right, 0f, rk.right - sw, 0f,
            Color.argb(50, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(rk.right - sw, rk.top, rk.right, rk.bottom, p)
        bevel(c, rk, 0f, s * 0.005f, light = 140, dark = 40)

        // light strip near the top, its blue light spilling onto the gold around it
        val lw = s * 0.1f
        val lh = s * 0.018f
        val ly = rk.top + s * 0.065f
        val led = RectF(rk.centerX() - lw / 2, ly, rk.centerX() + lw / 2, ly + lh)
        spill(c, rk, 0f, led.centerX(), led.centerY(), s * 0.22f, Color.rgb(90, 210, 255), g * 0.2f)
        reset().color = mix(g, "#5E6A68", "#8FE9FF")
        if (g > 0f) p.setShadowLayer(s * 0.05f * g, 0f, 0f, Color.argb(alpha(g, 230), 60, 200, 255))
        c.drawRoundRect(led, lh / 2, lh / 2, p)

        return RectF(f)
    }

    // black glass touch panel with a ring that glows warm gold
    private fun glass(c: Canvas, f: RectF, m: Float, g: Float): RectF {
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
        // warm light blooming in the glass around the ring
        spill(c, f, rr, cx, cy, s * 0.42f, Color.rgb(255, 170, 60), g * 0.2f)
        val ringColor = mix(g, "#4A4D52", "#FFD9A0")
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.013f
        p.color = ringColor
        if (g > 0f) p.setShadowLayer(s * 0.07f * g, 0f, 0f, Color.argb(alpha(g, 255), 255, 179, 71))
        c.drawCircle(cx, cy, s * 0.16f, p)
        reset().color = mix(g, "#4A4D52", "#FFE2B5")
        if (g > 0f) p.setShadowLayer(s * 0.04f * g, 0f, 0f, Color.argb(alpha(g, 255), 255, 179, 71))
        c.drawCircle(cx, cy, s * 0.025f, p)

        // status bar at the bottom
        val bw = s * 0.15f
        val bh = s * 0.016f
        val by = f.bottom - s * 0.11f
        reset().color = mix(g, "#2C2E32", "#FFB347")
        if (g > 0f) p.setShadowLayer(s * 0.03f * g, 0f, 0f, Color.argb(alpha(g, 255), 255, 179, 71))
        c.drawRoundRect(RectF(cx - bw / 2, by, cx + bw / 2, by + bh), bh / 2, bh / 2, p)

        return RectF(f)
    }

    // dark plate with a round push button and a ring that lights up blue
    private fun round(c: Canvas, f: RectF, m: Float, g: Float): RectF {
        val s = f.width()
        reset().shader = LinearGradient(f.left, f.top, f.left, f.bottom,
            Color.parseColor("#2D2F33"), Color.parseColor("#18191B"), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(150, 0, 0, 0))
        c.drawRoundRect(f, s * 0.12f, s * 0.12f, p)
        bevel(c, f, s * 0.12f, s * 0.01f, light = 45, dark = 90)

        val cx = f.centerX()
        val cy = f.centerY()
        // the knob sits in a touch while on, and further under the finger
        val kr = s * 0.26f * (1f - 0.02f * m - 0.03f * press)
        spill(c, f, s * 0.12f, cx, cy, s * 0.5f, Color.rgb(47, 155, 255), g * 0.16f)
        reset().shader = android.graphics.RadialGradient(cx, cy - kr * 0.3f, kr * 1.2f,
            Color.parseColor("#3B3E43"), Color.parseColor("#222428"), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, kr, p)
        reset().shader = android.graphics.RadialGradient(cx - kr * 0.35f, cy - kr * 0.45f, kr * 0.6f,
            Color.argb(40, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, kr, p)

        // the dark ring, then the lit part sweeping round it from the top as the light comes on
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.035f
        p.color = Color.parseColor("#45484D")
        c.drawCircle(cx, cy, kr, p)
        if (g > 0f) {
            reset().style = Paint.Style.STROKE
            p.strokeWidth = s * 0.035f
            p.strokeCap = Paint.Cap.ROUND
            p.color = Color.parseColor("#2F9BFF")
            p.setShadowLayer(s * 0.08f * g, 0f, 0f, Color.argb(alpha(g, 255), 47, 155, 255))
            c.drawArc(RectF(cx - kr, cy - kr, cx + kr, cy + kr), -90f, 360f * g.coerceIn(0f, 1f), false, p)
        }

        powerIcon(c, cx, cy, s * 0.09f, s * 0.028f, mix(g, "#777777", "#7CC4FF"),
            if (g > 0f) Color.argb(alpha(g, 200), 47, 155, 255) else 0, s * 0.04f * g)
        return RectF(f)
    }

    private fun powerIcon(c: Canvas, cx: Float, cy: Float, r: Float, w: Float, color: Int, glow: Int, glowR: Float) {
        reset().style = Paint.Style.STROKE
        p.strokeWidth = w
        p.strokeCap = Paint.Cap.ROUND
        p.color = color
        if (glow != 0) p.setShadowLayer(glowR, 0f, 0f, glow)
        c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -55f, 290f, false, p)
        c.drawLine(cx, cy - r * 1.25f, cx, cy - r * 0.15f, p)
    }

    // industrial start/stop station: yellow housing, green I on top, red O below
    private fun factory(c: Canvas, f: RectF, m: Float, g: Float): RectF {
        val k = f.width() / 170f
        fun r(l: Float, tp: Float, rt: Float, b: Float) = RectF(f.left + l * k, f.top + tp * k, f.left + rt * k, f.top + b * k)
        val w = 170f

        reset().shader = LinearGradient(f.left, f.top, f.right, f.bottom,
            Color.parseColor("#2A2B2E"), Color.parseColor("#111214"), Shader.TileMode.CLAMP)
        p.setShadowLayer(14 * k, 0f, 8 * k, Color.argb(150, 0, 0, 0))
        c.drawRoundRect(f, 14 * k, 14 * k, p)

        for (hy in floatArrayOf(17f, 263f)) {
            reset().color = Color.parseColor("#F5F5F5")
            c.drawCircle(f.left + 85 * k, f.top + hy * k, 6 * k, p)
            reset().style = Paint.Style.STROKE
            p.strokeWidth = 2.5f * k
            p.color = Color.parseColor("#555555")
            c.drawCircle(f.left + 85 * k, f.top + hy * k, 7.5f * k, p)
        }
        reset().color = Color.parseColor("#0C0D0E")
        val screen = r(22f, 34f, w - 22f, 64f)
        c.drawRoundRect(screen, 3 * k, 3 * k, p)
        // the display flips with the buttons; ON blinks in as the light comes on, OFF lights as it fades
        val showOn = m > 0.5f
        dotMatrix(c, screen, if (showOn) "ON" else "OFF", on = showOn, brightness = if (showOn) g else 1f - g)

        reset().shader = LinearGradient(0f, f.top + 74 * k, 0f, f.top + 244 * k,
            Color.parseColor("#FFCC33"), Color.parseColor("#E6A400"), Shader.TileMode.CLAMP)
        p.setShadowLayer(6 * k, 0f, 4 * k, Color.argb(110, 0, 0, 0))
        c.drawRoundRect(r(12f, 74f, w - 12f, 244f), 12 * k, 12 * k, p)
        bevel(c, r(12f, 74f, w - 12f, 244f), 12 * k, 2.5f * k, light = 150, dark = 60)

        reset().color = Color.parseColor("#151617")
        c.drawRoundRect(r(28f, 84f, w - 28f, 234f), 12 * k, 12 * k, p)

        fun button(rect: RectF, top: String, bottom: String, pressed: Float) {
            // a pushed button sinks into its seat: a touch smaller and darker, it does not slide
            rect.inset(1.2f * k * pressed, 1.2f * k * pressed)
            val dim = 1f - 0.2f * pressed
            fun shade(hex: String): Int {
                val col = Color.parseColor(hex)
                return Color.rgb((Color.red(col) * dim).toInt(), (Color.green(col) * dim).toInt(), (Color.blue(col) * dim).toInt())
            }
            reset().shader = LinearGradient(0f, rect.top, 0f, rect.bottom, shade(top), shade(bottom), Shader.TileMode.CLAMP)
            if (pressed < 0.5f) p.setShadowLayer(3 * k, 0f, 4 * k * (1 - pressed), Color.argb(110, 0, 0, 0))
            c.drawRoundRect(rect, 7 * k, 7 * k, p)
            gloss(c, rect, 7 * k, 0.28f * (1f - pressed))
            bevel(c, rect, 7 * k, 1.5f * k, light = alpha(1f - pressed, 120), dark = 60)
        }
        val green = r(36f, 92f, w - 36f, 154f)
        val redB = r(36f, 158f, w - 36f, 220f)
        val mm = m.coerceIn(0f, 1f)
        button(green, "#3AC06A", "#1F8F48", maxOf(mm, if (mm < 0.5f) press else 0f))
        button(redB, "#E5483B", "#B4271D", maxOf(1f - mm, if (mm >= 0.5f) press else 0f))
        if (g > 0f) {
            reset().style = Paint.Style.STROKE
            p.strokeWidth = 2 * k
            p.color = Color.argb(alpha(g, 180), 61, 255, 134)
            p.setShadowLayer(14 * k * g, 0f, 0f, Color.argb(alpha(g, 255), 61, 255, 134))
            c.drawRoundRect(green, 7 * k, 7 * k, p)
        }
        reset().strokeWidth = 4 * k
        p.strokeCap = Paint.Cap.ROUND
        p.color = Color.parseColor("#E9FFF0")
        c.drawLine(green.centerX(), green.centerY() - 12 * k, green.centerX(), green.centerY() + 12 * k, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = 3 * k
        p.color = Color.parseColor("#FFE9E6")
        c.drawCircle(redB.centerX(), redB.centerY(), 11 * k, p)

        // screws and the soft glare of the rubber cover
        for (sx in floatArrayOf(18f, w - 18f)) {
            reset().color = Color.parseColor("#C9CDD2")
            c.drawCircle(f.left + sx * k, f.top + 159 * k, 7 * k, p)
            reset().strokeWidth = 1.6f * k
            p.color = Color.parseColor("#4A4F54")
            c.drawLine(f.left + (sx - 4) * k, f.top + 159 * k, f.left + (sx + 4) * k, f.top + 159 * k, p)
            c.drawLine(f.left + sx * k, f.top + 155 * k, f.left + sx * k, f.top + 163 * k, p)
        }
        val cover = r(28f, 84f, w - 28f, 234f)
        reset().shader = LinearGradient(cover.left, cover.top, cover.right, cover.bottom,
            intArrayOf(Color.argb(70, 255, 255, 255), Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(20, 255, 255, 255)),
            floatArrayOf(0f, 0.4f, 0.7f, 1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(cover, 12 * k, 12 * k, p)

        return r(12f, 74f, w - 12f, 244f)
    }

    // 5x7 dot-matrix letters for the factory model's little display
    private val glyphs = mapOf(
        'O' to arrayOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
        'N' to arrayOf("#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"),
        'F' to arrayOf("#####", "#....", "#....", "####.", "#....", "#....", "#....")
    )

    /** Dots on a dark grid, like an old LCD: every dot is drawn, the letters' dots are lit (green ON, red OFF, like the button that is down). */
    private fun dotMatrix(c: Canvas, screen: RectF, text: String, on: Boolean, brightness: Float) {
        val pitch = screen.height() * 0.78f / 7f
        val dot = pitch * 0.36f
        val cols = (screen.width() / pitch).toInt() - 1
        val ox = screen.centerX() - (cols - 1) * pitch / 2f
        val oy = screen.centerY() - 3 * pitch
        val textCols = text.length * 6 - 1
        val start = (cols - textCols) / 2
        val lit = Array(7) { BooleanArray(cols) }
        text.forEachIndexed { i, ch ->
            val g = glyphs[ch] ?: return@forEachIndexed
            for (row in 0 until 7) for (col in 0 until 5) {
                val x = start + i * 6 + col
                if (g[row][col] == '#' && x in 0 until cols) lit[row][x] = true
            }
        }
        for (row in 0 until 7) for (col in 0 until cols) {
            val dim = if (on) "#0E2414" else "#2A1010"
            reset().color = if (lit[row][col]) mix(brightness, dim, if (on) "#3DFF7A" else "#FF3B30") else Color.parseColor(dim)
            if (lit[row][col] && brightness > 0f) {
                p.setShadowLayer(dot * 1.6f, 0f, 0f,
                    if (on) Color.argb(alpha(brightness, 200), 34, 255, 102) else Color.argb(alpha(brightness, 200), 255, 40, 30))
            }
            c.drawCircle(ox + col * pitch, oy + row * pitch, dot, p)
        }
    }

    // white Carrara marble with grey and gold veins, a gold rocker and a light line
    private fun marble(c: Canvas, f: RectF, m: Float, g: Float): RectF {
        val s = f.width()
        val rr = s * 0.06f
        reset().shader = android.graphics.RadialGradient(f.left + s * 0.3f, f.top + s * 0.2f, s,
            intArrayOf(Color.WHITE, Color.parseColor("#ECEAE6"), Color.parseColor("#DCD9D3")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(120, 0, 0, 0))
        c.drawRoundRect(f, rr, rr, p)

        path.reset()
        path.addRoundRect(f, rr, rr, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        fun vein(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int, w: Float) {
            reset().style = Paint.Style.STROKE
            p.strokeWidth = s * w
            p.color = color
            val v = Path().apply {
                moveTo(f.left + s * x1, f.top + s * y1)
                quadTo(f.left + s * x2, f.top + s * y2, f.left + s * x3, f.top + s * y3)
            }
            c.drawPath(v, p)
        }
        vein(0.25f, -0.05f, 0.45f, 0.5f, 0.2f, 1.05f, Color.argb(70, 120, 120, 130), 0.012f)
        vein(0.55f, 0f, 0.75f, 0.45f, 1.05f, 0.6f, Color.argb(55, 120, 120, 130), 0.008f)
        vein(-0.05f, 0.75f, 0.5f, 0.68f, 1.05f, 0.9f, Color.argb(90, 190, 160, 100), 0.01f)
        c.restore()

        val rw = s * 0.41f
        val rh = s * 0.565f
        val rk = RectF(f.centerX() - rw / 2, f.centerY() - rh / 2 + s * 0.03f, f.centerX() + rw / 2, f.centerY() + rh / 2 + s * 0.03f)
        val light = Color.parseColor("#F3DF9E")
        val dark = Color.parseColor("#C8A24F")
        val topC = ev.evaluate(m.coerceIn(0f, 1f), light, dark) as Int
        val botC = ev.evaluate(m.coerceIn(0f, 1f), dark, light) as Int
        reset().shader = LinearGradient(0f, rk.top, 0f, rk.bottom,
            intArrayOf(topC, topC, botC, botC), floatArrayOf(0f, 0.48f, 0.52f, 1f), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.035f, 0f, s * 0.02f, Color.argb(90, 0, 0, 0))
        c.drawRoundRect(rk, s * 0.035f, s * 0.035f, p)
        gloss(c, rk, s * 0.035f, 0.3f)
        bevel(c, rk, s * 0.035f, s * 0.007f, light = 140, dark = 60)
        bevel(c, f, rr, s * 0.008f, light = 200, dark = 35)

        val gw = s * 0.13f
        val gh = s * 0.024f
        val gy = rk.top - s * 0.09f
        spill(c, f, rr, f.centerX(), gy + gh / 2, s * 0.3f, Color.rgb(255, 210, 90), g * 0.25f)
        reset().color = mix(g, "#BFB7A2", "#FFF4C2")
        if (g > 0f) p.setShadowLayer(s * 0.05f * g, 0f, 0f, Color.argb(alpha(g, 255), 255, 216, 90))
        c.drawRoundRect(RectF(f.centerX() - gw / 2, gy, f.centerX() + gw / 2, gy + gh), gh / 2, gh / 2, p)
        return RectF(f)
    }

    // a candy-coloured switch with a face: asleep when off, wakes up and smiles when on
    private fun kids(c: Canvas, f: RectF, m: Float, g: Float): RectF {
        val k = f.width() / 170f
        reset().shader = LinearGradient(f.left, f.top, f.right, f.bottom,
            Color.parseColor("#FFD6E7"), Color.parseColor("#C9E7FF"), Shader.TileMode.CLAMP)
        p.setShadowLayer(12 * k, 0f, 6 * k, Color.argb(90, 0, 0, 0))
        c.drawRoundRect(f, 40 * k, 40 * k, p)

        val cx = f.centerX()
        val cy = f.centerY()
        val rk = RectF(cx - 52 * k, cy - 65 * k, cx + 52 * k, cy + 65 * k)
        reset().color = Color.argb(30, 0, 0, 0)
        c.drawRoundRect(RectF(rk).apply { offset(0f, 6 * k) }, 34 * k, 34 * k, p)
        reset().shader = LinearGradient(0f, rk.top, 0f, rk.bottom,
            mix(g, "#CDD3FF", "#FFF38A"), mix(g, "#A9B2FF", "#FFD23F"), Shader.TileMode.CLAMP)
        c.drawRoundRect(rk, 34 * k, 34 * k, p)
        gloss(c, RectF(rk.left + 10 * k, rk.top + 6 * k, rk.right - 10 * k, rk.bottom), 30 * k, 0.4f)

        val eyeY = rk.top + 52 * k
        // the eyes pop open with the switch (a little wide while it snaps)
        val eyeH = (4 + 12 * m.coerceIn(0f, 1.15f)) * k
        for (ex in floatArrayOf(-22f, 22f)) {
            val ecx = cx + ex * k
            reset().color = Color.parseColor("#3B2B4A")
            c.drawRoundRect(RectF(ecx - 8 * k, eyeY - eyeH / 2, ecx + 8 * k, eyeY + eyeH / 2), 8 * k, 8 * k, p)
            if (m > 0.6f) {
                reset().color = Color.WHITE
                c.drawCircle(ecx - 2.5f * k, eyeY - 2.5f * k, 2.5f * k, p)
            }
            if (g > 0f) {
                reset().color = Color.argb(alpha(g, 140), 255, 120, 150)
                c.drawOval(RectF(ecx + ex * 0.45f * k - 7 * k, eyeY + 18 * k, ecx + ex * 0.45f * k + 7 * k, eyeY + 26 * k), p)
            }
        }
        val my = rk.top + 82 * k
        if (m > 0.5f) {
            reset().color = Color.parseColor("#FF6B8B")
            c.drawArc(RectF(cx - 17 * k, my - 17 * k, cx + 17 * k, my + 17 * k), 0f, 180f, true, p)
        } else {
            reset().color = Color.parseColor("#7A6FB0")
            c.drawRoundRect(RectF(cx - 9 * k, my, cx + 9 * k, my + 5 * k), 2.5f * k, 2.5f * k, p)
        }
        if (g < 1f) {
            reset().color = Color.argb(alpha(1f - g, 255), 122, 111, 176)
            p.textSize = 16 * k
            p.isFakeBoldText = true
            c.drawText("z z", f.right - 46 * k, f.top + 30 * k, p)
        }
        return RectF(f)
    }

    // matte black with a soft white line under the rocker when on
    private fun black(c: Canvas, f: RectF, m: Float, g: Float): RectF {
        val s = f.width()
        reset().shader = LinearGradient(f.left, f.top, f.right, f.bottom,
            Color.parseColor("#26272A"), Color.parseColor("#121315"), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(150, 0, 0, 0))
        c.drawRoundRect(f, s * 0.07f, s * 0.07f, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.006f
        p.color = Color.parseColor("#2E3034")
        c.drawRoundRect(f, s * 0.07f, s * 0.07f, p)

        val ins = s * 0.14f
        val rk = RectF(f.left + ins, f.top + ins, f.right - ins, f.bottom - ins)
        reset().shader = LinearGradient(0f, rk.top, 0f, rk.bottom,
            mix(m, "#1D1E21", "#2A2B2F"), mix(m, "#2A2B2F", "#1B1C1F"), Shader.TileMode.CLAMP)
        c.drawRoundRect(rk, s * 0.045f, s * 0.045f, p)
        bevel(c, rk, s * 0.045f, s * 0.008f, light = 30, dark = 90)

        powerIcon(c, rk.centerX(), rk.centerY(), s * 0.065f, s * 0.02f, mix(g, "#4A4C50", "#E9E9EA"),
            if (g > 0f) Color.argb(alpha(g, 220), 255, 255, 255) else 0, s * 0.04f * g)

        // the light line: a dim groove when off; when on it spreads out from the middle
        val ly = rk.bottom - s * 0.02f
        val half = (rk.width() / 2 - s * 0.03f)
        reset().color = Color.parseColor("#2F3135")
        c.drawRect(RectF(rk.centerX() - half, ly - s * 0.009f, rk.centerX() + half, ly + s * 0.009f), p)
        if (g > 0f) {
            spill(c, rk, s * 0.045f, rk.centerX(), ly, s * 0.3f, Color.WHITE, g * 0.12f)
            val lit = half * g.coerceIn(0f, 1f)
            reset().shader = LinearGradient(rk.centerX() - lit, 0f, rk.centerX() + lit, 0f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#E8E8EA"), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            p.setShadowLayer(s * 0.04f * g, 0f, 0f, Color.argb(alpha(g, 230), 255, 255, 255))
            c.drawRect(RectF(rk.centerX() - lit, ly - s * 0.009f, rk.centerX() + lit, ly + s * 0.009f), p)
        }
        return RectF(f)
    }

    // oak frame with real-looking grain, a soft white rocker and a warm orange dot
    private fun oak(c: Canvas, f: RectF, m: Float, g: Float): RectF {
        val s = f.width()
        val rr = s * 0.08f
        reset().shader = LinearGradient(f.left, f.top, f.right, f.bottom,
            intArrayOf(Color.parseColor("#C8955C"), Color.parseColor("#A8743F"), Color.parseColor("#C48F55"), Color.parseColor("#9C6A37")),
            null, Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(120, 0, 0, 0))
        c.drawRoundRect(f, rr, rr, p)

        path.reset()
        path.addRoundRect(f, rr, rr, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        // gently wavy grain lines, darker and lighter
        val rnd = java.util.Random(7)
        var gy = f.top - s * 0.1f
        while (gy < f.bottom + s * 0.1f) {
            reset().style = Paint.Style.STROKE
            p.strokeWidth = s * (0.004f + rnd.nextFloat() * 0.008f)
            p.color = if (rnd.nextBoolean()) Color.argb(40 + rnd.nextInt(30), 80, 45, 15) else Color.argb(25, 255, 240, 210)
            val g = Path().apply {
                moveTo(f.left, gy)
                cubicTo(f.left + s * 0.3f, gy - s * 0.04f, f.left + s * 0.6f, gy + s * 0.06f, f.right, gy + s * 0.02f)
            }
            c.drawPath(g, p)
            gy += s * (0.025f + rnd.nextFloat() * 0.03f)
        }
        c.restore()

        val ins = s * 0.155f
        val rk = RectF(f.left + ins, f.top + ins, f.right - ins, f.bottom - ins)
        reset().shader = LinearGradient(0f, rk.top, 0f, rk.bottom,
            mix(m, "#FFFFFF", "#E9EBEE"), mix(m, "#EEF0F2", "#FFFFFF"), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.025f, 0f, s * 0.012f, Color.argb(70, 0, 0, 0))
        c.drawRoundRect(rk, s * 0.07f, s * 0.07f, p)
        gloss(c, rk, s * 0.07f, 0.35f)
        bevel(c, rk, s * 0.07f, s * 0.008f, light = 180, dark = 45)
        bevel(c, f, rr, s * 0.01f, light = 60, dark = 70)

        val dy = rk.bottom - s * 0.1f
        spill(c, rk, s * 0.07f, rk.centerX(), dy, s * 0.22f, Color.rgb(255, 150, 60), g * 0.3f)
        reset().color = mix(g, "#D4D7DA", "#FF9A3C")
        if (g > 0f) p.setShadowLayer(s * 0.05f * g, 0f, 0f, Color.argb(alpha(g, 255), 255, 138, 31))
        c.drawCircle(rk.centerX(), dy, s * 0.024f, p)
        return RectF(f)
    }
}
