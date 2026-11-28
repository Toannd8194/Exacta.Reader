package com.exacta.reader.data

import com.exacta.reader.game.DanDao
import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Treasure
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/**
 * 2.3 Bái Nhập Sơn Môn: the sect.
 *
 *  - Five sects, each leaning to a way of cultivation, open once the linh căn has awakened.
 *  - Cống hiến from the weekly tasks (three a week, things to read), the books read through, the
 *    rival overtaken and the quarterly contest; it buys scrolls, recipes, ores and a giai of the
 *    linh mạch in the sect's Bảo khố. The total earned and the realm raise the rank.
 *  - A rival in the sect, a sư huynh or sư tỷ, whose tu vi grows each day by the reader's own
 *    pace of the last thirty days, a tenth faster: to stay ahead is to read more evenly than oneself.
 *  - Tông môn đại bỉ each quarter: the words of the quarter against seven fellows.
 */
object TongMon {
    // --- The sects ------------------------------------------------------------------------------------

    const val VAN_QUYEN = 0
    const val THIET_COT = 1
    const val MINH_TAM = 2
    const val KIEM_LU = 3
    const val BACH_THAO = 4

    /**
     * 2.5: thirty-six sects in five lines (the five founding sects lead them: Pháp, Thể, Thần, Kiếm,
     * Bách nghệ). A sect gives the perks of its line, leans to some hành, and takes a disciple only
     * when what it asks is met; at a time 6 to 8 sects will take one in, by the aptitude.
     */
    class Sect(val name: String, val family: Int, val attrs: IntArray, val reqs: List<Req>, val text: String) {
        val way get() = WAYS[family]
        val perk get() = PERKS[family]
    }

    /** What a sect asks: a kind and a number. */
    class Req(val kind: Int, val n: Int = 0)

    const val R_ATTR = 0      // linh căn or công pháp of the sect's hành
    const val R_ROOT = 1      // linh căn of the sect's hành
    const val R_THIEN = 2     // Thiên or song linh căn
    const val R_DI = 3        // dị linh căn of the sect's hành
    const val R_TAP = 4       // ngũ hành tạp linh căn
    const val R_WAY = 5       // the đạo lộ of the sect's line
    const val R_CANCO = 6
    const val R_THE = 7       // thể phách tier
    const val R_THAN = 8      // thần thức trọng
    const val R_STREAK = 9    // longest streak
    const val R_BOOKS = 10    // books read through
    const val R_REALM = 11
    const val R_TAM = 12      // tâm cảnh tier
    const val R_PILLS = 13    // lò đan made
    const val R_BEASTS = 14   // pháp bảo owned

    val WAYS = arrayOf("Pháp tu", "Thể tu", "Thần tu", "Kiếm tu", "Bách nghệ")
    val PERKS = arrayOf(
        "Tàng Kinh Các rẻ hơn 15%; nhiệm vụ đọc nhiều chữ, đọc trọn sách",
        "Thể phách +15%; đan luyện thể ra thêm một viên; nhiệm vụ buổi đọc dài",
        "Tâm pháp vận chuyển gấp đôi; tâm ma tăng chậm 20%; nhiệm vụ đọc đều ngày",
        "Công pháp Kiếm luôn hợp (+20%); đột phá +3%; nhiệm vụ đọc trọn nhanh",
        "Dược linh +5 năm mỗi ngày; luyện đan +10%; nhiệm vụ luyện đan, bế quan",
    )

    private fun r(kind: Int, n: Int = 0) = Req(kind, n)
    private fun a(vararg x: Int) = x

