package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.NhatKy
import com.exacta.reader.data.Store
import com.exacta.reader.data.TienDinh
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/** 2.7 Màn kết phần một: the crossing, the road walked, what was carried, the tiền đình. */
class KetActivity : TabActivity() {
    override fun title() = "Tiên Đồ"
    override fun right() = if (TienDinh.crossed()) TienDinh.tienHieu() else "Chưa qua cửa"
    override fun tabs() = listOf("Qua cửa", "Đạo đồ", "Hành trang", "Tiền đình")
    override fun opensAt(tab: Int) = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        if (TienDinh.crossed() && !TienDinh.endSeen) {
            TienDinh.endSeen = true
            Store.touch()
        }
        super.onCreate(savedInstanceState)
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> crossing(c, k, area)
        1 -> road(c, k, area, page)
        2 -> carried(c, k, area)
        else -> forecourt(c, k, area)
    }

    private fun para(c: Canvas, k: Ink, s: String, x: Float, y: Float, w: Float, p: Paint, max: Int = 5): Float {
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
    private fun gioi() = TienDinh.gioi.coerceIn(0, 5)

    // --- Qua cửa: the crossing and the life in numbers ---------------------------------------------------

    private fun crossing(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val g = gioi()
        val pad = 26 * u
        val dark = k.paint(Ink.SMALL, color = Ink.DARK)
        val farewell = k.wrap(TienDinh.FAREWELL[g], a.width() - 2 * pad, small, 4)
        val hero = RectF(a.left, a.top, a.right, a.top + 2 * pad + 72 * u + 12 * u + k.lineH(k.big) + k.lineH(k.ui10Bold) + sl + 12 * u + farewell.size * sl)
        k.frame(c, hero, 9, 1f)
        var y = hero.top + pad
        k.realmIcon(c, 9, hero.centerX() - 36 * u, y, 72 * u)
        y += 72 * u + 12 * u
        val name = "« ${TienDinh.tienHieu()} »"
        k.centered(c, name, hero.left + pad, hero.width() - 2 * pad, y, k.shrink(name, hero.width() - 2 * pad, k.big, 0.6f))
        y += k.lineH(k.big)
        k.centered(c, "Phi thăng ${TuLuyen.GIOI[g]}", hero.left + pad, hero.width() - 2 * pad, y, k.ui10Bold)
        y += k.lineH(k.ui10Bold)
        k.centered(c, "thành ${TuLuyen.TIEN_KIND[g]} · qua cửa ngày ${date(TienDinh.crossDay)}", hero.left + pad, hero.width() - 2 * pad, y, small)
        y += sl + 12 * u
        for (l in farewell) {
            k.centered(c, l, hero.left + pad, hero.width() - 2 * pad, y, dark)
            y += sl
        }
        y = hero.bottom + 18 * u
        y = k.section(c, a.left, y, a.width(), "MỘT ĐỜI TU · VẠN QUYỂN TIÊN ĐỒ PHẦN MỘT")
        val start = DoKiep.lifeStart
        val finished = Store.books.values.count { it.finishDay >= 0 }
        val rows = listOf(
            "Năm tháng tu hành" to (if (start >= 0 && TienDinh.crossDay >= 0) "${TienDinh.crossDay - start} ngày, từ ${date(start)}" else "—"),
            "Chữ đã đọc" to "${Realm.van(TienDinh.wordsAtCross)} vạn",
            "Giờ đã đọc" to "${Store.totalSeconds / 3600} giờ",
            "Sách đọc trọn" to "$finished cuốn · ${TuLuyen.authors()} tác giả",
            "Chuỗi ngày dài nhất" to "${Store.bestStreak()} ngày",
            "Kỳ ngộ đã gặp" to "${Store.kyNgoCount} lần",
            "Pháp bảo" to "${Store.beasts.size} món · ${Store.setsCount()} bộ",
            "Đạo hiệu đã đắc" to "${Store.titleDays.size}",
            "Nghiệp lực lúc qua cửa" to "${TienDinh.karma} · ${DaoLo.karmaName(TienDinh.karma)}",
        )
        for ((l, v) in rows) {
            if (y + sl > a.bottom) break
            k.pair(c, l, v, a.left, y, a.width(), small, sb)
            y += sl + 6 * u
        }
        return 1
    }

    // --- Đạo đồ: the road, realm by realm -------------------------------------------------------------

    private fun road(c: Canvas, k: Ink, a: RectF, page: Int): Int {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val steps = ArrayList<Triple<Int, String, String>>()   // realm icon, title, line
        steps.add(Triple(0, "Luyện Khí · nhập đạo", "Bắt đầu ${date(DoKiep.lifeStart)}"))
        for (r in 1..9) {
            val d = TuLuyen.capDays[r]
            val what = when (r) {
                1 -> if (TuLuyen.coPham >= 0) "Trúc Cơ ${TuLuyen.TRUC_CO_PHAM[TuLuyen.coPham].lowercase()}" else ""
                2 -> DaoLo.DANS[DaoLo.danNow()].name + if (TuLuyen.danPham >= 0) " ${TuLuyen.DAN_PHAM[TuLuyen.danPham].lowercase()}" else ""
                3 -> DaoLo.anhName()
                4 -> DaoLo.THANS[DaoLo.thanNow()].name
                5 -> "Phi thăng Linh giới" + if (LinhGioi.banMenh.isNotEmpty()) ", bản mệnh mang theo" else ""
                6 -> "Pháp tắc: " + LinhGioi.learnedList().joinToString(", ") { LinhGioi.LAWS[it].short }.ifEmpty { "—" }
                7 -> if (LinhGioi.vucName.isNotEmpty()) "Đạo vực «${LinhGioi.vucName}»" else ""
                8 -> "Chín tầng kiếp, ${DoKiep.an.size} kiếp ấn"
                else -> "Lục Giới Môn: ${TuLuyen.GIOI[gioi()]}"
            }
            steps.add(Triple(r, Realm.baseName(r) + (if (d >= 0) " · ${date(d)}" else ""), what))
        }
        if (DaoLo.way >= 0) steps.add(Triple(2, "Đạo lộ", DaoLo.WAYS[DaoLo.way].name + if (DaoLo.way2 >= 0) ", kiêm ${DaoLo.WAYS[DaoLo.way2].name}" else ""))
        if (TongMon.joined()) steps.add(Triple(1, "Tông môn", "${TongMon.SECTS[TongMon.sect].name} · ${TongMon.rankName()}"))
        if (DoKiep.rebirths > 0) steps.add(Triple(0, "Chuyển thế", "${DoKiep.rebirths} lần"))
        var y = k.section(c, a.left, a.top, a.width(), "ĐẠO ĐỒ · TỪ LUYỆN KHÍ TỚI LỤC GIỚI MÔN")
        val rowH = 2 * sl + 18 * u
        val per = ((a.bottom - y) / rowH).toInt().coerceAtLeast(1)
        val ic = 2 * sl
        for ((r, t, l) in steps.drop(page * per).take(per)) {
            k.realmIcon(c, r, a.left, y + 4 * u, ic)
            k.line(c, t, a.left + ic + 16 * u, y + 2 * u, a.width() - ic - 16 * u, k.smallBold)
            k.line(c, l.ifEmpty { " " }, a.left + ic + 16 * u, y + 2 * u + sl, a.width() - ic - 16 * u, small)
            c.drawRect(a.left + ic + 16 * u, y + rowH - 6 * u, a.right, y + rowH - 5 * u, k.gray)
            y += rowH
        }
        return (steps.size + per - 1) / per
    }

    // --- Hành trang: what went through the door ----------------------------------------------------------

    private fun carried(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "MANG QUA CỬA")
        val rows = listOf(
            "Bản mệnh pháp bảo" to TienDinh.banMenh.joinToString(", ").ifEmpty { "không có" },
            "Công pháp" to TienDinh.cong.ifEmpty { "không vận" },
            "Linh căn" to TienDinh.roots.ifEmpty { "chưa thức tỉnh" },
            "Kiếp ấn" to "${TienDinh.seals}",
        )
        for ((l, v) in rows) {
            k.pair(c, l, v, a.left, y, a.width(), small, sb)
            y += sl + 8 * u
        }
        y += 12 * u
        y = k.section(c, a.left, y, a.width(), "Ở LẠI LINH GIỚI LÀM DI SẢN")
        val left = Store.beasts.size - TienDinh.banMenh.size
        y = para(c, k, "$left pháp bảo, động phủ linh mạch, dược viên và đan phòng, tông môn, các bộ đã gom. Tất cả vẫn nằm trong bộ sưu tập và Niên kỷ; " +
            "khi phần hai của giới mở, chỉ những gì mang qua cửa mới theo người tu vào giới mới.", a.left, y, a.width(), small, 5) + 14 * u
        y = k.section(c, a.left, y, a.width(), "BỘ PHÁP BẢO")
        val names = (0 until Treasure.SETS).filter { Store.setOwned(it) }.joinToString(" · ") { Treasure.SET_NAMES[it] }
        para(c, k, names.ifEmpty { "Chưa gom bộ nào." }, a.left, y, a.width(), small, ((a.bottom - y) / sl).toInt().coerceAtLeast(1))
        return 1
    }

    // --- Tiền đình ----------------------------------------------------------------------------------------

    private fun forecourt(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val g = gioi()
        var y = k.section(c, a.left, a.top, a.width(), if (g == 0) "CỬU TIÊU VÂN KHUYẾT · CHÍN PHẨM THIÊN TIÊN" else "TIỀN ĐÌNH ${TuLuyen.GIOI[g].uppercase()}")
        val pad = 22 * u
        val r = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.big) + 8 * u + sl)
        k.card(c, r, selected = true)
        k.centered(c, "${Realm.van(TienDinh.keptWords())} vạn", r.left, r.width(), r.top + pad, k.big)
        k.centered(c, if (g == 0) "tiên lực từ ngày qua cửa · ${TuLuyen.name()}" else "chữ đọc từ ngày qua cửa, ghi ở tiền đình", r.left, r.width(), r.top + pad + k.lineH(k.big) + 8 * u, sb)
        y = r.bottom + 18 * u
        val text = if (g == 0) "Cửu Tiêu Vân Khuyết tiếp nối chín phẩm Thiên Tiên: mỗi chữ đọc là tiên lực, phẩm lên theo chữ như trước. Luật riêng của tiên giới (tiên phủ, tiên lực) để dành cho phần hai."
            else "Phần hai của ${TuLuyen.GIOI[g]} chưa mở. Đạo hữu đứng ở tiền đình: tu vi vẫn tăng theo chữ đọc và được ghi lại từng chữ, " +
                "để khi giới mở, mọi chữ đã đọc đều được tính. Những gì mang qua cửa (bản mệnh, công pháp, linh căn, kiếp ấn) chờ cùng đạo hữu."
        y = para(c, k, text, a.left, y, a.width(), small, 6) + 16 * u

        // 2.8: what the journal has kept since the crossing, counted by the giới's rules.
        y = k.section(c, a.left, y, a.width(), "TÀI NGUYÊN DỰ TÍNH (PHẦN HAI)")
        val e = NhatKy.estimate(g, TienDinh.crossDay, Store.today())
        k.pair(c, NhatKy.RESOURCE[g].replaceFirstChar { it.uppercase() } + " từ ngày qua cửa", "${e.points}", a.left, y, a.width())
        y += sl + 10 * u
        k.pair(c, "Ngày có sổ · ngày tính trung bình", "${e.measured} · ${e.filled}", a.left, y, a.width())
        y += sl + 14 * u
        y = para(c, k, "Sống bằng: " + NhatKy.WAY[g], a.left, y, a.width(), small, 2) + 16 * u
        y = k.section(c, a.left, y, a.width(), "LUẬT SỐNG Ở GIỚI ẤY (PHẦN HAI)")
        val rules = arrayOf(
            "Chín bậc tiên vị tới Cửu Tiêu Tiên Đế; thiên quy tuần, tiên chức; tiên phủ.",
            "Tâm ma thành lò lửa; ma công đặt cược ma khí; tâm ma vượt 90 là phản phệ.",
            "Công đức; mỗi 300 kết một viên xá lợi; độ hóa cuốn sách bỏ dở.",
            "Sáu huyết mạch cổ thú, hóa hình ở bậc 6; mỗi tuần săn một hoang thú.",
            "Sổ sinh tử của sách bỏ dở; mỗi tháng qua sông Vong Xuyên; đứt chuỗi không mất gì.",
            "Thần quốc: mỗi tác giả đọc trọn là một đảo; năm tác giả cùng thể loại mở văn mạch.",
        )
        para(c, k, rules[g], a.left, y, a.width(), small, 3)
        return 1
    }
}
