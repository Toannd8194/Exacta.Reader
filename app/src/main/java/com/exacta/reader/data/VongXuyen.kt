package com.exacta.reader.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 4.0 Vong Xuyên Bỉ Ngạn, the giới of the return: beyond the river of forgetting, all that was left waits.
 *
 *  - Hồn lực ([NhatKy.estimate]): a point each 2.000 words read again (a book read through, opened from the
 *    start; a book left 90 days, taken up), a quarter of that for new pages; 30 for each book left and
 *    then read through (siêu độ); 10 for each rebirth.
 *  - Sổ sinh tử: each book begun and left a month is a soul in the book; each one read through is struck.
 *  - Vong Xuyên độ: each month an old book (read through half a year ago or more) is the boat; read it
 *    through again within the month to cross: +80 hồn lực.
 *  - Time means nothing to the dead: a chain broken stirs no tâm ma ([TuLuyen.tick]).
 *  - Minh phủ (Luân hồi bàn, Vọng hương đài, Bỉ ngạn hoa viên), the factions Thập Điện Minh Phủ, Bỉ Ngạn
 *    Hoa Cốc, Luân Hồi Ty, the Luân hồi kiếp (a long book read again) and the Đại kiếp Minh Đế (nine souls).
 */
object VongXuyen : GioiRules {
    override val CHUC_LABEL = "minh lệnh"
    override val SEAT_NAME = "Minh phủ"
    override val GREET = "Sông Vong Xuyên lặng lờ, hoa bỉ ngạn đỏ rực bờ bên kia."
    override val END_LINE = "Mười điện cúi đầu, sổ sinh tử khép lại trong tay."
    override val CHUC = arrayOf("Dẫn Hồn Lệnh", "Câu Hồn Lệnh", "Phán Quan Lệnh", "Minh Quân Lệnh", "Luân Hồi Lệnh", "Bỉ Ngạn Lệnh", "Minh Hà Lệnh", "Thập Điện Lệnh", "Minh Đế Lệnh")
    override val CHUC_PERK = arrayOf(
        "Hồn lực từ chữ đọc lại; đứt chuỗi không sinh tâm ma",
        "Vong Xuyên độ thêm 20 hồn lực",
        "Hồn lực +3%",
        "Mỗi oan hồn siêu độ thêm 10 hồn lực",
        "Tiên kiếp +5%",
        "Hồn lực +3% nữa",
        "Việc thế lực +5 cống hiến",
        "Tiên kiếp +5% nữa",
        "Đạo hiệu tối thượng «Vong Xuyên Minh Đế»",
    )
    override val HALLS = arrayOf("Luân hồi bàn", "Vọng hương đài", "Bỉ ngạn hoa viên")
    override val HALL_TEXT = arrayOf(
        "Bàn xoay luân hồi: mỗi cấp hồn lực +2%.",
        "Đài trông về cố hương: mỗi cấp, mỗi tháng +5 hồn lực.",
        "Vườn hoa bên kia sông: mỗi cấp, mỗi oan hồn siêu độ thêm 6 hồn lực.",
    )
    override val FACTIONS = arrayOf("Thập Điện Minh Phủ", "Bỉ Ngạn Hoa Cốc", "Luân Hồi Ty")
    override val FACTION_TEXT = arrayOf(
        "Mười điện phán xét, trọng phép âm. Hồn lực +5%.",
        "Thung lũng hoa bỉ ngạn, trọng siêu độ. Mỗi oan hồn siêu độ +10.",
        "Ty coi luân hồi, trọng nhân quả. Tiên kiếp +5%.",
    )
    override val TASKS = arrayOf(
        "10 ngày có đọc lại từ 2.000 chữ",
        "Siêu độ một oan hồn",
        "Mở lại 2 cuốn bỏ dở",
        "Qua Vong Xuyên độ",
        "Đọc 15 vạn chữ",
        "15 ngày đọc từ 20 phút",
    )
    override val TASK_GOAL = longArrayOf(10, 1, 2, 1, 150_000, 15)
    override val WORD_TASK = 4
    override val KIEP_NAME = arrayOf("Luân hồi kiếp thứ nhất", "Luân hồi kiếp thứ hai", "Đại kiếp Minh Đế")
    override val KIEP_RULE = arrayOf(
        "Trong 30 ngày đọc lại trọn một cuốn từ 30 vạn chữ",
        "Trong 30 ngày đọc lại trọn một cuốn từ 30 vạn chữ",
        "Trong 120 ngày siêu độ 9 oan hồn: 9 cuốn bỏ dở được đọc xong",
    )

    const val LEFT_DAYS = 30
    const val BOAT_AGE = 180
    const val BOAT_REWARD = 80
    const val LONG_BOOK = 300_000L

    var boatMonth = -1L
    var boatBook = -1
    var boatDone = false
    var crossed = 0
    val crossDays = ArrayList<Long>()
    var vongMonth = -1L          // the last month the Vọng hương đài gave
    var lastBoat = ""

    override fun reset() {
        boatMonth = -1; boatBook = -1; boatDone = false; crossed = 0; crossDays.clear(); vongMonth = -1; lastBoat = ""
    }

    private fun today() = Store.today()

    /** Sổ sinh tử: the souls, books begun and left a month or more. */
    fun souls() = Store.books.values.filter { it.finishDay < 0 && it.wordsRead > 0 && it.lastDay in 0..(today() - LEFT_DAYS) }.sortedBy { it.lastDay }

    /** The souls freed: books left and taken up again, then read through. */
    fun freed() = Store.books.values.filter { it.finishDay >= 0 && it.backDay in 0..it.finishDay }.sortedByDescending { it.finishDay }