    val SECTS = listOf(
        // The five founding sects, the heads of the five lines.
        Sect("Vạn Quyển Tông", 0, a(PhapMon.HOA), listOf(r(R_BOOKS, 3)), "Tông môn của người đọc rộng: kinh các chất cao như núi, đệ tử lấy chữ làm linh khí."),
        Sect("Thiết Cốt Môn", 1, a(PhapMon.THO), listOf(r(R_THE, 1)), "Luyện thân như luyện sắt: buổi đọc càng dài, xương cốt càng cứng."),
        Sect("Minh Tâm Tự", 2, a(PhapMon.THUY), listOf(r(R_STREAK, 7)), "Chùa trên núi tuyết, tu tâm trước tu thân: ngày nào cũng đọc, không vội, không bỏ."),
        Sect("Kiếm Lư", 3, a(PhapMon.KIM, PhapMon.KIEM), listOf(r(R_ATTR)), "Một túp lều, một thanh kiếm, một cuốn sách đọc tới cuối: kiếm ý ở sự dứt khoát."),
        Sect("Bách Thảo Cốc", 4, a(PhapMon.MOC), listOf(r(R_ATTR)), "Thung lũng trăm loài thảo dược: đệ tử giỏi trồng, giỏi luyện, giỏi chờ."),
        // Pháp tu.
        Sect("Thái Hư Đạo Tông", 0, a(PhapMon.KIM, PhapMon.LOI), listOf(r(R_THIEN), r(R_REALM, 1)), "Đạo tông đứng đầu chính đạo, chỉ nhận kẻ tư chất hơn người."),
        Sect("Lôi Âm Tông", 0, a(PhapMon.LOI), listOf(r(R_ATTR)), "Sấm rền trên đỉnh, đệ tử luyện thuật pháp trong mưa giông."),
        Sect("Xích Viêm Cốc", 0, a(PhapMon.HOA), listOf(r(R_ROOT)), "Hỏa mạch dưới đáy cốc chưa từng tắt, chỉ nhận người mang hỏa căn."),
        Sect("Huyền Thủy Các", 0, a(PhapMon.THUY), listOf(r(R_ROOT)), "Lầu các dựng giữa hồ, thuật pháp mềm như nước mà bền."),
        Sect("Thanh Vân Môn", 0, a(PhapMon.KIM), emptyList(), "Tông nhỏ dưới chân núi, cửa mở cho mọi tán tu biết chữ."),
        Sect("Thiên Diễn Tông", 0, a(PhapMon.THO), listOf(r(R_REALM, 2), r(R_CANCO, 60)), "Suy diễn thiên cơ bằng muôn quyển sách; chỉ nhận người đã kết đan, nền vững."),
        // Thể tu.
        Sect("Bá Thể Tông", 1, a(PhapMon.THO), listOf(r(R_THE, 3)), "Đệ tử gánh núi mà đọc, nhục thân Ngọc Tủy mới qua được sơn môn."),
        Sect("Man Hoang Bộ", 1, a(PhapMon.MOC, PhapMon.THO), listOf(r(R_ROOT)), "Bộ lạc giữa rừng già, sống bằng sức vóc và cây cỏ."),
        Sect("Kim Cương Tự", 1, a(PhapMon.KIM), listOf(r(R_ROOT), r(R_THE, 2)), "Võ tăng thân như kim cương, tụng kinh trong tư thế trung bình tấn."),
        Sect("Huyết Sát Đường", 1, a(PhapMon.HOA), listOf(r(R_WAY)), "Luyện thể bằng máu và lửa; chỉ người đã bước vào Thể tu mới được dạy."),
        Sect("Hắc Thạch Bảo", 1, a(PhapMon.THO), emptyList(), "Pháo đài đá đen nơi biên ải, nhận bất cứ ai chịu được khổ."),
        Sect("Long Tượng Môn", 1, a(PhapMon.THO, PhapMon.KIM), listOf(r(R_REALM, 2), r(R_THE, 4)), "Sức của rồng và voi: chỉ nhận Kim Đan có thân Kim Thân."),
        // Thần tu.
        Sect("Tinh Thần Các", 2, a(PhapMon.THUY), listOf(r(R_THAN, 3)), "Đài ngắm sao, đệ tử đọc thiên văn mà mở thức hải."),
        Sect("Huyễn Mộng Tông", 2, a(PhapMon.THUY, PhapMon.BANG), listOf(r(R_ATTR)), "Tu trong mộng, mỗi cuốn sách là một giấc mơ dài."),
        Sect("Thái Thượng Thư Viện", 2, a(10), listOf(r(R_BOOKS, 10)), "Thư viện cổ nhất thiên hạ: đọc trọn mười cuốn mới được ghi danh."),
        Sect("Băng Tâm Am", 2, a(PhapMon.BANG), listOf(r(R_DI)), "Am tu giữa băng nguyên, chỉ nhận người mang băng linh căn."),
        Sect("Vô Niệm Thiền Viện", 2, a(PhapMon.THO), listOf(r(R_TAM, 2)), "Ngồi tĩnh mà đọc, lòng như mặt nước; tâm cảnh Thanh Minh mới vào được."),
        Sect("Bách Gia Học Cung", 2, IntArray(0), emptyList(), "Học cung trăm nhà, ai ham đọc đều được ngồi nghe giảng."),
        // Kiếm tu.
        Sect("Thục Sơn Kiếm Phái", 3, a(PhapMon.KIM, PhapMon.KIEM), listOf(r(R_THIEN), r(R_ATTR)), "Kiếm phái trên mây, chỉ chọn kiếm tài có linh căn thuần."),
        Sect("Phong Lôi Kiếm Các", 3, a(PhapMon.PHONG, PhapMon.LOI), listOf(r(R_DI)), "Kiếm nhanh như gió, nặng như sấm; nhận người mang dị linh căn Phong, Lôi."),
        Sect("Thiên Kiếm Tông", 3, a(PhapMon.KIEM), listOf(r(R_REALM, 2), r(R_WAY)), "Tông của kiếm tu đã kết đan, kiếm ý thông thiên."),
        Sect("Tuyệt Tình Cốc", 3, a(PhapMon.BANG), listOf(r(R_STREAK, 14)), "Đoạn tình mà luyện kiếm: chuỗi ngày đọc không đứt mới thấy lối vào cốc."),
        Sect("Lạc Hà Kiếm Trang", 3, a(PhapMon.KIM), emptyList(), "Kiếm trang bên bờ sông, dạy kiếm cho kẻ áo vải."),
        Sect("Liệt Hỏa Kiếm Tông", 3, a(PhapMon.HOA), listOf(r(R_ROOT)), "Kiếm nung trong hỏa mạch, chỉ hỏa căn cầm nổi."),
        // Bách nghệ.
        Sect("Đan Đỉnh Tông", 4, a(PhapMon.HOA, PhapMon.MOC), listOf(r(R_PILLS, 5)), "Ba nghìn lò đan cháy ngày đêm; đã thành năm lò mới được nhận."),
        Sect("Thiên Công Các", 4, a(PhapMon.KIM), listOf(r(R_BEASTS, 5)), "Các của thợ luyện khí; có năm pháp bảo mới được bước vào."),
        Sect("Phù Lục Sơn", 4, a(PhapMon.PHONG), emptyList(), "Núi của thợ vẽ bùa, giấy vàng bay như lá."),
        Sect("Dược Vương Cốc", 4, a(PhapMon.MOC, PhapMon.THUY), listOf(r(R_ROOT)), "Cốc của dược vương, cỏ cây nghe tiếng người đọc mà lớn."),
        Sect("Trận Đạo Môn", 4, a(PhapMon.THO), listOf(r(R_THAN, 2)), "Bày trận bằng thần thức; thức hải Tam trọng mới hiểu nổi trận đồ."),
        Sect("Linh Thú Sơn", 4, a(PhapMon.MOC), listOf(r(R_BEASTS, 3)), "Núi nuôi linh thú, pháp bảo cũng thành bạn đồng hành."),
        Sect("Tạp Học Đường", 4, IntArray(0), listOf(r(R_TAP)), "Nơi ngũ hành tạp linh căn được trọng: biết ít mỗi thứ, thành nhiều mỗi đường."),
    )

