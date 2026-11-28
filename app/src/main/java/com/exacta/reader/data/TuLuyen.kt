package com.exacta.reader.data

import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Treasure
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/**
 * 2.0 Ngũ Khí Triều Nguyên: the cultivator is more than a number of words.
 *
 *  - Five measures, all from the reading journal: linh lực (tu vi), căn cơ, tâm cảnh, thần thức,
 *    thể phách; and tâm ma, which grows with broken streaks, skipped chapters and failures.
 *  - Each major realm after Luyện Khí has four tiểu cảnh (sơ, trung, hậu kỳ, viên mãn); entering
 *    trung, hậu kỳ and viên mãn needs căn cơ 30, 45, 60. Short of it, only half the words count.
 *  - Bình cảnh: at the realm's ceiling only 20% of the words become linh lực; the rest is áp chế,
 *    which makes the breakthrough surer.
 *  - Đột phá: the reader taps to break into the next realm, with the chance shown part by part.
 *    The gates of Nhân giới ask their own things (căn cơ, ngưng đan, lôi kiếp and tâm ma kiếp,
 *    ngộ đạo); the realms beyond keep the thiên kiếp of 1.0. A failure slows and hurts, never takes
 *    an opened realm away.
 */
object TuLuyen {
    val TAM_CANH_TIERS = arrayOf("Phù Táo", "Bình Hòa", "Thanh Minh", "Chỉ Thủy", "Vô Cấu")
    val TAM_CANH_AT = intArrayOf(0, 25, 50, 75, 95)
    val TRONG = arrayOf("Nhất trọng", "Nhị trọng", "Tam trọng", "Tứ trọng", "Ngũ trọng", "Lục trọng", "Thất trọng", "Bát trọng", "Cửu trọng")
    val THAN_THUC_AT = intArrayOf(0, 2, 5, 9, 13, 18, 24, 31, 40)
    val THE_TIERS = arrayOf("Phàm Thể", "Đồng Bì", "Thiết Cốt", "Ngọc Tủy", "Kim Thân", "Lưu Ly Thân", "Bất Diệt Thể")
    val THE_AT = intArrayOf(0, 20, 60, 150, 300, 600, 1000)
    val TIEU = arrayOf("sơ kỳ", "trung kỳ", "hậu kỳ", "đại viên mãn")
    /** Luyện Khí's nine tầng (on the bar only) make three kỳ. */
    val LK_KY = arrayOf("sơ kỳ", "trung kỳ", "hậu kỳ")
    /** From this realm on, each tiểu cảnh asks a small breakthrough besides the căn cơ. */
    const val MINOR_FROM = 2
    /** Căn cơ to enter each tiểu cảnh. */
    val SUB_NEED = intArrayOf(0, 30, 45, 60)
    val TRUC_CO_PHAM = arrayOf("Hạ phẩm", "Trung phẩm", "Thượng phẩm", "Hoàn mỹ")
    val TRUC_CO_GOC = intArrayOf(0, 5, 10, 15)
    val DAN_PHAM = arrayOf("Cửu phẩm", "Bát phẩm", "Thất phẩm", "Lục phẩm", "Ngũ phẩm", "Tứ phẩm", "Tam phẩm", "Nhị phẩm", "Nhất phẩm", "Vô Hà")
    /** The chance a gate starts from (1 Trúc Cơ … 9 Tiên nhân). */
    val BASE = intArrayOf(0, 40, 35, 30, 30, 25, 25, 20, 20, 15)
    const val NGO_DAO_WORDS = 200_000L
    const val LONG_SITTING = 45 * 60L
    const val VERY_LONG_SITTING = 60 * 60L
    const val TAM_SITTING = 30 * 60L
    const val VONG_NGA_SITTING = 90 * 60L

    // --- State --------------------------------------------------------------------------------------

    var started = false       // the 2.0 state exists (else it is made from the 1.0 one)
    var adj = 0L              // tu vi held back (−) or added (+) by the rules of conversion
    var apChe = 0L            // words pressed down at the ceiling
    var subCap = 0            // the tiểu cảnh opened in the realm reached
    var tamMa = 0
    var lastTick = -1L        // the last whole day looked at
    var injuryUntil = -1L     // kinh mạch tổn thương: 70% until this day
    var restUntil = -1L       // hồi khí: no breakthrough before this day
    var tauHoaDay = -1L
    var tamMaKiep = false     // after tẩu hỏa nhập ma: no breakthrough until tâm ma is below 50
    var fails = 0             // failed rolls at this gate, +10% each
    var pill = false          // Phá Cảnh Đan taken for the next roll
    var trialDone = -1        // the gate whose ngưng đan, lôi kiếp or thiên kiếp is passed
    var trialBook = ""        // tâm ma kiếp: the book being read
    var attacks = 0
    var coPham = -1           // Trúc Cơ phẩm (0 hạ … 3 hoàn mỹ)
    var danPham = -1          // Kim Đan phẩm (0 cửu … 8 nhất, 9 Vô Hà)
    var breaks = 0
    var breakFails = 0
    var lastResult = ""
    val capDays = LongArray(10) { -1L }
    val failDays = ArrayList<Long>()
    var stageSeen = -1
    var ceilingSeen = -1
    var stuckSeen = -1
    var minorSeen = -1
    var minorRest = -1L
    var minorFails = 0
    val dayBest = HashMap<Long, Long>()      // the longest sitting of a day (s)
    val dayLong = HashMap<Long, Int>()       // sittings of 45 minutes or more
    val dayVeryLong = HashMap<Long, Int>()   // sittings of 60 minutes or more
    val dayJumps = HashMap<Long, Int>()      // jumps forward past unread text
    var rng: Random = Random.Default

    private fun today() = Store.today()

    fun reset() {
        started = false; adj = 0; apChe = 0; subCap = 0; tamMa = 0; lastTick = -1; injuryUntil = -1; restUntil = -1
        tauHoaDay = -1; tamMaKiep = false; fails = 0; pill = false; trialDone = -1; trialBook = ""; attacks = 0
        coPham = -1; danPham = -1; breaks = 0; breakFails = 0; lastResult = ""; capDays.fill(-1); failDays.clear()
        stageSeen = -1; ceilingSeen = -1; stuckSeen = -1; minorSeen = -1; minorRest = -1; minorFails = 0; gioi = -1
        dayBest.clear(); dayLong.clear(); dayVeryLong.clear(); dayJumps.clear()
    }

