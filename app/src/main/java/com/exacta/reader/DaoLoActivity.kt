package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/** 2.4 Đạo lộ, in tabs: the way, the twelve Kim Đan, the six Hóa Thần, body and mind, kiêm tu and Song Anh, Lục Giới. */
class DaoLoActivity : TabActivity() {
    override fun title() = "Đạo Lộ"
    override fun right() = if (DaoLo.way >= 0) DaoLo.WAYS[DaoLo.way].name + (if (DaoLo.way2 >= 0) " · kiêm ${DaoLo.WAYS[DaoLo.way2].name}" else "") else "Chưa chọn đường"
    override fun tabs() = listOf("Đạo lộ", "Kim Đan", "Hóa Thần", "Thể · Thần", "Kiêm tu", "Lục Giới")

    private var armed = false
    private var result: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    /** Đạo lộ and Kim Đan open at Trúc Cơ, Hóa Thần and kiêm tu at Nguyên Anh, Lục Giới in Linh giới. */
    override fun opensAt(tab: Int) = when (tab) {
        0 -> if (DaoLo.way >= 0) -1 else 1
        1 -> 1
        2, 4 -> 3
        5 -> 5
        else -> -1
    }

    override fun onTab(tab: Int) {
        armed = false
        result = null
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> if (DaoLo.way < 0) choose(c, k, area, page, hits) else mine(c, k, area)
        1 -> dans(c, k, area, page, hits)
        2 -> thans(c, k, area, page, hits)
        3 -> ladder(c, k, area, page, body = page == 0).let { 2 }
        4 -> (if (page == 0) kiemTu(c, k, area, 0, hits) else songAnh(c, k, area, hits)).let { 2 }
        else -> lucGioi(c, k, area, hits)
    }

    // --- Mười hai Kim Đan, sáu Hóa Thần -------------------------------------------------------------------

    private fun dans(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val formed = Store.realmCap >= 2 && DaoLo.dan >= 0
        val mine = DaoLo.danNow()
        var y = k.section(c, a.left, a.top, a.width(), "MƯỜI HAI KIM ĐAN · " + if (formed) "ĐÃ KẾT ${DaoLo.DANS[mine].name.uppercase()}" else "NẾU NGƯNG ĐAN HÔM NAY: ${DaoLo.DANS[mine].name.uppercase()}")
        y = para(c, k, "Đan kết ra tùy cả đời tu: Sinh Tử trước, rồi Âm Dương, biến dị, cuối cùng ngũ hành theo linh căn và công pháp. " +
            "Nguyên Anh kế thừa đan: tên, tỉ lệ kết anh, và tốc độ của đan tăng thêm một nửa.", a.left, y, a.width(), k.small, 3) + 10 * u
        val items = DaoLo.DANS.mapIndexed { i, d ->
            Item("", d.name, if (i == mine) (if (formed) "đan của bạn" else "sẽ kết") else d.group, listOf("Khi: ${d.how}", d.good.replaceFirstChar { it.uppercase() }), i == mine) {
                AlertDialog.Builder(this).setTitle(d.name).setMessage("${d.group}.\n\nKết khi: ${d.how}.\n\nĐược: ${d.good}.\n\nNguyên Anh thành ${d.prefix} " +
                    (if (DaoLo.way >= 0) DaoLo.WAYS[DaoLo.way].forms[1] else "Nguyên Anh") + ".\n\nBạn đang:\n" + DaoLo.danStatus(i).joinToString("\n") { (t, ok) -> (if (ok) "✓ " else "✗ ") + t } +
                    "\n\nĐan xét theo thứ tự: Sinh Tử, Âm Dương, biến dị, ngũ hành; đủ điều kiện nhóm trước thì kết đan nhóm trước.").setPositiveButton("Đóng", null).show()
            }
        }
        return cards(c, k, a, y, page, items, hits)
    }

