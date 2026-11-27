package com.exacta.reader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.exacta.reader.data.Book
import com.exacta.reader.data.Store
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.game.Lunar
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Texts
import com.exacta.reader.game.Treasure
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Home, layout A2 of Exacta.OS: the book being read on top; under it a rail (Pháp bảo with a large
 * treasure, then Thư viện, Tu vi, Cài đặt) and the cultivation card. The lunar date sits in the
 * status line. Every block is a touch target.
 */
@SuppressLint("ViewConstructor")
class HomeView(context: Context, private val onAction: (Action, String?) -> Unit) : View(context) {
    enum class Action { READ, LIBRARY, PHAP_BAO, TU_VI, SETTINGS, PERMISSION, DAO_HIEU }

    var book: Book? = null
    var needsPermission = false
    private var ink: Ink? = null
    private val targets = ArrayList<Pair<RectF, () -> Unit>>()
    private val covers = HashMap<String, Bitmap?>()

    // Picked again on every visit, like the firmware.
    private var scripture = 0
    private var practice = 0
    private var termPhrase = 0
    private var proverb = 0

    fun shuffle() {
        scripture = Random.nextInt(Texts.SCRIPTURES.size)
        practice = Random.nextInt(Texts.PRACTICES.size)
        termPhrase = Random.nextInt(Texts.TERM_PHRASES.size)
        proverb = Random.nextInt(Texts.PROVERBS.size)
    }

    init {
        setBackgroundColor(Color.WHITE)
        shuffle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        ink = Ink(context, w)
    }

    /** After a change of the UI size. */
    fun rescale() {
        if (width > 0) ink = Ink(context, width)
        invalidate()
    }

    private fun on(r: RectF, action: Action, path: String? = null) = targets.add(r to { onAction(action, path) })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            targets.firstOrNull { it.first.contains(event.x, event.y) }?.second?.invoke()
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val k = ink ?: return
        val u = k.u
        val w = width.toFloat()
        val h = height.toFloat()
        val hu = h / u
        targets.clear()
        val today = LocalDate.now()
        val lunar = Lunar.fromSolar(today.dayOfMonth, today.monthValue, today.year)
        k.status(c, w, Lunar.ancientMonthDay(lunar))

        val m = Ink.MARGIN * u
        val g = Ink.GAP * u
        var top = (Ink.STATUS_H + 12) * u
        val bookH = (hu * 0.27f).coerceIn(210f, 320f) * u
        val bookRect = RectF(m, top, w - m, top + bookH)
        drawBook(c, k, bookRect)
        val b = book
        if (needsPermission) on(bookRect, Action.PERMISSION) else if (b != null) on(bookRect, Action.READ, b.path) else on(bookRect, Action.LIBRARY)
        top = bookRect.bottom + g

        // Other books read lately, when there is room.
        val others = if (needsPermission) emptyList() else Store.recent(6).filter { it.path != b?.path }
        if (others.isNotEmpty() && hu >= 900) {
            val stripH = 128 * u
            drawRecent(c, k, RectF(m, top, w - m, top + stripH), others)
            top += stripH + g
        }

        // The three Omega cards along the bottom, the rail and the cultivation card above them.
        val miniH = 148 * u
        val miniTop = h - 10 * u - miniH
        val miniW = (w - 2 * m - 2 * g) / 3
        val cards = listOf(
            RectF(m, miniTop, m + miniW, miniTop + miniH),
            RectF(m + miniW + g, miniTop, m + 2 * miniW + g, miniTop + miniH),
            RectF(m + 2 * (miniW + g), miniTop, w - m, miniTop + miniH),
        )
        drawTrial(c, k, cards[0]); on(cards[0], Action.TU_VI)
        drawBiCanh(c, k, cards[1], today); on(cards[1], Action.TU_VI)
        drawTitle(c, k, cards[2]); on(cards[2], Action.DAO_HIEU)

