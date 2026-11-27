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
        1 -> if (page == 0) congPhap(c, k, area, hits) else owned(c, k, area, page - 1, hits) + 1
        2 -> tamPhap(c, k, area, page, hits)
        3 -> thuatPhap(c, k, area, page, hits)
        else -> tangKinh(c, k, area, page, hits)
    }.let { if (tab == 1) maxOf(it, 1 + ownedPages(k, area)) else it }

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
        val reach = PhapMon.REACH[d.pham].let { if (it >= 9) "chạm Tiên cảnh" else "${TuLuyen.shortName(it)} đại viên mãn" }
        val status = when {
            DaoPhap.complete() -> "Tầng 9 viên mãn: công pháp đã luyện tới cùng."
            DaoPhap.waiting() -> "Đủ chữ: lĩnh ngộ tầng ${DaoPhap.nextLock()} cần ${DaoPhap.openCost()} ngộ tính (đang có ${DaoPhap.ngo})."
            else -> "Tầng $tang · còn ${Realm.van(DaoPhap.toNext())} vạn chữ tới tầng ${tang + 1}" +
                (DaoPhap.nextLock()?.let { " · tầng $it cần lĩnh ngộ" } ?: "")
        }
        val statusLines = k.wrap(status, a.width() - 2 * pad, small, 2).size
        val cardH = 2 * pad + k.lineH(k.ui12Bold) + 4 * u + 2 * sl + 16 * u + 36 * u + 10 * u + statusLines * sl + (if (DaoPhap.waiting()) 64 * u else 0f)
        val card = RectF(a.left, y, a.right, y + cardH)
        k.card(c, card, selected = false)
        val x = a.left + pad
        val w = a.width() - 2 * pad
        y += pad
        k.text(c, k.fit(d.name, w - 140 * u, k.ui12Bold), x, y, k.ui12Bold)
        k.right(c, "${PhapMon.PHAM[d.pham]} phẩm", card.right - pad, y + (k.lineH(k.ui12Bold) - k.lineH(sb)) / 2, sb)
        y += k.lineH(k.ui12Bold) + 4 * u
        k.text(c, k.fit("Thuộc tính ${PhapMon.attrName(d.attr)}$affText · tu tới $reach", w, small), x, y, small)
        y += sl
        k.text(c, k.fit("Đặc hiệu: ${PhapMon.EFFECTS[d.effect]}", w, sb), x, y, sb)
        y += sl + 16 * u

        // Nine tầng; 4, 7, 9 wait for ngộ tính.
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
            k.centered(c, if (locked) "ngộ" else "$i", cx, cw, y + 26 * u, if (i == tang) sb else small)
        }
        y += 36 * u + 10 * u
        y = para(c, k, status, x, y, w, small, 2)
        if (DaoPhap.waiting()) {
            val b = RectF(x, y + 10 * u, card.right - pad, y + 60 * u)
            val ok = DaoPhap.ngo >= DaoPhap.openCost()
            k.button(c, b, if (ok) "Lĩnh ngộ tầng ${DaoPhap.nextLock()} · ${DaoPhap.openCost()} ngộ tính" else "Thiếu ngộ tính: đọc sách tới tầng 4, 7 và đọc trọn", sb, enabled = ok)
            if (ok) hits.add(b) {
                if (DaoPhap.open()) toast("${d.name} lên tầng ${DaoPhap.tang()}")
                refresh()
            }
        }
        y = card.bottom + 18 * u

        // The speed of cultivation.
        y = k.section(c, a.left, y, a.width(), "TỐC ĐỘ TU LUYỆN")
        val parts = DaoPhap.speedParts() + listOfNotNull(TuLuyen.danBonus().takeIf { it > 0 }?.let { "${com.exacta.reader.data.DaoLo.form(2) ?: "Kim Đan"} ${TuLuyen.DAN_PHAM[TuLuyen.danPham].lowercase()}" to it })
        for ((label, v) in parts) {
            k.pair(c, label, if (v >= 0) "+$v%" else "−${-v}%", a.left, y, a.width())
            y += sl + 4 * u
        }
        c.drawRect(a.left, y + 2 * u, a.right, y + 3 * u, k.fill)
        y += 8 * u
        k.pair(c, "Mỗi chữ thành linh lực (trước bình cảnh, thương thế)", "${DaoPhap.speed() * (100 + TuLuyen.danBonus()) / 100}%", a.left, y, a.width(), sb, k.ui10Bold)
        y += k.lineH(k.ui10Bold) + 16 * u

        // What the gates ask.
        y = k.section(c, a.left, y, a.width(), "PHẨM CÔNG PHÁP")
        val g = TuLuyen.gate()
        val rules = listOf(
            "Hoàng phẩm tu tới Trúc Cơ, Huyền tới Kim Đan, Địa tới Nguyên Anh, Thiên tới Hóa Thần; Linh giới cần Linh phẩm, Phi Thăng cần Tiên phẩm.",
            if (Store.realmCap in 0..8 && g >= 2) "Cửa kế tiếp ${Realm.baseName(g)} cần công pháp ${PhapMon.PHAM[PhapMon.phamFor(g)]} phẩm" +
                (if (DaoPhap.canBreakWith(g)) ": đã đủ." else ": tìm ở kỳ ngộ, bí cảnh tuần, Tàng Kinh Các.") else "Cửa kế tiếp không đòi phẩm công pháp.",
            "Đổi công pháp không mất tầng: mỗi bộ giữ chữ đã luyện." + if (others().isNotEmpty()) " Trang sau là các bộ khác đã có." else "",
        )
        for (r in rules) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl * 0.5f, 4 * u)
            y = para(c, k, r, a.left + 20 * u, y, a.width() - 20 * u, small, 3) + 6 * u
        }
        return 1
    }

    private fun others() = DaoPhap.cong.keys.filter { it != DaoPhap.active }.sortedWith(compareBy<Int>({ -PhapMon.CONG_PHAP[it].pham }, { it }))

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
            val on = id == DaoPhap.active
            val line1 = "Thuộc tính ${PhapMon.attrName(d.attr)}" + (if (aff > 0) " · hợp +20%" else if (aff < 0) " · khắc −20%" else "") +
                " · tầng $tang" + (if (DaoPhap.waiting(id)) " (chờ lĩnh ngộ)" else "") + " · linh lực +${PhapMon.PHAM_BONUS[d.pham] + tang - 1 + aff}%"
            row(c, k, RectF(a.left, y, a.right, y + h), d.name, "${PhapMon.PHAM[d.pham]} phẩm", line1, PhapMon.EFFECTS[d.effect],
                if (on) "Đang vận" else "Vận", !on, on, hits) { DaoPhap.run(id) }
            y += h + 10 * u
        }
        return (list.size + per - 1) / per
    }

    // --- Tâm pháp ------------------------------------------------------------------------------------

    private fun tamPhap(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val slots = DaoPhap.tamSlots()
        var y = k.section(c, a.left, a.top, a.width(), "TÂM PHÁP · ĐANG VẬN ${DaoPhap.tamOn.size}/$slots Ô")
        y = para(c, k, (if (slots == 0) "Ô tâm pháp mở ở Trúc Cơ (một ô), thêm ô thứ hai ở Nguyên Anh. " else if (slots == 1) "Thêm ô thứ hai ở Nguyên Anh. " else "") +
            "Mỗi buổi đọc liền từ 30 phút là một lần vận chuyển cho mọi tâm pháp đang vận: ${PhapMon.TAM_NHAP_MON} lần là nhập môn, hiệu quả bắt đầu; ${PhapMon.TAM_DAI_THANH} lần là đại thành, tâm cảnh +5.",
            a.left, y, a.width(), small, 3) + 12 * u
        val list = DaoPhap.tam.keys.sortedWith(compareBy<Int>({ it !in DaoPhap.tamOn }, { it }))
        if (list.isEmpty()) {
            para(c, k, "Chưa có tâm pháp. Ngọc giản tâm pháp rơi từ kỳ ngộ, bí cảnh tuần, hoặc đổi ở Tàng Kinh Các. Phá Trúc Cơ được truyền Thanh Tâm Quyết.", a.left, y, a.width(), small, 3)
            return 1
        }
        val h = rowH(k)
        val per = ((a.bottom - y) / (h + 10 * u)).toInt().coerceAtLeast(1)
        for (id in list.drop(page * per).take(per)) {
            val t = PhapMon.TAM_PHAP[id]
            val n = DaoPhap.tam[id] ?: 0
            val on = id in DaoPhap.tamOn
            val level = listOf("chưa nhập môn", "nhập môn", "đại thành")[DaoPhap.tamLevel(id)]
            val next = when {
                n < PhapMon.TAM_NHAP_MON -> " · nhập môn ở ${PhapMon.TAM_NHAP_MON}"
                n < PhapMon.TAM_DAI_THANH -> " · đại thành ở ${PhapMon.TAM_DAI_THANH}"
                else -> ""
            }
            val btn = if (on) "Thôi vận" else if (DaoPhap.tamOn.size < slots) "Vận" else "Hết ô"
            row(c, k, RectF(a.left, y, a.right, y + h), t.name, if (on) "đang vận" else "", "$n lần vận chuyển · $level$next", t.text,
                btn, on || DaoPhap.tamOn.size < slots, on, hits) { if (on) DaoPhap.stopTam(id) else DaoPhap.runTam(id) }
            y += h + 10 * u
        }
        return (list.size + per - 1) / per
    }

    // --- Thuật pháp ----------------------------------------------------------------------------------

    private fun thuatText(id: Int): String {
        val m = DaoPhap.mastery(id)
        return when (id) {
            PhapMon.T_HOA_DAN, PhapMon.T_NGU_KIEM, PhapMon.T_CHUONG_LOI, PhapMon.T_HAN_BANG -> "Qua bí cảnh tuần: thêm ${10 * (m + 1)} linh thạch"
            PhapMon.T_KIM_CANG, PhapMon.T_THUY_MAC, PhapMon.T_MOC_GIAP -> "Kiếp đang độ (ngưng đan, lôi kiếp, thiên kiếp): lỡ ${if (m >= 3) 2 else 1} ngày vẫn không sao"
            PhapMon.T_THO_DON -> "Đột phá hỏng: hụt khí chỉ mất 1%, kinh mạch chỉ ${if (m >= 2) 2 else 3} ngày"
            PhapMon.T_PHONG_DON -> "Đột phá hỏng: hồi khí chỉ ${if (m >= 2) 0 else 1} ngày"
            PhapMon.T_HUYET_DON -> "Đột phá hỏng: tẩu hỏa nhập ma thành kinh mạch tổn thương ${if (m >= 2) 5 else 8} ngày"
            PhapMon.T_THIEN_NHAN -> "Lôi kiếp: chặn ${if (m >= 3) 2 else 1} lần tâm ma tấn công"
            PhapMon.T_PHAP_TUONG -> "Tỉ lệ đột phá +${5 + m}%"
            else -> PhapMon.THUAT_PHAP[id].text
        }
    }

    private fun thuatPhap(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val slots = DaoPhap.thuatSlots()
        var y = k.section(c, a.left, a.top, a.width(), "THUẬT PHÁP · MANG ${DaoPhap.thuatOn.size}/$slots Ô")
        y = para(c, k, "Thuật pháp đang mang tự ra tay khi biến cố tới; mỗi lần dùng thêm thuần thục: Sơ Học, Tiểu Thành (3), Đại Thành (8), Viên Mãn (15), Hóa Cảnh (25). " +
            "Hợp linh căn thì học không tốn ngộ tính. Thêm một ô mỗi đại cảnh giới.", a.left, y, a.width(), small, 3) + 12 * u
        val list = DaoPhap.thuat.keys.sortedWith(compareBy<Int>({ !DaoPhap.on(it) }, { DaoPhap.thuat[it]?.learned != true }, { it }))
        if (list.isEmpty()) {
            para(c, k, "Chưa có thuật pháp. Linh căn thức tỉnh được truyền một thuật hợp chủ hành; ngọc giản rơi từ kỳ ngộ, bí cảnh tuần, Tàng Kinh Các.", a.left, y, a.width(), small, 3)
            return 1
        }
        val h = rowH(k)
        val per = ((a.bottom - y) / (h + 10 * u)).toInt().coerceAtLeast(1)
        for (id in list.drop(page * per).take(per)) {
            val t = PhapMon.THUAT_PHAP[id]
            val st = DaoPhap.thuat[id] ?: continue
            val on = DaoPhap.on(id)
            val cost = DaoPhap.learnCost(id)
            val line1 = "${PhapMon.attrName(t.attr)} · ${PhapMon.KINDS[t.kind]} · " +
                if (st.learned) "${PhapMon.MASTERY[DaoPhap.mastery(id)]} (${st.uses} lần dùng)" else "chưa học"
            val (btn, ok) = when {
                !st.learned && !DaoPhap.canLearnRealm(id) -> "Nguyên Anh mới học" to false
                !st.learned -> (if (cost == 0) "Học" else "Học · $cost ngộ") to DaoPhap.canLearn(id)
                on -> "Cất" to true
                DaoPhap.thuatOn.size < slots -> "Mang" to true
                else -> "Hết ô" to false
            }
            row(c, k, RectF(a.left, y, a.right, y + h), t.name, if (on) "đang mang" else "", line1, thuatText(id), btn, ok, on, hits) {
                when {
                    !st.learned -> DaoPhap.learn(id)
                    on -> DaoPhap.unequip(id)
                    else -> DaoPhap.equip(id)
                }
            }
            y += h + 10 * u
        }
        return (list.size + per - 1) / per
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
                    Triple(it.name, "Công pháp ${PhapMon.PHAM[it.pham]}", "${PhapMon.attrName(it.attr)}" +
                        (if (aff > 0) " · hợp" else if (aff < 0) " · khắc" else "") + " · ${PhapMon.EFFECTS[it.effect]}")
                }
                PhapMon.TAM -> PhapMon.TAM_PHAP[id].let { Triple(it.name, "Tâm pháp", it.text) }
                PhapMon.DAN -> com.exacta.reader.game.DanDao.RECIPES[id].let {
                    Triple(it.name, "Đan phương", "${com.exacta.reader.game.DanDao.GROUPS[it.group]} · ${it.text} · cần ${com.exacta.reader.game.DanDao.CAP[it.cap - 1].lowercase()}")
                }
                else -> PhapMon.THUAT_PHAP[id].let { Triple(it.name, "Thuật pháp", "${PhapMon.attrName(it.attr)} · ${PhapMon.KINDS[it.kind]} · ${it.text}") }
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
