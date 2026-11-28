package com.exacta.reader.data

import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Realm
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/**
 * 3.0 Cửu Tiêu Khai Thiên: part two begins. The tiên lộ of a giới, common to the six:
 *
 *  - Nine bậc tiên vị. Each asks two things: the words read since the crossing (as tu vi did) and the
 *    giới's own resource, which only the giới's way of reading makes. Every bậc is sơ, trung, hậu kỳ
 *    and đại viên mãn by the words.
 *  - Tiên kiếp guard bậc 3, 6 and 9: a reading trial with a limit of days, then one roll. A failure
 *    keeps every bậc held, only wounds (half the resource for 7 days) and makes the next try easier.
 *  - What was carried through Lục Giới Môn becomes the giới's: the bản mệnh its tiên khí, the công
 *    pháp its tiên công, the linh căn its tiên căn, the kiếp ấn; each adds to the resource.
 *  - Tiên thạch (100 linh thạch to 1 when the giới opens) raise the three halls of the seat.
 *  - Three factions: the cultivator serves one as khách khanh, three tasks a month, ranks by cống hiến.
 *
 * The resource is counted from the journal of 2.8 ([NhatKy.estimate]), day by day once a day is over;
 * the days at the tiền đình are counted the same way when the giới opens.
 *
 * Each giới writes its own part as [GioiRules]: Cửu Tiêu Vân Khuyết in 3.0 ([CuuTieu]), Huyết Nguyệt
 * Trầm Uyên in 3.5 ([HuyetNguyet]), Bát Nhã Liên Đài in 3.7 ([BatNha]). The others wait at their tiền đình ([OPEN]).
 */
object TienLo {
    /** The giới whose part two is written. */
    val OPEN = intArrayOf(0, 1, 2)

    /** The rules of the giới entered. */
    fun rules(): GioiRules = when (gioi) {
        1 -> HuyetNguyet
        2 -> BatNha
        else -> CuuTieu
    }

    val BAC_VAN = longArrayOf(0, 60, 140, 240, 360, 500, 660, 840, 1040)
    val BAC_RES = intArrayOf(0, 200, 500, 900, 1400, 2000, 2700, 3500, 4400)
    /** Bậc 9 is đại viên mãn at this many vạn. */
    const val TOP_VAN = 1240L
    val KIEP_AT = intArrayOf(3, 6, 9)

    val VI = arrayOf(
        arrayOf("Thiên Tiên", "Chân Tiên", "Huyền Tiên", "Kim Tiên", "Thái Ất Kim Tiên", "Đại La Kim Tiên", "Tiên Quân", "Tiên Tôn", "Cửu Tiêu Tiên Đế"),
        arrayOf("Nghịch Tiên", "Huyết Ảnh", "Ma Tướng", "Ma Soái", "Ma Quân", "Ma Vương", "Ma Hoàng", "Ma Tôn", "Nghịch Thiên Ma Đế"),
        arrayOf("Thiền Tiên", "Tịnh Giả", "Minh Tâm Tôn Giả", "Kim Thân Tôn Giả", "Bồ Đề Tử", "Liên Đài Tôn Giả", "Đại Trí Tôn Giả", "Bát Nhã Tôn Giả", "Liên Hoa Thánh Tôn"),
        arrayOf("Chiến Tiên", "Hoang Binh", "Hoang Tướng", "Man Vương", "Cổ Thần Thể", "Bất Diệt Chiến Thể", "Hồng Hoang Chiến Thần", "Cổ Tổ", "Hồng Hoang Chiến Tổ"),
        arrayOf("Minh Tiên", "Dẫn Hồn Sứ", "Phán Quan", "Minh Quân", "Luân Hồi Sứ", "Bỉ Ngạn Hành Giả", "Minh Hà Chi Chủ", "Luân Hồi Thánh Giả", "Vong Xuyên Minh Đế"),
        arrayOf("Văn Tiên", "Mặc Khách", "Văn Sĩ", "Học Giả", "Đại Nho", "Văn Tông", "Văn Thánh", "Thư Hải Chi Chủ", "Vạn Quyển Thánh Đế"),
    )
    val SEAT = arrayOf("Thiên Đình", "Ma Cung", "Thiền Viện", "Tổ Địa", "Minh Phủ", "Thư Các")
    /** The pháp tắc that answer each giới: an đạo vực holding one «echoes», +5%. */
    val LAWS_OF = arrayOf(intArrayOf(0, 5), intArrayOf(3, 7), intArrayOf(4), intArrayOf(1, 4), intArrayOf(2, 7), intArrayOf(6))
    val TIEU = arrayOf("sơ kỳ", "trung kỳ", "hậu kỳ", "đại viên mãn")
    const val INJURY_DAYS = 7
    const val TT_WORDS = 40_000L          // a tiên thạch every 4 vạn words
    const val TT_BAC = 20L
    const val TT_KIEP = 30L
    val HALL_COST = longArrayOf(15, 30, 50, 80, 120)

