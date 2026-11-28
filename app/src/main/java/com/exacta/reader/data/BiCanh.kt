package com.exacta.reader.data

import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Treasure
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/**
 * 2.3: the weekly bí cảnh as a map, and the ancient ruins of each month.
 *
 *  - Each week a map of nodes, as many as the days the bí cảnh asks (5, or 4 with the set). Each
 *    day of 30 minutes read opens one more. Before each node the reader picks the left or the right
 *    way (each shows what waits), then meets its biến cố: yêu thú, cấm chế, linh thảo, động phủ cổ,
 *    tàn hồn truyền thừa, đồng đạo gặp nạn. The last node is the master of the bí cảnh.
 *  - Each biến cố offers two to four ways out; thuật pháp, pháp bảo, the five measures open the
 *    better ones. A bad end is a wound; two wounds are trọng thương: the bí cảnh is left at once
 *    and the next week cultivation runs at 85%.
 *  - What is opened and left untouched when the week ends is walked the safe way by itself.
 *  - Thượng cổ di tích: twenty days of 30 minutes in a month open the ruins of that month.
 */
object BiCanh {
    const val YEU_THU = 0
    const val CAM_CHE = 1
    const val LINH_THAO = 2
    const val DONG_PHU_CO = 3
    const val TAN_HON = 4
    const val DONG_DAO = 5
    const val CHU = 6
    val TYPES = arrayOf("Yêu thú chặn đường", "Cấm chế cổ", "Dược viên hoang", "Động phủ cổ", "Tàn hồn truyền thừa", "Đồng đạo gặp nạn", "Bí cảnh chi chủ")
    val SCENES = arrayOf(
        "Một con yêu lang mắt đỏ chắn ngang lối mòn, hơi thở nóng như lửa.",
        "Trước cửa đá, những đường cấm chế cổ sáng lên rồi lặng đi như nhịp thở.",
        "Một khoảnh dược viên bỏ hoang, linh thảo mọc lẫn cỏ dại, thoang thoảng mùi yêu thú.",
        "Cửa động phủ của một vị tiền bối không rõ danh tính, khép hờ, bụi phủ dày.",
        "Một luồng tàn hồn lơ lửng trên bệ đá, muốn trao lại điều cả đời học được.",
        "Một đồng đạo bị thương nằm bên vách núi, pháp bảo vỡ nát bên cạnh.",
        "Chủ nhân bí cảnh hiện thân giữa điện, uy áp như núi đè xuống.",
    )
    const val WOUNDS_MAX = 2
    const val DI_TICH_DAYS = 20

    class Choice(val label: String, val need: String, val ok: Boolean)

    var started = false
    var week = -1L
    val nodes = ArrayList<Int>()
    val results = ArrayList<String>()
    var resolved = 0
    var wounds = 0
    var fled = false
    var forkL = -1
    var forkR = -1
    var woundWeek = -1L         // the week of slower cultivation after trọng thương
    var noted = 0
    var diTichMonth = -1
    var maps = 0
    var masters = 0
    var rng: Random = Random.Default

    private fun today() = Store.today()

    fun reset() {
        started = false; week = -1; nodes.clear(); results.clear(); resolved = 0; wounds = 0; fled = false
        forkL = -1; forkR = -1; woundWeek = -1; noted = 0; diTichMonth = -1; maps = 0; masters = 0
    }

    fun length() = Store.biCanhNeed()

    /** Days of 30 minutes (and Bí Cảnh Phù) of a week, as nodes open. */
    fun opened(w: Long = week): Int {
        val days = (w..minOf(w + 6, today())).count { Store.minutes(it) >= Store.BI_CANH_MINUTES } +
            if (Store.biCanhBonusWeek == w) Store.biCanhBonusDays else 0
        return minOf(days, length())
    }

    fun done() = resolved >= length() && !fled
    /** A biến cố waits for the reader. */
    fun pending() = !fled && resolved < nodes.size
    /** A fork waits: the next node is open and not chosen yet. */
    fun atFork() = !fled && resolved == nodes.size && nodes.size < opened() && nodes.size < length()
    fun current(): Int = if (pending()) nodes[resolved] else -1

