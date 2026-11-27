package com.exacta.reader.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.BatteryManager
import androidx.core.content.res.ResourcesCompat
import com.exacta.reader.R
import java.time.LocalTime

/**
 * The Exacta.OS look on a canvas: Bitter type, black ink on white, rounded cards, a light-gray
 * selection, the nine-cell bar. Sizes are the firmware's pixels (the X4's ~215 ppi) turned into the
 * same physical size on this screen by `u`, times the reader's "Cỡ giao diện"; `W` is the width in
 * those units, so a 300-ppi screen holds more than the firmware's 480.
 */
class Ink(private val context: Context, width: Int) {
    val u = unit(context, width)
    val W = width / u
    private val ui = ResourcesCompat.getFont(context, R.font.bitter_ui) ?: Typeface.SERIF
    private val uiBold = ResourcesCompat.getFont(context, R.font.bitter_ui_bold) ?: Typeface.DEFAULT_BOLD
    private val title = ResourcesCompat.getFont(context, R.font.bitter_title) ?: Typeface.DEFAULT_BOLD

    fun paint(size: Float, bold: Boolean = false, big: Boolean = false, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (big) title else if (bold) uiBold else ui
        textSize = size * u
        this.color = color
    }

    val small get() = paint(SMALL)
    val smallBold get() = paint(SMALL, bold = true)
    val ui10 get() = paint(UI10)
    val ui10Bold get() = paint(UI10, bold = true)
    val ui12Bold get() = paint(UI12, bold = true)
    val big get() = paint(BIG, big = true)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
    val gray = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LIGHT; style = Paint.Style.FILL }
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    fun stroke(w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = w * u }
    private val pixels = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    fun lineH(p: Paint) = p.fontSpacing

    /** Text with its top at y (the firmware draws from the top of the line). */
    fun text(c: Canvas, s: String, x: Float, y: Float, p: Paint) = c.drawText(s, x, y - p.ascent(), p)

    fun centered(c: Canvas, s: String, x: Float, w: Float, y: Float, p: Paint) {
        val t = fit(s, w, p)
        text(c, t, x + (w - p.measureText(t)) / 2, y, p)
    }

    fun right(c: Canvas, s: String, xr: Float, y: Float, p: Paint) = text(c, s, xr - p.measureText(s), y, p)

    fun fit(s: String, w: Float, p: Paint): String {
        if (p.measureText(s) <= w) return s
        var n = s.length
        while (n > 0 && p.measureText(s.substring(0, n) + "…") > w) n--
        return s.substring(0, n).trimEnd() + "…"
    }

    /** Word wrap to at most `maxLines`; the last line ends in "…" when text is left over. */
    fun wrap(s: String, w: Float, p: Paint, maxLines: Int): List<String> {
        val words = s.split(' ').filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        var cur = ""
        var i = 0
        while (i < words.size) {
            val next = if (cur.isEmpty()) words[i] else "$cur ${words[i]}"
            if (p.measureText(next) <= w || cur.isEmpty()) {
                cur = next
                i++
            } else {
                lines.add(cur)
                cur = ""
                if (lines.size == maxLines) break
            }
        }
        if (cur.isNotEmpty() && lines.size < maxLines) lines.add(cur)
        if (i < words.size && lines.isNotEmpty()) lines[lines.size - 1] = fit(lines.last() + " " + words.drop(i).joinToString(" "), w, p)
        return lines.map { fit(it, w, p) }
    }

    /** One line, or two of even length (breaking at " · " or ", " when both halves fit). */
    fun evenLines(s: String, w: Float, p: Paint): List<String> {
        if (p.measureText(s) <= w) return listOf(s)
        for (mark in listOf(" · ", ", ")) {
            val at = s.indexOf(mark)
            if (at < 0) continue
            val a = s.substring(0, if (mark == ", ") at + 1 else at).trim()
            val b = s.substring(at + mark.length).trim()
            if (p.measureText(a) <= w && p.measureText(b) <= w) return listOf(a, b)
        }
        return wrap(s, w, p, 2)
    }

    fun card(c: Canvas, r: RectF, selected: Boolean = false) {
        val rad = RADIUS * u
        if (selected) c.drawRoundRect(r, rad, rad, gray)
        c.drawRoundRect(inset(r, u), rad, rad, stroke(if (selected) 3f else 2f))
    }

    fun inset(r: RectF, d: Float) = RectF(r.left + d, r.top + d, r.right - d, r.bottom - d)

    fun diamond(c: Canvas, cx: Float, cy: Float, rad: Float, p: Paint = fill) {
        val path = Path().apply {
            moveTo(cx, cy - rad); lineTo(cx + rad, cy); lineTo(cx, cy + rad); lineTo(cx - rad, cy); close()
        }
        c.drawPath(path, p)
    }

    // --- Pixel art -------------------------------------------------------------------------

    private fun bitmap(id: Int) = bitmaps.getOrPut(id) {
        BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
    }

    fun coverBitmap(path: String): Bitmap? {
        if (path.isEmpty() || path == "none") return null
        return covers.getOrPut(path) { if (java.io.File(path).exists()) runCatching { BitmapFactory.decodeFile(path) }.getOrNull() else null }
    }

    fun badge(c: Canvas, species: Int, x: Float, y: Float, size: Float, alpha: Int = 255) {
        val id = context.resources.getIdentifier("badge_" + species.coerceIn(0, 35).toString().padStart(2, '0'), "drawable", context.packageName)
        if (id != 0) c.drawBitmap(bitmap(id), null, RectF(x, y, x + size, y + size), if (alpha >= 255) pixels else Paint(pixels).apply { this.alpha = alpha })
    }

    fun realmIcon(c: Canvas, realm: Int, x: Float, y: Float, size: Float) {
        val id = context.resources.getIdentifier("realm_${realm.coerceIn(0, 9)}", "drawable", context.packageName)
        if (id != 0) c.drawBitmap(bitmap(id), null, RectF(x, y, x + size, y + size), pixels)
    }

    /** A grayscale drawing (res/drawable-nodpi/<name>.png) as it is, optionally faded. */
    fun art(c: Canvas, name: String, x: Float, y: Float, size: Float, alpha: Int = 255) {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return
        val p = if (alpha >= 255) pixels else Paint(pixels).apply { this.alpha = alpha }
        c.drawBitmap(bitmap(id), null, RectF(x, y, x + size, y + size), p)
    }

    /**
     * An ore of a grade (each its own stone, from a rough rock to the chaos star); with `bar`, a thin
     * progress bar under it, filled to `percent`.
     */
    fun ore(c: Canvas, tier: Int, percent: Int, x: Float, y: Float, size: Float, bar: Boolean = false, alpha: Int = 255) {
        val art = if (bar) size * 0.84f else size
        art(c, "art_ore_" + tier.coerceIn(0, 5), x + (size - art) / 2, y, art, alpha)
        if (bar) {
            val by = y + size - 5 * u
            c.drawRoundRect(x + size * 0.1f, by, x + size * 0.9f, by + 5 * u, 2.5f * u, 2.5f * u, gray)
            c.drawRoundRect(x + size * 0.1f, by, x + size * 0.1f + size * 0.8f * percent.coerceIn(0, 100) / 100f, by + 5 * u, 2.5f * u, 2.5f * u, fill)
        }
    }

    /**
     * Nine cells in three rising groups (sơ, trung, hậu kỳ) filled to `ninths`, the stage names under,
     * the current one bold. Returns the y below.
     */
    fun realmBar(c: Canvas, x: Float, y: Float, w: Float, ninths: Double, labels: List<String>, here: Int): Float {
        val heights = floatArrayOf(7f, 11f, 15f)
        val gg = 8 * u
        val cg = 3 * u
        val cw = (w - 2 * gg - 6 * cg) / 9
        val gw = 3 * cw + 2 * cg
        val x0 = x + (w - (3 * gw + 2 * gg)) / 2
        val base = y + 15 * u
        for (i in 0 until 9) {
            val g = i / 3
            val hh = heights[g] * u
            val cx = x0 + g * (gw + gg) + (i % 3) * (cw + cg)
            val r = RectF(cx, base - hh, cx + cw, base)
            val f = (ninths - i).coerceIn(0.0, 1.0).toFloat()
            if (f >= 1f) {
                c.drawRoundRect(r, 3 * u, 3 * u, fill)
            } else {
                c.drawRoundRect(inset(r, 0.5f * u), 3 * u, 3 * u, stroke(if (g == 2) 2f else 1.2f))
                if (f > 0) c.drawRect(cx + u, base - hh + u, cx + u + (cw - 2 * u) * f, base - u, fill)
            }
        }
        val p = small
        val pb = smallBold
        val ly = base + 5 * u
        for (g in 0 until 3) {
            val paint = if (g == here) pb else p
            val lab = fit(labels[g], gw + gg - 2 * u, paint)
            text(c, lab, x0 + g * (gw + gg) + (gw - paint.measureText(lab)) / 2, ly, paint)
        }
        return ly + lineH(p)
    }

    // --- Controls ------------------------------------------------------------------------------

    /** An outlined button; `selected` fills it black with white text; disabled is gray. */
    fun button(c: Canvas, r: RectF, label: String, p: Paint = ui10Bold, enabled: Boolean = true, selected: Boolean = false) {
        val rad = 10 * u
        if (selected) {
            c.drawRoundRect(r, rad, rad, fill)
        } else {
            c.drawRoundRect(r, rad, rad, white)
            c.drawRoundRect(inset(r, u), rad, rad, stroke(if (enabled) 2f else 1.2f).apply { if (!enabled) color = LIGHT })
        }
        val tp = Paint(p).apply { color = if (selected) Color.WHITE else if (enabled) Color.BLACK else DARK }
        centered(c, label, r.left + 6 * u, r.width() - 12 * u, r.centerY() - lineH(tp) / 2, tp)
    }

    /** One tab of a tab row: the chosen one black with white bold text. */
    fun tab(c: Canvas, r: RectF, label: String, selected: Boolean) {
        val rad = r.height() / 2
        if (selected) {
            c.drawRoundRect(r, rad, rad, fill)
        } else {
            c.drawRoundRect(inset(r, 0.8f * u), rad, rad, stroke(1.6f))
        }
        val p = paint(SMALL + 1f, bold = selected, color = if (selected) Color.WHITE else Color.BLACK)
        centered(c, label, r.left + 8 * u, r.width() - 16 * u, r.centerY() - lineH(p) / 2, p)
    }

    /** The app's logo (res/drawable-nodpi/logo.png). */
    fun logo(c: Canvas, x: Float, y: Float, size: Float) {
        val id = context.resources.getIdentifier("logo", "drawable", context.packageName)
        if (id != 0) c.drawBitmap(bitmap(id), null, RectF(x, y, x + size, y + size), pixels)
    }

    /** A drawable icon (res/drawable-nodpi/ic_<name>.png), drawn black. */
    fun icon(c: Canvas, name: String, x: Float, y: Float, size: Float, color: Int = Color.BLACK) {
        val id = context.resources.getIdentifier("ic_$name", "drawable", context.packageName)
        if (id == 0) return
        val p = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        c.drawBitmap(bitmap(id), null, RectF(x, y, x + size, y + size), p)
    }

    /** A section title with a rule under it, from x to x + w; returns the y below. */
    fun section(c: Canvas?, x: Float, y: Float, w: Float, title: String): Float {
        val p = smallBold
        if (c != null) {
            text(c, fit(title, w, p), x, y, p)
            c.drawRect(x, y + lineH(p) + 2 * u, x + w, y + lineH(p) + 3 * u, fill)
        }
        return y + lineH(p) + 12 * u
    }

    /** Label on the left, value on the right, one line. */
    fun pair(c: Canvas, label: String, value: String, x: Float, y: Float, w: Float, p: Paint = small, vp: Paint = smallBold) {
        text(c, label, x, y, p)
        right(c, fit(value, w - p.measureText(label) - 16 * u, vp), x + w, y, vp)
    }

    /** A cover image (or a gray block with the title) cropped to fill r. */
    fun cover(c: Canvas, path: String, title: String, r: RectF) {
        val bmp = coverBitmap(path)
        val rad = 6 * u
        if (bmp != null) {
            val scale = maxOf(r.width() / bmp.width, r.height() / bmp.height)
            val sw = r.width() / scale
            val sh = r.height() / scale
            val sx = (bmp.width - sw) / 2
            val sy = (bmp.height - sh) / 2
            c.save()
            c.clipRect(r)
            c.drawBitmap(bmp, android.graphics.Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt()), r, pixels)
            c.restore()
        } else {
            c.drawRoundRect(r, rad, rad, gray)
            val p = if (r.width() < 110 * u) paint(SMALL - 2f, bold = true) else smallBold
            var y = r.top + 10 * u
            for (line in wrap(title, r.width() - 14 * u, p, 6)) {
                if (y + lineH(p) > r.bottom - 6 * u) break
                centered(c, line, r.left, r.width(), y, p)
                y += lineH(p)
            }
        }
        c.drawRoundRect(inset(r, 0.6f * u), rad, rad, stroke(1.4f))
    }

    // --- Small vector marks for the Omega systems --------------------------------------------

    /** Thiên kiếp: a cloud and a bolt. */
    fun bolt(c: Canvas, x: Float, y: Float, s: Float) {
        c.drawCircle(x + s * 0.32f, y + s * 0.34f, s * 0.2f, fill)
        c.drawCircle(x + s * 0.56f, y + s * 0.26f, s * 0.24f, fill)
        c.drawCircle(x + s * 0.76f, y + s * 0.38f, s * 0.17f, fill)
        c.drawRect(x + s * 0.18f, y + s * 0.36f, x + s * 0.9f, y + s * 0.52f, fill)
        val p = Path().apply {
            moveTo(x + s * 0.52f, y + s * 0.5f); lineTo(x + s * 0.34f, y + s * 0.78f); lineTo(x + s * 0.5f, y + s * 0.76f)
            lineTo(x + s * 0.4f, y + s * 1.0f); lineTo(x + s * 0.7f, y + s * 0.66f); lineTo(x + s * 0.54f, y + s * 0.68f)
            lineTo(x + s * 0.66f, y + s * 0.5f); close()
        }
        c.drawPath(p, fill)
    }

    /** Bí cảnh: a moon gate in a wall. */
    fun gate(c: Canvas, x: Float, y: Float, s: Float) {
        c.drawRect(x, y + s * 0.18f, x + s, y + s, fill)
        c.drawCircle(x + s * 0.5f, y + s * 0.6f, s * 0.3f, white)
        c.drawRect(x + s * 0.2f, y + s * 0.6f, x + s * 0.8f, y + s, white)
        val roof = Path().apply { moveTo(x - s * 0.06f, y + s * 0.2f); lineTo(x + s * 0.5f, y); lineTo(x + s * 1.06f, y + s * 0.2f); close() }
        c.drawPath(roof, fill)
        c.drawRect(x + s * 0.47f, y + s * 0.62f, x + s * 0.53f, y + s, fill)
    }

    /** Đạo hiệu: a square seal with a carved frame. */
    fun seal(c: Canvas, x: Float, y: Float, s: Float, on: Boolean = true) {
        val r = RectF(x, y, x + s, y + s)
        if (on) {
            c.drawRoundRect(r, s * 0.14f, s * 0.14f, fill)
            c.drawRoundRect(inset(r, s * 0.14f), s * 0.06f, s * 0.06f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = s * 0.07f })
            c.drawRect(x + s * 0.3f, y + s * 0.34f, x + s * 0.7f, y + s * 0.42f, white)
            c.drawRect(x + s * 0.3f, y + s * 0.58f, x + s * 0.7f, y + s * 0.66f, white)
            c.drawRect(x + s * 0.46f, y + s * 0.3f, x + s * 0.54f, y + s * 0.7f, white)
        } else {
            c.drawRoundRect(inset(r, s * 0.04f), s * 0.14f, s * 0.14f, stroke(1.4f))
        }
    }

    /** Seven day cells (Monday first): filled when the day counts, outlined today. */
    fun week(c: Canvas, x: Float, y: Float, w: Float, h: Float, met: List<Boolean>, todayIndex: Int) {
        val gap = 4 * u
        val cw = (w - 6 * gap) / 7
        for (i in 0 until 7) {
            val r = RectF(x + i * (cw + gap), y, x + i * (cw + gap) + cw, y + h)
            when {
                met.getOrElse(i) { false } -> c.drawRoundRect(r, 3 * u, 3 * u, fill)
                i < todayIndex -> c.drawRoundRect(r, 3 * u, 3 * u, gray)
                else -> c.drawRoundRect(inset(r, 0.6f * u), 3 * u, 3 * u, stroke(if (i == todayIndex) 2.2f else 1.1f))
            }
        }
    }

    // --- Chrome ----------------------------------------------------------------------------

    /** Clock, a centre text, battery; a rule under. Returns the y below the rule. */
    fun status(c: Canvas, width: Float, centre: String): Float {
        val p = ui10Bold
        val y = STATUS_TEXT_Y * u
        val now = LocalTime.now()
        text(c, "%02d:%02d".format(now.hour, now.minute), MARGIN * u, y, p)
        val battery = batteryPercent()
        val bat = if (battery >= 0) "$battery%" else ""
        right(c, bat, width - MARGIN * u, y, p)
        centered(c, centre, MARGIN * u + 70 * u, width - 2 * MARGIN * u - 140 * u, y, p)
        c.drawRect(MARGIN * u, STATUS_H * u, width - MARGIN * u, STATUS_H * u + u, fill)
        return STATUS_H * u
    }

    /** A page title under the status line, with an optional label on the right. */
    fun title(c: Canvas, width: Float, s: String, right: String? = null) {
        val p = big
        val y = TITLE_TOP * u
        if (right != null) {
            val r = ui10
            right(c, right, width - MARGIN * u, y + (-p.ascent()) - (-r.ascent()), r)
        }
        text(c, fit(s, width - 2 * MARGIN * u - (right?.let { ui10.measureText(it) + 12 * u } ?: 0f), p), MARGIN * u, y, p)
    }

    private fun batteryPercent(): Int {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return if (level < 0) -1 else level * 100 / scale.coerceAtLeast(1)
    }

    companion object {
        // Decoded images, shared by every screen (an e-ink app opens few at a time).
        private val bitmaps = HashMap<Int, Bitmap>()
        private val covers = object : LinkedHashMap<String, Bitmap?>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap?>?) = size > 40
        }

        /** Set by the screen tests to draw at a given density. */
        var dpiOverride = 0f

        /** Pixels per firmware pixel: the screen's real density against the X4's, times the UI size. */
        fun unit(context: Context, width: Int = 0): Float {
            val dm = context.resources.displayMetrics
            val dpi = when {
                dpiOverride > 0f -> dpiOverride
                dm.xdpi in 180f..480f -> dm.xdpi
                dm.densityDpi in 180..480 -> dm.densityDpi.toFloat()
                else -> 300f
            }
            // 1.1: the firmware's sizes read a little small at arm's length on a 6" screen.
            var u = dpi / 215f * 1.1f * com.exacta.reader.data.Store.uiScale.toFloat()
            if (width > 0) u = u.coerceAtMost(width / 380f)  // keep about the firmware's room
            return u.coerceAtLeast(0.6f)
        }

        const val SMALL = 16.7f
        const val UI10 = 20.8f
        const val UI12 = 25f
        const val BIG = 37.5f
        const val MARGIN = 20f
        const val GAP = 14f
        const val RADIUS = 14f
        const val STATUS_TEXT_Y = 14f
        const val STATUS_H = 44f
        const val TITLE_TOP = 56f
        const val CONTENT_TOP = 112f
        val LIGHT = Color.rgb(205, 205, 205)
        val DARK = Color.rgb(110, 110, 110)
    }
}
