package com.exacta.reader.data

import com.exacta.reader.game.Lunar
import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.PhapMon.CONG_PHAP
import com.exacta.reader.game.PhapMon.TAM_PHAP
import com.exacta.reader.game.PhapMon.THUAT_PHAP
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/**
 * 2.1 Vạn Pháp Quy Nguyên: what the cultivator is born with and what they learn.
 *
 *  - Linh căn awakens at Luyện Khí trung kỳ from the hours the reader reads at: each hành has its
 *    hours, and every hành with 15% of the reading time is a root. One root tu fastest, five
 *    slowest but learn everything. Rare habits give a dị linh căn (Lôi, Băng, Phong).
 *  - Công pháp: one runs at a time. Its phẩm says how far it carries (a gate asks for a phẩm), its
 *    nine tầng grow with the words read while it runs, and tầng 4, 7, 9 need ngộ tính.
 *  - Ngộ tính: what the tầng of each book were in 1.0. A book read to tầng 4 and 7 gives one each,
 *    read through one more (two for a long book).
 *  - Tâm pháp (3.2): one slot from Trúc Cơ, two from Nguyên Anh, three from Luyện Hư. Each sitting of
 *    30 minutes is one vận chuyển, two if it keeps to the tâm pháp's way; five cảnh at 5, 15, 30, 60,
 *    100. Ma đạo will not run beside Phật môn or Nho môn; Đạo gia beside Phật môn turns 20% more.
 *  - Thuật pháp: cards that act by themselves in the events of the game (bí cảnh, kiếp, đột phá
 *    hỏng, tâm ma); each use adds thuần thục.
 *  - Ngọc giản come from kỳ ngộ, the weekly bí cảnh and the Tàng Kinh Các (linh thạch).
 */
object DaoPhap {
    const val WASH_PRICE = 300L
    const val WASH_HOURS = 10L
    const val AWAKEN_SECONDS = 3600L
    const val ROOT_SHARE = 15

    class Cong(var words: Long = 0, var opened: Int = 3, var sect: Int = -1)
    class Thuat(var learned: Boolean = false, var uses: Int = 0, var sits: Int = 0, var cool: Int = 0)

    var started = false
    val roots = ArrayList<Int>()         // hành of the linh căn, strongest first; empty until awakened
    var di = -1                          // dị linh căn (Lôi, Băng, Phong) or -1
    var awakenDay = -1L
    val hoursAt = LongArray(24)          // the hours at the awakening: tẩy linh reads the ones since
    var washed = false
    var ngo = 0
    var ngoEarned = 0
    val cong = LinkedHashMap<Int, Cong>()
    var active = -1
    var capNoted = -1
    val tam = LinkedHashMap<Int, Int>()  // tâm pháp -> vận chuyển
    val tamOn = ArrayList<Int>()
    var bangTamMonth = -1
    // 3.2 Tâm Đăng Bất Diệt.
    val tamFrac = HashMap<Int, Int>()    // tenths of a vận chuyển owed by Đạo gia beside Phật môn
    var tamDay = -1L                     // the day of the sittings counted below
    var tamSittings = 0                  // sittings of 30 minutes that day
    val thuat = LinkedHashMap<Int, Thuat>()
    val thuatOn = ArrayList<Int>()
    var shields = 0                      // tâm ma attacks still blocked in this kiếp
    var liemTucDay = -1L
    var offerWeek = -1L
    val offers = ArrayList<Offer>()
    // 3.4 Tàng Kinh Cửu Lâu.
    val shelf = ArrayList<Scroll>()          // ngọc giản waiting to be read through; the first lies on the án
    val frags = HashMap<Int, Int>()          // tàn quyển of Linh and Tiên công pháp
    val seenCong = HashSet<Int>()
    val seenTam = HashSet<Int>()
    val seenThuat = HashSet<Int>()
    val custom = ArrayList<PhapMon.CongPhap>()   // công pháp of the cultivator's own making
    var scrolls = 0
    var gifted = 0
    // 3.1 Vạn Pháp Trùng Tu.
    var phu = -1                         // the công pháp of phụ tu, or -1
    var xung = 0                         // the tầng a xung quan is breaking through (4, 7, 9), or 0
    var xungCost = 0                     // the ngộ tính it holds, given back if it fails
    val inherited = HashSet<Int>()       // công pháp that already took the words of their line
    var wayWords = 0L                    // words read in the way of the công pháp running
    var lastXung = ""
    var thuatDay = -1L                   // 3.3: the last day of reading that shortened the hồi chiêu                       // 1: the awakening's gifts, 2: the tâm pháp of Trúc Cơ

    private fun today() = Store.today()

    fun reset() {
        started = false; roots.clear(); di = -1; awakenDay = -1; hoursAt.fill(0); washed = false
        ngo = 0; ngoEarned = 0; cong.clear(); active = -1; capNoted = -1; tam.clear(); tamOn.clear(); bangTamMonth = -1
        tamFrac.clear(); tamDay = -1; tamSittings = 0
        thuat.clear(); thuatOn.clear(); shields = 0; liemTucDay = -1; offerWeek = -1; offers.clear(); scrolls = 0; gifted = 0
        phu = -1; xung = 0; xungCost = 0; inherited.clear(); wayWords = 0; lastXung = ""; thuatDay = -1
        shelf.clear(); frags.clear(); seenCong.clear(); seenTam.clear(); seenThuat.clear(); custom.clear()
        while (CONG_PHAP.size > PhapMon.BASE_CONG) CONG_PHAP.removeAt(CONG_PHAP.size - 1)
    }

    // --- Linh căn ------------------------------------------------------------------------------------

    fun awakened() = roots.isNotEmpty()

    /** Seconds read in each hành's hours, since `base` (the hours at some moment) when given. */
    fun hanhSeconds(base: LongArray? = null): LongArray {
        val s = LongArray(5)
        for (h in 0 until 24) s[PhapMon.hanhOfHour(h)] += (Store.hourSeconds[h] - (base?.get(h) ?: 0)).coerceAtLeast(0)
        return s
    }

    fun shares(sec: LongArray = hanhSeconds()): IntArray {
        val total = sec.sum()
        return IntArray(5) { if (total > 0) (sec[it] * 100 / total).toInt() else 0 }
    }

    fun rootsFrom(sec: LongArray): List<Int> {
        val total = sec.sum()
        val list = (0 until 5).filter { total > 0 && sec[it] * 100 >= ROOT_SHARE * total }.sortedByDescending { sec[it] }
        return list.ifEmpty { listOf((0 until 5).maxBy { sec[it] }) }
    }

    /** Luyện Khí trung kỳ reached and an hour read, so the hours say something. */
    fun awakenReady() = (Store.realmCap >= 1 || TuLuyen.sub(0, Store.tuVi()) >= 3) && Store.hourSeconds.sum() >= AWAKEN_SECONDS

    /** Lôi: a tenth of the reading after midnight (0 – 3 giờ) and two days of rằm read. */
    fun loiNight(): Int {
        val total = Store.hourSeconds.sum()
        return if (total > 0) ((Store.hourSeconds[0] + Store.hourSeconds[1] + Store.hourSeconds[2]) * 100 / total).toInt() else 0
    }

    fun loiRam(): Int = Store.daySeconds.count { (d, s) ->
        s >= 600 && LocalDate.ofEpochDay(d).let { Lunar.fromSolar(it.dayOfMonth, it.monthValue, it.year).day == 15 }
    }

    /** Băng: the most days of 30 minutes in one season from Đông chí (22/12) to Đại hàn (20/1). */
    fun bangDays(): Int = Store.daySeconds.keys.filter { Store.minutes(it) >= 30 }.mapNotNull { d ->
        val t = LocalDate.ofEpochDay(d)
        when {
            t.monthValue == 12 && t.dayOfMonth >= 22 -> t.year
            t.monthValue == 1 && t.dayOfMonth <= 20 -> t.year - 1
            else -> null
        }
    }.groupingBy { it }.eachCount().values.maxOrNull() ?: 0

    /** Phong: the most books read through within seven days. */
    fun phongBooks(): Int {
        val days = Store.books.values.filter { it.finishDay >= 0 }.map { it.finishDay }.sorted()
        var best = 0
        for ((i, d) in days.withIndex()) best = maxOf(best, days.drop(i).count { it - d < 7 })
        return best
    }

    fun diMet(): Int = when {
        loiNight() >= 10 && loiRam() >= 2 -> PhapMon.LOI
        bangDays() >= 20 -> PhapMon.BANG
        phongBooks() >= 3 -> PhapMon.PHONG
        else -> -1
    }

