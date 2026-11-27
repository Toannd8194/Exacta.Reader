package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.Store
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/** 2.3 Bí cảnh, in tabs: the week's map and its biến cố, the ancient ruins of the month, the notes. */
class BiCanhActivity : TabActivity() {
    override fun title() = "Bí Cảnh"
    override fun right() = "Tuần này ${BiCanh.opened()}/${BiCanh.length()} nút"
    override fun tabs() = listOf("Bản đồ", "Di tích", "Ghi chép")

    private var last: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    override fun onTab(tab: Int) {
        last = null
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> map(c, k, area, hits)
        1 -> ruins(c, k, area, hits)
        else -> notes(c, k, area, page)
    }

    private fun para(c: Canvas, k: Ink, s: String, x: Float, y: Float, w: Float, p: Paint, max: Int = 3): Float {
        var yy = y
        for (l in k.wrap(s, w, p, max)) {
            k.text(c, l, x, yy, p)
            yy += k.lineH(p)
        }
        return yy
    }

    private val SHORT = arrayOf("Yêu thú", "Cấm chế", "Dược viên", "Động phủ", "Tàn hồn", "Đồng đạo", "Điện chủ")

    // --- The map ------------------------------------------------------------------------------------------

    private fun map(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "BÍ CẢNH TUẦN · THƯƠNG ${BiCanh.wounds}/${BiCanh.WOUNDS_MAX}")

        // The nodes along a path.
        val n = BiCanh.length()
        val r = 26 * u
        val step = (a.width() - 2 * r - 20 * u) / (n - 1).coerceAtLeast(1)
        val cy = y + r + 6 * u
        val x0 = a.left + r + 10 * u
        for (i in 0 until n - 1) c.drawRect(x0 + i * step + r, cy - 1.5f * u, x0 + (i + 1) * step - r, cy + 1.5f * u, if (i < BiCanh.resolved) k.fill else k.gray)
        for (i in 0 until n) {
            val x = x0 + i * step
            val type = BiCanh.nodes.getOrNull(i)
            when {
                i < BiCanh.resolved -> { c.drawCircle(x, cy, r, k.fill); c.drawCircle(x, cy, r - 6 * u, k.white); c.drawCircle(x, cy, r - 11 * u, k.fill) }
                i == BiCanh.resolved && (BiCanh.pending() || BiCanh.atFork()) -> { c.drawCircle(x, cy, r, k.white); c.drawCircle(x, cy, r - u, k.stroke(3.5f)) }
                i < BiCanh.opened() -> c.drawCircle(x, cy, r - u, k.stroke(1.8f))
                else -> c.drawCircle(x, cy, r - u, k.faint)
            }
            if (i == n - 1) k.diamond(c, x, cy, 7 * u, if (i < BiCanh.resolved) k.white else k.fill)
            val label = if (type != null) SHORT[type] else if (i == n - 1) "Điện chủ" else "Ngày ${i + 1}"
            k.centered(c, label, x - step / 2, step, cy + r + 6 * u, if (i == BiCanh.resolved) sb else small)
        }
        y = cy + r + 6 * u + sl + 18 * u

