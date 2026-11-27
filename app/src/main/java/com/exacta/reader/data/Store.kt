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
data class Beast(
    val seed: Long,
    val tier: Int,
    val bornDay: Long,
    val book: String,
    val kind: Int = Treasure.legacyKind(seed),
    var spirit: String = "",   // khí linh awoken (kỳ ngộ) or the mark of a forge trial
) {
    val species get() = Treasure.speciesOf(tier, kind)
    val name get() = Treasure.shortName(seed, species)
}

/**
 * A slot of the forge: an ore that words read (any book) forge, or a book (`book` = its path) that
 * forges a treasure of its length's grade when read to the end.
 */
data class Ore(val tier: Int, var words: Long = 0, val book: String = "", val goal: Int = GOAL_ORE, var kiep: Long = -1, var spared: Int = 0, var bonus: Int = 0) {
    /** In a kiếp phá lô: the day it began (-1 none). */
    val inKiep get() = kiep >= 0
    val isBook get() = book.isNotEmpty()
    val isStage get() = goal == GOAL_STAGE
    val isFinish get() = goal == GOAL_FINISH
    /** Words this slot needs: an ore's forging, or a book's stage; a whole book counts its %. */
    val need get() = if (isStage) Store.stageNeed(tier) else Store.forgeNeed(tier)
    val percent get() = if (isFinish) (Store.books[book]?.percent ?: 0) else (words * 100 / need).toInt().coerceIn(0, 100)

    companion object {
        const val GOAL_ORE = 0     // an ore: forged by the words read in any book
        const val GOAL_STAGE = 1   // a stage of a book: every `need` words read in it forge a treasure of `tier`
        const val GOAL_FINISH = 2  // a whole book: read to the end, a treasure of its length's grade
    }
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
    const val FORGE_SLOTS = 6
    const val SLOTS_PER_BOOK = 3
    /** Ores the forge takes in by itself; the other slots wait for books. */
    private const val AUTO_ORES = 3
    /** Readium's font size for the reader's 100%. */
    const val BASE_SIZE = 1.56
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

    // Bộ pháp bảo: bit per set completed (once complete, for good).
    var setsDone = 0

    // 0.8: đan dược and phù, kỳ ngộ, kiếp phá lô.
    val items = IntArray(Treasure.ITEMS)
    var fireWords = 0L          // Thiên Hỏa Phù: words left that burn twice
    var kyNgoWords = 0L         // words toward the next roll of kỳ ngộ
    var kyNgoCount = 0
    val kyNgoLog = ArrayList<String>()
    var phaLoPassed = 0
    var phaLoFailed = 0
    var pillsUsed = 0
    var talismansUsed = 0
    var breakPillUsed = false
    var trialBonus = 0          // days of the pending thiên kiếp given by Phá Cảnh Đan
    // 0.9: what the other pills and talismans leave behind.
    var bonusTuVi = 0L          // Ngưng Thần Đan
    var trialSpare = 0          // Hộ Mạch Đan: missed days the pending thiên kiếp forgives
    var seekWords = 0L          // Tầm Bảo Phù: words left with kỳ ngộ ×3
    var giftLuck = 0            // Tụ Bảo Phù: gifts rolled three times
    var biCanhBonusWeek = -1L   // Bí Cảnh Phù: the week, and its extra days
    var biCanhBonusDays = 0
    val bonusMinutes = HashMap<Long, Int>()  // Định Thần Phù: minutes added to a day
    val sealedDays = HashSet<Long>()         // Phong Ấn Phù: missed days the streak keeps

    // 0.10: linh thạch and bế quan.
    var linhThach = 0L
    var linhThachEarned = 0L
    var ltWords = 0L            // words toward the next 5 linh thạch
    var beQuanTarget = 0        // minutes of the bế quan under way, 0 = none
    var beQuanSeconds = 0L      // reading seconds in it so far
    var beQuanDone = 0
    var beQuanMinutes = 0L      // minutes of all bế quan completed

    // Niên kỷ: the lunar year whose summary was opened at Tết.
    var nienKySeen = -1

    // Đạo hiệu.
    val titleDays = HashMap<Int, Long>()  // title -> day earned
    var titleChosen = -1       // -1 = show the newest
    var titleLast = -1
    var titleUnseen = 0

    /** Lines for the reader's footer and Home (a treasure forged, a trial, a title), oldest first. */
    private val notes = ArrayDeque<String>()

