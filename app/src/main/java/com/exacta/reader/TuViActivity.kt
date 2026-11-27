package com.exacta.reader

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.Store
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/** Tu vi & thành tích, in tabs: the realm and its ladder, the trials, the stats, Kinh Các. */
class TuViActivity : TabActivity() {
    override fun title() = "Tu Vi"
    override fun right() = Realm.name(Store.level())
    override fun tabs() = listOf("Cảnh giới", "Thiên kiếp", "Bí cảnh", "Thống kê", "Kinh Các")

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    private var pillButton: RectF? = null

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> realm(c, k, area, hits)
        1 -> trials(c, k, area, gates = true).also {
            pillButton?.let { b -> hits.add(b) { if (Store.useBreakPill()) refresh() } }
        }
        2 -> trials(c, k, area, gates = false)
        3 -> stats(c, k, area)
        else -> kinhCac(c, k, area, page)
    }

    private fun realm(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val words = Store.tuVi()
        val level = Store.level()
        val small = k.small
        val sl = k.lineH(small)
        var y = a.top

        // The realm, large, with its logo; the title worn under it.
        val name = Realm.name(level)
        val big = k.big
        val icon = 72 * u
        val nw = big.measureText(name)
        val nx = a.left + maxOf(0f, (a.width() - icon - 14 * u - nw) / 2)
        k.realmIcon(c, Realm.realmOf(level), nx, y, icon)
        k.text(c, name, nx + icon + 14 * u, y + (icon - k.lineH(big)) / 2, big)
        y += icon + 8 * u
        val t = Store.shownTitle()
        if (t >= 0) {
            val r = RectF(a.left, y, a.right, y + k.lineH(k.ui10Bold))
            k.centered(c, "« ${DaoHieu.NAMES[t]} »", a.left, a.width(), y, k.ui10Bold)
            hits.add(r) { startActivity(Intent(this, DaoHieuActivity::class.java)) }
            y += k.lineH(k.ui10Bold) + 4 * u
        }
        val lost = if (Store.tuViLoss > 0) " · mất ${Realm.van(Store.tuViLoss)} vạn vì độ kiếp" else ""
        k.centered(c, "Tu vi ${Realm.van(words)} vạn từ$lost", a.left, a.width(), y, small)
        y += sl + 12 * u
        y = k.realmBar(c, a.left + 30 * u, y, a.width() - 60 * u, Realm.ninths(words, level), Realm.stageLabels(level), Realm.stageIndex(level))
        y += 8 * u
        val next = when {
            Store.inTrial() -> "Đang độ kiếp vào ${Realm.baseName(Store.nextGate())}"
            level < Realm.LEVELS - 1 -> "Còn ${Realm.van(Realm.floor(level + 1) - words)} vạn từ tới ${Realm.name(level + 1)}"
            else -> "Đã tới đỉnh tiên lộ"
        }
        k.centered(c, next, a.left, a.width(), y, k.smallBold)
        y += sl + 18 * u

        // The ladder of ten realms fills the rest.
        y = k.section(c, a.left, y, a.width(), "CẢNH GIỚI")
        val here = Realm.realmOf(level)
        val rowH = ((a.bottom - y) / 10).coerceIn(34 * u, 64 * u)
        for (i in 9 downTo 0) {
            val r = RectF(a.left, y, a.right, y + rowH - 4 * u)
            if (i == here) k.card(c, r, selected = true)
            val ic = minOf(r.height() - 6 * u, 40 * u)
            k.realmIcon(c, i, r.left + 10 * u, r.top + (r.height() - ic) / 2, ic)
            val p = if (i == here) k.ui10Bold else k.ui10
            val label = when {
                i == here -> name
                i == Store.realmCap + 1 -> Realm.baseName(i) + "  · cửa ải kế tiếp"
                else -> Realm.baseName(i)
            }
            k.text(c, k.fit(label, r.width() - 190 * u, p), r.left + ic + 24 * u, r.top + (r.height() - k.lineH(p)) / 2, p)
            k.right(c, "${Realm.floor(i * 3) / 10000} vạn", r.right - 12 * u, r.top + (r.height() - sl) / 2, if (i < here) k.smallBold else small)
            y += rowH
        }
        return 1
    }

    /** Thiên kiếp with the nine gates (gates = true), or the week's bí cảnh. */
    private fun trials(c: Canvas, k: Ink, a0: RectF, gates: Boolean): Int {
        // Each tab shows one card; the other is drawn off the page.
        val a = if (gates) RectF(a0.left, a0.top, a0.right, a0.bottom + 2000 * k.u) else RectF(a0.left, a0.top - 2000 * k.u, a0.right, a0.bottom)
        if (!gates) {
            biCanhPage(c, k, a0)
            return 1
        }
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        val iconS = 52 * u
        val tx = a.left + pad + iconS + 18 * u
        val tw = a.right - pad - tx

        // Thiên kiếp: what comes next, sized to its text.
        val gate = Realm.baseName(Store.nextGate())
        val rule = "${Store.trialNeed()} ngày liền, mỗi ngày đọc từ ${Store.trialMinutes()} phút"
        val lines = when {
            Store.realmCap >= 9 -> listOf("Đã độ kiếp phi thăng. Tiên nhân không còn thiên kiếp." to false)
            Store.inTrial() -> listOf(
                "Đang độ kiếp ${Treasure.GATES[Store.nextGate()]}: ${Store.trialDays().first}/${Store.trialNeed()} ngày" to true,
                "Cần $rule. Hôm nay ${Store.minutes(Store.today())}/${Store.trialMinutes()} phút. Lỡ một ngày ở giữa là thất bại." to false,
            )
            else -> listOf(
                "Kiếp tới: ${Treasure.GATES[Store.nextGate()]} (vào $gate), còn ${Realm.van(Store.wordsToGate())} vạn từ" to true,
                "Khi tu vi chạm cửa ải, thiên kiếp giáng: $rule. Lỡ một ngày ở giữa là thất bại, tu vi lui về gần cửa ải." to false,
            )
        }
        val wrapped = lines.map { (t, b) -> k.wrap(t, tw, if (b) sb else small, 3) to b }
        val cellsH = if (Store.inTrial()) 34 * u else 0f
        val topH = pad + k.lineH(k.ui12Bold) + 4 * u + wrapped.sumOf { it.first.size.toDouble() }.toFloat() * sl + cellsH + 10 * u + sl + pad
        val top = RectF(a.left, a.top, a.right, a.top + maxOf(topH, iconS + 2 * pad))
        k.card(c, top)
        k.icon(c, "thienkiep", top.left + pad, top.top + pad, iconS)
        k.text(c, "Thiên kiếp", tx, top.top + pad, k.ui12Bold)
        var y = top.top + pad + k.lineH(k.ui12Bold) + 4 * u
        for ((ls, bold) in wrapped) for (l in ls) {
            k.text(c, l, tx, y, if (bold) sb else small)
            y += sl
        }
        if (Store.inTrial()) {
            val need = Store.trialNeed()
            val days = Store.trialDays().first
            val cw = minOf(64 * u, (tw - (need - 1) * 8 * u) / need)
            y += 8 * u
            for (i in 0 until need) {
                val cell = RectF(tx + i * (cw + 8 * u), y, tx + i * (cw + 8 * u) + cw, y + 20 * u)
                if (i < days) c.drawRoundRect(cell, 5 * u, 5 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 5 * u, 5 * u, k.stroke(1.6f))
            }
            y += 26 * u
        }
        val tally = "Đã vượt ${Store.trialPassed} lần · thất bại ${Store.failCount} lần" + if (Store.setActive(4)) " · Đạo Môn Tứ Bảo tha 1 ngày" else ""
        k.text(c, k.fit(tally, tw, small), tx, top.bottom - pad - sl, small)
        if (Store.canBreakPill()) {
            // Phá Cảnh Đan: a day of this hard gate.
            val btn = RectF(top.right - pad - 210 * u, top.top + pad, top.right - pad, top.top + pad + 42 * u)
            k.button(c, btn, "Dùng Phá Cảnh Đan (${Store.items[Treasure.PHA_CANH_DAN]})", k.smallBold)
            pillButton = btn
        } else {
            pillButton = null
        }

        // Between them, the nine gates and what each asks, in two columns.
        y = k.section(c, a.left, top.bottom + 18 * u, a.width(), "CỬA ẢI THIÊN KIẾP")
        val gap = 10 * u
        val colW = (a.width() - gap) / 2
        val rowH = ((a0.bottom - y) / 5).coerceIn(2 * sl + 8 * u, 96 * u)
        for (r in 1..9) {
            val col = (r - 1) / 5
            val row = (r - 1) % 5
            val passed = Store.realmCap >= r
            val now = Store.inTrial() && Store.nextGate() == r
            val cell = RectF(a.left + col * (colW + gap), y + row * rowH, a.left + col * (colW + gap) + colW, y + row * rowH + rowH - 6 * u)
            if (now) k.card(c, cell, selected = true) else c.drawRect(cell.left, cell.bottom + 2 * u, cell.right, cell.bottom + 3 * u, k.gray)
            val ic = minOf(cell.height() - 8 * u, 34 * u)
            k.realmIcon(c, r, cell.left + 8 * u, cell.centerY() - ic / 2, ic)
            val tx2 = cell.left + ic + 20 * u
            val tw2 = cell.right - 8 * u - tx2
            val ty2 = cell.centerY() - sl
            k.text(c, k.fit(Treasure.GATES[r], tw2, if (passed || now) sb else small), tx2, ty2, if (passed || now) sb else small)
            val state = when { passed -> "đã vượt"; now -> "đang độ kiếp"; else -> "chưa tới" }
            k.text(c, k.fit("${TRIAL_DAYS[r - 1]} ngày × ${TRIAL_MINUTES[r - 1]} phút · $state", tw2, small), tx2, ty2 + sl, small)
        }
        return 1
    }

    private fun biCanhPage(c: Canvas, k: Ink, a: RectF) {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        val iconS = 52 * u
        val tx = a.left + pad + iconS + 18 * u
        val tw = a.right - pad - tx
        var y: Float
        // Bí cảnh tuần.
        val today = Store.today()
        val need = Store.biCanhNeed()
        val status = if (Store.biCanhDone()) "Tuần này đã qua bí cảnh, nhận khoáng thạch." else
            "Thứ Hai đến Chủ nhật, đủ $need ngày đọc từ ${Store.BI_CANH_MINUTES} phút thì nhận một khoáng thạch Huyền phẩm trở lên."
        val statusLines = k.wrap(status, tw, small, 3)
        val cellH = 66 * u
        val botH = pad + k.lineH(k.ui12Bold) + 4 * u + statusLines.size * sl + 14 * u + cellH + 12 * u + sl + pad
        val bot = RectF(a.left, a.top, a.right, a.top + botH)
        k.card(c, bot)
        k.icon(c, "bicanh", bot.left + pad, bot.top + pad, iconS)
        k.text(c, "Bí cảnh tuần", tx, bot.top + pad, k.ui12Bold)
        y = bot.top + pad + k.lineH(k.ui12Bold) + 4 * u
        for (l in statusLines) {
            k.text(c, l, tx, y, small)
            y += sl
        }
        y += 14 * u
        val monday = Store.weekStart(today)
        val names = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dw = (bot.width() - 2 * pad - 6 * 8 * u) / 7
        for (i in 0 until 7) {
            val d = monday + i
            val x = bot.left + pad + i * (dw + 8 * u)
            val mins = Store.minutes(d)
            val cell = RectF(x, y, x + dw, y + cellH)
            val met = mins >= Store.BI_CANH_MINUTES
            when {
                met -> c.drawRoundRect(cell, 8 * u, 8 * u, k.fill)
                d < today -> c.drawRoundRect(cell, 8 * u, 8 * u, k.gray)
                else -> c.drawRoundRect(k.inset(cell, 0.6f * u), 8 * u, 8 * u, k.stroke(if (d == today) 2.6f else 1.3f))
            }
            val p = if (met) k.paint(Ink.SMALL, bold = true, color = Color.WHITE) else sb
            k.centered(c, names[i], x, dw, cell.top + 8 * u, p)
            if (d <= today) k.centered(c, "$mins phút", x, dw, cell.bottom - 8 * u - sl, if (met) k.paint(Ink.SMALL, color = Color.WHITE) else small)
        }
        k.text(c, "Tuần này ${Store.biCanhDays()}/$need ngày · đã qua ${Store.biCanhCount} lần", bot.left + pad, bot.bottom - pad - sl, sb)


        // How it works, and what the sets add.
        y = k.section(c, a.left, bot.bottom + 24 * u, a.width(), "LUẬT BÍ CẢNH")
        val rules = listOf(
            "Mỗi tuần tính từ thứ Hai đến Chủ nhật; ngày nào đọc đủ ${Store.BI_CANH_MINUTES} phút là một ngày bí cảnh.",
            "Đủ ${Store.biCanhNeed()} ngày trong tuần: nhận ngay một khoáng thạch Huyền phẩm trở lên vào lò luyện.",
            "Thương Kích Liệt Trận: chỉ cần 4 ngày" + if (Store.setActive(2)) " (đang có)." else ".",
            "Phong Thần Bảo Lục: thêm một khoáng thạch" + if (Store.setActive(5)) " (đang có)." else ".",
            "Qua bí cảnh 10 lần đắc đạo hiệu Bí Cảnh Thám Hiểm.",
        )
        for (r in rules) {
            for (l in k.wrap(r, a.width() - 24 * u, small, 3)) {
                if (y + sl > a.bottom) break
                k.text(c, l, a.left + 20 * u, y, small)
                y += sl
            }
            k.diamond(c, a.left + 6 * u, y - sl * 0.5f - (sl * (k.wrap(r, a.width() - 24 * u, small, 3).size - 1)), 4 * u)
            y += 10 * u
        }
    }

    private val TRIAL_MINUTES = intArrayOf(30, 30, 35, 40, 45, 45, 50, 55, 60)
    private val TRIAL_DAYS = intArrayOf(3, 3, 4, 4, 5, 5, 6, 6, 7)

    private fun stats(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val d = Store.today()
        val small = k.small
        val sl = k.lineH(small)
        val finished = Store.books.values.count { it.finishDay >= 0 }
        val wpm = Store.pace()
        val stats = listOf(
            "Thời gian đọc" to HomeView.duration(Store.totalSeconds),
            "Đã đọc" to "${Realm.van(Store.totalWords)} vạn từ",
            "Hôm nay" to HomeView.duration(Store.daySeconds[d] ?: 0),
            "7 ngày qua" to HomeView.hours(Store.secondsBetween(d - 6, d)),
            "30 ngày qua" to HomeView.hours(Store.secondsBetween(d - 29, d)),
            "Chữ 30 ngày" to "${Realm.van(Store.wordsBetween(d - 29, d))} vạn",
            "Chuỗi ngày đọc" to "${Store.streak()} ngày",
            "Chuỗi dài nhất" to "${Store.bestStreak()} ngày",
            "Ngày đọc nhiều nhất" to HomeView.duration(Store.daySeconds.values.maxOrNull() ?: 0L),
            "Lần đọc dài nhất" to HomeView.duration(Store.bestSession),
            "Sách đã mở" to "${Store.books.values.count { it.lastOpened > 0 }} cuốn",
            "Đọc xong" to "$finished cuốn",
            "Tầng công pháp" to "${Store.tangCount} lần",
            "Tốc độ" to if (wpm > 0) "$wpm từ/phút" else "đọc thêm",
            "Vượt thiên kiếp" to "${Store.trialPassed} lần",
            "Bí cảnh" to "${Store.biCanhCount} lần",
            "Pháp bảo" to "${Store.beasts.size} món · ${Store.kinds().size}/36 loại",
            "Bộ pháp bảo" to "${Store.setsCount()}/${Treasure.SETS} bộ",
        )
        var y = k.section(c, a.left, a.top, a.width(), "THỐNG KÊ")
        val cols = if (k.W >= 600) 2 else 1
        val colW = (a.width() - (cols - 1) * 28 * u) / cols
        val rows = (stats.size + cols - 1) / cols
        val rowH = sl + 10 * u
        for ((i, pair) in stats.withIndex()) {
            val x = a.left + (i / rows) * (colW + 28 * u)
            k.pair(c, pair.first, pair.second, x, y + (i % rows) * rowH, colW)
        }
        y += rows * rowH + 18 * u

        // The last 14 days fill the rest.
        y = k.section(c, a.left, y, a.width(), "14 NGÀY QUA (PHÚT)")
        val chartH = a.bottom - y - sl - 4 * u
        val days = (13 downTo 0).map { Store.daySeconds[d - it] ?: 0L }
        val peak = maxOf(days.maxOrNull() ?: 0L, 60L)
        val slot = a.width() / 14
        for ((i, s) in days.withIndex()) {
            val bx = a.left + i * slot
            val bh = if (s == 0L) 0f else maxOf(3 * u, s.toFloat() / peak * (chartH - sl - 6 * u))
            c.drawRect(bx, y + chartH - u, bx + slot - 2 * u, y + chartH, k.fill)
            if (bh > 0) c.drawRoundRect(bx + 4 * u, y + chartH - bh, bx + slot - 6 * u, y + chartH, 3 * u, 3 * u, if (i == 13) k.fill else k.gray)
            if (s >= 60) k.centered(c, "${s / 60}", bx, slot - 2 * u, y + chartH - bh - sl - 2 * u, small)
            k.centered(c, LocalDate.ofEpochDay(d - 13 + i).dayOfMonth.toString(), bx, slot, y + chartH + 3 * u, if (i == 13) k.smallBold else small)
        }
        return 1
    }

    private fun kinhCac(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val finished = Store.books.values.filter { it.finishDay >= 0 }.sortedByDescending { it.finishDay }
        var y = k.section(c, a.left, a.top, a.width(), "KINH CÁC · ${finished.size} BỘ CÔNG PHÁP")
        if (finished.isEmpty()) {
            k.text(c, "Đọc hết một cuốn, sách sẽ được cất vào đây.", a.left, y, k.small)
            return 1
        }
        val rowH = 120 * u
        val per = ((a.bottom - y) / rowH).toInt().coerceAtLeast(1)
        val pages = (finished.size + per - 1) / per
        val tb = k.ui10Bold
        val small = k.small
        for (b in finished.drop(page * per).take(per)) {
            val cr = RectF(a.left, y, a.left + 72 * u, y + rowH - 14 * u)
            k.cover(c, b.cover, b.title, cr)
            val tx = cr.right + 16 * u
            val tw = a.right - tx
            var ty = y + 4 * u
            for (l in k.wrap(b.title, tw, tb, 2)) {
                k.text(c, l, tx, ty, tb)
                ty += k.lineH(tb)
            }
            if (b.author.isNotEmpty()) {
                k.text(c, k.fit(b.author, tw, small), tx, ty, small)
                ty += k.lineH(small)
            }
            val day = LocalDate.ofEpochDay(b.finishDay)
            k.text(c, k.fit("Xong ${day.dayOfMonth}/${day.monthValue}/${day.year} · ${Realm.van(b.words.coerceAtLeast(0))} vạn từ · ${HomeView.duration(b.seconds)}", tw, small), tx, ty, small)
            y += rowH
        }
        return pages
    }
}
