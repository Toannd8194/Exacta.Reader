package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.BatNha
import com.exacta.reader.data.CuuTieu
import com.exacta.reader.data.HongHoang
import com.exacta.reader.data.QuyNhat
import com.exacta.reader.data.ThuHai
import com.exacta.reader.data.VongXuyen
import com.exacta.reader.data.HuyetNguyet
import com.exacta.reader.data.NhatKy
import com.exacta.reader.data.Store
import com.exacta.reader.data.TienDinh
import com.exacta.reader.data.TienLo
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/**
 * 3.0 The seat of the giới (Cửu Tiêu: Thiên Đình; 3.5 Huyết Nguyệt: Ma Cung; 3.7 Bát Nhã: Thiền Viện), in five tabs: the tiên lộ of nine bậc, the seat's
 * three halls, the giới's own rules, the factions, the tiên kiếp.
 */
class GioiActivity : TabActivity() {
    override fun title() = TienLo.seat()
    override fun right() = TienLo.viName()
    override fun tabs() = listOf("Tiên vị", "Cơ sở", "Luật", "Thế lực", "Kiếp")

    private var greeting = false
    private var ending = false
    private var armed = -1

    override fun onTab(tab: Int) {
        armed = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        greeting = TienLo.opened && !TienLo.welcomeSeen
        ending = TienLo.endDay >= 0 && !TienLo.endSeen
        if (greeting || ending) {
            TienLo.welcomeSeen = true
            if (ending) TienLo.endSeen = true
            Store.touch()
        }
        super.onCreate(savedInstanceState)
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> when (page) {
            0 -> road(c, k, area)
            1 -> carried(c, k, area)
            else -> quyNhat(c, k, area, hits)
        }
        1 -> if (page == 0) seat(c, k, area, hits) else ledgerPage(c, k, area, page - 1)
        2 -> if (page == 0) when (TienLo.gioi) {
            1 -> maRules(c, k, area, hits)
            2 -> thienRules(c, k, area, hits)
            3 -> hoangRules(c, k, area)
            4 -> minhRules(c, k, area)
            5 -> thuRules(c, k, area)
            else -> rules(c, k, area)
        } else posts(c, k, area)
        3 -> factions(c, k, area, hits)
        else -> kiep(c, k, area, hits)
    }

    private fun para(c: Canvas, k: Ink, s: String, x: Float, y: Float, w: Float, p: Paint, max: Int = 4): Float {
        var yy = y
        for (l in k.wrap(s, w, p, max)) {
            k.text(c, l, x, yy, p)
            yy += k.lineH(p)
        }
        return yy
    }

    private fun centeredPara(c: Canvas, k: Ink, s: String, x: Float, y: Float, w: Float, p: Paint, max: Int = 5): Float {
        var yy = y
        for (l in k.wrap(s, w, p, max)) {
            k.centered(c, l, x, w, yy, p)
            yy += k.lineH(p)
        }
        return yy
    }

    private fun date(d: Long) = if (d < 0) "—" else LocalDate.ofEpochDay(d).let { "${it.dayOfMonth}/${it.monthValue}/${it.year}" }
    private fun resName() = NhatKy.RESOURCE[TienLo.gioi.coerceIn(0, 5)]
    private val rl get() = TienLo.rules()

    /** A row: label left, value right, and a meter under it. */
    private fun meterRow(c: Canvas, k: Ink, label: String, value: String, f: Float, x: Float, y: Float, w: Float): Float {
        val u = k.u
        k.pair(c, label, value, x, y, w)
        val my = y + k.lineH(k.small) + 4 * u
        k.meter(c, x, my, w, f.coerceIn(0f, 1f))
        return my + 12 * u + 12 * u
    }

    // --- Tiên lộ ------------------------------------------------------------------------------------