    // --- The five measures -------------------------------------------------------------------------

    private fun sum(map: HashMap<Long, Int>, from: Long, to: Long) = (from..to).sumOf { map[it] ?: 0 }
    private fun recent(day: Long) = day >= 0 && today() - day < 30
    fun daysMet(from: Long, to: Long, minutes: Int) = (from..to).count { Store.minutes(it) >= minutes }
    fun finishedSince(day: Long) = Store.books.values.count { it.finishDay >= day }
    fun jumps30(): Int = today().let { sum(dayJumps, it - 29, it) }
    fun long30(): Int = today().let { sum(dayLong, it - 29, it) }
    fun baseCanCo() = if (coPham in TRUC_CO_GOC.indices) TRUC_CO_GOC[coPham] else 0

    /** Căn cơ: 2 a day read 30 minutes (30 days), 10 a book read through (90 days, 4 at most), the Trúc Cơ's root. */
    fun canCo(): Int {
        val t = today()
        val v = 2 * daysMet(t - 29, t, 30) + 10 * minOf(4, finishedSince(t - 89)) + baseCanCo() + DaoPhap.canCoBonus() + DaoLo.canCoBonus() + LinhGioi.canCoBonus() - DongPhu.canCoPenalty() -
            (if (recent(tauHoaDay)) 10 else 0) - (jumps30() - 5).coerceIn(0, 20)
        return v.coerceIn(0, 100)
    }

    fun abandoned(): Int {
        val t = today()
        return Store.books.values.count { it.finishDay < 0 && it.progress in 0.05..0.95 && it.lastDay in (t - 90)..(t - 30) }
    }

    /** Tâm cảnh: the streak, long sittings, reading in order, a book finished; less failures and books left half read. */
    fun tamCanh(): Int {
        val t = today()
        val v = minOf(Store.streak(), 30) + 3 * minOf(10, long30()) + (30 - 2 * jumps30()).coerceAtLeast(0) +
            (if (finishedSince(t - 29) > 0) 10 else 0) - 10 * failDays.count { t - it < 30 } -
            (if (recent(tauHoaDay)) 20 else 0) - minOf(15, 3 * abandoned()) + DaoPhap.tamCanhBonus()
        return v.coerceIn(0, 100)
    }

    fun tamCanhTier(v: Int = tamCanh()) = TAM_CANH_AT.indexOfLast { v >= it }

    fun authors(): Int = Store.books.values.filter { it.finishDay >= 0 }.map { it.author.trim().lowercase() }.filter { it.isNotEmpty() }.toSet().size

    /** Thần thức: 5 an author read through, 1 a book. */
    fun thanThuc(): Int = (5 * authors() + Store.books.values.count { it.finishDay >= 0 } + (if (DaoPhap.effect(PhapMon.E_THAN_THUC)) 5 else 0)) * DaoLo.thanPercent() / 100 * LinhGioi.thanPercent() / 100
    fun trong(v: Int = thanThuc()) = THAN_THUC_AT.indexOfLast { v >= it }

    /** Thể phách: hours read, 3 a sitting of an hour. */
    fun thePhach(): Int = ((Store.totalSeconds / 3600).toInt() + 3 * dayVeryLong.values.sum() + DongPhu.theBonus).let { if (DaoPhap.effect(PhapMon.E_THE_PHACH)) it * 6 / 5 else it }.let { if (TongMon.perk(TongMon.THIET_COT)) it * 115 / 100 else it }.let { it * DaoLo.thePercent() / 100 * LinhGioi.thePercent() / 100 }
    fun theTier(v: Int = thePhach()) = THE_AT.indexOfLast { v >= it }

    // --- Realms and tiểu cảnh ----------------------------------------------------------------------

    fun floor(r: Int) = Realm.floor(r.coerceIn(0, 9) * 3)
    fun quarter(r: Int, q: Int) = floor(r) + (floor(r + 1) - floor(r)) * q / 4
    fun gate() = Store.nextGate()
    fun realm() = Realm.realmOf(Store.level())

    /** Tiểu cảnh of `words` in realm r: Luyện Khí's tầng 0..8, else 0..3. */
    fun sub(r: Int, words: Long): Int = if (r == 0) (words * 9 / floor(1)).toInt().coerceIn(0, 8)
        else ((words - floor(r)) * 4 / (floor(r + 1) - floor(r))).toInt().coerceIn(0, 3)

    fun shownSub(): Int {
        val r = realm()
        val s = sub(r, Store.tuVi())
        return if (r in 1..8 && r == Store.realmCap) minOf(s, subCap) else s
    }

    /** At the top of the realm reached: đan điền full. */
    fun atCeiling() = Store.realmCap in 0..8 && Store.tuVi() >= floor(Store.realmCap + 1)

    fun shortName(r: Int) = Realm.baseName(r)

    // --- Lục Giới Môn: from Phi Thăng, one of six realms and one of six kinds of immortal ---------

    val GIOI = arrayOf("Cửu Tiêu Vân Khuyết", "Huyết Nguyệt Trầm Uyên", "Bát Nhã Liên Đài", "Hồng Hoang Cổ Địa", "Vong Xuyên Bỉ Ngạn", "Thư Hải Vô Nhai")
    val TIEN_KIND = arrayOf("Thiên Tiên", "Nghịch Tiên", "Thiền Tiên", "Chiến Tiên", "Minh Tiên", "Văn Tiên")
    val GIOI_TEXT = arrayOf(
        "Chín tầng mây dựng cung khuyết, tiên quang tiếp dẫn người đi đường chính: thành Thiên Tiên, chín phẩm tiên lực.",
        "Trăng máu treo trên vực sâu, chỉ kẻ từng ngã rồi tự đứng dậy mới thấy lối: thành Nghịch Tiên, tâm ma hóa sức.",
        "Đài sen trí tuệ nở giữa hư không, lòng không vướng bụi mới ngồi được: thành Thiền Tiên, lấy công đức làm tu vi.",
        "Đất hoang thuở trời đất mới mở, chỉ nhục thân cứng như sắt mới đứng vững: thành Chiến Tiên, thân bất hoại.",
        "Bên kia sông quên lãng, những gì bỏ dở đều chờ người quay lại: thành Minh Tiên, nhớ trọn các đời tu.",
        "Biển sách không bờ, mỗi tác giả là một vì sao: thành Văn Tiên, thần thức bao trùm một phương.",
    )
    var gioi = -1             // the realm ascended to, fixed at the breakthrough into Tiên nhân

