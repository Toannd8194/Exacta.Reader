package com.exacta.reader.data

import com.exacta.reader.game.KyNgo
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * 2.6 Cửu Cửu Quy Nhất: Độ Kiếp, thiên nhân ngũ suy, thọ nguyên and the Chân Tu mode.
 *
 *  - Độ Kiếp is the special stage between Đại Thừa and Phi Thăng: nine tầng kiếp, each a trial of
 *    reading and then a roll, one after another. Lôi kiếp (1 – 3) is held by the pháp bảo and the
 *    body, pháp tắc kiếp (4 – 6) by the laws and the mind, tâm ma kiếp (7 – 8) by the heart, and
 *    the cửu cửu thiên kiếp (9) by all of it, its last wave a tâm lôi. Words read between tầng are
 *    kiếp lực. Each tầng passed leaves a kiếp ấn that says how it was held, and that leans Lục Giới.
 *    A failure keeps what was passed: wounds and tâm ma, rest, then again with 10% more.
 *  - Phi Thăng opens with thiên nhân ngũ suy: căn cơ and tâm cảnh fall 15; within thirty days the
 *    five measures must stand at their marks, or the decay comes back the next month.
 *  - Thọ nguyên: a day is a year; each realm has its span. Near the end a breakthrough is a gamble
 *    (+10%). In the Chân Tu mode, the reader's choice, a span run out is tọa hóa: the cultivator is
 *    reborn and cultivates again from Luyện Khí, faster while below the realm reached before.
 */
object DoKiep {
    // --- The nine tầng -------------------------------------------------------------------------------

    const val LOI = 0
    const val PHAP_TAC = 1
    const val TAM_MA = 2
    const val CUU_CUU = 3

    class Tang(val name: String, val kind: Int, val base: Int, val rule: String)

    val TANGS = listOf(
        Tang("Thiên Lôi kiếp", LOI, 55, "5 ngày liền, mỗi ngày đọc 55 phút"),
        Tang("Địa Hỏa kiếp", LOI, 50, "5 ngày liền, mỗi ngày đọc 55 phút"),
        Tang("Cương Phong kiếp", LOI, 45, "5 ngày liền, mỗi ngày đọc 55 phút"),
        Tang("Pháp tắc kiếp · Thiên", PHAP_TAC, 50, "đọc trọn một cuốn của tác giả mới trong 30 ngày"),
        Tang("Pháp tắc kiếp · Địa", PHAP_TAC, 45, "đọc trọn một cuốn của tác giả mới trong 30 ngày"),
        Tang("Pháp tắc kiếp · Nhân", PHAP_TAC, 40, "đọc trọn một cuốn của tác giả mới trong 30 ngày"),
        Tang("Tâm ma kiếp · Vọng niệm", TAM_MA, 45, "7 ngày liền đọc một cuốn, mỗi ngày 30 phút, không nhảy chương, không đổi sách"),
        Tang("Tâm ma kiếp · Chấp niệm", TAM_MA, 40, "7 ngày liền đọc một cuốn, mỗi ngày 30 phút, không nhảy chương, không đổi sách"),
        Tang("Cửu cửu thiên kiếp", CUU_CUU, 35, "chín đợt trong 15 ngày, mỗi đợt một ngày đọc 60 phút, nghỉ không quá 6 ngày giữa hai đợt"),
    )

    /** How a tầng was held: the kiếp ấn's mark, and the realm of Lục Giới it leans to. */
    val BY = arrayOf("pháp bảo", "nhục thân", "pháp tắc", "thần thức", "tâm cảnh", "kiên trì")
    val BY_GIOI = intArrayOf(0, 3, 0, 5, 2, 1)

    class An(val tang: Int, val day: Long, val by: Int, val fails: Int)

    // --- State ---------------------------------------------------------------------------------------

    var started = false
    var passed = 0                       // tầng passed (9: Độ Kiếp done)
    var active = -1                      // the tầng under way, or -1
    var activeDay = -1L
    var book = ""                        // tâm ma kiếp: the one book
    var broken = false                   // tâm ma kiếp: a jump or another book
    val fails = IntArray(9)
    var restUntil = -1L
    var kiepBase = 0L                    // total words when the last tầng ended: what is above is kiếp lực
    val an = ArrayList<An>()
    var lastResult = ""
    // Thiên nhân ngũ suy.
    var suyStart = -1L
    var suyPassed = false
    var suySeen = -1L
    // Thọ nguyên and Chân Tu.
    var lifeStart = -1L
    var rebirths = 0
    var prevMax = -1
    var rng: Random = Random.Default