    // --- Admission -----------------------------------------------------------------------------------

    /**
     * 3.0.1: the sects open their gates by realm. Ten take a Luyện Khí disciple, nine look for Trúc Cơ,
     * nine for Kim Đan, four for Nguyên Anh, four (the greatest) for Hóa Thần.
     */
    val OPENS_AT = intArrayOf(
        0, 0, 0, 0, 0,          // the five founding sects
        2, 1, 1, 1, 0, 2,       // Pháp tu: Thái Hư, Lôi Âm, Xích Viêm, Huyền Thủy, Thanh Vân, Thiên Diễn
        3, 1, 2, 2, 0, 3,       // Thể tu: Bá Thể, Man Hoang, Kim Cương, Huyết Sát, Hắc Thạch, Long Tượng
        2, 1, 3, 2, 4, 0,       // Thần tu: Tinh Thần, Huyễn Mộng, Thái Thượng, Băng Tâm, Vô Niệm, Bách Gia
        4, 2, 3, 1, 0, 1,       // Kiếm tu: Thục Sơn, Phong Lôi, Thiên Kiếm, Tuyệt Tình, Lạc Hà, Liệt Hỏa
        4, 4, 0, 1, 2, 2, 1,    // Bách nghệ: Đan Đỉnh, Thiên Công, Phù Lục, Dược Vương, Trận Đạo, Linh Thú, Tạp Học
    )
    fun opened(i: Int) = Store.realmCap >= OPENS_AT[i]
    fun openCount() = SECTS.indices.count { opened(it) }
    /** The next realm at which more sects open, and how many: null when all are open. */
    fun nextOpening(): Pair<Int, Int>? = SECTS.indices.filter { !opened(it) }.groupBy { OPENS_AT[it] }.minByOrNull { it.key }?.let { it.key to it.value.size }

    private fun attrOk(s: Sect) = s.attrs.any { it in DaoPhap.roots || it == DaoPhap.di || it == DaoPhap.activeDef()?.attr }

