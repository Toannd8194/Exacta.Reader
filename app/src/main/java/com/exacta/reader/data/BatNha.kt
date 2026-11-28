package com.exacta.reader.data

import org.json.JSONObject

/**
 * 3.7 Bát Nhã Liên Đài, the giới of the even, unbroken way: words read are only the occasion; the bậc
 * rise by công đức.
 *
 *  - Công đức ([NhatKy.estimate]): 2 a day of 30 minutes with no chapter skipped, 1 each 7 days of the
 *    chain, 20 a book read through (35 when it was a book left and taken up again), −2 a jump; a day of
 *    tâm ma from 50 makes none.
 *  - Xá lợi: one forms each 300 công đức. Held, it keeps the chain over a day missed (the day is sealed,
 *    as the Phong Ấn Phù does) or takes the blow of a tiên kiếp failed (no wound).
 *  - Độ hóa đồng đạo: each month a fellow is lost in a book of the library left a month or more; read on
 *    in it three days, ten minutes each, and he is freed: +40 công đức.
 *  - Thiền viện (Kinh lâu, Xá lợi tháp, Liên trì), the factions Tây Lĩnh Thiền Viện, Liên Trì Tịnh Xá,
 *    Bồ Đề Học Đường, the Vô niệm kiếp (a sitting of 45 minutes, no jump) and the Đại kiếp Liên Hoa
 *    (49 days unbroken).
 */
object BatNha : GioiRules {
    override val CHUC_LABEL = "liên tòa"
    override val SEAT_NAME = "Thiền viện"
    override val GREET = "Đài sen nở giữa biển mây, chuông Thiền Viện ngân xa."
    override val END_LINE = "Ngàn cánh sen khép lại, trí tuệ tròn đầy như trăng rằm."
    override val CHUC = arrayOf("Bồ Đoàn Tòa", "Thanh Liên Tòa", "Bạch Liên Tòa", "Hồng Liên Tòa", "Kim Liên Tòa", "Thất Bảo Liên Tòa", "Cửu Phẩm Liên Tòa", "Bát Nhã Liên Tòa", "Liên Hoa Thánh Tòa")
    override val CHUC_PERK = arrayOf(
        "Công đức thay tu vi; xá lợi giữ chuỗi qua ngày lỡ, đỡ một lần kiếp hỏng",
        "Độ hóa đồng đạo thêm 10 công đức",
        "Công đức +3%",
        "Xá lợi kết sớm hơn 30 công đức",
        "Tiên kiếp +5%",
        "Công đức +3% nữa",
        "Việc thế lực +5 cống hiến",
        "Liên trì: mỗi ngày đọc tâm ma giảm thêm 1",
        "Đạo hiệu tối thượng «Liên Hoa Thánh Tôn»",
    )
    override val HALLS = arrayOf("Kinh lâu", "Xá lợi tháp", "Liên trì")
    override val HALL_TEXT = arrayOf(
        "Mỗi cuốn đọc trọn thành một bộ kinh trên giá: mỗi cấp, mỗi cuốn đọc trọn thêm 4 công đức.",
        "Tháp thờ xá lợi: mỗi cấp, xá lợi kết sớm hơn 20 công đức (300 xuống 200).",
        "Ao sen tịnh tâm: mỗi cấp, mỗi ngày đọc từ 10 phút tâm ma lắng xuống 1.",
    )
    override val FACTIONS = arrayOf("Tây Lĩnh Thiền Viện", "Liên Trì Tịnh Xá", "Bồ Đề Học Đường")
    override val FACTION_TEXT = arrayOf(
        "Thiền viện trên núi tây, trọng thiền định. Công đức +5%.",
        "Tịnh xá bên ao sen, trọng tịnh tâm. Mỗi ngày đọc tâm ma lắng 1.",
        "Học đường dưới cây bồ đề, trọng kinh điển. Tiên kiếp +5%.",
    )
    override val TASKS = arrayOf(
        "Đọc 20 ngày trong tháng",
        "Giữ chuỗi 14 ngày liền",
        "Đọc trọn một cuốn",
        "12 ngày đủ 30 phút, không nhảy chương",
        "Đọc 15 vạn chữ",
        "Độ hóa một đồng đạo",
    )
    override val TASK_GOAL = longArrayOf(20, 14, 1, 12, 150_000, 1)
    override val WORD_TASK = 4
    override val KIEP_NAME = arrayOf("Vô niệm kiếp thứ nhất", "Vô niệm kiếp thứ hai", "Đại kiếp Liên Hoa")
    override val KIEP_RULE = arrayOf(
        "21 ngày trong 30 ngày, mỗi ngày một buổi liền từ 45 phút, không nhảy chương",
        "21 ngày trong 30 ngày, mỗi ngày một buổi liền từ 45 phút, không nhảy chương",
        "49 ngày liền đọc từ 10 phút trong 70 ngày; ngày lỡ có xá lợi giữ thì chuỗi không đứt",
    )

