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
 *  - Tâm pháp: one slot from Trúc Cơ, two from Nguyên Anh; each sitting of 30 minutes is one vận
 *    chuyển, five to nhập môn (the effect turns on), thirty to đại thành.
 *  - Thuật pháp: cards that act by themselves in the events of the game (bí cảnh, kiếp, đột phá
 *    hỏng, tâm ma); each use adds thuần thục.
 *  - Ngọc giản come from kỳ ngộ, the weekly bí cảnh and the Tàng Kinh Các (linh thạch).
 */
object DaoPhap {
    const val WASH_PRICE = 300L
    const val WASH_HOURS = 10L
    const val AWAKEN_SECONDS = 3600L
    const val ROOT_SHARE = 15

    class Cong(var words: Long = 0, var opened: Int = 3)
    class Thuat(var learned: Boolean = false, var uses: Int = 0)

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
    val thuat = LinkedHashMap<Int, Thuat>()
    val thuatOn = ArrayList<Int>()
    var shields = 0                      // tâm ma attacks still blocked in this kiếp
    var liemTucDay = -1L
    var offerWeek = -1L
    val offers = ArrayList<Pair<Int, Int>>()
    var scrolls = 0
    var gifted = 0                       // 1: the awakening's gifts, 2: the tâm pháp of Trúc Cơ

    private fun today() = Store.today()