    /** Whether one thing a sect asks is met, and how it reads. */
    fun reqText(s: Sect, q: Req): Pair<String, Boolean> {
        val names = s.attrs.joinToString(", ") { PhapMon.attrName(it) }
        return when (q.kind) {
            R_ATTR -> "Linh căn hoặc công pháp $names" to attrOk(s)
            R_ROOT -> "Linh căn $names" to s.attrs.any { it in DaoPhap.roots }
            R_THIEN -> "Thiên linh căn hoặc song linh căn" to (DaoPhap.roots.size in 1..2)
            R_DI -> "Dị linh căn $names" to (DaoPhap.di in s.attrs)
            R_TAP -> "Ngũ hành tạp linh căn" to (DaoPhap.roots.size >= 5)
            R_WAY -> "Đạo lộ ${WAYS[s.family]}" to (DaoLo.way == s.family)
            R_CANCO -> "Căn cơ ${q.n} (đang ${TuLuyen.canCo()})" to (TuLuyen.canCo() >= q.n)
            R_THE -> "Thể phách ${TuLuyen.THE_TIERS[q.n]}" to (TuLuyen.theTier() >= q.n)
            R_THAN -> "Thần thức ${TuLuyen.TRONG[q.n]}" to (TuLuyen.trong() >= q.n)
            R_STREAK -> "Chuỗi ngày đọc ${q.n} (dài nhất ${Store.bestStreak()})" to (Store.bestStreak() >= q.n)
            R_BOOKS -> "Đọc trọn ${q.n} cuốn" to (Store.books.values.count { it.finishDay >= 0 } >= q.n)
            R_REALM -> "Cảnh giới ${Realm.baseName(q.n)}" to (Store.realmCap >= q.n)
            R_TAM -> "Tâm cảnh ${TuLuyen.TAM_CANH_TIERS[q.n]}" to (TuLuyen.tamCanhTier() >= q.n)
            R_PILLS -> "Luyện thành ${q.n} lò đan" to (DongPhu.made >= q.n)
            else -> "Có ${q.n} pháp bảo" to (Store.beasts.size >= q.n)
        }
    }

    fun admits(i: Int) = SECTS[i].reqs.all { reqText(SECTS[i], it).second }

    /** How well a sect fits: its hành in the linh căn and the công pháp, the way, how much it asks. */
    fun fit(i: Int): Int {
        val s = SECTS[i]
        var v = 0
        DaoPhap.roots.forEachIndexed { n, root -> if (root in s.attrs) v += 30 - 6 * n }
        if (DaoPhap.di in s.attrs) v += 30
        if (DaoPhap.activeDef()?.attr?.let { it in s.attrs } == true) v += 15
        if (DaoLo.way == s.family) v += 25
        v += 6 * s.reqs.size
        if (s.reqs.isEmpty()) v -= 15
        return v
    }

    /** How many sects will look at this cultivator: 6, 7 with song linh căn or a dị linh căn, 8 with Thiên linh căn. */
    fun choiceCount() = (6 + (if (DaoPhap.roots.size in 1..2) 1 else 0) + (if (DaoPhap.roots.size == 1) 1 else 0) + (if (DaoPhap.di >= 0) 1 else 0)).coerceAtMost(8)

    /** The sects that would take this cultivator now, the best fitting first. */
    fun choices(): List<Int> = SECTS.indices.filter { it != sect && opened(it) && it !in betrayed && admits(it) }.sortedByDescending { fit(it) * 100 - it }.take(choiceCount())

    /** The line of the sect joined, or -1. */
    fun family() = if (joined()) SECTS[sect].family else -1

    val RANKS = arrayOf("Ngoại môn", "Nội môn", "Chân truyền", "Trưởng lão", "Thái thượng trưởng lão")
    val RANK_AT = intArrayOf(0, 300, 1_000, 2_500, 5_000)
    /** The realm each rank asks besides the cống hiến (Trưởng lão: Nguyên Anh; Thái thượng: Hóa Thần). */
    val RANK_REALM = intArrayOf(0, 0, 2, 3, 4)
    val SALARY = longArrayOf(20, 40, 80, 150, 250)
    /** 3.0.1: a rank also asks days in the sect: one stays long to rise. */
    val RANK_DAYS = intArrayOf(0, 14, 60, 150, 300)
    /** Bổng ngày: linh thạch each day of 10 minutes read, by rank; cống hiến adds 1 a 500 (10 at most). */
    val DAILY = longArrayOf(1, 2, 4, 7, 12)
    const val DAILY_MINUTES = 10
    const val BAN_DAYS = 30
    const val FINE_PERCENT = 30
    const val BETRAY_TAM_MA = 15

    // --- The weekly tasks -----------------------------------------------------------------------------

    class Task(val text: String, val goal: Int, val reward: Int)

