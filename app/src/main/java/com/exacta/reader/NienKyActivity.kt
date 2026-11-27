package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.Store
import com.exacta.reader.game.Lunar
import com.exacta.reader.game.NienKy
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/** Niên kỷ: one tab per lunar year with a journal, each summed up on one page. */
class NienKyActivity : TabActivity() {
    private val reports = HashMap<Int, NienKy.Report>()
    private var years = 1

    private fun report(back: Int) = reports.getOrPut(back) { NienKy.report(back) }

    override fun title() = "Niên Kỷ"
    override fun tabs() = (0 until years).map { if (it == 0) "Năm nay" else "Năm " + Lunar.yearName(report(it).lunarYear) }

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
}
