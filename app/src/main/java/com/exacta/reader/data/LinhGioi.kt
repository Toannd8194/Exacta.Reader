package com.exacta.reader.data

import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Treasure
import org.json.JSONArray
import org.json.JSONObject

/**
 * 2.5 Phi Thăng Linh Giới: from Hóa Thần to Đại Thừa.
 *
 *  - Phi thăng (the gate into Luyện Hư) asks three things: không gian tiết điểm (the bí cảnh four
 *    weeks running), trảm trần duyên (a bản mệnh pháp bảo and three more carried up; the rest stay
 *    behind as di bảo Nhân giới, their sets held down by the thick linh khí), and the phi thăng kiếp.
 *  - In Linh giới the bản mệnh grows with the hours read; each thirty days one more di bảo can be
 *    tế luyện and carried.
 *  - Pháp tắc: eight laws, open by the linh căn, the công pháp and the way. One is pondered at a time
 *    through three trials of reading; Luyện Hư asks one to enter Hợp Thể, with body and mind as one
 *    (thân thần hợp nhất). Hợp Thể asks three and an đạo vực, named by the cultivator, to enter Đại
 *    Thừa: the three laws in it count twice.
 */
object LinhGioi {
    // --- Pháp tắc -------------------------------------------------------------------------------------

    const val KIM = 0
    const val MOC = 1
    const val THUY = 2
    const val HOA = 3
    const val THO = 4
    const val LOI = 5
    const val KHONG = 6
    const val THOI = 7

    class Law(val name: String, val short: String, val opens: String, val good: String, val steps: List<String>)

    val LAWS = listOf(
        Law("Canh Kim pháp tắc", "Kim", "linh căn Kim, hoặc công pháp Kim, Kiếm", "đột phá +5%",
            listOf("Đọc trọn một cuốn trong 7 ngày", "Mười buổi đọc liền 45 phút", "Ba cuốn, mỗi cuốn đọc trọn trong 14 ngày")),
        Law("Ất Mộc pháp tắc", "Mộc", "linh căn Mộc, hoặc công pháp Mộc", "căn cơ +5",
            listOf("20 ngày đọc từ 30 phút", "40 ngày đọc từ 30 phút", "80 ngày đọc từ 30 phút")),
        Law("Nhâm Thủy pháp tắc", "Thủy", "linh căn Thủy, Băng, hoặc công pháp Thủy", "tâm ma tăng chậm 25%",
            listOf("Chuỗi 14 ngày đọc", "Chuỗi 30 ngày đọc", "Chuỗi 60 ngày đọc")),
        Law("Bính Hỏa pháp tắc", "Hỏa", "linh căn Hỏa, hoặc công pháp Hỏa", "linh lực +10%",
            listOf("Một ngày đọc 3 giờ", "Năm ngày, mỗi ngày đọc 2 giờ", "Đọc 30 vạn chữ")),
        Law("Mậu Thổ pháp tắc", "Thổ", "linh căn Thổ, hoặc công pháp Thổ", "thể phách +15%",
            listOf("Đọc trọn một cuốn từ 50 vạn chữ", "Thể phách Kim Thân", "Đọc trọn ba cuốn từ 50 vạn chữ")),
        Law("Tử Lôi pháp tắc", "Lôi", "dị linh căn Lôi, hoặc công pháp Lôi", "thiên kiếp ít hơn 1 ngày",
            listOf("Một ngày đọc 5 vạn chữ", "Đọc trọn cuốn từ 30 vạn chữ trong 5 ngày", "Ba lần như thế")),
        Law("Không gian pháp tắc", "Không gian", "Thần tu, hoặc thần thức Bát trọng, hoặc ngũ hành tạp linh căn", "thần thức +15%",
            listOf("Đọc trọn sách của 6 tác giả mới", "12 tác giả mới", "20 tác giả mới")),
        Law("Thời gian pháp tắc", "Thời gian", "Thể tu, hoặc chuỗi dài nhất từ 100 ngày, hoặc ngũ hành tạp linh căn", "linh lực +5%, hồi khí bớt 1 ngày",
            listOf("Chuỗi 60 ngày đọc", "Có đọc 120 trong 150 ngày", "Đọc xong ba cuốn bỏ dở từ ba tháng")),
    )

    // --- State -------------------------------------------------------------------------------------

