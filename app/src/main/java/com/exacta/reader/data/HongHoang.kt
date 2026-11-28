package com.exacta.reader.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 4.0 Hồng Hoang Cổ Địa, the giới of the long sitting: the body tempered by a thousand hours of reading.
 *
 *  - Khí huyết ([NhatKy.estimate]): a point each 10 minutes of a sitting of 45 minutes or more, twice from
 *    90 minutes, three times from 120. Short sittings make none.
 *  - Huyết mạch: at bậc 2 an old beast's blood wakes, by the way of reading since the crossing (Long the
 *    very long sittings, Phượng the dawn, Kỳ Lân books read through, Huyền Vũ the chain, Bạch Hổ the
 *    words of a day, Côn Bằng the authors). At bậc 6 it takes form (hóa hình): its gift twice.
 *  - Săn hoang thú: a beast each week, brought down by three sittings of 45 minutes.
 *  - Tổ địa (Tế đàn, Thú lan, Đấu trường), the factions Long tộc, Phượng tộc, Kỳ Lân tộc, the Phản tổ
 *    kiếp (sittings of 90 minutes) and the Đại kiếp Chiến Tổ (sittings of 120).
 */
object HongHoang : GioiRules {
    override val CHUC_LABEL = "chiến văn"
    override val SEAT_NAME = "Tổ địa"
    override val GREET = "Đất hoang thuở trời đất mới mở, tiếng thú gầm vọng khắp núi non."
    override val END_LINE = "Vạn thú cúi đầu, đất hoang rung chuyển dưới chân."
    override val CHUC = arrayOf("Đồng Bì Văn", "Thiết Cốt Văn", "Ngân Huyết Văn", "Kim Thân Văn", "Long Lân Văn", "Phượng Vũ Văn", "Kỳ Lân Văn", "Bàn Cổ Văn", "Chiến Tổ Văn")
    override val CHUC_PERK = arrayOf(
        "Khí huyết chỉ sinh từ buổi liền từ 45 phút; buổi 90 phút gấp đôi, 120 phút gấp ba",
        "Huyết mạch cổ thú thức tỉnh theo thói quen đọc",
        "Khí huyết +3%",
        "Săn hoang thú thêm 5 khí huyết",
        "Tiên kiếp +5%",
        "Hóa hình: ưu đãi huyết mạch nhân đôi",
        "Việc thế lực +5 cống hiến",
        "Khí huyết +3% nữa",
        "Đạo hiệu tối thượng «Hồng Hoang Chiến Tổ»",
    )
    override val HALLS = arrayOf("Tế đàn", "Thú lan", "Đấu trường")
    override val HALL_TEXT = arrayOf(
        "Tế đàn huyết mạch: mỗi cấp, huyết mạch đã thức tỉnh thêm 2% khí huyết.",
        "Bản mệnh thành thú đồng hành: mỗi cấp +1 cấp tiên khí (+1% khí huyết).",
        "Đấu trường luyện chiến: mỗi cấp tiên kiếp +2%.",
    )
    override val FACTIONS = arrayOf("Long tộc", "Phượng tộc", "Kỳ Lân tộc")
    override val FACTION_TEXT = arrayOf(
        "Tộc rồng giữa biển mây, trọng sức bền. Khí huyết +5%.",
        "Tộc phượng trên núi lửa, trọng săn bắt. Hạ hoang thú thêm 5 khí huyết.",
        "Tộc kỳ lân nơi rừng cổ, trọng điềm lành. Tiên kiếp +5%.",
    )
    override val TASKS = arrayOf(
        "8 buổi liền từ 45 phút",
        "Một buổi liền từ 120 phút",
        "Hạ 2 hoang thú",
        "15 ngày đọc từ 30 phút",
        "Đọc 15 vạn chữ",
        "4 buổi liền từ 90 phút",
    )
    override val TASK_GOAL = longArrayOf(8, 1, 2, 15, 150_000, 4)
    override val WORD_TASK = 4
    override val KIEP_NAME = arrayOf("Phản tổ kiếp thứ nhất", "Phản tổ kiếp thứ hai", "Đại kiếp Chiến Tổ")
    override val KIEP_RULE = arrayOf(
        "7 ngày trong 14 ngày, mỗi ngày một buổi liền từ 90 phút",
        "7 ngày trong 14 ngày, mỗi ngày một buổi liền từ 90 phút",
        "20 ngày trong 30 ngày, mỗi ngày một buổi liền từ 120 phút",
    )