    fun awaken() {
        roots.clear()
        roots.addAll(rootsFrom(hanhSeconds()))
        di = diMet()
        awakenDay = today()
        Store.hourSeconds.copyInto(hoursAt)
        Store.note("Linh căn thức tỉnh: ${rootName()}")
        if (gifted and 1 == 0) {
            gifted = gifted or 1
            val chu = roots[0]
            val gift = PhapMon.FIRST_CONG[chu]
            if (gainCong(gift)) {
                // The starter has no hành: the gift runs instead.
                if (active == PhapMon.DAN_KHI) active = gift
                Store.note("Sư phụ truyền ${CONG_PHAP[gift].name}, hợp linh căn ${PhapMon.ATTR[chu]}")
            }
            val t = PhapMon.FIRST_THUAT[chu]
            if (gainThuat(t)) {
                thuat[t]?.learned = true
                if (t !in thuatOn && thuatOn.size < thuatSlots()) thuatOn.add(t)
            }
        }
        Store.touch()
    }

    fun baseName(): String = when (roots.size) {
        0 -> "Chưa thức tỉnh"
        1 -> "Thiên linh căn ${PhapMon.ATTR[roots[0]]}"
        5 -> "Ngũ hành tạp linh căn"
        else -> roots.joinToString(" ") { PhapMon.ATTR[it] } + " " + PhapMon.ROOT_NAMES[roots.size]
    }

    fun rootName(): String = if (di >= 0) "${PhapMon.ATTR[di]} linh căn (dị) · ${baseName()}" else baseName()
    fun shortName(): String = if (!awakened()) "chưa thức tỉnh" else if (di >= 0) "${PhapMon.ATTR[di]} linh căn" else baseName()

    fun rootBonus(): Int = if (!awakened()) 0 else PhapMon.ROOT_BONUS[roots.size] + if (di >= 0) PhapMon.DI_BONUS else 0

    /** +20 when an attribute is one of the roots, −20 when it and the chủ hành khắc each other. */
    fun affinity(attr: Int): Int {
        if (attr == PhapMon.KIEM && TongMon.perk(TongMon.KIEM_LU)) return 20
        if (!awakened() || attr < 0) return 0
        if (attr in roots || attr == di) return 20
        if (roots.size == 5) return 0
        val chu = roots[0]
        return if (PhapMon.khac(chu, attr) || PhapMon.khac(attr, chu)) -20 else 0
    }

    fun washHours() = hanhSeconds(hoursAt).sum() / 3600
    fun canWash() = awakened() && !washed && washHours() >= WASH_HOURS && Store.linhThach >= WASH_PRICE

    /** Tẩy linh: once (again with Tẩy Linh Đan, `free`), the roots from the hours read since the awakening. */
    fun wash(free: Boolean = false): Boolean {
        if (!awakened() || washHours() < WASH_HOURS) return false
        if (!free && (washed || Store.linhThach < WASH_PRICE)) return false
        if (!free) Store.linhThach -= WASH_PRICE
        val before = rootName()
        roots.clear()
        roots.addAll(rootsFrom(hanhSeconds(hoursAt)))
        di = diMet()
        washed = true
        Store.note("Tẩy linh: $before thành ${rootName()}")
        Store.touch()
        return true
    }

    // --- Công pháp -----------------------------------------------------------------------------------

    fun activeDef(): PhapMon.CongPhap? = if (active in cong) CONG_PHAP.getOrNull(active) else null
    fun phuDef(): PhapMon.CongPhap? = if (phu in cong && phu != active && phuOpen()) CONG_PHAP.getOrNull(phu) else null

    /** Is đặc hiệu `slot` (0, 1, 2) of công pháp `id` open: tầng 1, 5, 9. */
    fun slotOpen(id: Int, slot: Int) = id in cong && tang(id) >= intArrayOf(1, 5, 9)[slot] && !(slot >= 1 && crippled(id))

    /**
     * The level of a kind of đặc hiệu: one for each open đặc hiệu of the công pháp running, two for
     * its tuyệt học, one for the first đặc hiệu of the phụ tu.
     */
    /** The level of an effect: the công pháp's, the tâm pháp's and the bản mệnh thần thông's together. */
    fun lv(e: Int): Int = (congLv(e) + tamLv(e) + banMenhLv(e)).let { if (PhapMon.isSwitch(e)) minOf(1, it) else it }

    /** The level the công pháp running (and the phụ tu) give an effect. */
    fun congLv(e: Int): Int {
        if (e == PhapMon.E_NONE) return 0
        var n = 0
        activeDef()?.let { d ->
            if (d.effect == e && slotOpen(active, 0)) n++
            if (d.e2 == e && slotOpen(active, 1)) n++
            if (d.e3 == e && slotOpen(active, 2)) n += 2
        }
        if (phuDef()?.effect == e) n++
        return if (PhapMon.isSwitch(e)) minOf(n, 1) else n
    }

    fun effect(e: Int) = lv(e) > 0
    /** The khuyết điểm of the công pháp running. */
    fun flaw(f: Int) = f != PhapMon.F_NONE && activeDef()?.flaw == f

    /** Everything the công pháp give now, kind by kind: (text, level). */
    fun effectsNow(): List<Pair<Int, Int>> = (1..PhapMon.E_BI_CANH).mapNotNull { e -> congLv(e).takeIf { it > 0 }?.let { e to it } }

    fun gainCong(id: Int): Boolean {
        if (id !in CONG_PHAP.indices || id in cong) return false
        cong[id] = Cong()
        if (active < 0) active = id
        Store.touch()
        return true
    }

    fun run(id: Int): Boolean {
        if (id !in cong || id == active) return false
        if (phu == id) phu = -1
        active = id
        Store.note("Chuyển sang vận ${CONG_PHAP[id].name}")
        inherit(id)
        Store.touch()
        return true
    }

    // --- Chuyển tu: a line carries on -------------------------------------------------------------------

    /** The công pháp of the same line, lower phẩm, already viên mãn, that `id` would take words from. */
    fun inheritFrom(id: Int): Int? {
        if (id !in cong || id in inherited || (cong[id]?.words ?: 0) > 0) return null
        val d = CONG_PHAP[id]
        return cong.keys.filter { CONG_PHAP[it].mach == d.mach && CONG_PHAP[it].pham < d.pham && complete(it) }
            .maxByOrNull { CONG_PHAP[it].pham }
    }

    /** The words carried: half the old one's, up to the new one's tầng 3. */
    fun inheritWords(id: Int): Long = inheritFrom(id)?.let { from ->
        minOf((cong[from]?.words ?: 0) * PhapMon.INHERIT_PERCENT / 100, (cong[id]?.opened ?: 3) * need(id))
    } ?: 0

    fun inherit(id: Int): Boolean {
        val from = inheritFrom(id) ?: return false
        val n = inheritWords(id)
        cong[id]?.words = n
        inherited.add(id)
        Store.note("Chuyển tu ${CONG_PHAP[from].name} sang ${CONG_PHAP[id].name}: kế thừa ${com.exacta.reader.game.Realm.van(n)} vạn chữ, tới tầng ${tang(id)}")
        Store.touch()
        return true
    }

    // --- Phụ tu ----------------------------------------------------------------------------------------

    /** From Kim Đan, a second công pháp of another line. */
    fun phuOpen() = Store.realmCap >= 2
    fun canPhu(id: Int) = phuOpen() && id in cong && id != active && activeDef()?.mach != CONG_PHAP[id].mach

    fun setPhu(id: Int): Boolean {
        if (!canPhu(id)) return false
        phu = id
        Store.note("Phụ tu ${CONG_PHAP[id].name}: ${PhapMon.effectText(CONG_PHAP[id].effect)}")
        Store.touch()
        return true
    }

    fun stopPhu() {
        phu = -1
        Store.touch()
    }

    // --- Lối vận công ------------------------------------------------------------------------------------

    /** Is the reading now in the way of line `mach`. */
    fun inWay(mach: Int, book: Book?, hour: Int = java.time.LocalTime.now().hour): Boolean {
        val t = today()
        return when (mach) {
            in 0..4 -> PhapMon.hanhOfHour(hour) == mach
            PhapMon.LOI -> hour in 0..2 || LocalDate.ofEpochDay(t).let { Lunar.fromSolar(it.dayOfMonth, it.monthValue, it.year).day == 15 }
            PhapMon.BANG -> (TuLuyen.dayJumps[t] ?: 0) == 0 && Store.sessionNow >= 20 * 60
            PhapMon.PHONG -> book != null && book.firstDay >= t - 6
            PhapMon.KIEM -> book != null && book.maxProgress >= 0.5
            PhapMon.THE -> Store.sessionNow >= 45 * 60
            PhapMon.THAN -> book != null && book.author.isNotBlank() &&
                Store.books.values.none { it.finishDay >= 0 && it.author.trim().equals(book.author.trim(), ignoreCase = true) }
            else -> {
                val hanh = HashSet<Int>()
                for (d in t - 6..t) NhatKy.days[d]?.hours?.forEachIndexed { h, s -> if (s >= 300) hanh.add(PhapMon.hanhOfHour(h)) }
                hanh.size >= 4
            }
        }
    }