        val lowerBottom = miniTop - g
        val railW = 108 * u
        drawRail(c, k, RectF(m, top, m + railW, lowerBottom))
        val card = RectF(m + railW + g, top, w - m, lowerBottom)
        drawCultivation(c, k, card, today, lunar)
        on(card, Action.TU_VI)
    }

    private fun coverOf(b: Book): Bitmap? = covers.getOrPut(b.cover) {
        if (b.cover.isNotEmpty() && File(b.cover).exists()) runCatching { BitmapFactory.decodeFile(b.cover) }.getOrNull() else null
    }

    // --- The book ---------------------------------------------------------------------------

    private fun drawBook(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        k.card(c, r)
        val b = book
        if (needsPermission || b == null) {
            val p = k.ui12Bold
            val lines = if (needsPermission) listOf("Cần quyền đọc thẻ nhớ", "Chạm để cấp quyền") else listOf("Chưa đọc cuốn nào", "Chạm để mở Thư viện")
            val y = r.centerY() - k.lineH(p)
            k.centered(c, lines[0], r.left, r.width(), y, p)
            k.centered(c, lines[1], r.left, r.width(), y + k.lineH(p) + 4 * u, k.ui10)
            return
        }
        val pad = 16 * u
        val coverH = r.height() - 2 * pad
        val coverW = coverH * 0.67f
        val cr = RectF(r.left + pad, r.top + pad, r.left + pad + coverW, r.top + pad + coverH)
        drawCover(c, k, b, cr)

        val tx = cr.right + pad
        val tw = r.right - pad - tx
        val ui10b = k.ui10Bold
        val small = k.small
        val smallBold = k.smallBold
        val readY = r.top + 16 * u
        k.centered(c, Texts.SCRIPTURES[scripture], tx, tw, readY, ui10b)
        val ruleY = readY + k.lineH(ui10b) + 4 * u
        c.drawRect(tx + tw / 2 - 24 * u, ruleY, tx + tw / 2 + 24 * u, ruleY + 2 * u, k.fill)
        k.diamond(c, tx + tw / 2 - 32 * u, ruleY + u, 3 * u)
        k.diamond(c, tx + tw / 2 + 32 * u, ruleY + u, 3 * u)

        // Pinned to the bottom: the tầng and its nine cells, then the call to read on.
        val contY = r.bottom - 18 * u - k.lineH(ui10b)
        val barH = 12 * u
        val barY = contY - 10 * u - barH
        val labelY = barY - 6 * u - k.lineH(small)
        val pct = b.percent
        val tang = Realm.tangOf(pct)
        val label = if (tang >= 10) "Đại viên mãn · 100%" else "Tu đến ${Realm.tangName(tang)} · $pct%"
        k.centered(c, label, tx, tw, labelY, smallBold)
        val gap = 3 * u
        val cellW = (tw - 8 * gap) / 9
        val x0 = tx + (tw - (9 * cellW + 8 * gap)) / 2
        val filled = pct * 9 / 100.0
        for (i in 0 until 9) {
            val cx = x0 + i * (cellW + gap)
            val cell = RectF(cx, barY, cx + cellW, barY + barH)
            val f = (filled - i).coerceIn(0.0, 1.0).toFloat()
            if (f >= 1f) {
                c.drawRoundRect(cell, 3 * u, 3 * u, k.fill)
            } else {
                c.drawRoundRect(k.inset(cell, 0.5f * u), 3 * u, 3 * u, k.stroke(1.2f))
                if (f > 0) c.drawRect(cx + u, barY + u, cx + u + (cellW - 2 * u) * f, barY + barH - u, k.fill)
            }
        }
        k.centered(c, Texts.PRACTICES[practice] + " ›", tx, tw, contY, ui10b)

        // Title and author between the rule and the tầng, centred.
        var y = ruleY + 12 * u
        val bottom = labelY - 8 * u
        val tp = k.ui12Bold
        val maxTitle = ((bottom - y) / k.lineH(tp)).toInt().coerceIn(1, 4)
        for (line in k.wrap(b.title, tw, tp, maxTitle)) {
            k.centered(c, line, tx, tw, y, tp)
            y += k.lineH(tp)
        }
        val info = bookInfo(b)
        val infoH = if (info.isNotEmpty()) 8 * u + small.fontSpacing else 0f
        if (b.author.isNotEmpty() && y + 6 * u + k.lineH(k.ui10) + infoH <= bottom) {
            y += 6 * u
            for (line in k.wrap(b.author, tw, k.ui10, ((bottom - infoH - y) / k.lineH(k.ui10)).toInt().coerceIn(1, 2))) {
                k.centered(c, line, tx, tw, y, k.ui10)
                y += k.lineH(k.ui10)
            }
        }
        if (info.isNotEmpty() && y + infoH <= bottom) {
            y += 8 * u
            k.centered(c, info, tx, tw, y, k.paint(Ink.SMALL, color = Ink.DARK))
        }
    }

    private fun drawCover(c: Canvas, k: Ink, b: Book, r: RectF) {
        val u = k.u
        val bmp = coverOf(b)
        c.drawRoundRect(r, 8 * u, 8 * u, k.white)
        if (bmp != null) {
            // Fill the slot, cropping the overflow.
            val scale = maxOf(r.width() / bmp.width, r.height() / bmp.height)
            val sw = r.width() / scale
            val sh = r.height() / scale
            val sx = (bmp.width - sw) / 2
            val sy = (bmp.height - sh) / 2
            c.save()
            c.clipRect(r)
            c.drawBitmap(bmp, android.graphics.Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt()), r, Paint(Paint.FILTER_BITMAP_FLAG))
            c.restore()
        } else {
            c.drawRoundRect(r, 8 * u, 8 * u, k.gray)
            val p = if (r.width() < 90 * u) k.smallBold else k.ui10Bold
            var y = r.top + 12 * u
            for (line in k.wrap(b.title, r.width() - 12 * u, p, 5)) {
                k.centered(c, line, r.left, r.width(), y, p)
                y += k.lineH(p)
            }
        }
        c.drawRoundRect(k.inset(r, u), 8 * u, 8 * u, k.stroke(2f))
    }

    // --- The rail ---------------------------------------------------------------------------

    private fun drawRail(c: Canvas, k: Ink, col: RectF) {
        val u = k.u
        val gap = 10 * u
        val iconCell = (col.height() * 0.17f).coerceIn(56 * u, 68 * u)
        val pbH = col.height() - 3 * (iconCell + gap)
        val pb = RectF(col.left, col.top, col.right, col.top + pbH)
        k.card(c, pb)
        on(pb, Action.PHAP_BAO)
        val smallBold = k.smallBold
        val small = k.small
        val line = k.lineH(small)
        val label = "Pháp bảo" + if (Store.unseen > 0) " •${Store.unseen}" else ""
        val beast = Store.strongest()
        val ore = Store.forge.firstOrNull()
        val caption = when {
            beast != null -> beast.name
            ore != null -> "${Treasure.TIER_NAMES[ore.tier]} · ${ore.percent}%"
            else -> "Lò luyện trống"
        }
        val name = k.wrap(caption, pb.width() - 8 * u, small, 3)
        val nameH = name.size * line
        val size = (pb.height() - line - nameH - 24 * u).coerceAtMost(pb.width() - 8 * u).coerceAtLeast(32 * u)
        val stack = line + 4 * u + size + 6 * u + nameH
        var y = pb.top + maxOf(6 * u, (pbH - stack) / 2)
        k.centered(c, label, pb.left, pb.width(), y, smallBold)
        y += line + 4 * u
        val ax = pb.left + (pb.width() - size) / 2
        when {
            beast != null -> k.badge(c, beast.species, ax, y, size)
            ore != null -> k.ore(c, ore.tier, ore.percent, ax, y, size)
            else -> k.ore(c, 0, 0, ax, y, size)
        }
        y += size + 6 * u
        for (l in name) {
            k.centered(c, l, pb.left, pb.width(), y, small)
            y += line
        }

        var cy = pb.bottom + gap
        val items = listOf(Action.LIBRARY to "Thư viện", Action.TU_VI to "Tu vi", Action.SETTINGS to "Cài đặt")
        for ((action, text) in items) {
            val cell = RectF(col.left, cy, col.right, cy + iconCell)
            k.card(c, cell)
            on(cell, action)
            val icon = 28 * u
            val total = icon + 3 * u + line
            val iy = cell.top + (iconCell - total) / 2
            drawIcon(c, k, action, cell.centerX() - icon / 2, iy, icon)
            k.centered(c, text, cell.left, cell.width(), iy + icon + 3 * u, smallBold)
            cy += iconCell + gap
        }
    }

    private fun drawIcon(c: Canvas, k: Ink, action: Action, x: Float, y: Float, s: Float) {
        val p = k.stroke(2.4f)
        when (action) {
            Action.LIBRARY -> {  // three spines and one leaning
                for ((i, hh) in listOf(0.8f, 0.95f, 0.85f).withIndex()) {
                    c.drawRect(x + s * (0.06f + i * 0.2f), y + s * (1 - hh), x + s * (0.2f + i * 0.2f), y + s, k.fill)
                }
                c.drawLine(x + s * 0.72f, y + s, x + s * 0.92f, y + s * 0.18f, k.stroke(4f))
            }
            Action.TU_VI -> {  // a mountain under the sun
                val path = Path().apply { moveTo(x, y + s); lineTo(x + s * 0.4f, y + s * 0.3f); lineTo(x + s * 0.62f, y + s * 0.62f); lineTo(x + s * 0.76f, y + s * 0.45f); lineTo(x + s, y + s); close() }
                c.drawPath(path, k.fill)
                c.drawCircle(x + s * 0.82f, y + s * 0.16f, s * 0.12f, p)
            }
            else -> {  // two sliders
                for ((i, kx) in listOf(0.3f, 0.68f).withIndex()) {
                    val yy = y + s * (0.3f + i * 0.42f)
                    c.drawLine(x, yy, x + s, yy, k.stroke(2.2f))
                    c.drawCircle(x + s * kx, yy, s * 0.13f, k.white)
                    c.drawCircle(x + s * kx, yy, s * 0.13f, p)
                }
            }
        }
    }

    // --- Cultivation ------------------------------------------------------------------------

    private fun drawCultivation(c: Canvas, k: Ink, r: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val words = Store.tuVi()
        val level = Store.level()
        k.card(c, r)
        drawRealmFrame(c, k, r, Realm.realmOf(level))
        val x = r.left + 16 * u
        val w = r.width() - 32 * u
        val small = k.small
        val smallBold = k.smallBold
        val sl = k.lineH(small)
        val bigP = k.big
        val bl = k.lineH(bigP)
        val hintY = r.bottom - 14 * u - sl
        val top = r.top + 16 * u
        val bottom = hintY - 8 * u

        val term = Lunar.termName(Lunar.solarTerm(today.dayOfMonth, today.monthValue, today.year)) + " · " + Texts.TERM_PHRASES[termPhrase]
        val head = k.evenLines((occasion(today, lunar) ?: greeting()) + " · " + term, w, smallBold)
        val headH = head.size * sl
        val realmH = bl + 2 * u + sl + 6 * u + 15 * u + 5 * u + sl
        val statsH = 1 * u + 10 * u + 2 * sl
        val saying = k.wrap(Texts.PROVERBS[proverb], w, small, 4)
        val sayingH = 12 * u + saying.size * sl
        var blocks = 3
        var used = headH + realmH + statsH
        val withSaying = used + sayingH + (blocks + 2) * 10 * u <= bottom - top
        if (withSaying) {
            used += sayingH
            blocks++
        }
        val spare = maxOf(0f, bottom - top - used)
        val gap = minOf(36 * u, spare / (blocks + 1))
        var y = top + maxOf(0f, (spare - gap * (blocks - 1)) / 2)

        for (l in head) {
            k.centered(c, l, x, w, y, smallBold)
            y += sl
        }
        y += gap

        // The realm logo and the words read, large; the realm and its road.
        val number = Realm.van(words)
        val unit = k.ui12Bold
        val icon = minOf(40 * u, bl)
        val nw = bigP.measureText(number)
        val uw = unit.measureText("vạn từ")
        val nx = x + maxOf(0f, (w - (icon + 10 * u + nw + 8 * u + uw)) / 2)
        k.realmIcon(c, Realm.realmOf(level), nx, y + (bl - icon) / 2, icon)
        k.text(c, number, nx + icon + 10 * u, y, bigP)
        k.text(c, "vạn từ", nx + icon + 10 * u + nw + 8 * u, y + (-bigP.ascent()) - (-unit.ascent()), unit)
        y += bl + 2 * u
        k.centered(c, Realm.name(level), x, w, y, smallBold)
        y += sl + 6 * u
        y = k.realmBar(c, x + 8 * u, y, w - 16 * u, Realm.ninths(words, level), Realm.stageLabels(level), Realm.stageIndex(level))
        y += gap

        // Today, the last seven days, the streak.
        c.drawRect(x, y, x + w, y + u, k.fill)
        y += 10 * u
        val d = Store.today()
        val cols = mutableListOf(
            duration(Store.daySeconds[d] ?: 0) to "hôm nay",
            hours(Store.secondsBetween(d - 6, d)) to "7 ngày",
            "${Store.streak()} ngày" to "chuỗi",
        )
        if (w >= 380 * u) cols.add((if (Store.pace() > 0) "${Store.pace()}" else "–") to "từ/phút")
        val colW = w / cols.size
        for ((i, pair) in cols.withIndex()) {
            val cx = x + i * colW
            if (i > 0) c.drawRect(cx, y + 2 * u, cx + u, y + 2 * sl - 2 * u, k.fill)
            k.centered(c, pair.first, cx, colW, y, smallBold)
            k.centered(c, pair.second, cx, colW, y + sl, small)
        }
        y += 2 * sl

        if (withSaying) {
            y += gap
            var sy = y + maxOf(0f, (bottom - y - sayingH) / 2)
            c.drawRect(x + (w - 40 * u) / 2, sy, x + (w + 40 * u) / 2, sy + 2 * u, k.fill)
            sy += 12 * u
            val sp = k.paint(Ink.SMALL, color = Ink.DARK)
            for (l in saying) {
                k.centered(c, l, x, w, sy, sp)
                sy += sl
            }
        }
        k.centered(c, "Tu vi & thành tích ›", x, w, hintY, smallBold)
    }

    // --- Recent books --------------------------------------------------------------------------

    private fun drawRecent(c: Canvas, k: Ink, r: RectF, books: List<Book>) {
        val u = k.u
        val g = 10 * u
        val n = ((r.width() + g) / (230 * u + g)).toInt().coerceIn(1, 4).coerceAtMost(books.size)
        val cw = (r.width() - (n - 1) * g) / n
        val small = k.small
        val sb = k.smallBold
        for (i in 0 until n) {
            val b = books[i]
            val cell = RectF(r.left + i * (cw + g), r.top, r.left + i * (cw + g) + cw, r.bottom)
            k.card(c, cell)
            on(cell, Action.READ, b.path)
            val pad = 10 * u
            val ch = cell.height() - 2 * pad
            val cr = RectF(cell.left + pad, cell.top + pad, cell.left + pad + ch * 0.67f, cell.bottom - pad)
            drawCover(c, k, b, cr)
            val tx = cr.right + pad
            val tw = cell.right - pad - tx
            var y = cell.top + pad + 2 * u
            for (line in k.wrap(b.title, tw, sb, 3)) {
                k.text(c, line, tx, y, sb)
                y += k.lineH(sb)
            }
            val pct = b.percent
            val bottomY = cell.bottom - pad - k.lineH(small)
            k.text(c, k.fit(if (b.finishDay >= 0) "Đại viên mãn" else "${Realm.tangName(Realm.tangOf(pct))} · $pct%", tw, small), tx, bottomY, small)
            val barY = bottomY - 8 * u
            c.drawRect(tx, barY, tx + tw, barY + 2 * u, k.gray)
            c.drawRect(tx, barY - u, tx + tw * pct / 100f, barY + 3 * u, k.fill)
        }
    }

    /** "Đã đọc 2 giờ 05 phút · còn ~3 giờ 10" for the book card. */
    private fun bookInfo(b: Book): String {
        val parts = mutableListOf<String>()
        if (b.seconds >= 60) parts.add("Đã đọc ${duration(b.seconds)}")
        timeLeft(b)?.let { parts.add("còn $it") }
        if (parts.isEmpty() && b.words > 0) parts.add("${Realm.van(b.words)} vạn từ")
        return parts.joinToString(" · ")
    }

    // --- Omega cards: thiên kiếp, bí cảnh, đạo hiệu ----------------------------------------------

    /** The card's head: an icon and a title; returns the y below it. */
    private fun miniHead(c: Canvas, k: Ink, r: RectF, title: String, icon: (Float, Float, Float) -> Unit): Float {
        val u = k.u
        k.card(c, r)
        val pad = 12 * u
        val s = 26 * u
        icon(r.left + pad, r.top + pad, s)
        val p = k.smallBold
        k.text(c, k.fit(title, r.width() - 3 * pad - s, p), r.left + pad + s + 8 * u, r.top + pad + (s - k.lineH(p)) / 2, p)
        return r.top + pad + s + 10 * u
    }

    private fun lines(c: Canvas, k: Ink, r: RectF, y0: Float, items: List<Pair<String, Boolean>>) {
        val u = k.u
        var y = y0
        val pad = 12 * u
        for ((text, bold) in items) {
            val p = if (bold) k.smallBold else k.small
            if (y + k.lineH(p) > r.bottom - 8 * u) break
            k.text(c, k.fit(text, r.width() - 2 * pad, p), r.left + pad, y, p)
            y += k.lineH(p)
        }
    }

    private fun drawTrial(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        var y = miniHead(c, k, r, "Thiên kiếp") { x, yy, s -> k.bolt(c, x, yy, s) }
        val pad = 12 * u
        val gate = Realm.baseName(Store.nextGate())
        when {
            Store.realmCap >= 9 -> lines(c, k, r, y, listOf("Đã phi thăng" to true, "Tiên lộ thênh thang" to false))
            Store.inTrial() -> {
                val days = Store.trialDays().first
                val need = Store.trialNeed()
                // One cell per day of the trial.
                val cw = minOf(22 * u, (r.width() - 2 * pad - (need - 1) * 4 * u) / need)
                for (i in 0 until need) {
                    val cell = RectF(r.left + pad + i * (cw + 4 * u), y, r.left + pad + i * (cw + 4 * u) + cw, y + 12 * u)
                    if (i < days) c.drawRoundRect(cell, 3 * u, 3 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 3 * u, 3 * u, k.stroke(1.2f))
                }
                y += 20 * u
                lines(c, k, r, y, listOf(
                    "Độ kiếp vào $gate" to true,
                    "Hôm nay ${Store.minutes(Store.today())}/${Store.trialMinutes()} phút" to false,
                ))
            }
            Store.failDay == Store.today() -> lines(c, k, r, y, listOf("Độ kiếp thất bại" to true, "Tu vi đã lui, đọc tiếp" to false))
            else -> lines(c, k, r, y, listOf(
                "Kiếp tới: $gate" to true,
                "Còn ${Realm.van(Store.wordsToGate())} vạn từ" to false,
                "${Store.trialNeed()} ngày × ${Store.trialMinutes()} phút" to false,
            ))
        }
    }

    private fun drawBiCanh(c: Canvas, k: Ink, r: RectF, today: LocalDate) {
        val u = k.u
        var y = miniHead(c, k, r, "Bí cảnh tuần") { x, yy, s -> k.gate(c, x, yy, s) }
        val pad = 12 * u
        val monday = Store.weekStart(Store.today())
        val met = (0 until 7).map { Store.minutes(monday + it) >= Store.BI_CANH_MINUTES }
        k.week(c, r.left + pad, y, r.width() - 2 * pad, 14 * u, met, today.dayOfWeek.value - 1)
        y += 22 * u
        val days = Store.biCanhDays()
        val status = if (Store.biCanhDone()) "Đã qua tuần này" else "$days/${Store.BI_CANH_DAYS} ngày · ${Store.BI_CANH_MINUTES} phút"
        lines(c, k, r, y, listOf(status to true, "Đã qua ${Store.biCanhCount} lần" to false))
    }

    private fun drawTitle(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        val head = "Đạo hiệu" + if (Store.titleUnseen > 0) " •${Store.titleUnseen}" else ""
        var y = miniHead(c, k, r, head) { x, yy, s -> k.seal(c, x, yy, s) }
        val pad = 12 * u
        val t = Store.shownTitle()
        val p = k.ui10Bold
        val name = if (t >= 0) DaoHieu.NAMES[t] else "Chưa đắc đạo hiệu"
        for (line in k.wrap(name, r.width() - 2 * pad, p, 2)) {
            if (y + k.lineH(p) > r.bottom - 8 * u - k.lineH(k.small)) break
            k.text(c, line, r.left + pad, y, p)
            y += k.lineH(p)
        }
        k.text(c, "${Store.titleCount()}/${DaoHieu.COUNT} đạo hiệu ›", r.left + pad, r.bottom - 10 * u - k.lineH(k.small), k.small)
    }

    /** Corner marks of the card that grow with the realm. */
    private fun drawRealmFrame(c: Canvas, k: Ink, r: RectF, realm: Int) {
        val u = k.u
        val inset = 10 * u
        val pts = listOf(r.left + inset to r.top + inset, r.right - inset to r.top + inset, r.left + inset to r.bottom - inset, r.right - inset to r.bottom - inset)
        for ((x, y) in pts) {
            when (realm) {
                0 -> c.drawCircle(x, y, 2 * u, k.fill)
                1 -> { c.drawCircle(x, y, 5 * u, k.fill); c.drawCircle(x, y, 2 * u, k.white) }
                2 -> { c.drawCircle(x, y, 6 * u, k.fill); c.drawCircle(x, y, 4 * u, k.white); c.drawCircle(x, y, 2 * u, k.fill) }
                3 -> k.diamond(c, x, y, 7 * u)
                4, 5 -> { k.diamond(c, x, y, 7 * u); c.drawCircle(x, y, 2 * u, k.white) }
                else -> star(c, k, x, y, 8 * u)
            }
        }
        if (realm >= 5) c.drawRoundRect(k.inset(r, 4 * u), (Ink.RADIUS - 2) * u, (Ink.RADIUS - 2) * u, k.stroke(1f))
    }

    private fun star(c: Canvas, k: Ink, cx: Float, cy: Float, rad: Float) {
        val path = Path()
        for (i in 0 until 10) {
            val a = Math.toRadians(-90.0 + i * 36.0)
            val rr = if (i % 2 == 0) rad else rad * 0.45f
            val px = cx + (rr * cos(a)).toFloat()
            val py = cy + (rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        c.drawPath(path, k.fill)
    }

    companion object {
        fun greeting(): String {
            val h = LocalTime.now().hour
            return when {
                h in 5..8 -> "Sớm mai an lành"
                h in 9..11 -> "Sáng an lành"
                h in 12..13 -> "Trưa an lành"
                h in 14..17 -> "Chiều an lành"
                h in 18..21 -> "Tối an lành"
                h >= 22 || h < 1 -> "Khuya rồi, đạo hữu"
                else -> "Canh ba rồi, đạo hữu"
            }
        }

        fun occasion(d: LocalDate, l: Lunar.Date): String? {
            val ld = l.day
            val lm = if (l.leap) 0 else l.month
            return when {
                lm == 1 && ld <= 3 -> "Chúc mừng năm mới!"
                lm == 1 && ld == 15 -> "Rằm tháng Giêng"
                lm == 3 && ld == 10 -> "Giỗ Tổ Hùng Vương"
                lm == 5 && ld == 5 -> "Tết Đoan Ngọ"
                lm == 7 && ld == 15 -> "Rằm tháng Bảy"
                lm == 8 && ld == 15 -> "Trung Thu"
                lm == 12 && ld == 23 -> "Ông Công ông Táo"
                d.monthValue == 1 && d.dayOfMonth == 1 -> "Năm mới dương lịch"
                d.monthValue == 4 && d.dayOfMonth == 21 -> "Ngày Sách Việt Nam"
                d.monthValue == 4 && d.dayOfMonth == 23 -> "Ngày Sách thế giới"
                d.monthValue == 9 && d.dayOfMonth == 2 -> "Mừng Quốc khánh"
                d.monthValue == 11 && d.dayOfMonth == 20 -> "Ngày Nhà giáo"
                else -> null
            }
        }

        /** Time to the end at this reader's pace in this book (needs a few minutes of reading). */
        fun timeLeft(b: Book): String? {
            if (b.words <= 0 || b.wordsRead < 2000 || b.seconds < 300 || b.finishDay >= 0) return null
            val perSecond = b.wordsRead.toDouble() / b.seconds
            val minutes = ((1 - b.progress) * b.words / perSecond / 60).toLong()
            return if (minutes < 60) "~$minutes phút" else "~${minutes / 60} giờ " + (minutes % 60).toString().padStart(2, '0')
        }

        fun duration(seconds: Long): String {
            val m = seconds / 60
            return if (m < 60) "$m phút" else "${m / 60} giờ " + (m % 60).toString().padStart(2, '0') + " phút"
        }

        fun hours(seconds: Long): String {
            if (seconds < 3600) return duration(seconds)
            val tenths = (seconds + 180) / 360
            return "${tenths / 10},${tenths % 10} giờ"
        }
    }
}
