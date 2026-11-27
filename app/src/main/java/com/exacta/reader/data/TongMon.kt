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

    class Sect(val name: String, val way: String, val perk: String, val text: String)

    val SECTS = listOf(
        Sect("Vạn Quyển Tông", "Pháp tu", "Tàng Kinh Các rẻ hơn 15%; nhiệm vụ đọc nhiều chữ, đọc trọn sách",
            "Tông môn của người đọc rộng: kinh các chất cao như núi, đệ tử lấy chữ làm linh khí."),
        Sect("Thiết Cốt Môn", "Thể tu", "Thể phách +15%; đan luyện thể ra thêm một viên; nhiệm vụ buổi đọc dài",
            "Luyện thân như luyện sắt: buổi đọc càng dài, xương cốt càng cứng."),
        Sect("Minh Tâm Tự", "Thần tu", "Tâm pháp vận chuyển gấp đôi; tâm ma tăng chậm 20%; nhiệm vụ đọc đều ngày",
            "Chùa trên núi tuyết, tu tâm trước tu thân: ngày nào cũng đọc, không vội, không bỏ."),
        Sect("Kiếm Lư", "Kiếm tu", "Công pháp Kiếm luôn hợp (+20%); đột phá +3%; nhiệm vụ đọc trọn nhanh",
            "Một túp lều, một thanh kiếm, một cuốn sách đọc tới cuối: kiếm ý ở sự dứt khoát."),
        Sect("Bách Thảo Cốc", "Bách nghệ", "Dược linh +5 năm mỗi ngày; luyện đan +10%; nhiệm vụ luyện đan, bế quan",
            "Thung lũng trăm loài thảo dược: đệ tử giỏi trồng, giỏi luyện, giỏi chờ."),
    )

    val RANKS = arrayOf("Ngoại môn", "Nội môn", "Chân truyền", "Trưởng lão", "Thái thượng trưởng lão")
    val RANK_AT = intArrayOf(0, 300, 1_000, 2_500, 5_000)
    /** The realm each rank asks besides the cống hiến (Trưởng lão: Nguyên Anh; Thái thượng: Hóa Thần). */
    val RANK_REALM = intArrayOf(0, 0, 2, 3, 4)
    val SALARY = longArrayOf(20, 40, 80, 150, 250)

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
    val lastTable = ArrayList<Pair<String, Long>>()
    var rng: Random = Random.Default

    private fun today() = Store.today()

    fun reset() {
        started = false; sect = -1; joinDay = -1; changed = false; gongs = 0; gongsTotal = 0; week = -1
        tasks.clear(); claimed.clear(); bonusClaimed = false; baseMade = 0; baseKyNgo = 0; baseBeQuan = 0
        rivalName = ""; rivalWords = 0.0; rivalDay = -1; ahead = false; quarter = -1; lastRank = 0; lastTable.clear()
    }

    fun joined() = sect >= 0
    fun canJoin() = !joined() && DaoPhap.awakened()
    fun perk(s: Int) = sect == s

    fun rank(): Int {
        var r = 0
        for (i in RANK_AT.indices) if (gongsTotal >= RANK_AT[i] && Store.realmCap >= RANK_REALM[i]) r = i
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

    /** The sect the linh căn leans to: Kim to Kiếm Lư, Mộc to Bách Thảo Cốc, Thủy to Minh Tâm Tự, Hỏa to Vạn Quyển Tông, Thổ to Thiết Cốt Môn. */
    fun suggested(): Int = when (DaoPhap.roots.firstOrNull()) {
        PhapMon.KIM -> KIEM_LU
        PhapMon.MOC -> BACH_THAO
        PhapMon.THUY -> MINH_TAM
        PhapMon.HOA -> VAN_QUYEN
        PhapMon.THO -> THIET_COT
        else -> VAN_QUYEN
    }

    fun join(s: Int): Boolean {
        if (s !in SECTS.indices || s == sect) return false
        if (joined()) {
            // Leaving one sect for another, once: the cống hiến stays behind.
            if (changed) return false
            changed = true
            gongs = 0
            gongsTotal = 0
        } else if (!DaoPhap.awakened()) return false
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
        val fav = FAVOURED.getOrElse(sect) { FAVOURED[0] }
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

    // --- Each day, and saving ---------------------------------------------------------------------

    fun check() {
        if (!started || !joined()) return
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
        o.optJSONArray("table")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { lastTable.add(it.getString(0) to it.getLong(1)) } }
    }
}