    var started = false
    val tietWeeks = HashSet<Long>()           // Mondays of the weeks whose bí cảnh was passed
    val banMenh = ArrayList<Long>()           // seeds of the bản mệnh pháp bảo
    val carried = ArrayList<Long>()           // seeds of the pháp bảo carried up
    var banMenhDay = -1L
    var rechoose = false                      // an old save got its choice made for it: one free change
    val stage = IntArray(LAWS.size)           // trials passed of each law (3 = lĩnh ngộ)
    val learnedDay = LongArray(LAWS.size) { -1L }
    var active = -1
    var activeDay = -1L
    var vucName = ""
    val vuc = ArrayList<Int>()
    var vucDay = -1L

    fun reset() {
        started = false; tietWeeks.clear(); banMenh.clear(); carried.clear(); banMenhDay = -1; rechoose = false
        stage.fill(0); learnedDay.fill(-1); active = -1; activeDay = -1; vucName = ""; vuc.clear(); vucDay = -1
    }

    private fun today() = Store.today()
    fun ascended() = Store.realmCap >= 5
    fun ascendDay() = TuLuyen.capDays[5]

    // --- Không gian tiết điểm -----------------------------------------------------------------------

    fun onBiCanh(week: Long) {
        tietWeeks.add(week)
        Store.touch()
    }

    /** Weeks of bí cảnh running, up to this week (or last week while this one is still open). */
    fun tietRun(): Int {
        var w = Store.weekStart(today())
        if (w !in tietWeeks) w -= 7
        var n = 0
        while (w in tietWeeks) { n++; w -= 7 }
        return n
    }

    const val TIET_NEED = 4

    // --- Trảm trần duyên ----------------------------------------------------------------------------

    fun beast(seed: Long) = Store.beasts.firstOrNull { it.seed == seed }
    fun banMenhSlots() = if (DaoLo.songAnh) 2 else 1
    /** Three carried at phi thăng, one more each thirty days in Linh giới. */
    fun carrySlots(): Int = 3 + if (ascended() && ascendDay() >= 0) ((today() - ascendDay()) / 30).toInt() else 0

    fun isBanMenh(seed: Long) = seed in banMenh
    fun isCarried(seed: Long) = seed in carried
    fun locked() = ascended() && !rechoose

    fun cleanLost() {
        val have = Store.beasts.map { it.seed }.toSet()
        if (banMenh.removeAll { it !in have } or carried.removeAll { it !in have }) Store.touch()
    }

    /** Before phi thăng anything goes; after it the bản mệnh stays and the carried can only grow. */
    fun canMakeBanMenh(seed: Long) = beast(seed) != null && !isBanMenh(seed) && (!locked() || (banMenh.size < banMenhSlots()))
    fun makeBanMenh(seed: Long): Boolean {
        if (!canMakeBanMenh(seed)) return false
        carried.remove(seed)
        if (banMenh.size >= banMenhSlots()) banMenh.removeAt(0)
        banMenh.add(seed)
        banMenhDay = today()
        Store.note("Bản mệnh pháp bảo: ${beast(seed)?.name}")
        Store.touch()
        return true
    }

    fun canCarry(seed: Long) = beast(seed) != null && !isBanMenh(seed) && !isCarried(seed) && carried.size < carrySlots()
    fun carry(seed: Long): Boolean {
        if (!canCarry(seed)) return false
        carried.add(seed)
        Store.note((if (ascended()) "Tế luyện di bảo: " else "Mang theo: ") + "${beast(seed)?.name}")
        Store.touch()
        return true
    }

    fun canLeave(seed: Long) = !locked() && (isCarried(seed) || isBanMenh(seed))
    fun leave(seed: Long): Boolean {
        if (!canLeave(seed)) return false
        carried.remove(seed); banMenh.remove(seed)
        Store.touch()
        return true
    }

    /** The old save's one free change is spent once phi thăng is behind and the choice is kept. */
    fun keepChoice() {
        rechoose = false
        Store.touch()
    }

    fun trimDone() = Store.beasts.isEmpty() || banMenh.isNotEmpty()

    /** In Linh giới a set works only while one of its kinds was carried up. */
    fun setHeld(set: Int): Boolean {
        if (!started || !ascended()) return false
        val members = Treasure.SET_MEMBERS[set]
        return (banMenh + carried).none { s -> beast(s)?.species?.let { it in members } == true }
    }

    /** The bản mệnh grows a level each 20 hours read since it was chosen, to 9. */
    fun banMenhLevel(): Int {
        if (banMenh.isEmpty() || banMenhDay < 0) return 0
        val s = (banMenhDay..today()).sumOf { Store.daySeconds[it] ?: 0L }
        return (s / (20 * 3600)).toInt().coerceIn(0, 9)
    }

