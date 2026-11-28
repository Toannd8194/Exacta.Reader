package com.exacta.reader.data

import com.exacta.reader.game.Realm
import com.exacta.reader.game.Treasure

/**
 * 4.1 Khai lô: a treasure takes form in six stages (Phôi liệu, Luyện hỏa, Đúc phôi, Khắc trận văn,
 * Khai phong at each fifth of its words, Thành hình at the end), then the furnace must be opened, and
 * the rarer the treasure, the harder:
 *
 *  - Hoàng phẩm (phàm binh): formed is done, it comes out at once.
 *  - Huyền, Địa phẩm: a task of reading (days of so many minutes) opens the furnace.
 *  - Thiên phẩm and above: the task, then a condition of the cultivator (a realm, a sect, a way, a
 *    công pháp, a quiet heart), then the kiếp phá lô. A condition not met may be bought instead by tụ
 *    linh: Tụ Linh Đan, the Tụ Linh trận set in the động phủ, linh thạch and tu vi paid, and a bế quan
 *    long enough held to its end.
 */
object KhaiLo {
    val STAGES = arrayOf("Phôi liệu", "Luyện hỏa", "Đúc phôi", "Khắc trận văn", "Khai phong", "Thành hình")

    /** Ore phases after the forming. */
    const val TASK = 1
    const val COND = 2
    const val KIEP = 3

    val TASK_DAYS = intArrayOf(0, 2, 3, 3, 4, 5)
    val TASK_MIN = intArrayOf(0, 15, 20, 25, 30, 30)
    val TL_MINUTES = intArrayOf(0, 0, 0, 60, 75, 90)

    class Cond(val tier: Int, val text: String, val met: () -> Boolean)

    val CONDS = listOf(
        Cond(3, "Cảnh giới từ Trúc Cơ") { Store.realmCap >= 1 },
        Cond(3, "Đã thức tỉnh linh căn") { DaoPhap.awakened() },
        Cond(3, "Đã bái nhập tông môn") { TongMon.joined() },
        Cond(3, "Tâm ma dưới 30") { TuLuyen.tamMa < 30 },
        Cond(4, "Cảnh giới từ Nguyên Anh") { Store.realmCap >= 3 },
        Cond(4, "Đệ tử nội môn trở lên") { TongMon.joined() && TongMon.rank() >= 1 },
        Cond(4, "Đã bước lên tiên lộ") { DaoLo.way >= 0 },
        Cond(4, "Công pháp đang vận từ tầng 5") { DaoPhap.activeDef() != null && DaoPhap.tang() >= 5 },
        Cond(5, "Cảnh giới từ Luyện Hư") { Store.realmCap >= 5 },
        Cond(5, "Chân truyền trở lên") { TongMon.joined() && TongMon.rank() >= 2 },
        Cond(5, "Công pháp đang vận tầng 9") { DaoPhap.activeDef() != null && DaoPhap.tang() >= 9 },
        Cond(5, "Tâm ma dưới 10") { TuLuyen.tamMa < 10 },
    )

    var rng: kotlin.random.Random = kotlin.random.Random.Default

    /** The stage 1 – 6 of a slot: by fifths of its words while forging, 6 once formed. */
    fun stage(o: Ore): Int = if (o.busy || o.percent >= 100) 6 else (1 + o.percent / 20).coerceIn(1, 5)

    fun stageName(o: Ore) = STAGES[stage(o) - 1]

    fun pickCond(tier: Int): Int = CONDS.indices.filter { CONDS[it].tier == tier.coerceIn(3, 5) }.let { it[rng.nextInt(it.size)] }

    fun condText(o: Ore) = CONDS.getOrNull(o.cond)?.text ?: ""
    fun condMet(o: Ore) = o.tuLinh == 2 || (CONDS.getOrNull(o.cond)?.met?.invoke() ?: true)

    fun taskText(tier: Int) = "${TASK_DAYS[tier]} ngày đọc từ ${TASK_MIN[tier]} phút"
    fun taskMet(o: Ore): Int = if (o.khaiStart < 0) 0 else (o.khaiStart..Store.today()).count { Store.minutes(it) >= TASK_MIN[o.tier] }

    /** What a phase asks, short, for a slot. */
    fun phaseLine(o: Ore): String = when {
        o.inKiep -> "Kiếp phá lô"
        o.khai == TASK -> "Nhiệm vụ khai lô"
        o.khai == COND && o.tuLinh == 1 -> "Tụ linh"
        o.khai == COND -> "Điều kiện khai lô"
        else -> stageName(o)
    }

    /** The steps a grade must pass after forming. */
    fun steps(tier: Int): List<String> = when {
        tier <= 0 -> listOf("Thành hình là xuất thế")
        tier <= 2 -> listOf("Nhiệm vụ khai lô: ${taskText(tier)}")
        else -> listOf("Nhiệm vụ khai lô: ${taskText(tier)}", "Điều kiện khai lô, hoặc tụ linh", "Kiếp phá lô: ${Store.kiepDays(tier)} ngày liền × ${Store.kiepMinutes(tier)} phút")
    }

    // --- Tụ linh ------------------------------------------------------------------------------------

    fun tlPills(tier: Int) = (tier - 2).coerceAtLeast(1)
    fun tlStones(tier: Int) = Store.ORE_PRICES[tier.coerceIn(0, 5)]
    fun tlTuVi(tier: Int) = 10_000L * (tier - 1).coerceAtLeast(1)
    fun tlMinutes(tier: Int) = TL_MINUTES[tier.coerceIn(0, 5)]

    /** What tụ linh asks, each with whether it is there. */
    fun needs(o: Ore): List<Pair<String, Boolean>> {
        val t = o.tier
        val m = tlMinutes(t)
        return listOf(
            "${Treasure.ITEM_NAMES[Treasure.TU_LINH_DAN]} ×${tlPills(t)} (còn ${Store.items[Treasure.TU_LINH_DAN]})" to (Store.items[Treasure.TU_LINH_DAN] >= tlPills(t)),
            "${TranPhap.TRANS[TranPhap.TU_LINH].name} đặt ở trận nhãn" to TranPhap.on(TranPhap.TU_LINH),
            "${tlStones(t)} linh thạch" to (Store.linhThach >= tlStones(t)),
            "${Realm.van(tlTuVi(t))} vạn tu vi" to (Store.tuVi() >= tlTuVi(t)),
            "Tĩnh thất cho bế quan $m phút" to (m in Store.BE_QUAN_CHOICES),
        )
    }

    fun canTuLinh(o: Ore) = o.khai == COND && o.tuLinh == 0 && !condMet(o) && needs(o).all { it.second }

    fun startTuLinh(o: Ore): Boolean {
        if (!canTuLinh(o)) return false
        val t = o.tier
        Store.items[Treasure.TU_LINH_DAN] -= tlPills(t)
        Store.pillsUsed += tlPills(t)
        Store.linhThach -= tlStones(t)
        Store.tuViLoss += tlTuVi(t)
        o.tuLinh = 1
        Store.note("Tụ linh cho lò ${Treasure.TIER_NAMES[t]}: bế quan ${tlMinutes(t)} phút để khai lô")
        Store.touch()
        return true
    }

    /** A bế quan held to its end gathers the spirit for the furnaces that wait on it. */
    fun onBeQuan(minutes: Int) {
        for (o in Store.forge) if (o.khai == COND && o.tuLinh == 1 && minutes >= tlMinutes(o.tier)) {
            o.tuLinh = 2
            Store.note("Tụ linh viên mãn: lò ${Treasure.TIER_NAMES[o.tier]} đủ linh khí khai lô")
        }
    }
}
