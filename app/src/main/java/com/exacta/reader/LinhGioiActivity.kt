package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/** 2.5 Linh giới, in tabs: the phi thăng, the eight pháp tắc, the đạo vực, the pháp bảo carried and left. */
class LinhGioiActivity : TabActivity() {
    override fun title() = "Linh Giới"
    override fun right() = if (LinhGioi.ascended()) "Đã phi thăng" else "Nhân giới"
    override fun tabs() = listOf("Phi thăng", "Pháp tắc", "Đạo vực", "Di bảo", "Độ Kiếp")

    /** Phi thăng and Di bảo open at Hóa Thần, pháp tắc at Luyện Hư, đạo vực at Hợp Thể. */
    override fun opensAt(tab: Int) = when (tab) {
        1 -> 5
        2 -> 6
        4 -> 7
        else -> 4
    }

    private var armed = false

    override fun onTab(tab: Int) {
        armed = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> phiThang(c, k, area, hits)
        1 -> laws(c, k, area, page, hits)
        2 -> vuc(c, k, area, hits)
        3 -> treasures(c, k, area, page, hits)
        else -> (if (page == 0) doKiep(c, k, area, hits) else seals(c, k, area)).let { 2 }
    }

    // --- Độ Kiếp ----------------------------------------------------------------------------------------