    // Reader preferences.
    /** Shown as a %: 100% is the base size, which is 130% of Readium's. */
    var fontSize = 1.0
    var lineHeight = 1.7
    var margins = 1.0
    var fontFamily = "Bitter"   // "Original", "Bitter", "serif", "sans-serif"
    var justify = true
    var swapTaps = false
    var uiScale = 1.0
    var birthDay = 0
    var birthMonth = 0
    var textWeight = 3          // 0 Mảnh … 5 Rất đậm; 3 Đậm vừa by default
    var footerMode = 0          // 0 chương · %, 1 chỉ %, 2 ẩn
    var homeStyle = 0           // see HomeView.STYLES
    var showHeader = true       // the book's title above the page
    var showNotices = true      // the cultivation's lines in the footer
    var hyphens = false
    var indent = false
    var showProverb = true
    var dailyGoal = 30          // minutes, 0 = none
    var librarySort = 0         // 0 recent, 1 title, 2 author
    var idleMinutes = 3         // reading time stops counting after this long without a touch
    var paraSpacing: Double? = null   // rem between paragraphs, null = the book's
    var refreshEvery = 8        // full refresh of the e-ink screen every N page turns, 0 = never

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
        o.optJSONArray("forge")?.let { a ->
            for (i in 0 until a.length()) a.getJSONArray(i).let {
                val book = it.optString(2, "")
                val goal = it.optInt(3, if (book.isEmpty()) Ore.GOAL_ORE else Ore.GOAL_FINISH)
                forge.add(Ore(it.getInt(0), it.getLong(1), book, goal, it.optLong(4, -1), it.optInt(5, 0), it.optInt(6, 0)))
            }
        }
        o.optJSONArray("beasts")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let {
                val seed = it.getLong("seed")
                beasts.add(Beast(seed, it.getInt("tier"), it.optLong("born", -1), it.optString("book"),
                    it.optInt("kind", Treasure.legacyKind(seed)), it.optString("spirit")))
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
        setsDone = o.optInt("sets"); nienKySeen = o.optInt("nienKySeen", -1)
        o.optJSONObject("tienTe")?.let {
            linhThach = it.optLong("lt"); linhThachEarned = it.optLong("ltEarned"); ltWords = it.optLong("ltWords")
            beQuanTarget = it.optInt("bqTarget"); beQuanSeconds = it.optLong("bqSeconds")
            beQuanDone = it.optInt("bqDone"); beQuanMinutes = it.optLong("bqMinutes")
        }
        o.optJSONObject("omega2")?.let {
            it.optJSONArray("items")?.let { a -> for (i in 0 until minOf(a.length(), items.size)) items[i] = a.getInt(i) }
            fireWords = it.optLong("fire"); kyNgoWords = it.optLong("kyNgoWords"); kyNgoCount = it.optInt("kyNgo")
            it.optJSONArray("log")?.let { a -> for (i in 0 until a.length()) kyNgoLog.add(a.getString(i)) }
            phaLoPassed = it.optInt("phaLoPassed"); phaLoFailed = it.optInt("phaLoFailed")
            pillsUsed = it.optInt("pills"); talismansUsed = it.optInt("talismans"); breakPillUsed = it.optBoolean("breakPill")
            trialBonus = it.optInt("trialBonus")
            bonusTuVi = it.optLong("bonusTuVi"); trialSpare = it.optInt("trialSpare"); seekWords = it.optLong("seek")
            giftLuck = it.optInt("giftLuck"); biCanhBonusWeek = it.optLong("biWeek", -1); biCanhBonusDays = it.optInt("biDays")
            it.optJSONObject("bonusMinutes")?.let { m -> for (k in m.keys()) bonusMinutes[k.toLong()] = m.getInt(k) }
            it.optJSONArray("sealed")?.let { a -> for (i in 0 until a.length()) sealedDays.add(a.getLong(i)) }
        }
        o.optJSONObject("titles")?.let { t ->
            val v2 = t.optInt("v") >= 2
            val move = { i: Int -> if (v2 || i < 0) i else (i / 6) * 12 + i % 6 }
            t.optJSONObject("days")?.let { d -> for (k in d.keys()) titleDays[move(k.toInt())] = d.getLong(k) }
            titleChosen = move(t.optInt("chosen", -1)); titleLast = move(t.optInt("last", -1)); titleUnseen = t.optInt("unseen")
        }
        o.optJSONObject("prefs")?.let {
            fontSize = it.optDouble("fontSize", fontSize); lineHeight = it.optDouble("lineHeight", lineHeight)
            margins = it.optDouble("margins", margins); fontFamily = it.optString("fontFamily", fontFamily)
            justify = it.optBoolean("justify", justify); swapTaps = it.optBoolean("swapTaps", swapTaps)
            uiScale = it.optDouble("uiScale", uiScale); birthDay = it.optInt("birthDay"); birthMonth = it.optInt("birthMonth")
            textWeight = it.optInt("textWeight", textWeight); footerMode = it.optInt("footerMode", footerMode)
            homeStyle = it.optInt("homeStyle", homeStyle); showHeader = it.optBoolean("showHeader", showHeader)
            showNotices = it.optBoolean("showNotices", showNotices); hyphens = it.optBoolean("hyphens", hyphens)
            indent = it.optBoolean("indent", indent); showProverb = it.optBoolean("showProverb", showProverb)
            dailyGoal = it.optInt("dailyGoal", dailyGoal); librarySort = it.optInt("librarySort", librarySort)
            idleMinutes = it.optInt("idleMinutes", idleMinutes)
            paraSpacing = if (it.has("paraSpacing")) it.optDouble("paraSpacing") else null
            refreshEvery = it.optInt("refreshEvery", refreshEvery)
            // Keep the letters the reader chose when the base moved: Readium's own before 0.5, 1.3 in 0.5.
            val oldBase = if (it.optBoolean("sizeBase156")) BASE_SIZE else if (it.optBoolean("sizeBase130")) 1.3 else 1.0
            if (oldBase != BASE_SIZE) fontSize = Math.round(fontSize * oldBase / BASE_SIZE * 20) / 20.0
            textWeight = textWeight.coerceIn(0, 5)
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
            put("forge", JSONArray().apply { forge.forEach { put(JSONArray().put(it.tier).put(it.words).put(it.book).put(it.goal).put(it.kiep).put(it.spared).put(it.bonus)) } })
            put("beasts", JSONArray().apply {
                beasts.forEach {
                    put(JSONObject().put("seed", it.seed).put("tier", it.tier).put("born", it.bornDay).put("book", it.book)
                        .put("kind", it.kind).put("spirit", it.spirit))
                }
            })
            put("unseen", unseen)
            put("hours", JSONArray().apply { hourSeconds.forEach { put(it) } })
            put("bestSession", bestSession); put("tangCount", tangCount)
            put("trial", JSONObject().put("cap", realmCap).put("seen", realmSeen).put("start", trialStart).put("loss", tuViLoss)
                .put("failDay", failDay).put("fails", failCount).put("passed", trialPassed).put("passedAfterFail", passedAfterFail))
            put("biCanh", JSONObject().put("week", biCanhWeek).put("count", biCanhCount))
            put("sets", setsDone); put("nienKySeen", nienKySeen)
            put("omega2", JSONObject().put("items", JSONArray().apply { items.forEach { put(it) } }).put("fire", fireWords)
                .put("kyNgoWords", kyNgoWords).put("kyNgo", kyNgoCount).put("log", JSONArray().apply { kyNgoLog.forEach { put(it) } })
                .put("phaLoPassed", phaLoPassed).put("phaLoFailed", phaLoFailed).put("pills", pillsUsed).put("talismans", talismansUsed)
                .put("breakPill", breakPillUsed).put("trialBonus", trialBonus)
                .put("bonusTuVi", bonusTuVi).put("trialSpare", trialSpare).put("seek", seekWords).put("giftLuck", giftLuck)
                .put("biWeek", biCanhBonusWeek).put("biDays", biCanhBonusDays)
                .put("bonusMinutes", JSONObject().apply { bonusMinutes.forEach { (d, m) -> put(d.toString(), m) } })
                .put("sealed", JSONArray().apply { sealedDays.forEach { put(it) } }))
            put("tienTe", JSONObject().put("lt", linhThach).put("ltEarned", linhThachEarned).put("ltWords", ltWords)
                .put("bqTarget", beQuanTarget).put("bqSeconds", beQuanSeconds).put("bqDone", beQuanDone).put("bqMinutes", beQuanMinutes))
            put("titles", JSONObject().put("v", 2).put("chosen", titleChosen).put("last", titleLast).put("unseen", titleUnseen)
                .put("days", JSONObject().apply { titleDays.forEach { (t, d) -> put(t.toString(), d) } }))
            put("prefs", JSONObject().put("fontSize", fontSize).put("lineHeight", lineHeight).put("margins", margins)
                .put("fontFamily", fontFamily).put("justify", justify).put("swapTaps", swapTaps)
                .put("uiScale", uiScale).put("birthDay", birthDay).put("birthMonth", birthMonth)
                .put("textWeight", textWeight).put("footerMode", footerMode)
                .put("homeStyle", homeStyle).put("showHeader", showHeader).put("showNotices", showNotices)
                .put("hyphens", hyphens).put("indent", indent).put("showProverb", showProverb)
                .put("dailyGoal", dailyGoal).put("librarySort", librarySort).put("idleMinutes", idleMinutes)
                .put("refreshEvery", refreshEvery).put("sizeBase156", true)
                .apply { paraSpacing?.let { v -> put("paraSpacing", v) } })
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(o.toString())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
        dirty = false
    }

    /** Copies the state to `target` (a backup the reader keeps on the storage). */
    @Synchronized
    fun backup(target: File) {
        dirty = true
        save()
        target.parentFile?.mkdirs()
        file.copyTo(target, overwrite = true)
    }

    /** Replaces everything with a backup; false when the file cannot be read. */
    @Synchronized
    fun restore(source: File): Boolean {
        val o = try {
            JSONObject(source.readText())
        } catch (e: Exception) {
            return false
        }
        books.clear(); daySeconds.clear(); dayWords.clear(); forge.clear(); beasts.clear(); titleDays.clear()
        bag.fill(0); hourSeconds.fill(0)
        realmCap = -1; trialStart = -1; tuViLoss = 0; failDay = -1; failCount = 0; trialPassed = 0; passedAfterFail = false
        biCanhWeek = -1; biCanhCount = 0; setsDone = 0; titleChosen = -1; titleLast = -1; titleUnseen = 0
        items.fill(0); fireWords = 0; kyNgoWords = 0; kyNgoCount = 0; kyNgoLog.clear()
        phaLoPassed = 0; phaLoFailed = 0; pillsUsed = 0; talismansUsed = 0; breakPillUsed = false; trialBonus = 0
        bonusTuVi = 0; trialSpare = 0; seekWords = 0; giftLuck = 0; biCanhBonusWeek = -1; biCanhBonusDays = 0
        bonusMinutes.clear(); sealedDays.clear()
        linhThach = 0; linhThachEarned = 0; ltWords = 0; beQuanTarget = 0; beQuanSeconds = 0; beQuanDone = 0; beQuanMinutes = 0
        read(o)
        if (realmCap < 0) {
            realmCap = Realm.realmOf(Realm.level(totalWords))
            realmSeen = Realm.level(totalWords)
        }
        dirty = true
        save()
        return true
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
        if (beQuanTarget > 0) {
            beQuanSeconds += seconds
            if (beQuanSeconds >= beQuanTarget * 60L) finishBeQuan()
        }
        checkKiep()
        checkRealm()
        checkBiCanh()
        checkTitles()
        touch()
    }

    /**
     * The reader is now at `progression` (0..1). Moving forward a little credits the words between;
     * a jump forward (contents, slider) only moves the mark.
     */
    fun moveTo(book: Book, progression: Double, pageTurn: Boolean = false) {
        book.progress = progression
        val step = progression - book.maxProgress
        if (step > 0) {
            // Only reading counts: a page turned (or swiped, a page's worth) forward, at most a full page.
            // Jumps by the contents, the chapter buttons or the slider only move the mark.
            if (book.words > 0) {
                val words = (step * book.words).toLong()
                if (pageTurn || words <= PAGE_SWIPE) credit(book, minOf(words, PAGE_MAX))
            }
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
                val slot = forge.firstOrNull { it.book == book.path && it.isFinish }
                forge.removeAll { it.book == book.path && it.isStage }
                if (slot != null) {
                    // A book in the forge becomes a treasure of its length's grade (through a trial when high).
                    complete(slot, book.title)
                } else if (forge.none { it.book == book.path }) {
                    // Read outside the forge: an ore of the same grade.
                    giveOre(tier, "Đọc xong «${book.title}»")
                }
                if (setActive(7)) giveOre(Treasure.rollTier(), "Giang Hồ Kỳ Binh")
            }
            checkTitles()
        }
        touch()
    }

