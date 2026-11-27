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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The reader's bottom sheet, as on Exacta.OS: a progress line with the chapter buttons and a
 * track to drag, one panel, and the tabs under it (Mục lục, Chữ, Trang, Khác). Every reading
 * setting lives here; sizes step with − and +. Nothing scrolls: the contents turn a page at a time.
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
        fun fonts(): List<Pair<String, String>>  // font id, name shown
        fun addFont()
        fun refreshScreen()
    }

    var tab = 1
    private var tocPage = -1
    private var morePage = 0
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
        var y = 12 * u
        val p = if (dragging) dragP else host.progress()
        val label = if (dragging) "Tới ${(dragP * 100).toInt()}%" else host.progressLabel()
        k.centered(c, label, m, w - 2 * m, y, k.smallBold)
        y += k.lineH(k.smallBold) + 6 * u
        val bh = 44 * u
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
        y += bh + 12 * u
        c.drawRect(m, y, w - m, y + u, k.gray)
        y += 12 * u

        // The tabs along the bottom.
        val tabH = 78 * u
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
                    morePage = 0
                    invalidate()
                }
            }
        }

        // The panel.
        val panel = RectF(m, y, w - m, tabTop - 18 * u)
        when (tab) {
            0 -> contents(c, k, panel)
            1 -> letters(c, k, panel)
            2 -> page(c, k, panel)
            else -> more(c, k, panel)
        }
    }

    // --- Rows ---------------------------------------------------------------------------------

    private fun labelW(k: Ink) = 132 * k.u

    /** A row of choices; the chosen one black. */
    private fun choices(c: Canvas, k: Ink, r: RectF, label: String, items: List<String>, chosen: Int, onPick: (Int) -> Unit) {
        val u = k.u
        val lw = labelW(k)
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

    /** A value with − and + on either side (and an optional reset in the middle). */
    private fun stepper(c: Canvas, k: Ink, r: RectF, label: String, value: String, onStep: (Int) -> Unit, onReset: (() -> Unit)? = null) {
        val u = k.u
        val lw = labelW(k)
        k.text(c, label, r.left, r.centerY() - k.lineH(k.smallBold) / 2, k.smallBold)
        val bw = 96 * u
        val minus = RectF(r.left + lw, r.top, r.left + lw + bw, r.bottom)
        val plus = RectF(r.right - bw, r.top, r.right, r.bottom)
        val mid = RectF(minus.right + 10 * u, r.top, plus.left - 10 * u, r.bottom)
        k.button(c, minus, "−", k.paint(24f, bold = true))
        k.button(c, plus, "+", k.paint(24f, bold = true))
        c.drawRoundRect(mid, 10 * u, 10 * u, k.gray)
        k.centered(c, value, mid.left, mid.width(), mid.centerY() - k.lineH(k.ui10Bold) / 2, k.ui10Bold)
        hits.add(minus) { onStep(-1); invalidate() }
        hits.add(plus) { onStep(+1); invalidate() }
        if (onReset != null) hits.add(mid) { onReset(); invalidate() }
    }

    private fun rows(k: Ink, panel: RectF, n: Int): List<RectF> {
        val u = k.u
        val rowH = minOf(56 * u, (panel.height() - (n - 1) * 10 * u) / n)
        val gap = if (n > 1) minOf(26 * u, (panel.height() - n * rowH) / (n - 1)) else 0f
        val top = panel.top + maxOf(0f, (panel.height() - n * rowH - (n - 1) * gap) / 2)
        return (0 until n).map { RectF(panel.left, top + it * (rowH + gap), panel.right, top + it * (rowH + gap) + rowH) }
    }

    private fun pct(v: Double) = "${(v * 100).roundToInt()}%"
    private fun num(v: Double) = String.format(Locale.US, "%.1f", v).replace('.', ',')
    private fun step(v: Double, d: Int, by: Double, lo: Double, hi: Double) = (Math.round((v + d * by) / by) * by).coerceIn(lo, hi)

    // --- Tabs ---------------------------------------------------------------------------------

    private fun letters(c: Canvas, k: Ink, panel: RectF) {
        val u = k.u
        val r = rows(k, panel, 4)
        // Font: ‹ name ›, through the built-in fonts and those added.
        val fonts = host.fonts()
        val at = fonts.indexOfFirst { it.first == Store.fontFamily }.coerceAtLeast(0)
        stepper(c, k, r[0], "Font", fonts.getOrNull(at)?.second ?: Store.fontFamily, { d ->
            Store.fontFamily = fonts[(at + d + fonts.size) % fonts.size].first
            host.applyPrefs(false)
        })
        stepper(c, k, r[1], "Cỡ chữ", pct(Store.fontSize), { d ->
            Store.fontSize = step(Store.fontSize, d, 0.05, 0.5, 3.0)
            host.applyPrefs(false)
        }) {
            Store.fontSize = 1.0
            host.applyPrefs(false)
        }
        stepper(c, k, r[2], "Độ đậm", BookCss.WEIGHT_NAMES[Store.textWeight.coerceIn(0, 5)], { d ->
            Store.textWeight = (Store.textWeight + d).coerceIn(0, 5)
            host.applyPrefs(false)
        }) {
            Store.textWeight = 1
            host.applyPrefs(false)
        }
        val add = RectF(r[3].left + labelW(k), r[3].top, r[3].right, r[3].bottom)
        k.text(c, "Thêm font", r[3].left, r[3].centerY() - k.lineH(k.smallBold) / 2, k.smallBold)
        k.button(c, add, "Chọn tệp TTF/OTF…  (hoặc chép vào ExactaReader/fonts)", k.small)
        hits.add(add) { host.addFont() }
    }

    private fun page(c: Canvas, k: Ink, panel: RectF) {
        val r = rows(k, panel, 5)
        stepper(c, k, r[0], "Giãn dòng", num(Store.lineHeight), { d ->
            Store.lineHeight = step(Store.lineHeight, d, 0.1, 1.0, 2.6)
            host.applyPrefs(false)
        }) {
            Store.lineHeight = 1.6
            host.applyPrefs(false)
        }
        stepper(c, k, r[1], "Lề hai bên", pct(Store.margins), { d ->
            Store.margins = step(Store.margins, d, 0.1, 0.0, 3.0)
            host.applyPrefs(false)
        }) {
            Store.margins = 1.0
            host.applyPrefs(false)
        }
        val ps = Store.paraSpacing
        stepper(c, k, r[2], "Giãn đoạn", if (ps == null) "Theo sách" else num(ps), { d ->
            Store.paraSpacing = step(ps ?: 0.5, if (ps == null) 0 else d, 0.1, 0.0, 2.5)
            host.applyPrefs(false)
        }) {
            Store.paraSpacing = null
            host.applyPrefs(false)
        }
        choices(c, k, r[3], "Căn chữ", listOf("Đều hai bên", "Trái"), if (Store.justify) 0 else 1) {
            Store.justify = it == 0
            host.applyPrefs(false)
        }
        choices(c, k, r[4], "Thụt đầu dòng", listOf("Tắt", "Bật"), if (Store.indent) 1 else 0) {
            Store.indent = it == 1
            host.applyPrefs(false)
        }
    }

    private fun more(c: Canvas, k: Ink, panel: RectF) {
        val u = k.u
        val footers = listOf("Chân trang: đầy đủ", "Chân trang: chỉ %", "Chân trang: ẩn")
        val refresh = listOf(0, 4, 8, 12, 20)
        val items = listOf<Pair<String, () -> Unit>>(
            "‹ Về Home" to { host.go("home") },
            "Thư viện" to { host.go("library") },
            (if (Store.swapTaps) "Lật tới: chạm trái" else "Lật tới: chạm phải") to { Store.swapTaps = !Store.swapTaps; host.applyPrefs(false) },
            footers[Store.footerMode.coerceIn(0, 2)] to { Store.footerMode = (Store.footerMode + 1) % 3; host.applyPrefs(false) },
            (if (Store.showHeader) "Tên sách trên đầu: hiện" else "Tên sách trên đầu: ẩn") to { Store.showHeader = !Store.showHeader; host.applyPrefs(false) },
            (if (Store.hyphens) "Ngắt từ cuối dòng: bật" else "Ngắt từ cuối dòng: tắt") to { Store.hyphens = !Store.hyphens; host.applyPrefs(false) },
            (if (Store.refreshEvery > 0) "Làm sạch mỗi ${Store.refreshEvery} trang" else "Tự làm sạch: tắt") to {
                Store.refreshEvery = refresh[(refresh.indexOf(Store.refreshEvery) + 1) % refresh.size]
                host.applyPrefs(false)
            },
            "Làm sạch màn hình ngay" to { host.refreshScreen() },
            (if (Store.showNotices) "Báo tin tu tiên: bật" else "Báo tin tu tiên: tắt") to { Store.showNotices = !Store.showNotices; host.applyPrefs(false) },
            "Tu vi" to { host.go("tuvi") },
        )
        val gap = 12 * u
        val bw = (panel.width() - gap) / 2
        val rowsPer = ((panel.height() + gap) / (60 * u + gap)).toInt().coerceIn(1, 5)
        val per = rowsPer * 2
        val pages = (items.size + per - 1) / per
        morePage = morePage.coerceIn(0, pages - 1)
        val pagerH = if (pages > 1) 48 * u + gap else 0f
        val bh = minOf(70 * u, (panel.height() - pagerH - (rowsPer - 1) * gap) / rowsPer)
        for ((i, pair) in items.drop(morePage * per).take(per).withIndex()) {
            val r = RectF(panel.left + (i % 2) * (bw + gap), panel.top + (i / 2) * (bh + gap), panel.left + (i % 2) * (bw + gap) + bw, panel.top + (i / 2) * (bh + gap) + bh)
            k.button(c, r, pair.first, k.smallBold)
            hits.add(r) {
                pair.second()
                invalidate()
            }
        }
        if (pages > 1) pager(c, k, panel, morePage, pages) { morePage = it }
    }

    private fun pager(c: Canvas, k: Ink, panel: RectF, page: Int, pages: Int, set: (Int) -> Unit) {
        val u = k.u
        val pagerH = 48 * u
        val py = panel.bottom - pagerH
        val bw = 120 * u
        val prev = RectF(panel.left, py, panel.left + bw, panel.bottom)
        val next = RectF(panel.right - bw, py, panel.right, panel.bottom)
        k.button(c, prev, "‹ Trước", k.smallBold, enabled = page > 0)
        k.button(c, next, "Sau ›", k.smallBold, enabled = page < pages - 1)
        k.centered(c, "Trang ${page + 1}/$pages", prev.right, next.left - prev.right, py + (pagerH - k.lineH(k.small)) / 2, k.small)
        hits.add(prev) { if (page > 0) { set(page - 1); invalidate() } }
        hits.add(next) { if (page < pages - 1) { set(page + 1); invalidate() } }
    }

    private fun contents(c: Canvas, k: Ink, panel: RectF) {
        val u = k.u
        val toc = host.toc()
        if (toc.isEmpty()) {
            k.text(c, "Sách này không có mục lục.", panel.left, panel.top, k.ui10)
            return
        }
        val pagerH = 48 * u
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
        if (pages > 1) pager(c, k, panel, tocPage, pages) { tocPage = it }
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
                } else if (abs(e.x - downX) > 70 * u && (tab == 0 || tab == 3)) {
                    val d = if (e.x < downX) 1 else -1
                    if (tab == 0) tocPage += d else morePage = (morePage + d).coerceAtLeast(0)
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