    var started = false
    var opened = false
    var gioi = -1
    var openDay = -1L
    var bac = 1
    var resTenths = 0L           // the resource, in tenths of a point
    var countedTo = -1L          // the last day whose resource is counted
    var tienThach = 0L
    var ttWords = 0L
    // What the opening counted.
    var retro = 0
    var retroMeasured = 0
    var retroFilled = 0
    var fromLt = 0L
    var fromPham = 0
    var welcomeSeen = false
    var endDay = -1L             // the day bậc 9 was reached
    var endSeen = false
    // Kiếp.
    var kiepPassed = 0           // bits 0, 1, 2 for bậc 3, 6, 9
    var kiepActive = -1          // 0..2 while a kiếp runs
    var kiepStart = -1L
    val kiepDays = HashSet<Long>()
    val kiepFails = IntArray(3)
    var injuryUntil = -1L
    // Seat and faction.
    val halls = IntArray(3)
    var faction = -1
    var congHien = 0
    var taskMonth = -1L
    var tasksDone = 0
    var lastResult = ""
    var rng: Random = Random.Default

    fun reset() {
        started = false; opened = false; gioi = -1; openDay = -1; bac = 1; resTenths = 0; countedTo = -1; tienThach = 0; ttWords = 0
        retro = 0; retroMeasured = 0; retroFilled = 0; fromLt = 0; fromPham = 0; welcomeSeen = false; endDay = -1; endSeen = false
        kiepPassed = 0; kiepActive = -1; kiepStart = -1; kiepDays.clear(); kiepFails.fill(0); injuryUntil = -1
        halls.fill(0); faction = -1; congHien = 0; taskMonth = -1; tasksDone = 0; lastResult = ""
        CuuTieu.reset()
        HuyetNguyet.reset()
        BatNha.reset()
    }

    private fun today() = Store.today()

    // --- The way ------------------------------------------------------------------------------------

    fun res() = (resTenths / 10).toInt()
    fun words() = TienDinh.keptWords()
    fun floorWords(b: Int) = (if (b >= 10) TOP_VAN else BAC_VAN[(b - 1).coerceIn(0, 8)]) * 10_000
    fun viName(b: Int = bac) = VI[gioi.coerceIn(0, 5)][(b - 1).coerceIn(0, 8)]
    fun seat() = SEAT[gioi.coerceIn(0, 5)]
    fun supreme() = VI[gioi.coerceIn(0, 5)][8]

    /** Sơ, trung, hậu kỳ, đại viên mãn: the words through the bậc. */
    fun sub(): Int {
        val lo = floorWords(bac)
        val hi = floorWords(bac + 1)
        return ((words() - lo) * 4 / (hi - lo).coerceAtLeast(1)).toInt().coerceIn(0, 3)
    }

    fun name() = "${viName()} ${TIEU[sub()]}"

    fun bar(): TuLuyen.Bar {
        val lo = floorWords(bac)
        val hi = floorWords(bac + 1)
        val cells = ((words() - lo) * 12.0 / (hi - lo).coerceAtLeast(1)).coerceIn(0.0, 12.0)
        return TuLuyen.Bar(cells, 4, TIEU.toList(), sub())
    }

