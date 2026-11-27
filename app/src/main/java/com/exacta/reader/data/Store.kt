package com.exacta.reader.data

import android.content.Context
import com.exacta.reader.game.DaoHieu
import com.exacta.reader.game.Lunar
import com.exacta.reader.game.Realm
import com.exacta.reader.game.Treasure
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/** One book on the storage and what the reader did with it. */
class Book(val path: String) {
    var title = File(path).nameWithoutExtension
    var author = ""
    var words = -1L          // words in the whole book, -1 until counted
    var progress = 0.0       // 0..1, where the reader is
    var maxProgress = 0.0    // the furthest point read in order
    var wordsRead = 0L
    var seconds = 0L
    var firstDay = -1L
    var lastDay = -1L
    var finishDay = -1L
    var lastOpened = 0L      // ms
    var locator = ""         // Readium locator JSON
    var cover = ""           // cached cover PNG
    var forged = false       // its finishing treasure was given
    var tang = 0             // highest tầng công pháp reached step by step

    val percent get() = (progress * 100).toInt().coerceIn(0, 100)

    fun toJson() = JSONObject().apply {
        put("path", path); put("title", title); put("author", author); put("words", words)
        put("progress", progress); put("maxProgress", maxProgress); put("wordsRead", wordsRead); put("seconds", seconds)
        put("firstDay", firstDay); put("lastDay", lastDay); put("finishDay", finishDay); put("lastOpened", lastOpened)
        put("locator", locator); put("cover", cover); put("forged", forged); put("tang", tang)
    }

    companion object {
        fun from(o: JSONObject) = Book(o.getString("path")).apply {
            title = o.optString("title", title); author = o.optString("author"); words = o.optLong("words", -1)
            progress = o.optDouble("progress", 0.0); maxProgress = o.optDouble("maxProgress", 0.0)
            wordsRead = o.optLong("wordsRead"); seconds = o.optLong("seconds"); firstDay = o.optLong("firstDay", -1)
            lastDay = o.optLong("lastDay", -1); finishDay = o.optLong("finishDay", -1); lastOpened = o.optLong("lastOpened")
            locator = o.optString("locator"); cover = o.optString("cover"); forged = o.optBoolean("forged")
            tang = o.optInt("tang", Realm.tangOf((maxProgress * 100).toInt()))
        }
    }
}

/** A forged treasure. */
data class Beast(val seed: Long, val tier: Int, val bornDay: Long, val book: String) {
    val species get() = Treasure.species(seed, tier)
    val name get() = Treasure.shortName(seed, tier)
}

/** An ore in the forge: words read (any book) forge it. */
data class Ore(val tier: Int, var words: Long = 0) {
    val percent get() = (words * 100 / Treasure.FORGE_WORDS[tier]).toInt().coerceIn(0, 100)
}

/**
 * Everything the app remembers, in one JSON file (written to a temporary file first, then renamed).
 * The reading journal counts seconds and words per calendar day; the game counts the words credited
 * (read forward in order: jumps through the book only move the mark).
 *
 * Omega, as on Exacta.OS:
 *  - thiên kiếp: each major realm opens only after days in a row of reading (30..60 minutes,
 *    3..7 days); a day missed in between fails it and tu vi falls back near the gate;
 *  - bí cảnh: 5 days of 30 minutes between Monday and Sunday give an ore (Huyền phẩm or better);
 *  - đạo hiệu: 36 titles earned by reading habits.
 */
object Store {
    const val FORGE_SLOTS = 3
    private lateinit var file: File
    val books = LinkedHashMap<String, Book>()
    val daySeconds = HashMap<Long, Long>()
    val dayWords = HashMap<Long, Long>()
    var totalWords = 0L
    var totalSeconds = 0L
    var giftsAwarded = 0L
    val bag = IntArray(Treasure.TIERS)
    val forge = ArrayList<Ore>()
    val beasts = ArrayList<Beast>()
    var unseen = 0

    // Habits for the titles.
    val hourSeconds = LongArray(24)
    var bestSession = 0L
    var tangCount = 0

    // Thiên kiếp.
    var realmCap = -1          // highest major realm opened (0..9); -1 until first computed
    var realmSeen = 0
    var trialStart = -1L       // day the pending trial began
    var tuViLoss = 0L          // words of tu vi lost to failed trials
    var failDay = -1L
    var failCount = 0
    var trialPassed = 0
    var passedAfterFail = false

    // Bí cảnh.
    var biCanhWeek = -1L       // Monday of the last rewarded week
    var biCanhCount = 0

