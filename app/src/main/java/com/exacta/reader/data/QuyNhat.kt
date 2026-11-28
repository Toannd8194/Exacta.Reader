package com.exacta.reader.data

import org.json.JSONObject

/**
 * 4.0 Quy Nhất, the end of part two. From bậc 6 an envoy of another giới comes each month with a task
 * in the way of his giới; done, it gives hỗn nguyên khí, the one resource the six share. Bậc 9 and 600
 * hỗn nguyên khí open Quy Nhất: the six giới were one. A door of Hỗn Độn is left open for part three.
 */
object QuyNhat {
    const val FROM_BAC = 6
    const val QUY_AT = 600
    const val REWARD = 60
    const val TITLE = "Hỗn Nguyên Đạo Tổ"

    /** What each giới's envoy asks, in its own way of reading. */
    val ENVOY_TASK = arrayOf(
        "Đọc 20 ngày trong tháng",
        "5 đêm đọc 30 phút sau 22 giờ",
        "Giữ chuỗi 10 ngày liền",
        "3 buổi liền từ 90 phút",
        "Đọc lại 3 vạn chữ",
        "Đọc sách của 3 tác giả",
    )
    val ENVOY_GOAL = longArrayOf(20, 5, 10, 3, 30_000, 3)
    const val ENVOY_WORDS = 4

    var hnk = 0                  // hỗn nguyên khí
    var month = -1L
    var envoy = -1               // the giới of this month's envoy, or -1
    var done = false
    var envoys = 0
    var quyDay = -1L
    var quySeen = false
    var last = ""

    fun reset() {
        hnk = 0; month = -1; envoy = -1; done = false; envoys = 0; quyDay = -1; quySeen = false; last = ""
    }

    private fun today() = Store.today()
    fun near() = TienLo.opened && TienLo.bac >= FROM_BAC
    fun united() = quyDay >= 0
    fun canUnite() = TienLo.opened && TienLo.bac >= 9 && hnk >= QUY_AT && !united()
    fun envoyName(g: Int = envoy) = "Sứ giả ${TienLo.SEAT[g.coerceIn(0, 5)]}"

    /** The envoy of month `m`: the other five in turn. */
    fun envoyOf(m: Long): Int {
        val others = (0..5).filter { it != TienLo.gioi }
        return others[(m % others.size).toInt().let { if (it < 0) it + others.size else it }]
    }

    private fun read(d: Long) = Store.minutes(d) >= 1 || d in Store.sealedDays

    fun progress(to: Long = today()): Long {
        if (envoy < 0) return 0
        val days = maxOf(TienLo.monthStart(today()), TienLo.openDay)..to
        return when (envoy) {
            0 -> days.count { Store.minutes(it) >= 10 }.toLong()
            1 -> days.count { d -> (NhatKy.days[d]?.let { NhatKy.nightSeconds(it) } ?: 0) >= 30 * 60 }.toLong()
            2 -> {
                var best = 0
                var n = 0
                for (d in days) if (read(d)) { n++; best = maxOf(best, n) } else n = 0
                best.toLong()
            }
            3 -> days.sumOf { d -> NhatKy.days[d]?.sittings?.count { it >= 90 * 60 } ?: 0 }.toLong()
            4 -> days.sumOf { d -> NhatKy.days[d]?.let { it.reread + it.back } ?: 0L }
            else -> days.flatMap { d -> NhatKy.days[d]?.books?.filterValues { it >= 60 }?.keys ?: emptySet() }.toSet()
                .mapNotNull { NhatKy.bookById(it) }.map { NhatKy.key(it.author) }.filter { it.isNotEmpty() }.toSet().size.toLong()
        }
    }

    fun check(): Boolean {
        if (!near()) return false
        var changed = false
        val m = TienLo.monthOf(today())
        if (m != month) {
            month = m
            envoy = envoyOf(m)
            done = false
            Store.note("${envoyName()} ghé ${TienLo.seat()}: ${ENVOY_TASK[envoy]}")
            changed = true
        }
        if (!done && envoy >= 0 && progress() >= ENVOY_GOAL[envoy]) {
            done = true
            envoys++
            hnk += REWARD
            last = "Xong việc ${envoyName()}: +$REWARD hỗn nguyên khí"
            Store.note(last)
            changed = true
        }
        return changed
    }

    fun unite(): Boolean {
        if (!canUnite()) return false
        quyDay = today()
        quySeen = false
        Store.note("Quy Nhất: sáu giới vốn là một. Đạo hiệu «$TITLE»")
        Store.touch()
        return true
    }

    fun toJson(): JSONObject = JSONObject().put("hnk", hnk).put("month", month).put("envoy", envoy).put("done", done).put("envoys", envoys)
        .put("quyDay", quyDay).put("quySeen", quySeen).put("last", last)

    fun read(o: JSONObject) {
        reset()
        hnk = o.optInt("hnk"); month = o.optLong("month", -1); envoy = o.optInt("envoy", -1); done = o.optBoolean("done")
        envoys = o.optInt("envoys"); quyDay = o.optLong("quyDay", -1); quySeen = o.optBoolean("quySeen"); last = o.optString("last")
    }
}