    /** Two lines for Home: (text, bold). */
    fun homeLines(): List<Pair<String, Boolean>> {
        val r = NhatKy.RESOURCE[gioi.coerceIn(0, 5)]
        return when {
            bac >= 9 -> listOf(supreme() to true, "${res()} $r · ${tienThach} tiên thạch" to false)
            kiepActive >= 0 -> listOf("${kiepName(kiepActive)} · ${kiepKept()}/${kiepNeed(kiepActive)} ngày" to true, "Còn ${kiepLeft()} ngày" to false)
            injured() -> listOf("Thương thế sau tiên kiếp" to true, "Còn ${injuryUntil - today() + 1} ngày, tài nguyên giảm nửa" to false)
            canStartKiep() -> listOf("Sẵn sàng ${kiepName(kiepDue())}" to true, "Vào ${seat()} › Kiếp" to false)
            else -> {
                val next = bac + 1
                val words = (floorWords(next) - words()).coerceAtLeast(0)
                val need = (BAC_RES[next - 1] - res()).coerceAtLeast(0)
                listOf("Tới ${viName(next)}" to true, when {
                    words > 0 -> "Còn ${Realm.van(words)} vạn chữ"
                    else -> "Còn $need $r"
                } to false)
            }
        }
    }

    fun kiepIndex(b: Int) = KIEP_AT.indexOf(b)
    fun kiepDone(i: Int) = (kiepPassed shr i) and 1 == 1
    fun injured() = today() <= injuryUntil

    /** The words and the resource the next bậc asks; met when both are. */
    fun wordsMet(b: Int = bac + 1) = b <= 9 && words() >= floorWords(b)
    fun resMet(b: Int = bac + 1) = b <= 9 && res() >= BAC_RES[b - 1]

    /** The next bậc waits on its kiếp. */
    fun kiepDue(): Int {
        val next = bac + 1
        val i = kiepIndex(next)
        return if (next <= 9 && i >= 0 && !kiepDone(i) && wordsMet(next) && resMet(next)) i else -1
    }

    fun canStartKiep() = kiepActive < 0 && kiepDue() >= 0 && !injured()

    /** Up the bậc that are met, stopping at a kiếp not passed. */
    fun advance() {
        while (bac < 9 && wordsMet() && resMet()) {
            val i = kiepIndex(bac + 1)
            if (i >= 0 && !kiepDone(i)) break
            bac++
            tienThach += TT_BAC
            Store.note("Lên ${viName()}: ${rules().CHUC_LABEL} ${rules().CHUC[bac - 1]}")
            if (bac == 9 && endDay < 0) {
                endDay = today()
                endSeen = false
            }
        }
    }

    // --- What was carried: the resource in percent ---------------------------------------------------

    /** Tiên khí cấp: the bản mệnh's cấm chế (a cấp each 4 tầng), raised by the seat's furnace. */
    fun tienKhi(): Int = (LinhGioi.banMenh.maxOfOrNull { TeLuyen.level(it) } ?: 0) / 4 + rules().tienKhiBonus()

    fun parts(): List<Pair<String, Int>> {
        val g = gioi.coerceIn(0, 5)
        val attrs = TuLuyen.GIOI_ATTR[g]
        val list = ArrayList<Pair<String, Int>>()
        tienKhi().takeIf { it > 0 }?.let { list.add("Tiên khí cấp $it" to it) }
        DaoPhap.activeDef()?.let { d ->
            list.add("Tiên công ${d.name} (${PhapMon.PHAM[d.pham]})" to d.pham + 1)
            if (d.attr in attrs) list.add("Tiên công hợp giới" to 5)
        }
        val roots = (if (DaoPhap.di >= 0) listOf(DaoPhap.di) else emptyList()) + DaoPhap.roots
        if (roots.any { it in attrs }) list.add("Tiên căn hợp giới" to 10)
        if (DoKiep.an.isNotEmpty()) list.add("Kiếp ấn ${DoKiep.an.size}" to DoKiep.an.size + DoKiep.an.count { DoKiep.BY_GIOI[it.by] == g })
        if (LinhGioi.vuc.any { it in LAWS_OF[g] }) list.add("Đạo vực «${LinhGioi.vucName}» vọng lại" to 5)
        list.addAll(rules().parts())
        return list
    }

    fun percent() = 100 + parts().sumOf { it.second }

