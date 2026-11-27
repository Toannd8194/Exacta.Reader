package com.exacta.reader.data

import android.content.Context
import com.exacta.reader.game.Treasure
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
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

    val percent get() = (progress * 100).toInt().coerceIn(0, 100)

    fun toJson() = JSONObject().apply {
        put("path", path); put("title", title); put("author", author); put("words", words)
        put("progress", progress); put("maxProgress", maxProgress); put("wordsRead", wordsRead); put("seconds", seconds)
        put("firstDay", firstDay); put("lastDay", lastDay); put("finishDay", finishDay); put("lastOpened", lastOpened)
        put("locator", locator); put("cover", cover); put("forged", forged)
    }

    companion object {
        fun from(o: JSONObject) = Book(o.getString("path")).apply {
            title = o.optString("title", title); author = o.optString("author"); words = o.optLong("words", -1)
            progress = o.optDouble("progress", 0.0); maxProgress = o.optDouble("maxProgress", 0.0)
            wordsRead = o.optLong("wordsRead"); seconds = o.optLong("seconds"); firstDay = o.optLong("firstDay", -1)
            lastDay = o.optLong("lastDay", -1); finishDay = o.optLong("finishDay", -1); lastOpened = o.optLong("lastOpened")
            locator = o.optString("locator"); cover = o.optString("cover"); forged = o.optBoolean("forged")
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
    /** A line for the reader's footer (a treasure forged, an ore given), shown once. */
    var note = ""

    // Reader preferences.
    var fontSize = 1.1
    var lineHeight = 1.5
    var margins = 1.2
    var fontFamily = "Bitter"   // "Original", "Bitter", "serif", "sans-serif"
    var justify = true
    var swapTaps = false

    private var dirty = false

    fun today(): Long = LocalDate.now().toEpochDay()

    fun load(context: Context) {
        file = File(context.filesDir, "exacta-state.json")
        if (!file.exists()) return
        try {
            val o = JSONObject(file.readText())
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
            o.optJSONObject("prefs")?.let {
                fontSize = it.optDouble("fontSize", fontSize); lineHeight = it.optDouble("lineHeight", lineHeight)
                margins = it.optDouble("margins", margins); fontFamily = it.optString("fontFamily", fontFamily)
                justify = it.optBoolean("justify", justify); swapTaps = it.optBoolean("swapTaps", swapTaps)
            }
        } catch (e: Exception) {
            com.exacta.reader.ExactaApp.log("state load failed: $e")
        }
    }

    fun touch() {
        dirty = true
    }

    fun save() {
        if (!dirty || !::file.isInitialized) return
        val o = JSONObject().apply {
            put("version", 1)
            put("books", JSONArray().apply { books.values.forEach { put(it.toJson()) } })
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
            put("prefs", JSONObject().put("fontSize", fontSize).put("lineHeight", lineHeight).put("margins", margins)
                .put("fontFamily", fontFamily).put("justify", justify).put("swapTaps", swapTaps))
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
    fun current(): Book? = books.values.filter { it.lastOpened > 0 && File(it.path).exists() }.maxByOrNull { it.lastOpened }

    // --- Reading ------------------------------------------------------------------------------

    fun addSeconds(book: Book, seconds: Long) {
        if (seconds <= 0) return
        val d = today()
        daySeconds[d] = (daySeconds[d] ?: 0) + seconds
        book.seconds += seconds
        totalSeconds += seconds
        book.lastDay = d
        if (book.firstDay < 0) book.firstDay = d
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
        if (progression >= FINISH && book.finishDay < 0) {
            book.finishDay = today()
            if (!book.forged && book.words > 0) {
                book.forged = true
                val tier = Treasure.tierForBook(book.words)
                hatch(tier, book.title)
            }
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
            if (note.isEmpty()) note = "Quà 10 vạn từ: khoáng thạch ${Treasure.TIER_NAMES[tier]}"
        }
    }

    private fun hatch(tier: Int, bookTitle: String) {
        val beast = Beast(Random.nextLong(0, Long.MAX_VALUE / 2), tier, today(), bookTitle)
        beasts.add(beast)
        unseen++
        note = "Pháp bảo xuất thế: ${beast.name} (${Treasure.TIER_NAMES[tier]})"
    }

    /** Ores wait in the bag while the forge is full; the best one goes in first. */
    fun fillForge() {
        while (forge.size < FORGE_SLOTS) {
            val t = (Treasure.TIERS - 1 downTo 0).firstOrNull { bag[it] > 0 } ?: return
            bag[t]--
            forge.add(Ore(t))
        }
    }

    fun takeNote(): String = note.also { note = "" }

    // --- Stats ---------------------------------------------------------------------------------

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
}