    // Lục Giới cảm ứng: not chosen. Each realm answers what the whole life of cultivation has been,
    // and the strongest answer is where the cultivator ascends.
    //   A. Linh căn (ngũ hành and the dị linh căn): the strongest root 30, then 18, 12, 8, 6.
    //   B. Công pháp đang vận: its attribute 20, đại thành (tầng 9) +5.
    //   C. Tâm pháp nhập môn: 8 each (Đạo Tâm Chủng Ma 15).
    //   D. Đạo lộ: the main way 20, the way of kiêm tu 10.
    //   E. Đời tu: what each realm asks of the five measures and the reading, up to 25.
    //   F. Tông môn: the sect that leads to it +10.
    /** The attributes of each realm: hành of the linh căn and of the công pháp. */
    val GIOI_ATTR = arrayOf(
        intArrayOf(PhapMon.KIM, PhapMon.LOI), intArrayOf(PhapMon.HOA, PhapMon.KIEM), intArrayOf(PhapMon.THO, PhapMon.PHONG),
        intArrayOf(PhapMon.MOC, 9), intArrayOf(PhapMon.THUY, PhapMon.BANG), intArrayOf(10),
    )
    /** The tâm pháp that lean to each realm. */
    val GIOI_TAM = arrayOf(
        intArrayOf(PhapMon.TP_TINH_TOA), intArrayOf(PhapMon.TP_CHUNG_MA), intArrayOf(PhapMon.TP_THANH_TAM, PhapMon.TP_VONG_NGA),
        intArrayOf(PhapMon.TP_DUONG_KHI), intArrayOf(PhapMon.TP_BANG_TAM, PhapMon.TP_DINH_HON), intArrayOf(),
    )
    /** The đạo lộ (main 20, kiêm 10) and the sect (+10) of each realm. */
    val GIOI_WAY = intArrayOf(DaoLo.PHAP, DaoLo.KIEM, -1, DaoLo.THE, DaoLo.BACH, DaoLo.THAN)
    val GIOI_SECT = intArrayOf(TongMon.VAN_QUYEN, TongMon.KIEM_LU, TongMon.MINH_TAM, TongMon.THIET_COT, TongMon.BACH_THAO, -1)
    /** What the life of cultivation must show, in words (E). */
    val GIOI_LIFE = arrayOf(
        "năm thước đo cân bằng: căn cơ 60, tâm cảnh Thanh Minh, thần thức Ngũ trọng, thể phách Thiết Cốt, tâm ma dưới 20; 5 điểm mỗi điều",
        "ngã rồi đứng dậy: 5 điểm mỗi lần đột phá hỏng (tối đa 15), từng tẩu hỏa nhập ma 10",
        "lòng tĩnh: 5 điểm mỗi bậc tâm cảnh, chuỗi ngày đọc dài nhất từ 100 ngày thêm 5",
        "nhục thân: 4 điểm mỗi bậc thể phách",
        "quay lại sách cũ: 8 điểm mỗi cuốn bỏ dở từ ba tháng rồi đọc xong",
        "đọc rộng: 3 điểm mỗi trọng thần thức; ngũ hành tạp linh căn thêm 15",
    )
    private val ROOT_W = intArrayOf(30, 18, 12, 8, 6)

    private fun lateFinished() = Store.books.values.count { it.finishDay >= 0 && it.firstDay >= 0 && it.finishDay - it.firstDay >= 90 }

    /** The answer of realm i, part by part. */
    fun gioiParts(i: Int): List<Pair<String, Int>> {
        val list = ArrayList<Pair<String, Int>>()
        val attrs = GIOI_ATTR[i]
        // A. Linh căn: the dị linh căn first, then the roots strongest first.
        val roots = (if (DaoPhap.di >= 0) listOf(DaoPhap.di) else emptyList()) + DaoPhap.roots
        var root = 0
        for ((n, r) in roots.withIndex()) if (r in attrs) root += ROOT_W[minOf(n, ROOT_W.size - 1)]
        if (root > 0) list.add("Linh căn ${roots.filter { it in attrs }.joinToString(", ") { PhapMon.attrName(it) }}" to root)
        // B. Công pháp.
        DaoPhap.activeDef()?.let { d ->
            if (d.attr in attrs) list.add("Công pháp ${d.name}" + (if (DaoPhap.tang() >= 9) ", đại thành" else "") to 20 + if (DaoPhap.tang() >= 9) 5 else 0)
        }
        // C. Tâm pháp.
        for (t in GIOI_TAM[i]) if (DaoPhap.tamLevel(t) >= 1) list.add("Tâm pháp ${PhapMon.TAM_PHAP[t].name}" to if (t == PhapMon.TP_CHUNG_MA) 15 else 8)
        // D. Đạo lộ.
        val w = GIOI_WAY[i]
        if (w >= 0 && DaoLo.way == w) list.add("Đạo lộ ${DaoLo.WAYS[w].name}" to 20)
        if (w >= 0 && DaoLo.way2 == w) list.add("Kiêm tu ${DaoLo.WAYS[w].name}" to 10)
        // E. Đời tu.
        val life = when (i) {
            0 -> 5 * listOf(canCo() >= 60, tamCanhTier() >= 2, trong() >= 4, theTier() >= 2, tamMa < 20).count { it }
            1 -> minOf(15, 5 * breakFails) + (if (tauHoaDay >= 0) 10 else 0)
            2 -> 5 * tamCanhTier() + (if (Store.bestStreak() >= 100) 5 else 0)
            3 -> 4 * theTier()
            4 -> minOf(24, 8 * lateFinished())
            else -> 3 * trong() + (if (DaoPhap.roots.size >= 5) 15 else 0)
        }.coerceAtMost(25 + if (i == 5) 15 else 0)
        if (life > 0) list.add("Đời tu" to life)
        // G. Kim Đan and Hóa Thần lean to a realm.
        if (Store.realmCap >= 2 && DaoLo.DANS[DaoLo.danNow()].gioi == i) list.add(DaoLo.DANS[DaoLo.danNow()].name to 10)
        if (Store.realmCap >= 4 && DaoLo.THANS[DaoLo.thanNow()].gioi == i) list.add(DaoLo.THANS[DaoLo.thanNow()].name to 10)
        // H. Nghiệp lực, from Phi Thăng.
        DaoLo.karmaPart(i).takeIf { it != 0 }?.let { list.add("Nghiệp lực ${DaoLo.karma()}" to it) }
        // F. Tông môn.
        if (GIOI_SECT[i] >= 0 && TongMon.family() == GIOI_SECT[i]) list.add(TongMon.SECTS[TongMon.sect].name to 10)
        return list
    }

