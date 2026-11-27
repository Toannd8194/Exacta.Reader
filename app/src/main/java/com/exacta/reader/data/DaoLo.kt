package com.exacta.reader.data

import com.exacta.reader.game.PhapMon
import org.json.JSONObject
import kotlin.random.Random

/**
 * 2.4 Ngũ Lộ Đồng Quy: the way of cultivation.
 *
 *  - Five ways (Pháp, Thể, Thần, Kiếm tu, Bách nghệ), chosen once at Trúc Cơ before ngưng đan.
 *    Each lives on its own kind of reading (linh lực from words, long sittings, books read through,
 *    books finished fast, the đan phòng and the lò luyện), changes the ngưng đan, gives its own
 *    Kim Đan, Nguyên Anh and Hóa Thần, and has a real strength and a real weakness.
 *  - Luyện thể (thể phách) and luyện thần (thần thức) are open to every way: the tiers of the body
 *    help through the lôi kiếp and thiên kiếp, the trọng of the mind through the small breakthroughs.
 *    Thể tu trains the body twice as fast, Thần tu the mind.
 *  - Kiêm tu: from Nguyên Anh a second way, its strengths without its weakness, for 20% of speed.
 *  - Song Anh: who walks both Thể and Thần to Nguyên Anh đại viên mãn may try for a second Nguyên Anh.
 *  - Twelve Kim Đan, formed by how the cultivator grew (the linh căn and công pháp, the dị linh căn,
 *    the hours of reading, life and death); the Nguyên Anh takes its nature from the đan. Six Hóa
 *    Thần, by the way walked, each with its weight on Hợp Thể and Độ Kiếp.
 *  - Nghiệp lực: the deeds of the whole life of cultivation, seen from Phi Thăng, weighing on the
 *    six realms of Lục Giới.
 */
object DaoLo {
    const val PHAP = 0
    const val THE = 1
    const val THAN = 2
    const val KIEM = 3
    const val BACH = 4

    class Way(
        val name: String, val icon: String, val lives: String, val forms: List<String>,
        val strong: String, val weak: String, val text: String,
        /** Ngưng đan: days, and the minutes of one sitting each day. */
        val danDays: Int, val danMinutes: Int,
    )

    val WAYS = listOf(
        Way("Pháp tu", "dao_phap", "chữ đọc: mỗi chữ thành linh lực",
            listOf("", "Pháp Anh", "Thuần Dương Nguyên Thần"),
            "Linh lực +15%", "Lôi kiếp, thiên kiếp −5%",
            "Lấy chữ làm linh khí, luyện thuật pháp chính tông. Đường rộng nhất, đi được xa nhất, nhưng nhục thân yếu nên kiếp số khó đỡ.",
            3, 45),
        Way("Thể tu", "dao_the", "giờ đọc: buổi liền 60 phút +1.000 linh lực",
            listOf("", "Thể Anh", "Bất Hoại Pháp Thân"),
            "Luyện thể ×2; thân đỡ kiếp: +2% mỗi bậc thể ở lôi kiếp, thiên kiếp", "Linh lực −10%",
            "Đan kết trong máu thịt, anh dung vào thân. Ngồi càng lâu, xương cốt càng cứng; chữ đọc thành linh lực chậm hơn.",
            2, 60),
        Way("Thần tu", "dao_than", "sách đọc trọn: +3.000, tác giả mới +5.000 linh lực",
            listOf("", "Thần Anh", "Phân Thần Hóa Thân"),
            "Luyện thần ×2; tâm ma tăng chậm một nửa", "Thể phách −25%",
            "Tu thức hải trước tu đan điền. Mỗi cuốn đọc xong là một lần mở mắt; đọc nhiều tác giả, thần thức càng rộng.",
            4, 35),
        Way("Kiếm tu", "dao_kiem", "đọc nhanh: cuốn xong trong 14 ngày +4.000; chuỗi từ 7 ngày +10%",
            listOf("", "Kiếm Anh", "Kiếm Ý Thông Thiên"),
            "Đột phá chí mạng: 1/4 lần thành công vào thẳng trung kỳ", "Tâm ma tăng gấp rưỡi",
            "Một kiếm một sách, đọc tới cuối không quay đầu. Dứt khoát thì phá cảnh như chém, nhưng tâm dễ động.",
            2, 50),
        Way("Bách nghệ", "dao_bach", "đan phòng, lò luyện: mỗi lò đan, mỗi pháp bảo +1.500 linh lực",
            listOf("", "Hỏa Anh", "Thiên Hỏa Hóa Thân"),
            "Luyện đan +15%, đan độc −25%", "Linh lực −15%",
            "Trồng thảo, luyện đan, rèn khí. Đan hỏa nuôi đan điền; tu vi chậm nhất mà tay nghề hơn người.",
            3, 45),
    )