    // --- Xung quan: tầng 4, 7, 9 break through in one sitting ---------------------------------------------

    fun canXung(id: Int = active) = id == active && xung == 0 && waiting(id) && ngo >= openCost(id)

    /** Ngộ tính is set down; the next sitting must hold the minutes of the tầng. */
    fun startXung(): Boolean {
        if (!canXung()) return false
        val lock = nextLock() ?: return false
        xungCost = openCost()
        ngo -= xungCost
        xung = lock
        Store.note("Xung quan tầng $lock của ${CONG_PHAP[active].name}: buổi đọc tới phải liền ${PhapMon.xungMinutes(lock)} phút")
        Store.touch()
        return true
    }

    fun cancelXung() {
        if (xung == 0) return
        ngo += xungCost
        xung = 0
        xungCost = 0
        Store.touch()
    }

    /** Seconds of the sitting so far: long enough, the tầng opens. */
    fun onSeconds(session: Long) {
        if (xung == 0 || session < PhapMon.xungMinutes(xung) * 60L) return
        val lock = xung
        xung = 0
        xungCost = 0
        val c = cong[active]
        if (c != null && nextLock() == lock) {
            c.opened = if (lock == 9) 9 else PhapMon.LOCKS[PhapMon.LOCKS.indexOf(lock) + 1] - 1
            lastXung = "Xung quan thành: ${CONG_PHAP[active].name} lên tầng $lock"
            Store.note(lastXung)
        }
        Store.touch()
    }

    /** The sitting ended short: khí nghịch, tâm ma +5; the ngộ tính comes back for another try. */
    fun onSessionEnd(seconds: Long) {
        if (xung == 0 || seconds < 60) return
        lastXung = "Xung quan tầng $xung đứt ở ${seconds / 60} phút: khí nghịch, tâm ma +5"
        ngo += xungCost
        xung = 0
        xungCost = 0
        TuLuyen.stir(5)
        Store.note(lastXung)
        Store.touch()
    }

    private fun need(id: Int) = PhapMon.TANG_WORDS[CONG_PHAP[id].pham]
    fun tang(id: Int = active): Int {
        val c = cong[id] ?: return 0
        return minOf(c.opened, 1 + (c.words / need(id)).toInt()).coerceIn(1, 9)
    }

    /** The tầng that waits for ngộ tính next (4, 7, 9), or null when all are open. */
    fun nextLock(id: Int = active): Int? = cong[id]?.let { c -> PhapMon.LOCKS.firstOrNull { it > c.opened } }
    fun waiting(id: Int = active): Boolean = cong[id]?.let { it.words >= it.opened * need(id) && nextLock(id) != null } ?: false
    fun complete(id: Int = active): Boolean = cong[id]?.let { it.opened >= 9 && it.words >= 9 * need(id) } ?: false
    fun openCost(id: Int = active): Int = nextLock(id)?.let { PhapMon.ngoCost(CONG_PHAP[id].pham, it) + (if (id == active && flaw(PhapMon.F_NGO)) 1 else 0) } ?: 0

    /** Words to the next tầng (0 when waiting for ngộ tính or viên mãn), and the words of a tầng. */
    fun toNext(id: Int = active): Long {
        val c = cong[id] ?: return 0
        val n = need(id)
        val goal = minOf(c.opened.toLong(), c.words / n + 1) * n
        return (goal - c.words).coerceAtLeast(0)
    }

    /** Lĩnh ngộ: spends ngộ tính to open tầng 4, 7 or 9. */
    fun open(id: Int = active): Boolean {
        val c = cong[id] ?: return false
        val lock = nextLock(id) ?: return false
        val cost = openCost(id)
        if (!waiting(id) || ngo < cost) return false
        ngo -= cost
        c.opened = if (lock == 9) 9 else PhapMon.LOCKS[PhapMon.LOCKS.indexOf(lock) + 1] - 1
        Store.note("Lĩnh ngộ ${CONG_PHAP[id].name}: lên tầng $lock")
        Store.touch()
        return true
    }

    /** Words read: into the tầng of the công pháp running (more in its way), half into the phụ tu. */
    fun onWords(words: Long, book: Book? = null) {
        study(words)
        phuDef()?.let { _ ->
            cong[phu]?.let { p -> p.words = minOf(p.words + words * PhapMon.PHU_PERCENT / 100, p.opened * need(phu)) }
        }
        val c = cong[active] ?: return
        val before = tang()
        val way = inWay(CONG_PHAP[active].mach, book)
        if (way) wayWords += words
        val n = words * (if (way) PhapMon.WAY_PERCENT else 100) / 100 * (100 + PhapMon.effectValue(PhapMon.E_VAN_CONG, lv(PhapMon.E_VAN_CONG)) + TranPhap.vanCong()) / 100
        c.words = minOf(c.words + n, c.opened * need(active))
        val after = tang()
        val name = CONG_PHAP[active].name
        if (after > before) Store.note("$name lên tầng $after")
        if (waiting()) {
            val key = active * 10 + c.opened
            if (capNoted != key) {
                capNoted = key
                Store.note("$name đủ chữ: lĩnh ngộ tầng ${nextLock()} cần ${openCost()} ngộ tính")
            }
        } else if (complete() && capNoted != active * 10 + 9) {
            capNoted = active * 10 + 9
            Store.note("$name viên mãn")
        }
    }

    fun congBonus(): Int = activeDef()?.let { PhapMon.PHAM_BONUS[it.pham] + tang() - 1 } ?: 0
    fun linhLucBonus(): Int = PhapMon.effectValue(PhapMon.E_LINH_LUC, lv(PhapMon.E_LINH_LUC))
    fun affinityBonus(): Int = activeDef()?.let { affinity(it.attr) } ?: 0

    /** Đạo Tâm Chủng Ma, with a công pháp of Kiếm: tâm ma / 5, 20% at most. */
    fun chungMa(): Int = if (tamWorks(PhapMon.TP_CHUNG_MA) && activeDef()?.attr == PhapMon.KIEM) minOf(20, TuLuyen.tamMa / 5) else 0

    /** Percent of linh lực the cultivator draws from a word (before bình cảnh and wounds). */
    fun speed(): Int = (100 + rootBonus() + congBonus() + affinityBonus() + linhLucBonus() + chungMa() + DongPhu.speedBonus() + DaoLo.speedBonus() + LinhGioi.speedBonus() + DoKiep.speedBonus() + TeLuyen.speedBonus() + TranPhap.speedBonus()).coerceIn(50, 250)

    fun speedParts(): List<Pair<String, Int>> {
        val list = ArrayList<Pair<String, Int>>()
        if (awakened()) list.add("Linh căn: ${shortName()}" to rootBonus())
        activeDef()?.let { d ->
            list.add("${PhapMon.PHAM[d.pham]} phẩm, tầng ${tang()}" to congBonus())
            val a = affinityBonus()
            if (a > 0) list.add("Hợp linh căn (${PhapMon.attrName(d.attr)})" to a)
            if (a < 0) list.add("Khắc linh căn (${PhapMon.attrName(d.attr)})" to a)
        }
        if (linhLucBonus() > 0) list.add((if (tamLv(PhapMon.E_LINH_LUC) > 0) "Đặc hiệu công pháp, tâm pháp" else "Đặc hiệu công pháp") to linhLucBonus())
        if (chungMa() > 0) list.add("Đạo Tâm Chủng Ma" to chungMa())
        if (DongPhu.speedBonus() > 0) list.add("Linh mạch ${DongPhu.giaiName()}" to DongPhu.speedBonus())
        list.addAll(DaoLo.speedParts())
        list.addAll(LinhGioi.speedParts())
        if (DoKiep.speedBonus() > 0) list.add("Ký ức tiền kiếp" to DoKiep.speedBonus())
        if (TeLuyen.speedBonus() > 0) list.add("Pháp bảo cấm chế tầng ${TeLuyen.bestLevel()}" to TeLuyen.speedBonus())
        if (TranPhap.speedBonus() > 0) list.add("Trận pháp" to TranPhap.speedBonus())
        return list
    }