    /** Slower cultivation the week after trọng thương. */
    fun hurt() = woundWeek == Store.weekStart(today())

    private fun rollFork() {
        if (nodes.size >= length() - 1) {
            forkL = CHU
            forkR = CHU
            return
        }
        val a = rng.nextInt(6)
        var b = rng.nextInt(6)
        if (b == a) b = (a + 1 + rng.nextInt(5)) % 6
        forkL = a
        forkR = b
    }

    fun choose(right: Boolean): Boolean {
        if (!atFork()) return false
        nodes.add(if (right) forkR else forkL)
        rollFork()
        Store.touch()
        return true
    }

    // --- Biến cố ----------------------------------------------------------------------------------------

    /** A thuật of the kind carried (it shields by being there). */
    private fun has(kind: Int) = DaoPhap.thuatOn.any { DaoPhap.on(it) && PhapMon.THUAT_PHAP[it].kind == kind }
    /** 3.3: a thuật of the kind carried and past its hồi chiêu, to be cast. */
    private fun ready(kind: Int) = DaoPhap.hasReady(kind)
    /** From Kim Đan only what is worn serves in a fight. */
    private fun strongest() = TrangBi.bestTier()
    private fun pills() = (0 until Treasure.ITEMS).filter { Treasure.isPill(it) && Store.items[it] > 0 }

    fun chances(): Map<String, Int> = mapOf(
        "fight" to (40 + 10 * (strongest() + 1) + 3 * TuLuyen.theTier() + 4 * TeLuyen.bestGrade() + TranPhap.biCanhPart()).coerceIn(10, 90),
        "force" to (30 + TuLuyen.canCo() / 2 + TranPhap.biCanhPart()).coerceIn(10, 90),
        "master" to (45 + 8 * (strongest() + 1) + (if (has(PhapMon.K_CONG_KICH)) 10 else 0) + 3 * Store.realmCap.coerceAtLeast(0) + 4 * TeLuyen.bestGrade() +
            (if (DaoPhap.hop(PhapMon.LOI)) 15 else 0)).coerceIn(10, 95),
    )

    /** Quyết chiến after a khống chế: the master is held down. */
    private fun heldChance() = (chances().getValue("master") + 15 + 5 * DaoPhap.bestMastery(PhapMon.K_KHONG_CHE)).coerceAtMost(98)