    val TASKS = listOf(
        Task("Đọc 4 ngày, mỗi ngày từ 30 phút", 4, 40),
        Task("Đọc tổng 5 giờ trong tuần", 300, 50),
        Task("Một buổi đọc liền 60 phút", 1, 40),
        Task("Đọc trọn một cuốn", 1, 60),
        Task("Đọc 3 vạn chữ", 30_000, 40),
        Task("Mở một cuốn của tác giả chưa đọc", 1, 30),
        Task("Có đọc 6 ngày trong tuần", 6, 50),
        Task("Hai buổi đọc liền từ 45 phút", 2, 40),
        Task("Luyện thành một lò đan", 1, 40),
        Task("Gặp hai lần kỳ ngộ", 2, 40),
        Task("Bế quan viên mãn một lần", 1, 40),
        Task("Đọc 10 vạn chữ", 100_000, 90),
    )
    private val FAVOURED = listOf(listOf(3, 4, 11), listOf(2, 7, 1), listOf(0, 6, 9), listOf(3, 2, 5), listOf(8, 10, 0))

    // --- State ----------------------------------------------------------------------------------------

    var started = false
    var sect = -1
    var joinDay = -1L
    var changed = false
    var gongs = 0              // cống hiến to spend
    var gongsTotal = 0         // cống hiến ever earned
    var week = -1L
    val tasks = ArrayList<Int>()
    val claimed = ArrayList<Boolean>()
    var bonusClaimed = false
    // What the counters were on Monday, for the tasks that count events.
    var baseMade = 0
    var baseKyNgo = 0
    var baseBeQuan = 0
    var rivalName = ""
    var rivalWords = 0.0
    var rivalDay = -1L
    var ahead = false
    var quarter = -1           // the quarter of the last đại bỉ held
    var lastRank = 0
    // 3.0.1: the daily stipend and the betrayals.
    var paidDay = -1L
    var paidTotal = 0L
    var banUntil = -1L
    var betrayals = 0
    val betrayed = ArrayList<Int>()
    var lastLeave = ""
    val lastTable = ArrayList<Pair<String, Long>>()
    var rng: Random = Random.Default

    private fun today() = Store.today()

    fun reset() {
        started = false; sect = -1; joinDay = -1; changed = false; gongs = 0; gongsTotal = 0; week = -1
        tasks.clear(); claimed.clear(); bonusClaimed = false; baseMade = 0; baseKyNgo = 0; baseBeQuan = 0
        rivalName = ""; rivalWords = 0.0; rivalDay = -1; ahead = false; quarter = -1; lastRank = 0; lastTable.clear()
        paidDay = -1; paidTotal = 0; banUntil = -1; betrayals = 0; betrayed.clear(); lastLeave = ""
    }

    fun joined() = sect >= 0
    fun canJoin() = !joined() && DaoPhap.awakened() && !banned()
    fun banned() = today() <= banUntil
    fun daysIn() = if (joined()) (today() - joinDay).toInt().coerceAtLeast(0) else 0
    /** The perks of a line: the founding sect's index is the line's. */
    fun perk(line: Int) = joined() && SECTS[sect].family == line

    fun rank(): Int {
        var r = 0
        for (i in RANK_AT.indices) if (gongsTotal >= RANK_AT[i] && Store.realmCap >= RANK_REALM[i] && daysIn() >= RANK_DAYS[i]) r = i
        return r
    }

    fun rankName() = if (joined()) RANKS[rank()] else "Tán tu"

    fun gain(n: Int, why: String) {
        if (!joined() || n <= 0) return
        val before = rank()
        gongs += n
        gongsTotal += n
        Store.note("$why: cống hiến +$n")
        if (rank() > before) Store.note("Thăng ${RANKS[rank()]} của ${SECTS[sect].name}")
        Store.touch()
    }

    /** The sect that fits best of those that would take the cultivator. */
    fun suggested(): Int = choices().firstOrNull() ?: VAN_QUYEN

    /** Joins sect s if it would take the cultivator (`check` off: for the tests and old saves). */
    fun join(s: Int, check: Boolean = true): Boolean {
        if (s !in SECTS.indices || joined() || (check && s !in choices())) return false
        if (check && !canJoin()) return false
        if (!check && !DaoPhap.awakened()) return false
        sect = s
        joinDay = today()
        week = -1
        rollWeek()
        val r = Random(joinDay * 31 + s)
        val sur = listOf("Lăng", "Tô", "Mạc", "Cố", "Bạch", "Liễu", "Thẩm", "Hạ", "Vân", "Tần")
        val given = listOf("Vân", "Phong", "Thanh", "Nguyệt", "Hàn", "Dao", "Khê", "Tuyết", "Trần", "Ly")
        rivalName = (if (r.nextBoolean()) "Sư huynh " else "Sư tỷ ") + sur.random(r) + " " + given.random(r)
        rivalWords = Store.tuVi() * 1.05
        rivalDay = today()
        ahead = false
        quarter = quarterOf(today())
        Store.note("Bái nhập ${SECTS[s].name}, làm đệ tử ${RANKS[0].lowercase()}")
        Store.touch()
        return true
    }

