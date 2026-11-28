package com.exacta.reader.data

import org.json.JSONObject

/**
 * 4.0 Thư Hải Vô Nhai, the giới of the wide reader: a sea of books with no shore, each author read a
 * lighthouse.
 *
 *  - Văn khí ([NhatKy.estimate]): 40 for each new author read through (15 the second book, 8 the third,
 *    4 after), a point each day with a book of an author not yet read through, 25 for each new genre or
 *    language.
 *  - Thần quốc: each author read through is an island; the islands give tín niệm each week (half the
 *    islands, an island of three books or more counting twice).
 *  - Văn mạch: five authors read through in one genre make an archipelago, and a văn mạch: +3% văn khí.
 *  - Thư các (Hải đồ, Hàn lâm viện, Mặc trì), the factions Hàn Lâm Thư Viện, Thiên Thư Các, Vạn Thư Đảo,
 *    the Văn kiếp (three new authors) and the Đại kiếp Thánh Đế (nine).
 */
object ThuHai : GioiRules {
    override val CHUC_LABEL = "văn bảo"
    override val SEAT_NAME = "Thư các"
    override val GREET = "Biển sách không bờ, ngọn hải đăng đầu tiên vừa sáng."
    override val END_LINE = "Vạn ngọn hải đăng cùng sáng, biển sách lặng sóng."
    override val CHUC = arrayOf("Tùng Yên Mặc", "Lang Hào Bút", "Tuyên Chỉ", "Đoan Nghiễn", "Thanh Đồng Trấn Chỉ", "Ngọc Giản", "Thiên Thư", "Hà Đồ", "Lạc Thư")
    override val CHUC_PERK = arrayOf(
        "Văn khí từ tác giả mới, thể loại và ngôn ngữ mới",
        "Thần quốc: mỗi tuần tín niệm cho văn khí",
        "Văn khí +3%",
        "Tín niệm thêm 2 mỗi tuần",
        "Tiên kiếp +5%",
        "Văn khí +3% nữa",
        "Việc thế lực +5 cống hiến",
        "Mỗi văn mạch thêm 1%",
        "Đạo hiệu tối thượng «Vạn Quyển Thánh Đế»",
    )
    override val HALLS = arrayOf("Hải đồ", "Hàn lâm viện", "Mặc trì")
    override val HALL_TEXT = arrayOf(
        "Bản đồ thần quốc: mỗi cấp, tín niệm mỗi tuần thêm 2 văn khí.",
        "Nơi nâng văn mạch: mỗi cấp, mỗi văn mạch thêm 1% văn khí.",
        "Ao mực mài bút thần: mỗi cấp +1 cấp tiên khí (+1% văn khí).",
    )
    override val FACTIONS = arrayOf("Hàn Lâm Thư Viện", "Thiên Thư Các", "Vạn Thư Đảo")
    override val FACTION_TEXT = arrayOf(
        "Thư viện của bậc hàn lâm, trọng uyên bác. Văn khí +5%.",
        "Gác giữ thiên thư, trọng tín niệm. Tín niệm thêm 3 mỗi tuần.",
        "Đảo của vạn cuốn sách, trọng du hải. Tiên kiếp +5%.",
    )
    override val TASKS = arrayOf(
        "Đọc trọn 2 cuốn",
        "Đọc trọn cuốn của một tác giả mới",
        "Đọc sách của 4 tác giả",
        "15 ngày đọc từ 20 phút",
        "Đọc 15 vạn chữ",
        "Đọc sách của 2 thể loại",
    )
    override val TASK_GOAL = longArrayOf(2, 1, 4, 15, 150_000, 2)
    override val WORD_TASK = 4
    override val KIEP_NAME = arrayOf("Văn kiếp thứ nhất", "Văn kiếp thứ hai", "Đại kiếp Thánh Đế")
    override val KIEP_RULE = arrayOf(
        "Trong 45 ngày đọc trọn sách của 3 tác giả mới",
        "Trong 45 ngày đọc trọn sách của 3 tác giả mới",
        "Trong 120 ngày đọc trọn sách của 9 tác giả mới",
    )

    const val ARCHIPELAGO = 5

    var tnWeek = -1L             // the Monday of the last week whose tín niệm is given
    var tnTotal = 0
    var lastTn = ""

    override fun reset() {
        tnWeek = -1; tnTotal = 0; lastTn = ""
    }

    private fun today() = Store.today()

    class Island(val author: String, val books: List<Book>, val genre: String)

    /** The thần quốc: each author read through, with the books read through, largest first. */
    fun islands(): List<Island> = Store.books.values.filter { it.finishDay >= 0 && it.author.isNotBlank() }
        .groupBy { NhatKy.key(it.author) }
        .map { (_, bs) -> Island(bs.first().author.trim(), bs.sortedBy { it.finishDay }, bs.flatMap { it.genres() }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "") }
        .sortedWith(compareByDescending<Island> { it.books.size }.thenBy { it.author.lowercase() })