    // --- Huyết mạch ---------------------------------------------------------------------------------

    val MACH = arrayOf("Long", "Phượng", "Kỳ Lân", "Huyền Vũ", "Bạch Hổ", "Côn Bằng")
    val MACH_WAY = arrayOf("buổi rất dài", "đọc sáng sớm", "đọc trọn nhiều cuốn", "chuỗi ngày dài", "nhiều chữ một ngày", "nhiều tác giả")
    val MACH_PERK = arrayOf(
        "Khí huyết +6%",
        "Ngày đọc sáng sớm (4 – 7 giờ) từ 20 phút: +2 khí huyết",
        "Mỗi cuốn đọc trọn: +10 khí huyết",
        "Tiên kiếp +5%",
        "Ngày đọc từ 2 vạn chữ: +2 khí huyết",
        "Mỗi tác giả mới đọc trọn: +15 khí huyết",
    )

    var mach = -1                // the huyết mạch woken, or -1

    /** The gift is twice once it takes form. */
    fun machTimes() = if (hasChuc(5)) 2 else 1

    /** How far the way of reading since the crossing leans to each blood. */
    fun scores(): IntArray {
        val from = TienDinh.crossDay.coerceAtLeast(0)
        val to = Store.today()
        val s = NhatKy.summary(from, to)
        var chain = 0
        var best = 0
        for (d in from..to) if (Store.minutes(d) >= 1 || d in Store.sealedDays) { chain++; best = maxOf(best, chain) } else chain = 0
        return intArrayOf(
            s.long90 * 3,
            (s.dawn / 1800).toInt(),
            Store.books.values.count { it.finishDay in from..to } * 4,
            best / 3,
            (from..to).count { (Store.dayWords[it] ?: 0) >= 20_000 } * 2,
            s.authors * 2,
        )
    }

    fun leaning(): Int = scores().let { sc -> sc.indices.maxByOrNull { sc[it] } ?: 0 }

    private fun awaken(): Boolean {
        if (mach >= 0 || !hasChuc(1)) return false
        mach = leaning()
        Store.note("Huyết mạch ${MACH[mach]} thức tỉnh")
        return true
    }

    // --- Săn hoang thú ------------------------------------------------------------------------------

    val BEASTS = arrayOf("Thao Thiết", "Cùng Kỳ", "Đào Ngột", "Cửu Anh", "Tất Phương", "Toan Nghê", "Bá Hạ", "Hoang Cổ Cự Viên", "Cửu Đầu Xà", "Lôi Thú", "Hỏa Lân Sư", "Băng Nguyên Cự Tượng")
    const val HUNT_SITTINGS = 3
    const val HUNT_REWARD = 15
    const val HUNT_TT = 3L

    var killWeek = -1L
    var kills = 0
    val killDays = ArrayList<Long>()
    var lastHunt = ""

    fun beastOf(week: Long) = BEASTS[((week / 7) % BEASTS.size).toInt().let { if (it < 0) it + BEASTS.size else it }]
    private fun long45(d: Long) = NhatKy.days[d]?.sittings?.count { it >= 45 * 60 } ?: 0

    /** Sittings of 45 minutes this week. */
    fun huntProgress(): Int {
        val w = Store.weekStart(Store.today())
        return (maxOf(w, TienLo.openDay)..Store.today()).sumOf { long45(it) }
    }

    fun huntReward() = HUNT_REWARD + (if (hasChuc(3)) 5 else 0) + (if (TienLo.faction == 1) 5 else 0)
    fun killedThisWeek() = killWeek == Store.weekStart(Store.today())

    private fun hunt(): Boolean {
        if (killedThisWeek() || huntProgress() < HUNT_SITTINGS) return false
        val w = Store.weekStart(Store.today())
        killWeek = w
        kills++
        killDays.add(Store.today())
        TienLo.resTenths += 10L * huntReward()
        TienLo.tienThach += HUNT_TT
        lastHunt = "Hạ ${beastOf(w)}: +${huntReward()} khí huyết, +$HUNT_TT tiên thạch"
        Store.note(lastHunt)
        return true
    }

