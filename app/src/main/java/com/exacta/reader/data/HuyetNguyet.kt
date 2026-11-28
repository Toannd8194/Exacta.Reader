package com.exacta.reader.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * What each giới writes for itself on the common tiên lộ ([TienLo]): its titles, halls, factions,
 * monthly tasks, kiếp, and the part of the resource that is its own.
 */
interface GioiRules {
    /** What a bậc's title is called (tiên chức, ma ấn…), the seat's name, a line to open and to close. */
    val CHUC_LABEL: String
    val SEAT_NAME: String
    val GREET: String
    val END_LINE: String
    val CHUC: Array<String>
    val CHUC_PERK: Array<String>
    val HALLS: Array<String>
    val HALL_TEXT: Array<String>
    val FACTIONS: Array<String>
    val FACTION_TEXT: Array<String>
    val TASKS: Array<String>
    val TASK_GOAL: LongArray
    /** Which tasks count words (shown in vạn). */
    val WORD_TASK: Int
    val KIEP_NAME: Array<String>
    val KIEP_RULE: Array<String>

    fun hasChuc(i: Int) = TienLo.bac >= i + 1
    fun reset()
    fun parts(): List<Pair<String, Int>>
    fun dayExtra(d: Long): Long
    fun kiepParts(): List<Pair<String, Int>>
    fun taskBonus(): Int
    fun tienKhiBonus(): Int
    fun onOpen()
    fun check(): Boolean
    fun taskProgress(task: Int, from: Long, to: Long): Long
    fun kiepDayKept(i: Int, d: Long): Boolean
    fun kiepEnd(i: Int): Boolean
    fun kiepWindow(i: Int): Int
    fun kiepNeed(i: Int): Int
    /** 3.7: a day is about to be counted (Bát Nhã's xá lợi may keep its chain). */
    fun beforeDay(d: Long) {}
    /** 3.7: a kiếp failed; a ward may take the wound. Returns what took it, or null. */
    fun ward(i: Int): String? = null
    /** 4.0: a kiếp counted by what was done (books, authors) rather than by days kept; null counts days. */
    fun kiepCount(i: Int): Int? = null
    /** What the kiếp counts. */
    fun kiepUnit(i: Int) = "ngày"
    fun toJson(): JSONObject
    fun read(o: JSONObject)
}

/**
 * 3.5 Huyết Nguyệt Trầm Uyên, the giới of the night: tâm ma is no enemy here but the furnace.
 *
 *  - Ma khí ([NhatKy.estimate]): a point each 10 minutes read from 22 to 4 o'clock, twice on the night
 *    of the full moon; a point a day for each 10 tâm ma from 40; 20 for each failure (a breakthrough,
 *    a kiếp): to fall is to gain.
 *  - Ma công: a share of the ma khí staked on an oath of reading. Kept, the stake comes back twice;
 *    broken, it is lost. One oath a week; the stake is at most a quarter of the week's ma khí, so the
 *    road is a quarter faster at most, for one who wins every time.
 *  - Phản phệ: tâm ma past 90 eats the ma khí of the last three days and falls back to 60.
 *  - Ma cung (Huyết trì, Ma binh doanh, Ám điện), the factions Huyết Hải Ma Cung, U Ảnh Điện, Thiên Ma
 *    Lĩnh, the Tâm ma đại kiếp (tâm ma held 40 – 80) and the Đại kiếp Ma Đế (nights).
 */
