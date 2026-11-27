package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.Lunar
import com.exacta.reader.game.Treasure
import com.exacta.reader.game.KyNgo
import com.exacta.reader.game.NienKy
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/**
 * Niên kỷ: one tab per lunar year with a journal, each summed up on one page; then the stats of
 * the whole way, and the book of the 36 kỳ ngộ.
 */
class NienKyActivity : TabActivity() {
    private val reports = HashMap<Int, NienKy.Report>()
    private var years = 1

    private fun report(back: Int) = reports.getOrPut(back) { NienKy.report(back) }

    override fun title() = "Niên Kỷ"
    override fun tabs() = (0 until years).map { if (it == 0) "Năm nay" else "Năm " + Lunar.yearName(report(it).lunarYear) } + listOf("Thống kê", "Kỳ ngộ")

    override fun onCreate(savedInstanceState: Bundle?) {
        years = NienKy.yearsWithData()
        super.onCreate(savedInstanceState)
        val due = NienKy.due()
        if (due != 0) {
            // At Tết the year just ended opens first, once.
            if (savedInstanceState == null && years > 1) selectTab(1)
            Store.nienKySeen = due
            Store.touch()
        }
    }

    private fun date(d: Long) = LocalDate.ofEpochDay(d).let { "${it.dayOfMonth}/${it.monthValue}/${it.year}" }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int {
        if (tab == years) return stats(c, k, area)
        if (tab == years + 1) return kyNgo(c, k, area, page)
        val u = k.u
        val r = report(tab)
        val small = k.small
        val sl = k.lineH(small)
        var y = area.top

        // Heading.
        k.icon(c, "nienky", area.left, y, 64 * u)
        val tx = area.left + 80 * u
        k.text(c, "Niên kỷ năm ${Lunar.yearName(r.lunarYear)}", tx, y, k.big)
        y += k.lineH(k.big)
        val span = "Từ ${date(r.from)} đến " + if (r.current) "hôm nay" else date(r.to)
        k.text(c, span + if (r.partial) " · nhật ký bắt đầu muộn hơn" else "", tx, y, small)
        y = maxOf(y + sl, area.top + 64 * u) + 20 * u

        // Four large numbers.
        val gap = 12 * u
        val bw = (area.width() - 3 * gap) / 4
        val bh = 118 * u
        val big = listOf(
            HomeView.hours(r.seconds) to "thời gian đọc",
            "${Realm.van(r.words)} vạn" to "chữ đã đọc",
            "${r.daysRead}/${r.daysTotal}" to "ngày có đọc",
            "${r.bestStreak} ngày" to "chuỗi dài nhất",
        )
        for ((i, pair) in big.withIndex()) {
            val b = RectF(area.left + i * (bw + gap), y, area.left + i * (bw + gap) + bw, y + bh)
            k.card(c, b)
            k.centered(c, pair.first, b.left + 6 * u, b.width() - 12 * u, b.top + 24 * u, k.ui12Bold)
            k.centered(c, pair.second, b.left + 6 * u, b.width() - 12 * u, b.top + 30 * u + k.lineH(k.ui12Bold), small)
        }
        y += bh + 24 * u

        // The year in lines.
        y = k.section(c, area.left, y, area.width(), "DẤU ẤN TRONG NĂM")
        val month = if (r.bestMonth > 0) "Tháng ${r.bestMonth}/${r.bestMonthYear} · ${HomeView.hours(r.bestMonthSeconds)}" else "–"
        val rows = listOf(
            "Ngày đọc nhiều nhất" to if (r.bestDay >= 0) "${date(r.bestDay)} · ${HomeView.duration(r.bestDaySeconds)}" else "–",
            "Tháng đọc nhiều nhất" to month,
            "Sách đọc xong" to "${r.booksFinished} cuốn",
            "Cuốn đọc lâu nhất" to if (r.longestBook.isNotEmpty()) "${r.longestBook} · ${HomeView.duration(r.longestBookSeconds)}" else "–",
            "Pháp bảo luyện thành" to "${r.treasures} món",
            "Pháp bảo mạnh nhất" to r.bestTreasure.ifEmpty { "–" },
            "Đạo hiệu đắc" to "${r.titles}",
            "Cảnh giới" to if (r.realmFrom == r.realmTo) Realm.name(r.realmTo) else "${Realm.name(r.realmFrom)} → ${Realm.name(r.realmTo)}",
        )
        val rowH = ((area.bottom - y) / rows.size).coerceIn(sl + 10 * u, 76 * u)
        for ((label, value) in rows) {
            k.pair(c, label, value, area.left, y + (rowH - sl) / 2, area.width())
            c.drawRect(area.left, y + rowH - u, area.right, y + rowH, k.gray)
            y += rowH
        }
        return 1
    }