    private const val PAGE_MAX = 450L     // words credited for one page at most
    private const val PAGE_SWIPE = 600L   // a move this small without a tap is a swiped page
    private const val FINISH = 0.95

    private fun credit(book: Book, words: Long) {
        if (words <= 0) return
        val d = today()
        dayWords[d] = (dayWords[d] ?: 0) + words
        book.wordsRead += words
        totalWords += words
        // The forge burns with every word read, in any book (twice on feast days with Tứ Hải Long Cung).
        var burn = if (setActive(6) && feastDay()) words * 2 else words
        if (fireWords > 0) {
            fireWords = (fireWords - words).coerceAtLeast(0)
            burn *= 2
        }
        if (beQuanTarget > 0) burn = burn * 3 / 2
        ltWords += words
        while (ltWords >= 10_000) {
            ltWords -= 10_000
            gainLinhThach(5L * feastMultiplier())
        }
        val done = ArrayList<Ore>()
        for (ore in forge) {
            if (ore.inKiep) continue
            when {
                ore.isStage && ore.book == book.path -> {
                    ore.words += burn
                    if (ore.words >= ore.need) done.add(ore)
                }
                !ore.isBook -> {
                    ore.words += burn
                    if (ore.words >= ore.need) done.add(ore)
                }
            }
        }
        for (ore in done) complete(ore, book.title)
        // Kỳ ngộ: a roll for every vạn words read.
        kyNgoWords += words
        if (seekWords > 0) seekWords = (seekWords - words).coerceAtLeast(0)
        while (kyNgoWords >= 10_000) {
            kyNgoWords -= 10_000
            kyNgo(book)
        }
        // A gift ore every 10 vạn words.
        while (totalWords / Treasure.GIFT_WORDS > giftsAwarded) {
            giftsAwarded++
            var tier = if (setActive(0)) maxOf(Treasure.rollTier(), Treasure.rollTier()) else Treasure.rollTier()
            if (giftLuck > 0) {
                giftLuck--
                tier = maxOf(tier, Treasure.rollTier(), Treasure.rollTier())
            }
            bag[tier]++
            fillForge()
            note("Quà 10 vạn từ: khoáng thạch ${Treasure.TIER_NAMES[tier]}")
        }
        checkRealm()
    }