    /** Each genre with the number of authors read through in it, most first. */
    fun genreAuthors(): List<Pair<String, Int>> = islands().flatMap { i -> i.books.flatMap { it.genres() }.distinct().map { it to NhatKy.key(i.author) } }
        .groupBy({ it.first }, { it.second }).map { it.key to it.value.distinct().size }.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

    /** The văn mạch: genres with five authors read through. */
    fun vanMach(): List<String> = genreAuthors().filter { it.second >= ARCHIPELAGO }.map { it.first }

    fun machPct() = 3 + TienLo.halls[1] + (if (hasChuc(7)) 1 else 0)

    /** The tín niệm of a week. */
    fun tinNiem(): Int {
        if (!hasChuc(1)) return 0
        val isl = islands()
        val base = (isl.size + isl.count { it.books.size >= 3 }) / 2
        return base + 2 * TienLo.halls[0] + (if (TienLo.faction == 1) 3 else 0) + (if (hasChuc(3)) 2 else 0)
    }

    /** New authors read through within `days`. */
    fun newAuthors(days: LongRange): Int = Store.books.values.filter { it.finishDay in days && it.author.isNotBlank() }
        .map { NhatKy.key(it.author) }.distinct().count { NhatKy.authorFirstFinish(it) in days }

    private fun booksRead(days: LongRange) = days.flatMap { d -> NhatKy.days[d]?.books?.filterValues { it >= 60 }?.keys ?: emptySet() }.toSet().mapNotNull { NhatKy.bookById(it) }

    // --- The common hooks ----------------------------------------------------------------------------

    override fun parts(): List<Pair<String, Int>> = buildList {
        val m = vanMach().size
        if (m > 0) add("Văn mạch $m" to m * machPct())
        if (hasChuc(2)) add("Văn bảo ${CHUC[2]}" to 3)
        if (hasChuc(5)) add("Văn bảo ${CHUC[5]}" to 3)
        if (TienLo.faction == 0) add("Thế lực ${FACTIONS[0]}" to 5)
        if (TienLo.faction >= 0 && TienLo.rank() > 0) add("${FACTIONS[TienLo.faction]} · ${TienLo.RANKS[TienLo.rank()]}" to TienLo.rank())
    }

    override fun dayExtra(d: Long) = 0L

    override fun kiepParts(): List<Pair<String, Int>> = buildList {
        if (TienLo.faction == 2) add("Thế lực ${FACTIONS[2]}" to 5)
        if (hasChuc(4)) add("Văn bảo ${CHUC[4]}" to 5)
    }

    override fun taskBonus() = if (hasChuc(6)) 5 else 0
    override fun tienKhiBonus() = TienLo.halls[2]
    override fun onOpen() {
        tnWeek = Store.weekStart(today()) - 7
    }

    /** Each finished week the thần quốc gives its tín niệm. */
    override fun check(): Boolean {
        val now = Store.weekStart(today())
        if (tnWeek < 0) tnWeek = now - 7
        var changed = false
        while (tnWeek + 7 < now) {
            tnWeek += 7
            val t = tinNiem()
            if (t > 0) {
                TienLo.resTenths += 10L * t
                tnTotal += t
                lastTn = "Tín niệm tuần qua: +$t văn khí"
                Store.note(lastTn)
            }
            changed = true
        }
        return changed
    }

    override fun taskProgress(task: Int, from: Long, to: Long): Long {
        val days = from..to
        return when (task) {
            0 -> Store.books.values.count { it.finishDay in days }.toLong()
            1 -> newAuthors(days).toLong()
            2 -> booksRead(days).map { NhatKy.key(it.author) }.filter { it.isNotEmpty() }.toSet().size.toLong()
            3 -> days.count { Store.minutes(it) >= 20 }.toLong()
            4 -> Store.wordsBetween(from, to)
            else -> booksRead(days).flatMap { it.genres() }.toSet().size.toLong()
        }
    }

    override fun kiepCount(i: Int) = newAuthors(TienLo.kiepStart..today())
    override fun kiepUnit(i: Int) = "tác giả"
    override fun kiepDayKept(i: Int, d: Long) = false
    override fun kiepEnd(i: Int) = true
    override fun kiepWindow(i: Int) = if (i == 2) 120 else 45
    override fun kiepNeed(i: Int) = if (i == 2) 9 else 3

    override fun toJson(): JSONObject = JSONObject().put("week", tnWeek).put("total", tnTotal).put("last", lastTn)

    override fun read(o: JSONObject) {
        reset()
        tnWeek = o.optLong("week", -1); tnTotal = o.optInt("total"); lastTn = o.optString("last")
    }
}