    // --- Thống kê: the whole way so far ---------------------------------------------------------------

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
            "Ngộ tính" to "${DaoPhap.ngo} (đã nhận ${DaoPhap.ngoEarned})",
            "Tốc độ" to if (wpm > 0) "$wpm từ/phút" else "đọc thêm",
            "Đột phá" to "${TuLuyen.breaks} lần · hỏng ${TuLuyen.breakFails}",
            "Bí cảnh" to "${Store.biCanhCount} lần",
            "Pháp bảo" to "${Store.beasts.size} món · ${Store.kinds().size}/${Treasure.SPECIES} loại",
            "Bộ pháp bảo" to "${Store.setsCount()}/${Treasure.SETS} bộ",
            "Linh thạch" to "${Store.linhThach} (đã nhận ${Store.linhThachEarned})",
            "Bế quan viên mãn" to "${Store.beQuanDone} lần · ${HomeView.hours(Store.beQuanMinutes * 60)}",
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

    // --- Kỳ ngộ: the 36, met or still waiting ---------------------------------------------------------

    private fun kyNgo(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val met = KyNgo.EVENTS.indices.count { Store.kyNgoMet[it] > 0 }
        var y = k.section(c, a.left, a.top, a.width(), "KỲ NGỘ LỤC · ĐÃ GẶP $met/${KyNgo.EVENTS.size} LOẠI · ${Store.kyNgoCount} LẦN")
        if (page == 0) {
            val recent = Store.kyNgoLog.take(3)
            for (l in recent.ifEmpty { listOf("Chưa gặp kỳ ngộ nào. Đọc đều, đọc lâu, đọc vào giờ lành ngày lễ.") }) {
                k.text(c, k.fit(l, a.width(), small), a.left, y, small)
                y += sl
            }
            y += 10 * u
        }
        val rowH = k.lineH(k.ui10Bold) + sl + 18 * u
        val top = y
        val per = ((a.bottom - top) / rowH).toInt().coerceAtLeast(1)
        val perRest = ((a.bottom - a.top - k.lineH(sb) - 14 * u) / rowH).toInt().coerceAtLeast(1)
        val from = if (page == 0) 0 else per + (page - 1) * perRest
        val count = if (page == 0) per else perRest
        for (i in (from until minOf(KyNgo.EVENTS.size, from + count))) {
            val e = KyNgo.EVENTS[i]
            val n = Store.kyNgoMet[i]
            val p = if (n > 0) k.ui10Bold else k.ui10
            val right = (if (n > 0) "gặp $n lần · " else "chưa gặp · ") + "${(Store.luck(e.permille) + 5) / 10}% · ${KyNgo.TIERS[e.tier]}"
            k.text(c, k.fit(e.name, a.width() - sb.measureText(right) - 16 * u, p), a.left, y, p)
            k.right(c, right, a.right, y + (k.lineH(p) - k.lineH(sb)) / 2, if (n > 0) sb else small)
            k.text(c, k.fit(e.text, a.width(), small), a.left, y + k.lineH(p) + 2 * u, small)
            c.drawRect(a.left, y + rowH - 6 * u, a.right, y + rowH - 5 * u, k.gray)
            y += rowH
        }
        val rest = KyNgo.EVENTS.size - per
        return if (rest <= 0) 1 else 1 + (rest + perRest - 1) / perRest
    }
}