    /** The resource of day `d` by the giới's rules and what was carried (halved while wounded). */
    fun dayTenths(d: Long): Long {
        var t = NhatKy.estimate(gioi, d, d).tenths * percent() / 100
        t += rules().dayExtra(d)
        if (d <= injuryUntil) t /= 2
        return t
    }

    /** Today so far, not yet counted. */
    fun todayPoints() = ((dayTenths(today()) + 5) / 10).toInt()

    // --- Opening the giới ---------------------------------------------------------------------------

    fun available() = TienDinh.crossed() && TienDinh.gioi in OPEN

    /** The part of the giới opens: the days at the tiền đình are counted, linh thạch become tiên thạch. */
    fun open() {
        val t = today()
        opened = true
        gioi = TienDinh.gioi
        openDay = t
        val e = NhatKy.estimate(gioi, TienDinh.crossDay, t - 1)
        resTenths = e.tenths * percent() / 100
        retro = res(); retroMeasured = e.measured; retroFilled = e.filled
        countedTo = t - 1
        fromLt = Store.linhThach / 100
        tienThach += fromLt
        Store.linhThach -= fromLt * 100
        // Cửu Tiêu: the phẩm of Thiên Tiên held become the bậc, their kiếp with them.
        if (gioi == 0) {
            fromPham = (Store.level() - Realm.TIEN_LEVEL + 1).coerceIn(1, 9)
            bac = fromPham
            for ((i, b) in KIEP_AT.withIndex()) if (bac >= b) kiepPassed = kiepPassed or (1 shl i)
        }
        rules().onOpen()
        advance()
        Store.note("${TuLuyen.GIOI[gioi]} mở cửa: ${viName()}, ${res()} ${NhatKy.RESOURCE[gioi]}")
        Store.touch()
    }

    // --- Each day -----------------------------------------------------------------------------------

    fun check() {
        if (!started) return
        if (!opened) {
            if (available()) open()
            return
        }
        val t = today()
        var changed = false
        while (countedTo < t - 1) {
            countedTo++
            if (countedTo >= openDay) {
                rules().beforeDay(countedTo)
                resTenths += dayTenths(countedTo)
            }
            changed = true
        }
        if (rules().check()) changed = true
        if (checkKiep()) changed = true
        if (checkTasks()) changed = true
        val before = bac
        advance()
        if (changed || bac != before) Store.touch()
    }

    fun onWords(words: Long) {
        if (!opened) return
        ttWords += words
        while (ttWords >= TT_WORDS) {
            ttWords -= TT_WORDS
            tienThach++
        }
    }

    // --- Kiếp ---------------------------------------------------------------------------------------

    fun kiepName(i: Int) = rules().KIEP_NAME[i]
    fun kiepRule(i: Int) = rules().KIEP_RULE[i]
    fun kiepWindow(i: Int) = rules().kiepWindow(i)
    fun kiepNeed(i: Int) = rules().kiepNeed(i)

    fun kiepChance(i: Int): Int {
        val v = 60 + 10 * kiepFails[i] + rules().kiepParts().sumOf { it.second } + minOf(10, DoKiep.an.size)
        return v.coerceIn(10, 95)
    }

    fun kiepParts(i: Int): List<Pair<String, Int>> = buildList {
        add("Gốc" to 60)
        if (kiepFails[i] > 0) add("Lần trước hỏng ×${kiepFails[i]}" to 10 * kiepFails[i])
        addAll(rules().kiepParts())
        if (DoKiep.an.isNotEmpty()) add("Kiếp ấn" to minOf(10, DoKiep.an.size))
    }

    fun startKiep(): Boolean {
        if (!canStartKiep()) return false
        kiepActive = kiepDue()
        kiepStart = today()
        kiepDays.clear()
        Store.note("Bắt đầu ${kiepName(kiepActive)}: ${kiepRule(kiepActive)}")
        Store.touch()
        return true
    }

    /** Days kept so far in the kiếp running. */
    fun kiepKept() = kiepDays.size
    fun kiepLeft() = if (kiepActive < 0) 0 else (kiepStart + kiepWindow(kiepActive) - today()).toInt().coerceAtLeast(0)