    // --- Pháp tắc -----------------------------------------------------------------------------------

    fun learned(i: Int) = stage[i] >= 3
    fun learnedCount() = stage.count { it >= 3 }
    fun learnedList() = LAWS.indices.filter { learned(it) }

    private fun cong() = DaoPhap.activeDef()?.attr ?: -1
    private fun root(a: Int) = a in DaoPhap.roots

    /** Whether law i can be pondered by this cultivator. */
    fun opens(i: Int): Boolean = when (i) {
        KIM -> root(PhapMon.KIM) || cong() == PhapMon.KIM || cong() == PhapMon.KIEM
        MOC -> root(PhapMon.MOC) || cong() == PhapMon.MOC
        THUY -> root(PhapMon.THUY) || DaoPhap.di == PhapMon.BANG || cong() == PhapMon.THUY
        HOA -> root(PhapMon.HOA) || cong() == PhapMon.HOA
        THO -> root(PhapMon.THO) || cong() == PhapMon.THO || DaoPhap.roots.isEmpty()
        LOI -> DaoPhap.di == PhapMon.LOI || cong() == PhapMon.LOI
        KHONG -> DaoLo.walks(DaoLo.THAN) || TuLuyen.trong() >= 7 || DaoPhap.roots.size >= 5
        else -> DaoLo.walks(DaoLo.THE) || Store.bestStreak() >= 100 || DaoPhap.roots.size >= 5
    }

    fun canPonder(i: Int) = Store.realmCap >= 5 && !learned(i) && active != i && opens(i)

    fun ponder(i: Int): Boolean {
        if (!canPonder(i)) return false
        if (active >= 0) stage[active] = 0
        active = i
        activeDay = today()
        stage[i] = 0
        Store.note("Tham ngộ ${LAWS[i].name}")
        Store.touch()
        return true
    }

    // What the reading since `from` shows.
    private fun finished(from: Long) = Store.books.values.filter { it.finishDay >= from }
    private fun daysAt(from: Long, minutes: Int) = (from..today()).count { Store.minutes(it) >= minutes }
    private fun longRun(from: Long): Int {
        var best = 0; var n = 0
        for (d in from..today()) {
            if ((Store.daySeconds[d] ?: 0) >= 60 || d in Store.sealedDays) { n++; best = maxOf(best, n) } else n = 0
        }
        return best
    }
    private fun newAuthors(from: Long): Int {
        val before = Store.books.values.filter { it.finishDay in 0 until from }.map { it.author.trim().lowercase() }.toSet()
        return finished(from).map { it.author.trim().lowercase() }.filter { it.isNotEmpty() && it !in before }.toSet().size
    }
    private fun fast(from: Long) = finished(from).count { it.words >= 300_000 && it.firstDay >= 0 && it.finishDay - it.firstDay < 5 }

    /** (have, need) of trial `s` of law i, counted since the pondering began. */
    fun progress(i: Int, s: Int): Pair<Int, Int> {
        val from = if (active == i && activeDay >= 0) activeDay else today()
        return when (i) {
            KIM -> when (s) {
                0 -> finished(from).count { it.firstDay >= 0 && it.finishDay - it.firstDay < 7 } to 1
                1 -> (from..today()).sumOf { TuLuyen.dayLong[it] ?: 0 } to 10
                else -> finished(from).count { it.firstDay >= 0 && it.finishDay - it.firstDay < 14 } to 3
            }
            MOC -> daysAt(from, 30) to listOf(20, 40, 80)[s]
            THUY -> longRun(from) to listOf(14, 30, 60)[s]
            HOA -> when (s) {
                0 -> daysAt(from, 180).coerceAtMost(1) to 1
                1 -> daysAt(from, 120) to 5
                else -> ((from..today()).sumOf { Store.dayWords[it] ?: 0L } / 10_000).toInt() to 30
            }
            THO -> when (s) {
                0 -> finished(from).count { it.words >= 500_000 } to 1
                1 -> TuLuyen.thePhach() to 300
                else -> finished(from).count { it.words >= 500_000 } to 3
            }
            LOI -> when (s) {
                0 -> (from..today()).count { (Store.dayWords[it] ?: 0L) >= 50_000 }.coerceAtMost(1) to 1
                1 -> fast(from).coerceAtMost(1) to 1
                else -> fast(from) to 3
            }
            KHONG -> newAuthors(from) to listOf(6, 12, 20)[s]
            else -> when (s) {
                0 -> longRun(from) to 60
                1 -> (maxOf(from, today() - 149)..today()).count { (Store.daySeconds[it] ?: 0) >= 60 } to 120
                else -> finished(from).count { it.firstDay >= 0 && it.firstDay < from - 90 } to 3
            }
        }
    }