    private fun thans(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val formed = Store.realmCap >= 4 && DaoLo.than >= 0
        val mine = DaoLo.thanNow()
        var y = k.section(c, a.left, a.top, a.width(), "SÁU HÓA THẦN · " + if (formed) "ĐÃ THÀNH" else "THEO ĐẠO LỘ")
        y = para(c, k, "Hóa Thần theo đạo lộ chính; ai thành Song Anh thì thành Song Thần. Mỗi loại một lợi một hại, đè lên cửa Hợp Thể và Độ Kiếp về sau.",
            a.left, y, a.width(), k.small, 3) + 10 * u
        val icons = DaoLo.WAYS.map { it.icon } + "daolo"
        val items = DaoLo.THANS.mapIndexed { i, t ->
            Item(icons[i], t.name, if (i == mine) (if (formed) "của bạn" else "đang hướng") else "", listOf("Từ ${t.from}", t.good.replaceFirstChar { it.uppercase() }), i == mine) {
                AlertDialog.Builder(this).setTitle(t.name).setMessage("Thành từ ${t.from}.\n\n${t.good.replaceFirstChar { it.uppercase() }}.").setPositiveButton("Đóng", null).show()
            }
        }
        return cards(c, k, a, y, page, items, hits)
    }

    // --- Lục Giới cảm ứng --------------------------------------------------------------------------------

    private fun lucGioi(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        var y = k.section(c, a.left, a.top, a.width(), "LỤC GIỚI CẢM ỨNG · DO ĐỜI TU QUYẾT ĐỊNH")
        y = para(c, k, "Không ai chọn được giới để phi thăng. Linh căn, công pháp, tâm pháp, đạo lộ, Kim Đan, Hóa Thần, đời tu, tông môn " +
            "và nghiệp lực đều là lời đáp; giới đáp lại mạnh nhất sẽ tiếp dẫn. Chạm một giới để xem cách tính.", a.left, y, a.width(), k.small, 4) + 6 * u
        k.line(c, com.exacta.reader.ui.LucGioi.summary(), a.left, y, a.width(), k.smallBold)
        y += k.lineH(k.smallBold) + 4 * u
        val karma = if (DaoLo.karmaShown()) "Nghiệp lực ${DaoLo.karma()} · ${DaoLo.karmaName()} ›" else "Nghiệp lực: hiện ở Phi Thăng"
        val kr = RectF(a.left, y, a.right, y + k.lineH(k.small))
        k.line(c, karma, a.left, y, a.width(), k.small)
        if (DaoLo.karmaShown()) hits.add(kr) { karmaDialog() }
        y += k.lineH(k.small) + 12 * u
        com.exacta.reader.ui.LucGioi.draw(c, k, RectF(a.left, y, a.right, a.bottom), hits, this)
        return 1
    }

    private fun karmaDialog() {
        val parts = DaoLo.karmaParts()
        AlertDialog.Builder(this).setTitle("Nghiệp lực ${DaoLo.karma()} · ${DaoLo.karmaName()}")
            .setMessage((if (parts.isEmpty()) "Chưa có thiện nghiệp, chưa có ác nghiệp." else parts.joinToString("\n") { "· ${it.first} ${if (it.second >= 0) "+" else "−"}${kotlin.math.abs(it.second)}" }) +
                "\n\nNghiệp lực đè lên Lục Giới: thiện nghiệp nâng Cửu Tiêu Vân Khuyết và Bát Nhã Liên Đài, ác nghiệp kéo về Huyết Nguyệt Trầm Uyên và Vong Xuyên Bỉ Ngạn, " +
                "Hồng Hoang Cổ Địa ưa người quân bình. Cửa Lục Giới Môn cộng nghiệp lực chia mười.")
            .setPositiveButton("Đóng", null).show()
    }

    private fun para(c: Canvas, k: Ink, s: String, x: Float, y: Float, w: Float, p: Paint, max: Int = 4): Float {
        var yy = y
        for (l in k.wrap(s, w, p, max)) {
            k.text(c, l, x, yy, p)
            yy += k.lineH(p)
        }
        return yy
    }

    private fun dayText(d: Long) = LocalDate.ofEpochDay(d).let { "${it.dayOfMonth}/${it.monthValue}/${it.year}" }

    // --- A list of cards, split into pages -----------------------------------------------------------

    private class Item(val icon: String, val title: String, val tag: String, val lines: List<String>, val selected: Boolean, val tap: () -> Unit)