    private fun checkKiep(): Boolean {
        val i = kiepActive
        if (i < 0) return false
        val t = today()
        if (rules().kiepDayKept(i, t) && kiepDays.add(t)) Store.touch()
        if (kiepDays.size >= kiepNeed(i) && rules().kiepEnd(i)) {
            roll(i)
            return true
        }
        if (t - kiepStart >= kiepWindow(i)) {
            fail(i, "hết ${kiepWindow(i)} ngày mới giữ ${kiepDays.size}/${kiepNeed(i)}")
            return true
        }
        return false
    }

    private fun roll(i: Int) {
        val chance = kiepChance(i)
        if (rng.nextInt(100) < chance) {
            kiepPassed = kiepPassed or (1 shl i)
            kiepActive = -1
            tienThach += TT_KIEP
            lastResult = "Vượt ${kiepName(i)} (tỉ lệ $chance%)"
            Store.note(lastResult)
            advance()
        } else fail(i, "gieo hỏng ở tỉ lệ $chance%")
    }

    private fun fail(i: Int, why: String) {
        kiepFails[i]++
        kiepActive = -1
        NhatKy.event(NhatKy.E_FAIL)
        val w = rules().ward(i)
        if (w != null) {
            lastResult = "${kiepName(i)} hỏng: $why. $w, lần sau +10%"
            Store.note(lastResult)
            return
        }
        injuryUntil = today() + INJURY_DAYS
        lastResult = "${kiepName(i)} hỏng: $why. Thương thế $INJURY_DAYS ngày, lần sau +10%"
        Store.note(lastResult)
    }

    // --- Seat ---------------------------------------------------------------------------------------

    fun hallCost(h: Int) = if (halls[h] >= 5) -1L else HALL_COST[halls[h]]

    fun raiseHall(h: Int): Boolean {
        val cost = hallCost(h)
        if (cost < 0 || tienThach < cost) return false
        tienThach -= cost
        halls[h]++
        Store.note("${rules().HALLS[h]} lên cấp ${halls[h]}")
        Store.touch()
        return true
    }

    // --- Faction ------------------------------------------------------------------------------------

    val RANKS = arrayOf("Khách khanh", "Cung phụng", "Trưởng lão", "Thái thượng trưởng lão")
    val RANK_AT = intArrayOf(0, 60, 150, 300)
    fun rank() = RANK_AT.indexOfLast { congHien >= it }

    fun join(f: Int) {
        if (faction == f) return
        if (faction >= 0) congHien = 0
        faction = f
        taskMonth = monthOf(today())
        tasksDone = 0
        Store.note("Làm khách khanh ${rules().FACTIONS[f]}")
        Store.touch()
    }

    fun monthOf(d: Long): Long = LocalDate.ofEpochDay(d).let { it.year * 12L + it.monthValue - 1 }
    fun monthStart(d: Long): Long = LocalDate.ofEpochDay(d).withDayOfMonth(1).toEpochDay()

    /** The three tasks of the month: indices in the giới's TASKS. */
    fun tasks(month: Long = monthOf(today())): IntArray {
        val n = rules().TASKS.size
        val m = (month % n).toInt()
        return intArrayOf(m, (m + 2) % n, (m + 4) % n)
    }

    fun taskDone(k: Int) = (tasksDone shr k) and 1 == 1

    private fun checkTasks(): Boolean {
        if (faction < 0) return false
        val t = today()
        val m = monthOf(t)
        var changed = false
        if (m != taskMonth) {
            taskMonth = m
            tasksDone = 0
            changed = true
        }
        val from = maxOf(monthStart(t), openDay)
        for ((k, task) in tasks(m).withIndex()) {
            if (taskDone(k)) continue
            if (rules().taskProgress(task, from, t) >= rules().TASK_GOAL[task]) {
                tasksDone = tasksDone or (1 shl k)
                congHien += 10 + rules().taskBonus()
                tienThach += 5
                Store.note("Xong việc ${rules().FACTIONS[faction]}: ${rules().TASKS[task]}")
                changed = true
            }
        }
        return changed
    }

    // --- Saving -------------------------------------------------------------------------------------

