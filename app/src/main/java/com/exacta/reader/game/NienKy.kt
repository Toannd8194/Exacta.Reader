package com.exacta.reader.game

import com.exacta.reader.data.Store
import java.time.LocalDate

/**
 * Niên kỷ: the summary of a lunar year from the reading journal, as on Exacta.OS Omega. A year runs
 * from mồng một tháng Giêng to the day before the next one; the year still going ends today.
 */
object NienKy {
    class Report(
        val lunarYear: Int,
        val from: Long,
        val to: Long,
        val current: Boolean,
        val partial: Boolean,            // the journal starts after the year began
        val seconds: Long,
        val words: Long,
        val daysRead: Int,
        val daysTotal: Int,
        val bestDay: Long,
        val bestDaySeconds: Long,
        val bestStreak: Int,
        val bestMonth: Int,              // solar month 1..12, 0 none
        val bestMonthYear: Int,
        val bestMonthSeconds: Long,
        val booksFinished: Int,
        val longestBook: String,
        val longestBookSeconds: Long,
        val treasures: Int,
        val bestTreasure: String,
        val titles: Int,
        val realmFrom: Int,
        val realmTo: Int,
    )

    private fun lunar(day: Long): Lunar.Date {
        val d = LocalDate.ofEpochDay(day)
        return Lunar.fromSolar(d.dayOfMonth, d.monthValue, d.year)
    }

    /** First day of the lunar year holding `day`, and that year. */
    fun yearStart(day: Long): Pair<Long, Int> {
        var d = day
        repeat(16) {
            val l = lunar(d)
            val first = d - (l.day - 1)
            if (l.month == 1 && !l.leap) return first to l.year
            d = first - 1  // the last day of the month before
        }
        return day to 0
    }

    /** The lunar year just ended while today is in the first half of tháng Giêng, else 0. */
    fun due(): Int {
        val l = lunar(Store.today())
        return if (!l.leap && l.month == 1 && l.day <= 15) l.year - 1 else 0
    }

    /** How many years back have a journal: 1 (this year only) and up. */
    fun yearsWithData(max: Int = 4): Int {
        val first = (Store.daySeconds.keys + Store.dayWords.keys).minOrNull() ?: return 1
        var n = 1
        var (start, _) = yearStart(Store.today())
        while (n < max && start > first) {
            start = yearStart(start - 1).first
            n++
        }
        return n
    }

    /** back 0: this lunar year so far; 1: the one before, and so on. */
    fun report(back: Int): Report {
        val today = Store.today()
        var (start, year) = yearStart(today)
        var end = today
        repeat(back) {
            end = start - 1
            val prev = yearStart(end)
            start = prev.first
            year = prev.second
        }
        val first = (Store.daySeconds.keys + Store.dayWords.keys).minOrNull()
        var seconds = 0L
        var words = 0L
        var daysRead = 0
        var bestDay = -1L
        var bestDaySeconds = 0L
        var run = 0
        var bestStreak = 0
        val months = HashMap<Int, Long>()  // year * 12 + month - 1
        for (d in start..end) {
            val s = Store.daySeconds[d] ?: 0
            seconds += s
            words += Store.dayWords[d] ?: 0
            val read = s >= 60
            if (read) daysRead++
            run = if (read) run + 1 else 0
            bestStreak = maxOf(bestStreak, run)
            if (s > bestDaySeconds) {
                bestDaySeconds = s
                bestDay = d
            }
            val date = LocalDate.ofEpochDay(d)
            val key = date.year * 12 + date.monthValue - 1
            months[key] = (months[key] ?: 0) + s
        }
        val busiest = months.maxByOrNull { it.value }?.takeIf { it.value > 0 }
        val finished = Store.books.values.count { it.finishDay in start..end }
        val longest = Store.books.values.filter { it.lastDay in start..end && it.seconds > 0 }.maxByOrNull { it.seconds }
        val made = Store.beasts.filter { it.bornDay in start..end }
        val best = made.maxWithOrNull(compareBy({ it.tier }, { it.bornDay }))
        val titles = Store.titleDays.values.count { it in start..end }
        // The realm at each end, from the words read then (past trial caps are not kept).
        val after = if (end < today) Store.wordsBetween(end + 1, today) else 0
        val atEnd = (Store.tuVi() - after).coerceAtLeast(0)
        val atStart = (atEnd - words).coerceAtLeast(0)
        val realmNow = Store.level()
        val realmTo = if (back == 0) realmNow else minOf(Realm.level(atEnd), realmNow)
        return Report(
            lunarYear = year, from = start, to = end, current = back == 0,
            partial = first != null && first > start,
            seconds = seconds, words = words, daysRead = daysRead, daysTotal = (end - start + 1).toInt(),
            bestDay = bestDay, bestDaySeconds = bestDaySeconds, bestStreak = bestStreak,
            bestMonth = busiest?.let { it.key % 12 + 1 } ?: 0, bestMonthYear = busiest?.let { it.key / 12 } ?: 0,
            bestMonthSeconds = busiest?.value ?: 0,
            booksFinished = finished, longestBook = longest?.title ?: "", longestBookSeconds = longest?.seconds ?: 0,
            treasures = made.size, bestTreasure = best?.let { "${it.name} · ${Treasure.CLANS[it.tier]}" } ?: "",
            titles = titles, realmFrom = minOf(Realm.level(atStart), realmTo), realmTo = realmTo,
        )
    }
}