    fun gioiScore(i: Int) = gioiParts(i).sumOf { it.second }

    /** The realm that answers most (the first of equals). */
    fun destined(): Int = GIOI.indices.maxByOrNull { gioiScore(it) * 10 - it } ?: 0

    /** The answer is clear when the first realm leads the second by 5 or more. */
    const val GIOI_LEAD = 5
    fun gioiLead(): Int {
        val s = GIOI.indices.map { gioiScore(it) }.sortedDescending()
        return s[0] - s[1]
    }
    fun gioiClear() = gioiLead() >= GIOI_LEAD

    /** The realm ascended to, or the one that answers now. */
    fun gioiNow() = if (gioi >= 0) gioi else destined()
    fun tienKind() = if (Store.realmCap >= 9) TIEN_KIND[gioiNow()] else "Tiên nhân"

    /** "Luyện Khí trung kỳ", "Kim Đan hậu kỳ", "Nguyên Anh đại viên mãn", "Tiên nhân tam phẩm". */
    fun name(): String {
        val r = realm()
        if (r >= 9) return Realm.name(Store.level()).replace("Tiên nhân", tienKind())
        val s = shownSub()
        return if (r == 0) "Luyện Khí " + (if (atCeiling()) TIEU[3] else LK_KY[s / 3]) else "${shortName(r)} ${TIEU[s]}"
    }

    class Bar(val cells: Double, val groups: Int, val labels: List<String>, val here: Int)

    /** The realm bar: three groups of three tầng in Luyện Khí, four tiểu cảnh after, the old phẩm in Tiên. */
    fun bar(): Bar {
        val r = realm()
        val t = Store.tuVi()
        return when {
            r >= 9 -> Bar(Realm.ninths(t, Store.level()), 3, Realm.stageLabels(Store.level()), Realm.stageIndex(Store.level()))
            r == 0 -> Bar((t * 9.0 / floor(1)).coerceIn(0.0, 9.0), 3, LK_KY.toList(), shownSub() / 3)
            else -> {
                val limit = if (r == Store.realmCap) subCap + 1 else 4
                val cells = ((t - floor(r)) * 12.0 / (floor(r + 1) - floor(r))).coerceIn(0.0, limit * 3.0)
                Bar(cells, 4, TIEU.toList(), shownSub())
            }
        }
    }

    /** Short of căn cơ at the next tiểu cảnh: only half the words count. */
    fun stuck(): Boolean {
        val cap = Store.realmCap
        if (cap !in 1..8 || subCap >= 3 || realm() != cap) return false
        return Store.tuVi() >= quarter(cap, subCap + 1) && canCo() < SUB_NEED[subCap + 1]
    }

    // --- Tiểu đột phá ---------------------------------------------------------------------------

    /** From Kim Đan: the words and the căn cơ of the next tiểu cảnh are there, the small breakthrough waits. */
    fun minorReady(): Boolean {
        val cap = Store.realmCap
        if (cap !in MINOR_FROM..8 || subCap >= 3 || realm() != cap) return false
        return Store.tuVi() >= quarter(cap, subCap + 1) && canCo() >= SUB_NEED[subCap + 1]
    }

    fun minorResting() = today() < minorRest

    /** Light: 60% and a third of căn cơ, the tâm cảnh, less a third of tâm ma; 30 – 98%. */
    fun minorChance() = (60 + canCo() / 3 + 5 * tamCanhTier() + DaoLo.minorPart() - tamMa / 3 + 10 * minorFails).coerceIn(30, 98)

    /** Tries the small breakthrough: true held, false failed (tâm ma +3, again tomorrow), null not now. */
    fun minorBreak(): Boolean? {
        if (!minorReady() || minorResting()) return null
        val ok = rng.nextInt(100) < minorChance()
        if (ok) {
            subCap++
            minorFails = 0
            lastResult = "Phá tiểu cảnh vào ${TIEU[subCap]} ngày ${dayText(today())}"
            Store.note("Phá tiểu cảnh: ${name()}")
            Store.fortune(com.exacta.reader.game.KyNgo.T_MINOR)
            advance()
        } else {
            minorFails++
            stir(3)
            minorRest = today() + 1
            lastResult = "Phá tiểu cảnh hỏng ngày ${dayText(today())}: tâm ma +3, mai thử lại (+10%)"
            Store.note(lastResult)
        }
        Store.touch()
        return ok
    }

    fun injured() = today() < injuryUntil
    fun resting() = today() < restUntil

    /** Percent of the words read that become linh lực now. */
    fun rate(): Int = (if (atCeiling()) ceilingRate() else if (stuck()) knotRate() else 100) * (if (injured()) 70 else 100) / 100 *
        (if (BiCanh.hurt()) 85 else 100) / 100
    fun ceilingRate() = if (DaoPhap.effect(PhapMon.E_BINH_CANH)) 30 else 20
    fun knotRate() = if (DaoPhap.effect(PhapMon.E_NUT_THAT)) 70 else 50

    /** Kim Đan phẩm speeds the way to Nguyên Anh: 2% a phẩm, Vô Hà 25%. */
    fun danBonus(): Int = if (Store.realmCap == 2 && danPham >= 0) (if (danPham >= 9) 25 else 2 * (danPham + 1)) else 0

    /** The rate and whether at the ceiling, taken before a page's words are added. */
    fun state() = rate() to atCeiling()

    /**
     * Tu vi gained by `gain` words of reading: what the rules keep (with the speed of the linh căn
     * and the công pháp), and the áp chế at the ceiling.
     */
    fun convert(gain: Long, state: Pair<Int, Boolean> = state()) {
        if (gain <= 0 || Store.realmCap !in 0..8) return
        val (pct, ceiling) = state
        val kept = gain * pct / 100 * (100 + danBonus()) / 100 * DaoPhap.speed() / 100
        adj += kept - gain
        if (ceiling) apChe += gain * (if (DaoPhap.effect(PhapMon.E_AP_CHE)) 100 else 80) / 100
    }

