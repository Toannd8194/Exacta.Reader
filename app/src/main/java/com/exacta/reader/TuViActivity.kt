package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.exacta.reader.data.Store
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.PageView
import java.time.LocalDate

/** Tu vi & thành tích: the realm and its ladder, reading stats, the last 14 days, Kinh Các. */
class TuViActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val level = Realm.level(Store.totalWords)
        val a = this
        setContentView(LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(Widgets.header(a, "Tu Vi", Realm.name(level)))
            addView(Widgets.rule(a), LinearLayout.LayoutParams(-1, Widgets.dp(a, 1)))
            addView(ScrollView(a).apply { addView(PageView(a) { _, c, k -> page(c, k) }) }, LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun page(c: Canvas?, k: Ink): Float {
        val u = k.u
        val w = 480 * u
        val m = Ink.MARGIN * u
        val words = Store.totalWords
        val level = Realm.level(words)
        val small = k.small
        val smallBold = k.smallBold
        val sl = k.lineH(small)
        var y = 18 * u

        // The realm, large, with its logo.
        val name = Realm.name(level)
        val big = k.big
        val icon = 48 * u
        val nw = big.measureText(name)
        val nx = maxOf(m, (w - icon - 12 * u - nw) / 2)
        if (c != null) {
            k.realmIcon(c, Realm.realmOf(level), nx, y + (k.lineH(big) - icon) / 2, icon)
            k.text(c, name, nx + icon + 12 * u, y, big)
        }
        y += k.lineH(big) + 4 * u
        if (c != null) k.centered(c, "Đã đọc ${Realm.van(words)} vạn từ", 0f, w, y, small)
        y += sl + 10 * u
        y = if (c != null) {
            k.realmBar(c, m + 20 * u, y, w - 2 * m - 40 * u, Realm.ninths(words, level), Realm.stageLabels(level), Realm.stageIndex(level))
        } else {
            y + 20 * u + sl
        }
        y += 8 * u
        if (level < Realm.LEVELS - 1) {
            val next = Realm.floor(level + 1)
            if (c != null) k.centered(c, "Còn ${Realm.van(next - words)} vạn từ tới ${Realm.name(level + 1)}", 0f, w, y, smallBold)
            y += sl
        }
        y += 18 * u

        // The ladder of ten realms.
        y = section(c, k, y, "CẢNH GIỚI")
        val here = Realm.realmOf(level)
        val rowH = 38 * u
        for (i in 9 downTo 0) {
            if (c != null) {
                val r = RectF(m, y, w - m, y + rowH - 4 * u)
                if (i == here) k.card(c, r, selected = true)
                k.realmIcon(c, i, r.left + 10 * u, r.top + (r.height() - 26 * u) / 2, 26 * u)
                val p = if (i == here) k.ui10Bold else k.ui10
                k.text(c, if (i == here) name else Realm.baseName(i), r.left + 48 * u, r.top + (r.height() - k.lineH(p)) / 2, p)
                val van = (Realm.floor(i * 3) / 10000).toString()
                k.right(c, "$van vạn", r.right - 12 * u, r.top + (r.height() - sl) / 2, if (i < here) smallBold else small)
            }
            y += rowH
        }
        y += 14 * u

        // Stats.
        y = section(c, k, y, "THỐNG KÊ")
        val d = Store.today()
        val finished = Store.books.values.filter { it.finishDay >= 0 }.sortedByDescending { it.finishDay }
        val wpm = if (Store.totalSeconds >= 600) Store.totalWords * 60 / Store.totalSeconds else 0
        val stats = listOf(
            "Thời gian đọc" to HomeView.duration(Store.totalSeconds),
            "Hôm nay" to HomeView.duration(Store.daySeconds[d] ?: 0),
            "7 ngày qua" to HomeView.hours(Store.secondsBetween(d - 6, d)),
            "30 ngày qua" to HomeView.hours(Store.secondsBetween(d - 29, d)) + " · ${Realm.van(Store.wordsBetween(d - 29, d))} vạn từ",
            "Chuỗi ngày đọc" to "${Store.streak()} ngày (dài nhất ${Store.bestStreak()})",
            "Sách đã mở" to "${Store.books.values.count { it.lastOpened > 0 }} cuốn",
            "Đọc xong" to "${finished.size} cuốn",
            "Tốc độ" to if (wpm > 0) "$wpm từ/phút" else "đọc thêm để máy học",
            "Pháp bảo" to "${Store.beasts.size} món",
        )
        for ((label, value) in stats) {
            if (c != null) {
                k.text(c, label, m + 6 * u, y, small)
                k.right(c, k.fit(value, w - 2 * m - 24 * u - small.measureText(label), smallBold), w - m - 6 * u, y, smallBold)
            }
            y += sl + 6 * u
        }
        y += 14 * u

        // The last 14 days.
        y = section(c, k, y, "14 NGÀY QUA")
        val chartH = 70 * u
        if (c != null) {
            val days = (13 downTo 0).map { Store.daySeconds[d - it] ?: 0L }
            val peak = maxOf(days.maxOrNull() ?: 0L, 60L)
            val slot = (w - 2 * m) / 14
            for ((i, s) in days.withIndex()) {
                val bx = m + i * slot
                val bh = if (s == 0L) 0f else maxOf(2 * u, s.toFloat() / peak * (chartH - 2 * u))
                c.drawRect(bx, y + chartH - u, bx + slot - 2 * u, y + chartH, k.fill)
                if (bh > 0) c.drawRect(bx + 2 * u, y + chartH - bh, bx + slot - 4 * u, y + chartH, if (i == 13) k.fill else k.gray)
                if (i % 2 == 1) k.centered(c, LocalDate.ofEpochDay(d - 13 + i).dayOfMonth.toString(), bx, slot, y + chartH + 2 * u, small)
            }
        }
        y += chartH + sl + 18 * u

        // Kinh Các: the books read to the end.
        y = section(c, k, y, "KINH CÁC · ${finished.size} BỘ CÔNG PHÁP")
        if (finished.isEmpty()) {
            if (c != null) k.text(c, "Đọc hết một cuốn, sách sẽ được cất vào đây.", m + 6 * u, y, small)
            y += sl
        }
        val tb = k.ui10Bold
        for (b in finished) {
            if (c != null) {
                c.drawRect(m + 6 * u, y + 4 * u, m + 14 * u, y + k.lineH(tb) + sl - 2 * u, k.fill)
                k.text(c, k.fit(b.title, w - 2 * m - 30 * u, tb), m + 24 * u, y, tb)
                val day = LocalDate.ofEpochDay(b.finishDay)
                val line = "Xong ${day.dayOfMonth}/${day.monthValue}/${day.year} · ${Realm.van(b.words.coerceAtLeast(0))} vạn từ · ${HomeView.duration(b.seconds)}"
                k.text(c, k.fit(line, w - 2 * m - 30 * u, small), m + 24 * u, y + k.lineH(tb), small)
            }
            y += k.lineH(tb) + sl + 12 * u
        }
        return y + 24 * u
    }

    private fun section(c: Canvas?, k: Ink, y: Float, title: String): Float {
        val u = k.u
        val m = Ink.MARGIN * u
        val p = k.smallBold
        if (c != null) {
            k.text(c, title, m, y, p)
            c.drawRect(m, y + k.lineH(p), 480 * u - m, y + k.lineH(p) + u, k.fill)
        }
        return y + k.lineH(p) + 8 * u
    }
}
