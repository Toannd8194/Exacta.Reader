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
    private var fontMenu = false
    private var fontPage = 0
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
        fontMenu = false
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
        val bh = 48 * u
        val cbw = 128 * u
        val prev = RectF(m, y, m + cbw, y + bh)
        val next = RectF(w - m - cbw, y, w - m, y + bh)
        k.button(c, prev, "‹ Chương", k.ui10Bold)
        k.button(c, next, "Chương ›", k.ui10Bold)
        hits.add(prev) { host.chapter(-1) }
        hits.add(next) { host.chapter(+1) }
        val tx0 = prev.right + 26 * u
        val tx1 = next.left - 26 * u
        val ty = y + bh / 2
        track = RectF(tx0 - 10 * u, y - 6 * u, tx1 + 10 * u, y + bh + 6 * u)
        c.drawRoundRect(tx0, ty - 1.5f * u, tx1, ty + 1.5f * u, u, u, k.gray)
        val kx = tx0 + (tx1 - tx0) * p.toFloat().coerceIn(0f, 1f)
        c.drawRoundRect(tx0, ty - 2 * u, kx, ty + 2 * u, 2 * u, 2 * u, k.fill)
        c.drawCircle(kx, ty, 12 * u, k.fill)
        c.drawCircle(kx, ty, 5 * u, k.white)
        y += bh + 12 * u
        c.drawRect(m, y, w - m, y + u, k.gray)
        y += 12 * u

        // The tabs along the bottom.
        val tabH = 70 * u
        val tabTop = h - tabH - 6 * u
        c.drawRect(m, tabTop - 8 * u, w - m, tabTop - 7 * u, k.gray)
        val names = listOf("toc" to "Mục lục", "font" to "Chữ", "page" to "Trang", "more" to "Khác")
        val sw = (w - 2 * m) / names.size
        for ((i, pair) in names.withIndex()) {
            val slot = RectF(m + i * sw, tabTop, m + (i + 1) * sw, tabTop + tabH)
            if (i == tab) c.drawRoundRect(k.inset(slot, 8 * u), 14 * u, 14 * u, k.stroke(2.2f))
            val icon = 24 * u
            val lp = if (i == tab) k.smallBold else k.small
            val total = icon + 4 * u + k.lineH(lp)
            val iy = slot.top + (slot.height() - total) / 2
            k.icon(c, pair.first, slot.centerX() - icon / 2, iy, icon, if (i == tab) Color.BLACK else Ink.DARK)
            k.centered(c, pair.second, slot.left, slot.width(), iy + icon + 4 * u, lp)
            hits.add(slot) {
                if (tab != i) {
                    tab = i
                    morePage = 0
                    fontMenu = false
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
        val bw = 92 * u
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
        val rowH = minOf(50 * u, (panel.height() - (n - 1) * 10 * u) / n)
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
        val fonts = host.fonts()
        if (fontMenu) {
            fontList(c, k, panel, fonts)
            return
        }
        val r = rows(k, panel, 3)
        // Font: the name in a box that drops the list down; "Thêm" beside it.
        val lw = labelW(k)
        k.text(c, "Font", r[0].left, r[0].centerY() - k.lineH(k.smallBold) / 2, k.smallBold)
        val addW = 130 * u
        val box = RectF(r[0].left + lw, r[0].top, r[0].right - addW - 10 * u, r[0].bottom)
        val add = RectF(box.right + 10 * u, r[0].top, r[0].right, r[0].bottom)
        val name = fonts.firstOrNull { it.first == Store.fontFamily }?.second ?: Store.fontFamily
        k.button(c, box, "$name  ▾", k.ui10Bold)
        k.button(c, add, "+ Thêm", k.smallBold)
        hits.add(box) {
            fontMenu = true
            fontPage = 0
            invalidate()
        }
        hits.add(add) { host.addFont() }
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
            Store.textWeight = 3
            host.applyPrefs(false)
        }
    }

    /** The fonts dropped down over the panel, a page at a time; the current one shaded. */
    private fun fontList(c: Canvas, k: Ink, panel: RectF, fonts: List<Pair<String, String>>) {
        val u = k.u
        val head = RectF(panel.left, panel.top, panel.right, panel.top + 44 * u)
        k.text(c, "Chọn font", head.left, head.centerY() - k.lineH(k.ui10Bold) / 2, k.ui10Bold)
        val close = RectF(head.right - 120 * u, head.top, head.right, head.bottom)
        k.button(c, close, "Đóng", k.smallBold)
        hits.add(close) { fontMenu = false; invalidate() }
        val listTop = head.bottom + 10 * u
        val pagerH = 48 * u
        val rowH = 50 * u
        val per = ((panel.bottom - listTop - pagerH - 8 * u) / rowH).toInt().coerceAtLeast(1)
        val pages = (fonts.size + per - 1) / per
        fontPage = fontPage.coerceIn(0, pages - 1)
        var y = listTop
        for ((id, name) in fonts.drop(fontPage * per).take(per)) {
            val r = RectF(panel.left, y, panel.right, y + rowH - 6 * u)
            val on = id == Store.fontFamily
            if (on) k.card(c, r, selected = true) else c.drawRect(r.left, r.bottom + 2 * u, r.right, r.bottom + 3 * u, k.gray)
            k.text(c, (if (on) "✓  " else "    ") + name, r.left + 14 * u, r.centerY() - k.lineH(k.ui10) / 2, if (on) k.ui10Bold else k.ui10)
            hits.add(r) {
                Store.fontFamily = id
                fontMenu = false
                host.applyPrefs(false)
                invalidate()
            }
            y += rowH
        }
        if (pages > 1) pager(c, k, panel, fontPage, pages) { fontPage = it }
    }

    private fun page(c: Canvas, k: Ink, panel: RectF) {
        val r = rows(k, panel, 5)
        stepper(c, k, r[0], "Giãn dòng", num(Store.lineHeight), { d ->
            Store.lineHeight = step(Store.lineHeight, d, 0.1, 1.0, 2.6)
            host.applyPrefs(false)
        }) {
            Store.lineHeight = 1.7
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
        // Where to go, as four buttons with icons.
        val actions = listOf<Triple<String, String, () -> Unit>>(
            Triple("home", "Home") { host.go("home") },
            Triple("library", "Thư viện") { host.go("library") },
            Triple("bequan", if (Store.beQuanTarget > 0) "Đang bế quan" else "Bế quan") { host.go("bequan") },
            Triple("refresh", "Làm sạch") { host.refreshScreen() },
        )
        val gap = 10 * u
        val aw = (panel.width() - 3 * gap) / 4
        val ah = 56 * u
        for ((i, a) in actions.withIndex()) {
            val r = RectF(panel.left + i * (aw + gap), panel.top, panel.left + i * (aw + gap) + aw, panel.top + ah)
            k.button(c, r, "", k.small)
            val icon = 28 * u
            val total = icon + 4 * u + k.lineH(k.smallBold)
            val iy = r.top + (ah - total) / 2
            k.icon(c, a.first, r.centerX() - icon / 2, iy, icon)
            k.centered(c, a.second, r.left, r.width(), iy + icon + 4 * u, k.smallBold)
            hits.add(r) { a.third() }
        }

        // The options as a list: the name, the value on the right; a tap moves to the next value.
        val footers = listOf("Đầy đủ", "Chỉ %", "Ẩn")
        val refresh = listOf(0, 4, 8, 12, 20)
        val options = listOf<Triple<String, String, () -> Unit>>(
            Triple("Chạm để lật tới", if (Store.swapTaps) "Bên trái" else "Bên phải") { Store.swapTaps = !Store.swapTaps },
            Triple("Chân trang", footers[Store.footerMode.coerceIn(0, 2)]) { Store.footerMode = (Store.footerMode + 1) % 3 },
            Triple("Tên sách trên đầu trang", if (Store.showHeader) "Hiện" else "Ẩn") { Store.showHeader = !Store.showHeader },
            Triple("Ngắt từ cuối dòng", if (Store.hyphens) "Bật" else "Tắt") { Store.hyphens = !Store.hyphens },
            Triple("Tự làm sạch màn hình", if (Store.refreshEvery > 0) "Mỗi ${Store.refreshEvery} trang" else "Tắt") {
                Store.refreshEvery = refresh[(refresh.indexOf(Store.refreshEvery) + 1) % refresh.size]
            },
            Triple("Báo tin tu tiên ở chân trang", if (Store.showNotices) "Bật" else "Tắt") { Store.showNotices = !Store.showNotices },
        )
        val top = panel.top + ah + 10 * u
        val pagerH = 48 * u
        val rowH = 36 * u
        var per = ((panel.bottom - top) / rowH).toInt().coerceAtLeast(1)
        if (per < options.size) per = ((panel.bottom - top - pagerH - 6 * u) / rowH).toInt().coerceAtLeast(1)
        val pages = (options.size + per - 1) / per
        morePage = morePage.coerceIn(0, pages - 1)
        var y = top
        for (o in options.drop(morePage * per).take(per)) {
            val r = RectF(panel.left, y, panel.right, y + rowH)
            k.text(c, k.fit(o.first, r.width() * 0.6f, k.small), r.left + 6 * u, r.centerY() - k.lineH(k.small) / 2, k.small)
            val vw = k.smallBold.measureText(o.second) + 40 * u
            val pill = RectF(r.right - vw, r.top + 6 * u, r.right, r.bottom - 6 * u)
            c.drawRoundRect(pill, pill.height() / 2, pill.height() / 2, k.gray)
            k.centered(c, o.second, pill.left, pill.width(), pill.centerY() - k.lineH(k.smallBold) / 2, k.smallBold)
            c.drawRect(r.left, r.bottom - u, r.right, r.bottom, k.gray)
            hits.add(r) {
                o.third()
                host.applyPrefs(false)
                invalidate()
            }
            y += rowH
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
