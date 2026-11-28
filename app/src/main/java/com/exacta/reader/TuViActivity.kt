package com.exacta.reader

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/**
 * Tu vi & thành tích, in tabs: the realm and its ladder, the body (the five measures and tâm ma),
 * the breakthrough, the week's bí cảnh, Kinh Các. The stats live in Niên kỷ.
 */
class TuViActivity : TabActivity() {
    override fun title() = "Tu Vi"
    override fun right() = TuLuyen.name()
    override fun tabs() = listOf("Cảnh giới", "Thân", "Đột phá", "Kinh Các")

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    /** The breakthrough asks for a second tap, so a stray touch on the e-ink screen does not roll it. */
    private var armed = false
    private var result: String? = null

    override fun onTab(tab: Int) {
        armed = false
        result = null
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> realm(c, k, area, hits)
        1 -> than(c, k, area, page)
        2 -> if (page == 0) dotPha(c, k, area, hits) else gates(c, k, area)
        else -> kinhCac(c, k, area, page)
    }.let { if (tab == 2) 2 else it }

    private fun realm(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val words = Store.tuVi()
        val level = Store.level()
        val small = k.small
        val sl = k.lineH(small)
        var y = a.top

        // The realm, large, with its logo; the title worn under it.
        val name = TuLuyen.name()
        val big = k.big
        val icon = 72 * u
        val nw = big.measureText(name)
        val nx = a.left + maxOf(0f, (a.width() - icon - 14 * u - nw) / 2)
        k.realmIcon(c, Realm.realmOf(level), nx, y, icon)
        k.text(c, k.fit(name, a.width() - icon - 14 * u, big), nx + icon + 14 * u, y + (icon - k.lineH(big)) / 2, big)
        y += icon + 8 * u
        val t = Store.shownTitle()
        if (t >= 0) {
            val r = RectF(a.left, y, a.right, y + k.lineH(k.ui10Bold))
            k.centered(c, "« ${DaoHieu.NAMES[t]} »", a.left, a.width(), y, k.ui10Bold)
            hits.add(r) { startActivity(Intent(this, DaoHieuActivity::class.java)) }
            y += k.lineH(k.ui10Bold) + 4 * u
        }
        val lost = if (Store.tuViLoss > 0) " · mất ${Realm.van(Store.tuViLoss)} vạn vì độ kiếp" else ""
        k.centered(c, "Tu vi ${Realm.van(words)} vạn từ$lost", a.left, a.width(), y, small)
        y += sl + 12 * u
        y = k.realmBar(c, a.left + 30 * u, y, a.width() - 60 * u, TuLuyen.bar())
        y += 8 * u
        val next = when {
            Store.realmCap < 9 -> TuLuyen.homeLines().joinToString(" · ") { it.first }
            level < Realm.LEVELS - 1 -> "Còn ${Realm.van(Realm.floor(level + 1) - words)} vạn từ tới ${Realm.name(level + 1)}"
            else -> "Đã tới đỉnh tiên lộ"
        }
        val nextR = RectF(a.left, y - 6 * u, a.right, y + sl + 6 * u)
        k.centered(c, next, a.left, a.width(), y, k.smallBold)
        if (Store.realmCap < 9) hits.add(nextR) { selectTab(2) }
        y += sl + 14 * u

        // 2.1: the công pháp running and the linh căn, a tap from here.
        val half = (a.width() - 12 * u) / 2
        val cp = RectF(a.left, y, a.left + half, y + 52 * u)
        val cpText = (DaoPhap.activeDef()?.let { "${it.name} tầng ${DaoPhap.tang()}" } ?: "Công pháp") + " ›"
        k.button(c, cp, k.fit(cpText, cp.width() - 24 * u, k.smallBold), k.smallBold)
        hits.add(cp) { startActivity(Intent(this, CongPhapActivity::class.java)) }
        val dp = RectF(cp.right + 12 * u, y, a.right, y + 52 * u)
        k.button(c, dp, k.fit("Động phủ · linh mạch ${DongPhu.giaiName()} ›", dp.width() - 24 * u, k.smallBold), k.smallBold)
        hits.add(dp) { startActivity(Intent(this, DongPhuActivity::class.java)) }
        y = cp.bottom + 16 * u

        // The ladder of ten realms fills the rest.
        y = k.section(c, a.left, y, a.width(), "CẢNH GIỚI")
        val here = Realm.realmOf(level)
        val rowH = ((a.bottom - y) / 10).coerceIn(34 * u, 64 * u)
        for (i in 9 downTo 0) {
            val r = RectF(a.left, y, a.right, y + rowH - 4 * u)
            if (i == here) k.card(c, r, selected = true)
            val ic = minOf(r.height() - 6 * u, 40 * u)
            k.realmIcon(c, i, r.left + 10 * u, r.top + (r.height() - ic) / 2, ic)
            val p = if (i == here) k.ui10Bold else k.ui10
            val world = when {
                i <= 4 -> "Nhân giới"
                i <= 8 -> "Linh giới"
                else -> "Tiên cảnh"
            }
            val label = when {
                i == here -> name
                i == Store.realmCap + 1 -> Realm.baseName(i) + "  · cửa ải kế tiếp"
                else -> Realm.baseName(i)
            }
            k.text(c, k.fit(label, r.width() - 300 * u, p), r.left + ic + 24 * u, r.top + (r.height() - k.lineH(p)) / 2, p)
            k.right(c, "$world · ${Realm.floor(i * 3) / 10000} vạn", r.right - 12 * u, r.top + (r.height() - sl) / 2, if (i < here) k.smallBold else small)
            y += rowH
        }
        return 1
    }