    /**
     * A slot reaches its words (or its book's end): Hoàng to Địa phẩm come out at once; Thiên phẩm and
     * above first pass a kiếp phá lô.
     */
    private fun complete(slot: Ore, title: String) {
        if (slot.tier >= 3 && !slot.inKiep) {
            slot.kiep = today()
            slot.spared = 0
            slot.words = slot.need
            note("Kiếp phá lô: ${Treasure.TIER_NAMES[slot.tier]} sắp xuất thế · ${kiepDays(slot.tier)} ngày × ${kiepMinutes(slot.tier)} phút")
            return
        }
        release(slot, title, "")
    }

    private fun release(slot: Ore, title: String, spirit: String) {
        hatch(slot.tier, title, spirit)
        slot.kiep = -1
        if (slot.isStage) {
            slot.words = (slot.words - slot.need).coerceAtLeast(0)
        } else {
            forge.remove(slot)
            fillForge()
        }
    }

    // --- Linh thạch -----------------------------------------------------------------------------------

    /** Tết ×3, mồng một and rằm ×2. */
    fun feastMultiplier(): Int {
        val d = LocalDate.now()
        val l = Lunar.fromSolar(d.dayOfMonth, d.monthValue, d.year)
        return when {
            !l.leap && l.month == 1 && l.day <= 3 -> 3
            l.day == 1 || l.day == 15 -> 2
            else -> 1
        }
    }

    fun gainLinhThach(n: Long) {
        if (n <= 0) return
        linhThach += n
        linhThachEarned += n
        touch()
    }

    /** Phường thị: an ore of a grade, or an item, for linh thạch. */
    val ORE_PRICES = longArrayOf(30, 70, 150, 320, 700, 1500)
    val ITEM_PRICES = longArrayOf(120, 60, 300, 100, 50, 150, 250, 120, 600, 200, 300, 80, 120, 60, 200, 90, 100, 60)

    fun buyOre(tier: Int): Boolean {
        val price = ORE_PRICES[tier]
        if (linhThach < price) return false
        linhThach -= price
        bag[tier]++
        fillForge()
        touch()
        return true
    }

    fun buyItem(i: Int): Boolean {
        val price = ITEM_PRICES[i]
        if (linhThach < price) return false
        linhThach -= price
        items[i]++
        touch()
        return true
    }

    // --- Bế quan --------------------------------------------------------------------------------------

    val BE_QUAN_CHOICES = intArrayOf(30, 60, 90, 120)
    fun beQuanReward(minutes: Int): Long = when (minutes) {
        30 -> 15; 60 -> 35; 90 -> 60; else -> 90
    }

    fun startBeQuan(minutes: Int) {
        beQuanTarget = minutes
        beQuanSeconds = 0
        note("Bế quan $minutes phút: lò ×1,5, kỳ ngộ ×2")
        touch()
    }

    fun cancelBeQuan() {
        beQuanTarget = 0
        beQuanSeconds = 0
        touch()
    }

    private fun finishBeQuan() {
        val m = beQuanTarget
        beQuanDone++
        beQuanMinutes += m
        beQuanTarget = 0
        beQuanSeconds = 0
        val lt = beQuanReward(m) * feastMultiplier()
        gainLinhThach(lt)
        note("Bế quan viên mãn $m phút: nhận $lt linh thạch")
        checkTitles()
    }

    // --- Kiếp phá lô -------------------------------------------------------------------------------------

    fun kiepDays(tier: Int): Int = ((tier - 1).coerceIn(2, 4) - if (setActive(9)) 1 else 0).coerceAtLeast(1)
    fun kiepMinutes(tier: Int): Int = 10 * (tier - 1).coerceIn(2, 4)

    /** Days of a forge trial met so far (the first and today only count when met). */
    fun kiepMet(slot: Ore): Int {
        if (!slot.inKiep) return 0
        val need = kiepMinutes(slot.tier)
        return (slot.kiep..today()).count { minutes(it) >= need } + slot.bonus
    }

