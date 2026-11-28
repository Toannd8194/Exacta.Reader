package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.NhatKy
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
 * the whole way, the way of reading kept for part two (2.8), and the book of the 36 kỳ ngộ.
 */
class NienKyActivity : TabActivity() {
    private val reports = HashMap<Int, NienKy.Report>()
    private var years = 1

    private fun report(back: Int) = reports.getOrPut(back) { NienKy.report(back) }

    override fun title() = "Niên Kỷ"
    override fun tabs() = (0 until years).map { if (it == 0) "Năm nay" else "Năm " + Lunar.yearName(report(it).lunarYear) } + listOf("Thống kê", "Lối đọc", "Kỳ ngộ")

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
        if (tab == years + 1) return way(c, k, area, page)
        if (tab == years + 2) return kyNgo(c, k, area, page)
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

    // --- Lối đọc: the journal of 2.8, and what part two would make of it --------------------------------

    private fun way(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        if (page == 1) return giois(c, k, a)
        val u = k.u
        val d = Store.today()
        val small = k.small
        val sl = k.lineH(small)
        val s = NhatKy.summary(d - 29, d)
        val jumps = (d - 29..d).sumOf { TuLuyen.dayJumps[it] ?: 0 }
        val rows = listOf(
            "Đọc đêm (22 – 4 giờ)" to HomeView.duration(s.night),
            "Sáng sớm (4 – 7 giờ)" to HomeView.duration(s.dawn),
            "Buổi đọc" to "${s.sittings} buổi",
            "Buổi dài nhất" to HomeView.duration(s.longest.toLong()),
            "Buổi từ 45 phút" to "${s.long45} buổi",
            "Buổi từ 90 phút" to "${s.long90} buổi",
            "Nhảy chương" to "$jumps lần",
            "Cân bằng năm thước đo" to "${NhatKy.balance()}%",
            "Chữ đọc lại" to "${Realm.van(s.reread)} vạn",
            "Sách quay lại sau 90 ngày" to "${s.comeBack} cuốn",
            "Chữ ở sách quay lại" to "${Realm.van(s.back)} vạn",
            "Sách đã đọc" to "${s.books} cuốn · ${s.authors} tác giả",
            "Thể loại" to "${s.genres}",
            "Ngôn ngữ" to "${s.langs}",
        )
        var y = k.section(c, a.left, a.top, a.width(), "LỐI ĐỌC 30 NGÀY QUA")
        val cols = if (k.W >= 600) 2 else 1
        val colW = (a.width() - (cols - 1) * 28 * u) / cols
        val per = (rows.size + cols - 1) / cols
        val rowH = sl + 10 * u
        for ((i, p) in rows.withIndex()) k.pair(c, p.first, p.second, a.left + (i / per) * (colW + 28 * u), y + (i % per) * rowH, colW)
        y += per * rowH + 18 * u

        // The hours of the last 30 days.
        y = k.section(c, a.left, y, a.width(), "GIỜ ĐỌC 30 NGÀY QUA (PHÚT)")
        val hours = LongArray(24)
        for (day in d - 29..d) NhatKy.days[day]?.hours?.forEachIndexed { i, v -> hours[i] += v }
        val noteH = 2 * sl + 10 * u
        val chartH = (a.bottom - y - sl - 6 * u - noteH).coerceAtMost(260 * u)
        val peak = maxOf(hours.maxOrNull() ?: 0L, 60L)
        val slot = a.width() / 24
        for ((i, v) in hours.withIndex()) {
            val bx = a.left + i * slot
            val bh = if (v < 60) 0f else maxOf(3 * u, v.toFloat() / peak * (chartH - 6 * u))
            c.drawRect(bx, y + chartH - u, bx + slot - u, y + chartH, k.fill)
            if (bh > 0) c.drawRect(bx + 2 * u, y + chartH - bh, bx + slot - 3 * u, y + chartH, if (i >= 22 || i < 4) k.fill else k.gray)
            if (i % 3 == 0) k.centered(c, "$i", bx, slot, y + chartH + 3 * u, small)
        }
        y += chartH + sl + 14 * u
        val since = if (NhatKy.since >= 0) date(NhatKy.since) else "hôm nay"
        val note = "Sổ ghi theo ngày từ $since (bản 2.8): giờ đọc, từng buổi, chữ đọc lại, thể loại và ngôn ngữ sách. Phần hai dùng sổ này để tính tài nguyên mỗi giới."
        for (l in k.wrap(note, a.width(), small, 2)) {
            k.text(c, l, a.left, y, small)
            y += sl
        }
        return 2
    }