    // --- Xá lợi -------------------------------------------------------------------------------------

    const val XA_LOI = 300
    var peak = 0                 // the most công đức ever held: xá lợi form from it
    var used = 0
    var guarded = 0              // days kept by a xá lợi
    var warded = 0               // kiếp failures taken by a xá lợi
    var formedNoted = 0
    var autoGuard = true
    var lastXa = ""

    fun xaAt() = XA_LOI - 20 * TienLo.halls[1] - (if (hasChuc(3)) 30 else 0)
    fun formed() = peak / xaAt()
    fun held() = (formed() - used).coerceAtLeast(0)
    fun nextXa() = (formed() + 1) * xaAt() - peak

    // --- Độ hóa đồng đạo ----------------------------------------------------------------------------

    val FELLOWS = arrayOf("Tuệ Minh", "Tịnh Không", "Viên Giác", "Minh Đăng", "Thanh Tâm", "Diệu Âm", "Pháp Vân", "Trí Hải", "Liễu Ngộ", "Không Tịch", "Chân Như", "Bảo Tạng")
    const val DO_DAYS = 3
    const val DO_SECONDS = 600L
    const val DO_REWARD = 40
    const val LEFT_DAYS = 30

    var doMonth = -1L
    var doBook = -1              // the id of the book the fellow is lost in, or -1
    var doFellow = 0
    var doDone = false
    var freed = 0
    val freedDays = ArrayList<Long>()
    var lienTo = -1L             // the last day the Liên trì stilled

    override fun reset() {
        peak = 0; used = 0; guarded = 0; warded = 0; formedNoted = 0; autoGuard = true; lastXa = ""
        doMonth = -1; doBook = -1; doFellow = 0; doDone = false; freed = 0; freedDays.clear(); lienTo = -1
    }

    private fun today() = Store.today()
    private fun jumps(d: Long) = TuLuyen.dayJumps[d] ?: 0
    private fun read(d: Long) = Store.minutes(d) >= 1 || d in Store.sealedDays

    /** The chain now: through today if read, else through yesterday. */
    fun chain() = chainAt(if (read(today())) today() else today() - 1)

    /** Days read in a row up to `d`, a sealed day counting. */
    private fun chainAt(d: Long): Int {
        var n = 0
        var x = d
        while (n < 400 && read(x)) { n++; x-- }
        return n
    }

    fun book(): Book? = if (doBook < 0) null else Store.books.values.firstOrNull { it.id == doBook }

    /** The books left a month or more before `from`: begun, not read through. */
    fun leftBooks(from: Long) = Store.books.values.filter { it.finishDay < 0 && it.wordsRead > 0 && it.lastDay in 0..(from - LEFT_DAYS) }.sortedBy { it.id }

