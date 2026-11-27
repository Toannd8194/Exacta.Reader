package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.exacta.reader.data.Store
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.PageView
import java.time.LocalDate

/**
 * Đạo hiệu: the 36 titles in six groups, those earned with their day. Tap an earned title to wear
 * it (Home and Tu vi show it); tap it again to go back to showing the newest.
 */
class DaoHieuActivity : AppCompatActivity() {
    private lateinit var pageView: PageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.checkTitles()
        Store.titleUnseen = 0
        Store.touch()
        val a = this
        pageView = PageView(a) { v, c, k -> page(v, c, k) }
        setContentView(LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(Widgets.header(a, "Đạo Hiệu", "${Store.titleCount()}/${DaoHieu.COUNT}"))
            addView(Widgets.rule(a), LinearLayout.LayoutParams(-1, Widgets.dp(a, 1).coerceAtLeast(1)))
            addView(ScrollView(a).apply { addView(pageView) }, LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    override fun onPause() {
        Store.save()
        super.onPause()
    }

    private fun page(v: PageView, c: Canvas?, k: Ink): Float {
        val u = k.u
        val w = k.W * u
        val m = Ink.MARGIN * u
        val small = k.small
        val sl = k.lineH(small)
        var y = 18 * u

        // The title worn.
        val shown = Store.shownTitle()
        val big = k.big
        val seal = 56 * u
        val name = if (shown >= 0) DaoHieu.NAMES[shown] else "Chưa đắc đạo hiệu"
        if (c != null) {
            k.seal(c, m, y, seal, shown >= 0)
            k.text(c, k.fit(name, w - 2 * m - seal - 16 * u, big), m + seal + 16 * u, y + (seal - k.lineH(big)) / 2, big)
        }
        y += seal + 10 * u
        val how = when {
            shown < 0 -> "Đọc sách để đắc đạo hiệu đầu tiên."
            Store.titleChosen >= 0 -> "Đang mang đạo hiệu đã chọn. Chạm lại để trở về đạo hiệu mới nhất."
            else -> "Đang hiện đạo hiệu mới nhất. Chạm một đạo hiệu đã đắc để mang."
        }
        for (line in k.wrap(how, w - 2 * m, small, 2)) {
            if (c != null) k.text(c, line, m, y, small)
            y += sl
        }
        y += 16 * u

        val cols = if (k.W >= 620) 2 else 1
        val gap = 10 * u
        val cellW = (w - 2 * m - (cols - 1) * gap) / cols
        val nameP = k.ui10Bold
        val cellH = 14 * u + k.lineH(nameP) + 2 * sl + 10 * u
        for (g in 0 until DaoHieu.GROUPS) {
            val ids = (g * DaoHieu.PER_GROUP until (g + 1) * DaoHieu.PER_GROUP)
            val got = ids.count { Store.hasTitle(it) }
            y = section(c, k, y, "${DaoHieu.GROUP_NAMES[g].uppercase()} · $got/${DaoHieu.PER_GROUP}")
            for ((i, t) in ids.withIndex()) {
                val x = m + (i % cols) * (cellW + gap)
                val top = y + (i / cols) * (cellH + gap)
                if (c != null) drawTitle(v, c, k, RectF(x, top, x + cellW, top + cellH), t)
            }
            y += ((DaoHieu.PER_GROUP + cols - 1) / cols) * (cellH + gap) + 12 * u
        }
        return y + 24 * u
    }

    private fun drawTitle(v: PageView, c: Canvas, k: Ink, r: RectF, t: Int) {
        val u = k.u
        val earned = Store.hasTitle(t)
        val worn = Store.shownTitle() == t
        k.card(c, r, selected = worn)
        val pad = 12 * u
        val s = 34 * u
        k.seal(c, r.left + pad, r.top + (r.height() - s) / 2, s, earned)
        val tx = r.left + pad + s + 12 * u
        val tw = r.right - pad - tx
        var y = r.top + 10 * u
        val nameP = if (earned) k.ui10Bold else k.paint(Ink.UI10, bold = true, color = Ink.DARK)
        k.text(c, k.fit(DaoHieu.NAMES[t], tw, nameP), tx, y, nameP)
        y += k.lineH(nameP) + 2 * u
        val small = k.small
        val dark = k.paint(Ink.SMALL, color = Ink.DARK)
        k.text(c, k.fit(DaoHieu.RULES[t], tw, if (earned) small else dark), tx, y, if (earned) small else dark)
        y += k.lineH(small)
        val day = Store.titleDays[t]
        val status = when {
            !earned -> "Chưa đắc"
            day != null && day > 0 -> LocalDate.ofEpochDay(day).let { "Đắc ngày ${it.dayOfMonth}/${it.monthValue}/${it.year}" } + if (worn) " · đang mang" else ""
            else -> if (worn) "Đang mang" else "Đã đắc"
        }
        k.text(c, k.fit(status, tw, if (worn) k.smallBold else dark), tx, y, if (worn) k.smallBold else dark)
        if (earned) {
            v.targets.add(r to {
                Store.titleChosen = if (Store.titleChosen == t) -1 else t
                Store.touch()
                pageView.invalidate()
            })
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
