package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.Beast
import com.exacta.reader.data.Store
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/**
 * Pháp bảo, in tabs: the forge (three ores burning with the words read, the bag, the next gift),
 * the treasury (strongest first, a page at a time) and the nine bộ pháp bảo.
 */
class PhapBaoActivity : TabActivity() {
    override fun title() = "Pháp Bảo"
    override fun right() = "${Store.linhThach} linh thạch"
    override fun tabs() = listOf("Lò luyện", "Túi đồ", "Phường thị", "Bảo khố", "Bộ pháp bảo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.checkSets()
        if (Store.unseen > 0 && savedInstanceState == null && !intent.hasExtra(EXTRA_TAB)) selectTab(3)
        Store.unseen = 0
        Store.touch()
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> forge(c, k, area, hits)
        1 -> bag(c, k, area, page, hits)
        2 -> market(c, k, area, hits)
        3 -> treasury(c, k, area, page, hits)
        else -> sets(c, k, area, page, hits)
    }

    private fun forge(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "LÒ LUYỆN · ${Store.forge.size}/${Store.FORGE_SLOTS} Ô")
        // Six slots, three by two.
        val gap = 12 * u
        val slotW = (a.width() - 2 * gap) / 3
        val slotH = ((a.height() - 2 * gap) * 0.3f).coerceIn(140 * u, 210 * u)
        for (i in 0 until Store.FORGE_SLOTS) {
            val x = a.left + (i % 3) * (slotW + gap)
            val sy = y + (i / 3) * (slotH + gap)
            val r = RectF(x, sy, x + slotW, sy + slotH)
            val ore = Store.forge.getOrNull(i)
            k.card(c, r)
            hits.add(r) { slot(ore) }
            val pad = 10 * u
            val textH = 3 * sl
            val pic = minOf(r.width() - 2 * pad, r.height() - 2 * pad - textH - 8 * u)
            val py = r.top + pad
            var ty = py + pic + 8 * u
            when {
                ore == null -> {
                    k.art(c, "art_crystal", r.centerX() - pic / 2, py, pic, 90)
                    k.centered(c, "Linh tinh chờ luyện", r.left + 4 * u, r.width() - 8 * u, ty, sb); ty += sl
                    k.centered(c, "chạm để khai lô", r.left, r.width(), ty, small)
                }
                ore.inKiep -> {
                    // Kiếp phá lô: the treasure waits behind a trial of days.
                    k.icon(c, "thienkiep", r.centerX() - pic * 0.35f, py, pic * 0.7f)
                    k.centered(c, "Kiếp phá lô · ${Treasure.TIER_NAMES[ore.tier]}", r.left + 4 * u, r.width() - 8 * u, ty, sb); ty += sl
                    k.centered(c, "ngày ${Store.kiepMet(ore)}/${Store.kiepDays(ore.tier)} · ${Store.kiepMinutes(ore.tier)} phút/ngày", r.left + 4 * u, r.width() - 8 * u, ty, small); ty += sl
                    k.centered(c, "hôm nay ${Store.minutes(Store.today())} phút", r.left, r.width(), ty, small)
                }
                ore.isBook -> {
                    val b = Store.books[ore.book]
                    val cw = pic * 0.66f
                    k.cover(c, b?.cover ?: "", b?.title ?: "Sách", RectF(r.centerX() - cw / 2, py, r.centerX() + cw / 2, py + pic))
                    k.centered(c, b?.title ?: "Sách", r.left + pad, r.width() - 2 * pad, ty, sb); ty += sl
                    val goal = if (ore.isStage) "Chặng ${Treasure.TIER_NAMES[ore.tier]}" else "Cả cuốn → ${Treasure.TIER_NAMES[ore.tier]}"
                    k.centered(c, goal, r.left + 4 * u, r.width() - 8 * u, ty, small); ty += sl
                    val left = if (ore.isStage) "còn ${((ore.need - ore.words).coerceAtLeast(0) + 999) / 1000} nghìn từ" else "đã đọc ${ore.percent}%"
                    k.centered(c, left, r.left, r.width(), ty, small)
                }
                else -> {
                    k.ore(c, ore.tier, ore.percent, r.centerX() - pic / 2, py, pic, bar = true)
                    k.centered(c, Treasure.TIER_NAMES[ore.tier], r.left, r.width(), ty, sb); ty += sl
                    k.centered(c, "${Treasure.stageName(ore.percent)} · ${ore.percent}%", r.left, r.width(), ty, small); ty += sl
                    k.centered(c, "còn ${((ore.need - ore.words).coerceAtLeast(0) + 999) / 1000} nghìn từ", r.left, r.width(), ty, small)
                }
            }
            if (ore != null && ore.isBook && !ore.inKiep) {
                // The stage's progress along the bottom edge.
                val bx = r.left + 14 * u
                val bw = r.width() - 28 * u
                val by = r.bottom - 9 * u
                c.drawRoundRect(bx, by, bx + bw, by + 4 * u, 2 * u, 2 * u, k.gray)
                c.drawRoundRect(bx, by, bx + bw * ore.percent / 100f, by + 4 * u, 2 * u, 2 * u, k.fill)
            }
        }
        y += 2 * slotH + gap + 20 * u