    fun migrate() {
        reset()
        started = true
        if (available()) open()
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().put("opened", opened).put("gioi", gioi).put("openDay", openDay).put("bac", bac)
        .put("res", resTenths).put("counted", countedTo).put("tt", tienThach).put("ttWords", ttWords)
        .put("retro", retro).put("retroM", retroMeasured).put("retroF", retroFilled).put("fromLt", fromLt).put("fromPham", fromPham)
        .put("welcome", welcomeSeen).put("endDay", endDay).put("endSeen", endSeen)
        .put("kiepPassed", kiepPassed).put("kiepActive", kiepActive).put("kiepStart", kiepStart)
        .put("kiepDays", JSONArray().apply { kiepDays.forEach { put(it) } })
        .put("kiepFails", JSONArray().apply { kiepFails.forEach { put(it) } }).put("injury", injuryUntil)
        .put("halls", JSONArray().apply { halls.forEach { put(it) } }).put("faction", faction).put("cong", congHien)
        .put("taskMonth", taskMonth).put("tasks", tasksDone).put("last", lastResult).put("cuuTieu", CuuTieu.toJson()).put("huyetNguyet", HuyetNguyet.toJson())
        .put("batNha", BatNha.toJson())

    fun read(o: JSONObject) {
        reset()
        started = true
        opened = o.optBoolean("opened"); gioi = o.optInt("gioi", -1); openDay = o.optLong("openDay", -1); bac = o.optInt("bac", 1)
        resTenths = o.optLong("res"); countedTo = o.optLong("counted", -1); tienThach = o.optLong("tt"); ttWords = o.optLong("ttWords")
        retro = o.optInt("retro"); retroMeasured = o.optInt("retroM"); retroFilled = o.optInt("retroF"); fromLt = o.optLong("fromLt")
        fromPham = o.optInt("fromPham"); welcomeSeen = o.optBoolean("welcome"); endDay = o.optLong("endDay", -1); endSeen = o.optBoolean("endSeen")
        kiepPassed = o.optInt("kiepPassed"); kiepActive = o.optInt("kiepActive", -1); kiepStart = o.optLong("kiepStart", -1)
        o.optJSONArray("kiepDays")?.let { a -> for (i in 0 until a.length()) kiepDays.add(a.getLong(i)) }
        o.optJSONArray("kiepFails")?.let { a -> for (i in 0 until minOf(3, a.length())) kiepFails[i] = a.getInt(i) }
        injuryUntil = o.optLong("injury", -1)
        o.optJSONArray("halls")?.let { a -> for (i in 0 until minOf(3, a.length())) halls[i] = a.getInt(i) }
        faction = o.optInt("faction", -1); congHien = o.optInt("cong"); taskMonth = o.optLong("taskMonth", -1); tasksDone = o.optInt("tasks")
        lastResult = o.optString("last")
        o.optJSONObject("cuuTieu")?.let { CuuTieu.read(it) }
        o.optJSONObject("huyetNguyet")?.let { HuyetNguyet.read(it) }
        o.optJSONObject("batNha")?.let { BatNha.read(it) }
    }
}

/**
 * Cửu Tiêu Vân Khuyết, the giới of the even way. Tiên lực: a point each 2.500 words, times the
 * balance of the five measures (0,6 – 1,4). Thiên quy each week, tiên chức each bậc, the Tiên phủ
 * (Tiên điền, Tiên lô, Tiên trận), the factions Thiên Đình, Dao Trì, Tử Phủ, the Thiên Nhân kiếp.
 */
object CuuTieu : GioiRules {
    override val CHUC_LABEL = "tiên chức"
    override val SEAT_NAME = "Tiên phủ"
    override val GREET = "Mây chín tầng mở cửa khuyết."
    override val END_LINE = "Chín tầng mây cúi đầu."
    override val WORD_TASK = 4
    override val CHUC = arrayOf("Thiên Binh", "Tuần Thiên Sứ", "Tinh Quân", "Thiên Tướng", "Thiên Vương", "Tiên Khanh", "Đế Quân", "Thiên Tôn", "Tiên Đế")
    override val CHUC_PERK = arrayOf(
        "Thiên quy giữ được: +30 tiên lực",
        "Thiên quy thêm 10 tiên lực",
        "Giữ thiên quy được 2 tiên thạch",
        "Tiên lực +3%",
        "Tiên kiếp +5%",
        "Tiên lực +3% nữa",
        "Việc thế lực +5 cống hiến",
        "Tiên kiếp +5% nữa",
        "Đạo hiệu tối thượng «Cửu Tiêu Tiên Đế»",
    )

