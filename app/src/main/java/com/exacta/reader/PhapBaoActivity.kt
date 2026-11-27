package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.exacta.reader.data.Beast
import com.exacta.reader.data.Store
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.PageView
import java.time.LocalDate

/**
 * Pháp bảo: the forge (three ores burning with the words read, the bag, the next gift) and the
 * treasury of forged treasures, strongest first; tap one to see it.
 */
class PhapBaoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.unseen = 0
        Store.touch()
        val a = this
        setContentView(LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(Widgets.header(a, "Pháp Bảo", "${Store.beasts.size} món"))
            addView(Widgets.rule(a), LinearLayout.LayoutParams(-1, Widgets.dp(a, 1).coerceAtLeast(1)))
            addView(ScrollView(a).apply { addView(PageView(a) { v, c, k -> page(v, c, k) }) }, LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    override fun onPause() {
        Store.save()
        super.onPause()
    }

    private fun page(v: PageView, c: Canvas?, k: Ink): Float {
        val u = k.u
        val w = k.W * u
        val m = Ink.MARGIN * u
        val small = k.small
        val smallBold = k.smallBold
        val sl = k.lineH(small)
        var y = 18 * u

        // The forge.
        y = section(c, k, y, "LÒ LUYỆN")
        val slotW = (w - 2 * m - 2 * 10 * u) / 3
        val slotH = 160 * u
        for (i in 0 until Store.FORGE_SLOTS) {
            val r = RectF(m + i * (slotW + 10 * u), y, m + i * (slotW + 10 * u) + slotW, y + slotH)
            if (c == null) continue
            k.card(c, r)
            val ore = Store.forge.getOrNull(i)
            if (ore == null) {
                k.centered(c, "Trống", r.left, r.width(), r.centerY() - sl / 2, small)
                continue
            }
            val size = 72 * u
            k.ore(c, ore.tier, ore.percent, r.centerX() - size / 2, r.top + 10 * u, size)
            var ty = r.top + 14 * u + size
            k.centered(c, Treasure.TIER_NAMES[ore.tier], r.left, r.width(), ty, smallBold); ty += sl
            k.centered(c, "${Treasure.stageName(ore.percent)} · ${ore.percent}%", r.left, r.width(), ty, small); ty += sl
            val left = (Treasure.FORGE_WORDS[ore.tier] - ore.words).coerceAtLeast(0)
            k.centered(c, "còn ${(left + 999) / 1000} nghìn từ", r.left, r.width(), ty, small)
        }
        y += slotH + 12 * u
        val bag = Store.bag.sum()
        val nextGift = (Store.giftsAwarded + 1) * Treasure.GIFT_WORDS - Store.totalWords
        val notes = listOf(
            if (bag > 0) "Túi: $bag khoáng thạch chờ vào lò" else "Túi khoáng thạch trống",
            "Quà 10 vạn từ: còn ${(nextGift.coerceAtLeast(0) + 999) / 1000} nghìn từ",
            "Đọc hết một cuốn: luyện thành một pháp bảo theo độ dày sách",
            "Bí cảnh tuần (5 ngày × 30 phút): một khoáng thạch Huyền phẩm trở lên",
            "Bảo khố: ${Store.beasts.size} món · ${Store.beasts.map { it.species }.toSet().size}/36 loại",
        )
        for (n in notes) {
            if (c != null) k.text(c, k.fit(n, w - 2 * m, small), m + 4 * u, y, small)
            y += sl + 4 * u
        }
        y += 16 * u

        // The treasury.
        y = section(c, k, y, "BẢO KHỐ")
        val beasts = Store.beasts.sortedWith(compareByDescending<Beast> { it.tier }.thenByDescending { it.bornDay })
        if (beasts.isEmpty()) {
            if (c != null) k.text(c, "Chưa có pháp bảo. Đọc sách để lò luyện thành.", m + 4 * u, y, small)
            return y + sl + 24 * u
        }
        val cols = ((k.W - 2 * Ink.MARGIN + 10) / 160).toInt().coerceIn(3, 6)
        val cellW = (w - 2 * m - (cols - 1) * 10 * u) / cols
        val art = minOf(cellW - 24 * u, 132 * u)
        val cellH = art + 2 * sl + 22 * u
        for ((i, b) in beasts.withIndex()) {
            val r = RectF(m + (i % cols) * (cellW + 10 * u), y + (i / cols) * (cellH + 10 * u), 0f, 0f)
            r.right = r.left + cellW
            r.bottom = r.top + cellH
            if (c != null) {
                k.card(c, r)
                k.badge(c, b.species, r.centerX() - art / 2, r.top + 8 * u, art)
                k.centered(c, b.name, r.left + 4 * u, r.width() - 8 * u, r.top + 12 * u + art, smallBold)
                k.centered(c, Treasure.CLANS[b.tier], r.left + 4 * u, r.width() - 8 * u, r.top + 12 * u + art + sl, small)
                v.targets.add(r to { showBeast(b) })
            }
        }
        val rows = (beasts.size + cols - 1) / cols
        return y + rows * (cellH + 10 * u) + 24 * u
    }

    private fun showBeast(b: Beast) {
        val day = if (b.bornDay >= 0) LocalDate.ofEpochDay(b.bornDay).let { " ngày ${it.dayOfMonth}/${it.monthValue}/${it.year}" } else ""
        AlertDialog.Builder(this)
            .setTitle(b.name)
            .setMessage(
                "${Treasure.CLANS[b.tier]} · ${Treasure.TIER_NAMES[b.tier]}\n" +
                    "Loại: ${Treasure.speciesName(b.species)}\n" +
                    "Luyện thành$day\n" +
                    (if (b.book.isNotEmpty()) "Từ «${b.book}»" else "")
            )
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun section(c: Canvas?, k: Ink, y: Float, title: String): Float {
        val u = k.u
        val m = Ink.MARGIN * u
        val p = k.smallBold
        if (c != null) {
            k.text(c, title, m, y, p)
            c.drawRect(m, y + k.lineH(p), k.W * u - m, y + k.lineH(p) + u, k.fill)
        }
        return y + k.lineH(p) + 10 * u
    }
}