    // --- Mười hai Kim Đan ------------------------------------------------------------------------------

    class Dan(
        val name: String, val prefix: String, val group: String, val how: String, val good: String,
        val speed: Int, val canCo: Int, val stir: Int, val anh: Int, val loiDays: Int,
        /** The realm of Lục Giới it leans to. */
        val gioi: Int,
    )

    val DANS = listOf(
        Dan("Canh Kim Đan", "Canh Kim", "Ngũ hành", "linh căn Kim, vận công pháp Kim", "linh lực +5%, kết anh +6%", 5, 0, 100, 6, 0, 0),
        Dan("Ất Mộc Đan", "Ất Mộc", "Ngũ hành", "linh căn Mộc, vận công pháp Mộc", "căn cơ +8, kết anh +3%", 0, 8, 100, 3, 0, 3),
        Dan("Nhâm Thủy Đan", "Nhâm Thủy", "Ngũ hành", "linh căn Thủy, vận công pháp Thủy", "tâm ma tăng chậm 25%, kết anh +4%", 0, 0, 75, 4, 0, 4),
        Dan("Bính Hỏa Đan", "Bính Hỏa", "Ngũ hành", "linh căn Hỏa, vận công pháp Hỏa", "linh lực +12%, tâm ma +15%, kết anh −3%", 12, 0, 115, -3, 0, 1),
        Dan("Mậu Thổ Đan", "Mậu Thổ", "Ngũ hành", "linh căn Thổ, hoặc khi chưa có đan nào khác", "căn cơ +5, lôi kiếp −1 ngày, kết anh +4%", 0, 5, 100, 4, 1, 2),
        Dan("Tử Lôi Đan", "Tử Lôi", "Biến dị", "dị linh căn Lôi", "linh lực +8%, lôi kiếp −2 ngày, kết anh +8%", 8, 0, 100, 8, 2, 0),
        Dan("Huyền Băng Đan", "Huyền Băng", "Biến dị", "dị linh căn Băng", "tâm ma tăng chậm 40%, kết anh +6%", 0, 0, 60, 6, 0, 4),
        Dan("Thanh Phong Đan", "Thanh Phong", "Biến dị", "dị linh căn Phong", "linh lực +15%", 15, 0, 100, 0, 0, 2),
        Dan("Thái Dương Đan", "Thái Dương", "Âm Dương", "tâm cảnh Thanh Minh, 3/4 giờ đọc từ 6 tới 18 giờ", "linh lực +15%, kết anh +5%", 15, 0, 100, 5, 0, 0),
        Dan("Thái Âm Đan", "Thái Âm", "Âm Dương", "tâm cảnh Thanh Minh, nửa giờ đọc từ 22 tới 5 giờ", "căn cơ +8, tâm ma tăng chậm 40%, kết anh +5%", 0, 8, 60, 5, 0, 4),
        Dan("Trường Sinh Đan", "Trường Sinh", "Sinh Tử", "căn cơ 80, chưa đột phá hỏng lần nào, chuỗi 60 ngày", "căn cơ +12, tâm ma tăng chậm 20%, kết anh +10%", 0, 12, 80, 10, 0, 2),
        Dan("Tịch Diệt Đan", "Tịch Diệt", "Sinh Tử", "ba lần đột phá hỏng, hoặc từng tẩu hỏa nhập ma", "linh lực +20%, tâm ma +30%, kết anh +10%", 20, 0, 130, 10, 0, 1),
    )

