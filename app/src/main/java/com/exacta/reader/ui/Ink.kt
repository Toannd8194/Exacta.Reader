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
 * selection, the nine-cell bar. Sizes follow the firmware's 480-px-wide layout, scaled by `u`.
 */
class Ink(private val context: Context, width: Int) {
    val u = width / 480f
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
    private val pixels = Paint().apply { isFilterBitmap = false; isAntiAlias = false }

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

    private val bitmaps = HashMap<Int, Bitmap>()
    private fun bitmap(id: Int) = bitmaps.getOrPut(id) {
        BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
    }

    fun badge(c: Canvas, species: Int, x: Float, y: Float, size: Float) {
        val id = context.resources.getIdentifier("badge_%02d".format(species.coerceIn(0, 35)), "drawable", context.packageName)
        if (id != 0) c.drawBitmap(bitmap(id), null, RectF(x, y, x + size, y + size), pixels)
    }

    fun realmIcon(c: Canvas, realm: Int, x: Float, y: Float, size: Float) {
        val id = context.resources.getIdentifier("realm_${realm.coerceIn(0, 9)}", "drawable", context.packageName)
        if (id != 0) c.drawBitmap(bitmap(id), null, RectF(x, y, x + size, y + size), pixels)
    }

    /** An ore being forged: a cut stone whose lower part fills with the progress. */
    fun ore(c: Canvas, tier: Int, percent: Int, x: Float, y: Float, size: Float) {
        val path = Path().apply {
            moveTo(x + size * 0.5f, y + size * 0.06f)
            lineTo(x + size * 0.9f, y + size * 0.32f)
            lineTo(x + size * 0.8f, y + size * 0.86f)
            lineTo(x + size * 0.2f, y + size * 0.86f)
            lineTo(x + size * 0.1f, y + size * 0.32f)
            close()
        }
        c.save()
        c.clipPath(path)
        val level = y + size * 0.86f - (size * 0.8f) * percent / 100f
        c.drawRect(x, level, x + size, y + size, if (tier >= 3) fill else gray)
        c.restore()
        c.drawPath(path, stroke(2.5f))
        c.drawLine(x + size * 0.1f, y + size * 0.32f, x + size * 0.9f, y + size * 0.32f, stroke(1.5f))
        c.drawLine(x + size * 0.5f, y + size * 0.06f, x + size * 0.38f, y + size * 0.32f, stroke(1.5f))
        c.drawLine(x + size * 0.5f, y + size * 0.06f, x + size * 0.62f, y + size * 0.32f, stroke(1.5f))
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