    /** A gate asks for a công pháp that carries that far. */
    fun canBreakWith(gate: Int) = (activeDef()?.pham ?: -1) >= PhapMon.phamFor(gate)
    fun breakPart(): Int = activeDef()?.let { it.pham * 2 + if (tang() >= 9) 3 else 0 } ?: 0

    // --- Ngộ tính ------------------------------------------------------------------------------------

    fun gainNgo(n: Int, why: String) {
        if (n <= 0) return
        ngo += n
        ngoEarned += n
        Store.note("$why: ngộ tính +$n")
        Store.touch()
    }

    fun onBookTang(book: Book, tang: Int) {
        if (started && (tang == 4 || tang == 7)) gainNgo(1, "Tham ngộ «${book.title}» tầng $tang")
    }

    fun onFinish(book: Book) {
        if (!started) return
        gainNgo(1 + (if (book.words >= TuLuyen.NGO_DAO_WORDS) 1 else 0) + lv(PhapMon.E_NGO_TINH) + TranPhap.ngo(), "Đọc trọn «${book.title}»")
        // 3.2: the tâm pháp that turn with a book read through.
        for (id in tamOn.toList()) if (TAM_PHAP[id].way == PhapMon.W_FINISH) turn(id, PhapMon.FINISH_TURNS)
    }

    // --- Tâm pháp ------------------------------------------------------------------------------------

    /** Ô tâm pháp: one at Trúc Cơ, two at Nguyên Anh, three at Luyện Hư. */
    fun tamSlots() = when {
        Store.realmCap >= 5 -> 3
        Store.realmCap >= 3 -> 2
        Store.realmCap >= 1 -> 1
        else -> 0
    }

    fun turns(id: Int) = tam[id] ?: 0
    /** Cảnh: 0 chưa nhập môn, 1 Nhập môn … 5 Hóa cảnh. */
    fun canh(id: Int) = PhapMon.canh(turns(id))
    /** The 2.1 levels: 1 from nhập môn, 2 from đại thành. */
    fun tamLevel(id: Int): Int = canh(id).let { if (it >= 3) 2 else if (it >= 1) 1 else 0 }
    /** A tâm pháp in a slot and past nhập môn: its trait is on. */
    fun tamWorks(id: Int) = id in tamOn && tamLevel(id) >= 1

    /** The level the tâm pháp in their slots give an effect. */
    fun tamLv(e: Int): Int {
        if (e == PhapMon.E_NONE) return 0
        var n = 0
        for (id in tamOn) {
            val t = TAM_PHAP[id]
            val c = canh(id)
            if (t.e1 == e) n += PhapMon.E1_LV[c]
            if (t.e2 == e) n += PhapMon.E2_LV[c]
        }
        return n
    }

    /** Hóa cảnh: tâm cảnh one tier higher. */
    fun hoaCanhStep() = if (tamOn.any { canh(it) >= 5 }) 1 else 0

    /** Why `id` cannot go into a slot beside the ones running, or null. */
    fun clashWith(id: Int): Int? = tamOn.firstOrNull { it != id && PhapMon.clash(id, it) }

    fun gainTam(id: Int): Boolean {
        if (id !in TAM_PHAP.indices || id in tam) return false
        tam[id] = 0
        if (tamOn.size < tamSlots() && clashWith(id) == null) tamOn.add(id)
        Store.touch()
        return true
    }

    fun runTam(id: Int): Boolean {
        if (id !in tam || id in tamOn || tamOn.size >= tamSlots() || clashWith(id) != null) return false
        tamOn.add(id)
        Store.touch()
        return true
    }

    fun stopTam(id: Int): Boolean {
        val ok = tamOn.remove(id)
        if (ok) Store.touch()
        return ok
    }

    fun maCount() = tamOn.count { TAM_PHAP[it].phai == PhapMon.PHAI_MA }
    /** Đạo gia beside Phật môn: both turn 20% more. */
    fun daoPhat() = tamOn.any { TAM_PHAP[it].phai == PhapMon.PHAI_DAO } && tamOn.any { TAM_PHAP[it].phai == PhapMon.PHAI_PHAT }

    /** Does the reading now keep to the way of `id`? */
    fun inTamWay(id: Int, book: Book?, hour: Int = java.time.LocalTime.now().hour): Boolean {
        val t = today()
        return when (TAM_PHAP[id].way) {
            PhapMon.W_NO_JUMP -> (TuLuyen.dayJumps[t] ?: 0) == 0
            PhapMon.W_LONG45 -> Store.sessionNow >= 45 * 60
            PhapMon.W_LONG60 -> Store.sessionNow >= 60 * 60
            PhapMon.W_LONG90 -> Store.sessionNow >= 90 * 60
            PhapMon.W_STREAK -> Store.streak() >= 7
            PhapMon.W_DAWN -> hour in 4..7
            PhapMon.W_NIGHT -> hour >= 21 || hour <= 2
            PhapMon.W_ONE_BOOK -> (NhatKy.days[t]?.books?.size ?: 0) <= 1
            PhapMon.W_THIRD -> tamDay == t && tamSittings >= 3
            PhapMon.W_NEW_AUTHOR -> inWay(PhapMon.THAN, book)
            PhapMon.W_REREAD -> book != null && (book.finishDay >= 0 || (book.backDay >= 0 && t - book.backDay < 7))
            PhapMon.W_TAM_MA -> TuLuyen.tamMa >= 30
            PhapMon.W_RESTLESS -> (TuLuyen.dayJumps[t] ?: 0) > 0
            PhapMon.W_DEEP -> book != null && book.maxProgress >= 0.5
            else -> false
        }
    }

    /** `n` vận chuyển for `id`: Minh Tâm Tự doubles them, Đạo gia beside Phật môn adds a fifth. */
    private fun turn(id: Int, n: Int) {
        val t = TAM_PHAP[id]
        var whole = n * (if (TongMon.perk(TongMon.MINH_TAM)) 2 else 1)
        if (daoPhat() && (t.phai == PhapMon.PHAI_DAO || t.phai == PhapMon.PHAI_PHAT)) {
            val f = (tamFrac[id] ?: 0) + whole * PhapMon.DAO_PHAT_PERCENT / 10
            whole += f / 10
            tamFrac[id] = f % 10
        }
        val before = turns(id)
        val after = before + whole
        tam[id] = after
        val c0 = PhapMon.canh(before)
        val c1 = PhapMon.canh(after)
        if (c1 > c0) Store.note("${t.name} ${PhapMon.CANH[c1].lowercase()}: " + when (c1) {
            1 -> t.text.replaceFirstChar { it.lowercase() }
            3 -> "mở ${PhapMon.effectText(t.e2).replaceFirstChar { it.lowercase() }}; tâm cảnh +5"
            5 -> "tâm cảnh lên một bậc"
            else -> PhapMon.effectText(t.e1, PhapMon.E1_LV[c1]).replaceFirstChar { it.lowercase() }
        })
    }

    /**
     * The sitting has gone from `before` to `session` seconds. At 30 minutes every tâm pháp in a slot
     * turns once, and once more if the reading keeps to its way; the ways of a long sitting are
     * judged at 45, 60, 90 minutes.
     */
    fun onTamSeconds(book: Book?, before: Long, session: Long) {
        if (tamOn.isEmpty()) return
        val t = today()
        var moved = false
        if (before < 30 * 60 && session >= 30 * 60) {
            if (tamDay != t) { tamDay = t; tamSittings = 0 }
            tamSittings++
            for (id in tamOn.toList()) {
                val w = TAM_PHAP[id].way
                turn(id, 1 + if (PhapMon.wayMinute(w) == 30 && inTamWay(id, book)) 1 else 0)
            }
            moved = true
        }
        for (m in intArrayOf(45, 60, 90)) {
            if (before < m * 60 && session >= m * 60) {
                for (id in tamOn.toList()) if (PhapMon.wayMinute(TAM_PHAP[id].way) == m) { turn(id, 1); moved = true }
            }
        }
        if (moved) Store.touch()
    }

    /** The 2.1 name: a sitting of 30 minutes, the reading out of every way. */
    fun onSitting30() = onTamSeconds(null, 0, 30 * 60L)

    fun tamCanhBonus() = 5 * tamOn.count { canh(it) >= 3 } + TranPhap.tamCanh() + PhapMon.effectValue(PhapMon.E_TAM_CANH, lv(PhapMon.E_TAM_CANH)) - (if (flaw(PhapMon.F_TAM_CANH)) 5 else 0)
    fun canCoBonus() = TranPhap.canCo() + PhapMon.effectValue(PhapMon.E_CAN_CO, lv(PhapMon.E_CAN_CO)) + (if (tamWorks(PhapMon.TP_DUONG_KHI)) 5 else 0) - (if (flaw(PhapMon.F_CAN_CO)) 5 else 0)