        // How it works.
        y = k.section(c, a.left, y, a.width(), "CÁCH LUYỆN")
        val nextGift = (Store.giftsAwarded + 1) * Treasure.GIFT_WORDS - Store.totalWords
        val notes = listOf(
            "Chặng: đọc đủ số chữ của phẩm trong cuốn đó là ra một pháp bảo, rồi luyện tiếp",
            "Mỗi cuốn tối đa ${Store.SLOTS_PER_BOOK} ô, mỗi ô một phẩm; hoặc chọn luyện cả cuốn",
            "Thiên phẩm trở lên phải qua kiếp phá lô: đọc đủ phút trong vài ngày liền",
            "Khoáng thạch cháy bằng chữ đọc ở bất kỳ cuốn nào",
            "Quà mỗi 10 vạn từ: còn ${(nextGift.coerceAtLeast(0) + 999) / 1000} nghìn từ",
        )
        for (n in notes) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl / 2, 4 * u)
            k.text(c, k.fit(n, a.width() - 24 * u, small), a.left + 20 * u, y, small)
            y += sl + 6 * u
        }
        return 1
    }

    /** Túi đồ: page 1 the ores and the eighteen pills and talismans; page 2 what is at work, and the kỳ ngộ. */
    private fun bag(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        if (page == 1) {
            var y = k.section(c, a.left, a.top, a.width(), "ĐANG CÓ HIỆU LỰC")
            val active = listOfNotNull(
                if (Store.fireWords > 0) "Thiên Hỏa Phù: lò ×2 thêm ${(Store.fireWords + 999) / 1000} nghìn từ" else null,
                if (Store.seekWords > 0) "Tầm Bảo Phù: kỳ ngộ ×3 thêm ${(Store.seekWords + 999) / 1000} nghìn từ" else null,
                if (Store.giftLuck > 0) "Tụ Bảo Phù: ${Store.giftLuck} lần quà 10 vạn từ gieo ba lần" else null,
                if (Store.trialSpare > 0) "Hộ Mạch Đan: thiên kiếp đang độ được lỡ thêm ${Store.trialSpare} ngày" else null,
                if (Store.trialBonus > 0) "Phá Cảnh Đan: thiên kiếp đang độ được tính thêm ${Store.trialBonus} ngày" else null,
                if (Store.bonusTuVi > 0) "Ngưng Thần Đan: tu vi thêm ${com.exacta.reader.game.Realm.van(Store.bonusTuVi)} vạn từ" else null,
                (Store.bonusMinutes[Store.today()] ?: 0).takeIf { it > 0 }?.let { "Định Thần Phù: hôm nay tính thêm $it phút" },
            )
            if (active.isEmpty()) {
                k.text(c, "Chưa có đan phù nào đang có hiệu lực.", a.left, y, small)
                y += sl
            }
            for (l in active) {
                k.diamond(c, a.left + 6 * u, y + sl / 2, 4 * u)
                k.text(c, k.fit(l, a.width() - 24 * u, small), a.left + 20 * u, y, small)
                y += sl + 4 * u
            }
            y += 18 * u
            y = k.section(c, a.left, y, a.width(), "KỲ NGỘ · ${Store.kyNgoCount} lần")
            if (Store.kyNgoLog.isEmpty()) {
                for (l in k.wrap("Mỗi vạn từ đọc được là một lần cơ duyên: có khi nhặt được khoáng thạch, đan dược, phù chú, hay đánh thức khí linh của pháp bảo.", a.width(), small, 3)) {
                    k.text(c, l, a.left, y, small)
                    y += sl
                }
            }
            for (line in Store.kyNgoLog) {
                if (y + sl > a.bottom) break
                k.diamond(c, a.left + 6 * u, y + sl / 2, 4 * u)
                k.text(c, k.fit(line, a.width() - 24 * u, small), a.left + 20 * u, y, small)
                y += sl + 4 * u
            }
            return 2
        }

        var y = k.section(c, a.left, a.top, a.width(), "KHOÁNG THẠCH · ${Store.bag.sum()} viên")
        val cw = (a.width() - 5 * 10 * u) / 6
        val ic = minOf(cw - 24 * u, 52 * u)
        for (t in 0 until Treasure.TIERS) {
            val x = a.left + t * (cw + 10 * u)
            k.ore(c, t, 0, x + (cw - ic) / 2, y, ic, alpha = if (Store.bag[t] > 0) 255 else 60)
            k.centered(c, Treasure.TIER_NAMES[t].removeSuffix(" phẩm") + " ×${Store.bag[t]}", x - 4 * u, cw + 8 * u, y + ic + 2 * u, small)
        }
        y += ic + sl + 18 * u

        y = k.section(c, a.left, y, a.width(), "ĐAN DƯỢC · PHÙ CHÚ · ${Store.items.sum()} món")
        val gap = 8 * u
        val cols = 3
        val rows = (Treasure.ITEMS + cols - 1) / cols
        val iw = (a.width() - (cols - 1) * gap) / cols
        val ih = ((a.bottom - y - (rows - 1) * gap) / rows).coerceAtMost(96 * u)
        for (i in 0 until Treasure.ITEMS) {
            val r = RectF(a.left + (i % cols) * (iw + gap), y + (i / cols) * (ih + gap), a.left + (i % cols) * (iw + gap) + iw, y + (i / cols) * (ih + gap) + ih)
            val have = Store.items[i] > 0
            k.card(c, r, selected = have)
            val pic = minOf(ih - 14 * u, 64 * u)
            k.art(c, "art_item_$i", r.left + 6 * u, r.centerY() - pic / 2, pic, if (have) 255 else 70)
            val tx = r.left + 10 * u + pic
            val tw = r.right - 6 * u - tx
            val name = k.wrap(Treasure.ITEM_NAMES[i], tw, sb, 2)
            var ty = r.centerY() - (name.size * k.lineH(sb) + sl) / 2
            for (l in name) {
                k.text(c, l, tx, ty, sb)
                ty += k.lineH(sb)
            }
            k.text(c, "× ${Store.items[i]}", tx, ty, small)
            hits.add(r) { itemDialog(i) }
        }
        return 2
    }

    private fun toast(t: String) = android.widget.Toast.makeText(this, t, android.widget.Toast.LENGTH_SHORT).show()

    /** Phường thị: ores and the eighteen items for linh thạch. */
    private fun market(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        // The purse.
        val purse = RectF(a.left, a.top, a.right, a.top + 64 * u)
        k.card(c, purse, selected = true)
        k.art(c, "art_linhthach", purse.left + 12 * u, purse.top + 8 * u, 48 * u)
        k.text(c, "${Store.linhThach} linh thạch", purse.left + 72 * u, purse.top + 10 * u, k.ui12Bold)
        val how = "Đọc mỗi vạn từ +5 · bế quan · bí cảnh · kỳ ngộ · vượt kiếp · Tết ×3, mồng một và rằm ×2"
        k.text(c, k.fit(how, purse.width() - 84 * u, small), purse.left + 72 * u, purse.top + 14 * u + k.lineH(k.ui12Bold), small)
        var y = purse.bottom + 16 * u

        y = k.section(c, a.left, y, a.width(), "KHOÁNG THẠCH")
        val cw = (a.width() - 5 * 10 * u) / 6
        val ic = minOf(cw - 24 * u, 48 * u)
        for (t in 0 until Treasure.TIERS) {
            val x = a.left + t * (cw + 10 * u)
            val price = Store.ORE_PRICES[t]
            val cell = RectF(x, y, x + cw, y + ic + 2 * sl + 10 * u)
            k.ore(c, t, 0, x + (cw - ic) / 2, y, ic, alpha = if (Store.linhThach >= price) 255 else 70)
            k.centered(c, Treasure.TIER_NAMES[t].removeSuffix(" phẩm"), x - 4 * u, cw + 8 * u, y + ic + 2 * u, small)
            k.centered(c, "$price", x, cw, y + ic + 2 * u + sl, sb)
            hits.add(cell) { buy("khoáng thạch ${Treasure.TIER_NAMES[t]}", price, "Vào túi, rồi tự vào lò khi còn ô.") { Store.buyOre(t) } }
        }
        y += ic + 2 * sl + 22 * u

        y = k.section(c, a.left, y, a.width(), "ĐAN DƯỢC · PHÙ CHÚ")
        val gap = 8 * u
        val cols = 3
        val rows = (Treasure.ITEMS + cols - 1) / cols
        val iw = (a.width() - (cols - 1) * gap) / cols
        val ih = ((a.bottom - y - (rows - 1) * gap) / rows).coerceAtMost(90 * u)
        for (i in 0 until Treasure.ITEMS) {
            val r = RectF(a.left + (i % cols) * (iw + gap), y + (i / cols) * (ih + gap), a.left + (i % cols) * (iw + gap) + iw, y + (i / cols) * (ih + gap) + ih)
            val price = Store.ITEM_PRICES[i]
            val afford = Store.linhThach >= price
            k.card(c, r)
            val pic = minOf(ih - 14 * u, 60 * u)
            k.art(c, "art_item_$i", r.left + 6 * u, r.centerY() - pic / 2, pic, if (afford) 255 else 80)
            val tx = r.left + 10 * u + pic
            val tw = r.right - 6 * u - tx
            val name = k.wrap(Treasure.ITEM_NAMES[i], tw, sb, 2)
            var ty = r.centerY() - (name.size * k.lineH(sb) + sl) / 2
            for (l in name) {
                k.text(c, l, tx, ty, sb)
                ty += k.lineH(sb)
            }
            k.text(c, "$price linh thạch", tx, ty, small)
            hits.add(r) { buy(Treasure.ITEM_NAMES[i], price, Treasure.ITEM_TEXTS[i]) { Store.buyItem(i) } }
        }
        return 1
    }

    private fun buy(what: String, price: Long, text: String, doIt: () -> Boolean) {
        if (Store.linhThach < price) {
            toast("Cần $price linh thạch, mới có ${Store.linhThach}")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Mua $what?")
            .setMessage("$text\n\nGiá $price linh thạch · đang có ${Store.linhThach}.")
            .setPositiveButton("Mua") { _, _ ->
                if (doIt()) toast("Đã mua $what")
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    /** One item: what it does, and Dùng when there is one. */
    private fun itemDialog(i: Int) {
        val b = AlertDialog.Builder(this)
            .setTitle("${Treasure.ITEM_NAMES[i]} · × ${Store.items[i]}")
            .setMessage(Treasure.ITEM_TEXTS[i] + if (Store.items[i] <= 0) "\n\nChưa có. Đọc sách để gặp kỳ ngộ." else "")
            .setNegativeButton("Đóng", null)
        if (Store.items[i] > 0 && i != Treasure.HO_LO_PHU) b.setPositiveButton("Dùng") { _, _ -> useItem(i) }
        b.show()
    }

    private fun choose(title: String, labels: List<String>, onPick: (Int) -> Unit) {
        AlertDialog.Builder(this).setTitle(title)
            .setItems(labels.toTypedArray()) { _, w -> onPick(w); refresh() }
            .setNegativeButton("Thôi", null).show()
    }

    private fun slotLabel(o: com.exacta.reader.data.Ore): String {
        val what = if (o.isBook) "«${Store.books[o.book]?.title ?: "sách"}» chặng" else "Khoáng thạch"
        return if (o.inKiep) "$what ${Treasure.TIER_NAMES[o.tier]} · kiếp phá lô ${Store.kiepMet(o)}/${Store.kiepDays(o.tier)}" else "$what ${Treasure.TIER_NAMES[o.tier]} · ${o.percent}%"
    }

    private fun useItem(i: Int) {
        when (i) {
            Treasure.TU_LINH_DAN, Treasure.BOI_NGUYEN_DAN, Treasure.CUU_CHUYEN_DAN, Treasure.DAN_LOI_PHU -> {
                val slots = Store.slotsFor(i)
                if (slots.isEmpty()) {
                    toast(when (i) {
                        Treasure.TU_LINH_DAN -> "Chỉ dùng cho lò Thiên phẩm trở lên đang luyện"
                        Treasure.CUU_CHUYEN_DAN -> "Chỉ dùng cho lò dưới Thiên phẩm đang luyện"
                        Treasure.DAN_LOI_PHU -> "Chưa có lò nào đang qua kiếp phá lô"
                        else -> "Chưa có lò nào đang luyện"
                    })
                    return
                }
                choose("Dùng ${Treasure.ITEM_NAMES[i]} cho lò nào?", slots.map { slotLabel(it) }) { Store.useOnSlot(i, slots[it]) }
            }
            Treasure.TAY_TUY_DAN -> {
                val tiers = Store.liftableOres()
                if (tiers.isEmpty()) { toast("Túi chưa có khoáng thạch dưới Hỗn Độn phẩm"); return }
                choose("Tẩy tủy khoáng thạch nào?", tiers.map { "${Treasure.TIER_NAMES[it]} (còn ${Store.bag[it]}) → ${Treasure.TIER_NAMES[it + 1]}" }) { Store.liftOre(tiers[it]) }
            }
            Treasure.KHAI_LINH_DAN -> {
                val sleeping = Store.beasts.filter { it.spirit.isEmpty() }.sortedByDescending { it.tier }
                if (sleeping.isEmpty()) { toast("Mọi pháp bảo đều đã có khí linh"); return }
                choose("Khai linh cho pháp bảo nào?", sleeping.map { "${it.name} · ${Treasure.TIER_NAMES[it.tier]}" }) { Store.awaken(sleeping[it]) }
            }
            Treasure.LUYEN_HOA_PHU -> {
                val dup = Store.duplicates().sortedBy { it.tier }
                if (dup.isEmpty()) { toast("Chưa có pháp bảo nào trùng loại"); return }
                choose("Luyện hóa pháp bảo nào?", dup.map { "${it.name} · ${Treasure.TIER_NAMES[it.tier]} → khoáng thạch" }) { Store.melt(dup[it]) }
            }
            else -> {
                val line = Store.useSimple(i)
                toast(line ?: when (i) {
                    Treasure.PHA_CANH_DAN -> "Chỉ dùng khi đang độ kiếp từ Phá Đan Thành Anh trở lên"
                    Treasure.HO_MACH_DAN -> "Chỉ dùng khi đang độ thiên kiếp"
                    Treasure.THO_NGUYEN_DAN -> "Tu vi chưa mất gì vì độ kiếp"
                    Treasure.BI_CANH_PHU -> "Tuần này đã qua bí cảnh"
                    Treasure.PHONG_AN_PHU -> "Tuần qua không có ngày nào lỡ"
                    else -> "Chưa dùng được lúc này"
                })
                refresh()
            }
        }
    }

    private fun treasury(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val beasts = Store.beasts.sortedWith(compareByDescending<Beast> { it.tier }.thenByDescending { it.bornDay })
        if (beasts.isEmpty()) {
            val s = 160 * u
            k.art(c, "art_crystal", a.centerX() - s / 2, a.top + 60 * u, s)
            k.centered(c, "Bảo khố còn trống", a.left, a.width(), a.top + 80 * u + s, k.ui12Bold)
            k.centered(c, "Đọc hết một cuốn hoặc chờ lò luyện thành pháp bảo.", a.left, a.width(), a.top + 84 * u + s + k.lineH(k.ui12Bold), k.small)
            return 1
        }
        // The hợp luyện bar.
        val barH = 52 * u
        val bar = RectF(a.left, a.top, a.right, a.top + barH)
        val ready = Store.mergeTiers()
        k.card(c, bar, selected = ready.isNotEmpty())
        val btn = RectF(bar.right - 190 * u, bar.top + 8 * u, bar.right - 8 * u, bar.bottom - 8 * u)
        k.button(c, btn, if (ready.isEmpty()) "Hợp luyện" else "Hợp luyện · ${ready.size}", enabled = ready.isNotEmpty())
        val hint = if (ready.isEmpty()) "Hợp luyện: 3 pháp bảo dư cùng phẩm thành 1 khoáng thạch phẩm trên · đã luyện ${Store.mergeCount} lần"
            else "Có ${ready.size} phẩm đủ 3 pháp bảo dư để hợp luyện · đã luyện ${Store.mergeCount} lần"
        val hl = k.wrap(hint, btn.left - bar.left - 24 * u, k.small, 2)
        var hy = bar.centerY() - hl.size * k.lineH(k.small) / 2
        for (l in hl) {
            k.text(c, l, bar.left + 14 * u, hy, k.small)
            hy += k.lineH(k.small)
        }
        hits.add(bar) { mergeDialog() }
        @Suppress("NAME_SHADOWING") val a = RectF(a.left, bar.bottom + 14 * u, a.right, a.bottom)

        val gap = 12 * u
        val cols = ((a.width() + gap) / (150 * u + gap)).toInt().coerceIn(3, 6)
        val cellW = (a.width() - (cols - 1) * gap) / cols
        val sb = k.smallBold
        val small = k.small
        val art = minOf(cellW - 28 * u, 120 * u)
        val cellH = art + k.lineH(sb) + k.lineH(small) + 26 * u
        val rows = ((a.height() + gap) / (cellH + gap)).toInt().coerceAtLeast(1)
        val per = rows * cols
        val pages = (beasts.size + per - 1) / per
        val rowGap = if (rows > 1) (a.height() - rows * cellH) / (rows - 1) else gap
        for ((i, b) in beasts.drop(page * per).take(per).withIndex()) {
            val x = a.left + (i % cols) * (cellW + gap)
            val y = a.top + (i / cols) * (cellH + rowGap)
            val r = RectF(x, y, x + cellW, y + cellH)
            k.card(c, r)
            k.badge(c, b.species, r.centerX() - art / 2, r.top + 10 * u, art)
            k.centered(c, b.name, r.left + 4 * u, r.width() - 8 * u, r.top + 14 * u + art, sb)
            k.centered(c, if (b.spirit.isNotEmpty()) "«${b.spirit}»" else Treasure.CLANS[b.tier], r.left + 4 * u, r.width() - 8 * u, r.top + 14 * u + art + k.lineH(sb), small)
            hits.add(r) { showBeast(b) }
        }
        return pages
    }

    /** Hợp luyện: pick a grade with three spares, see which three go, confirm. */
    private fun mergeDialog() {
        val tiers = Store.mergeTiers()
        if (tiers.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Hợp luyện")
                .setMessage("Ba pháp bảo dư cùng phẩm hợp luyện thành một khoáng thạch phẩm cao hơn, kèm ít linh thạch.\n\n" +
                    "Mỗi loại pháp bảo luôn giữ lại một món (món có khí linh, hoặc món lâu nhất); chỉ những món trùng loại mới là dư. " +
                    "Hỗn Độn phẩm là phẩm cao nhất nên không hợp luyện được.\n\nHiện chưa phẩm nào đủ 3 món dư.")
                .setPositiveButton("Đóng", null)
                .show()
            return
        }
        val labels = tiers.map { t -> "${Treasure.TIER_NAMES[t]}: dư ${Store.spares(t).size} món → khoáng thạch ${Treasure.TIER_NAMES[t + 1]}" }
        AlertDialog.Builder(this)
            .setTitle("Hợp luyện phẩm nào?")
            .setItems(labels.toTypedArray()) { _, w -> confirmMerge(tiers[w]) }
            .setNegativeButton("Thôi", null)
            .show()
    }

    private fun confirmMerge(tier: Int) {
        val three = Store.spares(tier).take(Store.MERGE_COUNT)
        val names = three.joinToString("\n") { "· ${it.name}" + if (it.spirit.isNotEmpty()) " «${it.spirit}»" else "" }
        AlertDialog.Builder(this)
            .setTitle("Hợp luyện ${Treasure.TIER_NAMES[tier]}")
            .setMessage("Ba món này sẽ tan vào lò:\n$names\n\nNhận: 1 khoáng thạch ${Treasure.TIER_NAMES[tier + 1]} và ${10 * (tier + 1)} linh thạch.")
            .setPositiveButton("Hợp luyện") { _, _ ->
                if (Store.merge(tier)) android.widget.Toast.makeText(this, "Vạn khí quy nhất: khoáng thạch ${Treasure.TIER_NAMES[tier + 1]} vào túi", android.widget.Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    private fun sets(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val have = Store.kinds()
        val gap = 12 * u
        val cols = 3
        val cw = (a.width() - (cols - 1) * gap) / cols
        val ch = (a.height() - 2 * gap) / 3
        val sb = k.smallBold
        val small = k.small
        for (s in page * 9 until minOf(Treasure.SETS, page * 9 + 9)) {
            val x = a.left + (s % 9 % cols) * (cw + gap)
            val y = a.top + (s % 9 / cols) * (ch + gap)
            val r = RectF(x, y, x + cw, y + ch)
            val done = Store.setActive(s)
            k.card(c, r, selected = done)
            val pad = 10 * u
            k.centered(c, Treasure.SET_NAMES[s], r.left + pad, r.width() - 2 * pad, r.top + pad, sb)
            val members = Treasure.SET_MEMBERS[s]
            val owned = members.count { it in have }
            val foot = if (done) "Đủ bộ · ${Treasure.SET_EFFECTS[s]}" else "$owned/4 · ${Treasure.SET_EFFECTS[s]}"
            val footLines = k.wrap(foot, r.width() - 2 * pad, small, 2)
            val gridTop = r.top + pad + k.lineH(sb) + 6 * u
            val gridBottom = r.bottom - pad - footLines.size * k.lineH(small) - 6 * u
            val cell = minOf((r.width() - 2 * pad - 8 * u) / 2, (gridBottom - gridTop - 6 * u) / 2)
            val gx = r.centerX() - cell - 3 * u
            for ((j, sp) in members.withIndex()) {
                val bx = gx + (j % 2) * (cell + 6 * u)
                val by = gridTop + (j / 2) * (cell + 6 * u)
                k.badge(c, sp, bx, by, cell, if (sp in have) 255 else 45)
            }
            var fy = r.bottom - pad - footLines.size * k.lineH(small)
            for (l in footLines) {
                k.centered(c, l, r.left + pad, r.width() - 2 * pad, fy, small)
                fy += k.lineH(small)
            }
            hits.add(r) { showSet(s) }
        }
        return (Treasure.SETS + 8) / 9
    }

    /** A slot tapped: an empty one takes a book being read or an ore of the bag; a full one gives it back. */
    private fun slot(ore: com.exacta.reader.data.Ore?) {
        if (ore != null && ore.tier >= 3 && !ore.inKiep && !ore.isFinish && Store.items[Treasure.TU_LINH_DAN] > 0) {
            AlertDialog.Builder(this)
                .setTitle("${Treasure.TIER_NAMES[ore.tier]} · ${ore.percent}%")
                .setItems(arrayOf("Rót Tụ Linh Đan (còn ${Store.items[Treasure.TU_LINH_DAN]})", "Lấy ra khỏi lò")) { _, w ->
                    if (w == 0) {
                        Store.useTuLinhDan(ore)
                        refresh()
                    } else {
                        Store.takeOut(ore)
                        refresh()
                    }
                }
                .setNegativeButton("Thôi", null)
                .show()
            return
        }
        if (ore != null) {
            val what = if (ore.isBook) "«${Store.books[ore.book]?.title ?: "sách"}»" else "khoáng thạch ${Treasure.TIER_NAMES[ore.tier]}"
            AlertDialog.Builder(this)
                .setTitle("Lấy $what ra khỏi lò?")
                .setMessage(if (ore.isBook) "Phần đã luyện của ô này sẽ mất." else "Khoáng thạch về lại túi, phần đã luyện thì mất.")
                .setPositiveButton("Lấy ra") { _, _ -> Store.takeOut(ore); refresh() }
                .setNegativeButton("Thôi", null)
                .show()
            return
        }
        val reading = Store.books.values.filter { it.lastOpened > 0 && Store.bookGoals(it).isNotEmpty() }.sortedByDescending { it.lastOpened }
        val ores = (Treasure.TIERS - 1 downTo 0).filter { Store.bag[it] > 0 }
        val labels = reading.map { "Sách: ${it.title} · ${it.percent}%" } + ores.map { "Khoáng thạch ${Treasure.TIER_NAMES[it]} (còn ${Store.bag[it]})" }
        if (labels.isEmpty()) {
            android.widget.Toast.makeText(this, "Chưa có sách đang đọc hay khoáng thạch trong túi", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Đặt vào lò luyện")
            .setItems(labels.toTypedArray()) { _, i ->
                if (i < reading.size) chooseGoal(reading[i]) else { Store.placeOre(ores[i - reading.size]); refresh() }
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    /** A stage of a grade, or the whole book. */
    private fun chooseGoal(b: com.exacta.reader.data.Book) {
        val goals = Store.bookGoals(b)
        val labels = goals.map { (goal, tier) ->
            if (goal == com.exacta.reader.data.Ore.GOAL_STAGE) {
                "Chặng ${Treasure.TIER_NAMES[tier]} · mỗi ${com.exacta.reader.game.Realm.van(Store.stageNeed(tier))} vạn từ một món"
            } else {
                "Cả cuốn · đọc xong thành ${Treasure.TIER_NAMES[tier]}"
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Luyện «${b.title}» thế nào?")
            .setItems(labels.toTypedArray()) { _, i ->
                Store.placeBook(b, goals[i].first, goals[i].second)
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    private fun showSet(s: Int) {
        val have = Store.kinds()
        val members = Treasure.SET_MEMBERS[s].joinToString("\n") {
            (if (it in have) "✓ " else "· ") + Treasure.speciesName(it) + " (" + Treasure.TIER_NAMES[it / Treasure.KINDS] + ")"
        }
        AlertDialog.Builder(this)
            .setTitle(Treasure.SET_NAMES[s])
            .setMessage((if (Store.setActive(s)) "Đã gom đủ bộ, hiệu quả đang có.\n\n" else "") + Treasure.SET_DETAILS[s] + "\n\n" + members)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun showBeast(b: Beast) {
        val day = if (b.bornDay >= 0) LocalDate.ofEpochDay(b.bornDay).let { " ngày ${it.dayOfMonth}/${it.monthValue}/${it.year}" } else ""
        val set = Treasure.setOf(b.species)
        AlertDialog.Builder(this)
            .setTitle(b.name)
            .setMessage(
                "${Treasure.CLANS[b.tier]} · ${Treasure.TIER_NAMES[b.tier]}\n" +
                    "Loại: ${Treasure.speciesName(b.species)}\n" +
                    (if (b.spirit == Treasure.TRIAL_MARK) "Đã qua kiếp phá lô: «${b.spirit}»\n" else if (b.spirit.isNotEmpty()) "Khí linh thức tỉnh: «${b.spirit}»\n" else "") +
                    (if (set >= 0) "Thuộc bộ: ${Treasure.SET_NAMES[set]}\n" else "") +
                    "Luyện thành$day\n" +
                    (if (b.book.isNotEmpty()) "Từ «${b.book}»" else "")
            )
            .setPositiveButton("Đóng", null)
            .show()
    }
}