    fun reset() {
        started = false; roots.clear(); di = -1; awakenDay = -1; hoursAt.fill(0); washed = false
        ngo = 0; ngoEarned = 0; cong.clear(); active = -1; capNoted = -1; tam.clear(); tamOn.clear(); bangTamMonth = -1
        thuat.clear(); thuatOn.clear(); shields = 0; liemTucDay = -1; offerWeek = -1; offers.clear(); scrolls = 0; gifted = 0
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
                if (thuatOn.size < thuatSlots()) thuatOn.add(t)
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
    fun effect(e: Int) = activeDef()?.effect == e

    fun gainCong(id: Int): Boolean {
        if (id !in CONG_PHAP.indices || id in cong) return false
        cong[id] = Cong()
        if (active < 0) active = id
        Store.touch()
        return true
    }

    fun run(id: Int): Boolean {
        if (id !in cong || id == active) return false
        active = id
        Store.note("Chuyển sang vận ${CONG_PHAP[id].name}")
        Store.touch()
        return true
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
    fun openCost(id: Int = active): Int = nextLock(id)?.let { PhapMon.ngoCost(CONG_PHAP[id].pham, it) } ?: 0

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

    fun onWords(words: Long) {
        val c = cong[active] ?: return
        val before = tang()
        c.words = minOf(c.words + words, c.opened * need(active))
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
    fun affinityBonus(): Int = activeDef()?.let { affinity(it.attr) } ?: 0

    /** Đạo Tâm Chủng Ma, with a công pháp of Kiếm: tâm ma / 5, 20% at most. */
    fun chungMa(): Int = if (tamWorks(PhapMon.TP_CHUNG_MA) && activeDef()?.attr == PhapMon.KIEM) minOf(20, TuLuyen.tamMa / 5) else 0

    /** Percent of linh lực the cultivator draws from a word (before bình cảnh and wounds). */
    fun speed(): Int = (100 + rootBonus() + congBonus() + affinityBonus() + chungMa() + DongPhu.speedBonus() + DaoLo.speedBonus() + LinhGioi.speedBonus() + DoKiep.speedBonus()).coerceIn(50, 250)

    fun speedParts(): List<Pair<String, Int>> {
        val list = ArrayList<Pair<String, Int>>()
        if (awakened()) list.add("Linh căn: ${shortName()}" to rootBonus())
        activeDef()?.let { d ->
            list.add("${PhapMon.PHAM[d.pham]} phẩm, tầng ${tang()}" to congBonus())
            val a = affinityBonus()
            if (a > 0) list.add("Hợp linh căn (${PhapMon.attrName(d.attr)})" to a)
            if (a < 0) list.add("Khắc linh căn (${PhapMon.attrName(d.attr)})" to a)
        }
        if (chungMa() > 0) list.add("Đạo Tâm Chủng Ma" to chungMa())
        if (DongPhu.speedBonus() > 0) list.add("Linh mạch ${DongPhu.giaiName()}" to DongPhu.speedBonus())
        list.addAll(DaoLo.speedParts())
        list.addAll(LinhGioi.speedParts())
        if (DoKiep.speedBonus() > 0) list.add("Ký ức tiền kiếp" to DoKiep.speedBonus())
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
        gainNgo(1 + (if (book.words >= TuLuyen.NGO_DAO_WORDS) 1 else 0) + (if (effect(PhapMon.E_NGO_TINH)) 1 else 0), "Đọc trọn «${book.title}»")
    }

    // --- Tâm pháp ------------------------------------------------------------------------------------

    fun tamSlots() = when {
        Store.realmCap >= 3 -> 2
        Store.realmCap >= 1 -> 1
        else -> 0
    }

    fun tamLevel(id: Int): Int = (tam[id] ?: 0).let { if (it >= PhapMon.TAM_DAI_THANH) 2 else if (it >= PhapMon.TAM_NHAP_MON) 1 else 0 }
    /** A tâm pháp in a slot and past nhập môn: its effect is on. */
    fun tamWorks(id: Int) = id in tamOn && tamLevel(id) >= 1

    fun gainTam(id: Int): Boolean {
        if (id !in TAM_PHAP.indices || id in tam) return false
        tam[id] = 0
        if (tamOn.size < tamSlots()) tamOn.add(id)
        Store.touch()
        return true
    }

    fun runTam(id: Int): Boolean {
        if (id !in tam || id in tamOn || tamOn.size >= tamSlots()) return false
        tamOn.add(id)
        Store.touch()
        return true
    }

    fun stopTam(id: Int): Boolean {
        val ok = tamOn.remove(id)
        if (ok) Store.touch()
        return ok
    }

    /** A sitting reached 30 minutes: one vận chuyển of every tâm pháp in a slot. */
    fun onSitting30() {
        // Minh Tâm Tự: each sitting counts twice.
        val step = if (TongMon.perk(TongMon.MINH_TAM)) 2 else 1
        for (id in tamOn) {
            val before = tam[id] ?: 0
            val n = before + step
            tam[id] = n
            if (before < PhapMon.TAM_NHAP_MON && n >= PhapMon.TAM_NHAP_MON) Store.note("${TAM_PHAP[id].name} nhập môn: ${TAM_PHAP[id].text.replaceFirstChar { it.lowercase() }}")
            if (before < PhapMon.TAM_DAI_THANH && n >= PhapMon.TAM_DAI_THANH) Store.note("${TAM_PHAP[id].name} đại thành: tâm cảnh +5")
        }
        if (tamOn.isNotEmpty()) Store.touch()
    }

    fun tamCanhBonus() = 5 * tamOn.count { tamLevel(it) >= 2 }
    fun canCoBonus() = (if (effect(PhapMon.E_CAN_CO)) 5 else 0) + if (tamWorks(PhapMon.TP_DUONG_KHI)) 5 else 0

    /** Percent of a rise of tâm ma that comes through. */
    fun stirPercent() = (if (tamWorks(PhapMon.TP_THANH_TAM)) 50 else 100) * (if (effect(PhapMon.E_TINH_TAM)) 70 else 100) / 100 *
        (if (TongMon.perk(TongMon.MINH_TAM)) 80 else 100) / 100

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

    // --- Thuật pháp ----------------------------------------------------------------------------------

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
        if (thuatOn.size < thuatSlots()) thuatOn.add(id)
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
    fun mastery(id: Int) = PhapMon.mastery(thuat[id]?.uses ?: 0)

    private fun used(id: Int) {
        val t = thuat[id] ?: return
        val before = PhapMon.mastery(t.uses)
        t.uses++
        val after = PhapMon.mastery(t.uses)
        if (after > before) Store.note("${THUAT_PHAP[id].name} ${PhapMon.MASTERY[after].lowercase()}")
        Store.touch()
    }

    private fun onKind(kind: Int) = thuatOn.filter { on(it) && THUAT_PHAP[it].kind == kind }

    /** The weekly bí cảnh is cleared: the công kích thuật pháp bring back linh thạch. */
    fun onBiCanh(): Long {
        var lt = 0L
        for (id in onKind(PhapMon.K_CONG_KICH)) {
            lt += 10L * (mastery(id) + 1)
            used(id)
        }
        return lt
    }

    /** Days missed that a kiếp under way forgives, by the phòng ngự thuật pháp. */
    fun trialSpare(): Int = onKind(PhapMon.K_PHONG_NGU).map { if (mastery(it) >= 3) 2 else 1 }.sum()

    fun onTrialStart() {
        shields = (if (on(PhapMon.T_THIEN_NHAN)) (if (mastery(PhapMon.T_THIEN_NHAN) >= 3) 2 else 1) else 0) +
            (if (on(PhapMon.T_THAN_KIEM)) 1 else 0)
    }

    fun onTrialEnd() {
        onKind(PhapMon.K_PHONG_NGU).forEach { used(it) }
        shields = 0
    }

    /** A tâm ma attack in the lôi kiếp: true when a thuật pháp blocks it. */
    fun block(): Boolean {
        if (shields <= 0) return false
        shields--
        val id = if (on(PhapMon.T_THIEN_NHAN)) PhapMon.T_THIEN_NHAN else PhapMon.T_THAN_KIEM
        used(id)
        Store.note("${THUAT_PHAP[id].name} chặn được tâm ma")
        return true
    }

    /** Liễm Tức Thuật: a jump past three a day stirs nothing. */
    fun quietJump(): Boolean {
        if (!on(PhapMon.T_LIEM_TUC)) return false
        if (liemTucDay != today()) {
            liemTucDay = today()
            used(PhapMon.T_LIEM_TUC)
        }
        return true
    }

    fun phapTuong(): Int = if (on(PhapMon.T_PHAP_TUONG)) 5 + mastery(PhapMon.T_PHAP_TUONG) else 0
    fun onRoll() {
        if (on(PhapMon.T_PHAP_TUONG)) used(PhapMon.T_PHAP_TUONG)
    }

    /** What a failed breakthrough costs once the độn thuật and the công pháp have done their part. */
    class Fall(var kind: Int, var lossDiv: Int, var injuryDays: Int, var restDays: Int, var tamMa: Int, val saved: ArrayList<String> = ArrayList())

    fun soften(kind: Int): Fall {
        val f = Fall(kind, 50, 5, if (effect(PhapMon.E_HOI_KHI)) 1 else 2, 10)
        if (f.kind == TuLuyen.TAU_HOA && on(PhapMon.T_HUYET_DON)) {
            f.kind = TuLuyen.KINH_MACH
            f.injuryDays = if (mastery(PhapMon.T_HUYET_DON) >= 2) 5 else 8
            used(PhapMon.T_HUYET_DON)
            f.saved.add("Huyết Độn thoát tẩu hỏa")
        }
        if (f.kind != TuLuyen.TAU_HOA && on(PhapMon.T_THO_DON)) {
            f.lossDiv = 100
            f.injuryDays = minOf(f.injuryDays, if (mastery(PhapMon.T_THO_DON) >= 2) 2 else 3)
            used(PhapMon.T_THO_DON)
            f.saved.add("Thổ Độn")
        }
        if (on(PhapMon.T_PHONG_DON)) {
            f.restDays = minOf(f.restDays, if (mastery(PhapMon.T_PHONG_DON) >= 2) 0 else 1)
            used(PhapMon.T_PHONG_DON)
            f.saved.add("Phong Độn")
        }
        if (on(PhapMon.T_THUAN_DI)) {
            f.restDays = 0
            used(PhapMon.T_THUAN_DI)
            f.saved.add("Thuấn Di")
        }
        if (on(PhapMon.T_THAN_KIEM)) f.tamMa = 5
        return f
    }

    // --- Ngọc giản -----------------------------------------------------------------------------------

    fun owns(kind: Int, id: Int) = when (kind) {
        PhapMon.CONG -> id in cong
        PhapMon.TAM -> id in tam
        PhapMon.DAN -> id in DongPhu.known
        else -> id in thuat
    }

    fun gain(kind: Int, id: Int): Boolean = when (kind) {
        PhapMon.CONG -> gainCong(id)
        PhapMon.TAM -> gainTam(id)
        PhapMon.DAN -> DongPhu.learn(id)
        else -> gainThuat(id)
    }

    fun price(kind: Int, id: Int) = when (kind) {
        PhapMon.CONG -> PhapMon.PRICE[CONG_PHAP[id].pham]
        PhapMon.TAM -> PhapMon.TAM_PRICE
        PhapMon.DAN -> com.exacta.reader.game.DanDao.recipePrice(id)
        else -> PhapMon.thuatPrice(id)
    }.let { if (TongMon.perk(TongMon.VAN_QUYEN)) it * 85 / 100 else it }

    /** The phẩm of công pháp the reader looks for now: what the next gate asks, or the one running. */
    fun targetPham() = maxOf(PhapMon.phamFor(TuLuyen.gate()), activeDef()?.pham ?: 0).coerceIn(0, 5)

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
                val list = CONG_PHAP.indices.filter { CONG_PHAP[it].pham == p && it !in cong }
                    .ifEmpty { CONG_PHAP.indices.filter { CONG_PHAP[it].pham <= target && it !in cong } }
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
        gain(pick.first, pick.second)
        val extra = if (pick.first == PhapMon.CONG) " (${PhapMon.PHAM[CONG_PHAP[pick.second].pham]} phẩm)" else ""
        return "ngọc giản ${PhapMon.SCROLL_KINDS[pick.first].lowercase()} ${PhapMon.scrollName(pick.first, pick.second)}$extra"
    }

    /** Tàng Kinh Các: what it lends this week (three công pháp, two tâm pháp, three thuật pháp). */
    fun weekOffers(): List<Pair<Int, Int>> {
        val w = Store.weekStart(today())
        if (w != offerWeek) {
            offerWeek = w
            offers.clear()
            val r = Random(w * 7919 + 17)
            val target = targetPham()
            val free = CONG_PHAP.indices.filter { it !in cong }
            val atTarget = free.filter { CONG_PHAP[it].pham == target }
            val first = atTarget.filter { affinity(CONG_PHAP[it].attr) > 0 }.randomOrNull(r) ?: atTarget.randomOrNull(r)
            val picks = listOfNotNull(first) + (free.filter { it != first && CONG_PHAP[it].pham in (target - 1)..target }.shuffled(r) +
                free.filter { it != first && CONG_PHAP[it].pham < target - 1 }.shuffled(r)).take(3 - (if (first != null) 1 else 0))
            picks.forEach { offers.add(PhapMon.CONG to it) }
            TAM_PHAP.indices.filter { it !in tam }.shuffled(r).take(2).forEach { offers.add(PhapMon.TAM to it) }
            THUAT_PHAP.indices.filter { it !in thuat && canLearnRealm(it) }.shuffled(r).take(3).forEach { offers.add(PhapMon.THUAT to it) }
            if (DongPhu.started) com.exacta.reader.game.DanDao.RECIPES.indices.filter { it !in DongPhu.known }.shuffled(r).take(2).forEach { offers.add(PhapMon.DAN to it) }
            Store.touch()
        }
        return offers
    }

    fun buy(kind: Int, id: Int): Boolean {
        if (owns(kind, id)) return false
        val p = price(kind, id)
        if (Store.linhThach < p) return false
        Store.linhThach -= p
        gain(kind, id)
        Store.note("Tàng Kinh Các: đổi ${PhapMon.scrollName(kind, id)} · $p linh thạch")
        Store.touch()
        return true
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
        put("cong", JSONObject().apply { cong.forEach { (id, c) -> put(id.toString(), JSONArray().put(c.words).put(c.opened)) } })
        put("tam", JSONObject().apply { tam.forEach { (id, n) -> put(id.toString(), n) } })
        put("tamOn", JSONArray().apply { tamOn.forEach { put(it) } }); put("bangTam", bangTamMonth)
        put("thuat", JSONObject().apply { thuat.forEach { (id, t) -> put(id.toString(), JSONArray().put(t.learned).put(t.uses)) } })
        put("thuatOn", JSONArray().apply { thuatOn.forEach { put(it) } }); put("shields", shields); put("liemTuc", liemTucDay)
        put("offerWeek", offerWeek)
        put("offers", JSONArray().apply { offers.forEach { put(JSONArray().put(it.first).put(it.second)) } })
        put("scrolls", scrolls); put("gifted", gifted)
    }

    fun read(o: JSONObject) {
        reset()
        started = true
        o.optJSONArray("roots")?.let { a -> for (i in 0 until a.length()) roots.add(a.getInt(i)) }
        di = o.optInt("di", -1); awakenDay = o.optLong("awaken", -1)
        o.optJSONArray("hoursAt")?.let { a -> for (i in 0 until minOf(a.length(), 24)) hoursAt[i] = a.getLong(i) }
        washed = o.optBoolean("washed"); ngo = o.optInt("ngo"); ngoEarned = o.optInt("ngoEarned")
        active = o.optInt("active", -1); capNoted = o.optInt("capNoted", -1)
        o.optJSONObject("cong")?.let { m ->
            for (k in m.keys()) {
                val id = k.toInt()
                if (id !in CONG_PHAP.indices) continue
                val a = m.getJSONArray(k)
                cong[id] = Cong(a.optLong(0), a.optInt(1, 3))
            }
        }
        o.optJSONObject("tam")?.let { m -> for (k in m.keys()) k.toInt().takeIf { it in TAM_PHAP.indices }?.let { tam[it] = m.getInt(k) } }
        o.optJSONArray("tamOn")?.let { a -> for (i in 0 until a.length()) a.getInt(i).takeIf { it in tam }?.let { tamOn.add(it) } }
        bangTamMonth = o.optInt("bangTam", -1)
        o.optJSONObject("thuat")?.let { m ->
            for (k in m.keys()) {
                val id = k.toInt()
                if (id !in THUAT_PHAP.indices) continue
                val a = m.getJSONArray(k)
                thuat[id] = Thuat(a.optBoolean(0), a.optInt(1))
            }
        }
        o.optJSONArray("thuatOn")?.let { a -> for (i in 0 until a.length()) a.getInt(i).takeIf { it in thuat }?.let { thuatOn.add(it) } }
        shields = o.optInt("shields"); liemTucDay = o.optLong("liemTuc", -1); offerWeek = o.optLong("offerWeek", -1)
        o.optJSONArray("offers")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { offers.add(it.getInt(0) to it.getInt(1)) } }
        scrolls = o.optInt("scrolls"); gifted = o.optInt("gifted")
    }
}
