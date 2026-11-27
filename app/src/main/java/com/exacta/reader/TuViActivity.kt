package com.exacta.reader

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.exacta.reader.data.Store
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.PageView
import java.time.LocalDate

/**
 * Tu vi & thành tích: the realm and its ladder, thiên kiếp, bí cảnh tuần, reading stats, the last
 * 14 days, Kinh Các.
 */
class TuViActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.checkRealm()
        val a = this
        setContentView(LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(Widgets.header(a, "Tu Vi", Realm.name(Store.level())))
            addView(Widgets.rule(a), LinearLayout.LayoutParams(-1, Widgets.dp(a, 1).coerceAtLeast(1)))
            addView(ScrollView(a).apply { addView(PageView(a) { v, c, k -> page(v, c, k) }) }, LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun page(v: PageView, c: Canvas?, k: Ink): Float {
        val u = k.u
        val w = k.W * u
        val m = Ink.MARGIN * u
        val words = Store.tuVi()
        val level = Store.level()
        val small = k.small
        val smallBold = k.smallBold
        val sl = k.lineH(small)
        var y = 18 * u

        // The realm, large, with its logo; the title worn under it.
        val name = Realm.name(level)
        val big = k.big
        val icon = 52 * u
        val nw = big.measureText(name)
        val nx = maxOf(m, (w - icon - 12 * u - nw) / 2)
        if (c != null) {
            k.realmIcon(c, Realm.realmOf(level), nx, y + (k.lineH(big) - icon) / 2, icon)
            k.text(c, name, nx + icon + 12 * u, y, big)
        }
        y += k.lineH(big) + 4 * u
        val t = Store.shownTitle()
        if (t >= 0) {
            if (c != null) {
                val r = RectF(m, y, w - m, y + k.lineH(k.ui10Bold))
                k.centered(c, "« ${DaoHieu.NAMES[t]} »", 0f, w, y, k.ui10Bold)
                v.targets.add(r to { startActivity(Intent(this, DaoHieuActivity::class.java)) })
            }
            y += k.lineH(k.ui10Bold) + 2 * u
        }
        val lost = if (Store.tuViLoss > 0) " · đã mất ${Realm.van(Store.tuViLoss)} vạn vì độ kiếp" else ""
        if (c != null) k.centered(c, "Tu vi ${Realm.van(words)} vạn từ$lost", 0f, w, y, small)
        y += sl + 10 * u
        y = if (c != null) {
            k.realmBar(c, m + 20 * u, y, w - 2 * m - 40 * u, Realm.ninths(words, level), Realm.stageLabels(level), Realm.stageIndex(level))
        } else {
            y + 20 * u + sl
        }
        y += 8 * u
        if (level < Realm.LEVELS - 1 && !Store.inTrial()) {
            val next = Realm.floor(level + 1)
            if (c != null) k.centered(c, "Còn ${Realm.van(next - words)} vạn từ tới ${Realm.name(level + 1)}", 0f, w, y, smallBold)
            y += sl
        }
        y += 18 * u

        // Thiên kiếp.
        y = section(c, k, y, "THIÊN KIẾP")
        val trial = trialLines()
        if (c != null) k.bolt(c, m + 6 * u, y, 34 * u)
        var ty = y
        for ((text, bold) in trial) {
            val p = if (bold) smallBold else small
            for (line in k.wrap(text, w - 2 * m - 60 * u, p, 3)) {
                if (c != null) k.text(c, line, m + 54 * u, ty, p)
                ty += sl
            }
            ty += 2 * u
        }
        y = maxOf(ty, y + 40 * u)
        if (Store.inTrial()) {
            val (days, _) = Store.trialDays()
            val need = Store.trialNeed()
            val cw = minOf(48 * u, (w - 2 * m - 54 * u - (need - 1) * 6 * u) / need)
            for (i in 0 until need) {
                if (c != null) {
                    val cell = RectF(m + 54 * u + i * (cw + 6 * u), y, m + 54 * u + i * (cw + 6 * u) + cw, y + 16 * u)
                    if (i < days) c.drawRoundRect(cell, 4 * u, 4 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 4 * u, 4 * u, k.stroke(1.4f))
                }
            }
            y += 26 * u
        }
        y += 14 * u

        // Bí cảnh tuần.
        y = section(c, k, y, "BÍ CẢNH TUẦN")
        val today = Store.today()
        val monday = Store.weekStart(today)
        val names = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dayW = (w - 2 * m - 54 * u - 6 * 6 * u) / 7
        if (c != null) {
            k.gate(c, m + 6 * u, y, 34 * u)
            for (i in 0 until 7) {
                val d = monday + i
                val x = m + 54 * u + i * (dayW + 6 * u)
                val mins = Store.minutes(d)
                val cell = RectF(x, y, x + dayW, y + 30 * u)
                when {
                    mins >= Store.BI_CANH_MINUTES -> c.drawRoundRect(cell, 5 * u, 5 * u, k.fill)
                    d < today -> c.drawRoundRect(cell, 5 * u, 5 * u, k.gray)
                    else -> c.drawRoundRect(k.inset(cell, 0.6f * u), 5 * u, 5 * u, k.stroke(if (d == today) 2.4f else 1.2f))
                }
                val p = if (mins >= Store.BI_CANH_MINUTES) k.paint(Ink.SMALL, bold = true, color = Color.WHITE) else smallBold
                k.centered(c, names[i], x, dayW, y + (30 * u - sl) / 2, p)
                k.centered(c, if (d <= today) "$mins′" else "", x, dayW, y + 32 * u, small)
            }
        }
        y += 32 * u + sl + 6 * u
        val days = Store.biCanhDays()
        val status = if (Store.biCanhDone()) "Tuần này đã qua bí cảnh, nhận khoáng thạch." else
            "Đã có $days/${Store.BI_CANH_DAYS} ngày đọc từ ${Store.BI_CANH_MINUTES} phút; đủ ${Store.BI_CANH_DAYS} ngày nhận khoáng thạch Huyền phẩm trở lên."
        for (line in k.wrap(status, w - 2 * m - 60 * u, small, 3)) {
            if (c != null) k.text(c, line, m + 54 * u, y, small)
            y += sl
        }
        if (c != null) k.text(c, "Đã qua ${Store.biCanhCount} lần", m + 54 * u, y, smallBold)
        y += sl + 18 * u

        // The ladder of ten realms.
        y = section(c, k, y, "CẢNH GIỚI")
        val here = Realm.realmOf(level)
        val rowH = 40 * u
        for (i in 9 downTo 0) {
            if (c != null) {
                val r = RectF(m, y, w - m, y + rowH - 4 * u)
                if (i == here) k.card(c, r, selected = true)
                k.realmIcon(c, i, r.left + 10 * u, r.top + (r.height() - 28 * u) / 2, 28 * u)
                val p = if (i == here) k.ui10Bold else k.ui10
                val label = when {
                    i == here -> name
                    i > Store.realmCap && i <= 9 && Store.realmCap < 9 -> Realm.baseName(i) + "  · chưa độ kiếp"
                    else -> Realm.baseName(i)
                }
                k.text(c, k.fit(label, r.width() - 160 * u, p), r.left + 50 * u, r.top + (r.height() - k.lineH(p)) / 2, p)
                val van = (Realm.floor(i * 3) / 10000).toString()
                k.right(c, "$van vạn", r.right - 12 * u, r.top + (r.height() - sl) / 2, if (i < here) smallBold else small)
            }
            y += rowH
        }
        y += 14 * u

        // Stats, two columns when there is room.
        y = section(c, k, y, "THỐNG KÊ")
        val d = today
        val finished = Store.books.values.filter { it.finishDay >= 0 }.sortedByDescending { it.finishDay }
        val wpm = Store.pace()
        val bestDay = Store.daySeconds.values.maxOrNull() ?: 0L
        val stats = listOf(
            "Thời gian đọc" to HomeView.duration(Store.totalSeconds),
            "Đã đọc" to "${Realm.van(Store.totalWords)} vạn từ",
            "Hôm nay" to HomeView.duration(Store.daySeconds[d] ?: 0),
            "7 ngày qua" to HomeView.hours(Store.secondsBetween(d - 6, d)),
            "30 ngày qua" to HomeView.hours(Store.secondsBetween(d - 29, d)),
            "Chữ 30 ngày" to "${Realm.van(Store.wordsBetween(d - 29, d))} vạn",
            "Chuỗi ngày đọc" to "${Store.streak()} ngày",
            "Chuỗi dài nhất" to "${Store.bestStreak()} ngày",
            "Ngày đọc nhiều nhất" to HomeView.duration(bestDay),
            "Lần đọc dài nhất" to HomeView.duration(Store.bestSession),
            "Sách đã mở" to "${Store.books.values.count { it.lastOpened > 0 }} cuốn",
            "Đọc xong" to "${finished.size} cuốn",
            "Tầng công pháp" to "${Store.tangCount} lần",
            "Tốc độ" to if (wpm > 0) "$wpm từ/phút" else "đọc thêm",
            "Vượt thiên kiếp" to "${Store.trialPassed} lần",
            "Độ kiếp thất bại" to "${Store.failCount} lần",
            "Bí cảnh" to "${Store.biCanhCount} lần",
            "Pháp bảo" to "${Store.beasts.size} món",
        )
        val cols = if (k.W >= 620) 2 else 1
        val colW = (w - 2 * m - (cols - 1) * 24 * u) / cols
        val rows = (stats.size + cols - 1) / cols
        for ((i, pair) in stats.withIndex()) {
            val col = i / rows
            val row = i % rows
            val x = m + col * (colW + 24 * u)
            val yy = y + row * (sl + 6 * u)
            if (c != null) {
                k.text(c, pair.first, x + 6 * u, yy, small)
                k.right(c, k.fit(pair.second, colW - 24 * u - small.measureText(pair.first), smallBold), x + colW - 6 * u, yy, smallBold)
            }
        }
        y += rows * (sl + 6 * u) + 14 * u

        // The last 14 days.
        y = section(c, k, y, "14 NGÀY QUA")
        val chartH = 80 * u
        if (c != null) {
            val daysList = (13 downTo 0).map { Store.daySeconds[d - it] ?: 0L }
            val peak = maxOf(daysList.maxOrNull() ?: 0L, 60L)
            val slot = (w - 2 * m) / 14
            for ((i, s) in daysList.withIndex()) {
                val bx = m + i * slot
                val bh = if (s == 0L) 0f else maxOf(2 * u, s.toFloat() / peak * (chartH - sl - 4 * u))
                c.drawRect(bx, y + chartH - u, bx + slot - 2 * u, y + chartH, k.fill)
                if (bh > 0) c.drawRect(bx + 2 * u, y + chartH - bh, bx + slot - 4 * u, y + chartH, if (i == 13) k.fill else k.gray)
                if (s >= 60 && slot >= 40 * u) k.centered(c, "${s / 60}", bx, slot - 2 * u, y + chartH - bh - sl - 2 * u, small)
                if (i % 2 == 1 || slot >= 40 * u) k.centered(c, LocalDate.ofEpochDay(d - 13 + i).dayOfMonth.toString(), bx, slot, y + chartH + 2 * u, small)
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

    private fun trialLines(): List<Pair<String, Boolean>> {
        val gate = Realm.baseName(Store.nextGate())
        val rule = "${Store.trialNeed()} ngày liền, mỗi ngày đọc từ ${Store.trialMinutes()} phút"
        return when {
            Store.realmCap >= 9 -> listOf("Đã độ kiếp phi thăng." to true, "Tiên nhân không còn thiên kiếp." to false)
            Store.inTrial() -> {
                val (days, _) = Store.trialDays()
                listOf(
                    "Đang độ kiếp vào $gate: $days/${Store.trialNeed()} ngày" to true,
                    rule to false,
                    "Hôm nay: ${Store.minutes(Store.today())}/${Store.trialMinutes()} phút. Lỡ một ngày là thất bại, tu vi lui về gần cửa ải." to false,
                )
            }
            else -> listOf(
                "Kiếp tới: vào $gate, còn ${Realm.van(Store.wordsToGate())} vạn từ" to true,
                "Khi tới cửa ải: $rule." to false,
                "Đã vượt ${Store.trialPassed} lần, thất bại ${Store.failCount} lần." to false,
            )
        }
    }

    private fun section(c: Canvas?, k: Ink, y: Float, title: String): Float {
        val u = k.u
        val m = Ink.MARGIN * u
        val p = k.smallBold
        if (c != null) {
            k.text(c, title, m, y, p)
            c.drawRect(m, y + k.lineH(p), k.W * u - m, y + k.lineH(p) + u, k.fill)
        }
        return y + k.lineH(p) + 10 * u
    }
}