    private fun doKiep(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val d = com.exacta.reader.data.DoKiep
        var y = k.section(c, a.left, a.top, a.width(), "CHÍN TẦNG KIẾP · ĐÃ VƯỢT ${d.passed}/9")
        // Nine cells: passed dark, the one under way ringed.
        val gap = 8 * u
        val cw = (a.width() - 8 * gap) / 9
        for (i in 0 until 9) {
            val r = RectF(a.left + i * (cw + gap), y + 4 * u, a.left + i * (cw + gap) + cw, y + 4 * u + cw * 0.8f)
            when {
                i < d.passed -> c.drawRoundRect(r, 6 * u, 6 * u, k.fill)
                i == d.active -> c.drawRoundRect(k.inset(r, u), 6 * u, 6 * u, k.stroke(3f))
                else -> c.drawRoundRect(k.inset(r, u), 6 * u, 6 * u, k.stroke(1.4f))
            }
            k.centered(c, "${i + 1}", r.left, r.width(), r.centerY() - k.lineH(sb) / 2, if (i < d.passed) k.paint(Ink.SMALL, bold = true, color = android.graphics.Color.WHITE) else sb)
        }
        y += cw * 0.8f + 20 * u
        if (d.passed >= 9) {
            para(c, k, "Độ Kiếp viên mãn: chín tầng đã qua, chín kiếp ấn ghi ở trang sau. Phi Thăng mở đầu bằng thiên nhân ngũ suy (trang sau).", a.left, y, a.width(), small, 3)
            return 2
        }
        val t = if (d.active >= 0) d.active else d.passed
        val tang = d.TANGS[t]
        val pad = 18 * u
        val (_, _, prog) = d.trial()
        val rule = k.wrap(tang.rule.replaceFirstChar { it.uppercase() } + ".", a.width() - 2 * pad, small, 3)
        val card = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + rule.size * sl + (if (d.active >= 0) sl + 6 * u else 0f))
        k.card(c, card, selected = d.active >= 0)
        k.text(c, "Tầng ${t + 1} · ${tang.name}", a.left + pad, y + pad, k.ui12Bold)
        var ty = y + pad + k.lineH(k.ui12Bold) + 6 * u
        for (l in rule) { k.text(c, l, a.left + pad, ty, small); ty += sl }
        if (d.active >= 0) k.text(c, "Đang độ: $prog", a.left + pad, ty + 6 * u, sb)
        y = card.bottom + 14 * u
        y = k.section(c, a.left, y, a.width(), "TỈ LỆ ĐỠ KIẾP")
        val bh = 60 * u
        for ((label, v, _) in d.parts(t)) {
            if (y + sl > a.bottom - bh - 3 * sl) break
            k.pair(c, label, if (v >= 0) "+$v%" else "−${-v}%", a.left, y, a.width())
            y += sl + 4 * u
        }
        k.pair(c, "Tổng (10 – 95%)", "${d.chance(t)}%", a.left, y, a.width(), sb, sb)
        val b = RectF(a.left, a.bottom - bh, a.right, a.bottom)
        if (d.lastResult.isNotEmpty()) {
            val ls = k.wrap(d.lastResult, a.width(), sb, 2)
            var iy = b.top - 10 * u - ls.size * sl
            for (l in ls) { k.text(c, l, a.left, iy, sb); iy += sl }
        }
        when {
            d.active >= 0 -> k.button(c, b, "Đang độ tầng ${t + 1}: đọc theo luật, xong là kiếp giáng", sb, enabled = false)
            d.canStart() -> {
                k.button(c, b, if (armed) "Chạm lần nữa để độ tầng ${t + 1}" else "Độ tầng ${t + 1} · ${tang.name}", k.ui10Bold, selected = armed)
                hits.add(b) {
                    if (!armed) armed = true else { armed = false; d.start() }
                    refresh()
                }
            }
            d.resting() -> k.button(c, b, "Hồi khí tới ${dayText(d.restUntil)}", sb, enabled = false)
            else -> k.button(c, b, "Cần Đại Thừa đại viên mãn và đủ điều kiện cửa Độ Kiếp", sb, enabled = false)
        }
        return 2
    }

    /** Page 2: the kiếp ấn, and the thiên nhân ngũ suy of Phi Thăng. */
    private fun seals(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val d = com.exacta.reader.data.DoKiep
        var y = k.section(c, a.left, a.top, a.width(), "KIẾP ẤN · ${d.an.size}")
        if (d.an.isEmpty()) {
            y = para(c, k, "Mỗi tầng kiếp vượt qua để lại một kiếp ấn, ghi lại nhờ gì mà đỡ được: pháp bảo, nhục thân, pháp tắc, thần thức, tâm cảnh hay kiên trì. " +
                "Kiếp ấn nghiêng Lục Giới về giới hợp với nó.", a.left, y, a.width(), small, 4) + 12 * u
        } else {
            for (an in d.an) {
                k.pair(c, "Tầng ${an.tang + 1} · ${d.TANGS[an.tang].name}", "nhờ ${d.BY[an.by]}" + if (an.fails > 0) " · hỏng ${an.fails}" else "", a.left, y, a.width())
                y += sl + 4 * u
            }
            y += 10 * u
        }
        y = k.section(c, a.left, y, a.width(), "THIÊN NHÂN NGŨ SUY")
        when {
            Store.realmCap < 8 -> para(c, k, "Vào Phi Thăng thì ngũ suy giáng xuống: căn cơ, tâm cảnh giảm ${d.SUY_DROP}; trong 30 ngày phải kéo năm thước đo về ngưỡng, không thì lặp lại tháng sau. Qua rồi Lục Giới Môn mới mở.",
                a.left, y, a.width(), small, 4)
            d.suyPassed -> para(c, k, "Đã qua thiên nhân ngũ suy. Lục Giới Môn đã mở.", a.left, y, a.width(), sb, 2)
            else -> {
                val left = d.suyWindow() + d.SUY_DAYS - Store.today()
                y = para(c, k, "Căn cơ, tâm cảnh đang −${d.SUY_DROP}. Còn $left ngày trong đợt này.", a.left, y, a.width(), sb, 2) + 8 * u
                val box = 22 * u
                for (n in d.suyNeeds()) {
                    k.check(c, a.left + 4 * u, y + (sl - box) / 2, box, n.met)
                    k.line(c, n.text, a.left + box + 16 * u, y, a.width() - box - 16 * u, if (n.met) small else sb)
                    y += sl + 8 * u
                }
            }
        }
        return 2
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

    // --- Phi thăng ------------------------------------------------------------------------------------

    private fun phiThang(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        if (LinhGioi.ascended()) {
            var y = k.section(c, a.left, a.top, a.width(), "LINH GIỚI · LINH KHÍ ĐẶC GẤP BA")
            val days = if (LinhGioi.ascendDay() >= 0) Store.today() - LinhGioi.ascendDay() else 0
            val rows = listOf(
                "Phi thăng" to (if (LinhGioi.ascendDay() >= 0) "ngày ${dayText(LinhGioi.ascendDay())} · $days ngày" else "từ bản cũ"),
                "Bản mệnh" to LinhGioi.banMenh.mapNotNull { LinhGioi.beast(it)?.name }.joinToString(", ").ifEmpty { "không có" },
                "Ôn dưỡng bản mệnh" to "tầng ${LinhGioi.banMenhLevel()}/9 · linh lực +${LinhGioi.banMenhLevel()}%",
                "Mang theo" to "${LinhGioi.carried.size}/${LinhGioi.carrySlots()} ô · thêm một ô mỗi 30 ngày",
                "Bộ bị áp chế" to "${(0 until Treasure.SETS).count { Store.setOwned(it) && LinhGioi.setHeld(it) }}/${Store.setsCount()}",
                "Pháp tắc lĩnh ngộ" to "${LinhGioi.learnedCount()}/8",
                "Đạo vực" to LinhGioi.vucName.ifEmpty { "chưa kết" },
            )
            for ((l, v) in rows) {
                k.pair(c, l, v, a.left, y, a.width())
                y += sl + 8 * u
            }
            y += 10 * u
            para(c, k, "Di bảo Nhân giới vẫn trong bộ sưu tập, nhưng bộ nào không có món nào được mang lên thì bị linh khí Linh giới áp chế, " +
                "mất hiệu quả. Mỗi 30 ngày ở Linh giới tế luyện thêm được một món (tab Di bảo). Bản mệnh lớn một tầng mỗi 20 giờ đọc.",
                a.left, y, a.width(), small, 5)
            return 1
        }
        var y = k.section(c, a.left, a.top, a.width(), "BA BƯỚC PHI THĂNG LINH GIỚI")
        y = para(c, k, "Hóa Thần đại viên mãn thì phi thăng: tìm không gian tiết điểm, trảm trần duyên, rồi vượt phi thăng kiếp.", a.left, y, a.width(), small, 2) + 12 * u
        val run = minOf(LinhGioi.tietRun(), LinhGioi.TIET_NEED)
        val steps = listOf(
            Triple("1 · Không gian tiết điểm", "Qua bí cảnh ${LinhGioi.TIET_NEED} tuần liền; lỡ một tuần là đếm lại. Đang $run/${LinhGioi.TIET_NEED}.", run >= LinhGioi.TIET_NEED),
            Triple("2 · Trảm trần duyên", "Chọn bản mệnh pháp bảo và ba món mang theo ở tab Di bảo. Món để lại thành di bảo Nhân giới.", LinhGioi.trimDone()),
            Triple("3 · Phi thăng kiếp", "${Store.PHI_THANG_DAYS} ngày × ${Store.PHI_THANG_MINUTES} phút, bắt đầu ở Tu vi › Đột phá khi đủ hai bước trên.", false),
        )
        for ((i, s) in steps.withIndex()) {
            val lines = k.wrap(s.second, a.width() - 2 * pad - 30 * u, small, 3)
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui10Bold) + 4 * u + lines.size * sl + (if (i == 0) 28 * u else 0f))
            k.card(c, r, selected = s.third)
            k.check(c, r.left + pad, r.top + pad + 2 * u, 22 * u, s.third)
            val tx = r.left + pad + 30 * u
            k.text(c, s.first, tx, r.top + pad, k.ui10Bold)
            var ty = r.top + pad + k.lineH(k.ui10Bold) + 4 * u
            for (l in lines) {
                k.text(c, l, tx, ty, small)
                ty += sl
            }
            if (i == 0) {
                val bw = 40 * u
                for (j in 0 until LinhGioi.TIET_NEED) {
                    val cell = RectF(tx + j * (bw + 8 * u), ty + 6 * u, tx + j * (bw + 8 * u) + bw, ty + 22 * u)
                    if (j < run) c.drawRoundRect(cell, 4 * u, 4 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 4 * u, 4 * u, k.stroke(1.6f))
                }
            }
            if (i == 1) hits.add(r) { selectTab(3) }
            y = r.bottom + 12 * u
        }
        return 1
    }

    // --- A list of cards, split into pages -----------------------------------------------------------

    private class Item(val title: String, val tag: String, val lines: List<String>, val selected: Boolean, val tap: () -> Unit)

    private fun cards(c: Canvas, k: Ink, a: RectF, top: Float, page: Int, items: List<Item>, hits: Hits): Int {
        val u = k.u
        val pad = 14 * u
        val gap = 10 * u
        val small = k.small
        val sl = k.lineH(small)
        val tw = a.width() - 2 * pad
        val wrapped = items.map { it.lines.flatMap { l -> k.wrap(l, tw, small, 2) } }
        val heights = wrapped.map { k.lineH(k.ui10Bold) + 4 * u + it.size * sl + 2 * pad }
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
            if (it.tag.isNotEmpty()) k.right(c, it.tag, r.right - pad, r.top + pad + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
            k.line(c, it.title, r.left + pad, r.top + pad, tw - (if (it.tag.isNotEmpty()) k.smallBold.measureText(it.tag) + 10 * u else 0f), k.ui10Bold)
            var ty = r.top + pad + k.lineH(k.ui10Bold) + 4 * u
            for (l in wrapped[i]) {
                k.text(c, l, r.left + pad, ty, small)
                ty += sl
            }
            hits.add(r, it.tap)
            y = r.bottom + gap
        }
        return pages.size.coerceAtLeast(1)
    }

    // --- Pháp tắc -------------------------------------------------------------------------------------

    private fun laws(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        var y = k.section(c, a.left, a.top, a.width(), "TÁM PHÁP TẮC · ĐÃ LĨNH NGỘ ${LinhGioi.learnedCount()}/8")
        y = para(c, k, "Mỗi lúc tham ngộ một pháp tắc, qua ba thử thách đọc tính từ ngày bắt đầu. Hợp Thể cần một, Đại Thừa cần ba. Chạm để xem.",
            a.left, y, a.width(), k.small, 3) + 10 * u
        val items = LinhGioi.LAWS.mapIndexed { i, law ->
            val tag = when {
                LinhGioi.learned(i) -> "đã lĩnh ngộ" + if (i in LinhGioi.vuc) " · trong vực" else ""
                LinhGioi.active == i -> "tham ngộ ${LinhGioi.stage[i]}/3"
                LinhGioi.opens(i) -> "hợp"
                else -> "chưa hợp"
            }
            Item(law.name, tag, listOf("Mở khi: ${law.opens}", "Được: ${law.good}"), LinhGioi.learned(i) || LinhGioi.active == i) { lawDialog(i) }
        }
        return cards(c, k, a, y, page, items, hits)
    }

    private fun lawDialog(i: Int) {
        val law = LinhGioi.LAWS[i]
        val steps = law.steps.mapIndexed { s, text ->
            val done = LinhGioi.stage[i] > s
            val now = if (LinhGioi.active == i && LinhGioi.stage[i] == s) LinhGioi.progress(i, s).let { " (${minOf(it.first, it.second)}/${it.second})" } else ""
            (if (done) "✓ " else "○ ") + text + now
        }
        val b = AlertDialog.Builder(this).setTitle(law.name)
            .setMessage("Mở khi: ${law.opens}.\nĐược: ${law.good}" + (if (i in LinhGioi.vuc) ", gấp đôi trong đạo vực" else "") + ".\n\nBa thử thách, tính từ ngày tham ngộ:\n" +
                steps.joinToString("\n") + when {
                    LinhGioi.learned(i) -> "\n\nĐã lĩnh ngộ."
                    !LinhGioi.opens(i) -> "\n\nChưa hợp với linh căn, công pháp, tiên lộ của đạo hữu."
                    LinhGioi.active >= 0 && LinhGioi.active != i -> "\n\nĐang tham ngộ ${LinhGioi.LAWS[LinhGioi.active].name}: đổi sang thì mất tiến độ."
                    else -> ""
                })
        if (LinhGioi.canPonder(i)) {
            b.setPositiveButton("Tham ngộ") { _, _ ->
                LinhGioi.ponder(i)
                refresh()
            }.setNegativeButton("Để sau", null)
        } else b.setPositiveButton("Đóng", null)
        b.show()
    }

    // --- Đạo vực --------------------------------------------------------------------------------------

    private fun vuc(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "ĐẠO VỰC · BA PHÁP TẮC THÀNH MỘT CÕI")
        if (LinhGioi.vuc.isNotEmpty()) {
            val pad = 22 * u
            val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.big) + 8 * u + 4 * sl)
            k.card(c, r, selected = true)
            k.centered(c, LinhGioi.vucName, r.left + pad, r.width() - 2 * pad, r.top + pad, k.shrink(LinhGioi.vucName, r.width() - 2 * pad, k.big, 0.6f))
            var ty = r.top + pad + k.lineH(k.big) + 8 * u
            for (i in LinhGioi.vuc) {
                k.centered(c, "${LinhGioi.LAWS[i].name}: ${LinhGioi.LAWS[i].good} ×2", r.left + pad, r.width() - 2 * pad, ty, small)
                ty += sl
            }
            k.centered(c, "Linh lực +10% · kết ngày ${dayText(LinhGioi.vucDay)}", r.left + pad, r.width() - 2 * pad, ty, sb)
            y = r.bottom + 16 * u
            para(c, k, "Đạo vực là hiệu quả thụ động mạnh nhất: ba pháp tắc trong vực tính gấp đôi. Đại Thừa và Độ Kiếp đều đứng trên nó.", a.left, y, a.width(), small, 3)
            return 1
        }
        y = para(c, k, "Ở Hợp Thể, lĩnh ngộ đủ ba pháp tắc thì kết một đạo vực và tự đặt tên. Ba pháp tắc trong vực tính gấp đôi, linh lực thêm 10%. " +
            "Muốn vào Đại Thừa phải có đạo vực.", a.left, y, a.width(), small, 4) + 14 * u
        val box = 22 * u
        for ((text, met) in listOf("Hợp Thể trở lên" to (Store.realmCap >= 6), "Lĩnh ngộ ba pháp tắc (đã ${LinhGioi.learnedCount()})" to (LinhGioi.learnedCount() >= 3))) {
            k.check(c, a.left + 4 * u, y + (sl - box) / 2, box, met)
            k.text(c, text, a.left + box + 16 * u, y, if (met) small else sb)
            y += sl + 10 * u
        }
        val b = RectF(a.left, a.bottom - 64 * u, a.right, a.bottom)
        if (LinhGioi.canFormVuc()) {
            k.button(c, b, "Kết đạo vực", k.ui10Bold)
            hits.add(b) { pickLaws() }
        } else k.button(c, b, "Chưa đủ điều kiện", sb, enabled = false)
        return 1
    }

    private fun pickLaws() {
        val learned = LinhGioi.learnedList()
        if (learned.size == 3) {
            nameVuc(learned)
            return
        }
        val chosen = BooleanArray(learned.size) { it < 3 }
        AlertDialog.Builder(this).setTitle("Chọn ba pháp tắc")
            .setMultiChoiceItems(learned.map { LinhGioi.LAWS[it].name }.toTypedArray(), chosen) { _, w, on -> chosen[w] = on }
            .setPositiveButton("Tiếp") { _, _ ->
                val laws = learned.filterIndexed { j, _ -> chosen[j] }
                if (laws.size == 3) nameVuc(laws)
                else AlertDialog.Builder(this).setMessage("Đạo vực cần đúng ba pháp tắc.").setPositiveButton("Đóng", null).show()
            }
            .setNegativeButton("Để sau", null).show()
    }

    private fun nameVuc(laws: List<Int>) {
        val field = EditText(this).apply {
            setText(LinhGioi.suggestName(laws))
            setSelectAllOnFocus(true)
        }
        AlertDialog.Builder(this).setTitle("Đặt tên đạo vực")
            .setMessage(laws.joinToString(" · ") { LinhGioi.LAWS[it].name })
            .setView(field)
            .setPositiveButton("Kết vực") { _, _ ->
                LinhGioi.formVuc(laws, field.text.toString())
                refresh()
            }
            .setNegativeButton("Để sau", null).show()
    }

    // --- Di bảo ---------------------------------------------------------------------------------------

    private fun treasures(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        LinhGioi.cleanLost()
        var y = k.section(c, a.left, a.top, a.width(),
            "BẢN MỆNH ${LinhGioi.banMenh.size}/${LinhGioi.banMenhSlots()} · MANG THEO ${LinhGioi.carried.size}/${LinhGioi.carrySlots()}")
        val list = Store.beasts.sortedWith(compareByDescending<com.exacta.reader.data.Beast> { LinhGioi.isBanMenh(it.seed) }
            .thenByDescending { LinhGioi.isCarried(it.seed) }.thenByDescending { it.tier }.thenByDescending { it.bornDay })
        if (list.isEmpty()) {
            para(c, k, "Chưa có pháp bảo nào. Không có gì để trảm, phi thăng nhẹ thân.", a.left, y, a.width(), small, 2)
            return 1
        }
        val keepH = if (LinhGioi.rechoose && LinhGioi.ascended()) 60 * u else 0f
        val rowH = 64 * u
        val per = ((a.bottom - y - keepH) / rowH).toInt().coerceAtLeast(1)
        val pages = (list.size + per - 1) / per
        for (b in list.drop(page * per).take(per)) {
            val r = RectF(a.left, y, a.right, y + rowH - 6 * u)
            val tag = when {
                LinhGioi.isBanMenh(b.seed) -> "Bản mệnh"
                LinhGioi.isCarried(b.seed) -> "Mang theo"
                LinhGioi.ascended() -> "Di bảo"
                else -> ""
            }
            if (tag == "Bản mệnh" || tag == "Mang theo") k.card(c, r, selected = tag == "Bản mệnh") else c.drawRect(r.left, r.bottom, r.right, r.bottom + u, k.gray)
            val ic = r.height() - 10 * u
            k.badge(c, b.species, r.left + 6 * u, r.top + 5 * u, ic)
            val tx = r.left + ic + 18 * u
            val tw = r.right - 12 * u - tx - k.smallBold.measureText("Mang theo") - 12 * u
            k.line(c, b.name, tx, r.top + 6 * u, tw, k.ui10Bold)
            val set = Treasure.setOf(b.species)
            k.line(c, Treasure.TIER_NAMES[b.tier] + (if (set >= 0) " · bộ ${Treasure.SET_NAMES[set]}" else ""), tx, r.top + 6 * u + k.lineH(k.ui10Bold), tw, small)
            if (tag.isNotEmpty()) k.right(c, tag, r.right - 12 * u, r.centerY() - sl / 2, k.smallBold)
            hits.add(r) { beastDialog(b.seed) }
            y += rowH
        }
        if (keepH > 0) {
            val kb = RectF(a.left, a.bottom - keepH + 8 * u, a.right, a.bottom)
            k.button(c, kb, "Giữ lựa chọn này (máy chọn sẵn, đổi được một lần)", k.smallBold)
            hits.add(kb) { LinhGioi.keepChoice(); refresh() }
        }
        return pages
    }

    private fun beastDialog(seed: Long) {
        val b = LinhGioi.beast(seed) ?: return
        val set = Treasure.setOf(b.species)
        val state = when {
            LinhGioi.isBanMenh(seed) -> "Đang là bản mệnh: ôn dưỡng trong đan điền, lớn một tầng mỗi 20 giờ đọc."
            LinhGioi.isCarried(seed) -> "Đang mang theo: bộ của nó còn hiệu quả ở Linh giới."
            LinhGioi.ascended() -> "Di bảo Nhân giới: còn trong bộ sưu tập, nhưng không giữ bộ khỏi bị áp chế."
            else -> "Chưa chọn: lúc phi thăng sẽ thành di bảo Nhân giới."
        }
        val d = AlertDialog.Builder(this).setTitle(b.name)
            .setMessage(Treasure.TIER_NAMES[b.tier] + (if (set >= 0) " · bộ ${Treasure.SET_NAMES[set]}" else "") + "\n\n" + state +
                if (LinhGioi.locked()) "\n\nĐã phi thăng: bản mệnh giữ nguyên, chỉ thêm được món mang theo khi có ô mới." else "")
        if (LinhGioi.canMakeBanMenh(seed)) d.setPositiveButton("Làm bản mệnh") { _, _ -> LinhGioi.makeBanMenh(seed); refresh() }
        if (LinhGioi.canCarry(seed)) d.setNeutralButton(if (LinhGioi.ascended()) "Tế luyện, mang theo" else "Mang theo") { _, _ -> LinhGioi.carry(seed); refresh() }
        if (LinhGioi.canLeave(seed)) d.setNegativeButton("Để lại") { _, _ -> LinhGioi.leave(seed); refresh() }
        else d.setNegativeButton("Đóng", null)
        d.show()
    }
}