object HuyetNguyet : GioiRules {
    override val CHUC_LABEL = "ma ấn"
    override val SEAT_NAME = "Ma cung"
    override val GREET = "Trăng máu treo trên vực, cửa Ma Cung mở."
    override val END_LINE = "Vực sâu dậy sóng máu, vạn ma cúi đầu."
    override val CHUC = arrayOf("Huyết Nguyệt Ấn", "Ma Khế Ấn", "Huyết Sát Ấn", "Phản Phệ Ấn", "Ma Kiếp Ấn", "Thôn Thiên Ấn", "Ma Lệnh Ấn", "Đổ Mệnh Ấn", "Ma Đế Ấn")
    override val CHUC_PERK = arrayOf(
        "Tâm ma không trừ gì ở giới này; ngã là có lời: đột phá hỏng, kiếp hỏng +20 ma khí",
        "Ma công: cược thêm 10% ma khí bảy ngày qua",
        "Ma khí +3%",
        "Phản phệ chỉ ăn ma khí 2 ngày",
        "Tiên kiếp +5%",
        "Ma khí +3% nữa",
        "Việc thế lực +5 cống hiến",
        "Ma công thắng thêm 10%",
        "Đạo hiệu tối thượng «Nghịch Thiên Ma Đế»",
    )
    override val HALLS = arrayOf("Huyết trì", "Ma binh doanh", "Ám điện")
    override val HALL_TEXT = arrayOf(
        "Ngâm ma binh trong huyết trì: mỗi cấp +1 cấp ma binh (+1% ma khí).",
        "Ma binh hộ thân khi độ kiếp: mỗi cấp tiên kiếp +2%.",
        "Kho cược: mỗi cấp cược thêm 5% ma khí bảy ngày qua; thua thì hoàn 10% phần cược.",
    )
    override val FACTIONS = arrayOf("Huyết Hải Ma Cung", "U Ảnh Điện", "Thiên Ma Lĩnh")
    override val FACTION_TEXT = arrayOf(
        "Cung điện trên biển máu, trọng sức mạnh. Ma khí +5%.",
        "Điện chìm trong bóng, trọng mưu kế. Ma công thắng thêm 10%.",
        "Núi của thiên ma, trọng chiến trận. Tiên kiếp +5%.",
    )
    override val TASKS = arrayOf(
        "10 đêm đọc từ 20 phút sau 22 giờ",
        "Thắng một ma công",
        "Đọc 30 phút một đêm rằm",
        "15 ngày tâm ma từ 40",
        "Đọc 15 vạn chữ",
        "8 đêm đọc từ 40 phút sau 22 giờ",
    )
    override val TASK_GOAL = longArrayOf(10, 1, 1, 15, 150_000, 8)
    override val WORD_TASK = 4
    override val KIEP_NAME = arrayOf("Tâm ma đại kiếp thứ nhất", "Tâm ma đại kiếp thứ hai", "Đại kiếp Ma Đế")
    override val KIEP_RULE = arrayOf(
        "14 ngày trong 30 ngày đọc từ 10 phút, tâm ma giữ trong 40 – 80",
        "14 ngày trong 30 ngày đọc từ 10 phút, tâm ma giữ trong 40 – 80",
        "21 đêm trong 30 ngày đọc từ 10 phút sau 22 giờ, tâm ma không vượt 90",
    )

    // --- Ma công -------------------------------------------------------------------------------------

    /** The oaths: what is sworn, the days it runs, and what must be reached. */
    val OATHS = arrayOf(
        "3 đêm liền, mỗi đêm 40 phút sau 22 giờ",
        "7 ngày liền, mỗi ngày 30 phút",
        "5 đêm trong 7 ngày, mỗi đêm 30 phút sau 22 giờ",
        "Đọc trọn một cuốn trong 10 ngày",
        "10 vạn chữ trong 7 ngày",
        "14 ngày liền, mỗi ngày 20 phút",
    )
    val OATH_DAYS = intArrayOf(3, 7, 7, 10, 7, 14)
    val OATH_NEED = longArrayOf(3, 7, 5, 1, 100_000, 14)
    const val OATH_BOOK = 3
    const val OATH_WORDS = 4
    const val MIN_STAKE = 10
    const val PHAN_PHE_AT = 90
    const val PHAN_PHE_TO = 60

    var oath = -1                // the oath sworn, or -1
    var stake = 0
    var oathStart = -1L
    var lastOathDay = -1L        // the day the last oath was sworn: one a week
    var wins = 0
    var losses = 0
    val winDays = ArrayList<Long>()
    var phanPhe = 0
    var lastOath = ""

    override fun reset() {
        oath = -1; stake = 0; oathStart = -1; lastOathDay = -1; wins = 0; losses = 0; winDays.clear(); phanPhe = 0; lastOath = ""
    }