    /** Each day: a trial passes, or a day missed between breaks it (Hộ Lô Phù and the set may spare it). */
    private fun checkKiep() {
        val today = today()
        for (slot in forge.toList()) {
            if (!slot.inKiep) continue
            val need = kiepMinutes(slot.tier)
            var failed = false
            var d = slot.kiep
            var spared = 0
            while (d <= today) {
                if (minutes(d) < need && d != slot.kiep && d != today) {
                    spared++
                    if (spared > slot.spared) {
                        when {
                            setActive(15) && slot.spared == 0 -> slot.spared++
                            items[Treasure.HO_LO_PHU] > 0 -> {
                                items[Treasure.HO_LO_PHU]--
                                talismansUsed++
                                slot.spared++
                                note("Hộ Lô Phù cháy thay, lò ${Treasure.TIER_NAMES[slot.tier]} không vỡ")
                            }
                            else -> { failed = true; break }
                        }
                    }
                }
                d++
            }
            val title = if (slot.isBook) books[slot.book]?.title ?: "" else "Lò luyện"
            when {
                failed -> {
                    phaLoFailed++
                    note("Phá lô thất bại: lò ${Treasure.TIER_NAMES[slot.tier]} nứt, lửa lui về bảy phần")
                    if (slot.isFinish) {
                        slot.kiep = today
                        slot.spared = 0
                    } else {
                        slot.kiep = -1
                        slot.words = slot.need * 7 / 10
                    }
                    touch()
                }
                kiepMet(slot) >= kiepDays(slot.tier) -> {
                    phaLoPassed++
                    gainLinhThach(30L * (slot.tier - 1))
                    release(slot, title, Treasure.TRIAL_MARK)
                    touch()
                }
            }
        }
    }

    // --- Đan dược and phù ---------------------------------------------------------------------------------

    /** Tụ Linh Đan into a slot of Thiên phẩm or above: 15% of its way (30% with Linh Đăng Chiếu Lộ). */
    fun useTuLinhDan(slot: Ore): Boolean {
        if (items[Treasure.TU_LINH_DAN] <= 0 || slot.tier < 3 || slot.inKiep || slot.isFinish) return false
        items[Treasure.TU_LINH_DAN]--
        pillsUsed++
        touch()
        slot.words += slot.need * (if (setActive(11)) 30 else 15) / 100
        if (slot.words >= slot.need) complete(slot, if (slot.isBook) books[slot.book]?.title ?: "" else "Lò luyện")
        checkTitles()
        touch()
        return true
    }

    /** Thiên Hỏa Phù: the forge burns twice as fast for 2 vạn words (4 vạn with Võ Lâm Ám Khí). */
    fun useThienHoaPhu(): Boolean {
        if (items[Treasure.THIEN_HOA_PHU] <= 0) return false
        items[Treasure.THIEN_HOA_PHU]--
        talismansUsed++
        fireWords += if (setActive(12)) 40_000 else 20_000
        checkTitles()
        touch()
        return true
    }

    /** Phá Cảnh Đan: a day of a hard thiên kiếp (Phá Đan Thành Anh and above), once per day of need. */
    fun canBreakPill() = items[Treasure.PHA_CANH_DAN] > 0 && inTrial() && nextGate() >= 3 && trialDays().first < trialNeed()

    fun useBreakPill(): Boolean {
        if (!canBreakPill()) return false
        items[Treasure.PHA_CANH_DAN]--
        pillsUsed++
        trialBonus++
        breakPillUsed = true
        note("Phá Cảnh Đan: bình cảnh ${Treasure.GATES[nextGate()]} lung lay")
        checkRealm()
        checkTitles()
        touch()
        return true
    }

    // --- Kỳ ngộ -------------------------------------------------------------------------------------------

    private fun kyNgo(book: Book) {
        val chance = (if (setActive(10)) 180 else 120) * (if (seekWords > 0) 3 else 1) * (if (beQuanTarget > 0) 2 else 1)
        if (Random.nextInt(1000) >= chance) return
        kyNgoCount++
        val sleeping = beasts.filter { it.spirit.isEmpty() }
        val r = Random.nextInt(100)
        val text = when {
            setActive(16) && r < 6 -> {
                hatch(Random.nextInt(2), book.title)
                "nhặt được pháp bảo ${beasts.last().name}"
            }
            r < 22 -> {
                val t = Treasure.rollTier()
                bag[t]++
                fillForge()
                "nhặt được khoáng thạch ${Treasure.TIER_NAMES[t]}"
            }
            r < 32 -> {
                val n = (20 + Random.nextInt(61)).toLong() * feastMultiplier()
                gainLinhThach(n)
                "nhặt được $n linh thạch"
            }
            r < 38 && sleeping.isNotEmpty() -> {
                val b = sleeping.random()
                b.spirit = Treasure.SPIRITS.random()
                unseen++
                "khí linh của ${b.name} thức tỉnh: «${b.spirit}»"
            }
            else -> {
                val i = rollItem()
                items[i]++
                "nhặt được ${Treasure.ITEM_NAMES[i]}"
            }
        }
        val d = LocalDate.now()
        kyNgoLog.add(0, "${d.dayOfMonth}/${d.monthValue} · $text")
        while (kyNgoLog.size > 40) kyNgoLog.removeAt(kyNgoLog.size - 1)
        note("Kỳ ngộ: $text")
        touch()
    }

    /** An item by its weight; the ones that cannot serve now are left out. */
    private fun rollItem(): Int {
        val highSlot = forge.any { it.tier >= 3 && !it.inKiep && !it.isFinish }
        val ok = (0 until Treasure.ITEMS).filter { i ->
            when (i) {
                Treasure.TU_LINH_DAN -> highSlot
                Treasure.PHA_CANH_DAN -> realmCap >= 2
                Treasure.THO_NGUYEN_DAN -> tuViLoss > 0
                Treasure.HO_LO_PHU, Treasure.DAN_LOI_PHU -> forge.any { it.tier >= 3 } || realmCap >= 3
                else -> true
            }
        }
        var roll = Random.nextInt(ok.sumOf { Treasure.ITEM_WEIGHTS[it] })
        for (i in ok) {
            roll -= Treasure.ITEM_WEIGHTS[i]
            if (roll < 0) return i
        }
        return Treasure.THIEN_HOA_PHU
    }

    // --- Using the other pills and talismans ----------------------------------------------------------

    private fun spend(i: Int) {
        items[i]--
        if (Treasure.isPill(i)) pillsUsed++ else talismansUsed++
        checkTitles()
        touch()
    }