    override fun reset() {
        mach = -1; killWeek = -1; kills = 0; killDays.clear(); lastHunt = ""
    }

    // --- The common hooks ----------------------------------------------------------------------------

    override fun parts(): List<Pair<String, Int>> = buildList {
        if (mach == 0) add("Huyết mạch Long" to 6 * machTimes())
        if (mach >= 0 && TienLo.halls[0] > 0) add("Tế đàn cấp ${TienLo.halls[0]}" to 2 * TienLo.halls[0])
        if (hasChuc(2)) add("Chiến văn ${CHUC[2]}" to 3)
        if (hasChuc(7)) add("Chiến văn ${CHUC[7]}" to 3)
        if (TienLo.faction == 0) add("Thế lực ${FACTIONS[0]}" to 5)
        if (TienLo.faction >= 0 && TienLo.rank() > 0) add("${FACTIONS[TienLo.faction]} · ${TienLo.RANKS[TienLo.rank()]}" to TienLo.rank())
    }

    override fun dayExtra(d: Long): Long {
        val n = machTimes()
        return 10L * n * when (mach) {
            1 -> if ((NhatKy.days[d]?.let { NhatKy.dawnSeconds(it) } ?: 0) >= 20 * 60) 2 else 0
            2 -> 10 * Store.books.values.count { it.finishDay == d }
            4 -> if ((Store.dayWords[d] ?: 0) >= 20_000) 2 else 0
            5 -> 15 * Store.books.values.filter { it.finishDay == d && it.author.isNotBlank() }
                .map { NhatKy.key(it.author) }.distinct().count { NhatKy.authorFirstFinish(it) == d }
            else -> 0
        }
    }

    override fun kiepParts(): List<Pair<String, Int>> = buildList {
        if (mach == 3) add("Huyết mạch Huyền Vũ" to 5 * machTimes())
        if (TienLo.halls[2] > 0) add("Đấu trường cấp ${TienLo.halls[2]}" to 2 * TienLo.halls[2])
        if (TienLo.faction == 2) add("Thế lực ${FACTIONS[2]}" to 5)
        if (hasChuc(4)) add("Chiến văn ${CHUC[4]}" to 5)
    }

    override fun taskBonus() = if (hasChuc(6)) 5 else 0
    override fun tienKhiBonus() = TienLo.halls[1]
    override fun onOpen() {}

    override fun check(): Boolean {
        var changed = false
        if (awaken()) changed = true
        if (hunt()) changed = true
        return changed
    }

    override fun taskProgress(task: Int, from: Long, to: Long): Long {
        val days = from..to
        return when (task) {
            0 -> days.sumOf { long45(it) }.toLong()
            1 -> days.sumOf { d -> NhatKy.days[d]?.sittings?.count { it >= 120 * 60 } ?: 0 }.toLong()
            2 -> killDays.count { it in days }.toLong()
            3 -> days.count { Store.minutes(it) >= 30 }.toLong()
            4 -> Store.wordsBetween(from, to)
            else -> days.sumOf { d -> NhatKy.days[d]?.sittings?.count { it >= 90 * 60 } ?: 0 }.toLong()
        }
    }

    override fun kiepDayKept(i: Int, d: Long): Boolean {
        val need = if (i == 2) 120 * 60 else 90 * 60
        return NhatKy.days[d]?.sittings?.any { it >= need } ?: false
    }

    override fun kiepEnd(i: Int) = true
    override fun kiepWindow(i: Int) = if (i == 2) 30 else 14
    override fun kiepNeed(i: Int) = if (i == 2) 20 else 7

    override fun toJson(): JSONObject = JSONObject().put("mach", mach).put("killWeek", killWeek).put("kills", kills)
        .put("killDays", JSONArray().apply { killDays.forEach { put(it) } }).put("last", lastHunt)

    override fun read(o: JSONObject) {
        reset()
        mach = o.optInt("mach", -1); killWeek = o.optLong("killWeek", -1); kills = o.optInt("kills")
        o.optJSONArray("killDays")?.let { a -> for (i in 0 until a.length()) killDays.add(a.getLong(i)) }
        lastHunt = o.optString("last")
    }
}
