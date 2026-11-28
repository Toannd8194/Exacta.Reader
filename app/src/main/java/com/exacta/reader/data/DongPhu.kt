package com.exacta.reader.data

import com.exacta.reader.game.DanDao
import com.exacta.reader.game.DanDao.HERBS
import com.exacta.reader.game.DanDao.RECIPES
import com.exacta.reader.game.Realm
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * 2.2 Động Phủ Khai Lô: the cave of the cultivator.
 *
 *  - Linh mạch, Nhất giai to Cửu giai, raised with linh thạch: each giai adds linh lực and the
 *    chance of a breakthrough, and the herbs grow faster; more giai open more luống.
 *  - Dược viên: a seed sown in a luống gains dược linh (năm) on every day of 30 minutes read.
 *    A herb of a hundred năm asks ten days, one of a thousand a hundred: the high pills ask patience.
 *  - Đan phòng: an đan phương, its herbs, and hỏa hầu: the next sitting of reading must last its
 *    minutes. Leaving early is nổ lò, the herbs are lost. Đan đạo cấp (Nhất … Cửu phẩm luyện đan sư)
 *    grows with the pills made and raises the chance and the phẩm.
 *  - Đan độc: every pill leaves some (the higher its phẩm, the less). Over 60 the pills work half
 *    and căn cơ wears down; it fades on every day of 30 minutes read, or with Giải Độc Đan.
 */
object DongPhu {
    const val DOC_HIGH = 60
    const val DOC_FADE = 5
    const val SHOP_PILL_DOC = 4

    class Plot(var herb: Int = -1, var day: Long = -1)

    var started = false
    var giai = 1
    val plots = Array(6) { Plot() }
    val seeds = IntArray(HERBS.size)
    val herbs = ArrayList<Pair<Int, Int>>()      // (kind, năm)
    val known = LinkedHashSet<Int>()             // đan phương
    var brew = -1
    var brewChance = 0
    var brewDay = -1L
    val brewHerbs = ArrayList<Pair<Int, Int>>()
    val pills = LinkedHashMap<Int, Int>()        // recipe * 4 + phẩm -> count
    var made = 0
    var failed = 0
    var burst = 0
    var danDoc = 0
    var lastTick = -1L
    val breakPill = IntArray(10)                 // the bonus a phá cảnh pill gives to its gate
    var theBonus = 0
    var thanBonus = 0                            // thần thức from Khai Thức and Dưỡng Thần Đan
    var canCoGain = 0                            // căn cơ from Bồi Nguyên Đan, 15 at most
    var lastResult = ""
    var rng: Random = Random.Default

    private fun today() = Store.today()

    fun reset() {
        started = false; giai = 1; plots.forEach { it.herb = -1; it.day = -1 }; seeds.fill(0); herbs.clear(); known.clear()
        brew = -1; brewChance = 0; brewDay = -1; brewHerbs.clear(); pills.clear(); made = 0; failed = 0; burst = 0
        danDoc = 0; lastTick = -1; breakPill.fill(0); theBonus = 0; thanBonus = 0; canCoGain = 0; lastResult = ""
    }

    // --- Linh mạch -----------------------------------------------------------------------------------

    fun giaiName(g: Int = giai) = DanDao.GIAI[(g - 1).coerceIn(0, 8)]
    fun speedBonus() = 2 * (giai - 1)
    fun breakBonus() = giai - 1
    fun upgradePrice() = if (giai >= 9) 0L else DanDao.MACH_PRICES[giai]

    fun upgrade(): Boolean {
        if (giai >= 9 || Store.linhThach < upgradePrice()) return false
        Store.linhThach -= upgradePrice()
        giai++
        Store.note("Linh mạch lên ${giaiName()}: linh lực +${speedBonus()}%, đột phá +${breakBonus()}%")
        Store.touch()
        return true
    }

    // --- Dược viên ------------------------------------------------------------------------------------

    fun open() = DanDao.plots(giai)
    fun yearsADay() = DanDao.YEARS_A_DAY + giai - 1 + (if (TongMon.perk(TongMon.BACH_THAO)) 5 else 0) + TranPhap.years()

    fun years(i: Int): Int {
        val p = plots[i]
        if (p.herb < 0) return 0
        val days = (p.day..today()).count { Store.minutes(it) >= 30 }
        return (days * yearsADay()).coerceAtMost(DanDao.MAX_YEARS)
    }