    fun reset() {
        started = false; passed = 0; active = -1; activeDay = -1; book = ""; broken = false; fails.fill(0); restUntil = -1
        kiepBase = 0; an.clear(); lastResult = ""; suyStart = -1; suyPassed = false; suySeen = -1
        lifeStart = -1; rebirths = 0; prevMax = -1
    }

    private fun today() = Store.today()

    // --- Starting a tầng ------------------------------------------------------------------------------

    fun resting() = today() < restUntil

    /** Đại Thừa đại viên mãn with all the gate asks besides the nine tầng. */
    fun ready() = Store.realmCap == 7 && passed < 9 && TuLuyen.needs(trial = false).filter { !it.text.startsWith(NEED) }.all { it.met }

    fun canStart() = started && ready() && active < 0 && !resting()

    fun start(): Boolean {
        if (!canStart()) return false
        active = passed
        activeDay = today()
        book = ""
        broken = false
        Store.note("Độ ${TANGS[active].name} (tầng ${active + 1}/9): ${TANGS[active].rule}")
        Store.touch()
        return true
    }

    const val NEED = "Chín tầng kiếp"

    // --- The trial of a tầng --------------------------------------------------------------------------

    private fun met(d: Long, minutes: Int) = Store.minutes(d) >= minutes

    /** (done, failed, progress text) of the tầng under way. */
    fun trial(): Triple<Boolean, Boolean, String> {
        val t = active
        if (t < 0) return Triple(false, false, "")
        val from = activeDay
        val now = today()
        return when (TANGS[t].kind) {
            LOI -> {
                if ((from until now).any { !met(it, 55) }) return Triple(false, true, "lỡ một ngày")
                val n = (from..now).count { met(it, 55) }
                Triple(n >= 5, false, "${minOf(n, 5)}/5 ngày · hôm nay ${Store.minutes(now)}/55 phút")
            }
            PHAP_TAC -> {
                val before = Store.books.values.filter { it.finishDay in 0 until from }.map { it.author.trim().lowercase() }.toSet()
                val done = Store.books.values.any { it.finishDay in from..(from + 29) && it.author.isNotBlank() && it.author.trim().lowercase() !in before }
                val left = from + 29 - now
                Triple(done, !done && left < 0, if (done) "đã đọc trọn" else "còn $left ngày")
            }
            TAM_MA -> {
                if (broken) return Triple(false, true, "tâm ma xâm nhập")
                if ((from until now).any { !met(it, 30) }) return Triple(false, true, "lỡ một ngày")
                val n = (from..now).count { met(it, 30) }
                Triple(n >= 7, false, "${minOf(n, 7)}/7 ngày · hôm nay ${Store.minutes(now)}/30 phút")
            }
            else -> {
                val waves = (from..minOf(now, from + 14)).filter { met(it, 60) }
                // The rest between waves (and before the first) may not pass six days.
                var last = from - 1
                var gapBad = false
                for (d in waves) { if (d - last - 1 > 6) gapBad = true; last = d }
                if (now - last - 1 > 6 && waves.size < 9) gapBad = true
                val n = waves.size
                Triple(n >= 9, gapBad || (n < 9 && now > from + 14), "đợt ${minOf(n, 9)}/9 · còn ${maxOf(0, from + 14 - now)} ngày")
            }
        }
    }

    /** Words read since the last tầng: 1% each 4 vạn, 20% at most. */
    fun kiepLuc() = ((Store.totalWords - kiepBase) / 40_000).toInt().coerceIn(0, 20)