    /** Tâm ma rises by `n`, less with Thanh Tâm Quyết and a công pháp that calms. */
    fun stir(n: Int) {
        tamMa = (tamMa + (n * DaoPhap.stirPercent() * DaoLo.stirPercent() / 100 * LinhGioi.stirPercent() / 100 + 50) / 100).coerceAtMost(100)
    }

    /** Áp chế: 5% a tenth of the realm, 20% at most. */
    fun apCheBonus(): Int {
        val cap = Store.realmCap
        if (cap !in 0..8) return 0
        val span = floor(cap + 1) - floor(cap)
        return (apChe * 10 / span * 5).toInt().coerceIn(0, 20)
    }

    // --- The gates ----------------------------------------------------------------------------------

    fun hasTrial(g: Int) = g == 2 || g == 3 || g >= 5

    /** Books for ngộ đạo: 20 vạn words or more, read through within 30 days, since Nguyên Anh. */
    fun ngoDao(): List<Book> = Store.books.values.filter {
        it.finishDay >= 0 && it.words >= NGO_DAO_WORDS && it.firstDay >= 0 && it.finishDay - it.firstDay < 30 &&
            (capDays[3] < 0 || it.finishDay >= capDays[3])
    }

    fun trialName(g: Int = gate()) = when (g) {
        2 -> "Ngưng đan"
        3 -> "Lôi kiếp"
        else -> "Thiên kiếp"
    }

    fun trialRule(g: Int = gate()) = when (g) {
        2 -> "${Store.trialNeed(g)} ngày, mỗi ngày một buổi đọc liền từ ${Store.trialMinutes(g)} phút"
        3 -> "${Store.trialNeed(g)} ngày × ${Store.trialMinutes(g)} phút, kèm tâm ma kiếp"
        else -> "${Store.trialNeed(g)} ngày × ${Store.trialMinutes(g)} phút"
    }

    class Need(val text: String, val met: Boolean)

    /** What the next gate asks, each with whether it is met. */
    fun needs(trial: Boolean = true): List<Need> {
        val g = gate()
        if (Store.realmCap !in 0..8) return emptyList()
        val t = Store.tuVi()
        val list = ArrayList<Need>()
        val top = floor(g)
        val where = "${shortName(Store.realmCap)} đại viên mãn"
        list.add(Need("$where: ${Realm.van(minOf(t, top))}/${top / 10_000} vạn", t >= top))
        when (g) {
            1 -> list.add(Need("Căn cơ từ 30, đang ${canCo()}", canCo() >= 30))
            2 -> list.add(Need(if (DaoLo.way >= 0) "Đạo lộ: ${DaoLo.WAYS[DaoLo.way].name}" else "Chọn đạo lộ ở màn Đạo lộ", DaoLo.way >= 0))
            4 -> {
                list.add(Need("Thần thức Lục trọng, đang ${TRONG[trong()]}", trong() >= 5))
                val n = ngoDao().size
                list.add(Need("Ngộ đạo ${minOf(n, 3)}/3: cuốn từ 20 vạn chữ đọc trọn trong 30 ngày", n >= 3))
            }
        }
        list.addAll(LinhGioi.needs(g))
        if (Store.realmCap >= 1) list.add(Need("Đã vào đại viên mãn (căn cơ từ 60 khi tới)", subCap >= 3))
        if (g == 9) list.add(Need("Lục Giới cảm ứng rõ: ${GIOI[destined()]} hơn giới kế tiếp từ $GIOI_LEAD điểm (đang ${gioiLead()})", gioiClear()))
        if (g >= 2) {
            val p = PhapMon.phamFor(g)
            val now = DaoPhap.activeDef()?.let { "${it.name}, ${PhapMon.PHAM[it.pham]} phẩm" } ?: "chưa vận"
            list.add(Need("Công pháp ${PhapMon.PHAM[p]} phẩm trở lên (đang vận $now)", DaoPhap.canBreakWith(g)))
        }
        if (trial && hasTrial(g)) list.add(Need("${trialName(g)}: ${trialRule(g)}", trialDone == g))
        return list
    }

    /** Why a breakthrough cannot be tried now, even with every need met. */
    fun blockers(): List<String> {
        val list = ArrayList<String>()
        if (resting()) list.add("Hồi khí còn ${restUntil - today()} ngày")
        if (tamMaKiep && tamMa >= 50) list.add("Tâm ma kiếp: hạ tâm ma dưới 50 (đang $tamMa)")
        if (Store.inTrial()) list.add("Đang ${trialName().lowercase()}")
        return list
    }

    fun canStartTrial(): Boolean {
        val g = gate()
        return Store.realmCap in 0..8 && hasTrial(g) && trialDone != g && !Store.inTrial() && needs(trial = false).all { it.met } && blockers().isEmpty()
    }

    fun canBreak() = Store.realmCap in 0..8 && needs().all { it.met } && blockers().isEmpty()

    /** The chance of the next breakthrough, part by part. */
    fun parts(): List<Pair<String, Int>> {
        val g = gate()
        val cc = canCo()
        val tier = tamCanhTier()
        val list = arrayListOf(
            "Nền cửa ${Treasure.GATES[g]}" to BASE[g],
            "Căn cơ $cc, chia đôi" to cc / 2,
            "Tâm cảnh ${TAM_CANH_TIERS[tier]}" to tier * 5,
        )
        if (apCheBonus() > 0) list.add("Áp chế cảnh giới" to apCheBonus())
        DaoPhap.activeDef()?.let { d ->
            if (DaoPhap.breakPart() > 0) list.add("Công pháp ${PhapMon.PHAM[d.pham]} phẩm tầng ${DaoPhap.tang()}" to DaoPhap.breakPart())
            if (d.effect == PhapMon.E_DOT_PHA) list.add("Đặc hiệu ${d.name}" to 5)
        }
        if (DaoPhap.phapTuong() > 0) list.add("Pháp Tướng Thiên Địa" to DaoPhap.phapTuong())
        if (DongPhu.breakBonus() > 0) list.add("Linh mạch ${DongPhu.giaiName()}" to DongPhu.breakBonus())
        if (TongMon.perk(TongMon.KIEM_LU)) list.add("Kiếm Lư kiếm ý" to 3)
        list.addAll(DaoLo.breakParts(g))
        list.addAll(LinhGioi.breakParts(g))
        if (DongPhu.breakPill[g] > 0) list.add(com.exacta.reader.game.DanDao.RECIPES[com.exacta.reader.game.DanDao.breakRecipe(g)].name to DongPhu.breakPill[g])
        if (DongPhu.danDoc >= 4) list.add("Đan độc ${DongPhu.danDoc}, chia bốn" to -(DongPhu.danDoc / 4))
        if (g == 9) list.add("Cảm ứng ${GIOI[destined()]}, chia mười" to minOf(10, gioiScore(destined()) / 10))
        if (pill) list.add("Phá Cảnh Đan" to 15)
        if (fails > 0) list.add("Kiên trì, $fails lần trước" to minOf(30, 10 * fails))
        if (tamMa > 1) list.add("Tâm ma $tamMa, chia đôi" to -(tamMa / 2))
        return list
    }