    /** The fellow of the month is chosen once, among the books left. */
    private fun pickFellow(): Boolean {
        val m = TienLo.monthOf(today())
        if (m == doMonth) return false
        doMonth = m
        doDone = false
        val left = leftBooks(TienLo.monthStart(today()))
        doBook = if (left.isEmpty()) -1 else left[(m % left.size).toInt()].id
        doFellow = (m % FELLOWS.size).toInt()
        return true
    }

    /** Days this month the book of the fellow was read ten minutes. */
    fun doProgress(to: Long = today()): Int {
        if (doBook < 0) return 0
        return (maxOf(TienLo.monthStart(today()), TienLo.openDay)..to).count { (NhatKy.days[it]?.books?.get(doBook) ?: 0) >= DO_SECONDS }
    }

    fun doReward() = DO_REWARD + (if (hasChuc(1)) 10 else 0)

    private fun checkFellow(): Boolean {
        var changed = pickFellow()
        if (!doDone && doBook >= 0 && doProgress() >= DO_DAYS) {
            doDone = true
            freed++
            freedDays.add(today())
            TienLo.resTenths += 10L * doReward()
            Store.note("Độ hóa đồng đạo ${FELLOWS[doFellow]} ra khỏi «${book()?.title ?: "cuốn sách"}»: +${doReward()} công đức")
            changed = true
        }
        return changed
    }

    // --- Liên trì -----------------------------------------------------------------------------------

    fun stillPerDay() = TienLo.halls[2] + (if (TienLo.faction == 1) 1 else 0) + (if (hasChuc(7)) 1 else 0)

    private fun lienTri(): Boolean {
        if (lienTo < 0) lienTo = TienLo.openDay - 1
        var changed = false
        while (lienTo < TienLo.countedTo) {
            lienTo++
            val s = stillPerDay()
            if (s > 0 && Store.minutes(lienTo) >= 10 && TuLuyen.tamMa > 0) {
                TuLuyen.tamMa = (TuLuyen.tamMa - s).coerceAtLeast(0)
                changed = true
            }
        }
        return changed
    }

    // --- The common hooks ----------------------------------------------------------------------------

    override fun parts(): List<Pair<String, Int>> = buildList {
        if (hasChuc(2)) add("Liên tòa ${CHUC[2]}" to 3)
        if (hasChuc(5)) add("Liên tòa ${CHUC[5]}" to 3)
        if (TienLo.faction == 0) add("Thế lực ${FACTIONS[0]}" to 5)
        if (TienLo.faction >= 0 && TienLo.rank() > 0) add("${FACTIONS[TienLo.faction]} · ${TienLo.RANKS[TienLo.rank()]}" to TienLo.rank())
    }

    /** Kinh lâu: 4 a level for each book read through; a pháp khí of cấp 9 forgives the jumps. */
    override fun dayExtra(d: Long): Long {
        var t = 10L * 4 * TienLo.halls[0] * Store.books.values.count { it.finishDay == d }
        val j = jumps(d)
        val day = NhatKy.days[d]
        if (j > 0 && day != null && day.tamMa < 50 && TienLo.tienKhi() >= 9) {
            val chain = chainAt(d)
            t += 10L * ((if (Store.minutes(d) >= 30) 2 else 0) + (if (chain > 0 && chain % 7 == 0) 1 else 0))
        }
        return t
    }

    /** A day ended unread, the one before it read: a xá lợi keeps the chain. */
    override fun beforeDay(d: Long) {
        if (!autoGuard || read(d) || Store.minutes(d - 1) < 1) return
        peak = maxOf(peak, TienLo.res())
        if (held() <= 0) return
        used++
        guarded++
        Store.sealedDays.add(d)
        lastXa = "Xá lợi giữ chuỗi qua ngày lỡ"
        Store.note(lastXa)
    }

    override fun ward(i: Int): String? {
        peak = maxOf(peak, TienLo.res())
        if (held() <= 0) return null
        used++
        warded++
        lastXa = "Xá lợi đỡ ${KIEP_NAME[i].lowercase()}: không thương thế"
        Store.note(lastXa)
        return lastXa
    }