    /** Percent of a rise of tâm ma that comes through. */
    fun stirPercent() = (if (tamWorks(PhapMon.TP_THANH_TAM)) 50 else 100) * (100 - PhapMon.effectValue(PhapMon.E_TINH_TAM, lv(PhapMon.E_TINH_TAM))).coerceAtLeast(40) / 100 *
        (if (flaw(PhapMon.F_TAM_MA)) 120 else 100) / 100 *
        (if (TongMon.perk(TongMon.MINH_TAM)) 80 else 100) / 100 *
        (100 + PhapMon.MA_STIR * maCount()) / 100

    /** Băng Tâm Quyết: the first broken streak of a month stirs nothing. */
    fun calmBreak(day: Long): Boolean {
        if (!tamWorks(PhapMon.TP_BANG_TAM)) return false
        val d = LocalDate.ofEpochDay(day)
        val month = d.year * 12 + d.monthValue
        if (bangTamMonth == month) return false
        bangTamMonth = month
        Store.note("Băng Tâm Quyết giữ lòng: đứt chuỗi không sinh tâm ma")
        return true
    }

    fun tauHoaAt() = if (tamWorks(PhapMon.TP_DINH_HON)) 85 else 70

    // --- Thuật pháp (3.3) ----------------------------------------------------------------------------

    /** Ô mang thuật: two at Luyện Khí, one more each great realm, six at most. */
    fun thuatSlots() = 2 + Store.realmCap.coerceIn(0, 4)

    fun gainThuat(id: Int): Boolean {
        if (id !in THUAT_PHAP.indices || id in thuat) return false
        thuat[id] = Thuat()
        if (learnCost(id) == 0 && canLearnRealm(id)) {
            thuat[id]?.learned = true
            if (thuatOn.size < thuatSlots()) thuatOn.add(id)
        }
        Store.touch()
        return true
    }

    fun canLearnRealm(id: Int) = THUAT_PHAP[id].kind != PhapMon.K_THAN_THONG || Store.realmCap >= 3

    /** Ngộ tính to learn a thuật pháp: none for a root's hành, more for one the chủ hành khắc. */
    fun learnCost(id: Int): Int {
        val a = THUAT_PHAP[id].attr
        if (!awakened()) return 1
        if (a in roots || a == di) return 0
        if (roots.size == 5) return if (a in 0..4) 0 else 1
        val single = roots.size == 1
        return if (affinity(a) < 0) (if (single) 3 else 2) else (if (single) 2 else 1)
    }

    fun canLearn(id: Int) = thuat[id]?.learned == false && canLearnRealm(id) && ngo >= learnCost(id)

    fun learn(id: Int): Boolean {
        if (!canLearn(id)) return false
        ngo -= learnCost(id)
        thuat[id]?.learned = true
        if (id !in thuatOn && thuatOn.size < thuatSlots()) thuatOn.add(id)
        Store.note("Học được ${THUAT_PHAP[id].name}")
        Store.touch()
        return true
    }

    fun equip(id: Int): Boolean {
        if (thuat[id]?.learned != true || id in thuatOn || thuatOn.size >= thuatSlots()) return false
        thuatOn.add(id)
        Store.touch()
        return true
    }

    fun unequip(id: Int): Boolean {
        val ok = thuatOn.remove(id)
        if (ok) Store.touch()
        return ok
    }

    fun on(id: Int) = id in thuatOn && thuat[id]?.learned == true
    fun points(id: Int) = thuat[id]?.let { PhapMon.USE_POINTS * it.uses + it.sits } ?: 0
    fun mastery(id: Int) = PhapMon.mastery(points(id))
    fun cool(id: Int) = thuat[id]?.cool ?: 0
    /** Carried, and not waiting out its hồi chiêu. */
    fun ready(id: Int) = on(id) && cool(id) <= 0

    /** Hồi chiêu in days of reading: shorter with thuần thục, and with Phong Hành Vạn Lý. */
    fun cd(id: Int): Int {
        val base = THUAT_PHAP[id].cd
        if (base <= 0) return 0
        return (base - PhapMon.cdLess(mastery(id)) - (if (hop(PhapMon.PHONG)) 1 else 0)).coerceAtLeast(1)
    }

    /** A use: thuần thục, and the hồi chiêu begins when `cast`. */
    private fun used(id: Int, cast: Boolean = true) {
        val t = thuat[id] ?: return
        val before = mastery(id)
        t.uses++
        if (cast && THUAT_PHAP[id].cd > 0) t.cool = cd(id)
        val after = mastery(id)
        if (after > before) Store.note("${THUAT_PHAP[id].name} ${PhapMon.MASTERY[after].lowercase()}")
        Store.touch()
    }

    /**
     * The sitting has gone from `before` to `session` seconds. The first ten minutes of a day make it
     * a day of reading: every hồi chiêu shortens by one. Each sitting of 30 minutes trains the thuật
     * carried.
     */
    fun onThuatSeconds(before: Long, session: Long) {
        val t = today()
        if (before < 10 * 60 && session >= 10 * 60 && thuatDay != t) {
            thuatDay = t
            for (s in thuat.values) if (s.cool > 0) s.cool--
            Store.touch()
        }
        if (before < 30 * 60 && session >= 30 * 60) {
            for (id in thuatOn) {
                val s = thuat[id] ?: continue
                if (!s.learned) continue
                val m0 = mastery(id)
                s.sits++
                val m1 = mastery(id)
                if (m1 > m0) Store.note("${THUAT_PHAP[id].name} ${PhapMon.MASTERY[m1].lowercase()}")
            }
            if (thuatOn.isNotEmpty()) Store.touch()
        }
    }

    // Hợp kích and bản mệnh thần thông.

    fun hopActive(): List<PhapMon.HopKich> = PhapMon.HOP_KICH.filter { h -> thuatOn.count { on(it) && THUAT_PHAP[it].attr == h.attr } >= 2 }
    fun hop(attr: Int) = hopActive().any { it.attr == attr }

    /** The bản mệnh thần thông of the Kim Đan formed. */
    fun banMenh(): PhapMon.BanMenh? = if (Store.realmCap >= 2 && DaoLo.dan in PhapMon.BAN_MENH.indices) PhapMon.BAN_MENH[DaoLo.dan] else null
    fun banMenhLv(e: Int) = banMenh()?.takeIf { it.effect == e }?.level ?: 0

    private fun readyKind(kind: Int) = thuatOn.filter { ready(it) && THUAT_PHAP[it].kind == kind }
    private fun onKind(kind: Int) = thuatOn.filter { on(it) && THUAT_PHAP[it].kind == kind }
    /** The best of a kind ready to cast: the most practised. */
    private fun best(kind: Int) = readyKind(kind).maxByOrNull { points(it) }
    fun hasReady(kind: Int) = readyKind(kind).isNotEmpty()
    fun bestMastery(kind: Int) = best(kind)?.let { mastery(it) } ?: 0

    /** The weekly bí cảnh is cleared: each công kích carried brings back linh thạch (no hồi chiêu). */
    fun onBiCanh(): Long {
        var lt = 0L
        for (id in onKind(PhapMon.K_CONG_KICH)) {
            lt += 10L * (mastery(id) + 1)
            used(id, cast = false)
        }
        return lt
    }

    /** Thi triển a công kích in the bí cảnh: (the thuật, whether it lands, the linh thạch), or null. */
    fun castAttack(roll: (Int) -> Boolean): Triple<Int, Boolean, Long>? {
        val id = best(PhapMon.K_CONG_KICH) ?: return null
        val m = mastery(id)
        val hit = hop(PhapMon.KIM) || roll(PhapMon.hitChance(m))
        used(id)
        val lt = if (hit) 10L * (m + 1) * (if (hop(PhapMon.HOA)) 2 else 1) else 0L
        return Triple(id, hit, lt)
    }

    /** Thi triển a khống chế: the thuật and its thuần thục, or null. */
    fun castControl(): Pair<Int, Int>? {
        val id = best(PhapMon.K_KHONG_CHE) ?: return null
        val m = mastery(id)
        used(id)
        return id to m
    }

    /** A độn thuật carried, cast to slip away in the bí cảnh. */
    fun castEscape(): Int? {
        val id = best(PhapMon.K_DON) ?: return null
        used(id)
        return id
    }

    private fun castOne(id: Int): Boolean {
        if (!ready(id)) return false
        used(id)
        return true
    }

    fun castThunder() = castOne(PhapMon.T_LOI_PHAT)
    fun castHeal() = castOne(PhapMon.T_HOI_XUAN)
    fun castCalm() = castOne(PhapMon.T_THANH_TAM_CHU)
    fun castEye() = castOne(PhapMon.T_THIEN_NHAN)