    /** Slots a pill can go into: Bồi Nguyên (any forging), Cửu Chuyển (below Thiên phẩm), Tụ Linh (Thiên and up). */
    fun slotsFor(i: Int): List<Ore> = forge.filter { o ->
        !o.inKiep && !o.isFinish && when (i) {
            Treasure.TU_LINH_DAN -> o.tier >= 3
            Treasure.CUU_CHUYEN_DAN -> o.tier < 3
            Treasure.BOI_NGUYEN_DAN -> true
            else -> false
        }
    } + if (i == Treasure.DAN_LOI_PHU) forge.filter { it.inKiep } else emptyList()

    /** A pill into a slot (Bồi Nguyên Đan, Cửu Chuyển Kim Đan, Tụ Linh Đan) or a talisman on a trial (Dẫn Lôi Phù). */
    fun useOnSlot(i: Int, slot: Ore): Boolean {
        if (items[i] <= 0 || slot !in slotsFor(i)) return false
        val title = if (slot.isBook) books[slot.book]?.title ?: "" else "Lò luyện"
        when (i) {
            Treasure.TU_LINH_DAN -> return useTuLinhDan(slot)
            Treasure.BOI_NGUYEN_DAN -> {
                spend(i)
                slot.words += slot.need / 10
                if (slot.words >= slot.need) complete(slot, title)
            }
            Treasure.CUU_CHUYEN_DAN -> {
                spend(i)
                slot.words = slot.need
                complete(slot, title)
            }
            Treasure.DAN_LOI_PHU -> {
                spend(i)
                slot.bonus++
                checkKiep()
            }
        }
        return true
    }

    /** Ore grades the Tẩy Tủy Đan can lift. */
    fun liftableOres() = (0 until Treasure.TIERS - 1).filter { bag[it] > 0 }

    fun liftOre(tier: Int): Boolean {
        if (items[Treasure.TAY_TUY_DAN] <= 0 || tier !in liftableOres()) return false
        spend(Treasure.TAY_TUY_DAN)
        bag[tier]--
        bag[tier + 1]++
        fillForge()
        return true
    }

    fun awaken(beast: Beast): Boolean {
        if (items[Treasure.KHAI_LINH_DAN] <= 0 || beast.spirit.isNotEmpty()) return false
        spend(Treasure.KHAI_LINH_DAN)
        beast.spirit = Treasure.SPIRITS.random()
        note("Khí linh của ${beast.name} thức tỉnh: «${beast.spirit}»")
        return true
    }

    /** Treasures that have another of their kind (the newest of them can be melted). */
    fun duplicates(): List<Beast> = beasts.groupBy { it.species }.filterValues { it.size > 1 }.values.map { group -> group.maxBy { it.bornDay } }

    fun melt(beast: Beast): Boolean {
        if (items[Treasure.LUYEN_HOA_PHU] <= 0 || beast !in duplicates()) return false
        spend(Treasure.LUYEN_HOA_PHU)
        beasts.remove(beast)
        bag[beast.tier]++
        fillForge()
        return true
    }

    /** The talismans and pills with no target; returns the line to show, or null when it cannot serve now. */
    fun useSimple(i: Int): String? {
        if (items[i] <= 0) return null
        val today = today()
        when (i) {
            Treasure.THIEN_HOA_PHU -> return if (useThienHoaPhu()) "Thiên hỏa bừng lên: lò luyện cháy gấp đôi" else null
            Treasure.PHA_CANH_DAN -> return if (useBreakPill()) "Bình cảnh lung lay: thêm một ngày độ kiếp" else null
            Treasure.NGUNG_THAN_DAN -> {
                spend(i); bonusTuVi += 10_000; checkRealm()
                return "Thần thức ngưng tụ: tu vi thêm 1 vạn từ"
            }
            Treasure.HO_MACH_DAN -> {
                if (!inTrial()) return null
                spend(i); trialSpare++
                return "Tâm mạch được hộ: kỳ thiên kiếp này lỡ thêm một ngày vẫn không sao"
            }
            Treasure.THO_NGUYEN_DAN -> {
                if (tuViLoss <= 0) return null
                spend(i); tuViLoss /= 2; checkRealm()
                return "Thọ nguyên kéo dài: một nửa tu vi đã mất trở về"
            }
            Treasure.TAM_BAO_PHU -> {
                spend(i); seekWords += 30_000
                return "Tầm bảo: 3 vạn từ kế tiếp, kỳ ngộ gấp ba"
            }
            Treasure.BI_CANH_PHU -> {
                if (biCanhDone()) return null
                spend(i)
                val week = weekStart(today)
                if (biCanhBonusWeek != week) { biCanhBonusWeek = week; biCanhBonusDays = 0 }
                biCanhBonusDays++
                checkBiCanh()
                return "Bí cảnh mở thêm một ngày"
            }
            Treasure.DINH_THAN_PHU -> {
                spend(i); bonusMinutes[today] = (bonusMinutes[today] ?: 0) + 15
                checkKiep(); checkRealm(); checkBiCanh()
                return "Tâm định thần an: hôm nay tính thêm 15 phút"
            }
            Treasure.PHONG_AN_PHU -> {
                val day = ((today - 1) downTo (today - 7)).firstOrNull { (daySeconds[it] ?: 0) < 60 && it !in sealedDays } ?: return null
                spend(i); sealedDays.add(day)
                val d = LocalDate.ofEpochDay(day)
                return "Phong ấn ngày ${d.dayOfMonth}/${d.monthValue}: chuỗi ngày đọc được nối liền"
            }
            Treasure.TU_BAO_PHU -> {
                spend(i); giftLuck++
                return "Tụ bảo: lần quà 10 vạn từ kế tiếp gieo ba lần"
            }
        }
        return null
    }

    private fun hatch(tier: Int, bookTitle: String, spirit: String = "") {
        val beast = Beast(Random.nextLong(0, Long.MAX_VALUE / 2), tier, today(), bookTitle, Random.nextInt(Treasure.KINDS), spirit)
        beasts.add(beast)
        unseen++
        note("Pháp bảo xuất thế: ${beast.name} (${Treasure.TIER_NAMES[tier]})")
        checkSets()
    }