    private fun road(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = a.top
        val pad = 20 * u
        when {
            ending -> {
                val text = "${rl.END_LINE} Đạo hữu ngồi lên ngai ${TienLo.seat()}, đạo hiệu tối thượng «${TienLo.supreme()}». " +
                    "Từ ngày qua Lục Giới Môn tới nay đã đọc ${Realm.van(TienLo.words())} vạn chữ, kết ${TienLo.res()} ${resName()}, " +
                    "vượt ba tiên kiếp. Phần hai của ${TuLuyen.GIOI[TienLo.gioi]} viên mãn; Quy Nhất còn chờ ở phía trước."
                val lines = k.wrap(text, a.width() - 2 * pad, small, 6)
                val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.big) + 8 * u + lines.size * sl)
                k.card(c, r, selected = true)
                k.centered(c, TienLo.supreme(), r.left, r.width(), r.top + pad, k.big)
                var ty = r.top + pad + k.lineH(k.big) + 8 * u
                for (l in lines) { k.centered(c, l, r.left, r.width(), ty, small); ty += sl }
                y = r.bottom + 18 * u
            }
            greeting -> {
                val text = "${rl.GREET} ${TuLuyen.GIOI[TienLo.gioi]} đón đạo hữu vào chín bậc tiên vị: chữ đọc từ ngày qua cửa tính đủ, " +
                    "những ngày ở tiền đình quy ra ${TienLo.retro} ${resName()}" +
                    (if (TienLo.fromLt > 0) ", ${TienLo.fromLt * 100} linh thạch đổi thành ${TienLo.fromLt} tiên thạch" else "") + "."
                val lines = k.wrap(text, a.width() - 2 * pad, small, 5)
                val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + lines.size * sl)
                k.card(c, r, selected = true)
                k.centered(c, "Khai thiên · ${TienLo.seat()}", r.left, r.width(), r.top + pad, k.ui12Bold)
                var ty = r.top + pad + k.lineH(k.ui12Bold) + 6 * u
                for (l in lines) { k.centered(c, l, r.left, r.width(), ty, small); ty += sl }
                y = r.bottom + 18 * u
            }
            else -> {
                val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.big) + 6 * u + sl)
                k.card(c, r, selected = true)
                k.centered(c, TienLo.name(), r.left, r.width(), r.top + pad, k.big)
                k.centered(c, "Bậc ${TienLo.bac}/9 · ${rl.CHUC_LABEL} ${rl.CHUC[TienLo.bac - 1]}", r.left, r.width(), r.top + pad + k.lineH(k.big) + 6 * u, sb)
                y = r.bottom + 16 * u
            }
        }
        y = k.realmBar(c, a.left, y, a.width(), TienLo.bar()) + 18 * u

        // The two conditions of the next bậc.
        if (TienLo.bac < 9) {
            val next = TienLo.bac + 1
            y = k.section(c, a.left, y, a.width(), "TỚI ${TienLo.viName(next).uppercase()}")
            val wNeed = TienLo.floorWords(next)
            y = meterRow(c, k, "Chữ từ ngày qua cửa", "${Realm.van(TienLo.words())} / ${Realm.van(wNeed)} vạn",
                TienLo.words().toFloat() / wNeed, a.left, y, a.width())
            val rNeed = TienLo.BAC_RES[next - 1]
            y = meterRow(c, k, resName().replaceFirstChar { it.uppercase() } + " (hôm nay +${TienLo.todayPoints()} dự tính)", "${TienLo.res()} / $rNeed",
                TienLo.res().toFloat() / rNeed, a.left, y, a.width())
            val ki = TienLo.kiepIndex(next)
            if (ki >= 0 && !TienLo.kiepDone(ki)) y = para(c, k, "Bậc này có ${TienLo.kiepName(ki).lowercase()}: đủ hai điều kiện rồi vào tab Kiếp.", a.left, y, a.width(), sb, 2) + 8 * u
            y += 6 * u
        }

        // The nine bậc.
        y = k.section(c, a.left, y, a.width(), "CHÍN BẬC TIÊN VỊ · CHỮ · ${resName().uppercase()}")
        val rowH = sl + 6 * u
        for (b in 1..9) {
            if (y + sl > a.bottom) break
            val here = b == TienLo.bac
            val p = if (here) sb else small
            val ki = TienLo.kiepIndex(b)
            val tail = if (ki >= 0) " · kiếp" else ""
            k.check(c, a.left, y + 2 * u, sl - 4 * u, b <= TienLo.bac)
            k.pair(c, "$b. ${TienLo.viName(b)}$tail", "${TienLo.BAC_VAN[b - 1]} vạn · ${TienLo.BAC_RES[b - 1]}", a.left + sl + 4 * u, y, a.width() - sl - 4 * u, p, p)
            y += rowH
        }
        return 3
    }

    /** Page 2: what was carried, and what the opening counted. */
    private fun carried(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "HÀNH TRANG · ${resName().uppercase()} ${TienLo.percent()}%")
        val parts = TienLo.parts()
        if (parts.isEmpty()) y = para(c, k, "Chưa có gì cộng thêm: tiên khí từ bản mệnh đã tế luyện, tiên công, tiên căn hợp giới, kiếp ấn, đạo vực.", a.left, y, a.width(), small, 3) + 8 * u
        for ((label, v) in parts) {
            k.pair(c, label, "+$v%", a.left, y, a.width())
            y += sl + 4 * u
        }
        y += 14 * u
        y = k.section(c, a.left, y, a.width(), "NGÀY MỞ GIỚI · ${date(TienLo.openDay)}")
        val rows = listOf(
            "Qua Lục Giới Môn" to date(TienDinh.crossDay),
            "${resName().replaceFirstChar { it.uppercase() }} quy đổi từ tiền đình" to "${TienLo.retro}",
            "Ngày có sổ · ngày tính trung bình" to "${TienLo.retroMeasured} · ${TienLo.retroFilled}",
            "Linh thạch đổi ra tiên thạch" to "${TienLo.fromLt}",
            "Phẩm Thiên Tiên chuyển thành bậc" to if (TienLo.fromPham > 0) "bậc ${TienLo.fromPham}" else "—",
        )
        for ((l, v) in rows) {
            k.pair(c, l, v, a.left, y, a.width())
            y += sl + 4 * u
        }
        y += 14 * u
        y = k.section(c, a.left, y, a.width(), "TIÊN THẠCH · ${TienLo.tienThach}")
        para(c, k, "Mỗi 4 vạn chữ +1, lên bậc +${TienLo.TT_BAC}, vượt tiên kiếp +${TienLo.TT_KIEP}, mỗi việc thế lực +5. Dùng nâng ba gian của ${rl.SEAT_NAME}.",
            a.left, y, a.width(), small, 3)
        return 3
    }

    // --- Cơ sở --------------------------------------------------------------------------------------

    private fun seat(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "${rl.SEAT_NAME.uppercase()} · ${TienLo.tienThach} TIÊN THẠCH")
        val pad = 18 * u
        val gap = 14 * u
        for (h in 0 until 3) {
            val lv = TienLo.halls[h]
            val cost = TienLo.hallCost(h)
            val text = k.wrap(rl.HALL_TEXT[h], a.width() - 2 * pad, small, 3)
            val bh = 52 * u
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + text.size * sl + 12 * u + bh)
            k.card(c, r, selected = lv > 0)
            k.text(c, rl.HALLS[h], r.left + pad, r.top + pad, k.ui12Bold)
            k.right(c, "cấp $lv/5", r.right - pad, r.top + pad, k.ui10Bold)
            var ty = r.top + pad + k.lineH(k.ui12Bold) + 6 * u
            for (l in text) { k.text(c, l, r.left + pad, ty, small); ty += sl }
            val b = RectF(r.left + pad, r.bottom - pad - bh, r.right - pad, r.bottom - pad)
            when {
                cost < 0 -> k.button(c, b, "Đã viên mãn", sb, enabled = false)
                TienLo.tienThach < cost -> k.button(c, b, "Lên cấp ${lv + 1}: cần $cost tiên thạch", sb, enabled = false)
                else -> {
                    k.button(c, b, if (armed == h) "Chạm lần nữa: trả $cost tiên thạch" else "Lên cấp ${lv + 1} · $cost tiên thạch", k.ui10Bold, selected = armed == h)
                    hits.add(b) {
                        if (armed != h) armed = h else { armed = -1; TienLo.raiseHall(h) }
                        refresh()
                    }
                }
            }
            y = r.bottom + gap
        }
        return 1 + ledgerPages(k, a)
    }

    // --- Ledgers: Kinh lâu (Bát Nhã), Sổ sinh tử (Vong Xuyên), Hải đồ (Thư Hải) -----------------------

    private class Row(val title: String, val left: String, val right: String, val struck: Boolean = false)
    private class Ledger(val title: String, val intro: String, val empty: String, val rows: List<Row>)

    private fun ledger(): Ledger? = when (TienLo.gioi) {
        2 -> {
            val list = Store.books.values.filter { it.finishDay >= TienDinh.crossDay && TienDinh.crossDay >= 0 }.sortedByDescending { it.finishDay }
            Ledger("KINH LÂU · ${list.size} BỘ KINH",
                "Mỗi cuốn đọc trọn từ ngày qua cửa thành một bộ kinh trên giá" +
                    (if (TienLo.halls[0] > 0) ", thêm ${4 * TienLo.halls[0]} công đức mỗi bộ." else ". Kinh lâu lên cấp thì mỗi bộ thêm công đức."),
                "Giá kinh còn trống. Đọc trọn một cuốn, bộ kinh đầu tiên sẽ đặt ở đây.",
                list.map { b -> Row("«${b.title}»", b.author.ifBlank { "Khuyết danh" }, "trọn ngày ${date(b.finishDay)}" + if (b.backDay in 0..b.finishDay) " · độ hóa" else "") })
        }
        4 -> {
            val souls = VongXuyen.souls()
            val freed = VongXuyen.freed()
            Ledger("SỔ SINH TỬ · ${souls.size} OAN HỒN · ${freed.size} ĐÃ GẠCH",
                "Mỗi cuốn bỏ dở từ một tháng là một oan hồn trong sổ; đọc tiếp tới hết là siêu độ, tên được gạch.",
                "Sổ trống: thư viện không có cuốn nào bỏ dở, cũng chưa oan hồn nào được siêu độ.",
                souls.map { b -> Row("«${b.title}»", "dừng ở ${b.percent}%", "bỏ từ ${date(b.lastDay)}") } +
                    freed.map { b -> Row("«${b.title}»", "siêu độ", "ngày ${date(b.finishDay)}", struck = true) })
        }
        5 -> {
            val isl = ThuHai.islands()
            Ledger("HẢI ĐỒ · ${isl.size} ĐẢO",
                "Mỗi tác giả đã đọc trọn là một hòn đảo có hải đăng; đảo lớn lên theo số cuốn. Đảo từ 3 cuốn tính đôi khi góp tín niệm.",
                "Biển còn trống. Đọc trọn một cuốn có tên tác giả, hòn đảo đầu tiên sẽ nổi lên.",
                isl.map { i -> Row(i.author, i.genre.ifBlank { "chưa rõ thể loại" }, "${i.books.size} cuốn" + if (i.books.size >= 3) " · đảo lớn" else "") })
        }
        else -> null
    }

    private fun ledgerRows(k: Ink, a: RectF): Int {
        val top = k.section(null, a.left, a.top, a.width(), "X") + 2 * k.lineH(k.small) + 10 * k.u
        return ((a.bottom - top) / (k.lineH(k.small) * 2 + 10 * k.u)).toInt().coerceAtLeast(1)
    }

    private fun ledgerPages(k: Ink, a: RectF): Int {
        val l = ledger() ?: return 0
        return maxOf(1, (l.rows.size + ledgerRows(k, a) - 1) / ledgerRows(k, a))
    }

    private fun ledgerPage(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val l = ledger() ?: return 1
        var y = k.section(c, a.left, a.top, a.width(), l.title)
        y = para(c, k, l.intro, a.left, y, a.width(), small, 2) + 10 * u
        if (l.rows.isEmpty()) {
            para(c, k, l.empty, a.left, y, a.width(), sb, 2)
            return 1 + ledgerPages(k, a)
        }
        val rows = ledgerRows(k, a)
        for (r in l.rows.drop(page * rows).take(rows)) {
            val t = k.fit(r.title, a.width(), if (r.struck) small else sb)
            k.text(c, t, a.left, y, if (r.struck) small else sb)
            if (r.struck) {
                val mid = y + sl * 0.55f
                c.drawRect(a.left, mid, a.left + small.measureText(t), mid + 2 * u, k.fill)
            }
            y += sl
            k.pair(c, k.fit(r.left, a.width() * 0.55f, small), r.right, a.left, y, a.width())
            y += sl + 10 * u
        }
        return 1 + ledgerPages(k, a)
    }

    // --- Luật ---------------------------------------------------------------------------------------

    private fun rules(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "TIÊN LỰC")
        y = para(c, k, "Mỗi 2.500 chữ một điểm, nhân hệ số cân bằng 0,6 – 1,4 của năm thước đo: thước nào tụt xa so với bốn thước kia thì hệ số giảm. Ngày đọc xong mới cộng; hôm nay hiện số dự tính.",
            a.left, y, a.width(), small, 4) + 10 * u
        val m = NhatKy.measures()
        for (i in m.indices) {
            y = meterRow(c, k, CuuTieu.MEASURE[i], "${(m[i] * 100).toInt()}%", m[i].toFloat(), a.left, y, a.width())
        }
        k.pair(c, "Hệ số cân bằng hôm nay", "${NhatKy.balance()}%", a.left, y, a.width(), sb, sb)
        y += sl + 22 * u

        val now = Store.weekStart(Store.today())
        val rule = CuuTieu.ruleOf(now)
        y = k.section(c, a.left, y, a.width(), "THIÊN QUY TUẦN NÀY")
        val (got, goal) = CuuTieu.ruleProgress(now, Store.today())
        val shown = if (rule == 5) "${Realm.van(minOf(got, goal))} / ${Realm.van(goal)} vạn" else "${minOf(got, goal)} / $goal"
        y = meterRow(c, k, CuuTieu.RULES[rule], shown, got.toFloat() / goal, a.left, y, a.width())
        y = para(c, k, "Giữ được: +${CuuTieu.ruleReward()} ${resName()} khi hết tuần. Phạm: không phạt, chỉ mất phần thưởng. Đã giữ ${CuuTieu.kept} tuần, lỡ ${CuuTieu.broken}.",
            a.left, y, a.width(), small, 3)
        if (CuuTieu.lastRule.isNotEmpty()) para(c, k, CuuTieu.lastRule, a.left, y + 6 * u, a.width(), sb, 2)
        return 2
    }

    /** Huyết Nguyệt: ma khí, tâm ma and phản phệ, ma công. */
    private fun maRules(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val hn = HuyetNguyet
        var y = k.section(c, a.left, a.top, a.width(), "MA KHÍ")
        y = para(c, k, "Mỗi 10 phút đọc từ 22 tới 4 giờ một điểm, đêm rằm gấp đôi. Tâm ma từ 40: mỗi ngày thêm một điểm mỗi 10 tâm ma. " +
            "Đột phá hỏng, kiếp hỏng: +20. Ngày đọc xong mới cộng.", a.left, y, a.width(), small, 4) + 10 * u
        y = meterRow(c, k, "Tâm ma · phản phệ ở ${HuyetNguyet.PHAN_PHE_AT}", "${TuLuyen.tamMa}", TuLuyen.tamMa / 100f, a.left, y, a.width())
        y = para(c, k, "Tâm ma vượt ${HuyetNguyet.PHAN_PHE_AT} là phản phệ: mất ma khí ${if (hn.hasChuc(3)) 2 else 3} ngày gần nhất, tâm ma về ${HuyetNguyet.PHAN_PHE_TO}.",
            a.left, y, a.width(), small, 2) + 16 * u
        y = k.section(c, a.left, y, a.width(), "MA CÔNG · ĐẶT CƯỢC LỜI THỀ")
        if (hn.oath >= 0) {
            val need = HuyetNguyet.OATH_NEED[hn.oath]
            val got = hn.oathProgress()
            val shown = if (hn.oath == HuyetNguyet.OATH_WORDS) "${Realm.van(minOf(got, need))} / ${Realm.van(need)} vạn" else "${minOf(got, need)} / $need"
            y = meterRow(c, k, HuyetNguyet.OATHS[hn.oath], shown, got.toFloat() / need, a.left, y, a.width())
            y = para(c, k, "Cược ${hn.stake} ma khí · giữ lời nhận ${hn.payout()} · còn ${hn.oathLeft()} ngày.", a.left, y, a.width(), sb, 2) + 12 * u
        } else if (hn.canSwear()) {
            val cap = hn.stakeCap()
            y = para(c, k, "Cược một phần ma khí vào một lời thề đọc. Giữ lời: nhận gấp đôi; phá: mất phần cược. Mỗi tuần một lời thề; cược tối đa $cap.",
                a.left, y, a.width(), small, 3) + 10 * u
            val bh = 50 * u
            for ((o, text) in HuyetNguyet.OATHS.withIndex()) {
                if (y + bh > a.bottom - 2 * sl) break
                val b = RectF(a.left, y, a.right, y + bh)
                k.button(c, b, text, k.smallBold)
                hits.add(b) { oathDialog(o) }
                y += bh + 8 * u
            }
        } else {
            y = para(c, k, if (TienLo.res() < HuyetNguyet.MIN_STAKE) "Cần ít nhất ${HuyetNguyet.MIN_STAKE} ma khí để cược."
                else "Lời thề kế tiếp từ ngày ${date(hn.nextOathDay())}: mỗi tuần một lời thề.", a.left, y, a.width(), small, 2) + 10 * u
        }
        val tail = "Đã giữ ${hn.wins} lời thề, phá ${hn.losses}; phản phệ ${hn.phanPhe} lần." + if (hn.lastOath.isNotEmpty()) " ${hn.lastOath}." else ""
        val ls = k.wrap(tail, a.width(), sb, 3)
        var iy = a.bottom - ls.size * sl
        if (iy > y) for (l in ls) { k.text(c, l, a.left, iy, sb); iy += sl }
        return 2
    }

    /** Bát Nhã: công đức, xá lợi, độ hóa đồng đạo. */
    private fun thienRules(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val bn = BatNha
        var y = k.section(c, a.left, a.top, a.width(), "CÔNG ĐỨC")
        y = para(c, k, "Ngày đọc đủ 30 phút, không nhảy chương: +2. Mỗi 7 ngày chuỗi: +1. Đọc trọn một cuốn: +20, cuốn bỏ dở được đọc tiếp tới hết: +35. " +
            "Nhảy chương: −2. Ngày tâm ma từ 50 không có công đức. Ngày đọc xong mới cộng.", a.left, y, a.width(), small, 4) + 10 * u
        y = meterRow(c, k, "Tâm ma · từ 50 mất công đức ngày ấy", "${TuLuyen.tamMa}", TuLuyen.tamMa / 100f, a.left, y, a.width())
        val chain = bn.chain()
        y = meterRow(c, k, "Chuỗi ngày đọc · mốc 7 ngày kế", "$chain ngày", (chain % 7) / 7f, a.left, y, a.width()) + 4 * u

        y = k.section(c, a.left, y, a.width(), "XÁ LỢI · ${bn.held()} VIÊN")
        val at = bn.xaAt()
        y = meterRow(c, k, "Viên kế tiếp, mỗi $at công đức", "${at - bn.nextXa()} / $at", (at - bn.nextXa()).toFloat() / at, a.left, y, a.width())
        y = para(c, k, "Xá lợi giữ chuỗi qua một ngày lỡ, hoặc đỡ một lần tiên kiếp hỏng (không thương thế). Đã giữ ${bn.guarded} ngày, đỡ ${bn.warded} kiếp.",
            a.left, y, a.width(), small, 3) + 8 * u
        val bh = 50 * u
        val tb = RectF(a.left, y, a.right, y + bh)
        k.button(c, tb, if (bn.autoGuard) "Xá lợi tự giữ chuỗi: bật" else "Xá lợi tự giữ chuỗi: tắt (chỉ đỡ kiếp)", sb, selected = bn.autoGuard)
        hits.add(tb) {
            bn.autoGuard = !bn.autoGuard
            Store.touch()
            refresh()
        }
        y = tb.bottom + 18 * u

        y = k.section(c, a.left, y, a.width(), "ĐỘ HÓA ĐỒNG ĐẠO · THÁNG ${LocalDate.now().monthValue}")
        val b = bn.book()
        if (b != null) {
            val name = BatNha.FELLOWS[bn.doFellow]
            y = para(c, k, "Đồng đạo $name lạc giữa «${b.title}», tâm ma vây khốn. Đọc tiếp cuốn ấy ${BatNha.DO_DAYS} ngày, mỗi ngày từ 10 phút: +${bn.doReward()} công đức.",
                a.left, y, a.width(), small, 3) + 6 * u
            val p = bn.doProgress()
            y = meterRow(c, k, "Ngày đã đọc cùng đồng đạo", if (bn.doDone) "đã độ hóa" else "${minOf(p, BatNha.DO_DAYS)} / ${BatNha.DO_DAYS}",
                if (bn.doDone) 1f else p.toFloat() / BatNha.DO_DAYS, a.left, y, a.width())
        } else {
            y = para(c, k, "Tháng này chưa đồng đạo nào gặp nạn: thư viện không có cuốn nào bỏ dở từ một tháng trở lên.", a.left, y, a.width(), small, 2) + 8 * u
        }
        val tail = "Đã độ hóa ${bn.freed} đồng đạo." + if (bn.lastXa.isNotEmpty()) " ${bn.lastXa}." else ""
        val ls = k.wrap(tail, a.width(), sb, 2)
        var iy = a.bottom - ls.size * sl
        if (iy > y) for (l in ls) { k.text(c, l, a.left, iy, sb); iy += sl }
        return 2
    }

    /** A small tail at the bottom of a page, if there is room. */
    private fun tail(c: Canvas, k: Ink, a: RectF, y: Float, s: String) {
        val sb = k.smallBold
        val sl = k.lineH(sb)
        val ls = k.wrap(s, a.width(), sb, 2)
        var iy = a.bottom - ls.size * sl
        if (iy > y) for (l in ls) { k.text(c, l, a.left, iy, sb); iy += sl }
    }

    /** Hồng Hoang: khí huyết, huyết mạch, săn hoang thú. */
    private fun hoangRules(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val hh = HongHoang
        var y = k.section(c, a.left, a.top, a.width(), "KHÍ HUYẾT")
        y = para(c, k, "Mỗi 10 phút trong một buổi đọc liền từ 45 phút một điểm; buổi từ 90 phút gấp đôi, từ 120 phút gấp ba. Buổi ngắn không ra khí huyết. Ngày đọc xong mới cộng.",
            a.left, y, a.width(), small, 4) + 10 * u
        val longest = (NhatKy.days[Store.today()]?.sittings?.maxOrNull() ?: 0) / 60
        y = meterRow(c, k, "Buổi liền dài nhất hôm nay", "$longest phút", longest / 120f, a.left, y, a.width()) + 4 * u
        if (hh.mach >= 0) {
            y = k.section(c, a.left, y, a.width(), "HUYẾT MẠCH ${HongHoang.MACH[hh.mach].uppercase()}" + if (hh.machTimes() > 1) " · HÓA HÌNH" else "")
            y = para(c, k, HongHoang.MACH_PERK[hh.mach] + if (hh.machTimes() > 1) ", nhân đôi nhờ hóa hình." else ". Bậc 6 hóa hình: ưu đãi nhân đôi.",
                a.left, y, a.width(), sb, 2) + 16 * u
        } else {
            y = k.section(c, a.left, y, a.width(), "HUYẾT MẠCH · THỨC TỈNH Ở BẬC 2")
            val sc = hh.scores()
            val lean = hh.leaning()
            for (i in HongHoang.MACH.indices) {
                val p = if (i == lean) sb else small
                k.pair(c, "${HongHoang.MACH[i]} · ${HongHoang.MACH_WAY[i]}", "${sc[i]}", a.left, y, a.width(), p, p)
                y += sl + 2 * u
            }
            y += 12 * u
        }
        val week = Store.weekStart(Store.today())
        y = k.section(c, a.left, y, a.width(), "HOANG THÚ TUẦN NÀY · ${HongHoang.beastOf(week).uppercase()}")
        val p = hh.huntProgress()
        y = meterRow(c, k, "Buổi liền từ 45 phút trong tuần", if (hh.killedThisWeek()) "đã hạ" else "${minOf(p, HongHoang.HUNT_SITTINGS)} / ${HongHoang.HUNT_SITTINGS}",
            if (hh.killedThisWeek()) 1f else p.toFloat() / HongHoang.HUNT_SITTINGS, a.left, y, a.width())
        y = para(c, k, "Hạ hoang thú: +${hh.huntReward()} khí huyết, +${HongHoang.HUNT_TT} tiên thạch để nâng Tổ địa.", a.left, y, a.width(), small, 2)
        tail(c, k, a, y, "Đã hạ ${hh.kills} hoang thú." + if (hh.lastHunt.isNotEmpty()) " ${hh.lastHunt}." else "")
        return 2
    }

    /** Vong Xuyên: hồn lực, Vong Xuyên độ, sổ sinh tử. */
    private fun minhRules(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val vx = VongXuyen
        var y = k.section(c, a.left, a.top, a.width(), "HỒN LỰC")
        y = para(c, k, "Mỗi 2.000 chữ đọc lại một điểm: sách đã trọn mở ra đọc lại, sách bỏ dở từ 90 ngày đọc tiếp. Sách mới chỉ được một phần tư. " +
            "Siêu độ một oan hồn +30, mỗi lần chuyển thế +10. Ở minh giới đứt chuỗi không sinh tâm ma.", a.left, y, a.width(), small, 4) + 10 * u
        val again = NhatKy.days[Store.today()]?.let { it.reread + it.back } ?: 0L
        y = meterRow(c, k, "Chữ đọc lại hôm nay", Realm.van(again) + " vạn", again / 20_000f, a.left, y, a.width()) + 4 * u
        y = k.section(c, a.left, y, a.width(), "VONG XUYÊN ĐỘ · THÁNG ${LocalDate.now().monthValue}")
        val b = vx.boat()
        if (b != null) {
            y = para(c, k, "Thuyền tháng này: «${b.title}». Đọc lại cuốn ấy tới hết trong tháng để qua sông: +${vx.boatReward()} hồn lực.", a.left, y, a.width(), small, 3) + 6 * u
            y = meterRow(c, k, "Đọc lại", if (vx.boatDone) "đã qua sông" else "${(b.rereadMax * 100).toInt()}%", if (vx.boatDone) 1f else b.rereadMax.toFloat(), a.left, y, a.width()) + 4 * u
        } else {
            y = para(c, k, "Tháng này chưa có thuyền: cần một cuốn đã đọc trọn từ nửa năm trước.", a.left, y, a.width(), small, 2) + 16 * u
        }
        y = k.section(c, a.left, y, a.width(), "SỔ SINH TỬ")
        k.pair(c, "Oan hồn còn trong sổ (sách bỏ dở)", "${vx.souls().size}", a.left, y, a.width())
        y += sl + 4 * u
        k.pair(c, "Đã siêu độ (đọc tiếp tới hết)", "${vx.freed().size}", a.left, y, a.width())
        y += sl + 4 * u
        y = para(c, k, "Từng tên trong sổ ở tab Cơ sở, trang 2. Mỗi oan hồn siêu độ +${30 + vx.freeReward()} hồn lực.", a.left, y, a.width(), small, 2)
        tail(c, k, a, y, "Đã qua sông ${vx.crossed} lần." + if (vx.lastBoat.isNotEmpty()) " ${vx.lastBoat}." else "")
        return 2
    }

    /** Thư Hải: văn khí, thần quốc, văn mạch. */
    private fun thuRules(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val th = ThuHai
        var y = k.section(c, a.left, a.top, a.width(), "VĂN KHÍ")
        y = para(c, k, "Tác giả mới đọc trọn một cuốn +40; cuốn thứ hai của cùng tác giả +15, thứ ba +8, từ thứ tư +4. Ngày đọc sách của tác giả chưa đọc trọn +1. " +
            "Thể loại hoặc ngôn ngữ mới +25.", a.left, y, a.width(), small, 4) + 14 * u
        val isl = th.islands()
        y = k.section(c, a.left, y, a.width(), "THẦN QUỐC")
        for ((l, v) in listOf(
            "Đảo (tác giả đã đọc trọn)" to "${isl.size}",
            "Đảo lớn (từ 3 cuốn)" to "${isl.count { it.books.size >= 3 }}",
            "Tín niệm mỗi tuần" to if (th.hasChuc(1)) "+${th.tinNiem()} văn khí" else "từ bậc 2",
        )) {
            k.pair(c, l, v, a.left, y, a.width())
            y += sl + 4 * u
        }
        y = para(c, k, "Hải đồ từng đảo ở tab Cơ sở, trang 2.", a.left, y, a.width(), small, 1) + 14 * u
        y = k.section(c, a.left, y, a.width(), "VĂN MẠCH · ${th.vanMach().size}")
        y = para(c, k, "${ThuHai.ARCHIPELAGO} tác giả cùng thể loại thành một quần đảo, mở một văn mạch: +${th.machPct()}% văn khí.", a.left, y, a.width(), small, 2) + 8 * u
        val ga = th.genreAuthors()
        if (ga.isEmpty()) y = para(c, k, "Chưa thể loại nào: thể loại lấy từ thông tin sách EPUB.", a.left, y, a.width(), small, 2)
        for ((g, n) in ga.take(4)) {
            y = meterRow(c, k, g.replaceFirstChar { it.uppercase() }, if (n >= ThuHai.ARCHIPELAGO) "văn mạch" else "$n / ${ThuHai.ARCHIPELAGO}", n.toFloat() / ThuHai.ARCHIPELAGO, a.left, y, a.width())
        }
        tail(c, k, a, y, "Tín niệm đã nhận ${th.tnTotal} văn khí." + if (th.lastTn.isNotEmpty()) " ${th.lastTn}." else "")
        return 2
    }

    // --- Quy Nhất -----------------------------------------------------------------------------------

    private fun quyNhat(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val q = QuyNhat
        var y = a.top
        val pad = 20 * u
        if (q.united()) {
            val text = "Sáu giới vốn là một. Từ ${TuLuyen.GIOI[TienLo.gioi]}, qua ${q.envoys} sứ giả của năm giới kia, đạo hữu nhận đạo hiệu «${QuyNhat.TITLE}». " +
                "Vạn Quyển Tiên Đồ phần hai viên mãn; cửa Hỗn Độn để ngỏ cho phần ba."
            val lines = k.wrap(text, a.width() - 2 * pad, small, 6)
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.big) + 8 * u + lines.size * sl)
            k.card(c, r, selected = true)
            k.centered(c, "Quy Nhất", r.left, r.width(), r.top + pad, k.big)
            var ty = r.top + pad + k.lineH(k.big) + 8 * u
            for (l in lines) { k.centered(c, l, r.left, r.width(), ty, small); ty += sl }
            y = r.bottom + 18 * u
            for ((l, v) in listOf("Ngày Quy Nhất" to date(q.quyDay), "Hỗn nguyên khí" to "${q.hnk}", "Chữ từ ngày qua cửa" to "${Realm.van(TienLo.words())} vạn")) {
                k.pair(c, l, v, a.left, y, a.width())
                y += sl + 4 * u
            }
            return 3
        }
        y = k.section(c, a.left, y, a.width(), "HỖN NGUYÊN KHÍ")
        y = meterRow(c, k, "Mở Quy Nhất ở bậc 9", "${q.hnk} / ${QuyNhat.QUY_AT}", q.hnk.toFloat() / QuyNhat.QUY_AT, a.left, y, a.width())
        y = para(c, k, "Loại tài nguyên chung duy nhất của phần hai, chỉ sứ giả giới khác mang tới. Bậc 9 cùng ${QuyNhat.QUY_AT} hỗn nguyên khí mở Quy Nhất: sáu giới vốn là một.",
            a.left, y, a.width(), small, 3) + 16 * u
        y = k.section(c, a.left, y, a.width(), "SỨ GIẢ THÁNG ${LocalDate.now().monthValue}")
        if (q.near() && q.envoy >= 0) {
            val g = q.envoy
            val got = q.progress()
            val need = QuyNhat.ENVOY_GOAL[g]
            k.text(c, q.envoyName(), a.left, y, sb)
            y += sl + 4 * u
            val shown = if (q.done) "xong" else if (g == QuyNhat.ENVOY_WORDS) "${Realm.van(minOf(got, need))} / ${Realm.van(need)} vạn" else "${minOf(got, need)} / $need"
            y = meterRow(c, k, QuyNhat.ENVOY_TASK[g], shown, if (q.done) 1f else got.toFloat() / need, a.left, y, a.width())
            y = para(c, k, "Xong việc: +${QuyNhat.REWARD} hỗn nguyên khí. Đã tiếp ${q.envoys} sứ giả.", a.left, y, a.width(), small, 2) + 14 * u
        } else {
            y = para(c, k, "Từ bậc ${QuyNhat.FROM_BAC}, mỗi tháng một sứ giả của giới khác ghé ${TienLo.seat()}, giao một việc theo lối đọc của giới họ: xong +${QuyNhat.REWARD} hỗn nguyên khí.",
                a.left, y, a.width(), small, 3) + 14 * u
        }
        y = k.section(c, a.left, y, a.width(), "SỨ GIẢ NĂM GIỚI")
        for (g in 0..5) {
            if (g == TienLo.gioi) continue
            val p = if (g == q.envoy && q.near()) sb else small
            k.pair(c, k.fit(q.envoyName(g), a.width() * 0.42f, sb), k.fit(QuyNhat.ENVOY_TASK[g], a.width() * 0.56f, p), a.left, y, a.width(), sb, p)
            y += sl + 6 * u
        }
        val bh = 60 * u
        val bottom = RectF(a.left, a.bottom - bh, a.right, a.bottom)
        if (q.canUnite()) {
            k.button(c, bottom, if (armed == 30) "Chạm lần nữa: Quy Nhất" else "Quy Nhất", k.ui10Bold, selected = armed == 30)
            hits.add(bottom) {
                if (armed != 30) armed = 30 else { armed = -1; q.unite() }
                refresh()
            }
        } else k.button(c, bottom, "Quy Nhất cần bậc 9 và ${QuyNhat.QUY_AT} hỗn nguyên khí", sb, enabled = false)
        return 3
    }

    private fun oathDialog(o: Int) {
        val hn = HuyetNguyet
        val cap = hn.stakeCap()
        val stakes = listOf(HuyetNguyet.MIN_STAKE, cap / 2, cap).filter { it >= HuyetNguyet.MIN_STAKE }.distinct()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Thề: ${HuyetNguyet.OATHS[o]}")
            .setItems(stakes.map { "Cược $it ma khí · giữ lời nhận ${hn.payoutFor(it)}" }.toTypedArray()) { _, i ->
                if (hn.swear(o, stakes[i])) android.widget.Toast.makeText(this, "Đã thề, cược ${stakes[i]} ma khí", android.widget.Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    /** Page 2: the tiên chức of the nine bậc. */
    private fun posts(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "${rl.CHUC_LABEL.uppercase()} Ở ${TienLo.seat().uppercase()}")
        y = para(c, k, "Lên bậc thì được ${rl.CHUC_LABEL} mới; ưu đãi cộng dồn.", a.left, y, a.width(), small, 2) + 10 * u
        for (i in 0 until 9) {
            val has = rl.hasChuc(i)
            val p = if (i == TienLo.bac - 1) sb else small
            k.check(c, a.left, y + 2 * u, sl - 4 * u, has)
            k.text(c, "${i + 1}. ${rl.CHUC[i]}", a.left + sl + 6 * u, y, p)
            y += sl + 2 * u
            y = para(c, k, rl.CHUC_PERK[i], a.left + sl + 6 * u, y, a.width() - sl - 6 * u, small, 2) + 10 * u
        }
        return 2
    }

    // --- Thế lực ------------------------------------------------------------------------------------

    private fun factions(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = a.top
        val pad = 18 * u
        if (TienLo.faction >= 0) {
            val f = TienLo.faction
            val rank = TienLo.rank()
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + 2 * sl)
            k.card(c, r, selected = true)
            k.text(c, rl.FACTIONS[f], r.left + pad, r.top + pad, k.ui12Bold)
            k.right(c, TienLo.RANKS[rank], r.right - pad, r.top + pad, k.ui10Bold)
            val nextAt = TienLo.RANK_AT.getOrNull(rank + 1)
            val ty = r.top + pad + k.lineH(k.ui12Bold) + 6 * u
            k.text(c, "Cống hiến ${TienLo.congHien}" + (if (nextAt != null) " · ${TienLo.RANKS[rank + 1]} ở $nextAt" else ""), r.left + pad, ty, small)
            k.text(c, k.fit(rl.FACTION_TEXT[f], r.width() - 2 * pad, small), r.left + pad, ty + sl, small)
            y = r.bottom + 18 * u
            val month = LocalDate.now()
            y = k.section(c, a.left, y, a.width(), "VIỆC THÁNG ${month.monthValue}")
            val from = maxOf(TienLo.monthStart(Store.today()), TienLo.openDay)
            for ((i, task) in TienLo.tasks().withIndex()) {
                val got = rl.taskProgress(task, from, Store.today())
                val goal = rl.TASK_GOAL[task]
                val done = TienLo.taskDone(i)
                val shown = if (done) "xong" else if (task == rl.WORD_TASK) "${Realm.van(minOf(got, goal))} / ${Realm.van(goal)} vạn" else "${minOf(got, goal)} / $goal"
                y = meterRow(c, k, rl.TASKS[task], shown, if (done) 1f else got.toFloat() / goal, a.left, y, a.width())
            }
            y = para(c, k, "Mỗi việc xong: +${10 + rl.taskBonus()} cống hiến, +5 tiên thạch. Mỗi bậc chức +1% ${resName()}.", a.left, y, a.width(), small, 2) + 16 * u
            y = k.section(c, a.left, y, a.width(), "ĐỔI THẾ LỰC")
            para(c, k, "Chạm hai lần một thế lực khác ở dưới để đổi; cống hiến về 0.", a.left, y, a.width(), small, 2)
            y += sl + 12 * u
            val bh = 52 * u
            var x = a.left
            for (o in 0 until 3) {
                if (o == f) continue
                val b = RectF(x, y, x + (a.width() - 12 * u) / 2, y + bh)
                k.button(c, b, if (armed == 10 + o) "Chạm lần nữa" else rl.FACTIONS[o], k.ui10Bold, selected = armed == 10 + o)
                hits.add(b) {
                    if (armed != 10 + o) armed = 10 + o else { armed = -1; TienLo.join(o) }
                    refresh()
                }
                x = b.right + 12 * u
            }
            return 1
        }
        y = k.section(c, a.left, y, a.width(), "BA THẾ LỰC")
        y = para(c, k, "Nhận làm khách khanh một thế lực: mỗi tháng ba việc đọc theo lối của giới, lên chức theo cống hiến. Chạm hai lần để nhận.",
            a.left, y, a.width(), small, 3) + 12 * u
        for (f in 0 until 3) {
            val text = k.wrap(rl.FACTION_TEXT[f], a.width() - 2 * pad, small, 2)
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + text.size * sl)
            k.card(c, r, selected = armed == 10 + f)
            k.text(c, rl.FACTIONS[f], r.left + pad, r.top + pad, k.ui12Bold)
            if (armed == 10 + f) k.right(c, "Chạm lần nữa để nhận", r.right - pad, r.top + pad, sb)
            var ty = r.top + pad + k.lineH(k.ui12Bold) + 6 * u
            for (l in text) { k.text(c, l, r.left + pad, ty, small); ty += sl }
            hits.add(r) {
                if (armed != 10 + f) armed = 10 + f else { armed = -1; TienLo.join(f) }
                refresh()
            }
            y = r.bottom + 14 * u
        }
        return 1
    }

    // --- Kiếp ---------------------------------------------------------------------------------------

    private fun kiep(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "BA TIÊN KIẾP")
        val pad = 16 * u
        for (i in 0 until 3) {
            val b = TienLo.KIEP_AT[i]
            val done = TienLo.kiepDone(i)
            val active = TienLo.kiepActive == i
            val rule = k.wrap(rl.KIEP_RULE[i] + ".", a.width() - 2 * pad, small, 3)
            val state = when {
                done -> "Đã vượt"
                active -> "Đang độ: ${TienLo.kiepKept()}/${TienLo.kiepNeed(i)} ${rl.kiepUnit(i)} · còn ${TienLo.kiepLeft()} ngày"
                TienLo.kiepDue() == i -> "Đủ điều kiện · tỉ lệ ${TienLo.kiepChance(i)}%"
                else -> "Giáng khi tới bậc $b" + if (TienLo.kiepFails[i] > 0) " · hỏng ${TienLo.kiepFails[i]}" else ""
            }
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui10Bold) + 4 * u + rule.size * sl + 4 * u + sl)
            k.card(c, r, selected = active || done)
            k.text(c, "Bậc $b · ${TienLo.kiepName(i)}", r.left + pad, r.top + pad, k.ui10Bold)
            var ty = r.top + pad + k.lineH(k.ui10Bold) + 4 * u
            for (l in rule) { k.text(c, l, r.left + pad, ty, small); ty += sl }
            k.text(c, k.fit(state, r.width() - 2 * pad, sb), r.left + pad, ty + 4 * u, sb)
            y = r.bottom + 12 * u
        }
        val due = if (TienLo.kiepActive >= 0) TienLo.kiepActive else TienLo.kiepDue()
        val bh = 60 * u
        val bottom = RectF(a.left, a.bottom - bh, a.right, a.bottom)
        if (due >= 0) {
            y = k.section(c, a.left, y + 4 * u, a.width(), "TỈ LỆ ĐỠ KIẾP")
            for ((label, v) in TienLo.kiepParts(due)) {
                if (y + sl > bottom.top - 3 * sl) break
                k.pair(c, label, "+$v%", a.left, y, a.width())
                y += sl + 4 * u
            }
            k.pair(c, "Tổng (10 – 95%)", "${TienLo.kiepChance(due)}%", a.left, y, a.width(), sb, sb)
        }
        if (TienLo.lastResult.isNotEmpty()) {
            val ls = k.wrap(TienLo.lastResult, a.width(), sb, 2)
            var iy = bottom.top - 10 * u - ls.size * sl
            for (l in ls) { k.text(c, l, a.left, iy, sb); iy += sl }
        }
        when {
            TienLo.kiepActive >= 0 -> k.button(c, bottom, "Đang độ: đọc theo luật kiếp, đủ ngày thì kiếp giáng", sb, enabled = false)
            TienLo.injured() -> k.button(c, bottom, "Thương thế tới ${date(TienLo.injuryUntil)}", sb, enabled = false)
            TienLo.canStartKiep() -> {
                val i = TienLo.kiepDue()
                k.button(c, bottom, if (armed == 20) "Chạm lần nữa để độ ${TienLo.kiepName(i).lowercase()}" else "Độ ${TienLo.kiepName(i)}", k.ui10Bold, selected = armed == 20)
                hits.add(bottom) {
                    if (armed != 20) armed = 20 else { armed = -1; TienLo.startKiep() }
                    refresh()
                }
            }
            TienLo.bac >= 9 -> k.button(c, bottom, "Ba tiên kiếp đã qua", sb, enabled = false)
            else -> k.button(c, bottom, "Kiếp giáng khi đủ chữ và ${resName()} của bậc kế", sb, enabled = false)
        }
        return 1
    }
}