    // --- Thân: the five measures and what weighs on them -------------------------------------------

    /** One measure: its name and tier, a meter, a value, and two lines on where it comes from. */
    private fun measure(c: Canvas, k: Ink, r: RectF, name: String, tier: String, value: String, f: Float, why: String) {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val pad = 14 * u
        k.card(c, r)
        var y = r.top + pad
        k.text(c, name, r.left + pad, y, k.ui10Bold)
        k.right(c, k.fit(tier, r.width() * 0.55f, k.smallBold), r.right - pad, y + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
        y += k.lineH(k.ui10Bold) + 6 * u
        if (f >= 0f) {
            // f < 0: a row of words only, no meter.
            val vw = 150 * u
            k.meter(c, r.left + pad, y + (sl - 12 * u) / 2, r.width() - 2 * pad - vw - 10 * u, f)
            k.right(c, value, r.right - pad, y, k.smallBold)
            y += sl + 4 * u
        }
        val room = ((r.bottom - pad - y) / sl).toInt().coerceAtLeast(1)
        for (l in k.wrap(why, r.width() - 2 * pad, small, minOf(room, 4))) {
            k.text(c, l, r.left + pad, y, small)
            y += sl
        }
    }

    private fun than(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val today = Store.today()
        if (page == 0) {
            var y = k.section(c, a.left, a.top, a.width(), "NGŨ KHÍ · NĂM THƯỚC ĐO CỦA NGƯỜI TU")
            val gap = 10 * u
            val rowH = (a.bottom - y - 4 * gap) / 5
            fun row(i: Int) = RectF(a.left, y + i * (rowH + gap), a.right, y + i * (rowH + gap) + rowH)

            // Linh lực.
            val bar = TuLuyen.bar()
            val rateWhy = when {
                Store.realmCap >= 9 -> "Tiên nhân: mỗi chữ lật trang đều thành tiên lực."
                TuLuyen.atCeiling() -> "Bình cảnh: chữ đọc chỉ 20% thành linh lực, phần còn lại ép thành áp chế ${apChePercent()}% cảnh giới (+${TuLuyen.apCheBonus()}% đột phá, tối đa 20%)."
                TuLuyen.stuck() -> "Nút thắt: cần căn cơ ${TuLuyen.SUB_NEED[TuLuyen.subCap + 1]} để vào ${TuLuyen.TIEU[TuLuyen.subCap + 1]}; tới lúc đó chỉ 50% chữ thành linh lực."
                else -> "Mỗi chữ lật trang thành linh lực, tốc độ ${TuLuyen.rate() * DaoPhap.speed() / 100}% (linh căn, công pháp)" + (if (TuLuyen.danBonus() > 0) ", kim đan cộng ${TuLuyen.danBonus()}%." else ".")
            } + if (TuLuyen.injured()) " Kinh mạch tổn thương còn ${TuLuyen.injuryUntil - today} ngày: chỉ 70%." else ""
            measure(c, k, row(0), "Linh lực", TuLuyen.name(), "${Realm.van(Store.tuVi())} vạn", (bar.cells / (bar.groups * 3)).toFloat(), rateWhy)

            // Căn cơ.
            val cc = TuLuyen.canCo()
            val days = TuLuyen.daysMet(today - 29, today, 30)
            val books = minOf(4, TuLuyen.finishedSince(today - 89))
            val root = if (TuLuyen.coPham >= 0) " · gốc Trúc Cơ ${TuLuyen.TRUC_CO_PHAM[TuLuyen.coPham].lowercase()} +${TuLuyen.baseCanCo()}" else ""
            val ccTier = TuLuyen.SUB_NEED.indexOfLast { cc >= it }.let { listOf("nền lỏng", "vào được trung kỳ", "vào được hậu kỳ", "vào được đại viên mãn")[it] }
            measure(c, k, row(1), "Căn cơ", ccTier, "$cc / 100", cc / 100f,
                "$days/30 ngày đọc từ 30 phút (×2) · $books cuốn đọc trọn trong 90 ngày (×10)$root" +
                    (if (TuLuyen.jumps30() > 5) " · nhảy chương ${TuLuyen.jumps30()} lần" else "") +
                    if (Store.today() - TuLuyen.tauHoaDay < 30 && TuLuyen.tauHoaDay >= 0) " · tẩu hỏa −10" else "")

            // Tâm cảnh.
            val tc = TuLuyen.tamCanh()
            val tier = TuLuyen.tamCanhTier(tc)
            val nextTc = if (tier < 4) " · tới ${TuLuyen.TAM_CANH_TIERS[tier + 1]} cần ${TuLuyen.TAM_CANH_AT[tier + 1]}" else ""
            measure(c, k, row(2), "Tâm cảnh", TuLuyen.TAM_CANH_TIERS[tier], "$tc / 100", tc / 100f,
                "Chuỗi ${Store.streak()} ngày · ${TuLuyen.long30()} buổi từ 45 phút · nhảy chương ${TuLuyen.jumps30()} lần (30 ngày)$nextTc")

            // Thần thức.
            val tt = TuLuyen.thanThuc()
            val tr = TuLuyen.trong(tt)
            val ttF = if (tr >= 8) 1f else (tt - TuLuyen.THAN_THUC_AT[tr]).toFloat() / (TuLuyen.THAN_THUC_AT[tr + 1] - TuLuyen.THAN_THUC_AT[tr])
            val finished = Store.books.values.count { it.finishDay >= 0 }
            measure(c, k, row(3), "Thần thức", TuLuyen.TRONG[tr], "$tt điểm", ttF,
                "${TuLuyen.authors()} tác giả (×5) · $finished cuốn đọc trọn (×1)" + if (tr < 8) " · tới ${TuLuyen.TRONG[tr + 1]} cần ${TuLuyen.THAN_THUC_AT[tr + 1]}" else "")

            // Thể phách.
            val tp = TuLuyen.thePhach()
            val tt2 = TuLuyen.theTier(tp)
            val tpF = if (tt2 >= TuLuyen.THE_AT.size - 1) 1f else (tp - TuLuyen.THE_AT[tt2]).toFloat() / (TuLuyen.THE_AT[tt2 + 1] - TuLuyen.THE_AT[tt2])
            measure(c, k, row(4), "Thể phách", TuLuyen.THE_TIERS[tt2], "$tp điểm", tpF,
                "${Store.totalSeconds / 3600} giờ đọc (×1) · ${TuLuyen.dayVeryLong.values.sum()} buổi liền từ 60 phút (×3)" +
                    if (tt2 < TuLuyen.THE_AT.size - 1) " · tới ${TuLuyen.THE_TIERS[tt2 + 1]} cần ${TuLuyen.THE_AT[tt2 + 1]}" else "")
            return 2
        }

        // Page 2: tâm ma, wounds, and the foundation laid at Trúc Cơ and Kim Đan.
        var y = k.section(c, a.left, a.top, a.width(), "ÁP LỰC, THỌ NGUYÊN VÀ ĐẠO CƠ")
        val gap = 10 * u
        val rowH = (a.bottom - y - 4 * gap) / 5
        fun row(i: Int) = RectF(a.left, y + i * (rowH + gap), a.right, y + i * (rowH + gap) + rowH)
        val tm = TuLuyen.tamMa
        val tmTier = when {
            tm < 20 -> "tĩnh lặng"
            tm < 50 -> "xao động"
            tm < 70 -> "hỗn loạn"
            else -> "nguy: hỏng là tẩu hỏa"
        }
        measure(c, k, row(0), "Tâm ma", tmTier, "$tm / 100", tm / 100f,
            "Tăng: đứt chuỗi +10, nhảy chương nhiều +2, đột phá hỏng +10, kiếp hỏng +15. Giảm: buổi 45 phút −3, ngày 30 phút −1, đọc xong sách −10.")

        val wounds = ArrayList<String>()
        if (TuLuyen.injured()) wounds.add("Kinh mạch tổn thương: còn ${TuLuyen.injuryUntil - today} ngày, tu luyện 70%; Thọ Nguyên Đan chữa được")
        if (TuLuyen.resting()) wounds.add("Hồi khí: còn ${TuLuyen.restUntil - today} ngày mới thử đột phá lại")
        if (TuLuyen.tauHoaDay >= 0 && today - TuLuyen.tauHoaDay < 30) wounds.add("Tẩu hỏa nhập ma: tâm cảnh −20, căn cơ −10 tới ngày ${dayText(TuLuyen.tauHoaDay + 30)}")
        if (TuLuyen.tamMaKiep) wounds.add("Tâm ma kiếp: hạ tâm ma dưới 50 mới đột phá được")
        if (Store.tuViLoss > 0) wounds.add("Tu vi đã mất ${Realm.van(Store.tuViLoss)} vạn; Thọ Nguyên Đan lấy lại một nửa")
        measure(c, k, row(1), "Thương thế", if (wounds.isEmpty()) "lành lặn" else "${wounds.size} vết", "",
            -1f, if (wounds.isEmpty()) "Không có thương thế. Đột phá hỏng có ba mức: hụt khí, kinh mạch tổn thương, tẩu hỏa nhập ma; không mức nào làm rớt cảnh giới." else wounds.joinToString(". ") + ".")

        val trucCo = if (TuLuyen.coPham >= 0) "Trúc Cơ ${TuLuyen.TRUC_CO_PHAM[TuLuyen.coPham].lowercase()}: căn cơ gốc +${TuLuyen.baseCanCo()} suốt đời tu." else
            "Trúc Cơ chưa có phẩm: căn cơ và áp chế lúc phá Trúc Cơ quan quyết định phẩm (hạ, trung, thượng, hoàn mỹ)."
        val kimDan = if (TuLuyen.danPham >= 0) " ${com.exacta.reader.data.DaoLo.form(2) ?: "Kim Đan"} ${TuLuyen.DAN_PHAM[TuLuyen.danPham].lowercase()}: tu tới Nguyên Anh nhanh thêm ${if (TuLuyen.danPham >= 9) 25 else 2 * (TuLuyen.danPham + 1)}%." else
            " Kim Đan: phẩm theo độ dư của lần ngưng đan, từ cửu phẩm tới nhất phẩm, hiếm khi Vô Hà."
        measure(c, k, row(2), "Đạo cơ", if (TuLuyen.coPham >= 0) "đã đặt nền" else "chưa đặt", "", -1f, trucCo + kimDan)

        val tally = "Đột phá thành công ${TuLuyen.breaks} lần, hỏng ${TuLuyen.breakFails} lần; vượt kiếp ${Store.trialPassed} lần, thất bại ${Store.failCount} lần." +
            if (TuLuyen.lastResult.isNotEmpty()) " Gần nhất: ${TuLuyen.lastResult}." else ""
        measure(c, k, row(3), "Đạo đồ", "${TuLuyen.breaks + TuLuyen.breakFails} lần phá cảnh", "", -1f, tally)

        // Thọ nguyên: a day is a year.
        val d = com.exacta.reader.data.DoKiep
        val span = d.span()
        val life = if (span == Int.MAX_VALUE) "Tiên nhân: thọ nguyên vô tận." else
            "Tuổi ${d.age()} năm, hạn ${span} năm của ${Realm.baseName(Store.realmCap.coerceIn(0, 9))}; mỗi ngày là một năm, lên cảnh giới thì hạn dài thêm. " +
                (if (d.lifeGamble() != null) "Sắp cạn: đột phá liều mạng +10%. " else "") +
                (if (Store.chanTu) "Chân Tu: cạn là tọa hóa, chuyển thế trùng tu." else "Tùy Duyên: thọ nguyên chỉ là cảm giác thời gian.") +
                if (d.rebirths > 0) " Đã chuyển thế ${d.rebirths} lần." else ""
        measure(c, k, row(4), "Thọ nguyên", if (span == Int.MAX_VALUE) "vô tận" else if (Store.chanTu) "Chân Tu" else "Tùy Duyên",
            if (span == Int.MAX_VALUE) "" else "${d.age()} / $span", if (span == Int.MAX_VALUE) -1f else d.age().toFloat() / span, life)
        return 2
    }

    private fun apChePercent(): Int {
        val cap = Store.realmCap.coerceIn(0, 8)
        val span = TuLuyen.floor(cap + 1) - TuLuyen.floor(cap)
        return (TuLuyen.apChe * 100 / span).toInt()
    }

    private fun dayText(d: Long) = LocalDate.ofEpochDay(d).let { "${it.dayOfMonth}/${it.monthValue}" }

    // --- Đột phá ------------------------------------------------------------------------------------

    private fun dotPha(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        val g = TuLuyen.gate()
        if (Store.realmCap >= 9) {
            val r = RectF(a.left, a.top, a.right, a.top + 140 * u)
            k.card(c, r)
            k.realmIcon(c, 9, r.left + pad, r.top + pad, 80 * u)
            k.text(c, "Đã phi thăng", r.left + 2 * pad + 80 * u, r.top + pad, k.ui12Bold)
            val tw = r.width() - 3 * pad - 80 * u
            var ty = r.top + pad + k.lineH(k.ui12Bold) + 6 * u
            for (l in k.wrap("${TuLuyen.tienKind()} của ${TuLuyen.GIOI[TuLuyen.gioiNow()]}. Không còn cửa ải; đường sau Lục Giới để dành cho chương sau.", tw, small, 2)) {
                k.text(c, l, r.left + 2 * pad + 80 * u, ty, small)
                ty += sl
            }
            return 1
        }

        // The gate.
        val iconS = 64 * u
        val head = RectF(a.left, a.top, a.right, a.top + iconS + 2 * pad)
        k.card(c, head, selected = TuLuyen.canBreak())
        k.realmIcon(c, g, head.left + pad, head.top + pad, iconS)
        val tx = head.left + 2 * pad + iconS
        k.text(c, k.fit("Cửa ải ${Treasure.GATES[g]}", head.right - pad - tx, k.ui12Bold), tx, head.top + pad, k.ui12Bold)
        val from = if (Store.realmCap == 0) "Luyện Khí" else TuLuyen.shortName(Store.realmCap)
        val world = if (g == 5) " · phi thăng Linh giới" else if (g == 9) " · chạm Tiên cảnh" else ""
        k.text(c, k.fit("Từ $from vào ${Realm.baseName(g)}$world", head.right - pad - tx, small), tx, head.top + pad + k.lineH(k.ui12Bold) + 4 * u, small)
        var y = head.bottom + 16 * u

        // What it asks.
        y = k.section(c, a.left, y, a.width(), "ĐIỀU KIỆN")
        val box = 22 * u
        for (n in TuLuyen.needs()) {
            k.check(c, a.left + 4 * u, y + (sl - box) / 2, box, n.met)
            val ls = k.wrap(n.text, a.width() - box - 20 * u, if (n.met) small else sb, 2)
            for (l in ls) {
                k.text(c, l, a.left + box + 16 * u, y, if (n.met) small else sb)
                y += sl
            }
            y += 6 * u
        }
        if (Store.inTrial()) {
            val need = Store.trialNeed()
            val days = Store.trialDays().first
            val cw = minOf(64 * u, (a.width() - box - 16 * u - (need - 1) * 8 * u) / need)
            val x0 = a.left + box + 16 * u
            for (i in 0 until need) {
                val cell = RectF(x0 + i * (cw + 8 * u), y, x0 + i * (cw + 8 * u) + cw, y + 20 * u)
                if (i < days) c.drawRoundRect(cell, 5 * u, 5 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 5 * u, 5 * u, k.stroke(1.6f))
            }
            y += 28 * u
            val today = Store.today()
            val now = if (g == 2) "Ngày ${days}/$need · buổi dài nhất hôm nay ${TuLuyen.bestMinutes(today)}/${Store.trialMinutes()} phút. Thoát giữa buổi thì phải đọc lại từ đầu buổi."
                else "Ngày ${days}/$need · hôm nay ${Store.minutes(today)}/${Store.trialMinutes()} phút. Lỡ một ngày ở giữa là thất bại."
            val attack = if (g == 3) " Tâm ma tấn công ${TuLuyen.attacks}/3: nhảy chương, đổi cuốn khác, bỏ buổi đọc dưới 10 phút." else ""
            for (l in k.wrap(now + attack, a.width() - box - 16 * u, small, 3)) {
                k.text(c, l, x0, y, small)
                y += sl
            }
        }
        y += 12 * u

        // The chance, part by part.
        y = k.section(c, a.left, y, a.width(), "TỈ LỆ ĐỘT PHÁ")
        val rowH = sl + 4 * u
        for ((label, v) in TuLuyen.parts()) {
            k.pair(c, label, if (v >= 0) "+$v%" else "−${-v}%", a.left, y, a.width())
            y += rowH
        }
        c.drawRect(a.left, y + 2 * u, a.right, y + 3 * u, k.fill)
        y += 8 * u
        k.pair(c, "Tỉ lệ thành công (5% – 95%)", "${TuLuyen.chance()}%", a.left, y, a.width(), sb, k.ui10Bold)
        y += k.lineH(k.ui10Bold) + 4 * u
        val failText = when (TuLuyen.failKind()) {
            TuLuyen.TAU_HOA -> "Nếu hỏng lúc tâm ma từ 70: tẩu hỏa nhập ma, tâm cảnh −20, căn cơ −10, phải qua tâm ma kiếp."
            TuLuyen.HUT_KHI -> "Nếu hỏng: hụt khí, mất 2% linh lực, hồi khí 2 ngày. Lần sau thêm 10%."
            else -> "Nếu hỏng (tỉ lệ dưới 50%): kinh mạch tổn thương, 5 ngày tu luyện 70%. Lần sau thêm 10%."
        }
        for (l in k.wrap(failText, a.width(), small, 2)) {
            k.text(c, l, a.left, y, small)
            y += sl
        }

        // The buttons, at the bottom.
        val bh = 56 * u
        val by = a.bottom - bh
        val info = result ?: TuLuyen.lastResult.takeIf { it.isNotEmpty() }?.let { "Gần nhất: $it" }
        if (info != null) {
            val ls = k.wrap(info, a.width(), sb, 2)
            var iy = by - 10 * u - ls.size * sl
            for (l in ls) {
                k.text(c, l, a.left, iy, sb)
                iy += sl
            }
        }
        val pillW = if (Store.canBreakPill()) 250 * u else 0f
        val main = RectF(a.left, by, a.right - (if (pillW > 0) pillW + 12 * u else 0f), a.bottom)
        when {
            // Tiểu đột phá, from Kim Đan: into the next tiểu cảnh, two taps too.
            TuLuyen.minorReady() && !TuLuyen.atCeiling() -> {
                val into = TuLuyen.TIEU[TuLuyen.subCap + 1]
                if (TuLuyen.minorResting()) {
                    k.button(c, main, "Tiểu cảnh $into: hồi khí, mai thử lại", sb, enabled = false)
                } else {
                    k.button(c, main, if (armed) "Chạm lần nữa để phá tiểu cảnh · ${TuLuyen.minorChance()}%" else "Phá tiểu cảnh vào $into · ${TuLuyen.minorChance()}%",
                        k.ui10Bold, selected = armed)
                    hits.add(main) {
                        if (!armed) {
                            armed = true
                        } else {
                            armed = false
                            result = when (TuLuyen.minorBreak()) {
                                true -> "Phá tiểu cảnh thành công: ${TuLuyen.name()}"
                                false -> TuLuyen.lastResult
                                null -> null
                            }
                        }
                        refresh()
                    }
                }
            }
            TuLuyen.canStartTrial() -> {
                k.button(c, main, "Bắt đầu ${TuLuyen.trialName().lowercase()} · ${TuLuyen.trialRule()}", sb)
                hits.add(main) {
                    if (TuLuyen.startTrial()) result = "${TuLuyen.trialName()} đã bắt đầu từ hôm nay."
                    refresh()
                }
            }
            TuLuyen.canBreak() -> {
                k.button(c, main, if (armed) "Chạm lần nữa để đột phá · ${TuLuyen.chance()}%" else "Đột phá ${Realm.baseName(g)} · ${TuLuyen.chance()}%",
                    k.ui10Bold, selected = armed)
                hits.add(main) {
                    if (!armed) {
                        armed = true
                    } else {
                        armed = false
                        result = when (TuLuyen.breakThrough()) {
                            true -> "Đột phá thành công! " + TuLuyen.lastResult
                            false -> TuLuyen.lastResult
                            null -> null
                        }
                    }
                    refresh()
                }
            }
            g == 8 && Store.realmCap == 7 -> {
                k.button(c, main, "Độ chín tầng kiếp ở Linh giới ›", k.ui10Bold)
                hits.add(main) { startActivity(Intent(this, LinhGioiActivity::class.java).putExtra(EXTRA_TAB, 4)) }
            }
            g == 2 && com.exacta.reader.data.DaoLo.canChoose() -> {
                k.button(c, main, "Chọn tiên lộ trước khi ngưng đan ›", k.ui10Bold)
                hits.add(main) { startActivity(Intent(this, DaoLoActivity::class.java)) }
            }
            else -> {
                val why = TuLuyen.blockers().firstOrNull() ?: TuLuyen.needs().firstOrNull { !it.met }?.let { "Chưa đủ điều kiện" } ?: "Chưa thể đột phá"
                k.button(c, main, why, sb, enabled = false)
            }
        }
        if (pillW > 0) {
            val pr = RectF(a.right - pillW, by, a.right, a.bottom)
            k.button(c, pr, "Phá Cảnh Đan (${Store.items[Treasure.PHA_CANH_DAN]})", sb)
            hits.add(pr) {
                val text = Store.useSimple(Treasure.PHA_CANH_DAN)
                if (text != null) result = text
                refresh()
            }
        }
        return 2
    }

    /**
     * Page 2 of Đột phá: the road as a row of marks, the gate ahead in full and the one after it in a
     * line, and what a failure costs. The gates far ahead are not spelled out.
     */
    private fun gates(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val passed = Store.realmCap.coerceIn(0, 9)
        var y = k.section(c, a.left, a.top, a.width(), "ĐƯỜNG TU · ĐÃ VƯỢT $passed/9 CỬA ẢI")

        // The ten realms as marks: those passed dark, the next ringed, the rest pale.
        val n = 10
        val gap = 8 * u
        val s = minOf(56 * u, (a.width() - (n - 1) * gap) / n)
        val x0 = a.left + (a.width() - n * s - (n - 1) * gap) / 2
        for (r in 0 until n) {
            val x = x0 + r * (s + gap)
            k.realmIcon(c, r, x, y + 6 * u, s)
            if (r > passed + 1 || (r == passed + 1 && passed >= 9)) c.drawRect(x - u, y + 5 * u, x + s + u, y + 7 * u + s, k.paint(1f).apply { color = android.graphics.Color.argb(185, 255, 255, 255) })
            if (r == passed + 1) c.drawRoundRect(RectF(x - 4 * u, y + 2 * u, x + s + 4 * u, y + 10 * u + s), 8 * u, 8 * u, k.stroke(2.4f))
        }
        y += s + 30 * u

        val asks = { g: Int ->
            when (g) {
                1 -> "Căn cơ từ 30. Căn cơ lúc đột phá định phẩm Trúc Cơ, theo suốt đời tu."
                2 -> "Chọn tiên lộ, rồi ngưng đan ${Store.trialNeed(2)} ngày, mỗi ngày một buổi liền ${Store.trialMinutes(2)} phút."
                3 -> "Lôi kiếp ${Store.trialNeed(3)} ngày × ${Store.trialMinutes(3)} phút, kèm tâm ma kiếp: đọc một cuốn tới nơi tới chốn."
                4 -> "Thần thức Lục trọng và ngộ đạo ba lần: ba cuốn từ 20 vạn chữ, mỗi cuốn đọc trọn trong 30 ngày."
                9 -> "Lục Giới cảm ứng rõ một giới, rồi thiên kiếp ${Store.trialNeed(9)} ngày × ${Store.trialMinutes(9)} phút."
                else -> "Thiên kiếp ${Store.trialNeed(g)} ngày × ${Store.trialMinutes(g)} phút."
            }
        }
        if (passed >= 9) {
            y = k.section(c, a.left, y, a.width(), "ĐÃ QUA CHÍN CỬA ẢI")
            for (l in k.wrap("${TuLuyen.tienKind()} của ${TuLuyen.GIOI[TuLuyen.gioiNow()]}. Đường sau Lục Giới để dành cho chương sau.", a.width(), small, 3)) {
                k.text(c, l, a.left, y, small)
                y += sl
            }
        } else {
            val g = passed + 1
            val pad = 18 * u
            val ic = 64 * u
            val lines = k.wrap(asks(g), a.width() - 2 * pad - ic - 16 * u, small, 3)
            val card = RectF(a.left, y, a.right, y + 2 * pad + maxOf(ic, k.lineH(k.ui12Bold) + 4 * u + sl + 6 * u + lines.size * sl))
            k.card(c, card, selected = true)
            k.realmIcon(c, g, card.left + pad, card.top + pad, ic)
            val tx = card.left + pad + ic + 16 * u
            val tw = card.right - pad - tx
            k.line(c, "Cửa ải ${Treasure.GATES[g]}", tx, card.top + pad, tw, k.ui12Bold)
            var ty = card.top + pad + k.lineH(k.ui12Bold) + 4 * u
            k.line(c, "Vào ${Realm.baseName(g)} · nền ${TuLuyen.BASE[g]}%", tx, ty, tw, sb)
            ty += sl + 6 * u
            for (l in lines) {
                k.text(c, l, tx, ty, small)
                ty += sl
            }
            y = card.bottom + 14 * u
            if (g < 9) {
                k.line(c, "Sau đó: ${Treasure.GATES[g + 1]}, vào ${Realm.baseName(g + 1)}", a.left, y, a.width(), small)
                y += sl + 18 * u
            }
        }

        y = k.section(c, a.left, y, a.width(), "KHI ĐỘT PHÁ HỎNG")
        val rules = listOf(
            "Tỉ lệ từ 50%: hụt khí, mất 2% linh lực, nghỉ 2 ngày.",
            "Tỉ lệ dưới 50%: kinh mạch tổn thương, 5 ngày chỉ tu 70%.",
            "Tâm ma từ 70: tẩu hỏa nhập ma, tâm cảnh và căn cơ giảm 30 ngày.",
            "Không bao giờ rớt cảnh giới; lần sau thêm 10%.",
        )
        for (r in rules) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl * 0.5f, 4 * u)
            k.line(c, r, a.left + 20 * u, y, a.width() - 20 * u, small)
            y += sl + 6 * u
        }
        return 2
    }

    private fun kinhCac(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val finished = Store.books.values.filter { it.finishDay >= 0 }.sortedByDescending { it.finishDay }
        var y = k.section(c, a.left, a.top, a.width(), "KINH CÁC · ${finished.size} BỘ CÔNG PHÁP")
        if (finished.isEmpty()) {
            k.text(c, "Đọc hết một cuốn, sách sẽ được cất vào đây.", a.left, y, k.small)
            return 1
        }
        val rowH = 120 * u
        val per = ((a.bottom - y) / rowH).toInt().coerceAtLeast(1)
        val pages = (finished.size + per - 1) / per
        val tb = k.ui10Bold
        val small = k.small
        for (b in finished.drop(page * per).take(per)) {
            val cr = RectF(a.left, y, a.left + 72 * u, y + rowH - 14 * u)
            k.cover(c, b.cover, b.title, cr)
            val tx = cr.right + 16 * u
            val tw = a.right - tx
            var ty = y + 4 * u
            for (l in k.wrap(b.title, tw, tb, 2)) {
                k.text(c, l, tx, ty, tb)
                ty += k.lineH(tb)
            }
            if (b.author.isNotEmpty()) {
                k.text(c, k.fit(b.author, tw, small), tx, ty, small)
                ty += k.lineH(small)
            }
            val day = LocalDate.ofEpochDay(b.finishDay)
            k.text(c, k.fit("Xong ${day.dayOfMonth}/${day.monthValue}/${day.year} · ${Realm.van(b.words.coerceAtLeast(0))} vạn từ · ${HomeView.duration(b.seconds)}", tw, small), tx, ty, small)
            y += rowH
        }
        return pages
    }
}