    // --- Weekly tasks ---------------------------------------------------------------------------------

    private fun rollWeek() {
        val w = Store.weekStart(today())
        if (w == week) return
        // A week ends: what was done and not claimed is lost, as in the sect's register.
        week = w
        val r = Random(w * 131 + sect * 7 + 3)
        val fav = FAVOURED[SECTS[sect].family]
        val first = fav.random(r)
        val rest = TASKS.indices.filter { it != first }.shuffled(r).take(2)
        tasks.clear()
        tasks.add(first)
        tasks.addAll(rest)
        claimed.clear()
        repeat(tasks.size) { claimed.add(false) }
        bonusClaimed = false
        baseMade = DongPhu.made
        baseKyNgo = Store.kyNgoCount
        baseBeQuan = Store.beQuanDone
        Store.touch()
    }

    /** Progress of task `t` this week, against its goal. */
    fun progress(t: Int): Int {
        val w = week
        val days = w..today()
        return when (t) {
            0 -> days.count { Store.minutes(it) >= 30 }
            1 -> (Store.secondsBetween(w, today()) / 60).toInt()
            2 -> if (days.any { (TuLuyen.dayBest[it] ?: 0) >= 3600 }) 1 else 0
            3 -> Store.books.values.count { it.finishDay in days }
            4, 11 -> Store.wordsBetween(w, today()).toInt()
            5 -> {
                val known = Store.books.values.filter { it.finishDay in 0 until w }.map { it.author.trim().lowercase() }.toSet()
                Store.books.values.count { it.firstDay in days && it.author.isNotBlank() && it.author.trim().lowercase() !in known }.coerceAtMost(1)
            }
            6 -> days.count { (Store.daySeconds[it] ?: 0) >= 600 }
            7 -> days.sumOf { TuLuyen.dayLong[it] ?: 0 }
            8 -> DongPhu.made - baseMade
            9 -> Store.kyNgoCount - baseKyNgo
            10 -> Store.beQuanDone - baseBeQuan
            else -> 0
        }.coerceAtLeast(0)
    }

    fun done(i: Int) = i in tasks.indices && progress(tasks[i]) >= TASKS[tasks[i]].goal

    fun claim(i: Int): Boolean {
        if (!done(i) || claimed[i]) return false
        claimed[i] = true
        gain(TASKS[tasks[i]].reward, "Xong nhiệm vụ tông môn")
        Store.fortune(com.exacta.reader.game.KyNgo.T_TASK)
        return true
    }

    fun canClaimBonus() = joined() && !bonusClaimed && claimed.isNotEmpty() && claimed.all { it }

    /** All three done: the week's bổng lộc of the rank, and 50 cống hiến. */
    fun claimBonus(): Boolean {
        if (!canClaimBonus()) return false
        bonusClaimed = true
        Store.gainLinhThach(SALARY[rank()])
        gain(50, "Bổng lộc tuần, ${SALARY[rank()]} linh thạch")
        return true
    }

    // --- The rival ------------------------------------------------------------------------------------

    /** The reader's words a day over the thirty days to `d` (2 000 at least). */
    private fun pace(d: Long) = maxOf(2_000.0, Store.wordsBetween(d - 29, d) / 30.0)

    private fun tickRival() {
        val t = today()
        if (rivalDay < 0) { rivalDay = t; return }
        if (rivalDay >= t) return
        for (d in rivalDay until t) rivalWords += pace(d) * 1.1
        rivalDay = t
        val now = Store.tuVi() > rivalWords
        if (now && !ahead) {
            ahead = true
            gain(40, "Vượt $rivalName")
            Store.fortune(com.exacta.reader.game.KyNgo.T_RIVAL)
        } else if (!now && ahead) {
            ahead = false
            Store.note("$rivalName đã vượt lên trước")
        }
        Store.touch()
    }

    fun rivalRealm() = Realm.name(Realm.level(rivalWords.toLong()))

    // --- Đại bỉ ---------------------------------------------------------------------------------------

    fun quarterOf(day: Long): Int = LocalDate.ofEpochDay(day).let { it.year * 4 + (it.monthValue - 1) / 3 }
    fun quarterStart(q: Int): Long = LocalDate.of(q / 4, (q % 4) * 3 + 1, 1).toEpochDay()

    private val FELLOWS = listOf("Lý Thanh Sơn", "Mộ Dung Tuyết", "Tiết Vô Ưu", "Hoa Ly", "Đoạn Hồng", "Kỷ Minh", "Âu Dương Lam")

    /** Words a fellow reads in a quarter: the 7 000 words a day of a steady reader, times their mettle. */
    private fun fellows(q: Int): List<Pair<String, Long>> {
        val r = Random(q * 977L + sect)
        val days = quarterStart(q + 1) - quarterStart(q)
        return FELLOWS.map { it to (7_000.0 * days * (0.5 + r.nextDouble() * 1.1)).toLong() }
    }