    /** The chance of tầng t, part by part, each with what holds it (BY index, or -1). */
    fun parts(t: Int = if (active >= 0) active else passed): List<Triple<String, Int, Int>> {
        val list = ArrayList<Triple<String, Int, Int>>()
        val tang = TANGS[t.coerceIn(0, 8)]
        list.add(Triple("Nền ${tang.name}", tang.base, -1))
        val kind = tang.kind
        val half = if (kind == CUU_CUU) 2 else 1
        if (kind == LOI || kind == CUU_CUU) {
            val best = TrangBi.bestTier()
            if (best >= 0) list.add(Triple("Pháp bảo đeo mạnh nhất, phẩm ${best + 1}", 3 * (best + 1) / half, 0))
            if (TrangBi.guard() > 0) list.add(Triple("Pháp bảo hộ thân", TrangBi.guard() / half, 0))
            if (LinhGioi.banMenhLevel() > 0) list.add(Triple("Bản mệnh tầng ${LinhGioi.banMenhLevel()}", 2 * LinhGioi.banMenhLevel() / half, 0))
            val body = DaoLo.breakParts(8).filter { it.second != 0 && !it.first.startsWith("Đạo lộ hợp") && !it.first.startsWith("Song Anh") }
            for (p in body) list.add(Triple(p.first, p.second / half, if (p.first.startsWith("Thể phách")) 1 else 0))
            if (LinhGioi.learned(LinhGioi.LOI)) list.add(Triple("Tử Lôi pháp tắc", 5, 2))
            if (TeLuyen.bestGrade() > 0) list.add(Triple("${TeLuyen.GRADES[TeLuyen.bestGrade()]} cấm chế tầng ${TeLuyen.bestLevel()}", 2 * TeLuyen.bestGrade() / half, 0))
            if (TranPhap.kiepPart() > 0) list.add(Triple("Hộ Sơn Trận đỡ lôi", TranPhap.kiepPart() / half, 0))
        }
        if (kind == PHAP_TAC || kind == CUU_CUU) {
            if (LinhGioi.learnedCount() > 0) list.add(Triple("Pháp tắc lĩnh ngộ ${LinhGioi.learnedCount()}", 6 * LinhGioi.learnedCount() / half, 2))
            if (LinhGioi.vuc.isNotEmpty()) list.add(Triple("Đạo vực «${LinhGioi.vucName}»", 10 / half, 2))
            if (TuLuyen.trong() > 0) list.add(Triple("Thần thức ${TuLuyen.TRONG[TuLuyen.trong()]}", 2 * TuLuyen.trong() / half, 3))
        }
        if (kind == TAM_MA || kind == CUU_CUU) {
            val tier = TuLuyen.tamCanhTier()
            if (tier > 0) list.add(Triple("Tâm cảnh ${TuLuyen.TAM_CANH_TIERS[tier]}" + if (kind == CUU_CUU) ", đỡ tâm lôi" else "", (if (kind == CUU_CUU) 6 else 5) * tier, 4))
            val tam = DaoPhap.tamOn.count { DaoPhap.tamLevel(it) >= 1 }
            if (tam > 0) list.add(Triple("Tâm pháp nhập môn $tam", 4 * tam / half, 4))
        }
        if (kiepLuc() > 0) list.add(Triple("Kiếp lực", kiepLuc(), 5))
        DaoPhap.lv(com.exacta.reader.game.PhapMon.E_DO_KIEP).takeIf { it > 0 }?.let {
            list.add(Triple("Đặc hiệu công pháp", com.exacta.reader.game.PhapMon.effectValue(com.exacta.reader.game.PhapMon.E_DO_KIEP, it), 5))
        }
        if (DongPhu.breakPill[8] > 0) list.add(Triple("Độ Kiếp Đan", DongPhu.breakPill[8], 5))
        if (fails[t.coerceIn(0, 8)] > 0) list.add(Triple("Kiên trì, ${fails[t.coerceIn(0, 8)]} lần trước", minOf(30, 10 * fails[t.coerceIn(0, 8)]), 5))
        lifeGamble()?.let { list.add(Triple(it.first, it.second, 5)) }
        if (TuLuyen.tamMa > 1) list.add(Triple("Tâm ma ${TuLuyen.tamMa}, chia đôi", -(TuLuyen.tamMa / 2), -1))
        return list
    }

    fun chance(t: Int = if (active >= 0) active else passed) = parts(t).sumOf { it.second }.coerceIn(10, 95)

    /** Each whole day (and as the reading goes): the tầng under way. */
    fun check() {
        if (!started) return
        lifeCheck()
        suyCheck()
        if (active < 0) return
        val (done, failed, _) = trial()
        when {
            failed -> fail("${TANGS[active].name}: ${trial().third}")
            done -> roll()
        }
    }

    private fun roll() {
        val t = active
        val c = chance(t)
        val ok = rng.nextInt(100) < c
        DongPhu.onRoll(8)
        if (!ok) {
            fail("${TANGS[t].name} giáng xuống, không đỡ nổi (tỉ lệ $c%)")
            return
        }
        // The kiếp ấn: what held most.
        val by = parts(t).filter { it.third >= 0 && it.second > 0 }.groupBy { it.third }.maxByOrNull { e -> e.value.sumOf { it.second } }?.key ?: 5
        an.add(An(t, today(), by, fails[t]))
        passed = t + 1
        active = -1
        activeDay = -1
        kiepBase = Store.totalWords
        lastResult = "Vượt ${TANGS[t].name} (tầng ${t + 1}/9) nhờ ${BY[by]}"
        Store.note("$lastResult · kiếp ấn")
        Store.gainLinhThach(100L * (t + 1))
        Store.fortune(KyNgo.T_KIEP)
        if (passed >= 9) TuLuyen.enterPhiThang()
        Store.touch()
    }

