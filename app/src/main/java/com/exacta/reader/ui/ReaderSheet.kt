package com.exacta.reader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.exacta.reader.data.BookCss
import com.exacta.reader.data.Store
import kotlin.math.abs

/**
 * The reader's bottom sheet, as on Exacta.OS: a progress line with the chapter buttons and a
 * track to drag, one panel, and the tabs under it (Mục lục, Chữ, Trang, Khác). Nothing scrolls:
 * the contents are turned a page at a time.
 */
@SuppressLint("ViewConstructor")
class ReaderSheet(context: Context, private val host: Host) : View(context) {
    interface Host {
        fun progress(): Double
        fun progressLabel(): String
        fun seek(progression: Double)
        fun chapter(delta: Int)
        fun toc(): List<Pair<String, Int>>   // title, depth
        fun tocCurrent(): Int
        fun openToc(index: Int)
        fun applyPrefs(rebuild: Boolean)
        fun go(where: String)                // "home", "library", "tuvi"
    }

    var tab = 1
    private var tocPage = -1
    private var ink: Ink? = null
    private val hits = Hits()
    private var track = RectF()
    private var dragging = false
    private var dragP = 0.0
    private var downX = 0f
    private var downY = 0f

    init {
        setBackgroundColor(Color.WHITE)
        isClickable = true
    }

    /** Called when the sheet opens: the contents page shows the current chapter. */
    fun opened() {
        tocPage = -1
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        ink = Ink(context, w)
    }

    override fun onDraw(c: Canvas) {
        val k = ink ?: return
        val u = k.u
        val w = width.toFloat()
        val h = height.toFloat()
        hits.list.clear()
        c.drawRect(0f, 0f, w, 2 * u, k.fill)
        val m = Ink.MARGIN * u

        // Progress: the label, then ‹ track ›.
        var y = 14 * u
        val p = if (dragging) dragP else host.progress()
        val label = if (dragging) "Tới ${(dragP * 100).toInt()}%" else host.progressLabel()
        k.centered(c, label, m, w - 2 * m, y, k.smallBold)
        y += k.lineH(k.smallBold) + 8 * u
        val bh = 46 * u
        val prev = RectF(m, y, m + 64 * u, y + bh)
        val next = RectF(w - m - 64 * u, y, w - m, y + bh)
        k.button(c, prev, "‹", k.paint(26f, bold = true))
        k.button(c, next, "›", k.paint(26f, bold = true))
        hits.add(prev) { host.chapter(-1) }
        hits.add(next) { host.chapter(+1) }
        val tx0 = prev.right + 22 * u
        val tx1 = next.left - 22 * u
        val ty = y + bh / 2
        track = RectF(tx0 - 10 * u, y - 6 * u, tx1 + 10 * u, y + bh + 6 * u)
        c.drawRoundRect(tx0, ty - 1.5f * u, tx1, ty + 1.5f * u, u, u, k.gray)
        val kx = tx0 + (tx1 - tx0) * p.toFloat().coerceIn(0f, 1f)
        c.drawRoundRect(tx0, ty - 2 * u, kx, ty + 2 * u, 2 * u, 2 * u, k.fill)
        c.drawCircle(kx, ty, 11 * u, k.fill)
        c.drawCircle(kx, ty, 5 * u, k.white)
        y += bh + 14 * u
        c.drawRect(m, y, w - m, y + u, k.gray)
        y += 14 * u

        // The tabs along the bottom.
        val tabH = 82 * u
        val tabTop = h - tabH - 6 * u
        c.drawRect(m, tabTop - 8 * u, w - m, tabTop - 7 * u, k.gray)
        val names = listOf("toc" to "Mục lục", "font" to "Chữ", "page" to "Trang", "more" to "Khác")
        val sw = (w - 2 * m) / names.size
        for ((i, pair) in names.withIndex()) {
            val slot = RectF(m + i * sw, tabTop, m + (i + 1) * sw, tabTop + tabH)
            if (i == tab) c.drawRoundRect(k.inset(slot, 8 * u), 14 * u, 14 * u, k.stroke(2.2f))
            val icon = 28 * u
            val lp = if (i == tab) k.smallBold else k.small
            val total = icon + 4 * u + k.lineH(lp)
            val iy = slot.top + (slot.height() - total) / 2
            k.icon(c, pair.first, slot.centerX() - icon / 2, iy, icon, if (i == tab) Color.BLACK else Ink.DARK)
            k.centered(c, pair.second, slot.left, slot.width(), iy + icon + 4 * u, lp)
            hits.add(slot) {
                if (tab != i) {
                    tab = i
                    invalidate()
                }
            }
        }

        // The panel.
        val panel = RectF(m, y, w - m, tabTop - 20 * u)
        when (tab) {
            0 -> contents(c, k, panel)
            1 -> letters(c, k, panel)
            2 -> page(c, k, panel)
            else -> more(c, k, panel)
        }
    }