    private fun holdContest() {
        val q = quarterOf(today())
        if (quarter < 0) { quarter = q; return }
        if (q <= quarter) return
        val held = q - 1
        quarter = q
        val from = maxOf(quarterStart(held), joinDay)
        val mine = Store.wordsBetween(from, quarterStart(q) - 1)
        val table = (fellows(held) + ("Bạn" to mine)).sortedByDescending { it.second }
        lastTable.clear()
        lastTable.addAll(table)
        lastRank = table.indexOfFirst { it.first == "Bạn" } + 1
        val reward = when (lastRank) {
            1 -> 200
            2, 3 -> 120
            4, 5 -> 60
            else -> 20
        }
        gain(reward, "Tông môn đại bỉ: hạng $lastRank/8")
        if (lastRank <= 3) Store.fortune(com.exacta.reader.game.KyNgo.T_DAIBI)
        if (lastRank == 1) Store.note("Đại bỉ khôi thủ: " + DaoPhap.dropScroll())
        else if (lastRank <= 3) Store.note("Đại bỉ: " + DongPhu.dropRecipe())
        Store.touch()
    }

    // --- Bảo khố --------------------------------------------------------------------------------------

    class Ware(val name: String, val text: String, val price: Int, val minRank: Int)

    fun wares(): List<Ware> {
        val p = DaoPhap.targetPham()
        return listOf(
            Ware("Ngọc giản công pháp ${PhapMon.PHAM[p]} phẩm", "Một bộ công pháp chưa có, đúng phẩm cửa kế tiếp cần", 120 * (p + 1), 0),
            Ware("Ngọc giản thuật pháp", "Một thuật pháp chưa có", 80, 0),
            Ware("Ngọc giản tâm pháp", "Một tâm pháp chưa có", 120, 1),
            Ware("Đan phương", "Một đan phương chưa biết", 100, 0),
            Ware("Túi hạt linh thảo", "Ba hạt linh thảo", 30, 0),
            Ware("Khoáng thạch Địa phẩm", "Vào túi, rồi tự vào lò", 150, 1),
            Ware("Khoáng thạch Thiên phẩm", "Vào túi, rồi tự vào lò", 400, 2),
            Ware("Nâng linh mạch một giai", "Linh mạch động phủ lên một giai", ((DongPhu.upgradePrice() / 5).toInt()).coerceAtLeast(40), 1),
        )
    }

    fun canBuy(i: Int): Boolean {
        val w = wares().getOrNull(i) ?: return false
        if (!joined() || rank() < w.minRank || gongs < w.price) return false
        return i != 7 || DongPhu.giai < 9
    }

    fun buy(i: Int): String? {
        if (!canBuy(i)) return null
        val w = wares()[i]
        val text = when (i) {
            0 -> {
                val p = DaoPhap.targetPham()
                val free = PhapMon.CONG_PHAP.indices.filter { PhapMon.CONG_PHAP[it].pham == p && it !in DaoPhap.cong }
                val id = free.firstOrNull { DaoPhap.affinity(PhapMon.CONG_PHAP[it].attr) > 0 } ?: free.randomOrNull(rng) ?: return null
                DaoPhap.gainCong(id)
                "ngọc giản ${PhapMon.CONG_PHAP[id].name}"
            }
            1 -> PhapMon.THUAT_PHAP.indices.filter { it !in DaoPhap.thuat && DaoPhap.canLearnRealm(it) }.randomOrNull(rng)?.let {
                DaoPhap.gainThuat(it); "ngọc giản ${PhapMon.THUAT_PHAP[it].name}"
            } ?: return null
            2 -> PhapMon.TAM_PHAP.indices.filter { it !in DaoPhap.tam }.randomOrNull(rng)?.let {
                DaoPhap.gainTam(it); "ngọc giản ${PhapMon.TAM_PHAP[it].name}"
            } ?: return null
            3 -> DanDao.RECIPES.indices.filter { it !in DongPhu.known }.randomOrNull(rng)?.let {
                DongPhu.learn(it); "đan phương ${DanDao.RECIPES[it].name}"
            } ?: return null
            4 -> (1..3).joinToString(", ") { DongPhu.dropSeed(rng) }
            5 -> { Store.bag[2]++; Store.fillForge(); "khoáng thạch ${Treasure.TIER_NAMES[2]}" }
            6 -> { Store.bag[3]++; Store.fillForge(); "khoáng thạch ${Treasure.TIER_NAMES[3]}" }
            else -> {
                DongPhu.giai++
                "linh mạch ${DongPhu.giaiName()}"
            }
        }
        gongs -= w.price
        Store.note("Bảo khố tông môn: $text")
        Store.touch()
        return text
    }