    // Đạo hiệu.
    val titleDays = HashMap<Int, Long>()  // title -> day earned
    var titleChosen = -1       // -1 = show the newest
    var titleLast = -1
    var titleUnseen = 0

    /** Lines for the reader's footer and Home (a treasure forged, a trial, a title), oldest first. */
    private val notes = ArrayDeque<String>()

    // Reader preferences.
    var fontSize = 1.1
    var lineHeight = 1.5
    var margins = 1.2
    var fontFamily = "Bitter"   // "Original", "Bitter", "serif", "sans-serif"
    var justify = true
    var swapTaps = false
    var uiScale = 1.0
    var birthDay = 0
    var birthMonth = 0

    private var dirty = false

    fun today(): Long = LocalDate.now().toEpochDay()

    fun load(context: Context) {
        file = File(context.filesDir, "exacta-state.json")
        if (file.exists()) {
            try {
                read(JSONObject(file.readText()))
            } catch (e: Exception) {
                com.exacta.reader.ExactaApp.log("state load failed: $e")
            }
        }
        if (realmCap < 0) {
            // First run of this version: the realm reached so far stays open.
            val level = Realm.level(totalWords)
            realmCap = Realm.realmOf(level)
            realmSeen = level
            dirty = true
        }
    }

    private fun read(o: JSONObject) {
        o.optJSONArray("books")?.let { a -> for (i in 0 until a.length()) Book.from(a.getJSONObject(i)).let { books[it.path] = it } }
        o.optJSONObject("days")?.let { d ->
            for (k in d.keys()) {
                val v = d.getJSONArray(k)
                daySeconds[k.toLong()] = v.getLong(0)
                dayWords[k.toLong()] = v.getLong(1)
            }
        }
        totalWords = o.optLong("totalWords"); totalSeconds = o.optLong("totalSeconds"); giftsAwarded = o.optLong("gifts")
        o.optJSONArray("bag")?.let { for (i in 0 until minOf(it.length(), bag.size)) bag[i] = it.getInt(i) }
        o.optJSONArray("forge")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { forge.add(Ore(it.getInt(0), it.getLong(1))) } }
        o.optJSONArray("beasts")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let {
                beasts.add(Beast(it.getLong("seed"), it.getInt("tier"), it.optLong("born", -1), it.optString("book")))
            }
        }
        unseen = o.optInt("unseen")
        o.optJSONArray("hours")?.let { for (i in 0 until minOf(it.length(), 24)) hourSeconds[i] = it.getLong(i) }
        bestSession = o.optLong("bestSession"); tangCount = o.optInt("tangCount")
        o.optJSONObject("trial")?.let {
            realmCap = it.optInt("cap", -1); realmSeen = it.optInt("seen"); trialStart = it.optLong("start", -1)
            tuViLoss = it.optLong("loss"); failDay = it.optLong("failDay", -1); failCount = it.optInt("fails")
            trialPassed = it.optInt("passed"); passedAfterFail = it.optBoolean("passedAfterFail")
        }
        o.optJSONObject("biCanh")?.let { biCanhWeek = it.optLong("week", -1); biCanhCount = it.optInt("count") }
        o.optJSONObject("titles")?.let { t ->
            t.optJSONObject("days")?.let { d -> for (k in d.keys()) titleDays[k.toInt()] = d.getLong(k) }
            titleChosen = t.optInt("chosen", -1); titleLast = t.optInt("last", -1); titleUnseen = t.optInt("unseen")
        }
        o.optJSONObject("prefs")?.let {
            fontSize = it.optDouble("fontSize", fontSize); lineHeight = it.optDouble("lineHeight", lineHeight)
            margins = it.optDouble("margins", margins); fontFamily = it.optString("fontFamily", fontFamily)
            justify = it.optBoolean("justify", justify); swapTaps = it.optBoolean("swapTaps", swapTaps)
            uiScale = it.optDouble("uiScale", uiScale); birthDay = it.optInt("birthDay"); birthMonth = it.optInt("birthMonth")
        }
    }

    fun touch() {
        dirty = true
    }

    @Synchronized
    fun save() {
        if (!dirty || !::file.isInitialized) return
        val o = JSONObject().apply {
            put("version", 2)
            put("books", JSONArray().apply { books.values.toList().forEach { put(it.toJson()) } })
            put("days", JSONObject().apply {
                for (d in daySeconds.keys + dayWords.keys) put(d.toString(), JSONArray().put(daySeconds[d] ?: 0).put(dayWords[d] ?: 0))
            })
            put("totalWords", totalWords); put("totalSeconds", totalSeconds); put("gifts", giftsAwarded)
            put("bag", JSONArray().apply { bag.forEach { put(it) } })
            put("forge", JSONArray().apply { forge.forEach { put(JSONArray().put(it.tier).put(it.words)) } })
            put("beasts", JSONArray().apply {
                beasts.forEach { put(JSONObject().put("seed", it.seed).put("tier", it.tier).put("born", it.bornDay).put("book", it.book)) }
            })
            put("unseen", unseen)
            put("hours", JSONArray().apply { hourSeconds.forEach { put(it) } })
            put("bestSession", bestSession); put("tangCount", tangCount)
            put("trial", JSONObject().put("cap", realmCap).put("seen", realmSeen).put("start", trialStart).put("loss", tuViLoss)
                .put("failDay", failDay).put("fails", failCount).put("passed", trialPassed).put("passedAfterFail", passedAfterFail))
            put("biCanh", JSONObject().put("week", biCanhWeek).put("count", biCanhCount))
            put("titles", JSONObject().put("chosen", titleChosen).put("last", titleLast).put("unseen", titleUnseen)
                .put("days", JSONObject().apply { titleDays.forEach { (t, d) -> put(t.toString(), d) } }))
            put("prefs", JSONObject().put("fontSize", fontSize).put("lineHeight", lineHeight).put("margins", margins)
                .put("fontFamily", fontFamily).put("justify", justify).put("swapTaps", swapTaps)
                .put("uiScale", uiScale).put("birthDay", birthDay).put("birthMonth", birthMonth))
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(o.toString())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
        dirty = false
    }

    fun book(path: String) = books.getOrPut(path) { Book(path) }

    /** The book read most recently, if any. */
    fun current(): Book? = recent(1).firstOrNull()

    /** Books opened, most recent first. */
    fun recent(n: Int): List<Book> =
        books.values.filter { it.lastOpened > 0 && File(it.path).exists() }.sortedByDescending { it.lastOpened }.take(n)

    // --- Notes ------------------------------------------------------------------------------------

    private fun note(s: String) {
        notes.addLast(s)
        while (notes.size > 6) notes.removeFirst()
    }

    fun takeNote(): String = notes.removeFirstOrNull() ?: ""

    // --- Reading ----------------------------------------------------------------------------------

    /** Seconds of reading just now; `session` is the length of the sitting so far. */
    fun addSeconds(book: Book, seconds: Long, session: Long = 0) {
        if (seconds <= 0) return
        val d = today()
        daySeconds[d] = (daySeconds[d] ?: 0) + seconds
        hourSeconds[LocalTime.now().hour] += seconds
        book.seconds += seconds
        totalSeconds += seconds
        book.lastDay = d
        if (book.firstDay < 0) book.firstDay = d
        bestSession = maxOf(bestSession, session)
        checkRealm()
        checkBiCanh()
        checkTitles()
        touch()
    }

    /**
     * The reader is now at `progression` (0..1). Moving forward a little credits the words between;
     * a jump forward (contents, slider) only moves the mark.
     */
    fun moveTo(book: Book, progression: Double) {
        book.progress = progression
        val step = progression - book.maxProgress
        if (step > 0) {
            if (book.words > 0 && step <= MAX_STEP) credit(book, (step * book.words).toLong())
            book.maxProgress = progression
        }
        val tang = Realm.tangOf(book.percent)
        if (tang > book.tang) {
            if (tang == book.tang + 1 && book.tang > 0) tangCount++
            book.tang = tang
        }
        if (progression >= FINISH && book.finishDay < 0) {
            book.finishDay = today()
            if (!book.forged && book.words > 0) {
                book.forged = true
                val tier = Treasure.tierForBook(book.words)
                hatch(tier, book.title)
            }
            checkTitles()
        }
        touch()
    }

    private const val MAX_STEP = 0.05
    private const val FINISH = 0.95

    private fun credit(book: Book, words: Long) {
        if (words <= 0) return
        val d = today()
        dayWords[d] = (dayWords[d] ?: 0) + words
        book.wordsRead += words
        totalWords += words
        // The forge burns with every word read, in any book.
        val done = ArrayList<Ore>()
        for (ore in forge) {
            ore.words += words
            if (ore.words >= Treasure.FORGE_WORDS[ore.tier]) done.add(ore)
        }
        for (ore in done) {
            forge.remove(ore)
            hatch(ore.tier, book.title)
            fillForge()
        }
        // A gift ore every 10 vạn words.
        while (totalWords / Treasure.GIFT_WORDS > giftsAwarded) {
            giftsAwarded++
            val tier = Treasure.rollTier()
            bag[tier]++
            fillForge()
            note("Quà 10 vạn từ: khoáng thạch ${Treasure.TIER_NAMES[tier]}")
        }
        checkRealm()
    }

    private fun hatch(tier: Int, bookTitle: String) {
        val beast = Beast(Random.nextLong(0, Long.MAX_VALUE / 2), tier, today(), bookTitle)
        beasts.add(beast)
        unseen++
        note("Pháp bảo xuất thế: ${beast.name} (${Treasure.TIER_NAMES[tier]})")
    }

    /** Ores wait in the bag while the forge is full; the best one goes in first. */
    fun fillForge() {
        while (forge.size < FORGE_SLOTS) {
            val t = (Treasure.TIERS - 1 downTo 0).firstOrNull { bag[it] > 0 } ?: return
            bag[t]--
            forge.add(Ore(t))
        }
    }

    // --- Cảnh giới and thiên kiếp ---------------------------------------------------------------

    private val TRIAL_MINUTES = intArrayOf(30, 30, 35, 40, 45, 45, 50, 55, 60)
    private val TRIAL_DAYS = intArrayOf(3, 3, 4, 4, 5, 5, 6, 6, 7)

    /** Words of tu vi: words read, less what failed trials cost. */
    fun tuVi() = (totalWords - tuViLoss).coerceAtLeast(0)

    private fun rawLevel() = Realm.level(tuVi())

    /** The realm level reached (0..35): the words may go further while a trial is pending. */
    fun level(): Int {
        val raw = rawLevel()
        return if (realmCap >= 9) raw else minOf(raw, realmCap.coerceAtLeast(0) * 3 + 2)
    }

    fun inTrial() = realmCap in 0..8 && Realm.realmOf(rawLevel()) > realmCap
    fun trialNeed() = TRIAL_DAYS[realmCap.coerceIn(0, 8)]
    fun trialMinutes() = TRIAL_MINUTES[realmCap.coerceIn(0, 8)]
    fun minutes(day: Long) = (daySeconds[day] ?: 0) / 60

    /** The realm the next trial opens (realmCap + 1), and the words to its gate. */
    fun nextGate() = (realmCap + 1).coerceAtMost(9)
    fun wordsToGate(): Long = (Realm.floor(nextGate() * 3) - tuVi()).coerceAtLeast(0)

    /**
     * Days that met the minutes since the trial began, and whether a day between fell short. The
     * first day (the gate may be reached late in it) and today (still going) only count when met.
     */
    fun trialDays(): Pair<Int, Boolean> {
        val today = today()
        if (trialStart < 0) return 0 to false
        val need = trialMinutes()
        var run = 0
        for (d in trialStart..today) {
            if (minutes(d) >= need) run++
            else if (d != trialStart && d != today) return run to true
        }
        return run to false
    }

    fun checkRealm() {
        if (realmCap < 0) return
        if (inTrial()) {
            val today = today()
            if (trialStart < 0) {
                trialStart = today
                note("Thiên kiếp giáng: ${trialNeed()} ngày, mỗi ngày ${trialMinutes()} phút để vào ${Realm.baseName(nextGate())}")
                touch()
            }
            val (days, failed) = trialDays()
            if (failed) {
                // Độ kiếp thất bại: tu vi falls to one ninth below the gate.
                val lo = Realm.floor(realmCap * 3)
                val hi = Realm.floor(realmCap * 3 + 3)
                val target = hi - (hi - lo) / 9
                if (totalWords > target) tuViLoss = totalWords - target
                trialStart = -1
                failDay = today
                failCount++
                note("Độ kiếp thất bại: tu vi lui về ${Realm.van(target)} vạn từ")
                touch()
            } else if (days >= trialNeed()) {
                realmCap++
                trialStart = -1
                trialPassed++
                if (failCount > 0) passedAfterFail = true
                note("Vượt thiên kiếp: mở cảnh giới ${Realm.baseName(realmCap)}")
                touch()
            }
        } else if (trialStart >= 0) {
            trialStart = -1
            touch()
        }
        val level = level()
        if (level > realmSeen) {
            realmSeen = level
            note("Đột phá: ${Realm.name(level)}")
            touch()
        }
    }

    // --- Bí cảnh ------------------------------------------------------------------------------------

    const val BI_CANH_DAYS = 5
    const val BI_CANH_MINUTES = 30

    fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()
    fun biCanhDays(): Int {
        val today = today()
        return (weekStart(today)..today).count { minutes(it) >= BI_CANH_MINUTES }
    }
    fun biCanhDone() = biCanhWeek == weekStart(today())

    private fun checkBiCanh() {
        val week = weekStart(today())
        if (biCanhWeek == week || biCanhDays() < BI_CANH_DAYS) return
        biCanhWeek = week
        biCanhCount++
        val tier = maxOf(Treasure.rollTier(), 1)  // never below Huyền
        bag[tier]++
        fillForge()
        note("Qua bí cảnh tuần: khoáng thạch ${Treasure.TIER_NAMES[tier]}")
        touch()
    }

    // --- Đạo hiệu -----------------------------------------------------------------------------------

    fun hasTitle(t: Int) = titleDays.containsKey(t)
    fun titleCount() = titleDays.size

    /** The title shown: the chosen one, else the newest; -1 none. */
    fun shownTitle(): Int = if (titleChosen >= 0 && hasTitle(titleChosen)) titleChosen else if (hasTitle(titleLast)) titleLast else -1

    fun checkTitles() {
        val today = today()
        val finished = books.values.filter { it.finishDay >= 0 }
        val quickest = finished.filter { it.firstDay >= 0 }.minOfOrNull { it.finishDay - it.firstDay + 1 } ?: -1
        val streak = maxOf(streak(), bestStreak())
        val night = listOf(23, 0, 1, 2, 3, 4).sumOf { hourSeconds[it] }
        val dawn = hourSeconds[5] + hourSeconds[6]
        val readToday = (daySeconds[today] ?: 0) >= 600
        val date = LocalDate.ofEpochDay(today)
        val lunar = Lunar.fromSolar(date.dayOfMonth, date.monthValue, date.year)
        val tet = readToday && !lunar.leap && lunar.month == 1 && lunar.day <= 3
        val trungThu = readToday && !lunar.leap && lunar.month == 8 && lunar.day == 15
        val birthday = readToday && birthDay > 0 && date.dayOfMonth == birthDay && date.monthValue == birthMonth
        val kinds = beasts.map { it.species }.toSet().size
        val tiers = beasts.map { it.tier }.toSet().size
        val earned = booleanArrayOf(
            // Tu hành
            totalWords >= 10_000, totalWords >= 100_000, totalWords >= 1_000_000, totalWords >= 5_000_000,
            totalWords >= 10_000_000, realmCap >= 9,
            // Đạo tâm
            streak >= 7, streak >= 30, streak >= 100, streak >= 365, trialPassed >= 1, passedAfterFail,
            // Thời khắc
            night >= 36_000, dawn >= 18_000, hourSeconds[0] >= 3_600, bestSession >= 7_200,
            (daySeconds.values.maxOrNull() ?: 0) >= 14_400, tet,
            // Kinh các
            finished.size >= 1, finished.size >= 10, finished.size >= 50, quickest in 1..3, tangCount >= 100,
            books.values.count { it.lastOpened > 0 } >= 30,
            // Pháp bảo
            beasts.isNotEmpty(), kinds >= 18, kinds >= 36, beasts.any { it.tier == Treasure.TIERS - 1 }, beasts.size >= 30, tiers >= 4,
            // Kỳ duyên
            giftsAwarded >= 20, totalSeconds >= 360_000, biCanhCount >= 10, trungThu, birthday, trialPassed >= 5,
        )
        for (t in 0 until DaoHieu.COUNT) {
            if (earned[t] && !hasTitle(t)) {
                titleDays[t] = today
                titleLast = t
                titleUnseen++
                note("Đắc đạo hiệu: ${DaoHieu.NAMES[t]}")
                touch()
            }
        }
    }

    // --- Stats ------------------------------------------------------------------------------------

    /** Days in a row with reading, ending today or yesterday. */
    fun streak(): Int {
        var d = today()
        if ((daySeconds[d] ?: 0) < 60) d--
        var n = 0
        while ((daySeconds[d] ?: 0) >= 60) {
            n++
            d--
        }
        return n
    }

    fun bestStreak(): Int {
        val days = daySeconds.filterValues { it >= 60 }.keys.sorted()
        var best = 0
        var run = 0
        var prev = Long.MIN_VALUE
        for (d in days) {
            run = if (d == prev + 1) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    fun secondsBetween(from: Long, to: Long) = (from..to).sumOf { daySeconds[it] ?: 0 }
    fun wordsBetween(from: Long, to: Long) = (from..to).sumOf { dayWords[it] ?: 0 }

    fun strongest(): Beast? = beasts.maxWithOrNull(compareBy<Beast>({ it.tier }, { it.bornDay }))

    /** Reading pace in words a minute (0 until there is enough to tell). */
    fun pace(): Long = if (totalSeconds >= 600) totalWords * 60 / totalSeconds else 0
}
