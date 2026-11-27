package com.exacta.reader

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.Store
import com.exacta.reader.game.DanDao
import com.exacta.reader.game.DanDao.HERBS
import com.exacta.reader.game.DanDao.RECIPES
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity

/**
 * 2.2 Động phủ, in tabs: the linh mạch and the rooms of the cave, the dược viên, the đan phòng,
 * the pills made, and the tĩnh thất for bế quan.
 */
class DongPhuActivity : TabActivity() {
    override fun title() = "Động Phủ"
    override fun right() = "${Store.linhThach} linh thạch"
    override fun tabs() = listOf("Linh mạch", "Dược viên", "Đan phòng", "Đan dược", "Tĩnh thất")

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> linhMach(c, k, area, hits)
        1 -> duocVien(c, k, area, hits)
        2 -> danPhong(c, k, area, page, hits)
        3 -> danDuoc(c, k, area, page, hits)
        else -> tinhThat(c, k, area, hits)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun para(c: Canvas, k: Ink, s: String, x: Float, y: Float, w: Float, p: Paint, max: Int = 3): Float {
        var yy = y
        for (l in k.wrap(s, w, p, max)) {
            k.text(c, l, x, yy, p)
            yy += k.lineH(p)
        }
        return yy
    }

    private fun rowH(k: Ink) = 2 * 14 * k.u + k.lineH(k.ui10Bold) + 2 * k.u + 2 * k.lineH(k.small)

