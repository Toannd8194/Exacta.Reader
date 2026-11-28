package com.exacta.reader.data

import com.exacta.reader.game.Treasure
import org.json.JSONObject

/**
 * 2.7.1 Tế Luyện Bố Trận: the cấm chế of the pháp bảo, and the trận pháp of the động phủ.
 *
 *  - Each pháp bảo can be refined through 36 tầng cấm chế, one at a time, with the words read while
 *    it is on the refining fire: tầng n asks 2.000 + 1.000 × n words. Tầng 1 – 9 make a pháp khí,
 *    10 – 18 a pháp bảo, 19 – 27 a linh bảo, 28 – 36 a thông thiên linh bảo, which only the linh khí
 *    of Linh giới can refine. Crossing into a new grade asks an ore and linh thạch.
 *  - The most refined pháp bảo carries the cultivator: linh lực, the breakthrough, the bí cảnh, the
 *    lôi kiếp of Độ Kiếp.
 */
object TeLuyen {
    val GRADES = arrayOf("Phàm vật", "Pháp khí", "Pháp bảo", "Linh bảo", "Thông thiên linh bảo")
    const val MAX = 36

    var started = false
    val levels = HashMap<Long, Int>()        // seed -> tầng cấm chế
    var active = -1L                          // the pháp bảo on the refining fire
    var words = 0L                            // words into its next tầng

    fun reset() {
        started = false; levels.clear(); active = -1; words = 0
    }

    fun level(seed: Long) = levels[seed] ?: 0
    fun grade(level: Int) = (level + 8) / 9               // 0 phàm, 1 … 4
    fun gradeName(seed: Long) = GRADES[grade(level(seed))]
    fun need(level: Int) = 2_000L + 1_000L * level         // words for tầng level + 1

    /** Crossing into a new grade (tầng 10, 19, 28) asks an ore and linh thạch; 28 asks Linh giới. */
    fun gate(level: Int) = level == 9 || level == 18 || level == 27
    fun gateOreTier(seed: Long) = minOf(LinhGioi.beast(seed)?.tier ?: 0, 3)
    fun gatePrice(level: Int) = when (level) { 9 -> 50L; 18 -> 150L; else -> 400L }

    fun canRefine(seed: Long) = LinhGioi.beast(seed) != null && level(seed) < MAX

    fun refine(seed: Long): Boolean {
        if (!canRefine(seed) || active == seed) return false
        active = seed
        words = 0
        Store.note("Tế luyện ${LinhGioi.beast(seed)?.name}: chữ đọc thành lửa luyện cấm chế")
        Store.touch()
        return true
    }

    fun stop() {
        active = -1
        words = 0
        Store.touch()
    }

    /** The words of a page also fire the refining; at a gate they wait for the ore. */
    fun onWords(read: Long) {
        if (active < 0 || read <= 0) return
        // 3.1: a công pháp of tế luyện speeds the furnace.
        val n = read * (100 + com.exacta.reader.game.PhapMon.effectValue(com.exacta.reader.game.PhapMon.E_TE_LUYEN, DaoPhap.lv(com.exacta.reader.game.PhapMon.E_TE_LUYEN))) / 100
        val b = LinhGioi.beast(active)
        if (b == null) { stop(); return }
        var left = n
        while (left > 0 && active >= 0) {
            val lv = level(active)
            if (lv >= MAX) { stop(); return }
            val take = minOf(left, need(lv) - words)
            words += take
            left -= take
            if (words < need(lv) || gate(lv)) break
            up()
        }
    }

    /** At the end of a grade: what it asks, and whether it is there. */
    fun gateReady(): Boolean {
        val lv = level(active)
        if (active < 0 || !gate(lv) || words < need(lv)) return false
        if (lv == 27 && Store.realmCap < 5) return false
        return (gateOreTier(active) until Treasure.TIERS).any { Store.bag[it] > 0 } && Store.linhThach >= gatePrice(lv)
    }

    fun breakGate(): Boolean {
        if (!gateReady()) return false
        val lv = level(active)
        val t = (gateOreTier(active) until Treasure.TIERS).first { Store.bag[it] > 0 }
        Store.bag[t]--
        Store.linhThach -= gatePrice(lv)
        up()
        return true
    }

    private fun up() {
        val lv = level(active) + 1
        levels[active] = lv
        words = 0
        val b = LinhGioi.beast(active)
        if (lv % 9 == 1 || lv == MAX) Store.note("${b?.name} thành ${GRADES[grade(lv)].lowercase()} (cấm chế tầng $lv)")
        if (lv >= MAX) {
            Store.note("${b?.name}: 36 tầng cấm chế viên mãn, thông thiên linh bảo")
            active = -1
        }
        Store.touch()
    }

