package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/**
 * 2.1 Công pháp, in tabs: the linh căn and the hours it comes from, the công pháp running and the
 * others owned, the tâm pháp in their slots, the thuật pháp carried, and the Tàng Kinh Các.
 */
class CongPhapActivity : TabActivity() {
    override fun title() = "Công Pháp"
    override fun right() = "Ngộ tính ${DaoPhap.ngo}"
    override fun tabs() = listOf("Linh căn", "Công pháp", "Tâm pháp", "Thuật pháp", "Tàng Kinh")

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> linhCan(c, k, area, hits)
        1 -> when (page) {
            0 -> congPhap(c, k, area, hits)
            1 -> speedPage(c, k, area)
            else -> owned(c, k, area, page - 2, hits) + 2
        }
        2 -> tamPhap(c, k, area, page, hits)
        3 -> thuatPhap(c, k, area, page, hits)
        else -> tangKinh(c, k, area, page, hits)
    }.let { if (tab == 1) maxOf(it, 2 + ownedPages(k, area)) else it }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    /** Lines of `s` wrapped to `w`, drawn from y; returns the y below. */
    private fun para(c: Canvas, k: Ink, s: String, x: Float, y: Float, w: Float, p: Paint, max: Int = 3): Float {
        var yy = y
        for (l in k.wrap(s, w, p, max)) {
            k.text(c, l, x, yy, p)
            yy += k.lineH(p)
        }
        return yy
    }

    /** A card with a title, a tag on the right, two lines, and a button on the right. */
    private fun row(
        c: Canvas, k: Ink, r: RectF, title: String, tag: String, line1: String, line2: String,
        button: String?, enabled: Boolean, selected: Boolean, hits: Hits, onTap: () -> Unit,
    ) {
        val u = k.u
        val pad = 14 * u
        k.card(c, r, selected = selected && button == null)
        val bw = if (button != null) 190 * u else 0f
        val tw = r.width() - 2 * pad - (if (bw > 0) bw + pad else 0f)
        var y = r.top + pad
        val tagW = if (tag.isNotEmpty()) k.smallBold.measureText(tag) + 12 * u else 0f
        k.text(c, k.fit(title, tw - tagW, k.ui10Bold), r.left + pad, y, k.ui10Bold)
        if (tag.isNotEmpty()) k.right(c, tag, r.left + pad + tw, y + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
        y += k.lineH(k.ui10Bold) + 2 * u
        k.text(c, k.fit(line1, tw, k.small), r.left + pad, y, k.small)
        y += k.lineH(k.small)
        val room = ((r.bottom - pad / 2 - y) / k.lineH(k.small)).toInt().coerceIn(1, 2)
        para(c, k, line2, r.left + pad, y, tw, k.small, room)
        if (button != null) {
            val b = RectF(r.right - pad - bw, r.centerY() - 26 * u, r.right - pad, r.centerY() + 26 * u)
            k.button(c, b, button, k.smallBold, enabled = enabled, selected = selected)
            if (enabled) hits.add(b) { onTap(); refresh() }
        }
    }

    private fun rowH(k: Ink) = 2 * 14 * k.u + k.lineH(k.ui10Bold) + 2 * k.u + 3 * k.lineH(k.small)

    // --- Linh căn ------------------------------------------------------------------------------------

    private fun linhCan(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u

        // The root, or what it waits for.
        val headLines = ArrayList<String>()
        if (DaoPhap.awakened()) {
            val d = LocalDate.ofEpochDay(DaoPhap.awakenDay)
            val b = DaoPhap.rootBonus()
            headLines.add("Thức tỉnh ngày ${d.dayOfMonth}/${d.monthValue}/${d.year} · chủ hành ${PhapMon.ATTR[DaoPhap.roots[0]]} (${PhapMon.TRAITS[DaoPhap.roots[0]]})")
            headLines.add("Linh lực ${if (b >= 0) "+$b" else "−${-b}"}% · công pháp, thuật pháp hệ " +
                (DaoPhap.roots + listOfNotNull(DaoPhap.di.takeIf { it >= 0 })).joinToString(", ") { PhapMon.ATTR[it] } + " hợp, học không tốn ngộ tính" +
                if (DaoPhap.roots.size == 5) "; không hệ nào bị khắc" else "")
        } else {
            val hours = Store.hourSeconds.sum() / 3600
            headLines.add("Linh căn thức tỉnh ở Luyện Khí trung kỳ, theo khung giờ bạn hay đọc" + if (hours < 1) " (cần đọc ít nhất một giờ)." else ".")
            headLines.add("Mỗi hành chiếm từ ${DaoPhap.ROOT_SHARE}% giờ đọc là một linh căn. Đọc vào giờ nào, thành người tu thế ấy.")
        }
        val tw = a.width() - 2 * pad
        val lines = headLines.flatMap { k.wrap(it, tw, small, 3) }
        val head = RectF(a.left, a.top, a.right, a.top + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + lines.size * sl)
        k.card(c, head, selected = false)
        k.text(c, k.fit(DaoPhap.rootName(), tw, k.ui12Bold), a.left + pad, head.top + pad, k.ui12Bold)
        var y = head.top + pad + k.lineH(k.ui12Bold) + 6 * u
        for (l in lines) {
            k.text(c, l, a.left + pad, y, small)
            y += sl
        }
        y = head.bottom + 18 * u

        // The five hành and their hours.
        y = k.section(c, a.left, y, a.width(), "KHUNG GIỜ ĐỌC · TỪ ${DaoPhap.ROOT_SHARE}% LÀ LINH CĂN")
        val sec = DaoPhap.hanhSeconds()
        val shares = DaoPhap.shares(sec)
        val rh = 2 * sl + 14 * u
        val box = 22 * u
        for (h in 0 until 5) {
            val root = h in DaoPhap.roots
            k.check(c, a.left + 2 * u, y + (sl - box) / 2, box, root)
            val nx = a.left + box + 14 * u
            k.text(c, PhapMon.ATTR[h], nx, y, if (root) k.ui10Bold else k.ui10)
            val ty = y + (k.lineH(k.ui10) - sl) / 2
            val tail = "${HomeHours.text(sec[h])} · ${shares[h]}%"
            k.right(c, tail, a.right, ty, sb)
            k.text(c, k.fit("${PhapMon.HOURS[h]} · ${PhapMon.TRAITS[h]}", a.right - nx - 90 * u - sb.measureText(tail) - 16 * u, small), nx + 90 * u, ty, small)
            val my = y + k.lineH(k.ui10) + 4 * u
            val mw = a.right - nx
            k.meter(c, nx, my, mw, shares[h] / 100f, 10 * u)
            // The line of 15%.
            val lx = nx + mw * DaoPhap.ROOT_SHARE / 100f
            c.drawRect(lx - u, my - 4 * u, lx + u, my + 14 * u, k.fill)
            y += rh
        }
        y += 8 * u

        // Dị linh căn.
        y = k.section(c, a.left, y, a.width(), "DỊ LINH CĂN · HIẾM, LINH LỰC THÊM ${PhapMon.DI_BONUS}%")
        val di = listOf(
            Triple(PhapMon.LOI, "Lôi: ${DaoPhap.loiNight()}/10% giờ đọc từ 0 tới 3 giờ sáng, ${minOf(DaoPhap.loiRam(), 2)}/2 ngày rằm có đọc", DaoPhap.loiNight() >= 10 && DaoPhap.loiRam() >= 2),
            Triple(PhapMon.BANG, "Băng: ${minOf(DaoPhap.bangDays(), 20)}/20 ngày đọc từ 30 phút, từ Đông chí (22/12) tới Đại hàn (20/1)", DaoPhap.bangDays() >= 20),
            Triple(PhapMon.PHONG, "Phong: ${minOf(DaoPhap.phongBooks(), 3)}/3 cuốn đọc trọn trong một tuần", DaoPhap.phongBooks() >= 3),
        )
        for ((attr, text, met) in di) {
            k.check(c, a.left + 2 * u, y + (sl - box) / 2, box, met)
            val p = if (DaoPhap.di == attr) sb else small
            y = para(c, k, text + if (DaoPhap.di == attr) " · đang có" else "", a.left + box + 14 * u, y, a.width() - box - 14 * u, p, 2) + 6 * u
        }
        y += 6 * u
        para(c, k, "Một hành (Thiên linh căn) +30%, hai +15%, ba +5%, bốn −10%, năm (tạp linh căn) −20% nhưng không bị khắc và học mọi thuật pháp hệ ngũ hành không tốn ngộ tính.",
            a.left, y, a.width(), small, 3)

        // Tẩy linh, once.
        val bh = 56 * u
        val btn = RectF(a.left, a.bottom - bh, a.right, a.bottom)
        when {
            !DaoPhap.awakened() -> k.button(c, btn, if (Store.realmCap >= 1 || TuLuyen.sub(0, Store.tuVi()) >= 3) "Chưa thức tỉnh: cần đọc đủ một giờ" else "Chưa thức tỉnh: tới Luyện Khí trung kỳ", sb, enabled = false)
            DaoPhap.washed -> k.button(c, btn, "Đã tẩy linh một lần", sb, enabled = false)
            DaoPhap.washHours() < DaoPhap.WASH_HOURS -> k.button(c, btn, "Tẩy linh: cần ${DaoPhap.WASH_HOURS} giờ đọc từ lúc thức tỉnh (đang ${DaoPhap.washHours()})", sb, enabled = false)
            else -> {
                k.button(c, btn, "Tẩy linh căn theo giờ đọc gần đây · ${DaoPhap.WASH_PRICE} linh thạch", sb, enabled = DaoPhap.canWash())
                if (DaoPhap.canWash()) hits.add(btn) { washDialog() }
            }
        }
        return 1
    }

    private fun washDialog() {
        val roots = DaoPhap.rootsFrom(DaoPhap.hanhSeconds(DaoPhap.hoursAt))
        AlertDialog.Builder(this)
            .setTitle("Tẩy linh căn?")
            .setMessage("Linh căn tính lại theo ${DaoPhap.washHours()} giờ đọc từ lúc thức tỉnh: " +
                roots.joinToString(", ") { PhapMon.ATTR[it] } + ". Chỉ được một lần.\n\nGiá ${DaoPhap.WASH_PRICE} linh thạch · đang có ${Store.linhThach}.")
            .setPositiveButton("Tẩy linh") { _, _ ->
                if (DaoPhap.wash()) toast("Linh căn mới: ${DaoPhap.rootName()}")
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    // --- Công pháp -----------------------------------------------------------------------------------

    private fun congPhap(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        val d = DaoPhap.activeDef()
        var y = a.top
        if (d == null) {
            k.text(c, "Chưa vận công pháp nào.", a.left, y, k.ui10Bold)
            return 1
        }
        val id = DaoPhap.active
        val tang = DaoPhap.tang()
        val aff = DaoPhap.affinity(d.attr)
        val affText = when {
            aff > 0 -> " · hợp linh căn +20%"
            aff < 0 -> " · khắc linh căn −20%"
            else -> ""
        }
        val reach = PhapMon.REACH[d.pham].let { if (it >= 9) "chạm Tiên cảnh" else "tu tới ${TuLuyen.shortName(it)}" }
        val lock = DaoPhap.nextLock()
        val status = when {
            DaoPhap.complete() -> "Tầng 9 viên mãn: tuyệt học đã thành. Bộ phẩm cao hơn cùng mạch sẽ kế thừa một nửa số chữ."
            DaoPhap.xung > 0 -> "Đang xung quan tầng ${DaoPhap.xung}: buổi đọc tới phải liền ${PhapMon.xungMinutes(DaoPhap.xung)} phút; dừng sớm là khí nghịch."
            DaoPhap.waiting() -> "Đủ chữ: xung quan tầng $lock cần ${DaoPhap.openCost()} ngộ tính (đang có ${DaoPhap.ngo}) và một buổi đọc liền ${PhapMon.xungMinutes(lock ?: 9)} phút."
            else -> "Tầng $tang · còn ${Realm.van(DaoPhap.toNext())} vạn chữ tới tầng ${tang + 1}" + (lock?.let { " · tầng $it phải xung quan" } ?: "")
        }
        val statusLines = k.wrap(status, a.width() - 2 * pad, small, 2).size
        val button = DaoPhap.waiting() || DaoPhap.xung > 0
        val cardH = 2 * pad + k.lineH(k.ui12Bold) + 4 * u + sl + 16 * u + 36 * u + 10 * u + statusLines * sl + (if (button) 64 * u else 0f)
        val card = RectF(a.left, y, a.right, y + cardH)
        k.card(c, card, selected = DaoPhap.xung > 0)
        val x = a.left + pad
        val w = a.width() - 2 * pad
        y += pad
        k.text(c, k.fit(d.name, w - 160 * u, k.ui12Bold), x, y, k.ui12Bold)
        k.right(c, "${PhapMon.PHAM[d.pham]} phẩm", card.right - pad, y + (k.lineH(k.ui12Bold) - k.lineH(sb)) / 2, sb)
        y += k.lineH(k.ui12Bold) + 4 * u
        k.text(c, k.fit("Mạch ${PhapMon.MACH[d.mach]}$affText · $reach", w, small), x, y, small)
        y += sl + 16 * u

        // Nine tầng; 4, 7, 9 are broken through.
        val gap = 8 * u
        val cw = (w - 8 * gap) / 9
        val need = PhapMon.TANG_WORDS[d.pham]
        val words = DaoPhap.cong[id]?.words ?: 0
        val opened = DaoPhap.cong[id]?.opened ?: 3
        for (i in 1..9) {
            val cx = x + (i - 1) * (cw + gap)
            val cell = RectF(cx, y, cx + cw, y + 24 * u)
            val f = ((words - (i - 1) * need).toFloat() / need).coerceIn(0f, 1f)
            val locked = i in PhapMon.LOCKS && i > opened
            when {
                i <= tang && f >= 1f -> c.drawRoundRect(cell, 4 * u, 4 * u, k.fill)
                else -> {
                    c.drawRoundRect(k.inset(cell, 0.6f * u), 4 * u, 4 * u, k.stroke(if (locked) 2.6f else 1.3f))
                    if (f > 0f && i <= tang) c.drawRect(cx + u, y + u, cx + u + (cw - 2 * u) * f, y + 23 * u, k.fill)
                }
            }
            k.centered(c, if (locked) "quan" else "$i", cx, cw, y + 26 * u, if (i == tang) sb else small)
        }
        y += 36 * u + 10 * u
        y = para(c, k, status, x, y, w, small, 2)
        if (button) {
            val b = RectF(x, y + 10 * u, card.right - pad, y + 60 * u)
            when {
                DaoPhap.xung > 0 -> {
                    k.button(c, b, "Thu công, trả lại ngộ tính", sb)
                    hits.add(b) { DaoPhap.cancelXung(); refresh() }
                }
                DaoPhap.canXung() -> {
                    k.button(c, b, "Xung quan tầng $lock · ${DaoPhap.openCost()} ngộ tính", k.ui10Bold)
                    hits.add(b) {
                        if (DaoPhap.startXung()) toast("Đọc liền ${PhapMon.xungMinutes(DaoPhap.xung)} phút để phá quan")
                        refresh()
                    }
                }
                else -> k.button(c, b, "Thiếu ngộ tính: đọc sách tới tầng 4, 7 và đọc trọn", sb, enabled = false)
            }
        }
        y = card.bottom + 16 * u

        // The three đặc hiệu, and what the công pháp asks back.
        y = k.section(c, a.left, y, a.width(), "ĐẶC HIỆU")
        val labels = arrayOf("Tầng 1", "Tầng 5", "Tầng 9 · tuyệt học")
        for ((slot, pair) in d.effects.withIndex()) {
            val open = DaoPhap.slotOpen(id, slot)
            val text = PhapMon.effectText(pair.first, if (slot == 2 && !PhapMon.isSwitch(pair.first)) 2 else 1)
            k.check(c, a.left, y + 2 * u, sl - 4 * u, open)
            k.pair(c, labels[slot], text, a.left + sl + 6 * u, y, a.width() - sl - 6 * u, small, if (open) sb else small)
            y += sl + 6 * u
        }
        if (d.flaw != PhapMon.F_NONE) {
            k.pair(c, "Khuyết điểm", PhapMon.FLAWS[d.flaw], a.left + sl + 6 * u, y, a.width() - sl - 6 * u, small, sb)
            y += sl + 6 * u
        }
        y += 10 * u

        // The way of the line.
        y = k.section(c, a.left, y, a.width(), "LỐI VẬN CÔNG · MẠCH ${PhapMon.MACH[d.mach].uppercase()}")
        val now = DaoPhap.inWay(d.mach, Store.current())
        y = para(c, k, "${PhapMon.WAY[d.mach].replaceFirstChar { it.uppercase() }}: chữ tính ×1,5 vào tầng công pháp (không vào tu vi). " +
            (if (now) "Lúc này đang đúng lối." else "Lúc này chưa đúng lối.") + " Đã đọc đúng lối ${Realm.van(DaoPhap.wayWords)} vạn chữ.",
            a.left, y, a.width(), small, 3) + 14 * u

        // Phụ tu.
        y = k.section(c, a.left, y, a.width(), "PHỤ TU")
        val p = DaoPhap.phuDef()
        val text = when {
            !DaoPhap.phuOpen() -> "Từ Kim Đan vận thêm một bộ khác mạch: nhận đặc hiệu tầng 1 của nó, và nó lên tầng bằng nửa số chữ."
            p == null -> "Chưa đặt. Chạm một bộ ở các trang sau để đặt làm phụ tu (khác mạch với bộ đang vận)."
            else -> "${p.name} (${PhapMon.PHAM[p.pham]}, mạch ${PhapMon.MACH[p.mach]}) tầng ${DaoPhap.tang(DaoPhap.phu)}: ${PhapMon.effectText(p.effect)}."
        }
        para(c, k, text, a.left, y, a.width(), small, 3)
        return 1
    }

    /** Page 2: the speed of cultivation and what the gates ask. */
    private fun speedPage(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "TỐC ĐỘ TU LUYỆN")
        val parts = DaoPhap.speedParts() + listOfNotNull(TuLuyen.danBonus().takeIf { it > 0 }?.let { "${com.exacta.reader.data.DaoLo.form(2) ?: "Kim Đan"} ${TuLuyen.DAN_PHAM[TuLuyen.danPham].lowercase()}" to it })
        for ((label, v) in parts) {
            k.pair(c, label, if (v >= 0) "+$v%" else "−${-v}%", a.left, y, a.width())
            y += sl + 4 * u
        }
        c.drawRect(a.left, y + 2 * u, a.right, y + 3 * u, k.fill)
        y += 8 * u
        k.pair(c, "Mỗi chữ thành linh lực (trước bình cảnh, thương thế)", "${DaoPhap.speed() * (100 + TuLuyen.danBonus()) / 100}%", a.left, y, a.width(), sb, k.ui10Bold)
        y += k.lineH(k.ui10Bold) + 16 * u

        // Every đặc hiệu at work now, added up.
        val now = DaoPhap.effectsNow()
        if (now.isNotEmpty()) {
            y = k.section(c, a.left, y, a.width(), "ĐẶC HIỆU ĐANG CÓ (CỘNG DỒN)")
            for ((e, lv) in now) {
                if (y + sl > a.bottom - 6 * sl) break
                k.diamond(c, a.left + 6 * u, y + sl * 0.5f, 4 * u)
                k.text(c, k.fit(PhapMon.effectText(e, lv), a.width() - 20 * u, small), a.left + 20 * u, y, small)
                y += sl + 4 * u
            }
            y += 12 * u
        }

        y = k.section(c, a.left, y, a.width(), "PHẨM VÀ MẠCH")
        val g = TuLuyen.gate()
        val rules = listOf(
            "Hoàng phẩm tu tới Trúc Cơ, Huyền tới Kim Đan, Địa tới Nguyên Anh, Thiên tới Hóa Thần; Linh giới cần Linh phẩm, Phi Thăng cần Tiên phẩm.",
            if (Store.realmCap in 0..8 && g >= 2) "Cửa kế tiếp ${Realm.baseName(g)} cần công pháp ${PhapMon.PHAM[PhapMon.phamFor(g)]} phẩm" +
                (if (DaoPhap.canBreakWith(g)) ": đã đủ." else ": tìm ở kỳ ngộ, bí cảnh tuần, Tàng Kinh Các.") else "Cửa kế tiếp không đòi phẩm công pháp.",
            "Mười hai mạch, mỗi mạch sáu phẩm. Bộ viên mãn truyền một nửa số chữ cho bộ phẩm cao hơn cùng mạch (chuyển tu).",
        )
        for (r in rules) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl * 0.5f, 4 * u)
            y = para(c, k, r, a.left + 20 * u, y, a.width() - 20 * u, small, 3) + 6 * u
        }
        return 1
    }

    private fun others() = DaoPhap.cong.keys.filter { it != DaoPhap.active }.sortedWith(compareBy<Int>({ it != DaoPhap.phu }, { -PhapMon.CONG_PHAP[it].pham }, { it }))

    private fun ownedPer(k: Ink, a: RectF): Int {
        val top = k.lineH(k.smallBold) + 14 * k.u
        return ((a.height() - top) / (rowH(k) + 10 * k.u)).toInt().coerceAtLeast(1)
    }

    private fun ownedPages(k: Ink, a: RectF) = (others().size + ownedPer(k, a) - 1) / ownedPer(k, a)

    private fun owned(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val list = others()
        var y = k.section(c, a.left, a.top, a.width(), "CÔNG PHÁP KHÁC ĐÃ CÓ · ${list.size} BỘ")
        val per = ownedPer(k, a)
        val h = rowH(k)
        for (id in list.drop(page * per).take(per)) {
            val d = PhapMon.CONG_PHAP[id]
            val aff = DaoPhap.affinity(d.attr)
            val tang = DaoPhap.tang(id)
            val isPhu = id == DaoPhap.phu
            val line1 = "Mạch ${PhapMon.MACH[d.mach]}" + (if (aff > 0) " · hợp +20%" else if (aff < 0) " · khắc −20%" else "") +
                " · tầng $tang" + (if (isPhu) " · đang phụ tu" else "") + (if (DaoPhap.inheritFrom(id) != null) " · kế thừa được" else "")
            val line2 = d.effects.joinToString(" · ") { (e, at) -> "$at: ${PhapMon.effectText(e, if (at == 9 && !PhapMon.isSwitch(e)) 2 else 1)}" }
            row(c, k, RectF(a.left, y, a.right, y + h), d.name, "${PhapMon.PHAM[d.pham]} phẩm", line1, line2,
                "Xem", true, isPhu, hits) { congDialog(id) }
            y += h + 10 * u
        }
        return (list.size + per - 1) / per
    }

    /** A công pháp in full: run it, set it as phụ tu, or close. */
    private fun congDialog(id: Int) {
        val d = PhapMon.CONG_PHAP[id]
        val labels = arrayOf("Tầng 1", "Tầng 5", "Tầng 9 (tuyệt học)")
        val lines = d.effects.mapIndexed { i, (e, _) -> "${labels[i]}: ${PhapMon.effectText(e, if (i == 2 && !PhapMon.isSwitch(e)) 2 else 1)}" }
        val inherit = DaoPhap.inheritFrom(id)?.let { "\n\nChuyển tu: vận bộ này sẽ kế thừa ${Realm.van(DaoPhap.inheritWords(id))} vạn chữ từ ${PhapMon.CONG_PHAP[it].name}." } ?: ""
        val msg = "${PhapMon.PHAM[d.pham]} phẩm · mạch ${PhapMon.MACH[d.mach]} · tầng ${DaoPhap.tang(id)}\n\n" + lines.joinToString("\n") +
            (if (d.flaw != PhapMon.F_NONE) "\nKhuyết điểm: ${PhapMon.FLAWS[d.flaw]}" else "") +
            "\n\nLối vận công: ${PhapMon.WAY[d.mach]}." + inherit
        val b = AlertDialog.Builder(this).setTitle(d.name).setMessage(msg)
            .setPositiveButton("Vận") { _, _ -> if (DaoPhap.run(id)) toast("Đang vận ${d.name}"); refresh() }
        if (id == DaoPhap.phu) b.setNeutralButton("Bỏ phụ tu") { _, _ -> DaoPhap.stopPhu(); refresh() }
        else if (DaoPhap.canPhu(id)) b.setNeutralButton("Phụ tu") { _, _ -> if (DaoPhap.setPhu(id)) toast("Phụ tu ${d.name}"); refresh() }
        b.setNegativeButton("Đóng", null).show()
    }

    // --- Tâm pháp ------------------------------------------------------------------------------------

    private fun tamOwned() = DaoPhap.tam.keys.sortedWith(compareBy<Int>({ it !in DaoPhap.tamOn }, { PhapMon.TAM_PHAP[it].phai }, { it }))

    private fun tamPer(k: Ink, a: RectF): Int {
        val top = k.lineH(k.smallBold) + 14 * k.u
        return ((a.height() - top) / (rowH(k) + 10 * k.u)).toInt().coerceAtLeast(1)
    }

    private fun tamNext(id: Int): String {
        val c = DaoPhap.canh(id)
        return if (c >= 5) "đã tới Hóa cảnh" else "${PhapMon.CANH[c + 1]} ở ${PhapMon.CANH_AT[c + 1]}"
    }

    private fun tamPhap(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val list = tamOwned()
        val per = tamPer(k, a)
        val pages = 1 + (list.size + per - 1) / per
        if (page == 0) tamSlotsPage(c, k, a, hits) else tamList(c, k, a, list, page - 1, per, hits)
        return pages
    }

    /** Page one: the slots, what they turn by, and what goes with what. */
    private fun tamSlotsPage(c: Canvas, k: Ink, a: RectF, hits: Hits) {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val slots = DaoPhap.tamSlots()
        var y = k.section(c, a.left, a.top, a.width(), "TÂM PHÁP · ĐANG VẬN ${DaoPhap.tamOn.size}/$slots Ô")
        val more = when { slots < 1 -> "Ô đầu mở ở Trúc Cơ. "; slots < 2 -> "Ô thứ hai mở ở Nguyên Anh. "; slots < 3 -> "Ô thứ ba mở ở Luyện Hư. "; else -> "" }
        y = para(c, k, more + "Buổi đọc liền 30 phút là một lần vận chuyển, đúng lối của bộ thì tính hai. " +
            "Cảnh: Nhập môn 5 · Tiểu thành 15 · Đại thành 30 · Viên mãn 60 · Hóa cảnh 100.",
            a.left, y, a.width(), small, 3) + 12 * u
        val h = rowH(k)
        if (slots == 0) {
            y = para(c, k, "Chưa mở ô nào. Phá Trúc Cơ được sư phụ truyền Thanh Tâm Quyết.", a.left, y, a.width(), small, 2) + 12 * u
        }
        for (i in 0 until slots) {
            val r = RectF(a.left, y, a.right, y + h)
            val id = DaoPhap.tamOn.getOrNull(i)
            if (id == null) {
                row(c, k, r, "Ô thứ ${i + 1} còn trống", "", "Chọn một bộ ở trang sau.",
                    if (DaoPhap.tam.isEmpty()) "Ngọc giản tâm pháp rơi từ kỳ ngộ, bí cảnh tuần, Tàng Kinh Các." else "Bấm «Xem» ở một bộ rồi «Vận».", null, false, false, hits) {}
            } else {
                val t = PhapMon.TAM_PHAP[id]
                val way = DaoPhap.inTamWay(id, null)
                row(c, k, r, t.name, PhapMon.CANH[DaoPhap.canh(id)], "${PhapMon.PHAI[t.phai]} · ${DaoPhap.turns(id)} lần vận chuyển · ${tamNext(id)}",
                    "Lối: ${PhapMon.TAM_WAY[t.way]}" + if (way) " (đang đúng lối)" else "", "Xem", true, true, hits) { tamDialog(id) }
            }
            y += h + 10 * u
        }
        // What goes with what.
        y += 6 * u
        y = k.section(c, a.left, y, a.width(), "TƯƠNG SINH · XUNG KHẮC")
        val notes = ArrayList<String>()
        notes.add("Đạo gia đi cùng Phật môn: cả hai vận chuyển nhanh ${PhapMon.DAO_PHAT_PERCENT}%" + if (DaoPhap.daoPhat()) " (đang có)." else ".")
        notes.add("Ma đạo không vận cùng Phật môn, Nho môn; mỗi bộ Ma đạo đang vận làm tâm ma tăng nhanh ${PhapMon.MA_STIR}%" +
            if (DaoPhap.maCount() > 0) " (đang ${PhapMon.MA_STIR * DaoPhap.maCount()}%)." else ".")
        for (n in notes) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl * 0.5f, 4 * u)
            y = para(c, k, n, a.left + 20 * u, y, a.width() - 20 * u, small, 2) + 6 * u
        }
        // What the tâm pháp give now.
        val now = (1..PhapMon.E_BI_CANH).mapNotNull { e -> DaoPhap.tamLv(e).takeIf { it > 0 }?.let { e to it } }
        val traits = DaoPhap.tamOn.filter { DaoPhap.canh(it) >= 1 && PhapMon.TAM_PHAP[it].trait.isNotEmpty() }
        if ((now.isNotEmpty() || traits.isNotEmpty()) && y + 3 * sl < a.bottom) {
            y += 10 * u
            y = k.section(c, a.left, y, a.width(), "TÂM PHÁP ĐANG CHO")
            val lines = traits.map { PhapMon.TAM_PHAP[it].trait } + now.map { (e, lv) -> PhapMon.effectText(e, lv) }
            para(c, k, lines.joinToString(" · "), a.left, y, a.width(), small, ((a.bottom - y) / sl).toInt().coerceIn(1, 4))
        }
    }

    private fun tamList(c: Canvas, k: Ink, a: RectF, list: List<Int>, page: Int, per: Int, hits: Hits) {
        val u = k.u
        var y = k.section(c, a.left, a.top, a.width(), "TÂM PHÁP ĐÃ CÓ · ${list.size}/${PhapMon.TAM_PHAP.size} BỘ")
        val h = rowH(k)
        for (id in list.drop(page * per).take(per)) {
            val t = PhapMon.TAM_PHAP[id]
            val on = id in DaoPhap.tamOn
            val clash = if (on) null else DaoPhap.clashWith(id)
            val line1 = "${PhapMon.CANH[DaoPhap.canh(id)]} · ${DaoPhap.turns(id)} lần vận chuyển" +
                (if (on) " · đang vận" else if (clash != null) " · khắc ${PhapMon.TAM_PHAP[clash].name}" else "")
            row(c, k, RectF(a.left, y, a.right, y + h), t.name, PhapMon.PHAI[t.phai], line1, "Lối: ${PhapMon.TAM_WAY[t.way]}",
                "Xem", true, on, hits) { tamDialog(id) }
            y += h + 10 * u
        }
    }

    /** A tâm pháp in full: run it, stop it, or close. */
    private fun tamDialog(id: Int) {
        val t = PhapMon.TAM_PHAP[id]
        val cc = DaoPhap.canh(id)
        val on = id in DaoPhap.tamOn
        val clash = if (on) null else DaoPhap.clashWith(id)
        val way = if (t.way == PhapMon.W_FINISH) "đọc trọn một cuốn tính ${PhapMon.FINISH_TURNS} lần vận chuyển."
            else "${PhapMon.TAM_WAY[t.way]}: buổi đó tính hai lần."
        val msg = buildString {
            append("${PhapMon.PHAI[t.phai]} · ${PhapMon.CANH[cc]} · ${DaoPhap.turns(id)} lần vận chuyển")
            if (cc < 5) append(" · ${tamNext(id)}")
            append("\n\nLối vận chuyển: $way")
            if (t.trait.isNotEmpty()) append("\n\nĐặc chất (từ Nhập môn): ${t.trait}.")
            append("\n\nNhập môn, Tiểu thành, Viên mãn: ${PhapMon.effectSteps(t.e1, 1, 2, 3)}.")
            append("\nĐại thành mở, Hóa cảnh gấp đôi: ${PhapMon.effectSteps(t.e2, 1, 2)}.")
            append("\nĐại thành: tâm cảnh +5. Hóa cảnh: tâm cảnh lên một bậc.")
            append("\n\n${PhapMon.PHAI[t.phai]}: ${PhapMon.PHAI_TEXT[t.phai]}.")
            if (clash != null) append("\n\nKhông vận được cùng ${PhapMon.TAM_PHAP[clash].name} đang vận.")
            else if (!on && DaoPhap.tamOn.size >= DaoPhap.tamSlots()) append("\n\nHết ô: thôi vận một bộ trước.")
        }
        val b = AlertDialog.Builder(this).setTitle(t.name).setMessage(msg)
        if (on) b.setPositiveButton("Thôi vận") { _, _ -> DaoPhap.stopTam(id); refresh() }
        else if (clash == null && DaoPhap.tamOn.size < DaoPhap.tamSlots()) b.setPositiveButton("Vận") { _, _ -> if (DaoPhap.runTam(id)) toast("Đang vận ${t.name}"); refresh() }
        b.setNegativeButton("Đóng", null).show()
    }

    // --- Thuật pháp ----------------------------------------------------------------------------------

    /** What a thuật does at its thuần thục now. */
    private fun thuatText(id: Int): String {
        val m = DaoPhap.mastery(id)
        val t = PhapMon.THUAT_PHAP[id]
        return when {
            id == PhapMon.T_THO_DON -> "Đột phá hỏng: hụt khí chỉ mất 1%, kinh mạch chỉ ${if (m >= 2) 2 else 3} ngày"
            id == PhapMon.T_PHONG_DON -> "Đột phá hỏng: hồi khí chỉ ${if (m >= 2) 0 else 1} ngày"
            id == PhapMon.T_HUYET_DON -> "Đột phá hỏng: tẩu hỏa nhập ma thành kinh mạch tổn thương ${if (m >= 2) 5 else 8} ngày"
            id == PhapMon.T_THIEN_NHAN -> "Lôi kiếp: chặn ${if (m >= 3) 2 else 1} lần tâm ma; bí cảnh: nhìn thấu trận văn"
            id == PhapMon.T_PHAP_TUONG -> "Tỉ lệ đột phá +${5 + m}%"
            t.kind == PhapMon.K_CONG_KICH -> "Bí cảnh: diệt yêu thú, trúng ${if (DaoPhap.hop(PhapMon.KIM)) 100 else PhapMon.hitChance(m)}%, ${10 * (m + 1) * (if (DaoPhap.hop(PhapMon.HOA)) 2 else 1)} linh thạch"
            t.kind == PhapMon.K_KHONG_CHE -> "Bí cảnh: trói yêu thú lấy thưởng; trấn áp chủ bí cảnh +${15 + 5 * m}%"
            t.kind == PhapMon.K_PHONG_NGU -> "Kiếp đang độ: lỡ ${if (m >= 3) 2 else 1} ngày vẫn không sao"
            else -> t.text
        }
    }

    /** Ready, waiting, or always at work. */
    private fun thuatState(id: Int): String {
        val st = DaoPhap.thuat[id] ?: return ""
        if (!st.learned) return "chưa học"
        val cd = DaoPhap.cd(id)
        return when {
            cd == 0 -> "luôn có tác dụng"
            st.cool > 0 -> "hồi chiêu còn ${st.cool} ngày đọc"
            else -> "sẵn sàng · hồi chiêu $cd ngày đọc"
        }
    }

    private fun thuatOwned() = DaoPhap.thuat.keys.sortedWith(compareBy<Int>({ !DaoPhap.on(it) }, { DaoPhap.thuat[it]?.learned != true }, { PhapMon.THUAT_PHAP[it].kind }, { it }))

    private fun thuatPhap(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val list = thuatOwned()
        val per = tamPer(k, a)
        val pages = 1 + (list.size + per - 1) / per
        if (page == 0) thuatSlotsPage(c, k, a, hits) else thuatList(c, k, a, list, page - 1, per, hits)
        return pages
    }

    private fun thuatSlotsPage(c: Canvas, k: Ink, a: RectF, hits: Hits) {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val slots = DaoPhap.thuatSlots()
        var y = k.section(c, a.left, a.top, a.width(), "THUẬT PHÁP · MANG ${DaoPhap.thuatOn.size}/$slots Ô")
        y = para(c, k, "Thuật đang mang được thi triển khi biến cố tới (bí cảnh, kiếp, đột phá hỏng), rồi chờ hồi chiêu tính bằng ngày đọc. " +
            "Thuần thục lên theo lần dùng và buổi đọc 30 phút lúc đang mang. Thêm một ô mỗi đại cảnh giới.", a.left, y, a.width(), small, 3) + 12 * u
        val h = rowH(k)
        val carried = DaoPhap.thuatOn.filter { DaoPhap.on(it) }
        // Leave room for hợp kích and bản mệnh below.
        val room = ((a.bottom - y - 6 * sl - 40 * u) / (h + 10 * u)).toInt().coerceAtLeast(1)
        if (carried.isEmpty()) {
            y = para(c, k, "Chưa mang thuật nào. Linh căn thức tỉnh được truyền một thuật hợp chủ hành; ngọc giản rơi từ kỳ ngộ, bí cảnh tuần, Tàng Kinh Các.", a.left, y, a.width(), small, 3) + 12 * u
        }
        for (id in carried.take(room)) {
            val t = PhapMon.THUAT_PHAP[id]
            row(c, k, RectF(a.left, y, a.right, y + h), t.name, PhapMon.MASTERY[DaoPhap.mastery(id)],
                "${PhapMon.attrName(t.attr)} · ${PhapMon.KINDS[t.kind]} · ${thuatState(id)}", thuatText(id), "Xem", true, true, hits) { thuatDialog(id) }
            y += h + 10 * u
        }
        if (carried.size > room) {
            y = para(c, k, "Và ${carried.size - room} thuật nữa ở trang sau.", a.left, y, a.width(), small, 1) + 8 * u
        }
        y += 4 * u
        y = k.section(c, a.left, y, a.width(), "HỢP KÍCH · BẢN MỆNH THẦN THÔNG")
        val notes = ArrayList<String>()
        val hops = DaoPhap.hopActive()
        if (hops.isEmpty()) notes.add("Mang hai thuật cùng hành thì mở hợp kích: Kim chắc trúng, Hỏa gấp đôi linh thạch, Thủy chặn thêm tâm ma, Phong hồi chiêu nhanh…")
        else hops.forEach { notes.add("${it.name} (${PhapMon.attrName(it.attr)}): ${it.text}") }
        val bm = DaoPhap.banMenh()
        notes.add(if (bm != null) "Bản mệnh ${bm.name}: ${PhapMon.effectText(bm.effect, bm.level)}"
            else "Bản mệnh thần thông mở khi kết Kim Đan: mỗi loại Kim Đan một thần thông riêng.")
        for (n in notes) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl * 0.5f, 4 * u)
            y = para(c, k, n, a.left + 20 * u, y, a.width() - 20 * u, small, 2) + 6 * u
        }
    }

    private fun thuatList(c: Canvas, k: Ink, a: RectF, list: List<Int>, page: Int, per: Int, hits: Hits) {
        val u = k.u
        var y = k.section(c, a.left, a.top, a.width(), "THUẬT PHÁP ĐÃ CÓ · ${list.size}/${PhapMon.THUAT_PHAP.size}")
        val h = rowH(k)
        for (id in list.drop(page * per).take(per)) {
            val t = PhapMon.THUAT_PHAP[id]
            val on = DaoPhap.on(id)
            row(c, k, RectF(a.left, y, a.right, y + h), t.name, if (on) "đang mang" else PhapMon.KINDS[t.kind],
                "${PhapMon.attrName(t.attr)} · ${if (DaoPhap.thuat[id]?.learned == true) PhapMon.MASTERY[DaoPhap.mastery(id)] + " · " else ""}${thuatState(id)}",
                thuatText(id), "Xem", true, on, hits) { thuatDialog(id) }
            y += h + 10 * u
        }
    }

    /** A thuật in full: learn it, carry it, put it away. */
    private fun thuatDialog(id: Int) {
        val t = PhapMon.THUAT_PHAP[id]
        val st = DaoPhap.thuat[id] ?: return
        val on = DaoPhap.on(id)
        val m = DaoPhap.mastery(id)
        val next = if (m < 4) " · ${PhapMon.MASTERY[m + 1]} ở ${PhapMon.MASTERY_AT[m + 1]} điểm" else ""
        val hop = PhapMon.HOP_KICH.firstOrNull { it.attr == t.attr }
        val msg = buildString {
            append("${PhapMon.attrName(t.attr)} · ${PhapMon.KINDS[t.kind]}")
            if (st.learned) append(" · ${PhapMon.MASTERY[m]} (${DaoPhap.points(id)} điểm$next)")
            append("\n\n${thuatText(id)}.")
            append("\n\n${thuatState(id).replaceFirstChar { it.uppercase() }}.")
            append("\n\nThuần thục: mỗi lần dùng 3 điểm, mỗi buổi đọc 30 phút lúc đang mang 1 điểm. Mỗi bậc thuật trúng dễ hơn, hai bậc hồi chiêu bớt một ngày.")
            if (hop != null) append("\n\nHợp kích ${hop.name}: mang thêm một thuật ${PhapMon.attrName(t.attr)} nữa thì ${hop.text}.")
            if (!st.learned) {
                val cost = DaoPhap.learnCost(id)
                append("\n\n" + if (!DaoPhap.canLearnRealm(id)) "Thần thông: từ Nguyên Anh mới học được." else if (cost == 0) "Hợp linh căn: học không tốn ngộ tính." else "Học tốn $cost ngộ tính (đang có ${DaoPhap.ngo}).")
            } else if (!on && DaoPhap.thuatOn.size >= DaoPhap.thuatSlots()) append("\n\nHết ô: cất một thuật trước.")
        }
        val b = AlertDialog.Builder(this).setTitle(t.name).setMessage(msg)
        when {
            !st.learned -> if (DaoPhap.canLearn(id)) b.setPositiveButton("Học") { _, _ -> if (DaoPhap.learn(id)) toast("Học được ${t.name}"); refresh() }
            on -> b.setPositiveButton("Cất") { _, _ -> DaoPhap.unequip(id); refresh() }
            DaoPhap.thuatOn.size < DaoPhap.thuatSlots() -> b.setPositiveButton("Mang") { _, _ -> DaoPhap.equip(id); refresh() }
        }
        b.setNegativeButton("Đóng", null).show()
    }

    // --- Tàng Kinh Các -------------------------------------------------------------------------------

    private fun tangKinh(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val offers = DaoPhap.weekOffers()
        var y = k.section(c, a.left, a.top, a.width(), "TÀNG KINH CÁC · ${Store.linhThach} LINH THẠCH")
        y = para(c, k, "Mỗi thứ Hai kho đổi ngọc giản mới, luôn có một bộ công pháp đúng phẩm cửa kế tiếp cần. Ngọc giản còn rơi từ kỳ ngộ (tàn hồn truyền thừa) và bí cảnh tuần.",
            a.left, y, a.width(), small, 2) + 10 * u
        val h = 24 * u + k.lineH(k.ui10Bold) + 2 * u + k.lineH(small)
        val per = ((a.bottom - y) / (h + 8 * u)).toInt().coerceAtLeast(1)
        for ((kind, id) in offers.drop(page * per).take(per)) {
            val have = DaoPhap.owns(kind, id)
            val price = DaoPhap.price(kind, id)
            val (title, tag, line) = when (kind) {
                PhapMon.CONG -> PhapMon.CONG_PHAP[id].let {
                    val aff = DaoPhap.affinity(it.attr)
                    Triple(it.name, "Công pháp ${PhapMon.PHAM[it.pham]}", "Mạch ${PhapMon.MACH[it.mach]}" +
                        (if (aff > 0) " · hợp" else if (aff < 0) " · khắc" else "") + " · ${PhapMon.effectText(it.effect)}")
                }
                PhapMon.TAM -> PhapMon.TAM_PHAP[id].let { Triple(it.name, "Tâm pháp", "${PhapMon.PHAI[it.phai]} · ${it.text}") }
                PhapMon.DAN -> com.exacta.reader.game.DanDao.RECIPES[id].let {
                    Triple(it.name, "Đan phương", "${it.text} · cần ${com.exacta.reader.game.DanDao.CAP[it.cap - 1].lowercase()}")
                }
                else -> PhapMon.THUAT_PHAP[id].let { Triple(it.name, PhapMon.KINDS[it.kind], "${PhapMon.attrName(it.attr)} · ${it.text}") }
            }
            val r = RectF(a.left, y, a.right, y + h)
            val pad = 12 * u
            k.card(c, r)
            val bw = 170 * u
            val tw = r.width() - 3 * pad - bw
            val tagW = k.smallBold.measureText(tag) + 12 * u
            k.text(c, k.fit(title, tw - tagW, k.ui10Bold), r.left + pad, r.top + pad, k.ui10Bold)
            k.right(c, tag, r.left + pad + tw, r.top + pad + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
            k.line(c, line, r.left + pad, r.top + pad + k.lineH(k.ui10Bold) + 2 * u, tw, small)
            val b = RectF(r.right - pad - bw, r.centerY() - 22 * u, r.right - pad, r.centerY() + 22 * u)
            val ok = !have && Store.linhThach >= price
            k.button(c, b, if (have) "Đã có" else "Đổi · $price", k.smallBold, enabled = ok)
            if (!have) hits.add(b) { buyDialog(kind, id, title, price, line) }
            y += h + 8 * u
        }
        return (offers.size + per - 1) / per
    }

    private fun buyDialog(kind: Int, id: Int, title: String, price: Long, text: String) {
        if (Store.linhThach < price) {
            toast("Cần $price linh thạch, mới có ${Store.linhThach}")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Đổi $title?")
            .setMessage("$text\n\nGiá $price linh thạch · đang có ${Store.linhThach}.")
            .setPositiveButton("Đổi") { _, _ ->
                if (DaoPhap.buy(kind, id)) toast("Đã nhận ngọc giản $title")
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }
}

/** Hours read, short. */
private object HomeHours {
    fun text(seconds: Long): String {
        val h = seconds / 3600
        val m = seconds % 3600 / 60
        return if (h > 0) "$h giờ ${m} phút" else "$m phút"
    }
}