    /** Days missed that a kiếp under way forgives, by the phòng ngự thuật pháp (and Vạn Mộc Hồi Xuân). */
    fun trialSpare(): Int = readyKind(PhapMon.K_PHONG_NGU).map { if (mastery(it) >= 3) 2 else 1 }.sum() + (if (hop(PhapMon.MOC)) 1 else 0)

    fun onTrialStart() {
        shields = (if (ready(PhapMon.T_THIEN_NHAN)) (if (mastery(PhapMon.T_THIEN_NHAN) >= 3) 2 else 1) else 0) +
            (if (ready(PhapMon.T_THAN_KIEM)) 1 else 0) + (if (hop(PhapMon.THUY)) 1 else 0)
    }

    fun onTrialEnd() {
        readyKind(PhapMon.K_PHONG_NGU).forEach { used(it) }
        shields = 0
    }

    /** A tâm ma attack in the lôi kiếp: true when a thuật pháp blocks it. */
    fun block(): Boolean {
        if (shields <= 0) return false
        shields--
        val id = listOf(PhapMon.T_THIEN_NHAN, PhapMon.T_THAN_KIEM).firstOrNull { on(it) }
        if (id != null) {
            used(id)
            Store.note("${THUAT_PHAP[id].name} chặn được tâm ma")
        } else Store.note("Thủy Kính Lưu Quang chặn được tâm ma")
        return true
    }

    /** Liễm Tức Thuật: a jump past three a day stirs nothing. */
    fun quietJump(): Boolean {
        if (!on(PhapMon.T_LIEM_TUC)) return false
        if (liemTucDay != today()) {
            liemTucDay = today()
            used(PhapMon.T_LIEM_TUC, cast = false)
        }
        return true
    }

    fun phapTuong(): Int = if (on(PhapMon.T_PHAP_TUONG)) 5 + mastery(PhapMon.T_PHAP_TUONG) else 0
    fun onRoll() {
        if (on(PhapMon.T_PHAP_TUONG)) used(PhapMon.T_PHAP_TUONG, cast = false)
    }

    /** What a failed breakthrough costs once the độn thuật and the công pháp have done their part. */
    class Fall(var kind: Int, var lossDiv: Int, var injuryDays: Int, var restDays: Int, var tamMa: Int, val saved: ArrayList<String> = ArrayList())

    fun soften(kind: Int): Fall {
        val f = Fall(kind, 50, 5, (if (effect(PhapMon.E_HOI_KHI)) 1 else 2) + (if (flaw(PhapMon.F_HOI_KHI)) 1 else 0), 10)
        if (f.kind == TuLuyen.TAU_HOA && ready(PhapMon.T_HUYET_DON)) {
            f.kind = TuLuyen.KINH_MACH
            f.injuryDays = if (mastery(PhapMon.T_HUYET_DON) >= 2) 5 else 8
            used(PhapMon.T_HUYET_DON)
            f.saved.add("Huyết Độn thoát tẩu hỏa")
        }
        if (f.kind != TuLuyen.TAU_HOA && ready(PhapMon.T_THO_DON)) {
            f.lossDiv = 100
            f.injuryDays = minOf(f.injuryDays, if (mastery(PhapMon.T_THO_DON) >= 2) 2 else 3)
            used(PhapMon.T_THO_DON)
            f.saved.add("Thổ Độn")
        }
        if (f.kind != TuLuyen.TAU_HOA && f.lossDiv < 100 && ready(PhapMon.T_THUY_DON)) {
            f.lossDiv = 100
            used(PhapMon.T_THUY_DON)
            f.saved.add("Thủy Độn")
        }
        if (f.kind != TuLuyen.TAU_HOA && f.injuryDays > 2 && ready(PhapMon.T_MOC_DON)) {
            f.injuryDays = 2
            used(PhapMon.T_MOC_DON)
            f.saved.add("Mộc Độn")
        }
        if (hop(PhapMon.THO) && f.lossDiv < 100) {
            f.lossDiv = 100
            f.saved.add("Hậu Thổ Hộ Thân")
        }
        if (ready(PhapMon.T_PHONG_DON)) {
            f.restDays = minOf(f.restDays, if (mastery(PhapMon.T_PHONG_DON) >= 2) 0 else 1)
            used(PhapMon.T_PHONG_DON)
            f.saved.add("Phong Độn")
        }
        if (ready(PhapMon.T_THUAN_DI)) {
            f.restDays = 0
            used(PhapMon.T_THUAN_DI)
            f.saved.add("Thuấn Di")
        }
        if (ready(PhapMon.T_BANG_VAN_LY)) {
            f.tamMa = 0
            used(PhapMon.T_BANG_VAN_LY)
            f.saved.add("Băng Phong Vạn Lý")
        } else if (on(PhapMon.T_THAN_KIEM)) {
            f.tamMa = 5
        }
        return f
    }

    // --- Ngọc giản -----------------------------------------------------------------------------------

    fun owns(kind: Int, id: Int) = when (kind) {
        PhapMon.CONG -> id in cong
        PhapMon.TAM -> id in tam
        PhapMon.DAN -> id in DongPhu.known
        else -> id in thuat
    }

    fun gain(kind: Int, id: Int): Boolean {
        see(kind, id)
        return when (kind) {
            PhapMon.CONG -> gainCong(id)
            PhapMon.TAM -> gainTam(id)
            PhapMon.DAN -> DongPhu.learn(id)
            else -> gainThuat(id)
        }
    }

    /** The phẩm of công pháp the reader looks for now: what the next gate asks, or the one running. */
    fun targetPham() = maxOf(PhapMon.phamFor(TuLuyen.gate()), activeDef()?.pham ?: 0).coerceIn(0, 5)

    // Đồ phổ: everything ever seen.

    fun see(kind: Int, id: Int) {
        val set = when (kind) { PhapMon.CONG -> seenCong; PhapMon.TAM -> seenTam; PhapMon.THUAT -> seenThuat; else -> return }
        if (set.add(id)) Store.touch()
    }

    fun seen(kind: Int, id: Int) = owns(kind, id) || onShelf(kind, id) || when (kind) {
        PhapMon.CONG -> id in seenCong || id in frags
        PhapMon.TAM -> id in seenTam
        else -> id in seenThuat
    }

    // Án tham ngộ: a ngọc giản is learned by reading.

    class Scroll(val kind: Int, val id: Int, val need: Long, var done: Long = 0, val sect: Int = -1)

    fun onShelf(kind: Int, id: Int) = shelf.any { it.kind == kind && it.id == id }

    /** A ngọc giản onto the shelf; the first on the shelf lies on the án and takes the words read. */
    fun toShelf(kind: Int, id: Int, sect: Int = -1): Boolean {
        if (kind == PhapMon.DAN) return DongPhu.learn(id)
        if (owns(kind, id) || onShelf(kind, id)) return false
        see(kind, id)
        shelf.add(Scroll(kind, id, PhapMon.STUDY_WORDS[PhapMon.floorOf(kind, id)], 0, sect))
        Store.touch()
        return true
    }

    fun putOnDesk(i: Int) {
        if (i <= 0 || i >= shelf.size) return
        shelf.add(0, shelf.removeAt(i))
        Store.touch()
    }

    /** Words read go into the ngọc giản on the án; what is left over flows on to the next. */
    fun study(words: Long) {
        var left = words * (if (TongMon.perk(TongMon.VAN_QUYEN)) 125 else 100) / 100
        while (left > 0 && shelf.isNotEmpty()) {
            val s = shelf[0]
            val take = minOf(left, s.need - s.done)
            s.done += take
            left -= take
            if (s.done < s.need) break
            shelf.removeAt(0)
            gain(s.kind, s.id)
            if (s.kind == PhapMon.CONG && s.sect >= 0) cong[s.id]?.sect = s.sect
            Store.note("Tham ngộ xong ngọc giản ${PhapMon.scrollName(s.kind, s.id)}")
        }
        if (words > 0 && shelf.isNotEmpty()) Store.touch()
    }

    // Phản tông thì phế công.

    /** A công pháp from the kinh các of a sect betrayed: its second đặc hiệu and tuyệt học are sealed. */
    fun crippled(id: Int) = cong[id]?.sect?.let { it >= 0 && it in TongMon.betrayed } == true

    // Tàn quyển.