    // --- What the most refined pháp bảo gives --------------------------------------------------------

    fun bestLevel() = Store.beasts.maxOfOrNull { level(it.seed) } ?: 0
    fun bestGrade() = grade(bestLevel())
    /** Linh lực: 1% each 4 tầng of the most refined pháp bảo, 9% at most. */
    fun speedBonus() = bestLevel() / 4
    /** Đột phá: 1% a grade. */
    fun breakPart() = bestGrade()

    fun cleanLost() {
        val have = Store.beasts.map { it.seed }.toSet()
        if (levels.keys.retainAll(have) or (active >= 0 && active !in have)) {
            if (active !in have) { active = -1; words = 0 }
            Store.touch()
        }
    }

    fun migrate() {
        reset()
        started = true
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().put("levels", JSONObject().apply { levels.forEach { (s, l) -> put(s.toString(), l) } })
        .put("active", active).put("words", words)

    fun read(o: JSONObject) {
        reset()
        started = true
        o.optJSONObject("levels")?.let { m -> for (k in m.keys()) levels[k.toLong()] = m.getInt(k) }
        active = o.optLong("active", -1); words = o.optLong("words")
    }
}

/** 2.7.1 Trận pháp: built in the động phủ with linh thạch, set in the trận nhãn the linh mạch opens. */
object TranPhap {
    class Tran(val name: String, val good: String, val price: Long, val giai: Int)

    const val TU_LINH = 0
    const val HO_SON = 1
    const val ME_TUNG = 2
    const val THANH_TAM = 3
    const val DUONG_LINH = 4
    const val DAN_HOA = 5
    const val TU_BAO = 6

    val TRANS = listOf(
        Tran("Tụ Linh Trận", "linh lực +10%", 300, 1),
        Tran("Hộ Sơn Trận", "đỡ một đạo lôi: mỗi kỳ kiếp lỡ thêm một ngày; lôi kiếp Độ Kiếp +8%", 400, 2),
        Tran("Mê Tung Trận", "bí cảnh tuần bớt nguy: liều đánh, cưỡng phá +10%", 250, 1),
        Tran("Thanh Tâm Trận", "tâm ma tăng chậm 20%", 350, 2),
        Tran("Dưỡng Linh Trận", "dược viên +5 năm dược linh mỗi ngày", 300, 2),
        Tran("Đan Hỏa Trận", "luyện đan +8%", 300, 3),
        Tran("Tụ Bảo Trận", "lò luyện cháy nhanh 15%", 450, 4),
    )

    var started = false
    val built = LinkedHashSet<Int>()
    val set = LinkedHashSet<Int>()

    fun reset() {
        started = false; built.clear(); set.clear()
    }

    /** Trận nhãn: one, two from Tứ giai of linh mạch, three from Thất giai. */
    fun eyes() = 1 + (DongPhu.giai - 1) / 3
    fun on(t: Int) = t in set

    fun canBuild(t: Int) = t !in built && DongPhu.giai >= TRANS[t].giai && Store.linhThach >= TRANS[t].price
    fun build(t: Int): Boolean {
        if (!canBuild(t)) return false
        Store.linhThach -= TRANS[t].price
        built.add(t)
        if (set.size < eyes()) set.add(t)
        Store.note("Bố trí ${TRANS[t].name}: ${TRANS[t].good}")
        Store.touch()
        return true
    }

    /** Sets a trận built into an eye, or takes it out. */
    fun toggle(t: Int): Boolean {
        if (t !in built) return false
        if (t in set) set.remove(t) else if (set.size < eyes()) set.add(t) else return false
        Store.touch()
        return true
    }

    fun speedBonus() = if (on(TU_LINH)) 10 else 0
    fun stirPercent() = if (on(THANH_TAM)) 80 else 100
    fun spare() = if (on(HO_SON)) 1 else 0
    fun kiepPart() = if (on(HO_SON)) 8 else 0
    fun biCanhPart() = if (on(ME_TUNG)) 10 else 0
    fun years() = if (on(DUONG_LINH)) 5 else 0
    fun brew() = if (on(DAN_HOA)) 8 else 0
    fun burnPercent() = if (on(TU_BAO)) 115 else 100

    fun migrate() {
        reset()
        started = true
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().put("built", org.json.JSONArray().apply { built.forEach { put(it) } })
        .put("set", org.json.JSONArray().apply { set.forEach { put(it) } })

    fun read(o: JSONObject) {
        reset()
        started = true
        o.optJSONArray("built")?.let { a -> for (i in 0 until a.length()) built.add(a.getInt(i)) }
        o.optJSONArray("set")?.let { a -> for (i in 0 until a.length()) set.add(a.getInt(i)) }
    }
}