    private fun fail(why: String) {
        val t = active
        fails[t]++
        NhatKy.event(NhatKy.E_FAIL)
        active = -1
        activeDay = -1
        TuLuyen.stir(10)
        TuLuyen.injuryUntil = maxOf(TuLuyen.injuryUntil, today() + 5)
        restUntil = today() + (2 - LinhGioi.restLess()).coerceAtLeast(1)
        Store.failCount++
        Store.failDay = today()
        lastResult = "$why: tâm ma +10, kinh mạch tổn thương 5 ngày, nghỉ rồi độ lại (+10%)"
        Store.note(lastResult)
        Store.touch()
    }

    fun onSeconds(path: String) {
        if (active < 0 || TANGS[active].kind != TAM_MA) return
        if (book.isEmpty()) book = path
        else if (book != path && !broken) {
            broken = true
            Store.note("Tâm ma kiếp: đổi sang cuốn khác, tâm ma xâm nhập")
        }
    }

    fun onJump() {
        if (active >= 0 && TANGS[active].kind == TAM_MA && !broken) {
            broken = true
            Store.note("Tâm ma kiếp: nhảy chương, tâm ma xâm nhập")
        }
    }

    /** The kiếp ấn lean each 3 to a realm of Lục Giới. */
    fun gioiPart(i: Int) = 3 * an.count { BY_GIOI[it.by] == i }

    // --- Thiên nhân ngũ suy ---------------------------------------------------------------------------

    const val SUY_DAYS = 30
    const val SUY_DROP = 15

    fun suyActive() = started && Store.realmCap == 8 && !suyPassed && suyStart >= 0
    fun suyPenalty() = if (suyActive()) SUY_DROP else 0
    /** The thirty days of decay now: from its first day. */
    fun suyWindow() = if (suyStart < 0) -1L else suyStart + (today() - suyStart) / SUY_DAYS * SUY_DAYS

    fun suyNeeds(): List<TuLuyen.Need> = listOf(
        TuLuyen.Need("Căn cơ 60 (đang ${TuLuyen.canCo()})", TuLuyen.canCo() >= 60),
        TuLuyen.Need("Tâm cảnh Thanh Minh (đang ${TuLuyen.TAM_CANH_TIERS[TuLuyen.tamCanhTier()]})", TuLuyen.tamCanhTier() >= 2),
        TuLuyen.Need("Thần thức Thất trọng (đang ${TuLuyen.TRONG[TuLuyen.trong()]})", TuLuyen.trong() >= 6),
        TuLuyen.Need("Thể phách Kim Thân (đang ${TuLuyen.THE_TIERS[TuLuyen.theTier()]})", TuLuyen.theTier() >= 4),
        TuLuyen.Need("Thọ nguyên: 20 trong 30 ngày đọc 30 phút (đang ${TuLuyen.daysMet(today() - 29, today(), 30)})", TuLuyen.daysMet(today() - 29, today(), 30) >= 20),
    )

    private fun suyCheck() {
        if (Store.realmCap == 8 && suyStart < 0) {
            suyStart = today()
            Store.note("Thiên nhân ngũ suy giáng xuống: căn cơ, tâm cảnh −$SUY_DROP; trong 30 ngày kéo năm thước đo về ngưỡng")
        }
        if (!suyActive()) return
        if (suyNeeds().all { it.met }) {
            suyPassed = true
            Store.note("Qua thiên nhân ngũ suy: Lục Giới Môn mở")
            Store.gainLinhThach(500)
            Store.touch()
            return
        }
        val w = suyWindow()
        if (w > suyStart && suySeen != w) {
            suySeen = w
            Store.note("Ngũ suy lặp lại: thêm 30 ngày nữa")
            Store.touch()
        }
    }

    // --- Thọ nguyên and Chân Tu -----------------------------------------------------------------------

    /** The span of each realm, in years (a day read or not is a year). */
    val SPAN = intArrayOf(120, 250, 500, 1_000, 2_000, 3_000, 5_000, 8_000, 12_000, Int.MAX_VALUE)