    /** A card: a title, a tag, two lines, a button on the right. */
    private fun row(c: Canvas, k: Ink, r: RectF, title: String, tag: String, line1: String, line2: String,
                    button: String?, enabled: Boolean, hits: Hits, onTap: () -> Unit) {
        val u = k.u
        val pad = 14 * u
        k.card(c, r)
        val bw = if (button != null) 180 * u else 0f
        val tw = r.width() - 2 * pad - (if (bw > 0) bw + pad else 0f)
        var y = r.top + pad
        val tagW = if (tag.isNotEmpty()) k.smallBold.measureText(tag) + 12 * u else 0f
        k.text(c, k.fit(title, tw - tagW, k.ui10Bold), r.left + pad, y, k.ui10Bold)
        if (tag.isNotEmpty()) k.right(c, tag, r.left + pad + tw, y + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
        y += k.lineH(k.ui10Bold) + 2 * u
        k.text(c, k.fit(line1, tw, k.small), r.left + pad, y, k.small)
        y += k.lineH(k.small)
        k.text(c, k.fit(line2, tw, k.small), r.left + pad, y, k.small)
        if (button != null) {
            val b = RectF(r.right - pad - bw, r.centerY() - 24 * u, r.right - pad, r.centerY() + 24 * u)
            k.button(c, b, button, k.smallBold, enabled = enabled)
            if (enabled) hits.add(b) { onTap() }
        }
    }

    // --- Linh mạch -------------------------------------------------------------------------------------

    private fun linhMach(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        val icon = 64 * u
        val head = RectF(a.left, a.top, a.right, a.top + 2 * pad + icon + 74 * u)
        k.card(c, head)
        k.icon(c, "dongphu", head.left + pad, head.top + pad, icon)
        val tx = head.left + 2 * pad + icon
        val tw = head.right - pad - tx
        k.text(c, k.fit("Linh mạch ${DongPhu.giaiName()}", tw, k.ui12Bold), tx, head.top + pad, k.ui12Bold)
        para(c, k, "Linh lực +${DongPhu.speedBonus()}% · đột phá +${DongPhu.breakBonus()}% · dược linh ${DongPhu.yearsADay()} năm mỗi ngày đọc 30 phút · ${DongPhu.open()}/6 luống",
            tx, head.top + pad + k.lineH(k.ui12Bold) + 4 * u, tw, small, 2)
        // Nine giai.
        val gy = head.top + 2 * pad + icon - 4 * u
        val gap = 8 * u
        val cw = (head.width() - 2 * pad - 8 * gap) / 9
        for (i in 1..9) {
            val cx = head.left + pad + (i - 1) * (cw + gap)
            val cell = RectF(cx, gy, cx + cw, gy + 22 * u)
            if (i <= DongPhu.giai) c.drawRoundRect(cell, 4 * u, 4 * u, k.fill) else c.drawRoundRect(k.inset(cell, 0.6f * u), 4 * u, 4 * u, k.stroke(1.3f))
            k.centered(c, DanDao.GIAI[i - 1].substringBefore(" "), cx, cw, gy + 26 * u, if (i == DongPhu.giai) sb else small)
        }
        var y = head.bottom + 12 * u
        val up = RectF(a.left, y, a.right, y + 54 * u)
        if (DongPhu.giai >= 9) {
            k.button(c, up, "Linh mạch đã tới Cửu giai", sb, enabled = false)
        } else {
            val price = DongPhu.upgradePrice()
            val ok = Store.linhThach >= price
            k.button(c, up, "Nâng lên ${DongPhu.giaiName(DongPhu.giai + 1)} · $price linh thạch", sb, enabled = ok)
            if (ok) hits.add(up) { upgradeDialog() }
        }
        y = up.bottom + 18 * u

        // Đan đạo and đan độc.
        y = k.section(c, a.left, y, a.width(), "ĐAN ĐẠO")
        val cap = DongPhu.cap()
        val next = if (cap < 9) " · ${DanDao.CAP[cap]} ở ${DanDao.CAP_AT[cap]} lò" else ""
        k.pair(c, "Luyện đan sư", "${DanDao.CAP[cap - 1]} · ${DongPhu.made} lò thành$next", a.left, y, a.width()); y += sl + 6 * u
        k.pair(c, "Đan phương đã biết", "${DongPhu.known.size}/${RECIPES.size}", a.left, y, a.width()); y += sl + 6 * u
        k.pair(c, "Lò hỏng · nổ lò", "${DongPhu.failed} · ${DongPhu.burst}", a.left, y, a.width()); y += sl + 16 * u
        y = k.section(c, a.left, y, a.width(), "ĐAN ĐỘC")
        k.meter(c, a.left, y + 2 * u, a.width() - 120 * u, DongPhu.danDoc / 100f)
        k.right(c, "${DongPhu.danDoc} / 100", a.right, y - 2 * u, sb)
        val mark = a.left + (a.width() - 120 * u) * DongPhu.DOC_HIGH / 100f
        c.drawRect(mark - u, y - 3 * u, mark + u, y + 17 * u, k.fill)
        y += 26 * u
        y = para(c, k, "Mỗi viên đan để lại độc (đan phẩm cao ít độc hơn; đan mua ở Phường thị ${DongPhu.SHOP_PILL_DOC}). Quá ${DongPhu.DOC_HIGH}: đan giảm nửa công hiệu, căn cơ −10. " +
            "Mọi lúc: tỉ lệ đột phá trừ đan độc / 4. Mỗi ngày đọc 30 phút tan ${DongPhu.DOC_FADE}; Giải Độc Đan tan nhanh.", a.left, y, a.width(), small, 4) + 16 * u

        // The other rooms.
        y = k.section(c, a.left, y, a.width(), "CÁC GIAN KHÁC")
        val bw = (a.width() - 2 * 10 * u) / 3
        val rooms = listOf(
            "Luyện khí thất ›" to { startActivity(Intent(this, PhapBaoActivity::class.java)) },
            "Tàng thư ›" to { startActivity(Intent(this, TuViActivity::class.java).putExtra(EXTRA_TAB, 4)) },
            "Tàng Kinh Các ›" to { startActivity(Intent(this, CongPhapActivity::class.java).putExtra(EXTRA_TAB, 4)) },
        )
        for ((i, room) in rooms.withIndex()) {
            val r = RectF(a.left + i * (bw + 10 * u), y, a.left + i * (bw + 10 * u) + bw, y + 52 * u)
            k.button(c, r, room.first, sb)
            hits.add(r) { room.second() }
        }
        return 1
    }

    private fun upgradeDialog() {
        val price = DongPhu.upgradePrice()
        AlertDialog.Builder(this)
            .setTitle("Nâng linh mạch lên ${DongPhu.giaiName(DongPhu.giai + 1)}?")
            .setMessage("Linh lực +2%, đột phá +1%, dược linh thêm 1 năm mỗi ngày; luống mở ở Nhị, Tứ, Lục, Bát giai.\n\nGiá $price linh thạch · đang có ${Store.linhThach}.")
            .setPositiveButton("Nâng") { _, _ ->
                if (DongPhu.upgrade()) toast("Linh mạch ${DongPhu.giaiName()}")
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    // --- Dược viên --------------------------------------------------------------------------------------

    private fun duocVien(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "DƯỢC VIÊN · ${DongPhu.open()}/6 LUỐNG · +${DongPhu.yearsADay()} NĂM MỖI NGÀY ĐỌC 30 PHÚT")
        val gap = 12 * u
        val cw = (a.width() - gap) / 2
        val ch = 150 * u
        for (i in 0 until 6) {
            val x = a.left + (i % 2) * (cw + gap)
            val cy = y + (i / 2) * (ch + gap)
            val r = RectF(x, cy, x + cw, cy + ch)
            val p = DongPhu.plots[i]
            val pad = 14 * u
            val open = i < DongPhu.open()
            if (open) k.card(c, r) else c.drawRoundRect(k.inset(r, u), 14 * u, 14 * u, k.faint)
            val tx = r.left + pad
            val tw = r.width() - 2 * pad
            val b = RectF(r.left + pad, r.bottom - pad - 46 * u, r.right - pad, r.bottom - pad)
            when {
                !open -> {
                    val need = (1..9).first { DanDao.plots(it) > i }
                    k.text(c, "Luống ${i + 1}", tx, r.top + pad, k.ui10Bold)
                    para(c, k, "Mở khi linh mạch tới ${DanDao.GIAI[need - 1]}", tx, r.top + pad + k.lineH(k.ui10Bold) + 4 * u, tw, small, 2)
                }
                p.herb < 0 -> {
                    k.text(c, "Luống ${i + 1} · trống", tx, r.top + pad, k.ui10Bold)
                    k.text(c, k.fit("Gieo một hạt, đọc đều để thảo lớn", tw, small), tx, r.top + pad + k.lineH(k.ui10Bold) + 4 * u, small)
                    val any = DongPhu.seeds.any { it > 0 }
                    k.button(c, b, if (any) "Gieo hạt" else "Hết hạt", sb, enabled = any)
                    if (any) hits.add(b) { sowDialog(i) }
                }
                else -> {
                    val years = DongPhu.years(i)
                    k.text(c, k.fit(HERBS[p.herb], tw, k.ui10Bold), tx, r.top + pad, k.ui10Bold)
                    k.text(c, k.fit("$years năm dược linh", tw, sb), tx, r.top + pad + k.lineH(k.ui10Bold) + 4 * u, sb)
                    k.button(c, b, "Thu hoạch", sb)
                    val name = HERBS[p.herb]
                    hits.add(b) {
                        if (DongPhu.harvest(i)) toast("Thu $name $years năm")
                        refresh()
                    }
                }
            }
        }
        y += 3 * (ch + gap) + 8 * u

        // Seeds and herbs.
        y = k.section(c, a.left, y, a.width(), "TÚI HẠT")
        val seeds = HERBS.indices.filter { DongPhu.seeds[it] > 0 }.joinToString(" · ") { "${HERBS[it]} ×${DongPhu.seeds[it]}" }
        y = para(c, k, seeds.ifEmpty { "Chưa có hạt: mua ở đây, hái ở bí cảnh tuần hoặc gặp trong kỳ ngộ." }, a.left, y, a.width(), small, 2) + 10 * u
        y = k.section(c, a.left, y, a.width(), "LINH THẢO ĐÃ THU")
        val herbs = DongPhu.herbs.groupBy { it.first }.toSortedMap().map { (kind, list) ->
            "${HERBS[kind]} " + list.map { it.second }.sortedDescending().joinToString(", ") + " năm"
        }.joinToString(" · ")
        val buy = RectF(a.left, a.bottom - 52 * u, a.right, a.bottom)
        val room = ((buy.top - 10 * u - y) / sl).toInt().coerceAtLeast(1)
        para(c, k, herbs.ifEmpty { "Chưa thu hoạch. Thảo càng già, đan càng dễ thành." }, a.left, y, a.width(), small, room)
        k.button(c, buy, "Mua hạt giống", sb)
        hits.add(buy) { seedShop() }
        return 1
    }

    private fun sowDialog(plot: Int) {
        val kinds = HERBS.indices.filter { DongPhu.seeds[it] > 0 }
        AlertDialog.Builder(this)
            .setTitle("Gieo vào luống ${plot + 1}")
            .setItems(kinds.map { "${HERBS[it]} · còn ${DongPhu.seeds[it]} hạt" }.toTypedArray()) { _, i ->
                if (DongPhu.plant(plot, kinds[i])) toast("Đã gieo ${HERBS[kinds[i]]}")
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    private fun seedShop() {
        AlertDialog.Builder(this)
            .setTitle("Hạt giống · có ${Store.linhThach} linh thạch")
            .setItems(HERBS.indices.map { "${HERBS[it]} · ${DanDao.SEED_PRICES[it]} linh thạch · đang có ${DongPhu.seeds[it]}" }.toTypedArray()) { _, i ->
                if (DongPhu.buySeed(i)) toast("Mua một hạt ${HERBS[i]}") else toast("Cần ${DanDao.SEED_PRICES[i]} linh thạch")
                refresh()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    // --- Đan phòng ---------------------------------------------------------------------------------------

    private fun herbText(r: Int) = RECIPES[r].herbs.joinToString(" · ") { (kind, min) ->
        val have = DongPhu.herbs.any { it.first == kind && it.second >= min }
        "${HERBS[kind]} $min năm ${if (have) "✓" else "✗"}"
    }

    private fun danPhong(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        var y = a.top
        if (DongPhu.brew >= 0) {
            val rec = RECIPES[DongPhu.brew]
            val lines = k.wrap("Hỏa hầu: buổi đọc tới phải liền ${rec.minutes} phút. Thoát giữa chừng (từ 1 phút trở lên) là nổ lò, mất linh thảo. Tỉ lệ thành ${DongPhu.brewChance}%.", a.width() - 2 * pad, small, 3)
            val card = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui12Bold) + 6 * u + lines.size * sl + 66 * u)
            k.card(c, card, selected = true)
            k.text(c, "Lò đang đỏ: ${rec.name}", a.left + pad, y + pad, k.ui12Bold)
            var ly = y + pad + k.lineH(k.ui12Bold) + 6 * u
            for (l in lines) {
                k.text(c, l, a.left + pad, ly, small)
                ly += sl
            }
            val b = RectF(a.left + pad, card.bottom - pad - 48 * u, a.right - pad, card.bottom - pad)
            k.button(c, b, "Tắt lò, lấy lại linh thảo", sb)
            hits.add(b) {
                DongPhu.cancelBrew()
                refresh()
            }
            y = card.bottom + 14 * u
        } else if (DongPhu.lastResult.isNotEmpty()) {
            k.text(c, k.fit("Gần nhất: ${DongPhu.lastResult}", a.width(), sb), a.left, y, sb)
            y += sl + 8 * u
        }
        y = k.section(c, a.left, y, a.width(), "ĐAN PHƯƠNG · ${DongPhu.known.size}/${RECIPES.size} · LUYỆN ĐAN SƯ ${DanDao.CAP[DongPhu.cap() - 1].uppercase()}")
        val list = DongPhu.known.sortedWith(compareBy<Int>({ !DongPhu.canBrew(it) }, { RECIPES[it].cap }, { it }))
        val h = rowH(k)
        val per = ((a.bottom - y) / (h + 8 * u)).toInt().coerceAtLeast(1)
        for (r in list.drop(page * per).take(per)) {
            val rec = RECIPES[r]
            val (btn, ok) = when {
                DongPhu.brew >= 0 -> "Lò đang đỏ" to false
                DongPhu.cap() < rec.cap -> "Cần ${DanDao.CAP[rec.cap - 1].lowercase()}" to false
                !DongPhu.hasHerbs(r) -> "Thiếu thảo" to false
                else -> "Khai lò · ${DongPhu.chance(r)}%" to true
            }
            row(c, k, RectF(a.left, y, a.right, y + h), rec.name, DanDao.GROUPS[rec.group], herbText(r),
                "Hỏa hầu ${rec.minutes} phút · độc ${rec.doc} · ${rec.text}", btn, ok, hits) { brewDialog(r) }
            y += h + 8 * u
        }
        return ((list.size + per - 1) / per).coerceAtLeast(1)
    }

    private fun brewDialog(r: Int) {
        val rec = RECIPES[r]
        AlertDialog.Builder(this)
            .setTitle("Khai lò ${rec.name}?")
            .setMessage("Linh thảo: ${herbText(r).replace(" ✓", "")}.\n\nHỏa hầu: buổi đọc tới phải liền ${rec.minutes} phút, thoát giữa chừng là nổ lò. Tỉ lệ thành ${DongPhu.chance(r)}%.")
            .setPositiveButton("Khai lò") { _, _ ->
                if (DongPhu.startBrew(r)) toast("Lò đã đỏ: đọc liền ${rec.minutes} phút")
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    // --- Đan dược ----------------------------------------------------------------------------------------

    private fun why(r: Int): String = when (r) {
        DanDao.HOI_KHI -> "chỉ dùng khi đang hồi khí"
        DanDao.TUC_MACH -> "chỉ dùng khi kinh mạch tổn thương"
        DanDao.THANH_TAM -> "tâm ma đang 0"
        DanDao.DINH_HON -> "chỉ dùng khi tẩu hỏa nhập ma"
        DanDao.GIAI_DOC -> "đan độc đang 0"
        DanDao.TAY_LINH -> "cần linh căn đã thức tỉnh và 10 giờ đọc từ lúc đó"
        else -> if (RECIPES[r].gate > 0) "chỉ cho cửa ${com.exacta.reader.game.Realm.baseName(RECIPES[r].gate)}, một viên mỗi lần đột phá" else ""
    }

    private fun danDuoc(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        var y = k.section(c, a.left, a.top, a.width(), "ĐAN DƯỢC · ĐAN ĐỘC ${DongPhu.danDoc}/100")
        if (DongPhu.danDoc > DongPhu.DOC_HIGH) {
            k.text(c, k.fit("Đan độc quá ${DongPhu.DOC_HIGH}: đan chỉ còn nửa công hiệu, căn cơ −10. Dùng Giải Độc Đan hoặc đọc đều.", a.width(), sb), a.left, y, sb)
            y += k.lineH(sb) + 8 * u
        }
        val keys = DongPhu.pills.keys.sortedWith(compareBy<Int>({ !DongPhu.usable(it / 4) }, { it / 4 }, { -(it % 4) }))
        if (keys.isEmpty()) {
            para(c, k, "Chưa có đan nào. Khai lò ở Đan phòng: chọn đan phương, đủ linh thảo, rồi đọc liền đủ hỏa hầu. Đan mua ở Phường thị vẫn nằm trong Pháp bảo › Túi đồ.", a.left, y, a.width(), small, 4)
            return 1
        }
        val h = rowH(k)
        val per = ((a.bottom - y) / (h + 8 * u)).toInt().coerceAtLeast(1)
        for (key in keys.drop(page * per).take(per)) {
            val r = key / 4
            val pham = key % 4
            val rec = RECIPES[r]
            val ok = DongPhu.usable(r)
            row(c, k, RectF(a.left, y, a.right, y + h), "${rec.name} · ${DanDao.PHAM[pham]}", "× ${DongPhu.count(r, pham)}",
                rec.text + if (pham > 0 && rec.value > 0) " (phẩm cao mạnh hơn)" else "",
                "Độc +${rec.doc * DanDao.DOC_PCT[pham] / 100}" + if (!ok) " · ${why(r)}" else "", if (ok) "Dùng" else "Chưa dùng được", ok, hits) {
                val text = DongPhu.use(r, pham)
                if (text != null) toast(text)
                refresh()
            }
            y += h + 8 * u
        }
        return ((keys.size + per - 1) / per).coerceAtLeast(1)
    }

    // --- Tĩnh thất ----------------------------------------------------------------------------------------

    private fun tinhThat(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        var y = a.top
        val card = RectF(a.left, y, a.right, y + 2 * pad + 64 * u + 8 * u + 2 * sl)
        k.card(c, card, selected = Store.beQuanTarget > 0)
        k.icon(c, "bequan", card.left + pad, card.top + pad, 64 * u)
        val tx = card.left + 2 * pad + 64 * u
        val tw = card.right - pad - tx
        if (Store.beQuanTarget > 0) {
            k.text(c, "Đang bế quan · ${Store.beQuanSeconds / 60}/${Store.beQuanTarget} phút", tx, card.top + pad, k.ui12Bold)
            k.meter(c, tx, card.top + pad + k.lineH(k.ui12Bold) + 8 * u, tw, Store.beQuanSeconds / 60f / Store.beQuanTarget)
            para(c, k, "Đọc đủ phút thì viên mãn, nhận ${Store.beQuanReward(Store.beQuanTarget)} linh thạch. Trong lúc bế quan: lò luyện ×1,5, kỳ ngộ ×2.",
                tx, card.top + pad + k.lineH(k.ui12Bold) + 30 * u, tw, small, 2)
        } else {
            k.text(c, "Tĩnh thất", tx, card.top + pad, k.ui12Bold)
            para(c, k, "Bế quan là một hẹn đọc: chọn số phút, đọc cho đủ (cộng dồn) thì viên mãn. Trong lúc bế quan lò luyện cháy ×1,5, kỳ ngộ ×2.",
                tx, card.top + pad + k.lineH(k.ui12Bold) + 4 * u, tw, small, 3)
        }
        y = card.bottom + 16 * u
        if (Store.beQuanTarget > 0) {
            val b = RectF(a.left, y, a.right, y + 54 * u)
            k.button(c, b, "Xuất quan sớm (không nhận thưởng)", sb)
            hits.add(b) {
                Store.cancelBeQuan()
                refresh()
            }
            y = b.bottom + 18 * u
        } else {
            y = k.section(c, a.left, y, a.width(), "BẾ QUAN")
            for (m in Store.BE_QUAN_CHOICES) {
                val b = RectF(a.left, y, a.right, y + 54 * u)
                k.button(c, b, "Bế quan $m phút · ${Store.beQuanReward(m)} linh thạch", sb)
                hits.add(b) {
                    Store.startBeQuan(m)
                    toast("Bế quan $m phút: mở sách mà đọc")
                    refresh()
                }
                y = b.bottom + 10 * u
            }
            y += 8 * u
        }
        y = k.section(c, a.left, y, a.width(), "ĐÃ BẾ QUAN")
        k.pair(c, "Viên mãn", "${Store.beQuanDone} lần · ${Store.beQuanMinutes} phút", a.left, y, a.width()); y += sl + 6 * u
        if (DongPhu.brew >= 0) {
            y += 10 * u
            para(c, k, "Đan phòng đang đỏ lửa: ${RECIPES[DongPhu.brew].name}, buổi đọc tới phải liền ${RECIPES[DongPhu.brew].minutes} phút. Bế quan và luyện đan đi cùng nhau được.",
                a.left, y, a.width(), sb, 3)
        }
        return 1
    }
}