    private fun giveOre(tier: Int, why: String) {
        bag[tier]++
        fillForge()
        note("$why: khoáng thạch ${Treasure.TIER_NAMES[tier]}")
    }

    /** Words to forge an ore of a grade (Khai Thiên Phủ Đao: 10% fewer). */
    fun forgeNeed(tier: Int): Long {
        val base = Treasure.FORGE_WORDS[tier.coerceIn(0, Treasure.TIERS - 1)]
        return if (setActive(1)) base * 9 / 10 else base
    }

    /** Mồng một, rằm and the three days of Tết. */
    fun feastDay(): Boolean {
        val d = LocalDate.now()
        val l = Lunar.fromSolar(d.dayOfMonth, d.monthValue, d.year)
        return l.day == 1 || l.day == 15 || (!l.leap && l.month == 1 && l.day <= 3)
    }

    // --- Bộ pháp bảo ------------------------------------------------------------------------------

    fun setActive(set: Int) = set in 0 until Treasure.SETS && (setsDone shr set) and 1 == 1
    fun setsCount() = Integer.bitCount(setsDone)
    fun kinds(): Set<Int> = beasts.map { it.species }.toSet()

    /** A set whose four kinds are all owned turns on for good; the first time gives an Địa phẩm ore. */
    fun checkSets() {
        val have = kinds()
        for (set in 0 until Treasure.SETS) {
            if (setActive(set) || !Treasure.SET_MEMBERS[set].all { it in have }) continue
            setsDone = setsDone or (1 shl set)
            note("Gom đủ bộ ${Treasure.SET_NAMES[set]}: ${Treasure.SET_EFFECTS[set]}")
            giveOre(2, "Thưởng bộ pháp bảo")
            gainLinhThach(50)
            touch()
        }
    }

    /** Words of a book's stage for each grade (the firmware's stage pages × 135). */
    private val STAGE_WORDS = longArrayOf(27_000, 67_500, 135_000, 270_000, 675_000, 1_350_000)

    fun stageNeed(tier: Int): Long {
        val base = STAGE_WORDS[tier.coerceIn(0, Treasure.TIERS - 1)]
        return if (setActive(1)) base * 9 / 10 else base
    }

    /** What a book may still be put in the forge for: stage grades not taken, and the whole book. */
    fun bookGoals(book: Book): List<Pair<Int, Int>> {  // (goal, tier)
        val mine = forge.filter { it.book == book.path }
        if (mine.size >= SLOTS_PER_BOOK || forge.size >= FORGE_SLOTS || book.finishDay >= 0) return emptyList()
        val stages = (0 until Treasure.TIERS).filter { t -> mine.none { it.isStage && it.tier == t } }.map { Ore.GOAL_STAGE to it }
        val whole = if (mine.none { it.isFinish } && book.words > 0) listOf(Ore.GOAL_FINISH to Treasure.tierForBook(book.words)) else emptyList()
        return stages + whole
    }

    /** Puts a book being read into an empty slot for a stage of `tier` or for the whole book. */
    fun placeBook(book: Book, goal: Int, tier: Int): Boolean {
        if ((goal to tier) !in bookGoals(book)) return false
        forge.add(Ore(tier, 0, book.path, goal))
        touch()
        return true
    }

    /** Takes a slot out: a book leaves, an ore goes back to the bag. */
    fun takeOut(slot: Ore) {
        if (!forge.remove(slot)) return
        if (!slot.isBook) bag[slot.tier]++
        touch()
    }

    /** Puts an ore of the bag into an empty slot. */
    fun placeOre(tier: Int): Boolean {
        if (forge.size >= FORGE_SLOTS || bag[tier] <= 0) return false
        bag[tier]--
        forge.add(Ore(tier))
        touch()
        return true
    }

    /** Ores wait in the bag; up to three go into the forge by themselves, the best first. */
    fun fillForge() {
        while (forge.size < FORGE_SLOTS && forge.count { !it.isBook } < AUTO_ORES) {
            val t = (Treasure.TIERS - 1 downTo 0).firstOrNull { bag[it] > 0 } ?: return
            bag[t]--
            forge.add(Ore(t))
        }
    }

    // --- Cảnh giới and thiên kiếp ---------------------------------------------------------------

    private val TRIAL_MINUTES = intArrayOf(30, 30, 35, 40, 45, 45, 50, 55, 60)
    private val TRIAL_DAYS = intArrayOf(3, 3, 4, 4, 5, 5, 6, 6, 7)

    /** Words of tu vi: words read (+5% with Thượng Cổ Thần Khí), less what failed trials cost. */
    fun tuViRaw() = totalWords + bonusTuVi + (if (setActive(8)) totalWords / 20 else 0) + (if (setActive(17)) totalWords / 20 else 0)
    fun tuVi() = (tuViRaw() - tuViLoss).coerceAtLeast(0)

    private fun rawLevel() = Realm.level(tuVi())

    /** The realm level reached (0..35): the words may go further while a trial is pending. */
    fun level(): Int {
        val raw = rawLevel()
        return if (realmCap >= 9) raw else minOf(raw, realmCap.coerceAtLeast(0) * 3 + 2)
    }