    fun chance() = parts().sumOf { it.second }.coerceIn(5, 95)

    private fun dayText(d: Long) = LocalDate.ofEpochDay(d).let { "${it.dayOfMonth}/${it.monthValue}" }

    fun startTrial(): Boolean {
        if (!canStartTrial()) return false
        Store.trialStart = today()
        Store.trialBonus = if (DaoPhap.effect(PhapMon.E_KIEP_NGAY)) 1 else 0
        Store.trialSpare = 0
        attacks = 0
        trialBook = ""
        DaoPhap.onTrialStart()
        Store.note("${trialName()} bắt đầu: ${trialRule()}" + if (Store.trialBonus > 0) " · công pháp tính sẵn 1 ngày" else "")
        Store.touch()
        return true
    }

    fun trialPassed() {
        val g = gate()
        trialDone = g
        Store.trialStart = -1
        Store.trialBonus = 0
        Store.trialSpare = 0
        DaoPhap.onTrialEnd()
        Store.note("Vượt ${trialName(g).lowercase()}: đột phá ${Realm.baseName(g)} được rồi")
        lastResult = "Vượt ${trialName(g).lowercase()} ngày ${dayText(today())}"
        Store.touch()
    }

    fun trialFailed(why: String) {
        val t = today()
        Store.trialStart = -1
        Store.trialBonus = 0
        Store.trialSpare = 0
        Store.failCount++
        Store.failDay = t
        failDays.add(t)
        while (failDays.size > 20) failDays.removeAt(0)
        val before = tamMa
        stir(15)
        restUntil = t + 2
        attacks = 0
        DaoPhap.onTrialEnd()
        lastResult = "$why ngày ${dayText(t)}: tâm ma +${tamMa - before}, hồi khí 2 ngày"
        Store.note("$why: tâm ma +${tamMa - before}, nghỉ hai ngày rồi thử lại")
        Store.touch()
    }

    const val HUT_KHI = 0
    const val KINH_MACH = 1
    const val TAU_HOA = 2

    /** Tries the breakthrough; true when it holds, false when it fails, null when it cannot be tried. */
    fun breakThrough(): Boolean? {
        if (!canBreak()) return null
        val g = gate()
        val t = today()
        val rate = chance()
        val roll = rng.nextInt(100)
        val ok = roll < rate
        DaoPhap.onRoll()
        DongPhu.onRoll(g)
        if (ok) {
            val margin = rate - roll
            var extra = ""
            when (g) {
                1 -> {
                    val score = canCo() + 2 * apCheBonus()
                    coPham = when { score < 45 -> 0; score < 65 -> 1; score < 85 -> 2; else -> 3 }
                    extra = " · Trúc Cơ ${TRUC_CO_PHAM[coPham].lowercase()}, căn cơ gốc +${TRUC_CO_GOC[coPham]}"
                }
                2 -> {
                    danPham = if (margin >= 70 && canCo() >= 90) 9 else (margin * 9 / rate).coerceIn(0, 8)
                    DaoLo.onBreak(2)
                    val f = DaoLo.DANS[DaoLo.dan].name
                    extra = if (danPham == 9) " · Vô Hà $f" else " · $f ${DAN_PHAM[danPham].lowercase()}"
                }
            }
            if (g == 4) {
                DaoLo.onBreak(4)
                extra = " · ${DaoLo.THANS[DaoLo.than].name}"
            }
            if (g == 3) extra = " · ${DaoLo.anhName()}"
            if (g == 5) {
                LinhGioi.keepChoice()
                extra = " · phi thăng Linh giới, bản mệnh ${LinhGioi.banMenh.mapNotNull { LinhGioi.beast(it)?.name }.joinToString(", ").ifEmpty { "không có" }}"
            }
            if (g == 9) {
                gioi = destined()
                extra = " · phi thăng ${GIOI[gioi]}, thành ${TIEN_KIND[gioi]}"
            }
            Store.realmCap = g
            capDays[g] = t
            subCap = 0
            apChe = 0
            fails = 0
            pill = false
            trialDone = -1
            attacks = 0
            trialBook = ""
            if (DaoLo.crit(g)) {
                // Kiếm tu: kiếm ý chí mạng, straight into trung kỳ.
                subCap = 1
                val short = quarter(g, 1) - Store.tuVi()
                if (short > 0) Store.bonusTuVi += short
                extra += " · kiếm ý chí mạng, vào thẳng trung kỳ"
            }
            breaks++
            Store.trialPassed++
            if (Store.failCount > 0) Store.passedAfterFail = true
            Store.gainLinhThach(50L * g)
            lastResult = "Đột phá ${Realm.baseName(g)} ngày ${dayText(t)}$extra"
            Store.note("Đột phá thành công: ${Realm.baseName(g)}$extra · ${50 * g} linh thạch")
            Store.fortune(com.exacta.reader.game.KyNgo.T_BREAK, g)
            advance()
        } else {
            // The độn thuật and the công pháp soften the fall.
            val f = DaoPhap.soften(failKind(rate))
            breakFails++
            fails++
            pill = false
            Store.failCount++
            Store.failDay = t
            failDays.add(t)
            while (failDays.size > 20) failDays.removeAt(0)
            stir(f.tamMa)
            restUntil = t + (f.restDays - LinhGioi.restLess()).coerceAtLeast(1)
            val what = when (f.kind) {
                HUT_KHI -> {
                    Store.tuViLoss += Store.tuVi() / f.lossDiv
                    "Hụt khí: mất ${100 / f.lossDiv}% linh lực"
                }
                KINH_MACH -> {
                    injuryUntil = t + f.injuryDays
                    "Kinh mạch tổn thương: ${f.injuryDays} ngày tu luyện chỉ còn 70%"
                }
                else -> {
                    tauHoaDay = t
                    tamMaKiep = true
                    "Tẩu hỏa nhập ma: tâm cảnh −20, căn cơ −10 trong 30 ngày"
                }
            } + (if (f.restDays != 2) ", hồi khí ${f.restDays} ngày" else "") +
                if (f.saved.isNotEmpty()) " (${f.saved.joinToString(", ")})" else ""
            lastResult = "Đột phá hỏng ngày ${dayText(t)} · $what"
            Store.note("Đột phá thất bại. $what")
        }
        Store.checkTitles()
        Store.touch()
        return ok
    }