    fun plant(i: Int, kind: Int): Boolean {
        if (i !in 0 until open() || plots[i].herb >= 0 || kind !in HERBS.indices || seeds[kind] <= 0) return false
        seeds[kind]--
        plots[i].herb = kind
        plots[i].day = today()
        Store.touch()
        return true
    }

    /** Takes the herb with its năm; the seed it bears goes back to the bag. */
    fun harvest(i: Int): Boolean {
        val p = plots[i]
        if (p.herb < 0) return false
        val y = years(i)
        herbs.add(p.herb to y)
        seeds[p.herb]++
        Store.note("Thu hoạch ${HERBS[p.herb]} $y năm")
        Store.fortune(com.exacta.reader.game.KyNgo.T_HARVEST)
        p.herb = -1
        p.day = -1
        Store.touch()
        return true
    }

    /** An đan phương bought in the đan phòng itself. */
    fun buyRecipe(r: Int): Boolean {
        val price = DanDao.recipePrice(r)
        if (r in known || r !in RECIPES.indices || Store.linhThach < price) return false
        Store.linhThach -= price
        known.add(r)
        Store.note("Mua đan phương ${RECIPES[r].name}")
        Store.touch()
        return true
    }

    fun buySeed(kind: Int): Boolean {
        val price = DanDao.SEED_PRICES[kind]
        if (Store.linhThach < price) return false
        Store.linhThach -= price
        seeds[kind]++
        Store.touch()
        return true
    }

    // --- Đan phòng -----------------------------------------------------------------------------------

    fun cap() = DanDao.cap(made)

    /** The youngest herb of `kind` with `min` năm or more, or -1. */
    private fun herbFor(kind: Int, min: Int, taken: Set<Int> = emptySet()): Int =
        herbs.indices.filter { it !in taken && herbs[it].first == kind && herbs[it].second >= min }.minByOrNull { herbs[it].second } ?: -1

    private fun picks(r: Int): List<Int>? {
        val taken = HashSet<Int>()
        for ((kind, min) in RECIPES[r].herbs) {
            val i = herbFor(kind, min, taken)
            if (i < 0) return null
            taken.add(i)
        }
        return taken.toList()
    }

    fun hasHerbs(r: Int) = picks(r) != null
    fun canBrew(r: Int) = r in known && brew < 0 && cap() >= RECIPES[r].cap && hasHerbs(r)

    /** The chance of a recipe now: the cấp above what it asks, and herbs older than they need. */
    fun chance(r: Int): Int {
        val rec = RECIPES[r]
        val age = picks(r)?.sumOf { i -> (herbs[i].second - rec.herbs.first { it.first == herbs[i].first }.second) / 100 * 3 } ?: 0
        return (60 + 8 * (cap() - rec.cap) + minOf(15, age) + (if (TongMon.perk(TongMon.BACH_THAO)) 10 else 0) + DaoLo.brewBonus() + TranPhap.brew()).coerceIn(20, 98)
    }

    fun startBrew(r: Int): Boolean {
        if (!canBrew(r)) return false
        brewChance = chance(r)
        val used = picks(r)!!.sortedDescending()
        brewHerbs.clear()
        for (i in used) brewHerbs.add(herbs.removeAt(i))
        brew = r
        brewDay = today()
        Store.note("Khai lò ${RECIPES[r].name}: buổi đọc tới phải liền ${RECIPES[r].minutes} phút")
        Store.touch()
        return true
    }

    /** Puts the fire out before reading: the herbs go back. */
    fun cancelBrew(): Boolean {
        if (brew < 0) return false
        herbs.addAll(brewHerbs)
        brewHerbs.clear()
        brew = -1
        Store.touch()
        return true
    }

    /** Hỏa hầu: the sitting reached the recipe's minutes. */
    fun onSeconds(session: Long) {
        if (brew >= 0 && session >= RECIPES[brew].minutes * 60L) finish()
    }

    /** A sitting ended (of a minute or more) before the hỏa hầu: nổ lò. */
    fun onSessionEnd(seconds: Long) {
        if (brew < 0 || seconds < 60) return
        val name = RECIPES[brew].name
        burst++
        brew = -1
        brewHerbs.clear()
        lastResult = "Nổ lò $name: buổi đọc dừng ở ${seconds / 60} phút"
        Store.note("$lastResult, mất linh thảo")
        Store.touch()
    }