    /** One labelled row of choices; returns nothing, registers taps. */
    private fun choices(c: Canvas, k: Ink, r: RectF, label: String, items: List<String>, chosen: Int, onPick: (Int) -> Unit) {
        val u = k.u
        val lw = 118 * u
        k.text(c, label, r.left, r.centerY() - k.lineH(k.smallBold) / 2, k.smallBold)
        val gap = 8 * u
        val bw = (r.width() - lw - (items.size - 1) * gap) / items.size
        for ((i, item) in items.withIndex()) {
            val b = RectF(r.left + lw + i * (bw + gap), r.top, r.left + lw + i * (bw + gap) + bw, r.bottom)
            k.button(c, b, item, if (i == chosen) k.smallBold else k.small, selected = i == chosen)
            hits.add(b) {
                onPick(i)
                invalidate()
            }
        }
    }

    private fun rows(k: Ink, panel: RectF, n: Int): List<RectF> {
        val u = k.u
        val rowH = minOf(56 * u, (panel.height() - (n - 1) * 12 * u) / n)
        val gap = if (n > 1) minOf(28 * u, (panel.height() - n * rowH) / (n - 1)) else 0f
        val top = panel.top + maxOf(0f, (panel.height() - n * rowH - (n - 1) * gap) / 2)
        return (0 until n).map { RectF(panel.left, top + it * (rowH + gap), panel.right, top + it * (rowH + gap) + rowH) }
    }

    private fun letters(c: Canvas, k: Ink, panel: RectF) {
        val r = rows(k, panel, 3)
        val fonts = listOf("Bitter" to "Bitter", "Original" to "Gốc", "serif" to "Có chân", "sans-serif" to "Không chân")
        choices(c, k, r[0], "Font", fonts.map { it.second }, fonts.indexOfFirst { it.first == Store.fontFamily }) {
            Store.fontFamily = fonts[it].first
            host.applyPrefs(false)
        }
        val pct = (Store.fontSize * 100 + 0.5).toInt()
        choices(c, k, r[1], "Cỡ chữ", listOf("A−", "$pct%", "A+"), 1) {
            if (it == 0) Store.fontSize = (Store.fontSize - 0.1).coerceAtLeast(0.6)
            if (it == 2) Store.fontSize = (Store.fontSize + 0.1).coerceAtMost(3.0)
            host.applyPrefs(false)
        }
        choices(c, k, r[2], "Độ đậm", BookCss.WEIGHT_NAMES, Store.textWeight) {
            if (Store.textWeight != it) {
                Store.textWeight = it
                host.applyPrefs(true)
            }
        }
    }

    private fun page(c: Canvas, k: Ink, panel: RectF) {
        val r = rows(k, panel, 4)
        val lines = listOf(1.2, 1.4, 1.6, 1.8, 2.0)
        choices(c, k, r[0], "Giãn dòng", lines.map { "%.1f".format(java.util.Locale.US, it).replace('.', ',') },
            lines.indexOfFirst { abs(Store.lineHeight - it) < 0.05 }) {
            Store.lineHeight = lines[it]
            host.applyPrefs(false)
        }
        val margins = listOf(0.6 to "Hẹp", 1.0 to "Vừa", 1.5 to "Rộng")
        choices(c, k, r[1], "Lề", margins.map { it.second }, margins.indexOfFirst { abs(Store.margins - it.first) < 0.05 }) {
            Store.margins = margins[it].first
            host.applyPrefs(false)
        }
        choices(c, k, r[2], "Căn chữ", listOf("Đều hai bên", "Trái"), if (Store.justify) 0 else 1) {
            Store.justify = it == 0
            host.applyPrefs(false)
        }
        choices(c, k, r[3], "Chân trang", listOf("Đầy đủ", "Chỉ %", "Ẩn"), Store.footerMode.coerceIn(0, 2)) {
            Store.footerMode = it
            host.applyPrefs(false)
        }
    }

