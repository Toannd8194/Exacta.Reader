package com.exacta.reader.data

import com.exacta.reader.game.Lunar
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 2.8 Tàng Kinh Lưu Ảnh: the reading journal kept day by day for part two (Lục Giới).
 *
 * Each giới of part two lives on one way of reading (the night, long sittings, reading in order,
 * rereading, many authors). What the app kept until 2.7 was a sum (minutes by hour for all days,
 * the longest sitting of a day); this keeps, for every day:
 *  - the seconds read in each hour,
 *  - every sitting and its length,
 *  - the seconds spent in each book,
 *  - the words read again (a book read through, read from the start once more),
 *  - the words read in a book come back to after 90 days or more,
 *  - the tâm ma (highest) and the balance of the five measures at the end of a sitting;
 * and a list of events: a book come back to, a reread finished, a failure, a rebirth.
 *
 * From it [estimate] counts, by the rules of part two, what each giới's resource would have been.
 */
object NhatKy {
    class Day {
        val hours = LongArray(24)
        val sittings = ArrayList<Int>()
        val books = HashMap<Int, Long>()
        var reread = 0L
        var back = 0L
        var tamMa = 0
        var balance = 100

        fun seconds() = hours.sum()
        fun toJson(): JSONObject = JSONObject().apply {
            put("h", JSONObject().apply { hours.forEachIndexed { i, s -> if (s > 0) put(i.toString(), s) } })
            if (sittings.isNotEmpty()) put("s", JSONArray().apply { sittings.forEach { put(it) } })
            if (books.isNotEmpty()) put("b", JSONObject().apply { books.forEach { (id, s) -> put(id.toString(), s) } })
            if (reread > 0) put("r", reread)
            if (back > 0) put("k", back)
            put("t", tamMa); put("c", balance)
        }

        companion object {
            fun from(o: JSONObject) = Day().apply {
                o.optJSONObject("h")?.let { h -> for (k in h.keys()) k.toIntOrNull()?.takeIf { it in 0..23 }?.let { hours[it] = h.getLong(k) } }
                o.optJSONArray("s")?.let { a -> for (i in 0 until a.length()) sittings.add(a.getInt(i)) }
                o.optJSONObject("b")?.let { b -> for (k in b.keys()) k.toIntOrNull()?.let { books[it] = b.getLong(k) } }
                reread = o.optLong("r"); back = o.optLong("k"); tamMa = o.optInt("t"); balance = o.optInt("c", 100)
            }
        }
    }

    /** An event: its day, kind, the book's id (or 0). */
    class Event(val day: Long, val kind: Int, val book: Int)

    const val E_BACK = 0      // a book left 90 days or more, opened again
    const val E_REREAD = 1    // a book read through once more
    const val E_FAIL = 2      // a breakthrough, a trial or a kiếp failed
    const val E_REBORN = 3    // tọa hóa, reborn

    const val BACK_DAYS = 90
    private const val PAGE_MAX = 450L
    private const val PAGE_SWIPE = 600L
    private const val FINISH = 0.95

    var started = false
    var since = -1L               // the first day kept
    var nextId = 1
    val days = HashMap<Long, Day>()
    val events = ArrayList<Event>()

    fun reset() {
        started = false; since = -1; nextId = 1; days.clear(); events.clear()
    }

    private fun today() = Store.today()
    fun day(d: Long = today()): Day = days.getOrPut(d) { Day() }

    fun idOf(book: Book): Int {
        if (book.id <= 0) {
            book.id = nextId++
            Store.touch()
        }
        return book.id
    }

    fun bookById(id: Int): Book? = Store.books.values.firstOrNull { it.id == id }

    fun event(kind: Int, book: Book? = null) {
        if (!started) return
        events.add(Event(today(), kind, book?.let { idOf(it) } ?: 0))
        while (events.size > 2000) events.removeAt(0)
        Store.touch()
    }

