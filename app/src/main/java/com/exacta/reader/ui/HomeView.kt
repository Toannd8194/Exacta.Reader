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
    enum class Action { READ, LIBRARY, PHAP_BAO, TU_VI, SETTINGS, PERMISSION, DAO_HIEU, NIEN_KY }

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
        if (Store.homeStyle != 4) k.status(c, w, Lunar.ancientMonthDay(lunar))
        val m = Ink.MARGIN * u
        val area = RectF(m, (Ink.STATUS_H + 12) * u, w - m, h - 10 * u)
        when (Store.homeStyle) {
            1 -> classic(c, k, area, today, lunar)
            2 -> bigCover(c, k, area)
            3 -> shelf(c, k, area)
            4 -> minimal(c, k, RectF(m, 0f, w - m, h - 10 * u), today, lunar)
            5 -> cultivation(c, k, area, today, lunar)
            else -> board(c, k, area, today, lunar)
        }
    }

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

    /** 0 · Bảng tu hành: the book, the realm and the treasures, the Omega cards, the dock. */
    private fun board(c: Canvas, k: Ink, a: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val g = Ink.GAP * u
        val dock = RectF(a.left, a.bottom - DOCK_H * u, a.right, a.bottom)
        drawDock(c, k, dock, listOf(Action.NIEN_KY, Action.SETTINGS))
        val mini = RectF(a.left, dock.top - g - MINI_H * u, a.right, dock.top - g)
        minis(c, k, mini, today)
        val free = mini.top - g - a.top
        val bookRect = RectF(a.left, a.top, a.right, a.top + ((free - g) * 0.52f).coerceIn(230 * u, 420 * u))
        drawBook(c, k, bookRect); bookTarget(bookRect); libraryButton(c, k, bookRect)
        val twoThirds = (a.width() - g) * 2 / 3
        val realm = RectF(a.left, bookRect.bottom + g, a.left + twoThirds, mini.top - g)
        val treasures = RectF(realm.right + g, realm.top, a.right, realm.bottom)
        drawRealm(c, k, realm, today, lunar); on(realm, Action.TU_VI)
        drawTreasures(c, k, treasures); on(treasures, Action.PHAP_BAO)
    }

    /** 1 · Cổ điển: Exacta.OS A2, the book on top, the rail of buttons beside the realm. */
    private fun classic(c: Canvas, k: Ink, a: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val g = Ink.GAP * u
        val bookRect = RectF(a.left, a.top, a.right, a.top + (a.height() * 0.33f).coerceIn(220 * u, 340 * u))
        drawBook(c, k, bookRect); bookTarget(bookRect); libraryButton(c, k, bookRect)
        val railW = 128 * u
        val rail = RectF(a.left, bookRect.bottom + g, a.left + railW, a.bottom)
        drawRail(c, k, rail)
        val right = RectF(rail.right + g, rail.top, a.right, a.bottom)
        val mini = RectF(right.left, right.bottom - MINI_H * u, right.right, right.bottom)
        minis(c, k, mini, today)
        val realm = RectF(right.left, right.top, right.right, mini.top - g)
        drawRealm(c, k, realm, today, lunar); on(realm, Action.TU_VI)
    }

    /** 2 · Bìa lớn: the cover large in the middle, the realm in one strip, the dock. */
    private fun bigCover(c: Canvas, k: Ink, a: RectF) {
        val u = k.u
        val g = Ink.GAP * u
        val dock = RectF(a.left, a.bottom - DOCK_H * u, a.right, a.bottom)
        drawDock(c, k, dock, listOf(Action.PHAP_BAO, Action.DAO_HIEU, Action.NIEN_KY, Action.SETTINGS))
        val strip = RectF(a.left, dock.top - g - 112 * u, a.right, dock.top - g)
        drawStrip(c, k, strip); on(strip, Action.TU_VI)
        val hero = RectF(a.left, a.top, a.right, strip.top - g)
        k.card(c, hero)
        bookTarget(hero)
        libraryButton(c, k, hero)
        val b = book
        if (needsPermission || b == null) {
            drawBook(c, k, hero)
            return
        }
        val pad = 26 * u
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

    /** 3 · Kệ sách: the book, the other books being read as a shelf, the realm strip, the dock. */
    private fun shelf(c: Canvas, k: Ink, a: RectF) {
        val u = k.u
        val g = Ink.GAP * u
        val dock = RectF(a.left, a.bottom - DOCK_H * u, a.right, a.bottom)
        drawDock(c, k, dock, listOf(Action.PHAP_BAO, Action.DAO_HIEU, Action.NIEN_KY, Action.SETTINGS))
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
        val textH = 2 * k.lineH(k.small) + 10 * u
        val ch = minOf(cw * 1.5f, inner.height() - textH)
        val rows = ((inner.height() + gap) / (ch + textH + gap)).toInt().coerceIn(1, 3)
        for ((i, b) in others.take(rows * cols).withIndex()) {
            val x = inner.left + (i % cols) * (cw + gap)
            val y = inner.top + (i / cols) * (ch + textH + gap)
            val covW = minOf(cw, ch / 1.45f)
            val cr = RectF(x + (cw - covW) / 2, y, x + (cw + covW) / 2, y + ch)
            k.cover(c, b.cover, b.title, cr)
            k.centered(c, b.title, x, cw, cr.bottom + 6 * u, k.small)
            k.centered(c, "${b.percent}%", x, cw, cr.bottom + 6 * u + k.lineH(k.small), sb)
            on(RectF(x, y, x + cw, y + ch + textH), Action.READ, b.path)
        }
    }

    /** 4 · Tối giản: the clock, the book in large type, one line of the realm, the names of the screens. */
    private fun minimal(c: Canvas, k: Ink, a: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val now = LocalTime.now()
        val clockP = k.paint(96f, big = true)
        var y = a.top + 60 * u
        k.centered(c, now.hour.toString().padStart(2, '0') + ":" + now.minute.toString().padStart(2, '0'), a.left, a.width(), y, clockP)
        y += k.lineH(clockP)
        val dark = k.paint(Ink.UI10, color = Ink.DARK)
        k.centered(c, "${today.dayOfMonth}/${today.monthValue} · ${Lunar.ancientMonthDay(lunar)}", a.left, a.width(), y, dark)
        y += k.lineH(dark) + 70 * u
        c.drawRect(a.centerX() - 30 * u, y, a.centerX() + 30 * u, y + 2 * u, k.fill)
        y += 50 * u
        val b = book
        val bookTop = y
        if (b == null || needsPermission) {
            k.centered(c, if (needsPermission) "Chạm để cấp quyền đọc thẻ nhớ" else "Chạm để chọn sách", a.left, a.width(), y, k.ui12Bold)
            y += k.lineH(k.ui12Bold)
        } else {
            for (line in k.wrap(b.title, a.width() - 40 * u, k.big, 3)) {
                k.centered(c, line, a.left, a.width(), y, k.big)
                y += k.lineH(k.big)
            }
            if (b.author.isNotEmpty()) {
                k.centered(c, b.author, a.left, a.width(), y + 4 * u, k.ui10)
                y += k.lineH(k.ui10) + 4 * u
            }
            y += 30 * u
            val bw = minOf(a.width() * 0.6f, 420 * u)
            tangBar(c, k, a.centerX() - bw / 2, y, bw, 10 * u, b.percent)
            y += 26 * u
            k.centered(c, "${Realm.tangName(Realm.tangOf(b.percent))} · ${b.percent}%" + (timeLeft(b)?.let { " · còn $it" } ?: ""), a.left, a.width(), y, k.small)
            y += k.lineH(k.small)
        }
        bookTarget(RectF(a.left, bookTop - 20 * u, a.right, y + 20 * u))
        y += 70 * u
        val realmLine = "${Realm.name(Store.level())} · ${Realm.van(Store.tuVi())} vạn từ · hôm nay ${todayText()}"
        k.centered(c, realmLine, a.left, a.width(), y, k.smallBold)
        on(RectF(a.left, y - 10 * u, a.right, y + k.lineH(k.smallBold) + 10 * u), Action.TU_VI)
        y += k.lineH(k.smallBold) + 24 * u
        if (Store.showProverb) {
            for (l in k.wrap(Texts.PROVERBS[proverb], a.width() - 80 * u, dark, 3)) {
                k.centered(c, l, a.left, a.width(), y, dark)
                y += k.lineH(dark)
            }
        }
        // The screens as words along the bottom.
        val items = listOf(Action.LIBRARY to "Thư viện", Action.TU_VI to "Tu vi", Action.PHAP_BAO to "Pháp bảo",
            Action.DAO_HIEU to "Đạo hiệu", Action.NIEN_KY to "Niên kỷ", Action.SETTINGS to "Cài đặt")
        val rowY = a.bottom - 60 * u
        c.drawRect(a.left, rowY - 16 * u, a.right, rowY - 15 * u, k.gray)
        val cw = a.width() / items.size
        for ((i, item) in items.withIndex()) {
            val cell = RectF(a.left + i * cw, rowY - 14 * u, a.left + (i + 1) * cw, a.bottom)
            k.centered(c, item.second, cell.left, cell.width(), rowY + 8 * u, k.smallBold)
            on(cell, item.first)
        }
    }

    /** 5 · Tu tiên: the realm large on top, the Omega cards, the book in one row, the dock. */
    private fun cultivation(c: Canvas, k: Ink, a: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val g = Ink.GAP * u
        val dock = RectF(a.left, a.bottom - DOCK_H * u, a.right, a.bottom)
        drawDock(c, k, dock, listOf(Action.PHAP_BAO, Action.NIEN_KY, Action.SETTINGS))
        val bookRect = RectF(a.left, dock.top - g - 170 * u, a.right, dock.top - g)
        drawBookRow(c, k, bookRect); bookTarget(bookRect)
        libraryButton(c, k, bookRect)
        val mini = RectF(a.left, bookRect.top - g - MINI_H * u, a.right, bookRect.top - g)
        minis(c, k, mini, today)
        val hero = RectF(a.left, a.top, a.right, mini.top - g)
        k.card(c, hero)
        drawRealmFrame(c, k, hero, Realm.realmOf(Store.level()))
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
        k.centered(c, Realm.name(level), x, w, y, nameP)
        y += k.lineH(nameP)
        if (t >= 0) {
            k.centered(c, "« ${DaoHieu.NAMES[t]} »", x, w, y, k.ui10)
            y += k.lineH(k.ui10)
        }
        k.centered(c, "${Realm.van(words)} vạn từ tu vi", x, w, y, k.ui10Bold)
        y += k.lineH(k.ui10) + 20 * u
        y = k.realmBar(c, x + w * 0.1f, y, w * 0.8f, Realm.ninths(words, level), Realm.stageLabels(level), Realm.stageIndex(level))
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
        k.card(c, r)
        val level = Store.level()
        val pad = 20 * u
        val icon = minOf(56 * u, r.height() - 2 * pad)
        k.realmIcon(c, Realm.realmOf(level), r.left + pad, r.centerY() - icon / 2, icon)
        val tx = r.left + pad + icon + 14 * u
        val left = r.width() * 0.42f
        k.text(c, k.fit(Realm.name(level), left - icon - 2 * pad, k.ui10Bold), tx, r.centerY() - k.lineH(k.ui10Bold), k.ui10Bold)
        k.text(c, "${Realm.van(Store.tuVi())} vạn từ", tx, r.centerY() + 2 * u, k.small)
        stats(c, k, r.left + left, r.centerY() - k.lineH(k.small), r.width() - left - pad)
    }

    /** The book in one row: a small cover, the title, the tầng. */
    private fun drawBookRow(c: Canvas, k: Ink, r: RectF) {
        val u = k.u
        k.card(c, r)
        val b = book
        if (needsPermission || b == null) {
            k.centered(c, if (needsPermission) "Chạm để cấp quyền đọc thẻ nhớ" else "Chưa đọc cuốn nào · chạm để mở Thư viện", r.left, r.width(), r.centerY() - k.lineH(k.ui10) / 2, k.ui10)
            return
        }
        val pad = 16 * u
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

    /** The rail of Cổ điển: Pháp bảo with its treasure, then the screens as icon buttons. */
    private fun drawRail(c: Canvas, k: Ink, col: RectF) {
        val u = k.u
        val g = 10 * u
        val items = listOf(
            Triple(Action.NIEN_KY, "nienky", "Niên kỷ"),
            Triple(Action.SETTINGS, "settings", "Cài đặt"),
        )
        val cell = 92 * u
        val pb = RectF(col.left, col.top, col.right, col.bottom - items.size * (cell + g))
        k.card(c, pb)
        on(pb, Action.PHAP_BAO)
        val sb = k.smallBold
        val small = k.small
        val beast = Store.strongest()
        val art = minOf(pb.width() - 20 * u, pb.height() - 3 * k.lineH(small) - 50 * u)
        val block = k.lineH(sb) + 8 * u + art + 6 * u + 2 * k.lineH(small)
        val by = pb.top + maxOf(12 * u, (pb.height() - block) / 2)
        k.centered(c, "Pháp bảo", pb.left, pb.width(), by, sb)
        val ay = by + k.lineH(sb) + 8 * u
        if (beast != null) {
            k.badge(c, beast.species, pb.centerX() - art / 2, ay, art)
            var ny = ay + art + 6 * u
            for (l in k.wrap(beast.name, pb.width() - 12 * u, small, 2)) {
                k.centered(c, l, pb.left, pb.width(), ny, small)
                ny += k.lineH(small)
            }
        } else {
            k.icon(c, "forge", pb.centerX() - art * 0.4f, ay, art * 0.8f)
            k.centered(c, "Lò đang chờ", pb.left, pb.width(), ay + art * 0.8f + 6 * u, small)
        }
        var y = pb.bottom + g
        for (item in items) {
            val r = RectF(col.left, y, col.right, y + cell)
            k.card(c, r)
            val icon = 34 * u
            val total = icon + 6 * u + k.lineH(sb)
            val iy = r.top + (cell - total) / 2
            k.icon(c, item.second, r.centerX() - icon / 2, iy, icon)
            k.centered(c, item.third, r.left, r.width(), iy + icon + 6 * u, sb)
            on(r, item.first)
            y += cell + g
        }
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
        val pad = 20 * u
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
        val title = if (k.wrap(b.title, titleW, k.big, 3).size <= 2 && y + 2 * k.lineH(k.big) + k.lineH(k.ui10) <= room) k.big else k.ui12Bold
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

    // --- The realm --------------------------------------------------------------------------

    private fun drawRealm(c: Canvas, k: Ink, r: RectF, today: LocalDate, lunar: Lunar.Date) {
        val u = k.u
        val words = Store.tuVi()
        val level = Store.level()
        k.card(c, r)
        drawRealmFrame(c, k, r, Realm.realmOf(level))
        val pad = 22 * u
        val x = r.left + pad
        val w = r.width() - 2 * pad
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val bigP = k.big
        val bl = k.lineH(bigP)

        val term = Lunar.termName(Lunar.solarTerm(today.dayOfMonth, today.monthValue, today.year)) + " · " + Texts.TERM_PHRASES[termPhrase]
        var head = k.evenLines((occasion(today, lunar) ?: greeting()) + " · " + term, w, sb)
        val t = Store.shownTitle()
        val realmH = 56 * u + 2 * u + k.lineH(k.ui10Bold) + (if (t >= 0) sl else 0f) + 10 * u + 15 * u + 5 * u + sl
        val statsH = 12 * u + 2 * sl
        val saying = k.wrap(Texts.PROVERBS[proverb], w, small, 3)
        val blocks = mutableListOf(head.size * sl, realmH, statsH)
        val sayingH = 14 * u + saying.size * sl
        val inner = r.height() - 2 * pad
        if (blocks.sum() + 2 * 8 * u > inner && head.size > 1) {
            // Short of room: the greeting alone, on one line.
            head = listOf(k.fit(occasion(today, lunar) ?: greeting(), w, sb))
            blocks[0] = sl
        }
        if (Store.showProverb && blocks.sum() + sayingH + 4 * 12 * u <= inner) blocks.add(sayingH)
        val gap = ((inner - blocks.sum()) / (blocks.size - 1)).coerceIn(8 * u, 40 * u)
        var y = r.top + pad + maxOf(0f, (inner - blocks.sum() - gap * (blocks.size - 1)) / 2)

        for (l in head) {
            k.centered(c, l, x, w, y, sb)
            y += sl
        }
        y += gap

        // The realm logo and the words read, large; the realm, the title worn, the road.
        val number = Realm.van(words)
        val unit = k.ui12Bold
        val icon = 56 * u
        val nw = bigP.measureText(number)
        val uw = unit.measureText("vạn từ")
        val nx = x + maxOf(0f, (w - (icon + 12 * u + nw + 8 * u + uw)) / 2)
        k.realmIcon(c, Realm.realmOf(level), nx, y, icon)
        val ny = y + (icon - bl) / 2
        k.text(c, number, nx + icon + 12 * u, ny, bigP)
        k.text(c, "vạn từ", nx + icon + 12 * u + nw + 8 * u, ny + (-bigP.ascent()) - (-unit.ascent()), unit)
        y += icon + 2 * u
        k.centered(c, Realm.name(level), x, w, y, k.ui10Bold)
        y += k.lineH(k.ui10Bold)
        if (t >= 0) {
            k.centered(c, "« ${DaoHieu.NAMES[t]} »", x, w, y, small)
            y += sl
        }
        y += 10 * u
        y = k.realmBar(c, x, y, w, Realm.ninths(words, level), Realm.stageLabels(level), Realm.stageIndex(level))
        y += gap

        // Today, the last seven days, the streak.
        c.drawRect(x, y, x + w, y + u, k.gray)
        y += 12 * u
        stats(c, k, x, y, w)
        y += 2 * sl

        if (blocks.size == 4) {
            y += gap
            c.drawRect(x + (w - 36 * u) / 2, y, x + (w + 36 * u) / 2, y + 2 * u, k.fill)
            y += 14 * u
            val sp = k.paint(Ink.SMALL, color = Ink.DARK)
            for (l in saying) {
                k.centered(c, l, x, w, y, sp)
                y += sl
            }
        }
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
        k.centered(c, "${Store.beasts.size} món · bộ ${Store.setsCount()}/${Treasure.SETS}", x, w, footY, small)

        // The forge: three slots with their progress, when the card is tall enough for it.
        val nb = k.ui10Bold
        val top = r.top + pad + sl + 10 * u
        val oreS = 40 * u
        val forgeH = oreS + 2 * u + sl + 24 * u
        val beast = Store.strongest()
        val names = if (beast != null) k.wrap(beast.name, w, nb, 2) else listOf("Lò đang chờ")
        val textH = names.size * k.lineH(nb) + sl + 8 * u
        val showForge = footY - top - forgeH - textH >= 90 * u
        val bottom = if (showForge) footY - 14 * u - forgeH else footY - 10 * u
        if (showForge) {
            val forgeY = bottom + 24 * u
            c.drawRect(x, forgeY - 14 * u, x + w, forgeY - 13 * u, k.gray)
            val slotW = w / 3
            for (i in 0 until Store.FORGE_SLOTS) {
                val ore = Store.forge.getOrNull(i)
                val sx = x + i * slotW
                when {
                    ore == null -> k.icon(c, "forge", sx + (slotW - oreS) / 2, forgeY, oreS, Ink.LIGHT)
                    ore.isBook -> {
                        val b = Store.books[ore.book]
                        val cw = oreS * 0.68f
                        k.cover(c, b?.cover ?: "", b?.title ?: "", RectF(sx + (slotW - cw) / 2, forgeY, sx + (slotW + cw) / 2, forgeY + oreS))
                    }
                    else -> k.ore(c, ore.tier, ore.percent, sx + (slotW - oreS) / 2, forgeY, oreS)
                }
                k.centered(c, if (ore != null) "${ore.percent}%" else "trống", sx, slotW, forgeY + oreS + 2 * u, small)
            }
        }

        // The strongest treasure (or the waiting furnace), as large as the card allows.
        val art = minOf(w * 0.82f, bottom - top - textH).coerceAtLeast(48 * u)
        val ay = top + maxOf(0f, (bottom - top - art - textH) / 2)
        if (beast != null) k.badge(c, beast.species, x + (w - art) / 2, ay, art)
        else k.icon(c, "forge", x + (w - art * 0.85f) / 2, ay + art * 0.075f, art * 0.85f)
        var ny = ay + art + 8 * u
        for (l in names) {
            k.centered(c, l, x, w, ny, nb)
            ny += k.lineH(nb)
        }
        val sub = if (beast != null) Treasure.CLANS[beast.tier] + " · " + Treasure.TIER_NAMES[beast.tier] else "chạm để đặt sách vào lò"
        k.centered(c, sub, x, w, ny, small)
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
        var y = miniHead(c, k, r, "Thiên kiếp", "thienkiep")
        val pad = 16 * u
        val gate = Realm.baseName(Store.nextGate())
        when {
            Store.realmCap >= 9 -> lines(c, k, r, y, listOf("Đã phi thăng" to true, "Tiên lộ thênh thang" to false))
            Store.inTrial() -> {
                val days = Store.trialDays().first
                val need = Store.trialNeed()
                val cw = minOf(24 * u, (r.width() - 2 * pad - (need - 1) * 5 * u) / need)
                for (i in 0 until need) {
                    val cell = RectF(r.left + pad + i * (cw + 5 * u), y, r.left + pad + i * (cw + 5 * u) + cw, y + 14 * u)
                    if (i < days) c.drawRoundRect(cell, 3 * u, 3 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 3 * u, 3 * u, k.stroke(1.3f))
                }
                y += 22 * u
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
        )
        val g = Ink.GAP * u
        val cw = (r.width() - (actions.size - 1) * g) / actions.size
        val icon = 34 * u
        val p = k.ui10Bold
        for ((i, action) in actions.withIndex()) {
            val (name, label) = all.getValue(action)
            val cell = RectF(r.left + i * (cw + g), r.top, r.left + i * (cw + g) + cw, r.bottom)
            k.card(c, cell)
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

    /** The library's button in the top right corner of a book card (drawn and hit after the card). */
    private fun libraryButton(c: Canvas, k: Ink, card: RectF) {
        val u = k.u
        val r = RectF(card.right - 14 * u - LIB_W * u, card.top + 14 * u, card.right - 14 * u, card.top + 14 * u + 46 * u)
        k.button(c, r, "", k.small)
        val icon = 24 * u
        val p = k.smallBold
        val total = icon + 8 * u + p.measureText("Thư viện")
        k.icon(c, "library", r.centerX() - total / 2, r.centerY() - icon / 2, icon)
        k.text(c, "Thư viện", r.centerX() - total / 2 + icon + 8 * u, r.centerY() - k.lineH(p) / 2, p)
        on(r, Action.LIBRARY)
    }

    /** Corner marks of the card that grow with the realm. */
    private fun drawRealmFrame(c: Canvas, k: Ink, r: RectF, realm: Int) {
        val u = k.u
        val inset = 11 * u
        val pts = listOf(r.left + inset to r.top + inset, r.right - inset to r.top + inset, r.left + inset to r.bottom - inset, r.right - inset to r.bottom - inset)
        for ((x, y) in pts) {
            when (realm) {
                0 -> c.drawCircle(x, y, 2 * u, k.fill)
                1 -> { c.drawCircle(x, y, 4 * u, k.fill); c.drawCircle(x, y, 1.6f * u, k.white) }
                2 -> { c.drawCircle(x, y, 5 * u, k.fill); c.drawCircle(x, y, 3.4f * u, k.white); c.drawCircle(x, y, 1.6f * u, k.fill) }
                3 -> k.diamond(c, x, y, 6 * u)
                4, 5 -> { k.diamond(c, x, y, 6 * u); c.drawCircle(x, y, 1.6f * u, k.white) }
                else -> star(c, k, x, y, 7 * u)
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
        private const val DOCK_H = 66f
        private const val MINI_H = 118f
        private const val LIB_W = 142f
        val STYLES = listOf("Bảng tu hành", "Cổ điển (A2)", "Bìa lớn", "Kệ sách", "Tối giản", "Tu tiên")
        val STYLE_NOTES = listOf(
            "Sách đang đọc, cảnh giới và pháp bảo cạnh nhau, ba thẻ Omega, thanh điều hướng",
            "Như Exacta.OS: sách trên cùng, cột nút bên trái, thẻ tu hành bên phải",
            "Bìa sách lớn ở giữa, cảnh giới gọn một dải",
            "Sách đang đọc và một kệ các cuốn đang đọc dở",
            "Đồng hồ lớn, tên sách, một dòng cảnh giới, nhiều khoảng trắng",
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