    fun failKind(rate: Int = chance()) = when {
        tamMa >= DaoPhap.tauHoaAt() -> TAU_HOA
        rate >= 50 -> HUT_KHI
        else -> KINH_MACH
    }

    // --- Reading hooks ------------------------------------------------------------------------------

    private fun read(d: Long) = (Store.daySeconds[d] ?: 0) >= 60 || d in Store.sealedDays

    /** Each whole day once: a broken streak stirs tâm ma, a day of 30 minutes calms it a little. */
    fun tick() {
        val t = today()
        if (lastTick < 0 || lastTick > t) {
            lastTick = t - 1
            return
        }
        if (lastTick >= t - 1) return
        for (d in lastTick + 1..t - 1) {
            if (!read(d) && read(d - 1) && !DaoPhap.calmBreak(d)) stir(10)
            if (Store.minutes(d) >= 30) tamMa = (tamMa - 1).coerceAtLeast(0)
        }
        lastTick = t - 1
        if (tamMaKiep && tamMa < 50) tamMaKiep = false
        Store.touch()
    }

    fun calm(n: Int) {
        tamMa = (tamMa - n).coerceAtLeast(0)
        if (tamMaKiep && tamMa < 50) tamMaKiep = false
    }

    private fun lightningTrial() = Store.inTrial() && gate() == 3

    private fun attack(why: String) {
        if (!lightningTrial() || DaoPhap.block()) return
        attacks++
        if (attacks >= 3) trialFailed("Tâm ma kiếp thất bại")
        else Store.note("Tâm ma tấn công ($why): $attacks/3")
    }

    fun onSeconds(book: Book, seconds: Long, session: Long) {
        val d = today()
        tick()
        if (session > (dayBest[d] ?: 0)) dayBest[d] = session
        val before = session - seconds
        if (before < LONG_SITTING && session >= LONG_SITTING) {
            dayLong[d] = (dayLong[d] ?: 0) + 1
            calm(3)
        }
        if (before < VERY_LONG_SITTING && session >= VERY_LONG_SITTING) {
            dayVeryLong[d] = (dayVeryLong[d] ?: 0) + 1
            DaoLo.onLongSitting()
        }
        if (before < TAM_SITTING && session >= TAM_SITTING) DaoPhap.onSitting30()
        if (before < VONG_NGA_SITTING && session >= VONG_NGA_SITTING && DaoPhap.tamWorks(PhapMon.TP_VONG_NGA)) {
            // Vong Ngã Chân Quyết: a sitting of 90 minutes counts twice for thể phách and tâm cảnh.
            dayLong[d] = (dayLong[d] ?: 0) + 1
            dayVeryLong[d] = (dayVeryLong[d] ?: 0) + 1
        }
        if (lightningTrial()) {
            if (trialBook.isEmpty()) trialBook = book.path
            else if (trialBook != book.path) {
                trialBook = book.path
                attack("đổi sang cuốn khác")
            }
        }
    }

    /** A jump forward past text not read (the contents, the slider). */
    fun onJump() {
        val d = today()
        val n = (dayJumps[d] ?: 0) + 1
        dayJumps[d] = n
        if (n > 3 && !DaoPhap.quietJump()) stir(2)
        attack("nhảy chương")
        Store.touch()
    }

    fun onFinish() = calm(10)

    /** A sitting ends (the reader closed, or left idle). */
    fun onSessionEnd(seconds: Long) {
        if (seconds in 60 until 600) attack("bỏ dở buổi đọc dưới 10 phút")
    }

    /** The longest sitting of a day in minutes, Định Thần Phù's minutes included. */
    fun bestMinutes(d: Long) = ((dayBest[d] ?: 0) / 60 + (Store.bonusMinutes[d] ?: 0)).toInt()

    /** Opens the tiểu cảnh that căn cơ allows, and says when a new one, the ceiling or a knot is reached. */
    fun advance() {
        val cap = Store.realmCap
        if (cap in 1 until MINOR_FROM && realm() == cap) {
            val t = Store.tuVi()
            val cc = canCo()
            while (subCap < 3 && t >= quarter(cap, subCap + 1) && cc >= SUB_NEED[subCap + 1]) subCap++
        }
        if (minorReady() && minorSeen != cap * 10 + subCap) {
            minorSeen = cap * 10 + subCap
            Store.note("Tiểu bình cảnh: phá vào ${TIEU[subCap + 1]} ở Tu vi › Đột phá")
            Store.touch()
        }
        val r = realm()
        val key = if (r >= 9) 100 + Store.level() else r * 10 + shownSub()
        if (key > stageSeen) {
            if (stageSeen >= 0) Store.note("Tiến cảnh: ${name()}")
            stageSeen = key
            Store.touch()
        }
        if (atCeiling() && ceilingSeen != gate()) {
            ceilingSeen = gate()
            Store.note("Bình cảnh: đan điền đã đầy, vào Tu vi › Đột phá")
            Store.touch()
        }
        if (stuck()) {
            val k = cap * 10 + subCap
            if (stuckSeen != k) {
                stuckSeen = k
                Store.note("Nút thắt: cần căn cơ ${SUB_NEED[subCap + 1]} để vào ${TIEU[subCap + 1]}")
                Store.touch()
            }
        }
    }