    private fun today() = Store.today()
    private fun night(d: Long) = NhatKy.days[d]?.let { NhatKy.nightSeconds(it) } ?: 0L

    /** The ma khí of the last seven days counted. */
    fun weekEarned(): Int = ((TienLo.countedTo - 6)..TienLo.countedTo).filter { it >= TienDinh.crossDay }.sumOf { TienLo.dayTenths(it) }.let { (it / 10).toInt() }

    /** The most that may be staked: a quarter of the week's ma khí (more with the Ám điện and the second ấn). */
    fun stakeCap(): Int {
        val pct = 25 + 5 * TienLo.halls[2] + (if (hasChuc(1)) 10 else 0)
        return maxOf(MIN_STAKE, weekEarned() * pct / 100).coerceAtMost(TienLo.res())
    }

    fun nextOathDay() = if (lastOathDay < 0) today() else lastOathDay + 7
    fun canSwear() = TienLo.opened && TienLo.gioi == 1 && oath < 0 && today() >= nextOathDay() && TienLo.res() >= MIN_STAKE

    fun swear(o: Int, s: Int): Boolean {
        if (!canSwear() || o !in OATHS.indices || s < MIN_STAKE || s > stakeCap()) return false
        oath = o; stake = s; oathStart = today(); lastOathDay = today()
        TienLo.resTenths -= 10L * s
        Store.note("Ma công: cược $s ma khí, thề «${OATHS[o]}»")
        Store.touch()
        return true
    }

    private fun dayMet(o: Int, d: Long) = when (o) {
        0 -> night(d) >= 40 * 60
        1 -> Store.minutes(d) >= 30
        2 -> night(d) >= 30 * 60
        else -> Store.minutes(d) >= 20
    }

    /** How far the oath is (days kept, books, words), from its start to `to`. */
    fun oathProgress(to: Long = today()): Long {
        if (oath < 0) return 0
        val end = minOf(to, oathStart + OATH_DAYS[oath] - 1)
        return when (oath) {
            OATH_BOOK -> Store.books.values.count { it.finishDay in oathStart..end }.toLong()
            OATH_WORDS -> Store.wordsBetween(oathStart, end)
            else -> (oathStart..end).count { dayMet(oath, it) }.toLong()
        }
    }

    fun oathLeft() = if (oath < 0) 0 else (oathStart + OATH_DAYS[oath] - today()).toInt().coerceAtLeast(0)

    /** Kept, broken, or still running (null). */
    private fun judge(): Boolean? {
        val t = today()
        val end = oathStart + OATH_DAYS[oath] - 1
        val need = OATH_NEED[oath]
        if (oathProgress(t) >= need) return true
        if (t > end) return false
        if (oath == OATH_BOOK || oath == OATH_WORDS) return null
        // Days already over that were not kept: can the rest still reach it?
        val keptPast = (oathStart until t).count { dayMet(oath, it) }
        val left = (end - t + 1).toInt()
        return if (keptPast + left < need) false else null
    }

    fun payoutFor(s: Int) = s * 2 * (100 + (if (TienLo.faction == 1) 10 else 0) + (if (hasChuc(7)) 10 else 0)) / 100
    fun payout() = payoutFor(stake)

    private fun settle(): Boolean {
        if (oath < 0) return false
        val r = judge() ?: return false
        if (r) {
            val p = payout()
            TienLo.resTenths += 10L * p
            wins++
            winDays.add(today())
            lastOath = "Giữ lời thề «${OATHS[oath]}»: nhận $p ma khí"
        } else {
            val back = stake * 10 * TienLo.halls[2] / 100
            TienLo.resTenths += 10L * back
            losses++
            lastOath = "Phá lời thề «${OATHS[oath]}»: mất ${stake - back} ma khí" + if (back > 0) " (Ám điện hoàn $back)" else ""
        }
        Store.note("Ma công · $lastOath")
        oath = -1; stake = 0; oathStart = -1
        return true
    }