    /** Draws the cards of page `page` from y down; returns the pages. */
    private fun cards(c: Canvas, k: Ink, a: RectF, top: Float, page: Int, items: List<Item>, hits: Hits): Int {
        val u = k.u
        val pad = 14 * u
        val ic = if (items.any { it.icon.isNotEmpty() }) 46 * u else 0f
        val gap = 10 * u
        val small = k.small
        val sl = k.lineH(small)
        val tw = a.width() - 2 * pad - (if (ic > 0) ic + 14 * u else 0f)
        val wrapped = items.map { it -> it.lines.flatMap { l -> k.wrap(l, tw, small, 2) } }
        val heights = wrapped.map { maxOf(ic, k.lineH(k.ui10Bold) + 4 * u + it.size * sl) + 2 * pad }
        // Pages: as many cards as fit, in order.
        val pages = ArrayList<List<Int>>()
        var cur = ArrayList<Int>()
        var y = top
        for (i in items.indices) {
            if (cur.isNotEmpty() && y + heights[i] > a.bottom) {
                pages.add(cur); cur = ArrayList(); y = top
            }
            cur.add(i); y += heights[i] + gap
        }
        if (cur.isNotEmpty()) pages.add(cur)
        y = top
        for (i in pages.getOrElse(page) { emptyList() }) {
            val it = items[i]
            val r = RectF(a.left, y, a.right, y + heights[i])
            k.card(c, r, selected = it.selected)
            if (ic > 0) k.icon(c, it.icon, r.left + pad, r.top + pad, ic)
            val tx = r.left + pad + (if (ic > 0) ic + 14 * u else 0f)
            if (it.tag.isNotEmpty()) k.right(c, it.tag, r.right - pad, r.top + pad + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
            k.line(c, it.title, tx, r.top + pad, tw - (if (it.tag.isNotEmpty()) k.smallBold.measureText(it.tag) + 10 * u else 0f), k.ui10Bold)
            var ty = r.top + pad + k.lineH(k.ui10Bold) + 4 * u
            for (l in wrapped[i]) {
                k.text(c, l, tx, ty, small)
                ty += sl
            }
            hits.add(r, it.tap)
            y = r.bottom + gap
        }
        return pages.size.coerceAtLeast(1)
    }

    // --- Đạo lộ ---------------------------------------------------------------------------------------

    private fun choose(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val best = DaoLo.suggested()
        var y = a.top
        run {
            y = k.section(c, a.left, y, a.width(), if (DaoLo.canChoose()) "CHỌN ĐẠO LỘ · MỘT LẦN CHO CẢ ĐỜI TU" else "NĂM ĐƯỜNG TU · CHỌN Ở TRÚC CƠ")
            val text = if (DaoLo.canChoose()) "Đường tu quyết định cách kết đan, kết anh, hóa thần. Chọn trước khi ngưng đan; đường hợp với cách đọc của bạn có dấu «hợp nhất»."
                else "Lên Trúc Cơ thì chọn một trong năm đường, trước khi ngưng đan. Mỗi đường sống bằng một cách đọc riêng."
            y = para(c, k, text, a.left, y, a.width(), k.small, 3) + 12 * u
        }
        val items = DaoLo.WAYS.mapIndexed { i, w ->
            Item(w.icon, w.name, if (i == best) "hợp nhất" else "", listOf("Sống bằng ${w.lives}", "Mạnh: ${w.strong}", "Yếu: ${w.weak}"), i == best) { wayDialog(i) }
        }
        return cards(c, k, a, y, page, items, hits)
    }

    private fun wayText(i: Int): String {
        val w = DaoLo.WAYS[i]
        return w.text + "\n\nSống bằng ${w.lives}.\nMạnh: ${w.strong}.\nYếu: ${w.weak}.\n" +
            "Ngưng đan: ${w.danDays} ngày, mỗi ngày một buổi liền ${w.danMinutes} phút.\n" +
            "Kim Đan tùy linh căn và cách đọc (mười hai loại), Nguyên Anh thành ${w.forms[1]} mang tên đan, Hóa Thần thành ${w.forms[2]}."
    }

    private fun wayDialog(i: Int) {
        val b = AlertDialog.Builder(this).setTitle(DaoLo.WAYS[i].name).setMessage(wayText(i) + if (DaoLo.canChoose()) "\n\nMột đời tu chỉ chọn một đường." else "")
        if (DaoLo.canChoose()) {
            b.setPositiveButton("Bước vào ${DaoLo.WAYS[i].name}") { _, _ ->
                DaoLo.choose(i)
                refresh()
            }.setNegativeButton("Nghĩ thêm", null)
        } else b.setPositiveButton("Đóng", null)
        b.show()
    }

    private fun mine(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val w = DaoLo.WAYS[DaoLo.way]
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        val ic = 64 * u
        val head = RectF(a.left, a.top, a.right, a.top + ic + 2 * pad)
        k.card(c, head, selected = true)
        k.icon(c, w.icon, head.left + pad, head.top + pad, ic)
        val tx = head.left + 2 * pad + ic
        val tw = head.right - pad - tx
        k.line(c, w.name, tx, head.top + pad, tw, k.ui12Bold)
        val form = DaoLo.form()?.let { "Đang là $it" } ?: "Chưa kết đan"
        k.line(c, form, tx, head.top + pad + k.lineH(k.ui12Bold) + 4 * u, tw, sb)
        k.line(c, "Bước vào ngày ${dayText(DaoLo.wayDay)}", tx, head.top + pad + k.lineH(k.ui12Bold) + 4 * u + sl, tw, small)
        var y = head.bottom + 16 * u

        y = k.section(c, a.left, y, a.width(), "BA LẦN HÓA HÌNH")
        val box = 22 * u
        val forms = listOf(DaoLo.DANS[DaoLo.danNow()].name, DaoLo.anhName(), DaoLo.THANS[DaoLo.thanNow()].name)
        for ((n, label) in listOf("Kết đan", "Kết anh", "Hóa thần").withIndex()) {
            val met = Store.realmCap >= n + 2
            k.check(c, a.left + 4 * u, y + (sl - box) / 2, box, met)
            k.line(c, "$label · ${forms[n]}" + if (met) "" else " (nếu tới hôm nay)", a.left + box + 16 * u, y, a.width() - box - 16 * u, if (met) sb else small)
            y += sl + 8 * u
        }
        y += 8 * u
        for ((title, text) in listOf("SỐNG BẰNG" to w.lives.replaceFirstChar { it.uppercase() }, "MẠNH" to w.strong, "YẾU" to w.weak)) {
            y = k.section(c, a.left, y, a.width(), title)
            y = para(c, k, text, a.left, y, a.width(), small, 2) + 12 * u
        }
        y = k.section(c, a.left, y, a.width(), "ĐƯỜNG NÀY ĐÃ CHO")
        val rows = arrayListOf(
            "Linh lực từ cách đọc riêng" to "${Realm.van(DaoLo.earned)} vạn",
            "Ngưng đan" to "${w.danDays} ngày × buổi ${w.danMinutes} phút",
        )
        if (DaoLo.main(DaoLo.KIEM)) rows.add("Kiếm ý chí mạng" to "${DaoLo.crits} lần")
        for ((p, v) in DaoLo.speedParts()) rows.add("Linh lực: $p" to (if (v >= 0) "+$v%" else "−${-v}%"))
        for ((label, v) in rows) {
            if (y + sl > a.bottom) break
            k.pair(c, label, v, a.left, y, a.width())
            y += sl + 4 * u
        }
        return 1
    }

    // --- Luyện thể, luyện thần --------------------------------------------------------------------------

    private fun ladder(c: Canvas, k: Ink, a: RectF, page: Int, body: Boolean): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val names = if (body) TuLuyen.THE_TIERS else TuLuyen.TRONG
        val at = if (body) TuLuyen.THE_AT else TuLuyen.THAN_THUC_AT
        val v = if (body) TuLuyen.thePhach() else TuLuyen.thanThuc()
        val tier = if (body) TuLuyen.theTier() else TuLuyen.trong()
        val twice = if (body) DaoLo.walks(DaoLo.THE) else DaoLo.walks(DaoLo.THAN)
        var y = a.top
        // The measure, its tier and the way to the next.
        val pad = 18 * u
        val head = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 8 * u + 14 * u + 6 * u + sl)
        k.card(c, head)
        k.line(c, (if (body) "Thể phách $v · " else "Thần thức $v · ") + names[tier], head.left + pad, head.top + pad, head.width() - 2 * pad, k.ui12Bold)
        var hy = head.top + pad + k.lineH(k.ui12Bold) + 8 * u
        val next = tier + 1
        val f = if (next < at.size) (v - at[tier]).toFloat() / (at[next] - at[tier]) else 1f
        k.meter(c, head.left + pad, hy, head.width() - 2 * pad, f, 14 * u)
        hy += 20 * u
        k.line(c, if (next < at.size) "Còn ${at[next] - v} tới ${names[next]}" else "Đã tới bậc cuối", head.left + pad, hy, head.width() - 2 * pad, small)
        y = head.bottom + 14 * u