    private fun finish() {
        val r = brew
        val rec = RECIPES[r]
        val roll = rng.nextInt(100)
        brew = -1
        brewHerbs.clear()
        if (roll < brewChance) {
            val margin = brewChance - roll
            val pham = when {
                margin >= 60 -> 3
                margin >= 40 -> 2
                margin >= 20 -> 1
                else -> 0
            }
            val n = 1 + (if (cap() >= 4) 1 else 0) + (if (cap() >= 7) 1 else 0) +
                (if (rec.group == DanDao.G_LUYEN_THE && TongMon.perk(TongMon.THIET_COT)) 1 else 0)
            pills[r * 4 + pham] = (pills[r * 4 + pham] ?: 0) + n
            val before = cap()
            made++
            lastResult = "Thành đan: $n viên ${rec.name} ${DanDao.PHAM[pham]}"
            Store.note(lastResult)
            if (cap() > before) Store.note("Đan đạo lên ${DanDao.CAP[cap() - 1]} luyện đan sư")
            Store.fortune(com.exacta.reader.game.KyNgo.T_PILL, pham)
            DaoLo.onPill()
            Store.gainLinhThach(5L * rec.cap)
        } else {
            failed++
            lastResult = "Đan hỏng: ${rec.name} thành tro (tỉ lệ $brewChance%)"
            Store.note(lastResult)
        }
        Store.touch()
    }

    // --- Đan dược --------------------------------------------------------------------------------------

    fun count(r: Int, pham: Int) = pills[r * 4 + pham] ?: 0

    /** Whether a pill can do anything now. */
    fun usable(r: Int): Boolean = when (r) {
        DanDao.TU_KHI, DanDao.NGUNG_NGUYEN, DanDao.HOA_LINH, DanDao.HUYEN_NGUYEN, DanDao.THAI_THANH -> Store.realmCap >= 0
        DanDao.BO_KHI -> TuLuyen.resting() || TuLuyen.injured() || DoKiep.resting()
        DanDao.TINH_TAM -> TuLuyen.tamMa > 0
        DanDao.DONG_BI, DanDao.NGOC_TUY, DanDao.KHAI_THUC, DanDao.DUONG_THAN, DanDao.NGO_DAO -> true
        DanDao.THANH_DOC -> danDoc > 0
        DanDao.DIEN_THO -> DoKiep.span() != Int.MAX_VALUE
        DanDao.BOI_NGUYEN -> canCoGain < 15
        DanDao.SONG_ANH -> DaoLo.songWays() && !DaoLo.songAnh && DaoLo.songPill == 0
        DanDao.DO_KIEP -> Store.realmCap == 7 && DoKiep.passed < 9 && breakPill[8] == 0
        DanDao.HOI_KHI -> TuLuyen.resting()
        DanDao.TUC_MACH -> TuLuyen.injured()
        DanDao.THANH_TAM -> TuLuyen.tamMa > 0
        DanDao.DINH_HON -> TuLuyen.tamMaKiep || (TuLuyen.tauHoaDay >= 0 && today() - TuLuyen.tauHoaDay < 30)
        DanDao.GIAI_DOC -> danDoc > 0
        DanDao.TAY_LINH -> DaoPhap.awakened() && DaoPhap.washHours() >= DaoPhap.WASH_HOURS
        DanDao.DOAN_COT, DanDao.KIM_THAN -> true
        else -> RECIPES[r].gate == TuLuyen.gate() && Store.realmCap in 0..8 && breakPill[RECIPES[r].gate] == 0
    }

