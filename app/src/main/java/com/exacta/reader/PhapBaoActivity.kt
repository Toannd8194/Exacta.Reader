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
    override fun right() = "${Store.beasts.size} món"
    override fun tabs() = listOf("Lò luyện", "Túi đồ", "Bảo khố", "Bộ pháp bảo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.checkSets()
        if (Store.unseen > 0 && savedInstanceState == null && !intent.hasExtra(EXTRA_TAB)) selectTab(2)
        Store.unseen = 0
        Store.touch()
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> forge(c, k, area, hits)
        1 -> bag(c, k, area, hits)
        2 -> treasury(c, k, area, page, hits)
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

    /** Túi đồ: the ores by grade, the pills and talismans (tap to use), the last kỳ ngộ. */
    private fun bag(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "KHOÁNG THẠCH · ${Store.bag.sum()} viên")
        val cw = (a.width() - 5 * 10 * u) / 6
        val ic = minOf(cw - 20 * u, 64 * u)
        for (t in 0 until Treasure.TIERS) {
            val x = a.left + t * (cw + 10 * u)
            k.ore(c, t, 0, x + (cw - ic) / 2, y, ic, alpha = if (Store.bag[t] > 0) 255 else 60)
            k.centered(c, Treasure.TIER_NAMES[t].removeSuffix(" phẩm"), x, cw, y + ic + 4 * u, small)
            k.centered(c, "× ${Store.bag[t]}", x, cw, y + ic + 4 * u + sl, sb)
        }
        y += ic + 2 * sl + 22 * u

        y = k.section(c, a.left, y, a.width(), "ĐAN DƯỢC · PHÙ")
        val gap = 12 * u
        val iw = (a.width() - gap) / 2
        val ih = 122 * u
        for (i in 0 until Treasure.ITEMS) {
            val r = RectF(a.left + (i % 2) * (iw + gap), y + (i / 2) * (ih + gap), a.left + (i % 2) * (iw + gap) + iw, y + (i / 2) * (ih + gap) + ih)
            k.card(c, r)
            val pad = 12 * u
            val pic = 64 * u
            k.art(c, "art_item_$i", r.left + pad, r.top + pad, pic, if (Store.items[i] > 0) 255 else 80)
            val tx = r.left + pad + pic + 12 * u
            val tw = r.right - pad - tx
            k.text(c, k.fit(Treasure.ITEM_NAMES[i], tw - 50 * u, k.ui10Bold), tx, r.top + pad, k.ui10Bold)
            k.right(c, "× ${Store.items[i]}", r.right - pad, r.top + pad, sb)
            var ty = r.top + pad + k.lineH(k.ui10Bold) + 2 * u
            for (l in k.wrap(Treasure.ITEM_TEXTS[i], tw, small, 3)) {
                if (ty + sl > r.bottom - 6 * u) break
                k.text(c, l, tx, ty, small)
                ty += sl
            }
            hits.add(r) { useItem(i) }
        }
        y += 2 * ih + gap + 20 * u
        if (Store.fireWords > 0) {
            k.text(c, "Thiên Hỏa Phù đang cháy: còn ${(Store.fireWords + 999) / 1000} nghìn từ lò ×2", a.left, y - 8 * u, sb)
            y += sl
        }

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
        return 1
    }

    private fun useItem(i: Int) {
        val toast = { t: String -> android.widget.Toast.makeText(this, t, android.widget.Toast.LENGTH_SHORT).show() }
        if (Store.items[i] <= 0) {
            toast("Chưa có ${Treasure.ITEM_NAMES[i]}. Đọc sách để gặp kỳ ngộ.")
            return
        }
        when (i) {
            Treasure.TU_LINH_DAN -> {
                val slots = Store.forge.filter { it.tier >= 3 && !it.inKiep && !it.isFinish }
                if (slots.isEmpty()) {
                    toast("Tụ Linh Đan chỉ dùng cho lò Thiên phẩm trở lên đang luyện")
                    return
                }
                val labels = slots.map { o ->
                    val what = if (o.isBook) "«${Store.books[o.book]?.title ?: "sách"}» chặng" else "Khoáng thạch"
                    "$what ${Treasure.TIER_NAMES[o.tier]} · ${o.percent}%"
                }
                AlertDialog.Builder(this).setTitle("Rót Tụ Linh Đan vào lò nào?")
                    .setItems(labels.toTypedArray()) { _, k -> Store.useTuLinhDan(slots[k]); refresh() }
                    .setNegativeButton("Thôi", null).show()
            }
            Treasure.THIEN_HOA_PHU -> {
                Store.useThienHoaPhu()
                toast("Thiên hỏa bừng lên: lò luyện cháy gấp đôi")
                refresh()
            }
            Treasure.PHA_CANH_DAN -> {
                if (Store.useBreakPill()) toast("Bình cảnh lung lay: thêm một ngày độ kiếp") else
                    toast("Phá Cảnh Đan chỉ dùng khi đang độ kiếp từ Phá Đan Thành Anh trở lên")
                refresh()
            }
            else -> toast("Hộ Lô Phù tự cháy khi kiếp phá lô lỡ một ngày")
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