        val how = if (body) "Mỗi giờ đọc +1, mỗi buổi đọc liền 60 phút +3." + (if (DaoLo.walks(DaoLo.THE)) " Thể tu luyện thể gấp đôi." else "") +
            (if (DaoLo.main(DaoLo.THAN)) " Thần tu thể phách mỏng, chỉ còn 75%." else "") + " Mỗi bậc giúp đỡ lôi kiếp, thiên kiếp."
            else "Mỗi tác giả đọc trọn một cuốn +5, mỗi cuốn đọc trọn +1." + (if (DaoLo.walks(DaoLo.THAN)) " Thần tu luyện thần gấp đôi." else "") +
            " Mỗi trọng giúp phá tiểu cảnh."
        y = para(c, k, how, a.left, y, a.width(), small, 3) + 10 * u
        y = k.section(c, a.left, y, a.width(), if (body) "LUYỆN THỂ · BẢY BẬC" else "LUYỆN THẦN · CHÍN TRỌNG")
        val box = 22 * u
        val rowH = sl + 10 * u
        for (i in names.indices) {
            val met = tier >= i
            k.check(c, a.left + 4 * u, y + (sl - box) / 2, box, met)
            val p = if (i == tier) sb else small
            k.text(c, names[i], a.left + box + 16 * u, y, p)
            val gain = if (body) i * (if (twice) 2 else 1) else i * (if (twice) 2 else 1)
            val what = if (i == 0) "từ 0" else "từ ${at[i]} · " + (if (body) "kiếp +$gain%" else "tiểu cảnh +$gain%")
            k.right(c, what, a.right, y, p)
            y += rowH
        }
        return 1
    }

    // --- Kiêm tu ---------------------------------------------------------------------------------------

    private fun kiemTu(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        var y = a.top
        run {
            y = k.section(c, a.left, y, a.width(), "KIÊM TU · ĐƯỜNG THỨ HAI TỪ NGUYÊN ANH")
            val text = when {
                DaoLo.way < 0 -> "Chọn đạo lộ trước (ở Trúc Cơ). Tới Nguyên Anh thì kiêm tu thêm một đường."
                DaoLo.way2 >= 0 -> "Đang kiêm tu ${DaoLo.WAYS[DaoLo.way2].name} từ ngày ${dayText(DaoLo.way2Day)}: có cái mạnh và cách sống của đường này, không mang cái yếu; linh lực −${DaoLo.KIEM_TU_COST}%."
                Store.realmCap < 3 -> "Tới Nguyên Anh thì kiêm tu được một đường thứ hai: có cái mạnh và cách sống của đường đó, không mang cái yếu, đổi lại linh lực −${DaoLo.KIEM_TU_COST}%."
                else -> "Chọn một đường thứ hai: có cái mạnh và cách sống của đường đó, không mang cái yếu, đổi lại linh lực −${DaoLo.KIEM_TU_COST}%. Chỉ một lần."
            }
            y = para(c, k, text, a.left, y, a.width(), small, 4) + 12 * u
        }
        if (DaoLo.way < 0) return 1
        val list = if (DaoLo.way2 >= 0) listOf(DaoLo.way2) else DaoLo.WAYS.indices.filter { it != DaoLo.way }
        val items = list.map { i ->
            val w = DaoLo.WAYS[i]
            Item(w.icon, w.name, if (i == DaoLo.way2) "đang kiêm" else "", listOf("Sống bằng ${w.lives}", "Mạnh: ${w.strong}"), i == DaoLo.way2) { secondDialog(i) }
        }
        return cards(c, k, a, y, page, items, hits)
    }

    private fun secondDialog(i: Int) {
        val w = DaoLo.WAYS[i]
        val b = AlertDialog.Builder(this).setTitle("Kiêm tu ${w.name}")
            .setMessage("Sống bằng ${w.lives}.\nMạnh: ${w.strong}.\n\nKhông mang cái yếu (${w.weak.lowercase()}), linh lực −${DaoLo.KIEM_TU_COST}%." +
                if (DaoLo.canKiemTu()) "\n\nMột đời tu chỉ kiêm một đường." else if (Store.realmCap < 3) "\n\nMở ở Nguyên Anh." else "")
        if (DaoLo.canKiemTu()) {
            b.setPositiveButton("Kiêm tu ${w.name}") { _, _ ->
                DaoLo.chooseSecond(i)
                refresh()
            }.setNegativeButton("Nghĩ thêm", null)
        } else b.setPositiveButton("Đóng", null)
        b.show()
    }

    // --- Song Anh --------------------------------------------------------------------------------------

    private fun songAnh(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "SONG ANH · CỬA KHÓ NHẤT NHÂN GIỚI")
        y = para(c, k, "Ai kiêm tu cả Thể tu và Thần tu tới Nguyên Anh đại viên mãn được thử kết Nguyên Anh thứ hai. Thành thì linh lực +10%, mọi đột phá về sau +5%; " +
            "khi phi thăng mang thêm một bản mệnh pháp bảo. Hỏng thì tâm ma +10, nghỉ ${DaoLo.SONG_REST_DAYS} ngày, lần sau +10%.", a.left, y, a.width(), small, 5) + 14 * u
        if (DaoLo.songAnh) {
            val r = RectF(a.left, y, a.right, y + 2 * 18 * u + k.lineH(k.ui12Bold) + sl)
            k.card(c, r, selected = true)
            k.line(c, "Song Anh đã thành", r.left + 18 * u, r.top + 18 * u, r.width() - 36 * u, k.ui12Bold)
            k.line(c, "Ngày ${dayText(DaoLo.songDay)} · Thể Anh và Thần Anh cùng ngồi trong đan điền", r.left + 18 * u, r.top + 18 * u + k.lineH(k.ui12Bold), r.width() - 36 * u, small)
            return 1
        }
        y = k.section(c, a.left, y, a.width(), "ĐIỀU KIỆN")
        val box = 22 * u
        for (n in DaoLo.songNeeds()) {
            k.check(c, a.left + 4 * u, y + (sl - box) / 2, box, n.met)
            k.line(c, n.text, a.left + box + 16 * u, y, a.width() - box - 16 * u, if (n.met) small else sb)
            y += sl + 8 * u
        }
        y += 8 * u
        y = k.section(c, a.left, y, a.width(), "TỈ LỆ")
        val parts = listOf("Nền cửa Song Anh" to 20, "Căn cơ ${TuLuyen.canCo()}, chia bốn" to TuLuyen.canCo() / 4,
            "Tâm cảnh ${TuLuyen.TAM_CANH_TIERS[TuLuyen.tamCanhTier()]}" to 5 * TuLuyen.tamCanhTier()) +
            (if (DaoLo.songFails > 0) listOf("Kiên trì, ${DaoLo.songFails} lần trước" to 10 * DaoLo.songFails) else emptyList())
        for ((label, v) in parts) {
            k.pair(c, label, "+$v%", a.left, y, a.width())
            y += sl + 4 * u
        }
        k.pair(c, "Tổng (5 – 80%)", "${DaoLo.songChance()}%", a.left, y, a.width(), sb, sb)
        y += sl + 16 * u

        val info = result ?: DaoLo.lastResult.takeIf { it.isNotEmpty() }
        val bh = 64 * u
        val b = RectF(a.left, a.bottom - bh, a.right, a.bottom)
        if (info != null) {
            val ls = k.wrap(info, a.width(), sb, 3)
            var iy = b.top - 12 * u - ls.size * sl
            for (l in ls) {
                k.text(c, l, a.left, iy, sb)
                iy += sl
            }
        }
        when {
            DaoLo.canSong() -> {
                k.button(c, b, if (armed) "Chạm lần nữa để kết Song Anh" else "Thử kết Song Anh · ${DaoLo.songChance()}%", k.ui10Bold, selected = armed)
                hits.add(b) {
                    if (!armed) armed = true
                    else {
                        armed = false
                        result = when (DaoLo.trySong()) {
                            true -> DaoLo.lastResult
                            false -> DaoLo.lastResult
                            null -> null
                        }
                    }
                    refresh()
                }
            }
            DaoLo.songResting() -> k.button(c, b, "Nghỉ tới ${dayText(DaoLo.songRest)}", sb, enabled = false)
            else -> k.button(c, b, "Chưa đủ điều kiện", sb, enabled = false)
        }
        return 1
    }
}