    // --- Hooks ------------------------------------------------------------------------------------

    /** Seconds read just now, in `hour`; called before the book's last day moves to today. */
    fun onSeconds(book: Book, seconds: Long, hour: Int) {
        if (!started) return
        val d = today()
        val day = day(d)
        day.hours[hour.coerceIn(0, 23)] += seconds
        val id = idOf(book)
        day.books[id] = (day.books[id] ?: 0) + seconds
        day.tamMa = maxOf(day.tamMa, TuLuyen.tamMa)
        if (book.finishDay < 0 && book.lastDay >= 0 && d - book.lastDay >= BACK_DAYS && book.maxProgress >= 0.02) {
            book.backDay = d
            event(E_BACK, book)
        }
    }

    /** A sitting is over: its length, and the balance of the measures now. */
    fun onSitting(seconds: Long) {
        if (!started || seconds < 60) return
        val day = day()
        day.sittings.add(seconds.toInt())
        day.tamMa = maxOf(day.tamMa, TuLuyen.tamMa)
        day.balance = balance()
        Store.touch()
    }

    /** Words credited (read forward): counted as come-back words when the book was left long. */
    fun onCredit(book: Book, words: Long) {
        if (!started || book.backDay < 0 || book.finishDay >= 0) return
        day().back += words
    }

    /**
     * A move inside a book already read through: page by page from where the reread stands, the
     * words are read again (jumps only move the mark, as for the first reading).
     */
    fun onMove(book: Book, progression: Double, pageTurn: Boolean) {
        if (!started || book.finishDay < 0 || book.words <= 0) return
        if (progression > book.maxProgress) return
        val step = progression - book.rereadMax
        if (step <= 0) return
        val words = (step * book.words).toLong()
        if (pageTurn || words <= PAGE_SWIPE) day().reread += minOf(words, PAGE_MAX)
        book.rereadMax = progression
        if (progression >= FINISH) {
            book.rereads++
            book.rereadDay = today()
            book.rereadMax = 0.0
            event(E_REREAD, book)
        }
        Store.touch()
    }

    // --- Measures ---------------------------------------------------------------------------------

    /**
     * Cửu Tiêu's balance (60..140): căn cơ, tâm cảnh, thần thức, thể phách, the days read in 30,
     * each on 0..1; a measure that lags behind the mean of the other four lowers it.
     */
    fun balance(): Int = balanceOf(measures())

    /** The five measures, each on 0..1: căn cơ, tâm cảnh, thần thức, thể phách, days read in 30. */
    fun measures(): DoubleArray {
        val t = today()
        return doubleArrayOf(
            TuLuyen.canCo() / 100.0,
            TuLuyen.tamCanh() / 100.0,
            TuLuyen.trong() / (TuLuyen.THAN_THUC_AT.size - 1.0),
            TuLuyen.theTier() / (TuLuyen.THE_AT.size - 1.0),
            (t - 29..t).count { Store.minutes(it) >= 1 } / 30.0,
        )
    }

    fun balanceOf(v: DoubleArray): Int {
        var lag = 0.0
        for (i in v.indices) {
            val others = (v.sum() - v[i]) / (v.size - 1)
            lag += (others - v[i]).coerceAtLeast(0.0)
        }
        return (140 - 160 * lag / v.size).toInt().coerceIn(60, 140)
    }

    fun nightSeconds(d: Day) = listOf(22, 23, 0, 1, 2, 3).sumOf { d.hours[it] }
    fun dawnSeconds(d: Day) = listOf(4, 5, 6).sumOf { d.hours[it] }

    private fun fullMoon(day: Long): Boolean {
        val d = LocalDate.ofEpochDay(day)
        return Lunar.fromSolar(d.dayOfMonth, d.monthValue, d.year).day == 15
    }

    private fun key(s: String) = s.trim().lowercase()

    // --- Part two: what each giới would have given --------------------------------------------------