    // --- Bổng ngày and phản tông ---------------------------------------------------------------------

    fun stipend(): Long = if (!joined()) 0 else DAILY[rank()] + minOf(10, gongsTotal / 500).toLong()

    /** Paid once a day, on the day the cultivator reads 10 minutes. */
    private fun pay() {
        val t = today()
        if (paidDay >= t || Store.minutes(t) < DAILY_MINUTES) return
        paidDay = t
        val n = stipend()
        paidTotal += n
        Store.gainLinhThach(n)
    }

    /** The fine of leaving: 30% of the linh thạch held. */
    fun fine(): Long = Store.linhThach * FINE_PERCENT / 100

    /**
     * Phản tông: the cultivator leaves the sect. The cống hiến is struck off, the sect takes 30% of the
     * linh thạch, tâm ma rises 15, no sect will take a phản đồ for 30 days, the old sect never again;
     * the deed weighs on nghiệp lực.
     */
    fun leave(): Boolean {
        if (!joined()) return false
        val name = SECTS[sect].name
        val f = fine()
        Store.linhThach -= f
        TuLuyen.stir(BETRAY_TAM_MA)
        betrayed.add(sect)
        betrayals++
        banUntil = today() + BAN_DAYS
        sect = -1; joinDay = -1; gongs = 0; gongsTotal = 0; week = -1; tasks.clear(); claimed.clear(); bonusClaimed = false
        rivalName = ""; rivalWords = 0.0
        lastLeave = "Phản xuất $name: mất cống hiến, nộp $f linh thạch, tâm ma +$BETRAY_TAM_MA, $BAN_DAYS ngày không tông nào nhận"
        Store.note(lastLeave)
        Store.touch()
        return true
    }

    // --- Each day, and saving ---------------------------------------------------------------------

    fun check() {
        if (!started || !joined()) return
        pay()
        rollWeek()
        tickRival()
        holdContest()
    }

    fun onFinish() = gain(20, "Đọc trọn một cuốn")

    fun migrate() {
        reset()
        started = true
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("sect", sect); put("join", joinDay); put("changed", changed); put("gongs", gongs); put("total", gongsTotal)
        put("week", week); put("tasks", JSONArray().apply { tasks.forEach { put(it) } })
        put("claimed", JSONArray().apply { claimed.forEach { put(it) } }); put("bonus", bonusClaimed)
        put("baseMade", baseMade); put("baseKyNgo", baseKyNgo); put("baseBeQuan", baseBeQuan)
        put("rival", rivalName); put("rivalWords", rivalWords); put("rivalDay", rivalDay); put("ahead", ahead)
        put("quarter", quarter); put("lastRank", lastRank)
        put("paidDay", paidDay); put("paid", paidTotal); put("ban", banUntil); put("betrayals", betrayals)
        put("betrayed", JSONArray().apply { betrayed.forEach { put(it) } }); put("leave", lastLeave)
        put("table", JSONArray().apply { lastTable.forEach { put(JSONArray().put(it.first).put(it.second)) } })
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        sect = o.optInt("sect", -1).takeIf { it in SECTS.indices } ?: -1
        joinDay = o.optLong("join", -1); changed = o.optBoolean("changed"); gongs = o.optInt("gongs"); gongsTotal = o.optInt("total")
        week = o.optLong("week", -1)
        o.optJSONArray("tasks")?.let { a -> for (i in 0 until a.length()) a.getInt(i).takeIf { it in TASKS.indices }?.let { tasks.add(it) } }
        o.optJSONArray("claimed")?.let { a -> for (i in 0 until a.length()) claimed.add(a.getBoolean(i)) }
        while (claimed.size < tasks.size) claimed.add(false)
        bonusClaimed = o.optBoolean("bonus")
        baseMade = o.optInt("baseMade"); baseKyNgo = o.optInt("baseKyNgo"); baseBeQuan = o.optInt("baseBeQuan")
        rivalName = o.optString("rival"); rivalWords = o.optDouble("rivalWords", 0.0); rivalDay = o.optLong("rivalDay", -1)
        ahead = o.optBoolean("ahead"); quarter = o.optInt("quarter", -1); lastRank = o.optInt("lastRank")
        paidDay = o.optLong("paidDay", -1); paidTotal = o.optLong("paid"); banUntil = o.optLong("ban", -1); betrayals = o.optInt("betrayals")
        o.optJSONArray("betrayed")?.let { a -> for (i in 0 until a.length()) betrayed.add(a.getInt(i)) }
        lastLeave = o.optString("leave")
        o.optJSONArray("table")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { lastTable.add(it.getString(0) to it.getLong(1)) } }
    }
}