    /** Takes a pill: its line, or null when it cannot serve now. Over 60 đan độc, half the effect. */
    fun use(r: Int, pham: Int): String? {
        if (count(r, pham) <= 0 || !usable(r)) return null
        val rec = RECIPES[r]
        val weak = danDoc > DOC_HIGH
        fun v(x: Int) = x * (100 + 20 * pham) / 100 / (if (weak) 2 else 1)
        val text = when (r) {
            DanDao.TU_KHI, DanDao.NGUNG_NGUYEN, DanDao.HOA_LINH, DanDao.HUYEN_NGUYEN, DanDao.THAI_THANH -> {
                val n = v(rec.value).toLong()
                gainLinhLuc(n)
                "Linh lực +${Realm.van(n)} vạn chữ" + if (TuLuyen.atCeiling()) " (ép thành áp chế)" else ""
            }
            DanDao.HOI_KHI -> { TuLuyen.restUntil = -1; "Khí huyết hồi phục: đột phá lại được" }
            DanDao.TUC_MACH -> { TuLuyen.injuryUntil = -1; "Kinh mạch lành lại" }
            DanDao.THANH_TAM -> { TuLuyen.calm(v(rec.value)); "Tâm ma −${v(rec.value)}" }
            DanDao.DINH_HON -> {
                TuLuyen.tauHoaDay = -1; TuLuyen.tamMaKiep = false; TuLuyen.calm(v(rec.value))
                "Hồn định: hết tẩu hỏa nhập ma, tâm ma −${v(rec.value)}"
            }
            DanDao.DOAN_COT, DanDao.KIM_THAN, DanDao.DONG_BI, DanDao.NGOC_TUY -> { theBonus += v(rec.value); "Thể phách +${v(rec.value)}" }
            DanDao.BO_KHI -> { TuLuyen.restUntil = -1; TuLuyen.injuryUntil = -1; DoKiep.restUntil = -1; "Khí huyết hồi phục, kinh mạch lành" }
            DanDao.TINH_TAM -> { TuLuyen.calm(v(rec.value)); "Tâm ma −${v(rec.value)}" }
            DanDao.KHAI_THUC, DanDao.DUONG_THAN -> { thanBonus += v(rec.value); "Thần thức +${v(rec.value)}" }
            DanDao.THANH_DOC -> { danDoc = (danDoc - v(rec.value)).coerceAtLeast(0); "Đan độc −${v(rec.value)}" }
            DanDao.DIEN_THO -> { DoKiep.lifeStart += v(rec.value); "Thọ nguyên +${v(rec.value)} năm" }
            DanDao.BOI_NGUYEN -> { val n = minOf(v(rec.value).coerceAtLeast(1), 15 - canCoGain); canCoGain += n; "Căn cơ +$n mãi mãi" }
            DanDao.NGO_DAO -> { DaoPhap.gainNgo(v(rec.value).coerceAtLeast(1), rec.name); "Ngộ tính +${v(rec.value).coerceAtLeast(1)}" }
            DanDao.SONG_ANH -> { DaoLo.songPill = v(rec.value); "Lần thử Song Anh kế tiếp +${DaoLo.songPill}%" }
            DanDao.GIAI_DOC -> { danDoc = (danDoc - v(rec.value)).coerceAtLeast(0); "Đan độc −${v(rec.value)}" }
            DanDao.TAY_LINH -> {
                if (!DaoPhap.wash(free = true)) return null
                "Tẩy linh: ${DaoPhap.rootName()}"
            }
            else -> {
                breakPill[rec.gate] = rec.value + 2 * pham
                if (weak) breakPill[rec.gate] /= 2
                "Lần đột phá kế tiếp +${breakPill[rec.gate]}%"
            }
        }
        pills[r * 4 + pham] = count(r, pham) - 1
        if (count(r, pham) <= 0) pills.remove(r * 4 + pham)
        poison(rec.doc * DanDao.DOC_PCT[pham] / 100)
        Store.pillsUsed++
        Store.note("${rec.name}: $text")
        Store.checkRealm()
        Store.touch()
        return text + if (weak) " · đan độc nặng, công hiệu giảm nửa" else ""
    }

    /** Words of linh lực: at the ceiling they press down as áp chế. */
    fun gainLinhLuc(n: Long) {
        if (TuLuyen.atCeiling()) TuLuyen.apChe += n else Store.bonusTuVi += n
    }

    fun poison(n: Int) {
        if (n <= 0 || !started) return
        val before = danDoc
        danDoc = (danDoc + n * DaoLo.docPercent() / 100).coerceAtMost(100)
        if (before <= DOC_HIGH && danDoc > DOC_HIGH) Store.note("Đan độc quá $DOC_HIGH: đan giảm công hiệu, căn cơ −10")
    }

    /** Each whole day once: a day of 30 minutes lets 5 đan độc fade. */
    fun tick() {
        if (!started) return
        val t = today()
        if (lastTick < 0 || lastTick > t) {
            lastTick = t - 1
            return
        }
        if (lastTick >= t - 1) return
        for (d in lastTick + 1..t - 1) if (Store.minutes(d) >= 30) danDoc = (danDoc - DOC_FADE).coerceAtLeast(0)
        lastTick = t - 1
        Store.touch()
    }

    fun canCoPenalty() = if (danDoc > DOC_HIGH) 10 else 0

    /** After a roll at gate `g`: the phá cảnh pill is spent. */
    fun onRoll(g: Int) {
        if (g in breakPill.indices) breakPill[g] = 0
    }

    // --- What is found --------------------------------------------------------------------------------