    override fun kiepParts(): List<Pair<String, Int>> = buildList {
        if (TienLo.faction == 2) add("Thế lực ${FACTIONS[2]}" to 5)
        if (hasChuc(4)) add("Liên tòa ${CHUC[4]}" to 5)
    }

    override fun taskBonus() = if (hasChuc(6)) 5 else 0
    override fun tienKhiBonus() = 0
    override fun onOpen() {
        peak = TienLo.res()
        formedNoted = formed()
        lienTo = TienLo.openDay - 1
    }

    override fun check(): Boolean {
        var changed = false
        if (TienLo.res() > peak) {
            peak = TienLo.res()
            changed = true
        }
        while (formedNoted < formed()) {
            formedNoted++
            Store.note("Kết xá lợi thứ $formedNoted")
            changed = true
        }
        if (lienTri()) changed = true
        if (checkFellow()) changed = true
        if (lotusBreak()) changed = true
        return changed
    }

    /** The Đại kiếp Liên Hoa counts only days in a row: a day missed and not sealed starts it again. */
    private fun lotusBreak(): Boolean {
        if (TienLo.kiepActive != 2) return false
        val miss = (TienLo.kiepStart until today()).lastOrNull { it !in TienLo.kiepDays && it !in Store.sealedDays } ?: return false
        return TienLo.kiepDays.removeIf { it <= miss }
    }

    /** The longest run of days read in a row within `days`. */
    private fun longestRun(days: LongRange): Long {
        var best = 0
        var n = 0
        for (d in days) if (read(d)) { n++; best = maxOf(best, n) } else n = 0
        return best.toLong()
    }

    override fun taskProgress(task: Int, from: Long, to: Long): Long {
        val days = from..to
        return when (task) {
            0 -> days.count { Store.minutes(it) >= 10 }.toLong()
            1 -> longestRun(days)
            2 -> Store.books.values.count { it.finishDay in days }.toLong()
            3 -> days.count { Store.minutes(it) >= 30 && jumps(it) == 0 }.toLong()
            4 -> Store.wordsBetween(from, to)
            else -> freedDays.count { it in days }.toLong()
        }
    }

    /** Vô niệm: one sitting of 45 minutes and no jump; Liên Hoa: ten minutes, day after day. */
    override fun kiepDayKept(i: Int, d: Long): Boolean {
        if (i == 2) return Store.minutes(d) >= 10
        return jumps(d) == 0 && (NhatKy.days[d]?.sittings?.any { it >= 45 * 60 } ?: false)
    }

    override fun kiepEnd(i: Int) = true
    override fun kiepWindow(i: Int) = if (i == 2) 70 else 30
    override fun kiepNeed(i: Int) = if (i == 2) 49 else 21

    override fun toJson(): JSONObject = JSONObject().put("peak", peak).put("used", used).put("guarded", guarded).put("warded", warded)
        .put("formed", formedNoted).put("auto", autoGuard).put("lastXa", lastXa)
        .put("doMonth", doMonth).put("doBook", doBook).put("doFellow", doFellow).put("doDone", doDone).put("freed", freed)
        .put("freedDays", org.json.JSONArray().apply { freedDays.forEach { put(it) } }).put("lien", lienTo)

    override fun read(o: JSONObject) {
        reset()
        peak = o.optInt("peak"); used = o.optInt("used"); guarded = o.optInt("guarded"); warded = o.optInt("warded")
        formedNoted = o.optInt("formed"); autoGuard = o.optBoolean("auto", true); lastXa = o.optString("lastXa")
        doMonth = o.optLong("doMonth", -1); doBook = o.optInt("doBook", -1); doFellow = o.optInt("doFellow"); doDone = o.optBoolean("doDone")
        freed = o.optInt("freed")
        o.optJSONArray("freedDays")?.let { a -> for (i in 0 until a.length()) freedDays.add(a.getLong(i)) }
        lienTo = o.optLong("lien", -1)
    }
}