        val bw = a.width()
        when {
            BiCanh.fled -> {
                y = para(c, k, "Trọng thương, đã rời bí cảnh tuần này. Tuần sau tu luyện chỉ còn 85%. Thứ Hai tới bí cảnh mới mở.", a.left, y, bw, sb, 3)
            }
            BiCanh.done() -> {
                y = para(c, k, "Đã qua bí cảnh tuần này, nhận khoáng thạch, linh thạch và cơ duyên. Thứ Hai tới bí cảnh mới mở.", a.left, y, bw, sb, 3)
            }
            BiCanh.pending() -> {
                val type = BiCanh.current()
                val pad = 18 * u
                val scene = k.wrap(BiCanh.SCENES[type], bw - 2 * pad, small, 3)
                val card = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + scene.size * sl)
                k.card(c, card, selected = false)
                k.text(c, BiCanh.TYPES[type], a.left + pad, y + pad, k.ui12Bold)
                var ty = y + pad + k.lineH(k.ui12Bold) + 6 * u
                for (l in scene) {
                    k.text(c, l, a.left + pad, ty, small)
                    ty += sl
                }
                y = card.bottom + 14 * u
                val choices = BiCanh.choices(type)
                val ch = 56 * u + sl
                for ((i, choice) in choices.withIndex()) {
                    val b = RectF(a.left, y, a.right, y + 52 * u)
                    k.button(c, b, choice.label, k.ui10Bold, enabled = choice.ok)
                    if (choice.ok) hits.add(b) {
                        last = BiCanh.resolve(i)
                        refresh()
                    }
                    k.centered(c, choice.need, a.left, bw, b.bottom + 3 * u, small)
                    y += ch + 8 * u
                }
            }
            BiCanh.atFork() -> {
                val final = BiCanh.forkL == BiCanh.CHU
                y = para(c, k, if (final) "Đường tới điện chủ đã mở. Bước vào là gặp chủ nhân bí cảnh." else "Ngã rẽ: bên trái và bên phải mỗi bên một biến cố. Chọn đường mà đi.", a.left, y, bw, small, 2) + 12 * u
                if (final) {
                    val b = RectF(a.left, y, a.right, y + 60 * u)
                    k.button(c, b, "Tiến vào điện chủ", k.ui10Bold)
                    hits.add(b) { BiCanh.choose(false); refresh() }
                } else {
                    val half = (bw - 12 * u) / 2
                    val l = RectF(a.left, y, a.left + half, y + 90 * u)
                    val rr = RectF(l.right + 12 * u, y, a.right, y + 90 * u)
                    for ((rect, type, right) in listOf(Triple(l, BiCanh.forkL, false), Triple(rr, BiCanh.forkR, true))) {
                        k.button(c, rect, "", k.small)
                        k.centered(c, if (right) "Rẽ phải ›" else "‹ Rẽ trái", rect.left, rect.width(), rect.top + 14 * u, sb)
                        k.centered(c, BiCanh.TYPES[type], rect.left, rect.width(), rect.top + 18 * u + sl, k.ui10Bold)
                        hits.add(rect) { BiCanh.choose(right); refresh() }
                    }
                }
                y += 100 * u
            }
            else -> {
                y = para(c, k, "Đọc đủ ${Store.BI_CANH_MINUTES} phút hôm nay để mở nút tiếp theo. Mỗi ngày đọc đủ mở thêm một nút; điện chủ ở nút cuối.", a.left, y, bw, small, 3)
            }
        }
        val info = last ?: BiCanh.results.lastOrNull()
        if (info != null) {
            val ls = k.wrap(info, bw, sb, 3)
            var iy = a.bottom - ls.size * sl
            c.drawRect(a.left, iy - 10 * u, a.right, iy - 9 * u, k.gray)
            for (l in ls) {
                k.text(c, l, a.left, iy, sb)
                iy += sl
            }
        }
        return 1
    }

    // --- Thượng cổ di tích ------------------------------------------------------------------------------

    private fun ruins(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val today = LocalDate.now()
        var y = k.section(c, a.left, a.top, a.width(), "THƯỢNG CỔ DI TÍCH · THÁNG ${today.monthValue}")
        y = para(c, k, "Trong một tháng, đủ ${BiCanh.DI_TICH_DAYS} ngày đọc từ 30 phút thì di tích của tháng ấy mở cửa: một ngọc giản, một đan phương, một bảo vật phẩm hiếm và 100 linh thạch.",
            a.left, y, a.width(), small, 3) + 14 * u
        // The month as a grid of days.
        val first = today.withDayOfMonth(1)
        val len = today.lengthOfMonth()
        val cols = 7
        val gap = 8 * u
        val cw = (a.width() - (cols - 1) * gap) / cols
        val ch = 56 * u
        val names = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        for (i in 0 until cols) k.centered(c, names[i], a.left + i * (cw + gap), cw, y, sb)
        y += sl + 6 * u
        val offset = first.dayOfWeek.value - 1
        for (d in 1..len) {
            val idx = offset + d - 1
            val x = a.left + (idx % cols) * (cw + gap)
            val cy = y + (idx / cols) * (ch + gap)
            val cell = RectF(x, cy, x + cw, cy + ch)
            val day = first.plusDays((d - 1).toLong()).toEpochDay()
            val met = Store.minutes(day) >= 30
            when {
                met -> c.drawRoundRect(cell, 8 * u, 8 * u, k.fill)
                day < Store.today() -> c.drawRoundRect(cell, 8 * u, 8 * u, k.gray)
                else -> c.drawRoundRect(k.inset(cell, 0.6f * u), 8 * u, 8 * u, k.stroke(if (day == Store.today()) 2.6f else 1.2f))
            }
            k.centered(c, "$d", x, cw, cell.centerY() - sl / 2, if (met) k.paint(Ink.SMALL, bold = true, color = android.graphics.Color.WHITE) else small)
        }
        val rows = (offset + len + cols - 1) / cols
        y += rows * (ch + gap) + 10 * u
        k.pair(c, "Tháng này", "${BiCanh.monthDays()}/${BiCanh.DI_TICH_DAYS} ngày", a.left, y, a.width(), sb, k.ui10Bold)
        val b = RectF(a.left, a.bottom - 56 * u, a.right, a.bottom)
        val opened = BiCanh.diTichMonth == BiCanh.monthKey()
        k.button(c, b, when {
            opened -> "Di tích tháng này đã mở"
            BiCanh.canOpenDiTich() -> "Mở thượng cổ di tích"
            else -> "Còn ${BiCanh.DI_TICH_DAYS - BiCanh.monthDays()} ngày nữa"
        }, k.ui10Bold, enabled = BiCanh.canOpenDiTich())
        if (BiCanh.canOpenDiTich()) hits.add(b) {
            last = BiCanh.openDiTich()
            android.widget.Toast.makeText(this, last ?: "", android.widget.Toast.LENGTH_LONG).show()
            refresh()
        }
        return 1
    }

    // --- Ghi chép -----------------------------------------------------------------------------------------

    private fun notes(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "GHI CHÉP BÍ CẢNH")
        k.pair(c, "Bí cảnh đã qua", "${Store.biCanhCount} lần", a.left, y, a.width()); y += sl + 6 * u
        k.pair(c, "Chủ nhân bí cảnh đã thắng", "${BiCanh.masters} lần", a.left, y, a.width()); y += sl + 18 * u
        y = k.section(c, a.left, y, a.width(), "TUẦN NÀY")
        if (BiCanh.results.isEmpty()) {
            k.text(c, "Chưa đi nút nào.", a.left, y, small)
            y += sl
        }
        for (r in BiCanh.results) {
            y = para(c, k, "· $r", a.left, y, a.width(), small, 2) + 4 * u
        }
        y += 14 * u
        y = k.section(c, a.left, y, a.width(), "LUẬT")
        val rules = listOf(
            "Mỗi tuần một bản đồ ${BiCanh.length()} nút; mỗi ngày đọc từ ${Store.BI_CANH_MINUTES} phút mở thêm một nút. Bí Cảnh Phù mở thêm một ngày.",
            "Trước mỗi nút chọn rẽ trái hay phải; mỗi nút là một biến cố với vài cách giải. Thuật pháp, pháp bảo, căn cơ, thần thức mở cách giải tốt hơn.",
            "Hai lần bị thương là trọng thương: rời bí cảnh ngay, tuần sau tu luyện 85%.",
            "Qua nút cuối (điện chủ) là qua bí cảnh: khoáng thạch từ Huyền phẩm, linh thạch, kỳ ngộ.",
            "Nút đã mở mà chưa đi, hết tuần tự đi đường vòng an toàn, không thưởng.",
        )
        for (r in rules) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl * 0.5f, 4 * u)
            y = para(c, k, r, a.left + 20 * u, y, a.width() - 20 * u, small, 3) + 6 * u
        }
        return 1
    }
}