    // --- Sáu Hóa Thần ---------------------------------------------------------------------------------

    class Than(
        val name: String, val from: String, val good: String,
        val speed: Int, val the: Int, val thanThuc: Int, val hop: Int, val hopDays: Int, val kiep: Int, val brew: Int, val gioi: Int,
    )

    val THANS = listOf(
        Than("Thuần Dương Nguyên Thần", "Pháp tu", "linh lực +10% · Hợp Thể −5% · Độ Kiếp +10%", 10, 100, 100, -5, 0, 10, 0, 0),
        Than("Bất Hoại Pháp Thân", "Thể tu", "thể phách +20% · Hợp Thể +10%, thiên kiếp −1 ngày · Độ Kiếp +5%", 0, 120, 100, 10, -1, 5, 0, 3),
        Than("Phân Thần Hóa Thân", "Thần tu", "thần thức +20% · Hợp Thể −10% · Độ Kiếp +8%", 0, 100, 120, -10, 0, 8, 0, 5),
        Than("Kiếm Ý Thông Thiên", "Kiếm tu", "linh lực +5% · Hợp Thể +5% · Độ Kiếp −10%, kiếm khí chiêu lôi", 5, 100, 100, 5, 0, -10, 0, 1),
        Than("Thiên Hỏa Hóa Thân", "Bách nghệ", "luyện đan +10% · Hợp Thể thiên kiếp +1 ngày · Độ Kiếp +15%", 0, 100, 100, 0, 1, 15, 10, 4),
        Than("Song Thần Đồng Tu", "Song Anh", "linh lực +5% · Hợp Thể +15%, thiên kiếp −1 ngày · Độ Kiếp +5%", 5, 100, 100, 15, -1, 5, 0, 2),
    )

    const val KIEM_TU_COST = 20
    const val SONG_REST_DAYS = 7

    // --- State -------------------------------------------------------------------------------------

    var started = false
    var way = -1
    var way2 = -1
    var wayDay = -1L
    var way2Day = -1L
    var songAnh = false
    var songDay = -1L
    var songFails = 0
    var songRest = -1L
    var crits = 0
    var dan = -1             // the Kim Đan formed (DANS), from Kim Đan on
    var than = -1            // the Hóa Thần (THANS), from Hóa Thần on
    var earned = 0L          // linh lực the way's own reading gave
    var lastResult = ""
    var rng: Random = Random.Default

    fun reset() {
        started = false; way = -1; way2 = -1; wayDay = -1; way2Day = -1; songAnh = false; songDay = -1
        songFails = 0; songRest = -1; crits = 0; earned = 0; lastResult = ""; dan = -1; than = -1
    }

    fun migrate() {
        reset()
        started = true
        Store.touch()
    }

    private fun today() = Store.today()

    /** The main way, or the second taken in kiêm tu: their strengths. */
    fun walks(w: Int) = way == w || way2 == w
    fun main(w: Int) = way == w

    // --- Choosing ------------------------------------------------------------------------------------

    fun canChoose() = way < 0 && Store.realmCap >= 1
    fun choose(w: Int): Boolean {
        if (!canChoose() || w !in WAYS.indices) return false
        way = w
        wayDay = today()
        Store.note("Đạo lộ: bước vào ${WAYS[w].name}")
        Store.touch()
        return true
    }

    fun canKiemTu() = way >= 0 && way2 < 0 && Store.realmCap >= 3
    fun chooseSecond(w: Int): Boolean {
        if (!canKiemTu() || w !in WAYS.indices || w == way) return false
        way2 = w
        way2Day = today()
        Store.note("Kiêm tu ${WAYS[w].name}: linh lực −$KIEM_TU_COST%")
        Store.touch()
        return true
    }