    /** Phản phệ: tâm ma past 90 eats the ma khí of the last days, and falls back to 60. */
    private fun backlash(): Boolean {
        if (TuLuyen.tamMa <= PHAN_PHE_AT) return false
        val n = if (hasChuc(3)) 2 else 3
        val lost = ((TienLo.countedTo - n + 1)..TienLo.countedTo).filter { it >= TienDinh.crossDay }.sumOf { TienLo.dayTenths(it) }
        TienLo.resTenths = (TienLo.resTenths - lost).coerceAtLeast(0)
        TuLuyen.tamMa = PHAN_PHE_TO
        phanPhe++
        Store.note("Phản phệ: tâm ma vượt $PHAN_PHE_AT, mất ${lost / 10} ma khí của $n ngày, tâm ma về $PHAN_PHE_TO")
        return true
    }

    // --- The common hooks ----------------------------------------------------------------------------

    override fun parts(): List<Pair<String, Int>> = buildList {
        if (hasChuc(2)) add("Ma ấn ${CHUC[2]}" to 3)
        if (hasChuc(5)) add("Ma ấn ${CHUC[5]}" to 3)
        if (TienLo.faction == 0) add("Thế lực ${FACTIONS[0]}" to 5)
        if (TienLo.faction >= 0 && TienLo.rank() > 0) add("${FACTIONS[TienLo.faction]} · ${TienLo.RANKS[TienLo.rank()]}" to TienLo.rank())
    }

    override fun dayExtra(d: Long) = 0L

    override fun kiepParts(): List<Pair<String, Int>> = buildList {
        if (TienLo.halls[1] > 0) add("Ma binh doanh cấp ${TienLo.halls[1]}" to 2 * TienLo.halls[1])
        if (TienLo.faction == 2) add("Thế lực ${FACTIONS[2]}" to 5)
        if (hasChuc(4)) add("Ma ấn ${CHUC[4]}" to 5)
    }

    override fun taskBonus() = if (hasChuc(6)) 5 else 0
    override fun tienKhiBonus() = TienLo.halls[0]
    override fun onOpen() {}

    override fun check(): Boolean {
        var changed = false
        if (backlash()) changed = true
        if (settle()) changed = true
        return changed
    }

    override fun taskProgress(task: Int, from: Long, to: Long): Long {
        val days = from..to
        return when (task) {
            0 -> days.count { night(it) >= 20 * 60 }.toLong()
            1 -> winDays.count { it in days }.toLong()
            2 -> days.count { NhatKy.fullMoon(it) && night(it) >= 30 * 60 }.toLong()
            3 -> days.count { (NhatKy.days[it]?.tamMa ?: 0) >= 40 }.toLong()
            4 -> Store.wordsBetween(from, to)
            else -> days.count { night(it) >= 40 * 60 }.toLong()
        }
    }

    /** A day counts in the kiếp: read 10 minutes, tâm ma held in 40 – 80; the đại kiếp asks a night under 90. */
    override fun kiepDayKept(i: Int, d: Long): Boolean {
        if (Store.minutes(d) < 10) return false
        return if (i < 2) TuLuyen.tamMa in 40..80 else night(d) >= 10 * 60 && TuLuyen.tamMa <= PHAN_PHE_AT
    }

    override fun kiepEnd(i: Int) = true
    override fun kiepWindow(i: Int) = 30
    override fun kiepNeed(i: Int) = if (i == 2) 21 else 14

    override fun toJson(): JSONObject = JSONObject().put("oath", oath).put("stake", stake).put("start", oathStart).put("lastDay", lastOathDay)
        .put("wins", wins).put("losses", losses).put("winDays", JSONArray().apply { winDays.forEach { put(it) } })
        .put("phanPhe", phanPhe).put("last", lastOath)

    override fun read(o: JSONObject) {
        reset()
        oath = o.optInt("oath", -1); stake = o.optInt("stake"); oathStart = o.optLong("start", -1); lastOathDay = o.optLong("lastDay", -1)
        wins = o.optInt("wins"); losses = o.optInt("losses")
        o.optJSONArray("winDays")?.let { a -> for (i in 0 until a.length()) winDays.add(a.getLong(i)) }
        phanPhe = o.optInt("phanPhe"); lastOath = o.optString("last")
    }
}