    fun choices(type: Int = current()): List<Choice> {
        val ch = chances()
        val best = TrangBi.best()
        return when (type) {
            YEU_THU -> listOf(
                Choice("Thi triển công kích", "cần thuật công kích hết hồi chiêu", ready(PhapMon.K_CONG_KICH)),
                Choice("Độn đi", "cần độn thuật hết hồi chiêu", ready(PhapMon.K_DON)),
                Choice("Liều đánh · ${ch["fight"]}%", "pháp bảo mạnh và thể phách giúp thắng; thua thì bị thương", true),
                Choice("Thi triển khống chế", "trói yêu thú, thưởng chắc chắn", ready(PhapMon.K_KHONG_CHE)),
            )
            CAM_CHE -> listOf(
                Choice("Phá bằng pháp bảo", "cần pháp bảo từ Địa phẩm" + (best?.let { " (đang có ${Treasure.TIER_NAMES[it.tier]})" } ?: ""), strongest() >= 2),
                Choice("Nhìn thấu trận văn", "cần Thiên Nhãn Thuật hết hồi chiêu hoặc thần thức Tứ trọng", DaoPhap.ready(PhapMon.T_THIEN_NHAN) || TuLuyen.trong() >= 3),
                Choice("Cưỡng phá · ${ch["force"]}%", "căn cơ càng vững càng dễ; hỏng thì bị thương", true),
                Choice("Bỏ qua", "không được gì, không mất gì", true),
            )
            LINH_THAO -> listOf(
                Choice("Hái cẩn thận", "một cây linh thảo 100 – 300 năm", true),
                Choice("Hái hết", "hai cây, một cây già; 35% yêu thú canh vườn, có thuật phòng ngự thì không sao", true),
            )
            DONG_PHU_CO -> listOf(
                Choice("Vào thám hiểm", "bảo vật quý; 30% sập bẫy, thuật phụ trợ hay phòng ngự đỡ được", true),
                Choice("Chỉ lấy đồ ngoài cửa", "vài chục linh thạch", true),
            )
            TAN_HON -> listOf(
                Choice("Tiếp nhận truyền thừa", "một ngọc giản; tâm cảnh dưới Thanh Minh thì tâm ma +10", true),
                Choice("Kính cẩn từ chối", "ngộ tính +1", true),
            )
            DONG_DAO -> listOf(
                Choice("Ra tay cứu", "có thuật phòng ngự hay công kích thì an toàn; không thì 40% bị thương", true),
                Choice("Tặng một viên đan", "cần một viên đan trong túi", pills().isNotEmpty()),
                Choice("Lặng lẽ đi qua", "tâm ma +5", true),
            )
            CHU -> listOf(
                if (DaoPhap.ready(PhapMon.T_LOI_PHAT)) Choice("Quyết chiến · Lôi Phạt Thiên Uy", "thần thông giáng lôi: chắc thắng", true)
                else Choice("Quyết chiến · ${ch["master"]}%", "thắng thì thêm một khoáng thạch cao phẩm; thua thì bị thương", true),
                Choice("Độn thuật lấy bảo rồi rút", "cần độn thuật hết hồi chiêu", ready(PhapMon.K_DON)),
                Choice("Cúi đầu rút lui", "vẫn qua bí cảnh, không thêm gì", true),
                Choice("Khống chế rồi quyết chiến · ${heldChance()}%", "cần thuật khống chế hết hồi chiêu", ready(PhapMon.K_KHONG_CHE)),
            )
            else -> emptyList()
        }
    }

    private fun roll(p: Int) = rng.nextInt(100) < p

    private fun wound(why: String): String {
        wounds++
        if (DaoPhap.castHeal()) {
            wounds--
            return "$why · Hồi Xuân Thuật chữa lành ngay"
        }
        if (wounds >= WOUNDS_MAX) {
            fled = true
            woundWeek = Store.weekStart(today()) + 7
            Store.note("Trọng thương trong bí cảnh: phải rời đi, tuần sau tu luyện chỉ còn 85%")
            return "$why · trọng thương, rời bí cảnh"
        }
        return "$why · bị thương ($wounds/$WOUNDS_MAX)"
    }

