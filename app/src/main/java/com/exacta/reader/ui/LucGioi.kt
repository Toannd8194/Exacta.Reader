package com.exacta.reader.ui

import android.app.Activity
import android.graphics.Canvas
import android.graphics.RectF
import androidx.appcompat.app.AlertDialog
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.Store
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.PhapMon

/**
 * Lục Giới cảm ứng: the six realms and how strongly each answers the life of cultivation so far.
 * Not chosen; the first one is where the cultivator ascends. A tap shows how a realm counts.
 */
object LucGioi {
    /** Draws the six rows in `a`; returns the y below them. */
    fun draw(c: Canvas, k: Ink, a: RectF, hits: Hits, activity: Activity): Float {
        val u = k.u
        val small = k.small
        val sb = k.smallBold
        val scores = TuLuyen.GIOI.indices.map { TuLuyen.gioiScore(it) }
        val top = scores.maxOrNull()?.coerceAtLeast(1) ?: 1
        val lead = if (TuLuyen.gioi >= 0) TuLuyen.gioi else TuLuyen.destined()
        val gap = 8 * u
        val pad = 12 * u
        val rowH = ((a.bottom - a.top - 5 * gap) / 6).coerceAtMost(2 * pad + k.lineH(k.ui10Bold) + 6 * u + 14 * u + 8 * u)
        var y = a.top
        for (i in TuLuyen.GIOI.indices) {
            val r = RectF(a.left, y, a.right, y + rowH)
            k.card(c, r, selected = i == lead)
            val kind = (if (i == lead) "◆ " else "") + "thành ${TuLuyen.TIEN_KIND[i]}"
            k.right(c, kind, r.right - pad, r.top + pad + (k.lineH(k.ui10Bold) - k.lineH(sb)) / 2, sb)
            k.line(c, TuLuyen.GIOI[i], r.left + pad, r.top + pad, r.width() - 2 * pad - sb.measureText(kind) - 10 * u, k.ui10Bold)
            val my = r.top + pad + k.lineH(k.ui10Bold) + 6 * u
            val num = "${scores[i]} điểm"
            val nw = sb.measureText("000 điểm") + 10 * u
            k.meter(c, r.left + pad, my + (k.lineH(small) - 14 * u) / 2, r.width() - 2 * pad - nw, scores[i].toFloat() / top, 14 * u)
            k.right(c, num, r.right - pad, my, sb)
            hits.add(r) { dialog(activity, i) }
            y = r.bottom + gap
        }
        return y
    }

    /** What realm i counts, and what it gives now. */
    fun explain(i: Int): String {
        val attrs = TuLuyen.GIOI_ATTR[i].joinToString(", ") { PhapMon.attrName(it) }
        val tam = TuLuyen.GIOI_TAM[i].joinToString(", ") { PhapMon.TAM_PHAP[it].name }
        val way = TuLuyen.GIOI_WAY[i].takeIf { it >= 0 }?.let { DaoLo.WAYS[it].name }
        val sect = TuLuyen.GIOI_SECT[i].takeIf { it >= 0 }?.let { "theo ${TongMon.WAYS[it]} (như ${TongMon.SECTS[it].name})" }
        val now = TuLuyen.gioiParts(i)
        return buildString {
            append(TuLuyen.GIOI_TEXT[i])
            append("\n\nCảm ứng hiện tại: ${TuLuyen.gioiScore(i)} điểm")
            if (now.isEmpty()) append(".") else for (p in now) append("\n· ${p.first} +${p.second}")
            append("\n\nCách tính:\n· Linh căn $attrs: căn mạnh nhất 30, rồi 18, 12, 8, 6")
            append("\n· Công pháp $attrs đang vận: 20, đại thành thêm 5")
            if (tam.isNotEmpty()) append("\n· Tâm pháp $tam nhập môn: ${if (i == 1) 15 else 8}")
            if (way != null) append("\n· Đạo lộ $way: 20, kiêm tu 10")
            append("\n· Đời tu: ${TuLuyen.GIOI_LIFE[i]}")
            if (sect != null) append("\n· Tông môn $sect: 10")
            val dans = DaoLo.DANS.filter { it.gioi == i }.joinToString(", ") { it.name }
            val thans = DaoLo.THANS.filter { it.gioi == i }.joinToString(", ") { it.name }
            if (dans.isNotEmpty()) append("\n· Kim Đan $dans: 10")
            if (thans.isNotEmpty()) append("\n· Hóa Thần $thans: 10")
            append("\n· Nghiệp lực (từ Phi Thăng): ${DaoLo.KARMA_TEXT[i]}")
            val by = com.exacta.reader.data.DoKiep.BY.indices.filter { com.exacta.reader.data.DoKiep.BY_GIOI[it] == i }.joinToString(", ") { com.exacta.reader.data.DoKiep.BY[it] }
            if (by.isNotEmpty()) append("\n· Kiếp ấn vượt nhờ $by: 3 mỗi ấn")
            if (i == 4) append("\n· Chuyển thế ở chế độ Chân Tu: 10 mỗi lần")
            append("\n\nKhông chọn được: giới nào đáp lại mạnh nhất thì phi thăng về đó.")
        }
    }

    fun dialog(activity: Activity, i: Int) {
        AlertDialog.Builder(activity).setTitle(TuLuyen.GIOI[i]).setMessage(explain(i)).setPositiveButton("Đóng", null).show()
    }

    /** One line on where the cultivation leads now. */
    fun summary(): String {
        if (Store.realmCap >= 9 && TuLuyen.gioi >= 0) return "Đã phi thăng ${TuLuyen.GIOI[TuLuyen.gioi]}"
        val d = TuLuyen.destined()
        return "Đang hướng về ${TuLuyen.GIOI[d]} · hơn giới kế tiếp ${TuLuyen.gioiLead()} điểm"
    }
}