    /** A piece of a Linh or Tiên công pháp: the line of what came of it. */
    private fun fragment(id: Int): String {
        val n = (frags[id] ?: 0) + 1
        val need = PhapMon.fragNeed(id)
        see(PhapMon.CONG, id)
        return if (n >= need) {
            frags.remove(id)
            toShelf(PhapMon.CONG, id)
            "đủ $need tàn quyển ${CONG_PHAP[id].name}, ghép thành ngọc giản, đặt lên giá tham ngộ"
        } else {
            frags[id] = n
            Store.touch()
            "tàn quyển ${CONG_PHAP[id].name} ($n/$need)"
        }
    }

    /** A ngọc giản found (kỳ ngộ, bí cảnh): its line; one already owned turns into ngộ tính. */
    fun dropScroll(rng: Random = Random.Default): String {
        val r = rng.nextInt(100)
        val target = PhapMon.phamFor(TuLuyen.gate())
        val pick: Pair<Int, Int>? = when {
            r < 45 -> {
                val p = when (rng.nextInt(10)) {
                    in 0..4 -> target
                    in 5..7 -> target - 1
                    else -> target + 1
                }.coerceIn(0, 5)
                val base = (0 until PhapMon.BASE_CONG).filter { it !in cong && !onShelf(PhapMon.CONG, it) }
                val list = base.filter { CONG_PHAP[it].pham == p }.ifEmpty { base.filter { CONG_PHAP[it].pham <= target } }
                list.randomOrNull(rng)?.let { PhapMon.CONG to it }
            }
            r < 85 -> THUAT_PHAP.indices.filter { it !in thuat && canLearnRealm(it) }.randomOrNull(rng)?.let { PhapMon.THUAT to it }
            else -> TAM_PHAP.indices.filter { it !in tam }.randomOrNull(rng)?.let { PhapMon.TAM to it }
        }
        scrolls++
        if (pick == null) {
            ngo++
            ngoEarned++
            Store.touch()
            return "ngọc giản đã thuộc, tham ngộ thêm: ngộ tính +1"
        }
        // 3.4: the công pháp of Linh and Tiên phẩm are found in pieces.
        if (pick.first == PhapMon.CONG && CONG_PHAP[pick.second].pham >= 4) return fragment(pick.second)
        gain(pick.first, pick.second)
        val extra = if (pick.first == PhapMon.CONG) " (${PhapMon.PHAM[CONG_PHAP[pick.second].pham]} phẩm)" else ""
        return "ngọc giản ${PhapMon.SCROLL_KINDS[pick.first].lowercase()} ${PhapMon.scrollName(pick.first, pick.second)}$extra"
    }

    // Tàng Kinh Cửu Lâu.

    data class Offer(val floor: Int, val kind: Int, val id: Int)

    /** Floors 1 – 3 are open to all; 4 – 6 ask a sect and cống hiến; 7 – 9 a rank. */
    fun floorOpen(f: Int) = when {
        f <= 3 -> true
        !TongMon.joined() -> false
        f <= 6 -> TongMon.gongsTotal >= PhapMon.FLOOR_GONG_TOTAL[f]
        else -> TongMon.rank() >= f - 5
    }

    fun floorNeed(f: Int) = when {
        f <= 3 -> "mở cho mọi người"
        f <= 6 -> "vào tông, cống hiến từng có ${PhapMon.FLOOR_GONG_TOTAL[f]}"
        else -> "chức ${TongMon.RANKS[f - 5]}"
    }

    /** The price of an offer: linh thạch below, cống hiến above; a ngọc giản kept higher up costs more down here. */
    fun price(o: Offer): Long = when {
        o.kind == PhapMon.DAN -> com.exacta.reader.game.DanDao.recipePrice(o.id)
        o.floor >= 4 -> PhapMon.FLOOR_GONG[o.floor].toLong()
        else -> PhapMon.floorOf(o.kind, o.id).let { f -> if (f <= 3) PhapMon.FLOOR_LT[f] else 60L * f }
    }.let { if (TongMon.perk(TongMon.VAN_QUYEN)) it * 85 / 100 else it }

    fun paysGong(o: Offer) = o.floor >= 4 && o.kind != PhapMon.DAN
    fun afford(o: Offer) = if (paysGong(o)) TongMon.gongs >= price(o) else Store.linhThach >= price(o)
    fun had(o: Offer) = owns(o.kind, o.id) || onShelf(o.kind, o.id)

    /**
     * What the nine floors lend this week. The first offer is always a công pháp of the phẩm the next
     * gate asks: on its own floor when that is open, else on Tam lâu, dearer.
     */
    fun weekOffers(): List<Offer> {
        val w = Store.weekStart(today())
        val key = w * 100 + TongMon.sect + 1
        if (key != offerWeek) {
            offerWeek = key
            offers.clear()
            val r = Random(w * 7919 + 17 + TongMon.sect)
            val target = targetPham()
            val free = (0 until PhapMon.BASE_CONG).filter { it !in cong && !onShelf(PhapMon.CONG, it) }
            val atTarget = free.filter { CONG_PHAP[it].pham == target }
            val first = atTarget.filter { affinity(CONG_PHAP[it].attr) > 0 }.randomOrNull(r) ?: atTarget.randomOrNull(r)
            if (first != null) {
                val f = PhapMon.FLOOR_OF_PHAM[target]
                offers.add(Offer(if (floorOpen(f)) f else minOf(f, 3), PhapMon.CONG, first))
            }
            val lines = if (TongMon.joined()) PhapMon.FAMILY_MACH[TongMon.family()].toSet() else emptySet()
            for (f in 1..9) {
                // A sect's kinh các keeps its own lines first.
                val cong = free.filter { it != first && PhapMon.FLOOR_OF_PHAM[CONG_PHAP[it].pham] == f }.shuffled(r)
                    .sortedBy { if (f >= 4 && CONG_PHAP[it].mach in lines) 0 else 1 }.take(2)
                cong.forEach { offers.add(Offer(f, PhapMon.CONG, it)) }
                TAM_PHAP.indices.filter { it !in tam && PhapMon.TAM_FLOOR[it] == f }.shuffled(r).take(1).forEach { offers.add(Offer(f, PhapMon.TAM, it)) }
                THUAT_PHAP.indices.filter { it !in thuat && PhapMon.THUAT_FLOOR[it] == f }.shuffled(r).take(if (cong.isEmpty()) 2 else 1)
                    .forEach { offers.add(Offer(f, PhapMon.THUAT, it)) }
            }
            if (DongPhu.started) com.exacta.reader.game.DanDao.RECIPES.indices.filter { it !in DongPhu.known }.shuffled(r).take(2)
                .forEachIndexed { i, id -> offers.add(Offer(1 + i, PhapMon.DAN, id)) }
            Store.touch()
        }
        for (o in offers) if (floorOpen(o.floor)) see(o.kind, o.id)
        return offers
    }

    fun floorOffers(f: Int) = weekOffers().filter { it.floor == f }

    /** Borrows an offer: paid, then onto the shelf (a đan phương is learned at once). */
    fun buy(o: Offer): Boolean {
        if (had(o) || !floorOpen(o.floor) || !afford(o)) return false
        val p = price(o)
        if (paysGong(o)) TongMon.gongs -= p.toInt() else Store.linhThach -= p
        toShelf(o.kind, o.id, if (o.floor >= 4) TongMon.sect else -1)
        Store.note("Tàng Kinh ${PhapMon.FLOOR_NAMES[o.floor]}: ${PhapMon.scrollName(o.kind, o.id)} · $p ${if (paysGong(o)) "cống hiến" else "linh thạch"}")
        Store.touch()
        return true
    }

    // Tự sáng công pháp.

    fun canCreate() = Store.realmCap >= 4 && ngo >= PhapMon.CREATE_NGO && custom.size < PhapMon.CREATE_MAX && cong.keys.count { complete(it) } >= 2

    /**
     * From Hóa Thần: two công pháp viên mãn and ngộ tính become one of the cultivator's own. Its phẩm
     * is the lower one's plus one; its line is the first's; it takes a đặc hiệu from each (the first's
     * again as tuyệt học), and has no khuyết điểm.
     */
    fun create(a: Int, b: Int, ea: Int, eb: Int, name: String): Int? {
        val n = name.trim()
        if (!canCreate() || a == b || !complete(a) || !complete(b) || n.isEmpty() || n.length > 28) return null
        val da = CONG_PHAP[a]
        val db = CONG_PHAP[b]
        if (da.effects.none { it.first == ea } || db.effects.none { it.first == eb }) return null
        val d = PhapMon.CongPhap(n, (minOf(da.pham, db.pham) + 1).coerceAtMost(5), da.attr, ea, eb, ea)
        CONG_PHAP.add(d)
        custom.add(d)
        ngo -= PhapMon.CREATE_NGO
        val id = CONG_PHAP.size - 1
        gainCong(id)
        Store.note("Tự sáng công pháp ${d.name}, ${PhapMon.PHAM[d.pham]} phẩm, từ ${da.name} và ${db.name}")
        Store.touch()
        return id
    }