    fun boat(): Book? = if (boatBook < 0) null else NhatKy.bookById(boatBook)
    fun boatReward() = BOAT_REWARD + (if (hasChuc(1)) 20 else 0)
    fun freeReward() = (if (hasChuc(3)) 10 else 0) + (if (TienLo.faction == 1) 10 else 0) + 6 * TienLo.halls[2]

    private fun pickBoat(): Boolean {
        val m = TienLo.monthOf(today())
        if (m == boatMonth) return false
        boatMonth = m
        boatDone = false
        val start = TienLo.monthStart(today())
        val old = Store.books.values.filter { it.finishDay in 0..(start - BOAT_AGE) }.sortedBy { it.id }
        boatBook = if (old.isEmpty()) -1 else old[(m % old.size).toInt()].id
        return true
    }

    private fun checkBoat(): Boolean {
        var changed = pickBoat()
        val b = boat()
        if (!boatDone && b != null && b.rereadDay >= maxOf(TienLo.monthStart(today()), TienLo.openDay)) {
            boatDone = true
            crossed++
            crossDays.add(today())
            TienLo.resTenths += 10L * boatReward()
            lastBoat = "Qua Vong Xuyên trên «${b.title}»: +${boatReward()} hồn lực"
            Store.note(lastBoat)
            changed = true
        }
        return changed
    }

    private fun vongHuong(): Boolean {
        val m = TienLo.monthOf(today())
        if (m == vongMonth) return false
        if (vongMonth >= 0 && TienLo.halls[1] > 0) TienLo.resTenths += 10L * 5 * TienLo.halls[1]
        vongMonth = m
        return true
    }

    // --- The common hooks ----------------------------------------------------------------------------

    override fun parts(): List<Pair<String, Int>> = buildList {
        if (TienLo.halls[0] > 0) add("Luân hồi bàn cấp ${TienLo.halls[0]}" to 2 * TienLo.halls[0])
        if (hasChuc(2)) add("Minh lệnh ${CHUC[2]}" to 3)
        if (hasChuc(5)) add("Minh lệnh ${CHUC[5]}" to 3)
        if (TienLo.faction == 0) add("Thế lực ${FACTIONS[0]}" to 5)
        if (TienLo.faction >= 0 && TienLo.rank() > 0) add("${FACTIONS[TienLo.faction]} · ${TienLo.RANKS[TienLo.rank()]}" to TienLo.rank())
    }

    /** What the seat, the faction and the lệnh add to each soul freed that day. */
    override fun dayExtra(d: Long): Long = 10L * freeReward() * Store.books.values.count { it.finishDay == d && it.backDay in 0..d }

    override fun kiepParts(): List<Pair<String, Int>> = buildList {
        if (TienLo.faction == 2) add("Thế lực ${FACTIONS[2]}" to 5)
        if (hasChuc(4)) add("Minh lệnh ${CHUC[4]}" to 5)
        if (hasChuc(7)) add("Minh lệnh ${CHUC[7]}" to 5)
    }

    override fun taskBonus() = if (hasChuc(6)) 5 else 0
    override fun tienKhiBonus() = 0
    override fun onOpen() {
        vongMonth = TienLo.monthOf(today())
    }

    override fun check(): Boolean {
        var changed = false
        if (vongHuong()) changed = true
        if (checkBoat()) changed = true
        return changed
    }

    override fun taskProgress(task: Int, from: Long, to: Long): Long {
        val days = from..to
        return when (task) {
            0 -> days.count { d -> NhatKy.days[d]?.let { it.reread + it.back >= 2_000 } ?: false }.toLong()
            1 -> Store.books.values.count { it.finishDay in days && it.backDay in 0..it.finishDay }.toLong()
            2 -> NhatKy.events.count { it.kind == NhatKy.E_BACK && it.day in days }.toLong()
            3 -> crossDays.count { it in days }.toLong()
            4 -> Store.wordsBetween(from, to)
            else -> days.count { Store.minutes(it) >= 20 }.toLong()
        }
    }

    /** Counted by books, not days. */
    override fun kiepCount(i: Int): Int {
        val days = TienLo.kiepStart..today()
        return if (i == 2) Store.books.values.count { it.finishDay in days && it.backDay in 0..it.finishDay }
        else Store.books.values.count { it.rereadDay in days && it.words >= LONG_BOOK }
    }

    override fun kiepUnit(i: Int) = if (i == 2) "oan hồn" else "cuốn"
    override fun kiepDayKept(i: Int, d: Long) = false
    override fun kiepEnd(i: Int) = true
    override fun kiepWindow(i: Int) = if (i == 2) 120 else 30
    override fun kiepNeed(i: Int) = if (i == 2) 9 else 1

    override fun toJson(): JSONObject = JSONObject().put("boatMonth", boatMonth).put("boatBook", boatBook).put("boatDone", boatDone)
        .put("crossed", crossed).put("crossDays", JSONArray().apply { crossDays.forEach { put(it) } }).put("vong", vongMonth).put("last", lastBoat)

    override fun read(o: JSONObject) {
        reset()
        boatMonth = o.optLong("boatMonth", -1); boatBook = o.optInt("boatBook", -1); boatDone = o.optBoolean("boatDone")
        crossed = o.optInt("crossed")
        o.optJSONArray("crossDays")?.let { a -> for (i in 0 until a.length()) crossDays.add(a.getLong(i)) }
        vongMonth = o.optLong("vong", -1); lastBoat = o.optString("last")
    }
}
