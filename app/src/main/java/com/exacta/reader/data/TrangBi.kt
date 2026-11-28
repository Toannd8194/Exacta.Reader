package com.exacta.reader.data

import com.exacta.reader.game.Treasure
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * 2.7.2 Hộ Thân Họa Phù: the slots worn on the body, and the phù lục drawn by hand.
 *
 *  - Ô trang bị: from Kim Đan the cultivator wears two pháp bảo, one more each great realm (nine at
 *    Tiên nhân). Only what is worn serves in the biến cố of the bí cảnh and in the kiếp; the bản mệnh
 *    of Linh giới is always worn besides. Before Kim Đan, the whole collection still serves.
 *  - Phù lục are drawn, not only found: phù chỉ (bought, or from the bí cảnh), chu sa (from the bí
 *    cảnh only), and one sitting of reading that holds the brush. Thần thức guides the brush.
 */
object TrangBi {
    var started = false
    val worn = ArrayList<Long>()

    fun reset() {
        started = false; worn.clear()
    }

    fun slots() = if (Store.realmCap < 2) 0 else Store.realmCap.coerceAtMost(9)
    fun active() = slots() > 0
    fun isWorn(seed: Long) = seed in worn || seed in LinhGioi.banMenh

    fun canWear(seed: Long) = active() && seed !in worn && LinhGioi.beast(seed) != null && worn.size < slots()
    fun wear(seed: Long): Boolean {
        if (!canWear(seed)) return false
        worn.add(seed)
        Store.note("Đeo ${LinhGioi.beast(seed)?.name} lên người")
        Store.touch()
        return true
    }

    fun takeOff(seed: Long): Boolean {
        if (!worn.remove(seed)) return false
        Store.touch()
        return true
    }

    /** What serves in a fight: the worn (and the bản mệnh) from Kim Đan, everything before. */
    fun serving(): List<Beast> = if (!active()) Store.beasts else Store.beasts.filter { isWorn(it.seed) }
    fun best(): Beast? = serving().maxWithOrNull(compareBy<Beast>({ it.tier }, { it.bornDay }))
    fun bestTier() = best()?.tier ?: -1
    /** Hộ thân: each worn piece past the first wards a little, 5% at most. */
    fun guard() = if (!active()) 0 else (serving().size - 1).coerceIn(0, 5)

    fun cleanLost() {
        val have = Store.beasts.map { it.seed }.toSet()
        if (worn.removeAll { it !in have }) Store.touch()
        while (worn.size > slots()) worn.removeAt(worn.size - 1)
    }

    /** First run: what the cultivator owns best is put on. */
    fun migrate() {
        reset()
        started = true
        Store.beasts.sortedWith(compareByDescending<Beast> { it.tier }.thenByDescending { it.bornDay })
            .filter { it.seed !in LinhGioi.banMenh }.take(slots()).forEach { worn.add(it.seed) }
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().put("worn", JSONArray().apply { worn.forEach { put(it) } })

    fun read(o: JSONObject) {
        reset()
        started = true
        o.optJSONArray("worn")?.let { a -> for (i in 0 until a.length()) worn.add(a.getLong(i)) }
    }
}

object PhuLuc {
    /** A phù: the item it makes, phù chỉ, chu sa, the minutes of the sitting, the thần thức trọng it asks. */
    class Phu(val item: Int, val paper: Int, val ink: Int, val minutes: Int, val trong: Int)

    val PHUS = listOf(
        Phu(1, 2, 1, 20, 0),    // Thiên Hỏa Phù
        Phu(3, 2, 1, 20, 1),    // Hộ Lô Phù
        Phu(13, 2, 1, 25, 1),   // Định Thần Phù
        Phu(11, 3, 2, 30, 2),   // Tầm Bảo Phù
        Phu(12, 3, 2, 30, 2),   // Bí Cảnh Phù
        Phu(16, 3, 2, 30, 3),   // Tụ Bảo Phù
        Phu(17, 4, 2, 40, 3),   // Luyện Hóa Phù
        Phu(15, 4, 3, 40, 4),   // Phong Ấn Phù
        Phu(14, 5, 3, 45, 5),   // Dẫn Lôi Phù
    )
    const val PAPER_PRICE = 10L

