package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.CuuTieu
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
 * 3.0 The seat of the giới (Cửu Tiêu: Thiên Đình), in five tabs: the tiên lộ of nine bậc, the seat's
 * three halls, the giới's own rules, the factions, the tiên kiếp.
 */
class GioiActivity : TabActivity() {
    override fun title() = TienLo.seat()
    override fun right() = TienLo.viName()
    override fun tabs() = listOf("Tiên lộ", "Cơ sở", "Luật", "Thế lực", "Kiếp")

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
        0 -> if (page == 0) road(c, k, area) else carried(c, k, area)
        1 -> seat(c, k, area, hits)
        2 -> if (page == 0) rules(c, k, area) else posts(c, k, area)
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
                val text = "Chín tầng mây cúi đầu. Đạo hữu ngồi lên ngai ${TienLo.seat()}, đạo hiệu tối thượng «${TienLo.supreme()}». " +
                    "Từ ngày qua Lục Giới Môn tới nay đã đọc ${Realm.van(TienLo.words())} vạn chữ, kết ${TienLo.res()} ${resName()}, " +
                    "vượt ba tiên kiếp. Phần hai của Cửu Tiêu viên mãn; Quy Nhất còn chờ ở phía trước."
                val lines = k.wrap(text, a.width() - 2 * pad, small, 6)
                val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.big) + 8 * u + lines.size * sl)
                k.card(c, r, selected = true)
                k.centered(c, TienLo.supreme(), r.left, r.width(), r.top + pad, k.big)
                var ty = r.top + pad + k.lineH(k.big) + 8 * u
                for (l in lines) { k.centered(c, l, r.left, r.width(), ty, small); ty += sl }
                y = r.bottom + 18 * u
            }
            greeting -> {
                val text = "Mây chín tầng mở cửa khuyết. ${TuLuyen.GIOI[TienLo.gioi]} đón đạo hữu vào tiên lộ: chữ đọc từ ngày qua cửa tính đủ, " +
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
                k.centered(c, "Bậc ${TienLo.bac}/9 · tiên chức ${CuuTieu.CHUC[TienLo.bac - 1]}", r.left, r.width(), r.top + pad + k.lineH(k.big) + 6 * u, sb)
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
        return 2
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
        para(c, k, "Mỗi 4 vạn chữ +1, lên bậc +${TienLo.TT_BAC}, vượt tiên kiếp +${TienLo.TT_KIEP}, mỗi việc thế lực +5. Dùng nâng ba gian của Tiên phủ.",
            a.left, y, a.width(), small, 3)
        return 2
    }

    // --- Cơ sở --------------------------------------------------------------------------------------

    private fun seat(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "TIÊN PHỦ · ${TienLo.tienThach} TIÊN THẠCH")
        val pad = 18 * u
        val gap = 14 * u
        for (h in 0 until 3) {
            val lv = TienLo.halls[h]
            val cost = TienLo.hallCost(h)
            val text = k.wrap(CuuTieu.HALL_TEXT[h], a.width() - 2 * pad, small, 3)
            val bh = 52 * u
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + text.size * sl + 12 * u + bh)
            k.card(c, r, selected = lv > 0)
            k.text(c, CuuTieu.HALLS[h], r.left + pad, r.top + pad, k.ui12Bold)
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
        return 1
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

    /** Page 2: the tiên chức of the nine bậc. */
    private fun posts(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "TIÊN CHỨC Ở THIÊN ĐÌNH")
        y = para(c, k, "Lên bậc thì được phong chức; ưu đãi cộng dồn.", a.left, y, a.width(), small, 2) + 10 * u
        for (i in 0 until 9) {
            val has = CuuTieu.hasChuc(i)
            val p = if (i == TienLo.bac - 1) sb else small
            k.check(c, a.left, y + 2 * u, sl - 4 * u, has)
            k.text(c, "${i + 1}. ${CuuTieu.CHUC[i]}", a.left + sl + 6 * u, y, p)
            y += sl + 2 * u
            y = para(c, k, CuuTieu.CHUC_PERK[i], a.left + sl + 6 * u, y, a.width() - sl - 6 * u, small, 2) + 10 * u
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
            k.text(c, CuuTieu.FACTIONS[f], r.left + pad, r.top + pad, k.ui12Bold)
            k.right(c, TienLo.RANKS[rank], r.right - pad, r.top + pad, k.ui10Bold)
            val nextAt = TienLo.RANK_AT.getOrNull(rank + 1)
            val ty = r.top + pad + k.lineH(k.ui12Bold) + 6 * u
            k.text(c, "Cống hiến ${TienLo.congHien}" + (if (nextAt != null) " · ${TienLo.RANKS[rank + 1]} ở $nextAt" else ""), r.left + pad, ty, small)
            k.text(c, k.fit(CuuTieu.FACTION_TEXT[f], r.width() - 2 * pad, small), r.left + pad, ty + sl, small)
            y = r.bottom + 18 * u
            val month = LocalDate.now()
            y = k.section(c, a.left, y, a.width(), "VIỆC THÁNG ${month.monthValue}")
            val from = maxOf(TienLo.monthStart(Store.today()), TienLo.openDay)
            for ((i, task) in TienLo.tasks().withIndex()) {
                val got = CuuTieu.taskProgress(task, from, Store.today())
                val goal = CuuTieu.TASK_GOAL[task]
                val done = TienLo.taskDone(i)
                val shown = if (done) "xong" else if (task == 4) "${Realm.van(minOf(got, goal))} / ${Realm.van(goal)} vạn" else "${minOf(got, goal)} / $goal"
                y = meterRow(c, k, CuuTieu.TASKS[task], shown, if (done) 1f else got.toFloat() / goal, a.left, y, a.width())
            }
            y = para(c, k, "Mỗi việc xong: +${10 + CuuTieu.taskBonus()} cống hiến, +5 tiên thạch. Mỗi bậc chức +1% ${resName()}.", a.left, y, a.width(), small, 2) + 16 * u
            y = k.section(c, a.left, y, a.width(), "ĐỔI THẾ LỰC")
            para(c, k, "Chạm hai lần một thế lực khác ở dưới để đổi; cống hiến về 0.", a.left, y, a.width(), small, 2)
            y += sl + 12 * u
            val bh = 52 * u
            var x = a.left
            for (o in 0 until 3) {
                if (o == f) continue
                val b = RectF(x, y, x + (a.width() - 12 * u) / 2, y + bh)
                k.button(c, b, if (armed == 10 + o) "Chạm lần nữa" else CuuTieu.FACTIONS[o], k.ui10Bold, selected = armed == 10 + o)
                hits.add(b) {
                    if (armed != 10 + o) armed = 10 + o else { armed = -1; TienLo.join(o) }
                    refresh()
                }
                x = b.right + 12 * u
            }
            return 1
        }
        y = k.section(c, a.left, y, a.width(), "BA THẾ LỰC")
        y = para(c, k, "Nhận làm khách khanh một thế lực: mỗi tháng ba việc đọc theo lối Cửu Tiêu, lên chức theo cống hiến. Chạm hai lần để nhận.",
            a.left, y, a.width(), small, 3) + 12 * u
        for (f in 0 until 3) {
            val text = k.wrap(CuuTieu.FACTION_TEXT[f], a.width() - 2 * pad, small, 2)
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + text.size * sl)
            k.card(c, r, selected = armed == 10 + f)
            k.text(c, CuuTieu.FACTIONS[f], r.left + pad, r.top + pad, k.ui12Bold)
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
            val rule = k.wrap(CuuTieu.KIEP_RULE[i] + ".", a.width() - 2 * pad, small, 3)
            val state = when {
                done -> "Đã vượt"
                active -> "Đang độ: ${TienLo.kiepKept()}/${TienLo.kiepNeed(i)} ngày · còn ${TienLo.kiepLeft()} ngày"
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