    /** How well the reading so far fits each way (0..100), to show the one that suits. */
    fun fit(w: Int): Int {
        val t = today()
        val words30 = (t - 29..t).sumOf { Store.dayWords[it] ?: 0L }
        val finished = Store.books.values.filter { it.finishDay >= 0 }
        return when (w) {
            PHAP -> (words30 / 3_000).toInt()
            THE -> TuLuyen.thePhach() / 3
            THAN -> 8 * TuLuyen.authors() + 2 * finished.size
            KIEM -> 12 * finished.count { it.firstDay >= 0 && it.finishDay - it.firstDay < 14 } + Store.bestStreak()
            else -> 4 * DongPhu.made + 3 * Store.beasts.size
        }.coerceIn(0, 100)
    }

    fun suggested(): Int = WAYS.indices.maxByOrNull { fit(it) * 10 - it } ?: PHAP

    /** The form of the realm reached: "Tử Lôi Đan", "Tử Lôi Kiếm Anh", "Kiếm Ý Thông Thiên". */
    fun form(r: Int = Store.realmCap): String? = when {
        r < 2 -> null
        r == 2 -> DANS[danNow()].name
        r == 3 -> anhName()
        else -> THANS[thanNow()].name
    }

    fun danNow() = if (dan >= 0) dan else danFor()
    fun thanNow() = if (than >= 0) than else thanFor()
    fun anhName() = DANS[danNow()].prefix + " " + (if (way >= 0) WAYS[way].forms[1] else "Nguyên Anh")

    // --- Which Kim Đan, which Hóa Thần -----------------------------------------------------------------

    /** Share (0..100) of the time read in the hours `h` of the clock (past 23 wraps to the morning). */
    private fun share(hours: IntRange): Int {
        val all = Store.hourSeconds.sum()
        if (all <= 0) return 0
        return (hours.sumOf { Store.hourSeconds[it % 24] } * 100 / all).toInt()
    }
    fun dayShare() = share(6..17)
    fun nightShare() = share(22..28)

    /** The Kim Đan that forms from the life so far: Sinh Tử, Âm Dương, biến dị, then ngũ hành. */
    fun danFor(): Int {
        val calm = TuLuyen.tamCanhTier() >= 2
        return when {
            TuLuyen.breakFails >= 3 || TuLuyen.tauHoaDay >= 0 -> 11
            TuLuyen.canCo() >= 80 && TuLuyen.breakFails == 0 && Store.bestStreak() >= 60 -> 10
            calm && dayShare() >= 75 -> 8
            calm && nightShare() >= 50 -> 9
            DaoPhap.di == PhapMon.LOI -> 5
            DaoPhap.di == PhapMon.BANG -> 6
            DaoPhap.di == PhapMon.PHONG -> 7
            else -> {
                val attr = DaoPhap.activeDef()?.attr ?: -1
                val first = DaoPhap.roots.firstOrNull() ?: -1
                when {
                    attr in 0..4 && attr in DaoPhap.roots -> attr
                    first in 0..4 -> first
                    else -> 4
                }
            }
        }
    }

    /** The Hóa Thần of the way walked; Song Anh above all. */
    fun thanFor(): Int = if (songAnh) 5 else if (way >= 0) way else 0

    /** On the breakthrough into Kim Đan and Hóa Thần: the form is fixed. */
    fun onBreak(g: Int) {
        if (g == 2) dan = danFor()
        if (g == 4) than = thanFor()
        Store.touch()
    }

    /** Old saves past Kim Đan or Hóa Thần: the form from the life so far. */
    fun settle() {
        if (Store.realmCap >= 2 && dan < 0) { dan = danFor(); Store.touch() }
        if (Store.realmCap >= 4 && than < 0) { than = thanFor(); Store.touch() }
    }

    private fun danOn() = if (Store.realmCap >= 2 && dan >= 0) DANS[dan] else null
    private fun thanOn() = if (Store.realmCap >= 4 && than >= 0) THANS[than] else null

    /** The Kim Đan's speed: half again once the Nguyên Anh has taken it in. */
    fun danSpeed() = danOn()?.let { if (Store.realmCap >= 3) it.speed * 3 / 2 else it.speed } ?: 0
    fun canCoBonus() = danOn()?.canCo ?: 0
    fun loiDays() = danOn()?.loiDays ?: 0
    fun hopDays() = thanOn()?.hopDays ?: 0

    // --- Nghiệp lực -------------------------------------------------------------------------------------