    val RULES = arrayOf(
        "Đọc 5 ngày trong tuần",
        "Cả tuần không nhảy chương (đọc từ 3 ngày)",
        "Đọc trọn một cuốn",
        "Không ngày nào dưới 15 phút",
        "Ba buổi đọc liền từ 30 phút",
        "Đọc 4 vạn chữ",
    )
    override val HALLS = arrayOf("Tiên điền", "Tiên lô", "Tiên trận")
    override val HALL_TEXT = arrayOf(
        "Trồng tiên thảo theo ngày đọc: mỗi ngày đọc từ 30 phút +1 tiên lực mỗi cấp.",
        "Luyện tiên khí từ bản mệnh: mỗi cấp +1 cấp tiên khí (+1% tiên lực).",
        "Trận pháp hộ thân khi độ kiếp: mỗi cấp tiên kiếp +2%.",
    )
    override val FACTIONS = arrayOf("Thiên Đình", "Dao Trì", "Tử Phủ")
    override val FACTION_TEXT = arrayOf(
        "Triều đình chín tầng mây, trọng phép tắc. Tiên lực +5%.",
        "Tiên cung bên ao ngọc, trọng nề nếp. Thiên quy thêm 15 tiên lực.",
        "Phủ sao Tử Vi, trọng thiên cơ. Tiên kiếp +5%.",
    )
    override val TASKS = arrayOf(
        "Đọc 20 ngày trong tháng",
        "Đọc trọn 2 cuốn",
        "10 ngày cân bằng từ 110%",
        "15 ngày đủ 30 phút",
        "Đọc 15 vạn chữ",
        "14 ngày đọc không nhảy chương",
    )
    override val TASK_GOAL = longArrayOf(20, 2, 10, 15, 150_000, 14)
    override val KIEP_NAME = arrayOf("Thiên Nhân kiếp thứ nhất", "Thiên Nhân kiếp thứ hai", "Đại kiếp Tiên Đế")
    override val KIEP_RULE = arrayOf(
        "21 ngày trong 30 ngày đọc từ 10 phút, năm thước đo không thước nào dưới 35%",
        "21 ngày trong 30 ngày đọc từ 10 phút, năm thước đo không thước nào dưới 45%",
        "81 ngày đọc trong 90 ngày, ngày cuối năm thước đo cân bằng từ 120%",
    )
    val MEASURE = arrayOf("Căn cơ", "Tâm cảnh", "Thần thức", "Thể phách", "Ngày đọc / 30")
    const val RULE_REWARD = 30

    var ruleWeek = -1L           // the Monday of the last week judged
    var kept = 0
    var broken = 0
    var lastRule = ""

    override fun reset() {
        ruleWeek = -1; kept = 0; broken = 0; lastRule = ""
    }

    private fun today() = Store.today()

    override fun parts(): List<Pair<String, Int>> = buildList {
        if (hasChuc(3)) add("Tiên chức ${CHUC[3]}" to 3)
        if (hasChuc(5)) add("Tiên chức ${CHUC[5]}" to 3)
        if (TienLo.faction == 0) add("Thế lực Thiên Đình" to 5)
        if (TienLo.faction >= 0 && TienLo.rank() > 0) add("${FACTIONS[TienLo.faction]} · ${TienLo.RANKS[TienLo.rank()]}" to TienLo.rank())
    }

    /** Tiên điền: a point a level on each day of 30 minutes (in tenths). */
    override fun dayExtra(d: Long): Long = if (Store.minutes(d) >= 30) 10L * TienLo.halls[0] else 0

    fun kiepBonus() = kiepParts().sumOf { it.second }
    override fun kiepParts(): List<Pair<String, Int>> = buildList {
        if (TienLo.halls[2] > 0) add("Tiên trận cấp ${TienLo.halls[2]}" to 2 * TienLo.halls[2])
        if (TienLo.faction == 2) add("Thế lực Tử Phủ" to 5)
        if (hasChuc(4)) add("Tiên chức ${CHUC[4]}" to 5)
        if (hasChuc(7)) add("Tiên chức ${CHUC[7]}" to 5)
    }