    /** First run of 2.0: what 1.0 left is kept, the excess over the ceiling becomes áp chế. */
    fun migrate() {
        reset()
        started = true
        lastTick = today() - 1
        val cap = Store.realmCap
        if (cap in 0..8) {
            val t = Store.tuVi()
            val ceiling = floor(cap + 1)
            if (t > ceiling) apChe = t - ceiling
            // The gates that lost their thiên kiếp: a trial under way there ends.
            if (Store.trialStart >= 0 && cap + 1 in listOf(1, 2, 4)) Store.trialStart = -1
            if (cap >= 1) subCap = sub(cap, t)
        }
        val r = realm()
        stageSeen = if (r >= 9) 100 + Store.level() else r * 10 + shownSub()
        if (atCeiling()) ceilingSeen = gate()
        Store.touch()
    }

    // --- Lines for Home ----------------------------------------------------------------------------

    /** Two or three lines on the way ahead: (text, bold). */
    fun homeLines(): List<Pair<String, Boolean>> {
        val g = gate()
        val gateName = Treasure.GATES[g.coerceIn(1, 9)]
        return when {
            Store.realmCap >= 9 -> listOf("Đã phi thăng" to true, "Tiên lộ thênh thang" to false)
            Store.inTrial() -> {
                val today = Store.today()
                val now = if (g == 2) "buổi dài nhất ${bestMinutes(today)}/${Store.trialMinutes()} phút" else "hôm nay ${Store.minutes(today)}/${Store.trialMinutes()} phút"
                val tail = if (g == 3) " · tâm ma $attacks/3" else ""
                listOf("${trialName(g)} · ngày ${Store.trialDays().first}/${Store.trialNeed()}" to true,
                    (now.replaceFirstChar { it.uppercase() } + tail) to false)
            }
            resting() -> listOf("Hồi khí sau đột phá" to true, "Còn ${restUntil - today()} ngày, đọc tiếp" to false)
            canBreak() -> listOf("Bình cảnh $gateName" to true, "Đột phá được · tỉ lệ ${chance()}%" to false)
            canStartTrial() -> listOf("Bình cảnh $gateName" to true, "Sẵn sàng ${trialName(g).lowercase()}" to false)
            atCeiling() -> listOf("Bình cảnh $gateName" to true, (needs().firstOrNull { !it.met }?.text ?: "Vào Tu vi › Đột phá") to false)
            stuck() -> listOf("Nút thắt ${TIEU[subCap + 1]}" to true, "Căn cơ cần ${SUB_NEED[subCap + 1]}, đang ${canCo()}" to false)
            minorReady() -> listOf("Tiểu bình cảnh ${TIEU[subCap + 1]}" to true,
                (if (minorResting()) "Hồi khí tới mai" else "Phá tiểu cảnh được · ${minorChance()}%") to false)
            else -> listOf("Cửa ải: $gateName" to true, "Còn ${Realm.van(floor(g) - Store.tuVi())} vạn từ tới đại viên mãn" to false)
        }
    }

    // --- Saving --------------------------------------------------------------------------------------

    fun toJson(): JSONObject = JSONObject().apply {
        put("adj", adj); put("apChe", apChe); put("sub", subCap); put("tamMa", tamMa); put("tick", lastTick)
        put("injury", injuryUntil); put("rest", restUntil); put("tauHoa", tauHoaDay); put("tamMaKiep", tamMaKiep)
        put("fails", fails); put("pill", pill); put("trialDone", trialDone); put("trialBook", trialBook); put("attacks", attacks)
        put("coPham", coPham); put("danPham", danPham); put("breaks", breaks); put("breakFails", breakFails); put("last", lastResult)
        put("capDays", JSONArray().apply { capDays.forEach { put(it) } })
        put("failDays", JSONArray().apply { failDays.forEach { put(it) } })
        put("stageSeen", stageSeen); put("ceilingSeen", ceilingSeen); put("stuckSeen", stuckSeen)
        put("minorSeen", minorSeen); put("minorRest", minorRest); put("minorFails", minorFails); put("gioi", gioi)
        put("days", JSONObject().apply {
            for (d in dayBest.keys + dayLong.keys + dayVeryLong.keys + dayJumps.keys) {
                put(d.toString(), JSONArray().put(dayBest[d] ?: 0).put(dayLong[d] ?: 0).put(dayVeryLong[d] ?: 0).put(dayJumps[d] ?: 0))
            }
        })
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        adj = o.optLong("adj"); apChe = o.optLong("apChe"); subCap = o.optInt("sub"); tamMa = o.optInt("tamMa")
        lastTick = o.optLong("tick", -1); injuryUntil = o.optLong("injury", -1); restUntil = o.optLong("rest", -1)
        tauHoaDay = o.optLong("tauHoa", -1); tamMaKiep = o.optBoolean("tamMaKiep"); fails = o.optInt("fails")
        pill = o.optBoolean("pill"); trialDone = o.optInt("trialDone", -1); trialBook = o.optString("trialBook")
        attacks = o.optInt("attacks"); coPham = o.optInt("coPham", -1); danPham = o.optInt("danPham", -1)
        breaks = o.optInt("breaks"); breakFails = o.optInt("breakFails"); lastResult = o.optString("last")
        o.optJSONArray("capDays")?.let { a -> for (i in 0 until minOf(a.length(), capDays.size)) capDays[i] = a.getLong(i) }
        o.optJSONArray("failDays")?.let { a -> for (i in 0 until a.length()) failDays.add(a.getLong(i)) }
        stageSeen = o.optInt("stageSeen", -1); ceilingSeen = o.optInt("ceilingSeen", -1); stuckSeen = o.optInt("stuckSeen", -1)
        minorSeen = o.optInt("minorSeen", -1); minorRest = o.optLong("minorRest", -1); minorFails = o.optInt("minorFails"); gioi = o.optInt("gioi", -1)
        o.optJSONObject("days")?.let { m ->
            for (k in m.keys()) {
                val v = m.getJSONArray(k)
                val d = k.toLong()
                v.optLong(0).takeIf { it > 0 }?.let { dayBest[d] = it }
                v.optInt(1).takeIf { it > 0 }?.let { dayLong[d] = it }
                v.optInt(2).takeIf { it > 0 }?.let { dayVeryLong[d] = it }
                v.optInt(3).takeIf { it > 0 }?.let { dayJumps[d] = it }
            }
        }
    }
}