    /** Each whole day: the trials of the law being pondered. */
    fun check() {
        if (!started) return
        cleanLost()
        val i = active
        if (i < 0) return
        while (stage[i] < 3) {
            val (have, need) = progress(i, stage[i])
            if (have < need) break
            stage[i]++
            if (stage[i] < 3) Store.note("${LAWS[i].name}: qua thử thách ${stage[i]}/3")
        }
        if (stage[i] >= 3) {
            learnedDay[i] = today()
            active = -1
            activeDay = -1
            Store.note("Lĩnh ngộ ${LAWS[i].name}: ${LAWS[i].good}")
            Store.gainLinhThach(300)
        }
        Store.touch()
    }

    // --- Đạo vực ----------------------------------------------------------------------------------------

    fun canFormVuc() = vuc.isEmpty() && Store.realmCap >= 6 && learnedCount() >= 3
    fun suggestName(laws: List<Int>) = laws.joinToString(" ") { LAWS[it].short } + " Đạo Vực"

    fun formVuc(laws: List<Int>, name: String): Boolean {
        if (!canFormVuc() || laws.size != 3 || laws.toSet().size != 3 || !laws.all { learned(it) }) return false
        vuc.clear(); vuc.addAll(laws)
        vucName = name.trim().ifEmpty { suggestName(laws) }.take(40)
        vucDay = today()
        Store.note("Kết đạo vực «$vucName»: ba pháp tắc trong vực tính gấp đôi")
        Store.touch()
        return true
    }

    /** A law's weight: 1 lĩnh ngộ, 2 in the đạo vực, 0 not yet. */
    fun weight(i: Int) = if (!learned(i)) 0 else if (i in vuc) 2 else 1

    // --- Effects ------------------------------------------------------------------------------------------

    fun speedBonus(): Int = 10 * weight(HOA) + 5 * weight(THOI) + (if (vuc.isNotEmpty()) 10 else 0) + banMenhLevel()
    fun speedParts(): List<Pair<String, Int>> {
        val list = ArrayList<Pair<String, Int>>()
        if (weight(HOA) > 0) list.add(LAWS[HOA].name to 10 * weight(HOA))
        if (weight(THOI) > 0) list.add(LAWS[THOI].name to 5 * weight(THOI))
        if (vuc.isNotEmpty()) list.add("Đạo vực «$vucName»" to 10)
        if (banMenhLevel() > 0) list.add("Bản mệnh ôn dưỡng tầng ${banMenhLevel()}" to banMenhLevel())
        return list
    }
    fun canCoBonus() = 5 * weight(MOC)
    fun stirPercent() = when (weight(THUY)) { 0 -> 100; 1 -> 75; else -> 50 }
    fun thePercent() = 100 + 15 * weight(THO)
    fun thanPercent() = 100 + 15 * weight(KHONG)
    fun kiepLess() = weight(LOI)
    fun restLess() = weight(THOI)
    fun breakParts(g: Int): List<Pair<String, Int>> {
        val list = ArrayList<Pair<String, Int>>()
        if (weight(KIM) > 0) list.add(LAWS[KIM].name to 5 * weight(KIM))
        return list
    }

    // --- The gates ------------------------------------------------------------------------------------

    /** Thân thần hợp nhất: thể phách 300 and thần thức 24, half for the ngũ hành tạp linh căn. */
    fun hopTheNeed() = if (DaoPhap.roots.size >= 5) 150 to 12 else 300 to 24