    val RESOURCE = arrayOf("tiên lực", "ma khí", "công đức", "khí huyết", "hồn lực", "văn khí")
    /** The way of reading each giới lives on, in a line. */
    val WAY = arrayOf(
        "Mỗi 2.500 chữ, nhân độ cân bằng của năm thước đo.",
        "Đọc từ 22 tới 4 giờ, đêm rằm gấp đôi; tâm ma từ 40; ngã là có lời.",
        "Ngày đủ 30 phút không nhảy chương, chuỗi ngày, sách đọc trọn.",
        "Buổi đọc liền từ 45 phút; từ 90 phút gấp đôi, từ 120 phút gấp ba.",
        "Đọc lại sách đã trọn, đọc tiếp sách bỏ dở từ 90 ngày.",
        "Tác giả mới đọc trọn, thể loại và ngôn ngữ mới.",
    )
    /** Days the journal did not keep count this much a day read (the design's 1,5 points). */
    const val FILL_TENTHS = 15

    /** What [estimate] found: points by the rules, the days measured, the days filled at the mean. */
    class Estimate(val tenths: Long, val measured: Int, val filled: Int) {
        val points get() = ((tenths + 5) / 10).toInt()
    }

    /** The resource of giới `g` from `from` to `to` (days), by the rules of part two. */
    fun estimate(g: Int, from: Long, to: Long): Estimate {
        if (to < from || from < 0) return Estimate(0, 0, 0)
        var tenths = 0L     // tenths of a point, rounded at the end
        var measured = 0
        var filled = 0
        val start = if (since < 0) to + 1 else maxOf(from, since)
        for (d in from until minOf(start, to + 1)) if (Store.minutes(d) >= 1) {
            filled++
            tenths += FILL_TENTHS
        }
        var chain = 0
        for (d in (start - 1 downTo start - 400)) if (Store.minutes(d) >= 1 || d in Store.sealedDays) chain++ else break
        for (d in start..to) {
            val day = days[d]
            val read = Store.minutes(d) >= 1 || d in Store.sealedDays
            chain = if (read) chain + 1 else 0
            if (day == null) continue
            measured++
            tenths += 10L * when (g) {
                0 -> ((Store.dayWords[d] ?: 0) * day.balance / 100 / 2_500).toInt()
                1 -> (nightSeconds(day) / 600 * (if (fullMoon(d)) 2 else 1)).toInt() + (if (day.tamMa >= 40) day.tamMa / 10 else 0)
                2 -> {
                    val jumps = TuLuyen.dayJumps[d] ?: 0
                    if (day.tamMa >= 50) 0 else (if (Store.minutes(d) >= 30 && jumps == 0) 2 else 0) - 2 * jumps + (if (chain > 0 && chain % 7 == 0) 1 else 0)
                }
                3 -> day.sittings.sumOf { s -> if (s < 45 * 60) 0 else s / 600 * (if (s >= 120 * 60) 3 else if (s >= 90 * 60) 2 else 1) }
                4 -> {
                    val fresh = ((Store.dayWords[d] ?: 0) - day.back).coerceAtLeast(0)
                    ((day.reread + day.back) / 2_000 + fresh / 8_000).toInt()
                }
                else -> if (day.books.keys.any { id -> bookById(id)?.let { b -> b.author.isNotBlank() && authorFirstFinish(b.author) !in 0..d } == true }) 1 else 0
            }.coerceAtLeast(0)
        }
        tenths += 10L * events(g, from, to)
        return Estimate(tenths, measured, filled)
    }

    /** The day the first book of `author` was read through, or -1. */
    private fun authorFirstFinish(author: String): Long {
        val a = key(author)
        return Store.books.values.filter { it.finishDay >= 0 && key(it.author) == a }.minOfOrNull { it.finishDay } ?: -1
    }