    /** Nghiệp lực shows from Phi Thăng, and only then weighs on Lục Giới. */
    fun karmaShown() = Store.realmCap >= 8

    /** The deeds of the whole life, part by part: thiện nghiệp (+), ác nghiệp (−). */
    fun karmaParts(): List<Pair<String, Int>> {
        val t = Store.today()
        val list = ArrayList<Pair<String, Int>>()
        val done = Store.books.values.count { it.finishDay >= 0 }
        if (done > 0) list.add("Đọc trọn $done cuốn" to minOf(40, 2 * done))
        val streak = Store.bestStreak()
        if (streak >= 10) list.add("Chuỗi dài nhất $streak ngày" to minOf(20, streak / 10))
        val tier = TuLuyen.tamCanhTier()
        if (tier > 0) list.add("Tâm cảnh ${TuLuyen.TAM_CANH_TIERS[tier]}" to 5 * tier)
        if (TongMon.gongsTotal >= 100) list.add("Cống hiến tông môn" to minOf(20, TongMon.gongsTotal / 100))
        val left = Store.books.values.count { it.finishDay < 0 && it.progress in 0.05..0.95 && it.lastDay in 0..(t - 90) }
        if (left > 0) list.add("Bỏ rơi $left cuốn đọc dở" to -minOf(30, 3 * left))
        val jumps = TuLuyen.dayJumps.values.sum()
        if (jumps >= 5) list.add("Nhảy chương $jumps lần" to -minOf(20, jumps / 5))
        if (TuLuyen.tamMa >= 2) list.add("Tâm ma ${TuLuyen.tamMa}" to -(TuLuyen.tamMa / 2))
        if (TuLuyen.tauHoaDay >= 0) list.add("Từng tẩu hỏa nhập ma" to -10)
        if (PhapMon.TP_CHUNG_MA in DaoPhap.tam) list.add("Luyện Đạo Tâm Chủng Ma" to -15)
        if (DongPhu.danDoc >= 4) list.add("Đan độc ${DongPhu.danDoc}" to -(DongPhu.danDoc / 4))
        return list
    }

    fun karma() = karmaParts().sumOf { it.second }.coerceIn(-100, 100)
    fun karmaName(v: Int = karma()) = when {
        v >= 60 -> "Đại thiện"
        v >= 20 -> "Thiện nghiệp"
        v > -20 -> "Quân bình"
        v > -60 -> "Ác nghiệp"
        else -> "Nghiệp chướng"
    }

    /** How each realm weighs nghiệp lực, in tenths: thiện lifts Cửu Tiêu and Bát Nhã, ác the abyss and the river. */
    val KARMA_W = intArrayOf(4, -5, 5, 0, -3, 2)
    val KARMA_TEXT = arrayOf("thiện nghiệp × 0,4", "ác nghiệp × 0,5", "thiện nghiệp × 0,5",
        "nghiệp lực quân bình: (20 − |nghiệp lực|) / 2", "ác nghiệp × 0,3", "thiện nghiệp × 0,2")

    /** What nghiệp lực gives realm i; nothing before Phi Thăng. */
    fun karmaPart(i: Int): Int {
        if (!karmaShown()) return 0
        val k = karma()
        return if (i == 3) (20 - kotlin.math.abs(k)) / 2 else k * KARMA_W[i] / 10
    }

    // --- Effects -----------------------------------------------------------------------------------

    /** Linh lực per word, in points of percent (added to the speed of the linh căn and công pháp). */
    fun speedBonus(): Int {
        if (way < 0) return danSpeed() + (thanOn()?.speed ?: 0)
        var v = 0
        if (walks(PHAP)) v += 15
        if (main(THE)) v -= 10
        if (main(BACH)) v -= 15
        if (walks(KIEM) && Store.streak() >= 7) v += 10
        if (way2 >= 0) v -= KIEM_TU_COST
        if (songAnh) v += 10
        v += danSpeed() + (thanOn()?.speed ?: 0)
        return v
    }