    // --- Each day, and the move from 2.0 -------------------------------------------------------------

    fun check() {
        if (!started) return
        if (!awakened() && awakenReady()) awaken()
        if (Store.realmCap >= 1 && gifted and 2 == 0) {
            gifted = gifted or 2
            if (tam.isEmpty() && gainTam(PhapMon.TP_THANH_TAM)) Store.note("Trúc Cơ mở ô tâm pháp: sư phụ truyền Thanh Tâm Quyết")
            Store.touch()
        }
    }

    /** First run of 2.1: the tầng of the books become ngộ tính, the hours read awaken the linh căn. */
    fun migrate() {
        reset()
        started = true
        gainCong(PhapMon.DAN_KHI)
        for (b in Store.books.values) {
            ngo += (if (b.tang >= 4) 1 else 0) + (if (b.tang >= 7) 1 else 0) +
                if (b.finishDay >= 0) 1 + (if (b.words >= TuLuyen.NGO_DAO_WORDS) 1 else 0) else 0
        }
        ngoEarned = ngo
        if (awakenReady()) awaken()
        // A công pháp that carries to the realm already opened.
        val p = PhapMon.phamFor(Store.realmCap.coerceAtLeast(0))
        if (p >= 1) {
            val chu = roots.firstOrNull() ?: -1
            val list = CONG_PHAP.indices.filter { CONG_PHAP[it].pham == p }
            val id = list.firstOrNull { CONG_PHAP[it].attr == chu } ?: list.first()
            gainCong(id)
            active = id
        }
        if (Store.realmCap >= 1) {
            gifted = gifted or 2
            gainTam(PhapMon.TP_THANH_TAM)
        }
        Store.touch()
    }

    // --- Saving --------------------------------------------------------------------------------------

    fun toJson(): JSONObject = JSONObject().apply {
        put("roots", JSONArray().apply { roots.forEach { put(it) } }); put("di", di); put("awaken", awakenDay)
        put("hoursAt", JSONArray().apply { hoursAt.forEach { put(it) } }); put("washed", washed)
        put("ngo", ngo); put("ngoEarned", ngoEarned); put("active", active); put("capNoted", capNoted)
        put("cong", JSONObject().apply { cong.forEach { (id, c) -> put(id.toString(), JSONArray().put(c.words).put(c.opened).put(c.sect)) } })
        put("tam", JSONObject().apply { tam.forEach { (id, n) -> put(id.toString(), n) } })
        put("tamOn", JSONArray().apply { tamOn.forEach { put(it) } }); put("bangTam", bangTamMonth)
        put("tamFrac", JSONObject().apply { tamFrac.forEach { (id, n) -> put(id.toString(), n) } }); put("tamDay", tamDay); put("tamSittings", tamSittings)
        put("thuat", JSONObject().apply { thuat.forEach { (id, t) -> put(id.toString(), JSONArray().put(t.learned).put(t.uses).put(t.sits).put(t.cool)) } })
        put("thuatOn", JSONArray().apply { thuatOn.forEach { put(it) } }); put("shields", shields); put("liemTuc", liemTucDay)
        put("offerWeek", offerWeek)
        put("offers", JSONArray().apply { offers.forEach { put(JSONArray().put(it.floor).put(it.kind).put(it.id)) } })
        put("shelf", JSONArray().apply { shelf.forEach { put(JSONArray().put(it.kind).put(it.id).put(it.need).put(it.done).put(it.sect)) } })
        put("frags", JSONObject().apply { frags.forEach { (id, n) -> put(id.toString(), n) } })
        put("seen", JSONArray().put(JSONArray(seenCong.toList())).put(JSONArray(seenTam.toList())).put(JSONArray(seenThuat.toList())))
        put("custom", JSONArray().apply { custom.forEach { put(JSONArray().put(it.name).put(it.pham).put(it.attr).put(it.effect).put(it.e2).put(it.e3)) } })
        put("scrolls", scrolls); put("gifted", gifted)
        put("phu", phu); put("xung", xung); put("xungCost", xungCost); put("wayWords", wayWords); put("lastXung", lastXung); put("thuatDay", thuatDay)
        put("inherited", JSONArray().apply { inherited.forEach { put(it) } })
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        o.optJSONArray("roots")?.let { a -> for (i in 0 until a.length()) roots.add(a.getInt(i)) }
        di = o.optInt("di", -1); awakenDay = o.optLong("awaken", -1)
        o.optJSONArray("hoursAt")?.let { a -> for (i in 0 until minOf(a.length(), 24)) hoursAt[i] = a.getLong(i) }
        washed = o.optBoolean("washed"); ngo = o.optInt("ngo"); ngoEarned = o.optInt("ngoEarned")
        active = o.optInt("active", -1); capNoted = o.optInt("capNoted", -1)
        // 3.4: the công pháp of the cultivator's own making, before the ones owned point at them.
        o.optJSONArray("custom")?.let { a ->
            for (i in 0 until a.length()) a.getJSONArray(i).let {
                val d = PhapMon.CongPhap(it.getString(0), it.getInt(1), it.getInt(2), it.getInt(3), it.getInt(4), it.getInt(5))
                custom.add(d)
                CONG_PHAP.add(d)
            }
        }
        o.optJSONObject("cong")?.let { m ->
            for (k in m.keys()) {
                val id = k.toInt()
                if (id !in CONG_PHAP.indices) continue
                val a = m.getJSONArray(k)
                cong[id] = Cong(a.optLong(0), a.optInt(1, 3), a.optInt(2, -1))
            }
        }
        o.optJSONObject("tam")?.let { m -> for (k in m.keys()) k.toInt().takeIf { it in TAM_PHAP.indices }?.let { tam[it] = m.getInt(k) } }
        o.optJSONArray("tamOn")?.let { a -> for (i in 0 until a.length()) a.getInt(i).takeIf { it in tam }?.let { tamOn.add(it) } }
        bangTamMonth = o.optInt("bangTam", -1)
        o.optJSONObject("tamFrac")?.let { m -> for (k in m.keys()) tamFrac[k.toInt()] = m.getInt(k) }
        tamDay = o.optLong("tamDay", -1); tamSittings = o.optInt("tamSittings")
        o.optJSONObject("thuat")?.let { m ->
            for (k in m.keys()) {
                val id = k.toInt()
                if (id !in THUAT_PHAP.indices) continue
                val a = m.getJSONArray(k)
                thuat[id] = Thuat(a.optBoolean(0), a.optInt(1), a.optInt(2), a.optInt(3))
            }
        }
        o.optJSONArray("thuatOn")?.let { a -> for (i in 0 until a.length()) a.getInt(i).takeIf { it in thuat && it !in thuatOn }?.let { thuatOn.add(it) } }
        shields = o.optInt("shields"); liemTucDay = o.optLong("liemTuc", -1); offerWeek = o.optLong("offerWeek", -1)
        // Offers of 3.3 and before had no floor: the week is drawn again.
        o.optJSONArray("offers")?.let { a ->
            for (i in 0 until a.length()) a.getJSONArray(i).let { if (it.length() >= 3) offers.add(Offer(it.getInt(0), it.getInt(1), it.getInt(2))) }
            if (offers.size < a.length()) { offers.clear(); offerWeek = -1 }
        }
        o.optJSONArray("shelf")?.let { a ->
            for (i in 0 until a.length()) a.getJSONArray(i).let { shelf.add(Scroll(it.getInt(0), it.getInt(1), it.getLong(2), it.optLong(3), it.optInt(4, -1))) }
        }
        o.optJSONObject("frags")?.let { m -> for (k in m.keys()) frags[k.toInt()] = m.getInt(k) }
        o.optJSONArray("seen")?.let { a ->
            listOf(seenCong, seenTam, seenThuat).forEachIndexed { i, set -> a.optJSONArray(i)?.let { b -> for (j in 0 until b.length()) set.add(b.getInt(j)) } }
        }
        scrolls = o.optInt("scrolls"); gifted = o.optInt("gifted")
        phu = o.optInt("phu", -1).takeIf { it in cong } ?: -1
        xung = o.optInt("xung"); xungCost = o.optInt("xungCost"); wayWords = o.optLong("wayWords"); lastXung = o.optString("lastXung"); thuatDay = o.optLong("thuatDay", -1)
        o.optJSONArray("inherited")?.let { a -> for (i in 0 until a.length()) inherited.add(a.getInt(i)) }
    }
}