    var started = false
    var paper = 0
    var ink = 0
    var drawing = -1               // index in PHUS on the desk, or -1
    var drawChance = 0
    var made = 0
    var spoiled = 0
    var lastResult = ""
    var rng: Random = Random.Default

    fun reset() {
        started = false; paper = 0; ink = 0; drawing = -1; drawChance = 0; made = 0; spoiled = 0; lastResult = ""
    }

    fun name(i: Int) = Treasure.ITEM_NAMES[PHUS[i].item]

    /** Thần thức guides the brush: 55%, 6% a trọng above the phù's, Bách nghệ and Phù Lục Sơn +10%. */
    fun chance(i: Int): Int {
        val p = PHUS[i]
        val sect = TongMon.joined() && TongMon.SECTS[TongMon.sect].name == "Phù Lục Sơn"
        return (55 + 6 * (TuLuyen.trong() - p.trong) + (if (DaoLo.walks(DaoLo.BACH)) 10 else 0) + (if (sect) 10 else 0)).coerceIn(20, 95)
    }

    fun canDraw(i: Int) = drawing < 0 && TuLuyen.trong() >= PHUS[i].trong && paper >= PHUS[i].paper && ink >= PHUS[i].ink

    fun start(i: Int): Boolean {
        if (!canDraw(i)) return false
        val p = PHUS[i]
        paper -= p.paper
        ink -= p.ink
        drawing = i
        drawChance = chance(i)
        Store.note("Trải phù chỉ vẽ ${name(i)}: buổi đọc tới phải liền ${p.minutes} phút")
        Store.touch()
        return true
    }

    /** Puts the brush down before reading: the paper and the chu sa come back. */
    fun cancel() {
        if (drawing < 0) return
        paper += PHUS[drawing].paper
        ink += PHUS[drawing].ink
        drawing = -1
        Store.touch()
    }

    fun buyPaper(): Boolean {
        if (Store.linhThach < PAPER_PRICE) return false
        Store.linhThach -= PAPER_PRICE
        paper++
        Store.touch()
        return true
    }

    fun onSeconds(session: Long) {
        if (drawing >= 0 && session >= PHUS[drawing].minutes * 60L) finish()
    }

    /** A sitting ended before the phù was done: the stroke broke, the materials are spent. */
    fun onSessionEnd(seconds: Long) {
        if (drawing < 0 || seconds < 60) return
        spoiled++
        lastResult = "Nét phù đứt: ${name(drawing)} hỏng, buổi đọc dừng ở ${seconds / 60} phút"
        drawing = -1
        Store.note(lastResult)
        Store.touch()
    }

    private fun finish() {
        val i = drawing
        val roll = rng.nextInt(100)
        drawing = -1
        if (roll < drawChance) {
            val n = if (drawChance - roll >= 40) 2 else 1
            Store.items[PHUS[i].item] += n
            made++
            lastResult = "Thành phù: $n tấm ${name(i)}"
        } else {
            spoiled++
            lastResult = "Phù không linh: ${name(i)} hỏng (tỉ lệ $drawChance%)"
        }
        Store.note(lastResult)
        Store.touch()
    }

    /** The bí cảnh of the week gives phù chỉ and chu sa. */
    fun onBiCanh() {
        if (!started) return
        paper += 2
        ink += 1 + rng.nextInt(2)
        Store.touch()
    }

    fun migrate() {
        reset()
        started = true
        paper = 3
        ink = 1
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().put("paper", paper).put("ink", ink).put("drawing", drawing).put("chance", drawChance)
        .put("made", made).put("spoiled", spoiled).put("last", lastResult)

    fun read(o: JSONObject) {
        reset()
        started = true
        paper = o.optInt("paper"); ink = o.optInt("ink"); drawing = o.optInt("drawing", -1); drawChance = o.optInt("chance")
        made = o.optInt("made"); spoiled = o.optInt("spoiled"); lastResult = o.optString("last")
    }
}
