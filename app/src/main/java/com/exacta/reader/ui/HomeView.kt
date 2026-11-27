package com.exacta.reader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.exacta.reader.data.Book
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.game.Lunar
import com.exacta.reader.game.NienKy
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Texts
import com.exacta.reader.game.Treasure
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Home: the book being read across the top; the realm and the treasures side by side; the three
 * Omega cards (thiên kiếp, bí cảnh, đạo hiệu); a dock of the other screens along the bottom. The
 * blocks share one grid, so the page stays balanced on any screen. Every block is a touch target.
 */
@SuppressLint("ViewConstructor")
class HomeView(context: Context, private val onAction: (Action, String?) -> Unit) : View(context) {
    enum class Action { READ, LIBRARY, PHAP_BAO, TU_VI, SETTINGS, PERMISSION, DAO_HIEU, NIEN_KY, CONG_PHAP, DONG_PHU }

    var book: Book? = null
    var needsPermission = false
    private var ink: Ink? = null
    private val targets = ArrayList<Pair<RectF, () -> Unit>>()

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

    private fun on(r: RectF, action: Action, path: String? = null) = targets.add(RectF(r) to { onAction(action, path) })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            targets.lastOrNull { it.first.contains(event.x, event.y) }?.second?.invoke()
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val k = ink ?: return
        val u = k.u
        val w = width.toFloat()
        val h = height.toFloat()
        targets.clear()
        val today = LocalDate.now()
        val lunar = Lunar.fromSolar(today.dayOfMonth, today.monthValue, today.year)
        k.status(c, w, Lunar.ancientMonthDay(lunar))
        val m = Ink.MARGIN * u
        val area = RectF(m, (Ink.STATUS_H + 12) * u, w - m, h - 10 * u)
        when (Store.homeStyle) {
            1 -> bigCover(c, k, area)
            2 -> shelf(c, k, area)
            3 -> cultivation(c, k, area, today, lunar)
            else -> board(c, k, area, today, lunar)
        }
    }

    /** The first of `tries` that fits `w`; the last one cut only when nothing fits. */
    private fun shortest(k: Ink, w: Float, p: android.graphics.Paint, tries: List<String>): String =
        tries.firstOrNull { p.measureText(it) <= w } ?: k.fit(tries.last(), w, p)

    private fun bookTarget(r: RectF) {
        val b = book
        if (needsPermission) on(r, Action.PERMISSION) else if (b != null) on(r, Action.READ, b.path) else on(r, Action.LIBRARY)
    }

    /** The three Omega cards in a row. */
    private fun minis(c: Canvas, k: Ink, r: RectF, today: LocalDate) {
        val g = Ink.GAP * k.u
        val mw = (r.width() - 2 * g) / 3
        val cards = (0 until 3).map { RectF(r.left + it * (mw + g), r.top, r.left + it * (mw + g) + mw, r.bottom) }
        drawTrial(c, k, cards[0]); on(cards[0], Action.TU_VI)
        drawBiCanh(c, k, cards[1], today); on(cards[1], Action.TU_VI)
        drawTitle(c, k, cards[2]); on(cards[2], Action.DAO_HIEU)
    }

    /** 0 · Bảng tu hành: the book; the realm (with thiên kiếp and bí cảnh) and the treasures; a bottom row. */
    private fun board(c: Canvas, k: Ink, a: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val g = Ink.GAP * u
        val row = RectF(a.left, a.bottom - 92 * u, a.right, a.bottom)
        drawBottomRow(c, k, row)
        val free = row.top - g - a.top
        val bookRect = RectF(a.left, a.top, a.right, a.top + ((free - g) * 0.4f).coerceIn(230 * u, 380 * u))
        drawBook(c, k, bookRect); bookTarget(bookRect); libraryButton(c, k, bookRect)
        val twoThirds = (a.width() - g) * 2 / 3
        val realm = RectF(a.left, bookRect.bottom + g, a.left + twoThirds, row.top - g)
        val treasures = RectF(realm.right + g, realm.top, a.right, realm.bottom)
        on(realm, Action.TU_VI)
        drawRealmFull(c, k, realm, today, lunar)
        drawTreasures(c, k, treasures); on(treasures, Action.PHAP_BAO)
    }

    /** The realm card with the trials inside: greeting, realm, road, the day's numbers, thiên kiếp, bí cảnh. */
    private fun drawRealmFull(c: Canvas, k: Ink, r: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val words = Store.tuVi()
        val level = Store.level()
        realmFrame(c, k, r)
        val pad = 26 * u
        val x = r.left + pad
        val w = r.width() - 2 * pad
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val bigP = k.big
        val nb = k.ui10Bold
        val t = Store.shownTitle()

        val term = Lunar.termName(Lunar.solarTerm(today.dayOfMonth, today.monthValue, today.year))
        val head = shortest(k, w, sb, listOf(
            (occasion(today, lunar) ?: greeting()) + " · " + term + " · " + Texts.TERM_PHRASES[termPhrase],
            (occasion(today, lunar) ?: greeting()) + " · " + term,
            occasion(today, lunar) ?: greeting(),
        ))
        val icon = 50 * u
        val realmH = icon + 2 * u + k.lineH(nb) + (if (t >= 0) sl else 0f) + 8 * u + 15 * u + 5 * u + sl
        val statsH = 2 * sl
        val trialH = 2 * sl + 6 * u
        val biH = 2 * sl + 6 * u
        val saying = k.wrap(Texts.PROVERBS[proverb], w, small, 2)
        val sayingH = 12 * u + saying.size * sl
        val blocks = mutableListOf(sl, realmH, statsH, trialH, biH)
        val inner = r.height() - 2 * pad
        if (Store.showProverb && blocks.sum() + sayingH + 6 * 10 * u <= inner) blocks.add(sayingH)
        val gap = ((inner - blocks.sum()) / (blocks.size - 1)).coerceIn(4 * u, 30 * u)
        var y = r.top + pad + maxOf(0f, (inner - blocks.sum() - gap * (blocks.size - 1)) / 2)

        k.centered(c, head, x, w, y, sb)
        y += sl + gap

        // The realm.
        val number = Realm.van(words)
        val unit = k.ui12Bold
        val nw = bigP.measureText(number)
        val uw = unit.measureText("vạn từ")
        val nx = x + maxOf(0f, (w - (icon + 12 * u + nw + 8 * u + uw)) / 2)
        k.realmIcon(c, Realm.realmOf(level), nx, y, icon)
        val ny = y + (icon - k.lineH(bigP)) / 2
        k.text(c, number, nx + icon + 12 * u, ny, bigP)
        k.text(c, "vạn từ", nx + icon + 12 * u + nw + 8 * u, ny + (-bigP.ascent()) - (-unit.ascent()), unit)
        y += icon + 2 * u
        k.centered(c, TuLuyen.name(), x, w, y, nb)
        y += k.lineH(nb)
        if (t >= 0) {
            k.centered(c, "« ${DaoHieu.NAMES[t]} »", x, w, y, small)
            y += sl
        }
        y += 8 * u
        y = k.realmBar(c, x, y, w, TuLuyen.bar())
        y += gap
        stats(c, k, x, y, w)
        y += statsH + gap

        // Thiên kiếp, one row.
        val ic = 28 * u
        val trialRow = RectF(r.left + 8 * u, y - gap / 2, r.right - 8 * u, y + trialH + gap / 2)
        c.drawRect(x, y - gap / 2, x + w, y - gap / 2 + u, k.gray)
        k.icon(c, "thienkiep", x, y + (2 * sl - ic) / 2, ic)
        val tx = x + ic + 12 * u
        val tw = w - ic - 12 * u
        val gateLines = TuLuyen.homeLines()
        val l1 = gateLines[0].first
        val l2 = gateLines.getOrNull(1)?.first ?: ""
        k.text(c, k.fit(l1, tw, sb), tx, y, sb)
        k.text(c, k.fit(l2, tw, small), tx, y + sl, small)
        on(trialRow, Action.TU_VI, "2")
        y += trialH + gap

        // Bí cảnh, one row with the week.
        val biRow = RectF(r.left + 8 * u, y - gap / 2, r.right - 8 * u, y + biH + gap / 2)
        c.drawRect(x, y - gap / 2, x + w, y - gap / 2 + u, k.gray)
        k.icon(c, "bicanh", x, y + (2 * sl - ic) / 2, ic)
        val status = if (Store.biCanhDone()) "Bí cảnh tuần · đã qua" else "Bí cảnh tuần · ${Store.biCanhDays()}/${Store.biCanhNeed()} ngày"
        k.text(c, k.fit(status, tw * 0.55f, sb), tx, y, sb)
        k.text(c, "từ ${Store.BI_CANH_MINUTES} phút/ngày · đã qua ${Store.biCanhCount} lần", tx, y + sl, small)
        val monday = Store.weekStart(Store.today())
        val met = (0 until 7).map { Store.minutes(monday + it) >= Store.BI_CANH_MINUTES }
        val ww = minOf(tw * 0.42f, 7 * 22 * u)
        k.week(c, x + w - ww, y + 3 * u, ww, 14 * u, met, today.dayOfWeek.value - 1)
        on(biRow, Action.TU_VI, "3")
        y += biH

        if (blocks.size == 6) {
            y += gap
            c.drawRect(x + (w - 36 * u) / 2, y, x + (w + 36 * u) / 2, y + 2 * u, k.fill)
            y += 12 * u
            val sp = k.paint(Ink.SMALL, color = Ink.DARK)
            for (l in saying) {
                k.centered(c, l, x, w, y, sp)
                y += sl
            }
        }
    }

    /** Đạo hiệu on a third of the width, then Công pháp, Động phủ, Niên kỷ and Cài đặt. */
    private fun drawBottomRow(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        val g = Ink.GAP * u
        val half = (r.width() - g) / 3
        val dao = RectF(r.left, r.top, r.left + half, r.bottom)
        k.card(c, dao)
        val s = 34 * u
        k.icon(c, "daohieu", dao.left + 14 * u, dao.centerY() - s / 2, s)
        val tx = dao.left + 14 * u + s + 10 * u
        val tw = dao.right - 12 * u - tx
        val t = Store.shownTitle()
        val name = if (t >= 0) DaoHieu.NAMES[t] else "Chưa đắc đạo hiệu"
        // The title on two lines when it must; the count short.
        val nb = if (k.ui10Bold.measureText(name) <= tw) k.ui10Bold else k.smallBold
        val nameLines = k.wrap(name, tw, nb, 2)
        val sub = "${Store.titleCount()}/${DaoHieu.COUNT}" + if (Store.titleUnseen > 0) " · ${Store.titleUnseen} mới" else ""
        var ty = dao.centerY() - (nameLines.size * k.lineH(nb) + k.lineH(k.small)) / 2
        for (l in nameLines) {
            k.text(c, l, tx, ty, nb)
            ty += k.lineH(nb)
        }
        k.text(c, k.fit(sub, tw, k.small), tx, ty, k.small)
        on(dao, Action.DAO_HIEU)
        val cells = listOf(Triple(Action.CONG_PHAP, "congphap", "Công pháp"), Triple(Action.DONG_PHU, "dongphu", "Động phủ"),
            Triple(Action.NIEN_KY, "nienky", "Niên kỷ"), Triple(Action.SETTINGS, "settings", "Cài đặt"))
        val q = (r.width() - half - cells.size * g) / cells.size
        val due = NienKy.due()
        for ((i, item) in cells.withIndex()) {
            val cell = RectF(dao.right + g + i * (q + g), r.top, dao.right + g + i * (q + g) + q, r.bottom)
            k.card(c, cell)
            val icon = 30 * u
            val p = k.smallBold
            val total = icon + 6 * u + k.lineH(p)
            val iy = cell.centerY() - total / 2
            k.icon(c, item.second, cell.centerX() - icon / 2, iy, icon)
            k.centered(c, k.fit(item.third, cell.width() - 6 * u, p), cell.left, cell.width(), iy + icon + 6 * u, p)
            if (item.first == Action.NIEN_KY && due != 0 && Store.nienKySeen != due) c.drawCircle(cell.centerX() + icon / 2 + 4 * u, iy + 4 * u, 5 * u, k.fill)
            on(cell, item.first)
        }
    }

    /** 1 · Bìa lớn: the cover large in the middle, the realm in one strip, the dock. */
    private fun bigCover(c: Canvas, k: Ink, a: RectF) {
        val u = k.u
        val g = Ink.GAP * u
        val dock = RectF(a.left, a.bottom - DOCK_H * u, a.right, a.bottom)
        drawDock(c, k, dock, listOf(Action.CONG_PHAP, Action.DONG_PHU, Action.PHAP_BAO, Action.DAO_HIEU, Action.NIEN_KY, Action.SETTINGS))
        val strip = RectF(a.left, dock.top - g - 112 * u, a.right, dock.top - g)
        drawStrip(c, k, strip); on(strip, Action.TU_VI)
        val hero = RectF(a.left, a.top, a.right, strip.top - g)
        bookFrame(c, k, hero)
        bookTarget(hero)
        libraryButton(c, k, hero)
        val b = book
        if (needsPermission || b == null) {
            drawBook(c, k, hero)
            return
        }
        val pad = 34 * u
        val sb = k.smallBold
        val textH = k.lineH(k.big) * 2 + k.lineH(k.ui10) + 18 * u + k.lineH(sb) + 14 * u + 14 * u + 14 * u + k.lineH(k.ui10Bold)
        val coverH = hero.height() - 2 * pad - textH - 16 * u
        val coverW = minOf(coverH * 0.67f, hero.width() - 2 * pad)
        val cr = RectF(hero.centerX() - coverW / 2, hero.top + pad, hero.centerX() + coverW / 2, hero.top + pad + coverH)
        k.cover(c, b.cover, b.title, cr)
        var y = cr.bottom + 16 * u
        val x = hero.left + pad
        val w = hero.width() - 2 * pad
        for (line in k.wrap(b.title, w, k.big, 2)) {
            k.centered(c, line, x, w, y, k.big)
            y += k.lineH(k.big)
        }
        if (b.author.isNotEmpty()) k.centered(c, b.author, x, w, y, k.ui10)
        y += k.lineH(k.ui10) + 18 * u
        val pct = b.percent
        val bw = minOf(w, 460 * u)
        k.centered(c, "${Realm.tangName(Realm.tangOf(pct))} · $pct%" + (timeLeft(b)?.let { " · còn $it" } ?: ""), x, w, y, sb)
        y += k.lineH(sb) + 8 * u
        tangBar(c, k, hero.centerX() - bw / 2, y, bw, 14 * u, pct)
        y += 28 * u
        k.centered(c, Texts.PRACTICES[practice] + "  ›", x, w, y, k.ui10Bold)
    }

    /** 2 · Kệ sách: the book, the other books being read as a shelf, the realm strip, the dock. */
    private fun shelf(c: Canvas, k: Ink, a: RectF) {
        val u = k.u
        val g = Ink.GAP * u
        val dock = RectF(a.left, a.bottom - DOCK_H * u, a.right, a.bottom)
        drawDock(c, k, dock, listOf(Action.CONG_PHAP, Action.DONG_PHU, Action.PHAP_BAO, Action.DAO_HIEU, Action.NIEN_KY, Action.SETTINGS))
        val strip = RectF(a.left, dock.top - g - 112 * u, a.right, dock.top - g)
        drawStrip(c, k, strip); on(strip, Action.TU_VI)
        val bookRect = RectF(a.left, a.top, a.right, a.top + (a.height() * 0.3f).coerceIn(220 * u, 320 * u))
        drawBook(c, k, bookRect); bookTarget(bookRect); libraryButton(c, k, bookRect)
        val r = RectF(a.left, bookRect.bottom + g, a.right, strip.top - g)
        k.card(c, r)
        val pad = 18 * u
        val sb = k.smallBold
        k.text(c, "KỆ SÁCH · ĐANG ĐỌC", r.left + pad, r.top + pad, sb)
        val others = Store.recent(12).filter { it.path != book?.path }
        val inner = RectF(r.left + pad, r.top + pad + k.lineH(sb) + 14 * u, r.right - pad, r.bottom - pad)
        if (others.isEmpty()) {
            k.centered(c, "Sách khác đang đọc sẽ xếp lên kệ này.", inner.left, inner.width(), inner.centerY(), k.small)
            on(inner, Action.LIBRARY)
            return
        }
        val gap = 14 * u
        val cols = ((inner.width() + gap) / (132 * u + gap)).toInt().coerceIn(3, 6)
        val cw = (inner.width() - (cols - 1) * gap) / cols
        val textH = 3 * k.lineH(k.small) + 10 * u
        val ch = minOf(cw * 1.5f, inner.height() - textH)
        val rows = ((inner.height() + gap) / (ch + textH + gap)).toInt().coerceIn(1, 3)
        for ((i, b) in others.take(rows * cols).withIndex()) {
            val x = inner.left + (i % cols) * (cw + gap)
            val y = inner.top + (i / cols) * (ch + textH + gap)
            val covW = minOf(cw, ch / 1.45f)
            val cr = RectF(x + (cw - covW) / 2, y, x + (cw + covW) / 2, y + ch)
            k.cover(c, b.cover, b.title, cr)
            var ty = cr.bottom + 6 * u
            val tl = k.wrap(b.title, cw, k.small, 2)
            for (l in tl) {
                k.centered(c, l, x, cw, ty, k.small)
                ty += k.lineH(k.small)
            }
            k.centered(c, "${b.percent}%", x, cw, cr.bottom + 6 * u + 2 * k.lineH(k.small), sb)
            on(RectF(x, y, x + cw, y + ch + textH), Action.READ, b.path)
        }
    }

    /** 3 · Tu tiên: the realm large on top, the Omega cards, the book in one row, the dock. */
    private fun cultivation(c: Canvas, k: Ink, a: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val g = Ink.GAP * u
        val dock = RectF(a.left, a.bottom - DOCK_H * u, a.right, a.bottom)
        drawDock(c, k, dock, listOf(Action.CONG_PHAP, Action.DONG_PHU, Action.PHAP_BAO, Action.NIEN_KY, Action.SETTINGS))
        val bookRect = RectF(a.left, dock.top - g - 170 * u, a.right, dock.top - g)
        drawBookRow(c, k, bookRect); bookTarget(bookRect)
        libraryButton(c, k, bookRect)
        val mini = RectF(a.left, bookRect.top - g - MINI_H * u, a.right, bookRect.top - g)
        minis(c, k, mini, today)
        val hero = RectF(a.left, a.top, a.right, mini.top - g)
        realmFrame(c, k, hero)
        on(hero, Action.TU_VI)
        val words = Store.tuVi()
        val level = Store.level()
        val pad = 30 * u
        val x = hero.left + pad
        val w = hero.width() - 2 * pad
        val sb = k.smallBold
        val term = Lunar.termName(Lunar.solarTerm(today.dayOfMonth, today.monthValue, today.year)) + " · " + Texts.TERM_PHRASES[termPhrase]
        val nameP = k.paint(44f, big = true)
        val t = Store.shownTitle()
        val rest = k.lineH(sb) + 16 * u + 10 * u + k.lineH(nameP) + (if (t >= 0) k.lineH(k.ui10) else 0f) +
            k.lineH(k.ui10) + 20 * u + 40 * u + 22 * u + 2 * k.lineH(k.small)
        val icon = minOf(120 * u, hero.height() * 0.3f, hero.height() - 2 * pad - rest).coerceAtLeast(40 * u)
        val blockH = rest + icon
        var y = hero.top + maxOf(pad, (hero.height() - blockH) / 2)
        k.centered(c, (occasion(today, lunar) ?: greeting()) + " · " + term, x, w, y, sb)
        y += k.lineH(sb) + 16 * u
        k.realmIcon(c, Realm.realmOf(level), hero.centerX() - icon / 2, y, icon)
        y += icon + 10 * u
        k.centered(c, TuLuyen.name(), x, w, y, nameP)
        y += k.lineH(nameP)
        if (t >= 0) {
            k.centered(c, "« ${DaoHieu.NAMES[t]} »", x, w, y, k.ui10)
            y += k.lineH(k.ui10)
        }
        k.centered(c, "${Realm.van(words)} vạn từ tu vi", x, w, y, k.ui10Bold)
        y += k.lineH(k.ui10) + 20 * u
        y = k.realmBar(c, x + w * 0.1f, y, w * 0.8f, TuLuyen.bar())
        y += 22 * u
        stats(c, k, x + w * 0.1f, y, w * 0.8f)
    }

    /** Today, the last seven days, the streak in three columns. */
    private fun stats(c: Canvas, k: Ink, x: Float, y: Float, w: Float) {
        val u = k.u
        val d = Store.today()
        val cols = listOf(
            todayText() to "hôm nay",
            hours(Store.secondsBetween(d - 6, d)) to "7 ngày",
            "${Store.streak()} ngày" to "chuỗi",
        )
        val sl = k.lineH(k.small)
        val colW = w / cols.size
        for ((i, pair) in cols.withIndex()) {
            val cx = x + i * colW
            if (i > 0) c.drawRect(cx, y + 3 * u, cx + u, y + 2 * sl - 3 * u, k.gray)
            k.centered(c, pair.first, cx, colW, y, k.smallBold)
            k.centered(c, pair.second, cx, colW, y + sl, k.small)
        }
    }

    /** The minutes read today, against the day's goal when there is one. */
    private fun todayText(): String {
        val sec = Store.daySeconds[Store.today()] ?: 0
        val min = sec / 60
        return if (Store.dailyGoal > 0 && min < Store.dailyGoal) "$min/${Store.dailyGoal} phút" else duration(sec)
    }

    /** The realm in one strip: logo, name, words, then the day's numbers. */
    private fun drawStrip(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        realmFrame(c, k, r)
        val level = Store.level()
        val pad = 26 * u
        val icon = minOf(56 * u, r.height() - 2 * pad)
        k.realmIcon(c, Realm.realmOf(level), r.left + pad, r.centerY() - icon / 2, icon)
        val tx = r.left + pad + icon + 14 * u
        val left = r.width() * 0.42f
        val nw = left - icon - 2 * pad + 20 * u
        val np = if (k.ui10Bold.measureText(TuLuyen.name()) <= nw) k.ui10Bold else k.smallBold
        k.text(c, k.fit(TuLuyen.name(), nw, np), tx, r.centerY() - k.lineH(np), np)
        k.text(c, "${Realm.van(Store.tuVi())} vạn từ", tx, r.centerY() + 2 * u, k.small)
        stats(c, k, r.left + left, r.centerY() - k.lineH(k.small), r.width() - left - pad)
    }

    /** The book in one row: a small cover, the title, the tầng. */
    private fun drawBookRow(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        bookFrame(c, k, r)
        val b = book
        if (needsPermission || b == null) {
            k.centered(c, if (needsPermission) "Chạm để cấp quyền đọc thẻ nhớ" else "Chưa đọc cuốn nào · chạm để mở Thư viện", r.left, r.width(), r.centerY() - k.lineH(k.ui10) / 2, k.ui10)
            return
        }
        val pad = 24 * u
        val ch = r.height() - 2 * pad
        val cr = RectF(r.left + pad, r.top + pad, r.left + pad + ch * 0.67f, r.bottom - pad)
        k.cover(c, b.cover, b.title, cr)
        val tx = cr.right + 20 * u
        val tw = r.right - pad - tx
        var y = cr.top + 4 * u
        for (l in k.wrap(b.title, tw, k.ui12Bold, 2)) {
            k.text(c, l, tx, y, k.ui12Bold)
            y += k.lineH(k.ui12Bold)
        }
        if (b.author.isNotEmpty()) k.text(c, k.fit(b.author, tw, k.small), tx, y, k.small)
        val by = cr.bottom - 12 * u - k.lineH(k.smallBold) - 8 * u
        k.text(c, "${Realm.tangName(Realm.tangOf(b.percent))} · ${b.percent}%  ·  ${Texts.PRACTICES[practice]} ›", tx, by, k.smallBold)
        tangBar(c, k, tx, cr.bottom - 12 * u, tw, 12 * u, b.percent)
    }

    /** Nine cells filled to `pct`. */
    private fun tangBar(c: Canvas, k: Ink, x: Float, y: Float, w: Float, h: Float, pct: Int) {
        val u = k.u
        val gap = 4 * u
        val cellW = (w - 8 * gap) / 9
        val filled = pct * 9 / 100.0
        for (i in 0 until 9) {
            val cx = x + i * (cellW + gap)
            val cell = RectF(cx, y, cx + cellW, y + h)
            val f = (filled - i).coerceIn(0.0, 1.0).toFloat()
            if (f >= 1f) {
                c.drawRoundRect(cell, 3 * u, 3 * u, k.fill)
            } else {
                c.drawRoundRect(k.inset(cell, 0.5f * u), 3 * u, 3 * u, k.stroke(1.3f))
                if (f > 0) c.drawRect(cx + u, y + u, cx + u + (cellW - 2 * u) * f, y + h - u, k.fill)
            }
        }
    }

    // --- The book ---------------------------------------------------------------------------

    /** The realm reached and the way through it (0..1), for the frames. */
    private fun realmProgress(): Float = TuLuyen.bar().let { (it.cells / (it.groups * 3)).toFloat() }

    private fun realmFrame(c: Canvas, k: Ink, r: RectF) =
        k.frame(c, r, k.grade, realmProgress(), "${Realm.baseName(Realm.realmOf(Store.level()))} · ${(realmProgress() * 100).toInt()}%")

    private fun bookFrame(c: Canvas, k: Ink, r: RectF) {
        val b = book
        if (b == null || needsPermission) k.card(c, r) else k.frame(c, r, k.grade, b.progress.toFloat(), "${Realm.tangName(Realm.tangOf(b.percent))} · ${b.percent}%")
    }

    private fun drawBook(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        bookFrame(c, k, r)
        val b = book
        if (needsPermission || b == null) {
            val p = k.ui12Bold
            val lines = if (needsPermission) listOf("Cần quyền đọc thẻ nhớ", "Chạm để cấp quyền") else listOf("Chưa đọc cuốn nào", "Chạm để mở Thư viện")
            val y = r.centerY() - k.lineH(p)
            k.centered(c, lines[0], r.left, r.width(), y, p)
            k.centered(c, lines[1], r.left, r.width(), y + k.lineH(p) + 4 * u, k.ui10)
            return
        }
        val pad = 28 * u
        val coverH = r.height() - 2 * pad
        val coverW = coverH * 0.67f
        val cr = RectF(r.left + pad, r.top + pad, r.left + pad + coverW, r.top + pad + coverH)
        k.cover(c, b.cover, b.title, cr)

        val tx = cr.right + 24 * u
        val tw = r.right - pad - tx
        val small = k.small
        val sb = k.smallBold
        val dark = k.paint(Ink.SMALL, color = Ink.DARK)

        // Bottom block: the tầng, its nine cells, the call to read on.
        val call = Texts.PRACTICES[practice] + "  ›"
        val callY = cr.bottom - k.lineH(k.ui10Bold)
        val barH = 14 * u
        val barY = callY - 14 * u - barH
        val labelY = barY - 8 * u - k.lineH(sb)
        val pct = b.percent
        val tang = Realm.tangOf(pct)
        k.text(c, if (tang >= 10) "Đại viên mãn" else "Tu đến ${Realm.tangName(tang)}", tx, labelY, sb)
        k.right(c, "$pct%", tx + tw, labelY, sb)
        val gap = 4 * u
        val cellW = (tw - 8 * gap) / 9
        val filled = pct * 9 / 100.0
        for (i in 0 until 9) {
            val cx = tx + i * (cellW + gap)
            val cell = RectF(cx, barY, cx + cellW, barY + barH)
            val f = (filled - i).coerceIn(0.0, 1.0).toFloat()
            if (f >= 1f) {
                c.drawRoundRect(cell, 3 * u, 3 * u, k.fill)
            } else {
                c.drawRoundRect(k.inset(cell, 0.5f * u), 3 * u, 3 * u, k.stroke(1.3f))
                if (f > 0) c.drawRect(cx + u, barY + u, cx + u + (cellW - 2 * u) * f, barY + barH - u, k.fill)
            }
        }
        k.text(c, k.fit(call, tw, k.ui10Bold), tx, callY, k.ui10Bold)

        // Top block: the scripture's name, the title, the author, the time (clear of the library button).
        var y = cr.top
        k.text(c, k.fit(Texts.SCRIPTURES[scripture].uppercase(), tw - LIB_W * u - 14 * u, dark), tx, y, dark)
        y += k.lineH(small) + 6 * u
        c.drawRect(tx, y, tx + 40 * u, y + 2 * u, k.fill)
        y += 14 * u
        val room = labelY - 14 * u
        val titleW = tw - LIB_W * u - 14 * u
        val title = if (k.big.measureText(b.title) <= titleW) k.big else k.ui12Bold
        for (line in k.wrap(b.title, titleW, title, 3)) {
            if (y + k.lineH(title) > room) break
            k.text(c, line, tx, y, title)
            y += k.lineH(title)
        }
        if (b.author.isNotEmpty() && y + k.lineH(k.ui10) <= room) {
            y += 2 * u
            k.text(c, k.fit(b.author, tw, k.ui10), tx, y, k.ui10)
            y += k.lineH(k.ui10)
        }
        val info = bookInfo(b)
        if (info.isNotEmpty() && y + 8 * u + k.lineH(small) <= room) {
            y += 8 * u
            k.text(c, k.fit(info, tw, dark), tx, y, dark)
        }
    }

    /** "Đã đọc 2 giờ 05 phút · còn ~3 giờ 10" for the book card. */
    private fun bookInfo(b: Book): String {
        val parts = mutableListOf<String>()
        if (b.seconds >= 60) parts.add("Đã đọc ${duration(b.seconds)}")
        timeLeft(b)?.let { parts.add("còn $it") }
        if (b.words > 0 && parts.size < 2) parts.add("${Realm.van(b.words)} vạn từ")
        return parts.joinToString(" · ")
    }

    // --- The treasures ----------------------------------------------------------------------

    private fun drawTreasures(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        k.card(c, r)
        val pad = 16 * u
        val x = r.left + pad
        val w = r.width() - 2 * pad
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        k.text(c, "PHÁP BẢO", x, r.top + pad, sb)
        if (Store.unseen > 0) k.right(c, "•${Store.unseen} mới", x + w, r.top + pad, sb)
        val footY = r.bottom - pad - sl
        k.centered(c, "${Store.beasts.size} món · ${Store.linhThach} linh thạch", x, w, footY, small)

        // The forge: six slots in two rows, when the card is tall enough for them.
        val nb = k.ui10Bold
        val top = r.top + pad + sl + 10 * u
        val slotW = w / 3
        val oreS = minOf(slotW - 8 * u, 42 * u)
        val cellH = oreS + 2 * u + sl
        val forgeH = 2 * cellH + 6 * u + 20 * u
        val beast = Store.strongest()
        val names = if (beast != null) k.wrap(beast.name, w, nb, 2) else listOf("Linh tinh chờ luyện")
        val textH = names.size * k.lineH(nb) + 2 * sl + 8 * u
        val showForge = footY - top - forgeH - textH >= 80 * u
        val bottom = if (showForge) footY - 8 * u - forgeH else footY - 10 * u
        if (showForge) {
            var fy = bottom + 20 * u
            c.drawRect(x, fy - 12 * u, x + w, fy - 11 * u, k.gray)
            for (i in 0 until Store.FORGE_SLOTS) {
                val ore = Store.forge.getOrNull(i)
                val sx = x + (i % 3) * slotW
                val cy = fy + (i / 3) * (cellH + 6 * u)
                when {
                    ore == null -> k.art(c, "art_crystal", sx + (slotW - oreS) / 2, cy, oreS, 60)
                    ore.inKiep -> k.icon(c, "thienkiep", sx + (slotW - oreS * 0.8f) / 2, cy + oreS * 0.1f, oreS * 0.8f)
                    ore.isBook -> {
                        val b = Store.books[ore.book]
                        val cw = oreS * 0.68f
                        k.cover(c, b?.cover ?: "", b?.title ?: "", RectF(sx + (slotW - cw) / 2, cy, sx + (slotW + cw) / 2, cy + oreS))
                    }
                    else -> k.ore(c, ore.tier, ore.percent, sx + (slotW - oreS) / 2, cy, oreS)
                }
                k.centered(c, if (ore != null) "${ore.percent}%" else "·", sx, slotW, cy + oreS + 2 * u, small)
            }
        }

        // The strongest treasure (or the waiting furnace), as large as the card allows.
        val art = minOf(w * 0.82f, bottom - top - textH).coerceAtLeast(48 * u)
        val ay = top + maxOf(0f, (bottom - top - art - textH) / 2)
        if (beast != null) k.badge(c, beast.species, x + (w - art) / 2, ay, art)
        else k.art(c, "art_crystal", x + (w - art * 0.9f) / 2, ay + art * 0.05f, art * 0.9f)
        var ny = ay + art + 8 * u
        for (l in names) {
            k.centered(c, l, x, w, ny, nb)
            ny += k.lineH(nb)
        }
        val sub = when {
            beast == null -> "Thiên tài địa bảo, đợi ngày khai lô"
            beast.spirit.isNotEmpty() -> "«${beast.spirit}» · " + Treasure.TIER_NAMES[beast.tier]
            else -> Treasure.CLANS[beast.tier] + " · " + Treasure.TIER_NAMES[beast.tier]
        }
        if (small.measureText(sub) <= w) k.centered(c, sub, x, w, ny, small)
        else for ((j, part) in sub.split(" · ").withIndex()) k.centered(c, part, x, w, ny + j * sl, small)
    }

    // --- Omega cards ------------------------------------------------------------------------

    /** The card's head: an icon and a title; returns the y below it. */
    private fun miniHead(c: Canvas, k: Ink, r: RectF, title: String, icon: String): Float {
        val u = k.u
        k.card(c, r)
        val pad = 16 * u
        val s = 30 * u
        k.icon(c, icon, r.left + pad, r.top + pad, s)
        val p = k.smallBold
        k.text(c, k.fit(title, r.width() - 2 * pad - s - 10 * u, p), r.left + pad + s + 10 * u, r.top + pad + (s - k.lineH(p)) / 2, p)
        return r.top + pad + s + 12 * u
    }

    private fun lines(c: Canvas, k: Ink, r: RectF, y0: Float, items: List<Pair<String, Boolean>>) {
        val u = k.u
        var y = y0
        val pad = 16 * u
        for ((text, bold) in items) {
            val p = if (bold) k.smallBold else k.small
            if (y + k.lineH(p) > r.bottom - 10 * u) break
            k.text(c, k.fit(text, r.width() - 2 * pad, p), r.left + pad, y, p)
            y += k.lineH(p)
        }
    }

    private fun drawTrial(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        var y = miniHead(c, k, r, "Cửa ải", "thienkiep")
        val pad = 16 * u
        if (Store.inTrial()) {
            val days = Store.trialDays().first
            val need = Store.trialNeed()
            val cw = minOf(24 * u, (r.width() - 2 * pad - (need - 1) * 5 * u) / need)
            for (i in 0 until need) {
                val cell = RectF(r.left + pad + i * (cw + 5 * u), y, r.left + pad + i * (cw + 5 * u) + cw, y + 14 * u)
                if (i < days) c.drawRoundRect(cell, 3 * u, 3 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 3 * u, 3 * u, k.stroke(1.3f))
            }
            y += 22 * u
        }
        val more = if (Store.realmCap in 0..8 && !Store.inTrial()) listOf("Căn cơ ${TuLuyen.canCo()} · tâm ma ${TuLuyen.tamMa}" to false) else emptyList()
        lines(c, k, r, y, TuLuyen.homeLines() + more)
    }

    private fun drawBiCanh(c: Canvas, k: Ink, r: RectF, today: LocalDate) {
        val u = k.u
        var y = miniHead(c, k, r, "Bí cảnh tuần", "bicanh")
        val pad = 16 * u
        val monday = Store.weekStart(Store.today())
        val met = (0 until 7).map { Store.minutes(monday + it) >= Store.BI_CANH_MINUTES }
        k.week(c, r.left + pad, y, r.width() - 2 * pad, 16 * u, met, today.dayOfWeek.value - 1)
        y += 26 * u
        val status = if (Store.biCanhDone()) "Đã qua tuần này" else "${Store.biCanhDays()}/${Store.biCanhNeed()} ngày · ${Store.BI_CANH_MINUTES} phút"
        lines(c, k, r, y, listOf(status to true, "Đã qua ${Store.biCanhCount} lần" to false))
    }

    private fun drawTitle(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        val head = "Đạo hiệu" + if (Store.titleUnseen > 0) " · ${Store.titleUnseen} mới" else ""
        var y = miniHead(c, k, r, head, "daohieu")
        val pad = 16 * u
        val t = Store.shownTitle()
        val p = k.ui10Bold
        val name = if (t >= 0) DaoHieu.NAMES[t] else "Chưa đắc đạo hiệu"
        val foot = r.bottom - 12 * u - k.lineH(k.small)
        for (line in k.wrap(name, r.width() - 2 * pad, p, 2)) {
            if (y + k.lineH(p) > foot) break
            k.text(c, line, r.left + pad, y, p)
            y += k.lineH(p)
        }
        k.text(c, "${Store.titleCount()}/${DaoHieu.COUNT} đạo hiệu", r.left + pad, foot, k.small)
    }

    // --- The dock ---------------------------------------------------------------------------

    private fun drawDock(c: Canvas, k: Ink, r: RectF, actions: List<Action>) {
        val u = k.u
        val due = NienKy.due()
        val all = mapOf(
            Action.PHAP_BAO to ("phapbao" to "Pháp bảo"),
            Action.DAO_HIEU to ("daohieu" to "Đạo hiệu"),
            Action.NIEN_KY to ("nienky" to "Niên kỷ"),
            Action.SETTINGS to ("settings" to "Cài đặt"),
            Action.TU_VI to ("tuvi" to "Tu vi"),
            Action.LIBRARY to ("library" to "Thư viện"),
            Action.CONG_PHAP to ("congphap" to "Công pháp"),
            Action.DONG_PHU to ("dongphu" to "Động phủ"),
        )
        val g = Ink.GAP * u
        val cw = (r.width() - (actions.size - 1) * g) / actions.size
        val icon = 34 * u
        val p = k.ui10Bold
        for ((i, action) in actions.withIndex()) {
            val (name, label) = all.getValue(action)
            val cell = RectF(r.left + i * (cw + g), r.top, r.left + i * (cw + g) + cw, r.bottom)
            k.card(c, cell)
            if (actions.size >= 5) {
                val ic = 30 * u
                val lp = k.smallBold
                val top = cell.centerY() - (ic + 4 * u + k.lineH(lp)) / 2
                k.icon(c, name, cell.centerX() - ic / 2, top, ic)
                k.centered(c, k.fit(label, cell.width() - 6 * u, lp), cell.left, cell.width(), top + ic + 4 * u, lp)
                if (action == Action.NIEN_KY && due != 0 && Store.nienKySeen != due) c.drawCircle(cell.centerX() + ic / 2 + 4 * u, top + 3 * u, 5 * u, k.fill)
                on(cell, action)
                continue
            }
            // Icon and label side by side, centred in the frame.
            val lw = p.measureText(label)
            val total = icon + 12 * u + lw
            val ix = cell.centerX() - total / 2
            k.icon(c, name, ix, cell.centerY() - icon / 2, icon)
            k.text(c, label, ix + icon + 12 * u, cell.centerY() - k.lineH(p) / 2, p)
            if (action == Action.NIEN_KY && due != 0 && Store.nienKySeen != due) {
                c.drawCircle(ix + icon + 2 * u, cell.centerY() - icon / 2 + 2 * u, 5 * u, k.fill)
            }
            on(cell, action)
        }
    }

    /**
     * The library's button in the top right corner of a book card, inside its frame: a square with
     * the icon over the word, and a touch area a little larger than it (drawn and hit after the card).
     */
    private fun libraryButton(c: Canvas, k: Ink, card: RectF) {
        val u = k.u
        val s = LIB_W * u
        val r = RectF(card.right - 22 * u - s, card.top + 22 * u, card.right - 22 * u, card.top + 22 * u + s)
        k.button(c, r, "", k.small)
        val icon = 34 * u
        val p = k.smallBold
        val top = r.centerY() - (icon + 4 * u + k.lineH(p)) / 2
        k.icon(c, "library", r.centerX() - icon / 2, top, icon)
        k.centered(c, "Thư viện", r.left, r.width(), top + icon + 4 * u, p)
        on(RectF(r.left - 12 * u, r.top - 12 * u, r.right + 14 * u, r.bottom + 12 * u), Action.LIBRARY)
    }

    companion object {
        private const val DOCK_H = 66f
        private const val MINI_H = 118f
        private const val LIB_W = 96f
        val STYLES = listOf("Bảng tu hành", "Bìa lớn", "Kệ sách", "Tu tiên")
        val STYLE_NOTES = listOf(
            "Sách đang đọc, cảnh giới và pháp bảo cạnh nhau, ba thẻ Omega, thanh điều hướng",
            "Bìa sách lớn ở giữa, cảnh giới gọn một dải",
            "Sách đang đọc và một kệ các cuốn đang đọc dở",
            "Cảnh giới lớn ở trên, ba thẻ Omega, sách đang đọc một hàng",
        )

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
            return com.exacta.reader.game.ThoiThan.span(minutes)
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