    /** Meets the biến cố with choice `i`: the line of what happened, or null. */
    fun resolve(i: Int): String? {
        if (!pending()) return null
        val type = current()
        val c = choices(type).getOrNull(i) ?: return null
        if (!c.ok) return null
        val ch = chances()
        val text = when (type) {
            YEU_THU -> when (i) {
                0 -> {
                    val (id, hit, lt) = DaoPhap.castAttack { roll(it) } ?: return null
                    val name = PhapMon.THUAT_PHAP[id].name
                    if (hit) {
                        Store.gainLinhThach(lt)
                        "$name diệt yêu thú, $lt linh thạch: " + Store.reward(1)
                    } else wound("$name trượt, yêu thú phản công")
                }
                1 -> {
                    DaoPhap.castEscape()
                    Store.gainLinhThach(10); "Độn đi an toàn, nhặt được 10 linh thạch"
                }
                3 -> {
                    val (id, _) = DaoPhap.castControl() ?: return null
                    "${PhapMon.THUAT_PHAP[id].name} trói yêu thú: " + Store.reward(1) +
                        (if (DaoPhap.hop(PhapMon.BANG)) ", Băng Phong Thiên Lý: " + Store.reward(1) else "")
                }
                else -> if (roll(ch.getValue("fight"))) "Thắng yêu thú: " + Store.reward(1) else wound("Yêu thú cắn trúng")
            }
            CAM_CHE -> when (i) {
                0 -> "Pháp bảo phá cấm chế: " + Store.reward(2)
                1 -> {
                    if (TuLuyen.trong() < 3) DaoPhap.castEye()
                    "Nhìn thấu trận văn: " + Store.reward(1)
                }
                2 -> if (roll(ch.getValue("force"))) "Cưỡng phá được: " + Store.reward(1) else wound("Cấm chế phản phệ")
                else -> "Bỏ qua cấm chế"
            }
            LINH_THAO -> when (i) {
                0 -> herb(100, 300)
                else -> {
                    val got = herb(100, 300) + ", " + herb(300, 600)
                    if (roll(35) && !has(PhapMon.K_PHONG_NGU)) wound("$got; yêu thú canh vườn đuổi theo") else got
                }
            }
            DONG_PHU_CO -> when (i) {
                0 -> if (roll(30) && !has(PhapMon.K_PHU_TRO) && !has(PhapMon.K_PHONG_NGU)) wound("Sập bẫy trong động phủ") else "Bảo vật trong động phủ: " + Store.reward(2)
                else -> {
                    val n = 20L + rng.nextInt(21)
                    Store.gainLinhThach(n)
                    "Nhặt được $n linh thạch ngoài cửa"
                }
            }
            TAN_HON -> when (i) {
                0 -> {
                    val scroll = DaoPhap.dropScroll(rng)
                    if (TuLuyen.tamCanhTier() < 2) TuLuyen.stir(10)
                    "Nhận truyền thừa: $scroll" + if (TuLuyen.tamCanhTier() < 2) ", tâm ma xao động" else ""
                }
                else -> {
                    DaoPhap.gainNgo(1, "Từ chối truyền thừa")
                    "Kính cẩn từ chối: ngộ tính +1"
                }
            }
            DONG_DAO -> when (i) {
                0 -> if (!has(PhapMon.K_PHONG_NGU) && !has(PhapMon.K_CONG_KICH) && roll(40)) wound("Cứu người bị vạ lây") else rescued()
                1 -> {
                    val p = pills().first()
                    Store.items[p]--
                    "Tặng ${Treasure.ITEM_NAMES[p]}: " + rescued()
                }
                else -> {
                    TuLuyen.stir(5)
                    "Lặng lẽ đi qua, lòng không yên: tâm ma +5"
                }
            }
            else -> {
                fun win(how: String): String {
                    masters++
                    Store.fortune(com.exacta.reader.game.KyNgo.T_MASTER)
                    val t = (Treasure.rollTier() + 1).coerceIn(2, Treasure.TIERS - 1)
                    Store.bag[t]++
                    Store.fillForge()
                    return "$how: khoáng thạch ${Treasure.TIER_NAMES[t]}"
                }
                when (i) {
                    0 -> if (DaoPhap.castThunder()) win("Lôi Phạt Thiên Uy giáng xuống, đánh bại chủ nhân bí cảnh")
                        else if (roll(ch.getValue("master"))) win("Đánh bại chủ nhân bí cảnh") else wound("Chủ nhân bí cảnh quá mạnh")
                    1 -> {
                        DaoPhap.castEscape()
                        "Độn thuật lấy bảo rồi rút: " + Store.reward(1)
                    }
                    3 -> {
                        val p = heldChance()
                        val (id, _) = DaoPhap.castControl() ?: return null
                        val name = PhapMon.THUAT_PHAP[id].name
                        if (roll(p)) win("$name trấn áp, rồi đánh bại chủ nhân bí cảnh") else wound("$name không giữ nổi chủ nhân bí cảnh")
                    }
                    else -> "Cúi đầu rút lui"
                }
            }
        }
        results.add("${TYPES[type]}: $text")
        resolved++
        if (resolved >= length() && !fled) finish()
        Store.note("Bí cảnh · $text")
        Store.touch()
        return text
    }

    private fun herb(min: Int, max: Int): String {
        val kind = rng.nextInt(com.exacta.reader.game.DanDao.HERBS.size)
        val years = min + rng.nextInt(max - min + 1)
        DongPhu.herbs.add(kind to years)
        return "${com.exacta.reader.game.DanDao.HERBS[kind]} $years năm"
    }