    private val SEED_WEIGHTS = intArrayOf(30, 20, 15, 12, 10, 7, 4, 2, 6, 4, 3, 1)

    fun dropSeed(r: Random = Random.Default): String {
        var roll = r.nextInt(SEED_WEIGHTS.sum())
        var kind = 0
        for ((i, w) in SEED_WEIGHTS.withIndex()) {
            roll -= w
            if (roll < 0) { kind = i; break }
        }
        val n = if (kind <= 1) 2 else 1
        seeds[kind] += n
        Store.touch()
        return "$n hạt ${HERBS[kind]}"
    }

    fun dropRecipe(r: Random = Random.Default): String {
        val free = RECIPES.indices.filter { it !in known && RECIPES[it].cap <= cap() + 3 }
        val pick = free.randomOrNull(r) ?: return dropSeed(r)
        known.add(pick)
        Store.touch()
        return "đan phương ${RECIPES[pick].name}"
    }

    fun learn(r: Int): Boolean {
        if (r !in RECIPES.indices || r in known) return false
        known.add(r)
        Store.touch()
        return true
    }

    // --- The move from 2.1 ----------------------------------------------------------------------------

    fun migrate() {
        reset()
        started = true
        lastTick = today() - 1
        seeds[0] = 3; seeds[1] = 1; seeds[2] = 1
        known.addAll(listOf(DanDao.TU_KHI, DanDao.HOI_KHI, DanDao.GIAI_DOC, DanDao.TRUC_CO))
        if (Store.realmCap in 1..8) DanDao.breakRecipe(Store.realmCap + 1).takeIf { it >= 0 }?.let { known.add(it) }
        Store.touch()
    }

    // --- Saving -----------------------------------------------------------------------------------------

    fun toJson(): JSONObject = JSONObject().apply {
        put("giai", giai)
        put("plots", JSONArray().apply { plots.forEach { put(JSONArray().put(it.herb).put(it.day)) } })
        put("seeds", JSONArray().apply { seeds.forEach { put(it) } })
        put("herbs", JSONArray().apply { herbs.forEach { put(JSONArray().put(it.first).put(it.second)) } })
        put("known", JSONArray().apply { known.forEach { put(it) } })
        put("brew", brew); put("brewChance", brewChance); put("brewDay", brewDay)
        put("brewHerbs", JSONArray().apply { brewHerbs.forEach { put(JSONArray().put(it.first).put(it.second)) } })
        put("pills", JSONObject().apply { pills.forEach { (k, v) -> put(k.toString(), v) } })
        put("made", made); put("failed", failed); put("burst", burst); put("doc", danDoc); put("tick", lastTick)
        put("breakPill", JSONArray().apply { breakPill.forEach { put(it) } }); put("the", theBonus); put("last", lastResult)
        put("than", thanBonus); put("canCo", canCoGain)
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        giai = o.optInt("giai", 1).coerceIn(1, 9)
        o.optJSONArray("plots")?.let { a -> for (i in 0 until minOf(a.length(), plots.size)) a.getJSONArray(i).let { plots[i].herb = it.optInt(0, -1); plots[i].day = it.optLong(1, -1) } }
        o.optJSONArray("seeds")?.let { a -> for (i in 0 until minOf(a.length(), seeds.size)) seeds[i] = a.getInt(i) }
        o.optJSONArray("herbs")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { herbs.add(it.getInt(0) to it.getInt(1)) } }
        o.optJSONArray("known")?.let { a -> for (i in 0 until a.length()) a.getInt(i).takeIf { it in RECIPES.indices }?.let { known.add(it) } }
        brew = o.optInt("brew", -1).takeIf { it in RECIPES.indices } ?: -1
        brewChance = o.optInt("brewChance"); brewDay = o.optLong("brewDay", -1)
        o.optJSONArray("brewHerbs")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { brewHerbs.add(it.getInt(0) to it.getInt(1)) } }
        o.optJSONObject("pills")?.let { m -> for (k in m.keys()) pills[k.toInt()] = m.getInt(k) }
        made = o.optInt("made"); failed = o.optInt("failed"); burst = o.optInt("burst"); danDoc = o.optInt("doc")
        lastTick = o.optLong("tick", -1)
        o.optJSONArray("breakPill")?.let { a -> for (i in 0 until minOf(a.length(), breakPill.size)) breakPill[i] = a.getInt(i) }
        theBonus = o.optInt("the"); lastResult = o.optString("last"); thanBonus = o.optInt("than"); canCoGain = o.optInt("canCo")
    }

}
