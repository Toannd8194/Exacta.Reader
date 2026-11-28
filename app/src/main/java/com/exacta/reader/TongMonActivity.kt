package com.exacta.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.Store
import com.exacta.reader.data.TongMon
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import java.time.LocalDate

/** 2.3 Tông môn, in tabs: the sect (or the five to choose from), the weekly tasks, the Bảo khố, the đại bỉ. */
class TongMonActivity : TabActivity() {
    override fun title() = "Tông Môn"
    override fun right() = if (TongMon.joined()) "${TongMon.gongs} cống hiến" else "Tán tu"
    override fun tabs() = if (TongMon.joined()) listOf("Tông môn", "Nhiệm vụ", "Bảo khố", "Đại bỉ") else listOf("Tông môn")

    override fun onCreate(savedInstanceState: Bundle?) {
        Store.checkRealm()
        super.onCreate(savedInstanceState)
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int {
        if (!TongMon.joined()) return choose(c, k, area, hits)
        return when (tab) {
            0 -> sect(c, k, area, hits)
            1 -> tasks(c, k, area, hits)
            2 -> vault(c, k, area, page, hits)
            else -> contest(c, k, area)
        }
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

    /** A card with a title, a tag, lines under it and a button on the right. */
    private fun row(c: Canvas, k: Ink, r: RectF, title: String, tag: String, lines: List<String>, button: String?, enabled: Boolean, hits: Hits, onTap: () -> Unit) {
        val u = k.u
        val pad = 14 * u
        k.card(c, r)
        val bw = if (button != null) 170 * u else 0f
        val tw = r.width() - 2 * pad - (if (bw > 0) bw + pad else 0f)
        var y = r.top + pad
        val tagW = if (tag.isNotEmpty()) k.smallBold.measureText(tag) + 12 * u else 0f
        k.text(c, k.fit(title, tw - tagW, k.ui10Bold), r.left + pad, y, k.ui10Bold)
        if (tag.isNotEmpty()) k.right(c, tag, r.left + pad + tw, y + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
        y += k.lineH(k.ui10Bold) + 2 * u
        for (l in lines) {
            if (y + k.lineH(k.small) > r.bottom - pad / 2) break
            y = para(c, k, l, r.left + pad, y, tw, k.small, 2)
        }
        if (button != null) {
            val b = RectF(r.right - pad - bw, r.centerY() - 24 * u, r.right - pad, r.centerY() + 24 * u)
            k.button(c, b, button, k.smallBold, enabled = enabled)
            if (enabled) hits.add(b) { onTap() }
        }
    }

    // --- Not in a sect yet: the sects that would take the cultivator ---------------------------------------

    /** A sect in two lines: the name and its line, then what it asks (or its text). */
    private fun sectCard(c: Canvas, k: Ink, r: RectF, i: Int, best: Boolean, hits: Hits) {
        val u = k.u
        val s = TongMon.SECTS[i]
        val pad = 14 * u
        k.card(c, r, selected = best)
        val tag = s.way + if (best) " · hợp nhất" else ""
        k.right(c, tag, r.right - pad, r.top + pad + (k.lineH(k.ui10Bold) - k.lineH(k.smallBold)) / 2, k.smallBold)
        k.line(c, s.name, r.left + pad, r.top + pad, r.width() - 2 * pad - k.smallBold.measureText(tag) - 10 * u, k.ui10Bold)
        val asks = s.reqs.joinToString(" · ") { TongMon.reqText(s, it).first }.ifEmpty { "Cửa mở cho mọi tán tu" }
        k.line(c, asks, r.left + pad, r.top + pad + k.lineH(k.ui10Bold) + 4 * u, r.width() - 2 * pad, k.small)
        hits.add(r) { joinDialog(i) }
    }

    private fun choose(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val list = if (DaoPhap.awakened() && !TongMon.banned()) TongMon.choices() else emptyList()
        var y = k.section(c, a.left, a.top, a.width(), "${TongMon.openCount()}/36 TÔNG MỞ CỬA · ${list.size} TÔNG CHỊU NHẬN")
        val later = TongMon.nextOpening()?.let { (r, n) -> " Thêm $n tông mở cửa khi tới ${Realm.baseName(r)}." } ?: ""
        val intro = when {
            !DaoPhap.awakened() -> "Linh căn thức tỉnh (Luyện Khí trung kỳ) thì các tông mới nhận đệ tử. Trước đó, đạo hữu là tán tu.$later"
            TongMon.banned() -> "${TongMon.lastLeave}. Tiếng phản đồ còn vang: tới ${date(TongMon.banUntil)} không tông nào nhận.$later"
            else -> "Tông môn mở cửa dần theo cảnh giới, và không phải tông nào cũng nhận: mỗi lúc chỉ ${TongMon.choiceCount()} tông hợp tư chất nhất chịu nhận.$later Chạm một tông để xem."
        }
        y = para(c, k, intro, a.left, y, a.width(), small, 4) + 10 * u
        val gap = 8 * u
        val h = minOf(2 * 14 * u + k.lineH(k.ui10Bold) + 4 * u + k.lineH(small), (a.bottom - y - (list.size - 1) * gap) / list.size.coerceAtLeast(1))
        for ((n, i) in list.withIndex()) {
            if (y + h > a.bottom + u) break
            sectCard(c, k, RectF(a.left, y, a.right, y + h), i, n == 0, hits)
            y += h + gap
        }
        return 1
    }

    private fun joinDialog(i: Int) {
        val s = TongMon.SECTS[i]
        val asks = s.reqs.joinToString("\n") { TongMon.reqText(s, it).let { (t, ok) -> (if (ok) "✓ " else "✗ ") + t } }.ifEmpty { "Không đòi gì." }
        val d = AlertDialog.Builder(this)
            .setTitle("Bái nhập ${s.name}?")
            .setMessage("${s.text}\n\nĐường tu: ${s.way}. Ưu đãi: ${s.perk}.\n\nTông đòi:\n$asks\n\n" +
                "Ở lâu thì lên chức, mỗi ngày đọc nhận bổng linh thạch theo chức và cống hiến. Bỏ tông là phản tông: mất cống hiến, nộp phạt linh thạch, tâm ma tăng, ${TongMon.BAN_DAYS} ngày không tông nào nhận.")
        if (TongMon.canJoin()) d.setPositiveButton("Bái nhập") { _, _ ->
            if (TongMon.join(i)) toast("Đã bái nhập ${s.name}")
            refresh()
        }
        d.setNegativeButton("Suy nghĩ thêm", null).show()
    }

    // --- The sect ---------------------------------------------------------------------------------------

    private fun sect(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val pad = 18 * u
        val s = TongMon.SECTS[TongMon.sect]
        val rank = TongMon.rank()
        val head = RectF(a.left, a.top, a.right, a.top + 2 * pad + 64 * u + 3 * sl + 10 * u)
        k.card(c, head)
        k.icon(c, "tongmon", head.left + pad, head.top + pad, 64 * u)
        val tx = head.left + 2 * pad + 64 * u
        val tw = head.right - pad - tx
        k.text(c, k.fit(s.name, tw, k.ui12Bold), tx, head.top + pad, k.ui12Bold)
        k.text(c, k.fit("${TongMon.RANKS[rank]} · ${s.way} · nhập môn ${date(TongMon.joinDay)}", tw, sb), tx, head.top + pad + k.lineH(k.ui12Bold) + 4 * u, sb)
        para(c, k, "Ưu đãi: ${s.perk}", head.left + pad, head.top + pad + 64 * u + 10 * u, head.width() - 2 * pad, small, 3)
        var y = head.bottom + 16 * u

        y = k.section(c, a.left, y, a.width(), "CỐNG HIẾN VÀ THÂN PHẬN")
        k.pair(c, "Cống hiến đang có", "${TongMon.gongs}", a.left, y, a.width()); y += sl + 6 * u
        k.pair(c, "Cống hiến đã nhận", "${TongMon.gongsTotal}", a.left, y, a.width()); y += sl + 6 * u
        val next = if (rank < 4) {
            val realm = TongMon.RANK_REALM[rank + 1]
            val days = TongMon.RANK_DAYS[rank + 1]
            "${TongMon.RANK_AT[rank + 1]} cống hiến · ${minOf(TongMon.daysIn(), days)}/$days ngày" + if (realm > 0) " · ${Realm.baseName(realm)}" else ""
        } else "Đã là bậc cao nhất"
        k.pair(c, if (rank < 4) "Lên ${TongMon.RANKS[rank + 1]}" else "Chức vụ", next, a.left, y, a.width()); y += sl + 6 * u
        k.pair(c, "Bổng ngày (đọc từ ${TongMon.DAILY_MINUTES} phút)", "${TongMon.stipend()} linh thạch · đã nhận ${TongMon.paidTotal}", a.left, y, a.width()); y += sl + 6 * u
        k.pair(c, "Bổng lộc tuần (xong cả ba nhiệm vụ)", "${TongMon.SALARY[rank]} linh thạch", a.left, y, a.width()); y += sl + 16 * u

        // The rival.
        y = k.section(c, a.left, y, a.width(), "ĐỐI THỦ ĐỒNG MÔN")
        val mine = Store.tuVi()
        val theirs = TongMon.rivalWords.toLong()
        val card = RectF(a.left, y, a.right, y + 2 * pad + k.lineH(k.ui10Bold) + 3 * sl)
        k.card(c, card, selected = mine > theirs)
        k.text(c, k.fit(TongMon.rivalName, card.width() - 2 * pad - 220 * u, k.ui10Bold), card.left + pad, card.top + pad, k.ui10Bold)
        k.right(c, TongMon.rivalRealm(), card.right - pad, card.top + pad + (k.lineH(k.ui10Bold) - sl) / 2, sb)
        var cy = card.top + pad + k.lineH(k.ui10Bold) + 2 * u
        k.text(c, "Tu vi ${Realm.van(theirs)} vạn · đạo hữu ${Realm.van(mine)} vạn", card.left + pad, cy, small); cy += sl
        val diff = mine - theirs
        k.text(c, if (diff >= 0) "Đạo hữu đang dẫn trước ${Realm.van(diff)} vạn" else "Còn kém ${Realm.van(-diff)} vạn", card.left + pad, cy, sb); cy += sl
        k.line(c, "Tu theo nhịp đọc 30 ngày của đạo hữu, nhanh hơn một phần mười.", card.left + pad, cy, card.width() - 2 * pad, small)
        y = card.bottom + 18 * u

        val b = RectF(a.left, a.bottom - 50 * u, a.right, a.bottom)
        k.button(c, b, "Phản tông (mất cống hiến, bị phạt)", sb)
        hits.add(b) { leaveDialog() }
        return 1
    }

    private fun leaveDialog() {
        val s = TongMon.SECTS[TongMon.sect]
        AlertDialog.Builder(this)
            .setTitle("Phản xuất ${s.name}?")
            .setMessage("Phản đồ phải trả giá:\n• mất toàn bộ ${TongMon.gongsTotal} cống hiến và chức ${TongMon.RANKS[TongMon.rank()]}\n" +
                "• tông truy thu ${TongMon.FINE_PERCENT}% linh thạch: ${TongMon.fine()} linh thạch\n• tâm ma +${TongMon.BETRAY_TAM_MA}\n" +
                "• ${TongMon.BAN_DAYS} ngày không tông nào nhận; ${s.name} không bao giờ nhận lại\n• ác nghiệp: nghiệp lực −10")
            .setPositiveButton("Phản tông") { _, _ ->
                if (TongMon.leave()) toast("Đã rời ${s.name}")
                selectTab(0)
                refresh()
            }
            .setNegativeButton("Ở lại", null)
            .show()
    }

    private fun date(d: Long) = if (d < 0) "–" else LocalDate.ofEpochDay(d).let { "${it.dayOfMonth}/${it.monthValue}/${it.year}" }

    // --- Weekly tasks -------------------------------------------------------------------------------------

    private fun tasks(c: Canvas, k: Ink, a: RectF, hits: Hits): Int {
        val u = k.u
        val small = k.small
        var y = k.section(c, a.left, a.top, a.width(), "NHIỆM VỤ TUẦN NÀY")
        y = para(c, k, "Mỗi thứ Hai chấp sự đường giao ba việc. Xong việc nào nhận cống hiến việc đó; xong cả ba nhận thêm bổng lộc tuần. Hết tuần chưa nhận là mất.",
            a.left, y, a.width(), small, 3) + 12 * u
        val h = 2 * 14 * u + k.lineH(k.ui10Bold) + 2 * k.lineH(small) + 8 * u
        for ((i, t) in TongMon.tasks.withIndex()) {
            val task = TongMon.TASKS[t]
            val p = TongMon.progress(t)
            val shown = when (t) {
                1 -> "${p / 60} giờ ${p % 60} phút / 5 giờ"
                4, 11 -> "${Realm.van(p.toLong())} / ${Realm.van(task.goal.toLong())} vạn chữ"
                else -> "${minOf(p, task.goal)} / ${task.goal}"
            }
            val claimed = TongMon.claimed.getOrElse(i) { false }
            val (btn, ok) = when {
                claimed -> "Đã nhận" to false
                TongMon.done(i) -> "Nhận ${task.reward}" to true
                else -> "${task.reward} cống hiến" to false
            }
            row(c, k, RectF(a.left, y, a.right, y + h), task.text, if (TongMon.done(i)) "xong" else "", listOf(shown), btn, ok, hits) {
                if (TongMon.claim(i)) toast("Cống hiến +${task.reward}")
                refresh()
            }
            y += h + 10 * u
        }
        y += 8 * u
        val b = RectF(a.left, y, a.right, y + 56 * u)
        val salary = TongMon.SALARY[TongMon.rank()]
        k.button(c, b, when {
            TongMon.bonusClaimed -> "Đã nhận bổng lộc tuần này"
            TongMon.canClaimBonus() -> "Nhận bổng lộc: 50 cống hiến, $salary linh thạch"
            else -> "Bổng lộc tuần: xong và nhận cả ba nhiệm vụ"
        }, k.smallBold, enabled = TongMon.canClaimBonus())
        if (TongMon.canClaimBonus()) hits.add(b) {
            TongMon.claimBonus()
            toast("Bổng lộc: $salary linh thạch")
            refresh()
        }
        return 1
    }

    // --- Bảo khố ------------------------------------------------------------------------------------------

    private fun vault(c: Canvas, k: Ink, a: RectF, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        var y = k.section(c, a.left, a.top, a.width(), "BẢO KHỐ · ${TongMon.gongs} CỐNG HIẾN")
        y = para(c, k, "Đổi cống hiến lấy ngọc giản, đan phương, khoáng thạch, linh mạch. Món cao cần thân phận cao.", a.left, y, a.width(), small, 2) + 10 * u
        val wares = TongMon.wares()
        val h = 24 * u + k.lineH(k.ui10Bold) + 2 * u + k.lineH(small)
        val per = ((a.bottom - y) / (h + 8 * u)).toInt().coerceAtLeast(1)
        for ((j, w) in wares.withIndex().drop(page * per).take(per)) {
            val rankOk = TongMon.rank() >= w.minRank
            val btn = if (!rankOk) "Cần ${TongMon.RANKS[w.minRank].lowercase()}" else "Đổi · ${w.price}"
            row(c, k, RectF(a.left, y, a.right, y + h), w.name, "", listOf(w.text), btn, TongMon.canBuy(j), hits) {
                val text = TongMon.buy(j)
                toast(text?.let { "Nhận $it" } ?: "Không đổi được lúc này")
                refresh()
            }
            y += h + 8 * u
        }
        return (wares.size + per - 1) / per
    }

    // --- Đại bỉ ---------------------------------------------------------------------------------------------

    private fun contest(c: Canvas, k: Ink, a: RectF): Int {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val sl = k.lineH(small)
        val q = TongMon.quarterOf(Store.today())
        val from = maxOf(TongMon.quarterStart(q), TongMon.joinDay)
        val end = TongMon.quarterStart(q + 1) - 1
        var y = k.section(c, a.left, a.top, a.width(), "TÔNG MÔN ĐẠI BỈ · MỖI QUÝ")
        y = para(c, k, "Cuối mỗi quý, chữ đọc trong quý của đạo hữu đọ với bảy đồng môn (mỗi người đọc như một người đều tay 7.000 chữ một ngày, hơn kém tùy tài). " +
            "Hạng nhất 200 cống hiến và một ngọc giản; hạng 2 – 3: 120 và một đan phương; 4 – 5: 60; còn lại 20.", a.left, y, a.width(), small, 4) + 12 * u
        k.pair(c, "Quý này (tới ${date(end)})", "${Realm.van(Store.wordsBetween(from, Store.today()))} vạn chữ", a.left, y, a.width(), sb, sb)
        y += sl + 6 * u
        val days = (Store.today() - from + 1).coerceAtLeast(1)
        k.pair(c, "Người đều tay tới hôm nay", "${Realm.van(7_000L * days)} vạn chữ", a.left, y, a.width())
        y += sl + 18 * u
        y = k.section(c, a.left, y, a.width(), if (TongMon.lastTable.isEmpty()) "CHƯA CÓ KỲ ĐẠI BỈ NÀO" else "KỲ TRƯỚC · BẠN HẠNG ${TongMon.lastRank}/8")
        val rowH = sl + 14 * u
        for ((i, pair) in TongMon.lastTable.withIndex()) {
            val me = pair.first == "Bạn"
            val r = RectF(a.left, y, a.right, y + rowH)
            if (me) k.card(c, r, selected = true)
            val p = if (me) sb else small
            k.text(c, "${i + 1}. ${pair.first}", a.left + 12 * u, y + 7 * u, p)
            k.right(c, "${Realm.van(pair.second)} vạn", a.right - 12 * u, y + 7 * u, p)
            y += rowH
        }
        return 1
    }
}