    fun speedParts(): List<Pair<String, Int>> {
        val list = ArrayList<Pair<String, Int>>()
        if (walks(PHAP)) list.add("Pháp tu" to 15)
        if (main(THE)) list.add("Thể tu" to -10)
        if (main(BACH)) list.add("Bách nghệ" to -15)
        if (walks(KIEM) && Store.streak() >= 7) list.add("Kiếm tu, chuỗi ${Store.streak()} ngày" to 10)
        if (way2 >= 0) list.add("Kiêm tu ${WAYS[way2].name}" to -KIEM_TU_COST)
        if (songAnh) list.add("Song Anh" to 10)
        if (danSpeed() != 0) list.add(DANS[dan].name + (if (Store.realmCap >= 3) ", anh kế thừa" else "") to danSpeed())
        thanOn()?.let { if (it.speed != 0) list.add(it.name to it.speed) }
        return list
    }

    /** Percent of thể phách gained: Thể tu twice, Thần tu three quarters. */
    fun thePercent() = (if (walks(THE)) 200 else 100) * (if (main(THAN)) 75 else 100) / 100 * (thanOn()?.the ?: 100) / 100
    fun thanPercent() = (if (walks(THAN)) 200 else 100) * (thanOn()?.thanThuc ?: 100) / 100

    /** Percent of a rise of tâm ma that comes through. */
    fun stirPercent() = (if (walks(THAN)) 50 else 100) * (if (main(KIEM)) 150 else 100) / 100 * (danOn()?.stir ?: 100) / 100

    fun brewBonus() = (if (walks(BACH)) 15 else 0) + (thanOn()?.brew ?: 0)
    fun docPercent() = if (walks(BACH)) 75 else 100

    private fun kiep(g: Int) = g == 3 || g >= 5

    /** The parts of the chance of gate g that come from the way, the body and Song Anh. */
    fun breakParts(g: Int): List<Pair<String, Int>> {
        val list = ArrayList<Pair<String, Int>>()
        val tier = TuLuyen.theTier()
        if (kiep(g) && tier > 0) {
            val per = if (walks(THE)) 2 else 1
            list.add("Thể phách ${TuLuyen.THE_TIERS[tier]}" + (if (per > 1) ", thân đỡ kiếp" else "") to per * tier)
        }
        if (kiep(g) && main(PHAP)) list.add("Pháp tu, nhục thân yếu" to -5)
        if (way >= 0 && TongMon.sect == way) list.add("Đạo lộ hợp tông môn" to 2)
        if (songAnh) list.add("Song Anh" to 5)
        danOn()?.let { if (g == 3 && it.anh != 0) list.add("${it.name} phá đan thành anh" to it.anh) }
        thanOn()?.let {
            if (g == 6 && it.hop != 0) list.add("${it.name} hợp thân" to it.hop)
            if (g == 8 && it.kiep != 0) list.add("${it.name} độ kiếp" to it.kiep)
        }
        if (g == 9 && karmaShown()) list.add("Nghiệp lực ${karma()}, chia mười" to karma() / 10)
        return list
    }

    /** Luyện thần: each trọng +1% to a small breakthrough (Thần tu +2%). */
    fun minorPart() = TuLuyen.trong() * (if (walks(THAN)) 2 else 1)

    /** Ngưng đan of the way: days and the minutes of the sitting. */
    fun danDays() = if (way >= 0) WAYS[way].danDays else 3
    fun danMinutes() = if (way >= 0) WAYS[way].danMinutes else 45

    // --- Living on the way's own reading -----------------------------------------------------------

    private fun gain(n: Long, why: String) {
        if (n <= 0 || Store.realmCap !in 0..8) return
        DongPhu.gainLinhLuc(n)
        earned += n
        Store.note("$why: linh lực +${n / 1_000}.${"%03d".format(n % 1_000)}")
        Store.touch()
    }

    fun onLongSitting() {
        if (walks(THE)) gain(1_000, "Thể tu, buổi đọc liền 60 phút")
    }