    private fun giois(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val d = Store.today()
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "NẾU ĐÃ Ở LỤC GIỚI · 30 NGÀY QUA")
        val est = (0 until 6).map { NhatKy.estimate(it, d - 29, d) }
        val best = est.indices.maxByOrNull { est[it].points } ?: 0
        val pad = 12 * u
        val gap = 10 * u
        val noteH = 3 * sl + 8 * u
        val h = ((a.bottom - y - noteH - 5 * gap) / 6).coerceAtMost(2 * pad + k.lineH(k.ui10Bold) + 4 * u + 2 * sl)
        for (g in 0 until 6) {
            val r = RectF(a.left, y, a.right, y + h)
            k.card(c, r, selected = g == best && est[g].points > 0)
            val e = est[g]
            val value = "${e.points} ${NhatKy.RESOURCE[g]}"
            k.right(c, value, r.right - pad, r.top + pad, k.ui10Bold)
            val nameW = r.width() - 2 * pad - k.ui10Bold.measureText(value) - 16 * u
            k.text(c, k.fit(TuLuyen.GIOI[g], nameW, k.ui10Bold), r.left + pad, r.top + pad, k.ui10Bold)
            var ly = r.top + pad + k.lineH(k.ui10Bold) + 4 * u
            for (l in k.wrap(NhatKy.WAY[g], r.width() - 2 * pad, small, 2)) {
                k.text(c, l, r.left + pad, ly, small)
                ly += sl
            }
            y = r.bottom + gap
        }
        val measured = est.firstOrNull()?.measured ?: 0
        val note = "Dự tính theo luật nháp của phần hai, có sổ $measured/30 ngày; ngày chưa có sổ tính 1,5 điểm. Giới thật do cảm ứng định, không do chọn."
        for (l in k.wrap(note, a.width(), sb, 3)) {
            k.text(c, l, a.left, y, sb)
            y += sl
        }
        return 2
    }

    // --- Kỳ ngộ: the 36, met or still waiting ---------------------------------------------------------

    private fun kyNgo(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val met = KyNgo.EVENTS.indices.count { Store.kyNgoMet[it] > 0 }
        var y = k.section(c, a.left, a.top, a.width(), "KỲ NGỘ LỤC · ĐÃ GẶP $met/${KyNgo.EVENTS.size} · ${Store.kyNgoCount} LẦN")
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
        // From the easiest to meet to the rarest.
        for (i in KyNgo.ORDER.drop(from).take(count)) {
            val e = KyNgo.EVENTS[i]
            val n = Store.kyNgoMet[i]
            val p = if (n > 0) k.ui10Bold else k.ui10
            val right = KyNgo.RARITY[e.rarity] + if (n > 0) " · gặp $n lần" else " · chưa gặp"
            k.text(c, k.fit(e.name, a.width() - sb.measureText(right) - 16 * u, p), a.left, y, p)
            k.right(c, right, a.right, y + (k.lineH(p) - k.lineH(sb)) / 2, if (n > 0) sb else small)
            k.text(c, k.fit("${e.text} · mỗi lần ${(Store.luck(e.permille) + 5) / 10}%", a.width(), small), a.left, y + k.lineH(p) + 2 * u, small)
            c.drawRect(a.left, y + rowH - 6 * u, a.right, y + rowH - 5 * u, k.gray)
            y += rowH
        }
        val rest = KyNgo.EVENTS.size - per
        return if (rest <= 0) 1 else 1 + (rest + perRest - 1) / perRest
    }
}