    override fun taskBonus() = if (hasChuc(6)) 5 else 0
    override fun tienKhiBonus() = TienLo.halls[1]
    override fun kiepWindow(i: Int) = if (i == 2) 90 else 30
    override fun kiepNeed(i: Int) = if (i == 2) 81 else 21

    fun ruleReward() = RULE_REWARD + (if (hasChuc(1)) 10 else 0) + (if (TienLo.faction == 1) 15 else 0)

    // --- Thiên quy ----------------------------------------------------------------------------------

    fun ruleOf(week: Long) = ((week / 7) % RULES.size).toInt().let { if (it < 0) it + RULES.size else it }

    /** How far the rule of the week starting `week` is kept, to `to`: (now, goal). */
    fun ruleProgress(week: Long, to: Long = week + 6): Pair<Long, Long> {
        val days = week..minOf(to, week + 6)
        return when (ruleOf(week)) {
            0 -> days.count { Store.minutes(it) >= 10 }.toLong() to 5L
            1 -> {
                val read = days.count { Store.minutes(it) >= 10 }
                val jumps = days.sumOf { TuLuyen.dayJumps[it] ?: 0 }
                (if (jumps == 0) minOf(read, 3) else 0).toLong() to 3L
            }
            2 -> Store.books.values.count { it.finishDay in days }.toLong() to 1L
            3 -> days.count { Store.minutes(it) >= 15 }.toLong() to 7L
            4 -> days.sumOf { d -> NhatKy.days[d]?.sittings?.count { it >= 1800 } ?: 0 }.toLong() to 3L
            else -> Store.wordsBetween(week, days.last) to 40_000L
        }
    }

    override fun onOpen() {
        ruleWeek = Store.weekStart(today()) - 7
    }

    /** Each finished week is judged once. */
    override fun check(): Boolean {
        val now = Store.weekStart(today())
        var changed = false
        while (ruleWeek + 7 < now) {
            ruleWeek += 7
            val (got, goal) = ruleProgress(ruleWeek)
            if (got >= goal) {
                kept++
                TienLo.resTenths += 10L * ruleReward()
                if (hasChuc(2)) TienLo.tienThach += 2
                lastRule = "Giữ thiên quy «${RULES[ruleOf(ruleWeek)]}»: +${ruleReward()} tiên lực"
            } else {
                broken++
                lastRule = "Lỡ thiên quy «${RULES[ruleOf(ruleWeek)]}»"
            }
            Store.note(lastRule)
            changed = true
        }
        return changed
    }

    // --- Tasks of the factions --------------------------------------------------------------------

    override fun taskProgress(task: Int, from: Long, to: Long): Long {
        val days = from..to
        return when (task) {
            0 -> days.count { Store.minutes(it) >= 10 }.toLong()
            1 -> Store.books.values.count { it.finishDay in days }.toLong()
            2 -> days.count { d -> Store.minutes(d) >= 10 && (NhatKy.days[d]?.balance ?: 0) >= 110 }.toLong()
            3 -> days.count { Store.minutes(it) >= 30 }.toLong()
            4 -> Store.wordsBetween(from, to)
            else -> days.count { Store.minutes(it) >= 10 && (TuLuyen.dayJumps[it] ?: 0) == 0 }.toLong()
        }
    }

    // --- Kiếp ---------------------------------------------------------------------------------------

    /** A day counts in the kiếp: read 10 minutes, and for the Thiên Nhân kiếp no measure below the line. */
    override fun kiepDayKept(i: Int, d: Long): Boolean {
        if (Store.minutes(d) < 10) return false
        if (i == 2) return true
        val line = if (i == 0) 0.35 else 0.45
        return NhatKy.measures().all { it >= line }
    }

    /** The đại kiếp ends only on a day the five measures stand balanced. */
    override fun kiepEnd(i: Int) = i != 2 || NhatKy.balance() >= 120

    override fun toJson(): JSONObject = JSONObject().put("week", ruleWeek).put("kept", kept).put("broken", broken).put("last", lastRule)

    override fun read(o: JSONObject) {
        reset()
        ruleWeek = o.optLong("week", -1); kept = o.optInt("kept"); broken = o.optInt("broken"); lastRule = o.optString("last")
    }
}