    private fun more(c: Canvas, k: Ink, panel: RectF) {
        val u = k.u
        val items = listOf(
            "Về Home" to { host.go("home") },
            "Thư viện" to { host.go("library") },
            "Tu vi" to { host.go("tuvi") },
            (if (Store.swapTaps) "Lật tới: chạm trái" else "Lật tới: chạm phải") to {
                Store.swapTaps = !Store.swapTaps
                Store.touch()
                invalidate()
            },
        )
        val gap = 14 * u
        val bw = (panel.width() - gap) / 2
        val bh = minOf(84 * u, (panel.height() - gap) / 2)
        val top = panel.top + maxOf(0f, (panel.height() - 2 * bh - gap) / 2)
        for ((i, pair) in items.withIndex()) {
            val r = RectF(panel.left + (i % 2) * (bw + gap), top + (i / 2) * (bh + gap), panel.left + (i % 2) * (bw + gap) + bw, top + (i / 2) * (bh + gap) + bh)
            k.button(c, r, pair.first, k.ui10Bold)
            hits.add(r) { pair.second() }
        }
    }

    private fun contents(c: Canvas, k: Ink, panel: RectF) {
        val u = k.u
        val toc = host.toc()
        if (toc.isEmpty()) {
            k.text(c, "Sách này không có mục lục.", panel.left, panel.top, k.ui10)
            return
        }
        val pagerH = 50 * u
        val rowH = 50 * u
        val per = ((panel.height() - pagerH - 10 * u) / rowH).toInt().coerceAtLeast(1)
        val pages = (toc.size + per - 1) / per
        val current = host.tocCurrent()
        if (tocPage < 0) tocPage = if (current >= 0) current / per else 0
        tocPage = tocPage.coerceIn(0, pages - 1)
        var y = panel.top
        for (i in tocPage * per until minOf(toc.size, (tocPage + 1) * per)) {
            val (title, depth) = toc[i]
            val r = RectF(panel.left, y, panel.right, y + rowH - 6 * u)
            if (i == current) k.card(c, r, selected = true)
            val p = if (i == current) k.ui10Bold else k.ui10
            val x = r.left + 14 * u + depth * 22 * u
            k.text(c, k.fit(title, r.right - 14 * u - x, p), x, r.top + (r.height() - k.lineH(p)) / 2, p)
            hits.add(r) { host.openToc(i) }
            y += rowH
        }
        if (pages > 1) {
            val py = panel.bottom - pagerH
            val bw = 120 * u
            val prev = RectF(panel.left, py, panel.left + bw, panel.bottom)
            val next = RectF(panel.right - bw, py, panel.right, panel.bottom)
            k.button(c, prev, "‹ Trước", k.smallBold, enabled = tocPage > 0)
            k.button(c, next, "Sau ›", k.smallBold, enabled = tocPage < pages - 1)
            k.centered(c, "Trang ${tocPage + 1}/$pages", prev.right, next.left - prev.right, py + (pagerH - k.lineH(k.small)) / 2, k.small)
            hits.add(prev) { if (tocPage > 0) { tocPage--; invalidate() } }
            hits.add(next) { if (tocPage < pages - 1) { tocPage++; invalidate() } }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val u = ink?.u ?: 1f
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                if (track.contains(e.x, e.y)) {
                    dragging = true
                    dragP = trackValue(e.x)
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                dragP = trackValue(e.x)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    dragging = false
                    host.seek(trackValue(e.x))
                    invalidate()
                } else if (abs(e.x - downX) > 70 * u && tab == 0) {
                    tocPage += if (e.x < downX) 1 else -1
                    invalidate()
                } else if (abs(e.x - downX) < 24 * u && abs(e.y - downY) < 24 * u) {
                    hits.list.lastOrNull { it.first.contains(e.x, e.y) }?.second?.invoke()
                }
            }
            MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }

    private fun trackValue(x: Float): Double {
        val u = ink?.u ?: 1f
        val x0 = track.left + 10 * u
        val x1 = track.right - 10 * u
        return ((x - x0) / (x1 - x0)).toDouble().coerceIn(0.0, 1.0)
    }
}
