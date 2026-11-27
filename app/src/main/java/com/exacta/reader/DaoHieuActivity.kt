package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.Store
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/**
 * Đạo hiệu: the 36 titles, one tab per group of six. Tap an earned title to wear it (Home and Tu vi
 * show it); tap it again to go back to showing the newest.
 */
class DaoHieuActivity : TabActivity() {
    override fun title() = "Đạo Hiệu"
    override fun right() = "${Store.titleCount()}/${DaoHieu.COUNT}"
    override fun tabs() = DaoHieu.GROUP_NAMES.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkTitles()
        super.onCreate(savedInstanceState)
        val shown = Store.shownTitle()
        if (savedInstanceState == null && shown >= 0) selectTab(DaoHieu.group(shown))
        Store.titleUnseen = 0
        Store.touch()
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int {
        val u = k.u
        // The title worn.
        val shown = Store.shownTitle()
        val head = RectF(area.left, area.top, area.right, area.top + 96 * u)
        k.card(c, head, selected = true)
        val seal = 60 * u
        k.seal(c, head.left + 18 * u, head.centerY() - seal / 2, seal, shown >= 0)
        val tx = head.left + 18 * u + seal + 18 * u
        val tw = head.right - 18 * u - tx
        val name = if (shown >= 0) DaoHieu.NAMES[shown] else "Chưa đắc đạo hiệu"
        k.text(c, k.fit(name, tw, k.ui12Bold), tx, head.top + 18 * u, k.ui12Bold)
        val how = when {
            shown < 0 -> "Đọc sách để đắc đạo hiệu đầu tiên."
            Store.titleChosen >= 0 -> "Đang mang đạo hiệu đã chọn. Chạm lại để hiện đạo hiệu mới nhất."
            else -> "Đang hiện đạo hiệu mới nhất. Chạm một đạo hiệu đã đắc để mang."
        }
        var hy = head.top + 18 * u + k.lineH(k.ui12Bold) + 2 * u
        for (l in k.wrap(how, tw, k.small, 2)) {
            k.text(c, l, tx, hy, k.small)
            hy += k.lineH(k.small)
        }

        // The six titles of the group.
        val top = head.bottom + 18 * u
        val cols = if (k.W >= 560) 2 else 1
        val gap = 12 * u
        val rows = ((area.bottom - top + gap) / (110 * u + gap)).toInt().coerceIn(1, 6)
        val per = rows * cols
        val pages = (DaoHieu.PER_GROUP + per - 1) / per
        val cw = (area.width() - (cols - 1) * gap) / cols
        val ch = minOf((area.bottom - top - (rows - 1) * gap) / rows, 170 * u)
        for (i in 0 until per) {
            val n = page * per + i
            if (n >= DaoHieu.PER_GROUP) break
            val t = tab * DaoHieu.PER_GROUP + n
            val x = area.left + (i % cols) * (cw + gap)
            val y = top + (i / cols) * (ch + gap)
            drawTitle(c, k, RectF(x, y, x + cw, y + ch), t, hits)
        }
        return pages
    }

    private fun drawTitle(c: Canvas, k: Ink, r: RectF, t: Int, hits: Hits) {
        val u = k.u
        val earned = Store.hasTitle(t)
        val worn = Store.shownTitle() == t
        k.card(c, r, selected = worn)
        val pad = 16 * u
        val s = minOf(54 * u, r.height() - 2 * pad)
        k.seal(c, r.left + pad, r.centerY() - s / 2, s, earned)
        val tx = r.left + pad + s + 16 * u
        val tw = r.right - pad - tx
        val nameP = if (earned) k.ui10Bold else k.paint(Ink.UI10, bold = true, color = Ink.DARK)
        val small = k.small
        val dark = k.paint(Ink.SMALL, color = Ink.DARK)
        val rule = k.wrap(DaoHieu.RULES[t], tw, if (earned) small else dark, 2)
        val day = Store.titleDays[t]
        val status = when {
            !earned -> "Chưa đắc"
            day != null && day > 0 -> LocalDate.ofEpochDay(day).let { "Đắc ${it.dayOfMonth}/${it.monthValue}/${it.year}" } + if (worn) " · mang" else ""
            else -> if (worn) "Đang mang" else "Đã đắc"
        }
        val names = k.wrap(DaoHieu.NAMES[t], tw, nameP, 2)
        val blockH = names.size * k.lineH(nameP) + 4 * u + rule.size * k.lineH(small) + 4 * u + k.lineH(small)
        var y = r.centerY() - blockH / 2
        for (l in names) {
            k.text(c, l, tx, y, nameP)
            y += k.lineH(nameP)
        }
        y += 4 * u
        for (l in rule) {
            k.text(c, l, tx, y, if (earned) small else dark)
            y += k.lineH(small)
        }
        y += 4 * u
        k.line(c, status, tx, y, tw, if (worn) k.smallBold else dark)
        if (earned) {
            hits.add(r) {
                Store.titleChosen = if (Store.titleChosen == t) -1 else t
                Store.touch()
                refresh()
            }
        }
    }
}
