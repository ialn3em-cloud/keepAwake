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
        SwitchStyle.ROUND -> round(c, r, t)
        SwitchStyle.FACTORY -> factory(c, r, t)
        SwitchStyle.MARBLE -> marble(c, r, t)
        SwitchStyle.KIDS -> kids(c, r, t)
        SwitchStyle.BLACK -> black(c, r, t)
        SwitchStyle.OAK -> oak(c, r, t)
    }

    /** On the factory model, a tap above this line is the green (on) button, below it the red (off) one. */
    fun factorySplit(r: RectF) = r.top + r.width() * 156f / 170f

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

    // dark plate with a round push button and a ring that lights up blue
    private fun round(c: Canvas, f: RectF, t: Float): RectF {
        val s = f.width()
        reset().shader = LinearGradient(f.left, f.top, f.left, f.bottom,
            Color.parseColor("#2D2F33"), Color.parseColor("#18191B"), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.05f, 0f, s * 0.03f, Color.argb(150, 0, 0, 0))
        c.drawRoundRect(f, s * 0.12f, s * 0.12f, p)

        val cx = f.centerX()
        val cy = f.centerY()
        val kr = s * 0.26f * (1f - 0.02f * t) // pressed in a touch while on
        reset().shader = android.graphics.RadialGradient(cx, cy - kr * 0.3f, kr * 1.2f,
            Color.parseColor("#3B3E43"), Color.parseColor("#222428"), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, kr, p)

        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.035f
        p.color = mix(t, "#45484D", "#2F9BFF")
        if (t > 0f) p.setShadowLayer(s * 0.08f * t, 0f, 0f, Color.argb((255 * t).toInt(), 47, 155, 255))
        c.drawCircle(cx, cy, kr, p)

        powerIcon(c, cx, cy, s * 0.09f, s * 0.028f, mix(t, "#777777", "#7CC4FF"),
            if (t > 0f) Color.argb((200 * t).toInt(), 47, 155, 255) else 0, s * 0.04f * t)
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
    private fun factory(c: Canvas, f: RectF, t: Float): RectF {
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
        dotMatrix(c, screen, if (t > 0.5f) "ON" else "OFF", on = t > 0.5f)

        reset().shader = LinearGradient(0f, f.top + 74 * k, 0f, f.top + 244 * k,
            Color.parseColor("#FFCC33"), Color.parseColor("#E6A400"), Shader.TileMode.CLAMP)
        p.setShadowLayer(6 * k, 0f, 4 * k, Color.argb(110, 0, 0, 0))
        c.drawRoundRect(r(12f, 74f, w - 12f, 244f), 12 * k, 12 * k, p)

        reset().color = Color.parseColor("#151617")
        c.drawRoundRect(r(28f, 84f, w - 28f, 234f), 12 * k, 12 * k, p)

        // a pressed button sits a little lower and darker
        fun button(rect: RectF, top: String, bottom: String, pressed: Float) {
            rect.offset(0f, 3 * k * pressed)
            val dim = 1f - 0.2f * pressed
            fun shade(hex: String): Int {
                val col = Color.parseColor(hex)
                return Color.rgb((Color.red(col) * dim).toInt(), (Color.green(col) * dim).toInt(), (Color.blue(col) * dim).toInt())
            }
            reset().shader = LinearGradient(0f, rect.top, 0f, rect.bottom, shade(top), shade(bottom), Shader.TileMode.CLAMP)
            if (pressed < 0.5f) p.setShadowLayer(3 * k, 0f, 4 * k * (1 - pressed), Color.argb(110, 0, 0, 0))
            c.drawRoundRect(rect, 7 * k, 7 * k, p)
        }
        val green = r(36f, 92f, w - 36f, 154f)
        val redB = r(36f, 158f, w - 36f, 220f)
        button(green, "#3AC06A", "#1F8F48", t)
        button(redB, "#E5483B", "#B4271D", 1f - t)
        if (t > 0f) {
            reset().style = Paint.Style.STROKE
            p.strokeWidth = 2 * k
            p.color = Color.argb((180 * t).toInt(), 61, 255, 134)
            p.setShadowLayer(14 * k * t, 0f, 0f, Color.argb((255 * t).toInt(), 61, 255, 134))
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
    private fun dotMatrix(c: Canvas, screen: RectF, text: String, on: Boolean) {
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
            reset().color = Color.parseColor(
                if (lit[row][col]) (if (on) "#3DFF7A" else "#FF3B30") else (if (on) "#0E2414" else "#2A1010")
            )
            if (lit[row][col]) {
                p.setShadowLayer(dot * 1.6f, 0f, 0f, if (on) Color.argb(200, 34, 255, 102) else Color.argb(200, 255, 40, 30))
            }
            c.drawCircle(ox + col * pitch, oy + row * pitch, dot, p)
        }
    }

    // white Carrara marble with grey and gold veins, a gold rocker and a light line
    private fun marble(c: Canvas, f: RectF, t: Float): RectF {
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
        val topC = ev.evaluate(t, light, dark) as Int
        val botC = ev.evaluate(t, dark, light) as Int
        reset().shader = LinearGradient(0f, rk.top, 0f, rk.bottom,
            intArrayOf(topC, topC, botC, botC), floatArrayOf(0f, 0.48f, 0.52f, 1f), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.035f, 0f, s * 0.02f, Color.argb(90, 0, 0, 0))
        c.drawRoundRect(rk, s * 0.035f, s * 0.035f, p)

        val gw = s * 0.13f
        val gh = s * 0.024f
        val gy = rk.top - s * 0.09f
        reset().color = mix(t, "#BFB7A2", "#FFF4C2")
        if (t > 0f) p.setShadowLayer(s * 0.05f * t, 0f, 0f, Color.argb((255 * t).toInt(), 255, 216, 90))
        c.drawRoundRect(RectF(f.centerX() - gw / 2, gy, f.centerX() + gw / 2, gy + gh), gh / 2, gh / 2, p)
        return RectF(f)
    }

    // a candy-coloured switch with a face: asleep when off, wakes up and smiles when on
    private fun kids(c: Canvas, f: RectF, t: Float): RectF {
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
            mix(t, "#CDD3FF", "#FFF38A"), mix(t, "#A9B2FF", "#FFD23F"), Shader.TileMode.CLAMP)
        c.drawRoundRect(rk, 34 * k, 34 * k, p)

        val eyeY = rk.top + 52 * k
        val eyeH = (4 + 12 * t) * k
        for (ex in floatArrayOf(-22f, 22f)) {
            val ecx = cx + ex * k
            reset().color = Color.parseColor("#3B2B4A")
            c.drawRoundRect(RectF(ecx - 8 * k, eyeY - eyeH / 2, ecx + 8 * k, eyeY + eyeH / 2), 8 * k, 8 * k, p)
            if (t > 0.6f) {
                reset().color = Color.WHITE
                c.drawCircle(ecx - 2.5f * k, eyeY - 2.5f * k, 2.5f * k, p)
            }
            if (t > 0f) {
                reset().color = Color.argb((140 * t).toInt(), 255, 120, 150)
                c.drawOval(RectF(ecx + ex * 0.45f * k - 7 * k, eyeY + 18 * k, ecx + ex * 0.45f * k + 7 * k, eyeY + 26 * k), p)
            }
        }
        val my = rk.top + 82 * k
        if (t > 0.5f) {
            reset().color = Color.parseColor("#FF6B8B")
            c.drawArc(RectF(cx - 17 * k, my - 17 * k, cx + 17 * k, my + 17 * k), 0f, 180f, true, p)
        } else {
            reset().color = Color.parseColor("#7A6FB0")
            c.drawRoundRect(RectF(cx - 9 * k, my, cx + 9 * k, my + 5 * k), 2.5f * k, 2.5f * k, p)
        }
        if (t < 1f) {
            reset().color = Color.argb((255 * (1 - t)).toInt(), 122, 111, 176)
            p.textSize = 16 * k
            p.isFakeBoldText = true
            c.drawText("z z", f.right - 46 * k, f.top + 30 * k, p)
        }
        return RectF(f)
    }

    // matte black with a soft white line under the rocker when on
    private fun black(c: Canvas, f: RectF, t: Float): RectF {
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
            mix(t, "#1D1E21", "#2A2B2F"), mix(t, "#2A2B2F", "#1B1C1F"), Shader.TileMode.CLAMP)
        c.drawRoundRect(rk, s * 0.045f, s * 0.045f, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.005f
        p.color = Color.parseColor("#333539")
        c.drawRoundRect(rk, s * 0.045f, s * 0.045f, p)

        powerIcon(c, rk.centerX(), rk.centerY(), s * 0.065f, s * 0.02f, mix(t, "#4A4C50", "#E9E9EA"),
            if (t > 0f) Color.argb((220 * t).toInt(), 255, 255, 255) else 0, s * 0.04f * t)

        val ly = rk.bottom - s * 0.02f
        reset().shader = LinearGradient(rk.left, 0f, rk.right, 0f,
            intArrayOf(Color.TRANSPARENT, mix(t, "#2F3135", "#E8E8EA"), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        if (t > 0f) p.setShadowLayer(s * 0.04f * t, 0f, 0f, Color.argb((230 * t).toInt(), 255, 255, 255))
        c.drawRect(RectF(rk.left + s * 0.03f, ly - s * 0.009f, rk.right - s * 0.03f, ly + s * 0.009f), p)
        return RectF(f)
    }

    // oak frame with real-looking grain, a soft white rocker and a warm orange dot
    private fun oak(c: Canvas, f: RectF, t: Float): RectF {
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
            mix(t, "#FFFFFF", "#E9EBEE"), mix(t, "#EEF0F2", "#FFFFFF"), Shader.TileMode.CLAMP)
        p.setShadowLayer(s * 0.025f, 0f, s * 0.012f, Color.argb(70, 0, 0, 0))
        c.drawRoundRect(rk, s * 0.07f, s * 0.07f, p)
        reset().style = Paint.Style.STROKE
        p.strokeWidth = s * 0.005f
        p.color = Color.parseColor("#D8DBDE")
        c.drawRoundRect(rk, s * 0.07f, s * 0.07f, p)

        reset().color = mix(t, "#D4D7DA", "#FF9A3C")
        if (t > 0f) p.setShadowLayer(s * 0.05f * t, 0f, 0f, Color.argb((255 * t).toInt(), 255, 138, 31))
        c.drawCircle(rk.centerX(), rk.bottom - s * 0.1f, s * 0.024f, p)
        return RectF(f)
    }
}
