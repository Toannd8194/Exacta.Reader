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
    override fun tabs() = listOf("Lò luyện", "Bảo khố", "Bộ pháp bảo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.checkSets()
        if (Store.unseen > 0 && savedInstanceState == null && !intent.hasExtra(EXTRA_TAB)) selectTab(1)
        Store.unseen = 0
        Store.touch()
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int = when (tab) {
        0 -> forge(c, k, area)
        1 -> treasury(c, k, area, page, hits)
        else -> sets(c, k, area, hits)
    }

    private fun forge(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        var y = k.section(c, a.left, a.top, a.width(), "LÒ LUYỆN")
        val gap = 14 * u
        val slotW = (a.width() - 2 * gap) / 3
        val slotH = minOf(300 * u, a.height() * 0.42f)
        for (i in 0 until Store.FORGE_SLOTS) {
            val r = RectF(a.left + i * (slotW + gap), y, a.left + i * (slotW + gap) + slotW, y + slotH)
            k.card(c, r)
            val ore = Store.forge.getOrNull(i)
            if (ore == null) {
                k.ore(c, 0, 0, r.centerX() - 40 * u, r.top + 30 * u, 80 * u)
                k.centered(c, "Ô trống", r.left, r.width(), r.top + 130 * u, sb)
                continue
            }
            val size = minOf(r.width() - 40 * u, slotH - 4 * sl - 50 * u)
            k.ore(c, ore.tier, ore.percent, r.centerX() - size / 2, r.top + 16 * u, size)
            var ty = r.top + 24 * u + size
            k.centered(c, Treasure.TIER_NAMES[ore.tier], r.left, r.width(), ty, k.ui10Bold); ty += k.lineH(k.ui10Bold)
            k.centered(c, "${Treasure.stageName(ore.percent)} · ${ore.percent}%", r.left, r.width(), ty, small); ty += sl
            val left = (ore.need - ore.words).coerceAtLeast(0)
            k.centered(c, "còn ${(left + 999) / 1000} nghìn từ", r.left, r.width(), ty, small)
        }
        y += slotH + 22 * u

        // The bag, by grade.
        y = k.section(c, a.left, y, a.width(), "TÚI KHOÁNG THẠCH · ${Store.bag.sum()} viên")
        val cw = (a.width() - 5 * 10 * u) / 6
        val ic = minOf(cw - 30 * u, 56 * u)
        for (t in 0 until Treasure.TIERS) {
            val x = a.left + t * (cw + 10 * u)
            k.ore(c, t, if (Store.bag[t] > 0) 100 else 0, x + (cw - ic) / 2, y, ic)
            k.centered(c, Treasure.TIER_NAMES[t].removeSuffix(" phẩm"), x, cw, y + ic + 4 * u, small)
            k.centered(c, "× ${Store.bag[t]}", x, cw, y + ic + 4 * u + sl, sb)
        }
        y += ic + 2 * sl + 26 * u

        // How ores come.
        y = k.section(c, a.left, y, a.width(), "NGUỒN KHOÁNG THẠCH")
        val nextGift = (Store.giftsAwarded + 1) * Treasure.GIFT_WORDS - Store.totalWords
        val notes = listOf(
            "Quà mỗi 10 vạn từ: còn ${(nextGift.coerceAtLeast(0) + 999) / 1000} nghìn từ",
            "Bí cảnh tuần (${Store.biCanhNeed()} ngày × ${Store.BI_CANH_MINUTES} phút): Huyền phẩm trở lên",
            "Đọc hết một cuốn: luyện thành một pháp bảo theo độ dày sách",
            "Gom đủ một bộ pháp bảo: một khoáng thạch Địa phẩm",
        )
        for (n in notes) {
            if (y + sl > a.bottom) break
            k.diamond(c, a.left + 6 * u, y + sl / 2, 4 * u)
            k.text(c, k.fit(n, a.width() - 24 * u, small), a.left + 20 * u, y, small)
            y += sl + 8 * u
        }

        // What the sets already give.
        y += 14 * u
        if (y + 3 * sl < a.bottom) {
            y = k.section(c, a.left, y, a.width(), "HIỆU QUẢ ĐANG CÓ · ${Store.setsCount()}/${Treasure.SETS} BỘ")
            val active = (0 until Treasure.SETS).filter { Store.setActive(it) }
            if (active.isEmpty()) {
                k.text(c, "Chưa gom đủ bộ nào. Xem tab Bộ pháp bảo.", a.left, y, small)
            }
            for (set in active) {
                if (y + sl > a.bottom) break
                k.text(c, Treasure.SET_NAMES[set], a.left, y, sb)
                k.right(c, k.fit(Treasure.SET_EFFECTS[set], a.width() * 0.55f, small), a.right, y, small)
                y += sl + 8 * u
            }
        }
        return 1
    }

    private fun treasury(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val beasts = Store.beasts.sortedWith(compareByDescending<Beast> { it.tier }.thenByDescending { it.bornDay })
        if (beasts.isEmpty()) {
            k.text(c, "Chưa có pháp bảo. Đọc sách để lò luyện thành.", a.left, a.top + 10 * u, k.ui10)
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
            k.centered(c, Treasure.CLANS[b.tier], r.left + 4 * u, r.width() - 8 * u, r.top + 14 * u + art + k.lineH(sb), small)
            hits.add(r) { showBeast(b) }
        }
        return pages
    }

    private fun sets(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val have = Store.kinds()
        val gap = 12 * u
        val cols = 3
        val cw = (a.width() - (cols - 1) * gap) / cols
        val ch = (a.height() - 2 * gap) / 3
        val sb = k.smallBold
        val small = k.small
        for (s in 0 until Treasure.SETS) {
            val x = a.left + (s % cols) * (cw + gap)
            val y = a.top + (s / cols) * (ch + gap)
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
        return 1
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
                    (if (set >= 0) "Thuộc bộ: ${Treasure.SET_NAMES[set]}\n" else "") +
                    "Luyện thành$day\n" +
                    (if (b.book.isNotEmpty()) "Từ «${b.book}»" else "")
            )
            .setPositiveButton("Đóng", null)
            .show()
    }
}