    fun needs(g: Int): List<TuLuyen.Need> = when (g) {
        5 -> listOf(
            TuLuyen.Need("Không gian tiết điểm: qua bí cảnh $TIET_NEED tuần liền (đang ${minOf(tietRun(), TIET_NEED)})", tietRun() >= TIET_NEED),
            TuLuyen.Need(if (trimDone()) "Trảm trần duyên: bản mệnh ${banMenh.mapNotNull { beast(it)?.name }.joinToString(", ").ifEmpty { "không có" }}, mang theo ${carried.size}"
                else "Trảm trần duyên: chọn bản mệnh pháp bảo ở Linh giới › Di bảo", trimDone()),
        )
        6 -> {
            val (tp, tt) = hopTheNeed()
            listOf(
                TuLuyen.Need("Lĩnh ngộ một pháp tắc (đã ${learnedCount()})", learnedCount() >= 1),
                TuLuyen.Need("Thân thần hợp nhất: thể phách $tp, thần thức $tt (đang ${TuLuyen.thePhach()}, ${TuLuyen.thanThuc()})",
                    TuLuyen.thePhach() >= tp && TuLuyen.thanThuc() >= tt),
            )
        }
        7 -> listOf(
            TuLuyen.Need("Lĩnh ngộ ba pháp tắc (đã ${learnedCount()})", learnedCount() >= 3),
            TuLuyen.Need(if (vuc.isNotEmpty()) "Đạo vực «$vucName»" else "Kết đạo vực ở Linh giới › Đạo vực", vuc.isNotEmpty()),
        )
        8 -> listOf(TuLuyen.Need(if (vuc.isNotEmpty()) "Đạo vực «$vucName»" else "Kết đạo vực ở Linh giới › Đạo vực", vuc.isNotEmpty()))
        else -> emptyList()
    }

    // --- Old saves ----------------------------------------------------------------------------------

    /** First run of 2.5: those already in Linh giới get their choices made for them, once changeable. */
    fun migrate() {
        reset()
        started = true
        if (Store.biCanhDone()) tietWeeks.add(Store.weekStart(today()))
        if (ascended()) {
            val sorted = Store.beasts.sortedWith(compareByDescending<Beast> { it.tier }.thenByDescending { it.bornDay })
            sorted.take(banMenhSlots()).forEach { banMenh.add(it.seed) }
            // Carry one of each set first, so the sets stay alive.
            val sets = HashSet<Int>()
            for (b in sorted) {
                if (carried.size >= carrySlots()) break
                if (b.seed in banMenh) continue
                val s = Treasure.setOf(b.species)
                if (s >= 0 && sets.add(s)) carried.add(b.seed)
            }
            banMenhDay = ascendDay().takeIf { it >= 0 } ?: today()
            rechoose = Store.beasts.isNotEmpty()
        }
        val order = LAWS.indices.sortedBy { if (opens(it)) 0 else 1 }
        if (Store.realmCap >= 6) learnFree(order.take(1))
        if (Store.realmCap >= 7) {
            learnFree(order.take(3))
            vuc.addAll(order.take(3)); vucName = suggestName(vuc); vucDay = today()
        }
        Store.touch()
    }

    private fun learnFree(list: List<Int>) {
        for (i in list) if (!learned(i)) { stage[i] = 3; learnedDay[i] = today() }
    }

    // --- Saving -------------------------------------------------------------------------------------

    fun toJson(): JSONObject = JSONObject().apply {
        put("tiet", JSONArray().apply { tietWeeks.sorted().forEach { put(it) } })
        put("banMenh", JSONArray().apply { banMenh.forEach { put(it) } })
        put("carried", JSONArray().apply { carried.forEach { put(it) } })
        put("banMenhDay", banMenhDay); put("rechoose", rechoose)
        put("stage", JSONArray().apply { stage.forEach { put(it) } })
        put("learned", JSONArray().apply { learnedDay.forEach { put(it) } })
        put("active", active); put("activeDay", activeDay)
        put("vucName", vucName); put("vuc", JSONArray().apply { vuc.forEach { put(it) } }); put("vucDay", vucDay)
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        o.optJSONArray("tiet")?.let { a -> for (i in 0 until a.length()) tietWeeks.add(a.getLong(i)) }
        o.optJSONArray("banMenh")?.let { a -> for (i in 0 until a.length()) banMenh.add(a.getLong(i)) }
        o.optJSONArray("carried")?.let { a -> for (i in 0 until a.length()) carried.add(a.getLong(i)) }
        banMenhDay = o.optLong("banMenhDay", -1); rechoose = o.optBoolean("rechoose")
        o.optJSONArray("stage")?.let { a -> for (i in 0 until minOf(a.length(), stage.size)) stage[i] = a.getInt(i) }
        o.optJSONArray("learned")?.let { a -> for (i in 0 until minOf(a.length(), learnedDay.size)) learnedDay[i] = a.getLong(i) }
        active = o.optInt("active", -1); activeDay = o.optLong("activeDay", -1)
        vucName = o.optString("vucName"); vucDay = o.optLong("vucDay", -1)
        o.optJSONArray("vuc")?.let { a -> for (i in 0 until a.length()) vuc.add(a.getInt(i)) }
    }
}