    /** Points from what happened rather than what was read each day: books finished, failures. */
    private fun events(g: Int, from: Long, to: Long): Int {
        val inRange = { d: Long -> d in from..to }
        val finished = Store.books.values.filter { it.finishDay >= 0 }.sortedBy { it.finishDay }
        return when (g) {
            1 -> 20 * events.count { it.kind == E_FAIL && inRange(it.day) }
            2 -> finished.filter { inRange(it.finishDay) }.map { if (it.backDay in 0..it.finishDay) 35 else 20 }.sum()
            4 -> 30 * finished.count { inRange(it.finishDay) && it.backDay in 0..it.finishDay } + 10 * events.count { it.kind == E_REBORN && inRange(it.day) }
            5 -> {
                val byAuthor = HashMap<String, Int>()
                val genres = HashSet<String>()
                var pts = 0
                for (b in finished) {
                    val a = key(b.author)
                    val n = if (a.isEmpty()) 3 else byAuthor.getOrDefault(a, 0).also { byAuthor[a] = it + 1 }
                    val newKinds = (b.genres() + b.langs().map { "lang:$it" }).count { genres.add(it) }
                    if (inRange(b.finishDay)) pts += intArrayOf(40, 15, 8).getOrElse(n) { 4 } + 25 * newKinds
                }
                pts
            }
            else -> 0
        }
    }

    // --- The last 30 days, as the Niên kỷ shows them -------------------------------------------------

    class Summary(
        val days: Int, val night: Long, val dawn: Long, val sittings: Int, val long45: Int, val long90: Int, val longest: Int,
        val reread: Long, val back: Long, val comeBack: Int, val books: Int, val authors: Int, val genres: Int, val langs: Int,
    )

    fun summary(from: Long, to: Long): Summary {
        val ds = (from..to).mapNotNull { days[it] }
        val ids = ds.flatMap { it.books.keys }.toSet()
        val bs = ids.mapNotNull { bookById(it) }
        val sit = ds.flatMap { it.sittings }
        return Summary(
            ds.size, ds.sumOf { nightSeconds(it) }, ds.sumOf { dawnSeconds(it) }, sit.size, sit.count { it >= 45 * 60 }, sit.count { it >= 90 * 60 },
            sit.maxOrNull() ?: 0, ds.sumOf { it.reread }, ds.sumOf { it.back }, events.count { it.kind == E_BACK && it.day in from..to },
            bs.size, bs.map { key(it.author) }.filter { it.isNotEmpty() }.toSet().size, bs.flatMap { it.genres() }.toSet().size,
            bs.flatMap { it.langs() }.toSet().size,
        )
    }

    // --- Saving -----------------------------------------------------------------------------------

    /** First run: ids for the books there are; the journal starts today. */
    fun migrate() {
        reset()
        started = true
        since = today()
        Store.books.values.sortedBy { it.firstDay }.forEach { idOf(it) }
        Store.touch()
    }

    fun toJson(): JSONObject = JSONObject().put("since", since).put("next", nextId)
        .put("days", JSONObject().apply { days.forEach { (d, v) -> put(d.toString(), v.toJson()) } })
        .put("ev", JSONArray().apply { events.forEach { put(JSONArray().put(it.day).put(it.kind).put(it.book)) } })

    fun read(o: JSONObject) {
        reset()
        started = true
        since = o.optLong("since", -1); nextId = o.optInt("next", 1)
        o.optJSONObject("days")?.let { a -> for (k in a.keys()) k.toLongOrNull()?.let { days[it] = Day.from(a.getJSONObject(k)) } }
        o.optJSONArray("ev")?.let { a -> for (i in 0 until a.length()) a.getJSONArray(i).let { events.add(Event(it.getLong(0), it.getInt(1), it.optInt(2))) } }
        // Ids handed out before a restore must stay unique.
        nextId = maxOf(nextId, (Store.books.values.maxOfOrNull { it.id } ?: 0) + 1)
    }
}