    fun age() = if (lifeStart < 0) 0 else (today() - lifeStart).toInt()
    fun span() = SPAN[Store.realmCap.coerceIn(0, 9)]
    fun spanLeft() = if (span() == Int.MAX_VALUE) Int.MAX_VALUE else span() - age()

    /** Near the end of the span (its last tenth): a breakthrough is a gamble, +10%. */
    fun lifeGamble(): Pair<String, Int>? =
        if (span() != Int.MAX_VALUE && age() >= span() * 9 / 10) "Thọ nguyên sắp cạn, liều mạng" to 10 else null

    private fun lifeCheck() {
        if (lifeStart < 0) {
            lifeStart = (Store.daySeconds.keys.minOrNull() ?: today()).coerceAtMost(today())
            Store.touch()
        }
        if (Store.chanTu && span() != Int.MAX_VALUE && age() >= span()) reborn()
    }

    /** Turning Chân Tu on: a span already nearly spent is given back to its half. */
    fun chanTuOn() {
        Store.chanTu = true
        if (span() != Int.MAX_VALUE && age() > span() / 2) lifeStart = today() - span() / 2
        Store.note("Chân Tu: thọ nguyên là thật, cạn là tọa hóa")
        Store.touch()
    }

    /** Tọa hóa: reborn at Luyện Khí; the collection, đạo hiệu and Niên kỷ stay. */
    fun reborn() {
        val was = Store.realmCap
        prevMax = maxOf(prevMax, was)
        rebirths++
        NhatKy.event(NhatKy.E_REBORN)
        TuLuyen.reborn()
        DaoLo.dan = -1; DaoLo.than = -1
        passed = 0; active = -1; activeDay = -1; fails.fill(0); an.clear(); suyStart = -1; suyPassed = false
        lifeStart = today()
        lastResult = "Thọ nguyên cạn ở ${com.exacta.reader.game.Realm.baseName(was)}: tọa hóa, chuyển thế lần $rebirths"
        Store.note("$lastResult · ký ức tiền kiếp: tu nhanh gấp rưỡi tới ${com.exacta.reader.game.Realm.baseName(prevMax)}")
        Store.touch()
    }

    /** Ký ức tiền kiếp: half again as fast while below the realm reached in the last life. */
    fun speedBonus() = if (rebirths > 0 && Store.realmCap < prevMax) 50 else 0

    // --- Old saves --------------------------------------------------------------------------------------

    fun migrate() {
        reset()
        started = true
        if (Store.realmCap >= 8) {
            passed = 9
            for (t in 0 until 9) an.add(An(t, today(), 5, 0))
        }
        if (Store.realmCap >= 9) suyPassed = true
        if (Store.realmCap == 7 && Store.trialStart >= 0) Store.trialStart = -1   // the one thiên kiếp of 2.5 gives way to nine tầng
        kiepBase = Store.totalWords
        lifeStart = (Store.daySeconds.keys.minOrNull() ?: today()).coerceAtMost(today())
        Store.touch()
    }

    // --- Saving -------------------------------------------------------------------------------------------

    fun toJson(): JSONObject = JSONObject().apply {
        put("passed", passed); put("active", active); put("activeDay", activeDay); put("book", book); put("broken", broken)
        put("fails", JSONArray().apply { fails.forEach { put(it) } }); put("rest", restUntil); put("kiepBase", kiepBase)
        put("an", JSONArray().apply { an.forEach { put(JSONArray().put(it.tang).put(it.day).put(it.by).put(it.fails)) } })
        put("last", lastResult); put("suyStart", suyStart); put("suyPassed", suyPassed); put("suySeen", suySeen)
        put("lifeStart", lifeStart); put("rebirths", rebirths); put("prevMax", prevMax)
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        passed = o.optInt("passed"); active = o.optInt("active", -1); activeDay = o.optLong("activeDay", -1)
        book = o.optString("book"); broken = o.optBoolean("broken")
        o.optJSONArray("fails")?.let { a -> for (i in 0 until minOf(9, a.length())) fails[i] = a.getInt(i) }
        restUntil = o.optLong("rest", -1); kiepBase = o.optLong("kiepBase")
        o.optJSONArray("an")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { an.add(An(it.getInt(0), it.getLong(1), it.getInt(2), it.getInt(3))) } }
        lastResult = o.optString("last"); suyStart = o.optLong("suyStart", -1); suyPassed = o.optBoolean("suyPassed"); suySeen = o.optLong("suySeen", -1)
        lifeStart = o.optLong("lifeStart", -1); rebirths = o.optInt("rebirths"); prevMax = o.optInt("prevMax", -1)
    }
}