    fun inTrial() = realmCap in 0..8 && Realm.realmOf(rawLevel()) > realmCap
    fun trialNeed() = TRIAL_DAYS[realmCap.coerceIn(0, 8)]
    fun trialMinutes() = TRIAL_MINUTES[realmCap.coerceIn(0, 8)] - if (setActive(13)) 5 else 0
    fun minutes(day: Long) = (daySeconds[day] ?: 0) / 60 + (bonusMinutes[day] ?: 0)

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
        var forgiven = (if (setActive(4)) 1 else 0) + trialSpare
        for (d in trialStart..today) {
            if (minutes(d) >= need) run++
            else if (d != trialStart && d != today) {
                if (forgiven > 0) forgiven-- else return run to true
            }
        }
        return (run + trialBonus) to false
    }

    fun checkRealm() {
        if (realmCap < 0) return
        if (inTrial()) {
            val today = today()
            if (trialStart < 0) {
                trialStart = today
                trialBonus = 0
                trialSpare = 0
                note("Thiên kiếp ${Treasure.GATES[nextGate()]}: ${trialNeed()} ngày, mỗi ngày ${trialMinutes()} phút")
                touch()
            }
            val (days, failed) = trialDays()
            if (failed) {
                // Độ kiếp thất bại: tu vi falls to one ninth below the gate.
                val lo = Realm.floor(realmCap * 3)
                val hi = Realm.floor(realmCap * 3 + 3)
                val target = hi - (hi - lo) / 9
                if (tuViRaw() > target) tuViLoss = tuViRaw() - target
                trialStart = -1
                trialBonus = 0
                trialSpare = 0
                failDay = today
                failCount++
                note("Độ kiếp thất bại: tu vi lui về ${Realm.van(target)} vạn từ")
                touch()
            } else if (days >= trialNeed()) {
                realmCap++
                trialStart = -1
                trialBonus = 0
                trialSpare = 0
                trialPassed++
                if (failCount > 0) passedAfterFail = true
                gainLinhThach(50L * realmCap)
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

    const val BI_CANH_MINUTES = 30
    fun biCanhNeed() = if (setActive(2)) 4 else 5

    fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()
    fun biCanhDays(): Int {
        val today = today()
        val week = weekStart(today)
        return (week..today).count { minutes(it) >= BI_CANH_MINUTES } + if (biCanhBonusWeek == week) biCanhBonusDays else 0
    }
    fun biCanhDone() = biCanhWeek == weekStart(today())

    private fun checkBiCanh() {
        val week = weekStart(today())
        if (biCanhWeek == week || biCanhDays() < biCanhNeed()) return
        biCanhWeek = week
        biCanhCount++
        val lift = if (setActive(14)) 1 else 0
        giveOre((maxOf(Treasure.rollTier(), 1) + lift).coerceAtMost(Treasure.TIERS - 1), "Qua bí cảnh tuần")  // never below Huyền
        if (setActive(5)) giveOre(maxOf(Treasure.rollTier(), 1), "Phong Thần Bảo Lục")
        gainLinhThach(20)
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
        val level = level()
        val readDays = daySeconds.values.count { it >= 60 }
        val months = daySeconds.filterValues { it >= 600 }.keys.map { LocalDate.ofEpochDay(it).monthValue }.toSet().size
        val sacred = daySeconds.filterValues { it >= 600 }.keys.count { lunarDay(it).let { l -> l == 1 || l == 15 } }
        val earned = booleanArrayOf(
            // Tu hành
            totalWords >= 10_000, totalWords >= 100_000, totalWords >= 1_000_000, totalWords >= 5_000_000,
            totalWords >= 10_000_000, realmCap >= 9,
            level >= 6, level >= 9, level >= 12, level >= 21, totalWords >= 20_000_000, level >= Realm.LEVELS - 1,
            // Đạo tâm
            streak >= 7, streak >= 30, streak >= 100, streak >= 365, trialPassed >= 1, passedAfterFail,
            streak >= 14, streak >= 60, streak >= 200, trialPassed >= 9, failCount >= 3, readDays >= 300,
            // Thời khắc
            night >= 36_000, dawn >= 18_000, hourSeconds[0] >= 3_600, bestSession >= 7_200,
            (daySeconds.values.maxOrNull() ?: 0) >= 14_400, tet,
            hourSeconds[11] + hourSeconds[12] >= 18_000, hourSeconds[17] + hourSeconds[18] >= 36_000, bestSession >= 14_400,
            (daySeconds.values.maxOrNull() ?: 0) >= 28_800, months >= 12, sacred >= 12,
            // Kinh các
            finished.size >= 1, finished.size >= 10, finished.size >= 50, quickest in 1..3, tangCount >= 100,
            books.values.count { it.lastOpened > 0 } >= 30,
            finished.size >= 25, finished.size >= 100, finished.any { it.words >= 1_000_000 }, quickest == 1L, tangCount >= 500,
            books.values.count { it.lastOpened > 0 } >= 100,
            // Pháp bảo
            beasts.isNotEmpty(), kinds >= 18, kinds >= 36, beasts.any { it.tier == Treasure.TIERS - 1 }, beasts.size >= 30, setsDone != 0,
            kinds >= 54, kinds >= Treasure.SPECIES, setsCount() >= 9, beasts.map { it.tier }.toSet().size >= Treasure.TIERS,
            phaLoPassed >= 5, beasts.count { it.spirit.isNotEmpty() && it.spirit != Treasure.TRIAL_MARK } >= 5,
            // Kỳ duyên
            giftsAwarded >= 20, beQuanDone >= 20 || totalSeconds >= 360_000, biCanhCount >= 10, trungThu, birthday, trialPassed >= 5,
            kyNgoCount >= 20, kyNgoCount >= 100, pillsUsed >= 10, talismansUsed >= 10, breakPillUsed, biCanhCount >= 30,
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

    private val lunarDays = HashMap<Long, Int>()
    private fun lunarDay(day: Long): Int = lunarDays.getOrPut(day) {
        val d = LocalDate.ofEpochDay(day)
        Lunar.fromSolar(d.dayOfMonth, d.monthValue, d.year).day
    }

    // --- Stats ------------------------------------------------------------------------------------

    /** Days in a row with reading, ending today or yesterday. */
    fun streak(): Int {
        fun read(day: Long) = (daySeconds[day] ?: 0) >= 60 || day in sealedDays
        var d = today()
        if (!read(d)) d--
        var n = 0
        val shielded = HashSet<Long>()  // weeks whose hộ pháp phù is used
        while (true) {
            if (read(d)) {
                n++
            } else if (setActive(3) && n > 0 && read(d - 1) && shielded.add(weekStart(d))) {
                // Hộ pháp phù: the day is forgiven, the streak goes on.
            } else {
                break
            }
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