    fun onFinish(book: Book) {
        if (walks(THAN)) {
            val a = book.author.trim().lowercase()
            val fresh = a.isNotEmpty() && Store.books.values.none { it !== book && it.finishDay >= 0 && it.author.trim().lowercase() == a }
            gain(if (fresh) 5_000 else 3_000, if (fresh) "Thần tu, tác giả mới" else "Thần tu, đọc trọn sách")
        }
        if (walks(KIEM) && book.firstDay >= 0 && book.finishDay - book.firstDay < 14) gain(4_000, "Kiếm tu, đọc trọn trong 14 ngày")
    }

    fun onPill() {
        if (walks(BACH)) gain(1_500, "Bách nghệ, thành đan")
    }

    fun onForge() {
        if (walks(BACH)) gain(1_500, "Bách nghệ, pháp bảo xuất thế")
    }

    /** Kiếm tu: a quarter of the breakthroughs that hold go straight into trung kỳ. */
    fun crit(g: Int): Boolean {
        if (!main(KIEM) || g !in 2..8) return false
        if (rng.nextInt(4) != 0) return false
        crits++
        return true
    }

    // --- Song Anh -----------------------------------------------------------------------------------

    fun songWays() = way >= 0 && way2 >= 0 && setOf(way, way2) == setOf(THE, THAN)

    class Need(val text: String, val met: Boolean)

    fun songNeeds(): List<Need> {
        val cap = Store.realmCap
        val r = TuLuyen.realm()
        return listOf(
            Need("Kiêm tu cả Thể tu và Thần tu", songWays()),
            Need("Nguyên Anh đại viên mãn trở lên", cap > 3 || (cap == 3 && r == 3 && TuLuyen.subCap >= 3)),
            Need("Thể phách Kim Thân, đang ${TuLuyen.THE_TIERS[TuLuyen.theTier()]}", TuLuyen.theTier() >= 4),
            Need("Thần thức Thất trọng, đang ${TuLuyen.TRONG[TuLuyen.trong()]}", TuLuyen.trong() >= 6),
            Need("Tâm ma dưới 30, đang ${TuLuyen.tamMa}", TuLuyen.tamMa < 30),
        )
    }

    fun songResting() = today() < songRest
    fun canSong() = !songAnh && songNeeds().all { it.met } && !songResting()

    /** The hardest gate of Nhân giới: 20%, a quarter of căn cơ, the tâm cảnh, +10% each failure. */
    fun songChance() = (20 + TuLuyen.canCo() / 4 + 5 * TuLuyen.tamCanhTier() + 10 * songFails).coerceIn(5, 80)

    fun trySong(): Boolean? {
        if (!canSong()) return null
        val ok = rng.nextInt(100) < songChance()
        if (ok) {
            songAnh = true
            songDay = today()
            lastResult = "Song Anh thành: Thể Anh và Thần Anh cùng ngồi trong đan điền"
            Store.gainLinhThach(500)
            Store.note("Song Anh thành: linh lực +10%, đột phá +5% · 500 linh thạch")
        } else {
            songFails++
            TuLuyen.stir(10)
            songRest = today() + SONG_REST_DAYS
            lastResult = "Song Anh hỏng: hai anh tranh nhau, tâm ma +10, nghỉ $SONG_REST_DAYS ngày (+10% lần sau)"
            Store.note(lastResult)
        }
        Store.touch()
        return ok
    }

    // --- Saving --------------------------------------------------------------------------------------

    fun toJson(): JSONObject = JSONObject().put("way", way).put("way2", way2).put("wayDay", wayDay).put("way2Day", way2Day)
        .put("song", songAnh).put("songDay", songDay).put("songFails", songFails).put("songRest", songRest)
        .put("crits", crits).put("earned", earned).put("last", lastResult).put("dan", dan).put("than", than)

    fun read(o: JSONObject) {
        reset()
        started = true
        way = o.optInt("way", -1); way2 = o.optInt("way2", -1); wayDay = o.optLong("wayDay", -1); way2Day = o.optLong("way2Day", -1)
        songAnh = o.optBoolean("song"); songDay = o.optLong("songDay", -1); songFails = o.optInt("songFails"); songRest = o.optLong("songRest", -1)
        crits = o.optInt("crits"); earned = o.optLong("earned"); lastResult = o.optString("last")
        dan = o.optInt("dan", -1); than = o.optInt("than", -1)
    }
}