    private fun rescued(): String {
        TuLuyen.calm(5)
        return if (TongMon.joined()) {
            TongMon.gain(30, "Cứu đồng đạo")
            "đồng đạo cảm tạ, cống hiến +30, tâm ma −5"
        } else {
            Store.gainLinhThach(40)
            "đồng đạo tạ 40 linh thạch, tâm ma −5"
        }
    }

    private fun finish() {
        maps++
        Store.biCanhComplete(week)
    }

    // --- Each day, and the end of a week ---------------------------------------------------------------

    fun check() {
        if (!started) return
        val w = Store.weekStart(today())
        if (w != week) {
            if (week >= 0) settle()
            week = w
            nodes.clear(); results.clear(); resolved = 0; wounds = 0; fled = false; noted = 0
            rollFork()
            Store.touch()
        }
        val o = opened()
        if (o > noted && !fled && !done()) {
            noted = o
            Store.note("Bí cảnh mở nút thứ $o: vào Bí cảnh chọn đường")
            Store.touch()
        }
    }

    /** The week ends: what was opened and not walked is walked the safe way (no wounds, little gain). */
    private fun settle() {
        if (fled || done()) return
        val open = opened(week)
        while (resolved < open && resolved < length()) {
            if (nodes.size <= resolved) nodes.add(if (nodes.size >= length() - 1) CHU else forkL)
            val type = nodes[resolved]
            if (type == CHU) {
                resolve(2)
            } else {
                results.add("${TYPES[type]}: đi vòng tránh")
                resolved++
            }
        }
        Store.touch()
    }

    // --- Thượng cổ di tích ---------------------------------------------------------------------------

    fun monthKey(d: Long = today()) = LocalDate.ofEpochDay(d).let { it.year * 12 + it.monthValue - 1 }
    fun monthDays(): Int {
        val first = LocalDate.ofEpochDay(today()).withDayOfMonth(1).toEpochDay()
        return (first..today()).count { Store.minutes(it) >= 30 }
    }

    fun canOpenDiTich() = monthDays() >= DI_TICH_DAYS && diTichMonth != monthKey()

    fun openDiTich(): String? {
        if (!canOpenDiTich()) return null
        diTichMonth = monthKey()
        val parts = listOf(DaoPhap.dropScroll(rng), DongPhu.dropRecipe(rng), Store.reward(3))
        Store.gainLinhThach(100)
        val text = parts.joinToString(", ") + ", 100 linh thạch"
        Store.note("Thượng cổ di tích: $text")
        Store.fortune(com.exacta.reader.game.KyNgo.T_DITICH)
        Store.touch()
        return text
    }

    // --- Saving ------------------------------------------------------------------------------------------

    fun migrate() {
        reset()
        started = true
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("week", week); put("nodes", JSONArray().apply { nodes.forEach { put(it) } })
        put("results", JSONArray().apply { results.forEach { put(it) } }); put("resolved", resolved); put("wounds", wounds)
        put("fled", fled); put("forkL", forkL); put("forkR", forkR); put("woundWeek", woundWeek); put("noted", noted)
        put("diTich", diTichMonth); put("maps", maps); put("masters", masters)
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        week = o.optLong("week", -1)
        o.optJSONArray("nodes")?.let { a -> for (i in 0 until a.length()) nodes.add(a.getInt(i)) }
        o.optJSONArray("results")?.let { a -> for (i in 0 until a.length()) results.add(a.getString(i)) }
        resolved = o.optInt("resolved"); wounds = o.optInt("wounds"); fled = o.optBoolean("fled")
        forkL = o.optInt("forkL", -1); forkR = o.optInt("forkR", -1); woundWeek = o.optLong("woundWeek", -1); noted = o.optInt("noted")
        diTichMonth = o.optInt("diTich", -1); maps = o.optInt("maps"); masters = o.optInt("masters")
    }
}
