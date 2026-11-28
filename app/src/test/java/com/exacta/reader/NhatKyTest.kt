package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.Book
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.NhatKy
import com.exacta.reader.data.PhuLuc
import com.exacta.reader.data.Store
import com.exacta.reader.data.TeLuyen
import com.exacta.reader.data.TienDinh
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TranPhap
import com.exacta.reader.data.TrangBi
import com.exacta.reader.data.TuLuyen
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 2.8: the journal kept day by day, and what part two would count from it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NhatKyTest {
    private val today get() = Store.today()

    @Before
    fun clean() {
        Store.books.clear(); Store.daySeconds.clear(); Store.dayWords.clear(); Store.forge.clear(); Store.beasts.clear()
        Store.bag.fill(0); Store.items.fill(0); Store.bonusMinutes.clear(); Store.sealedDays.clear(); Store.titleDays.clear()
        Store.totalWords = 0; Store.totalSeconds = 0; Store.bonusTuVi = 0; Store.tuViLoss = 0; Store.setsDone = 0
        Store.realmCap = 0; Store.realmSeen = 0; Store.trialStart = -1; Store.trialPassed = 0; Store.failCount = 0
        Store.trialBonus = 0; Store.trialSpare = 0; Store.giftsAwarded = 0; Store.linhThach = 0; Store.kyNgoWords = 0; Store.kyNgoTarget = Long.MAX_VALUE / 2
        Store.hourSeconds.fill(0)
        Store.luckRng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = until - 1 }
        TuLuyen.reset(); TuLuyen.started = true; TuLuyen.lastTick = today - 1
        DaoPhap.reset(); DongPhu.reset(); TongMon.reset(); BiCanh.reset(); DaoLo.reset(); LinhGioi.reset(); DoKiep.reset(); TienDinh.reset()
        TeLuyen.reset(); TranPhap.reset(); TrangBi.reset(); PhuLuc.reset()
        NhatKy.reset(); NhatKy.started = true; NhatKy.since = today - 10
    }

    private fun book(path: String, words: Long = 200_000): Book = Store.book(path).also { it.words = words; it.author = "Tác giả $path" }

    @Test
    fun hoursSittingsAndBooksAreKeptByDay() {
        val b = book("a")
        NhatKy.onSeconds(b, 600, 23)
        NhatKy.onSeconds(b, 300, 5)
        Store.endSession(50 * 60L)
        Store.endSession(30)                       // under a minute: not a sitting
        val d = NhatKy.day()
        assertEquals(600L, d.hours[23])
        assertEquals(600L, NhatKy.nightSeconds(d))
        assertEquals(300L, NhatKy.dawnSeconds(d))
        assertEquals(listOf(3000), d.sittings)
        assertEquals(900L, d.books[b.id])
        assertTrue(b.id > 0)
        // Hồng Hoang: a 50-minute sitting is 5 khí huyết; 100 minutes ×2; 130 minutes ×3.
        assertEquals(5, NhatKy.estimate(3, today, today).points)
        d.sittings.add(100 * 60); d.sittings.add(130 * 60)
        assertEquals(5 + 20 + 39, NhatKy.estimate(3, today, today).points)
        // Huyết Nguyệt: 10 minutes at night is one ma khí (two on the full moon).
        assertTrue(NhatKy.estimate(1, today, today).points in 1..2)
    }

    @Test
    fun aBookReadAgainCountsItsWords() {
        val b = book("b", 100_000)
        b.finishDay = today - 30; b.maxProgress = 0.97; b.progress = 0.97
        Store.moveTo(b, 0.0)                       // back to the start by the contents
        assertEquals(0L, NhatKy.day().reread)
        Store.moveTo(b, 0.004, pageTurn = true)    // a page: 400 words read again
        assertEquals(400L, NhatKy.day().reread)
        Store.moveTo(b, 0.5)                       // a jump only moves the mark
        assertEquals(400L, NhatKy.day().reread)
        Store.moveTo(b, 0.002, pageTurn = true)    // behind the reread: nothing
        assertEquals(400L, NhatKy.day().reread)
        Store.moveTo(b, 0.94); Store.moveTo(b, 0.951, pageTurn = true)
        assertEquals(1, b.rereads)
        assertEquals(today, b.rereadDay)
        assertEquals(NhatKy.E_REREAD, NhatKy.events.last().kind)
        assertEquals(0L, b.wordsRead)              // the first reading's credit is untouched
    }

    @Test
    fun aBookComeBackToAfterNinetyDays() {
        val b = book("c")
        b.lastDay = today - 100; b.maxProgress = 0.3; b.progress = 0.3
        NhatKy.onSeconds(b, 60, 10)
        assertEquals(today, b.backDay)
        assertEquals(NhatKy.E_BACK, NhatKy.events.last().kind)
        Store.moveTo(b, 0.302, pageTurn = true)
        assertEquals(400L, NhatKy.day().back)
        // Vong Xuyên: 2.000 words come back to is one hồn lực; reading it to the end, 30.
        assertEquals(0, NhatKy.estimate(4, today, today).points)
        NhatKy.day().back = 4_000; Store.dayWords[today] = 4_000
        assertEquals(2, NhatKy.estimate(4, today, today).points)
        b.finishDay = today
        assertEquals(32, NhatKy.estimate(4, today, today).points)
    }

    @Test
    fun daysBeforeTheJournalCountTheMean() {
        NhatKy.since = today
        for (d in 1..4) Store.daySeconds[today - d] = 1800
        val e = NhatKy.estimate(2, today - 4, today)
        assertEquals(4, e.filled)
        assertEquals(0, e.measured)
        assertEquals(6, e.points)                  // 4 days × 1,5
    }

    @Test
    fun thưHảiCountsAuthorsGenresAndLanguages() {
        val a = book("d"); a.author = "A"; a.genre = "Tiên hiệp"; a.lang = "vi"; a.finishDay = today - 2
        val b = book("e"); b.author = "A"; b.genre = "Tiên hiệp"; b.lang = "vi-VN"; b.finishDay = today - 1
        val c = book("f"); c.author = "B"; c.genre = "Khoa học"; c.lang = "en"; c.finishDay = today
        // A first: 40 + a genre and a language 50; A again: 15; B: 40 + 50.
        assertEquals(40 + 50 + 15 + 40 + 50, NhatKy.estimate(5, today - 2, today).points)
    }

    @Test
    fun theJournalIsSaved() {
        val b = book("g")
        NhatKy.onSeconds(b, 120, 22)
        Store.endSession(120)
        NhatKy.day().reread = 77
        NhatKy.event(NhatKy.E_FAIL)
        val o = JSONObject(NhatKy.toJson().toString())
        NhatKy.reset(); NhatKy.read(o)
        val d = NhatKy.day()
        assertEquals(120L, d.hours[22])
        assertEquals(listOf(120), d.sittings)
        assertEquals(77L, d.reread)
        assertEquals(1, NhatKy.events.size)
        assertEquals(today - 10, NhatKy.since)
        val copy = Book.from(JSONObject(b.toJson().toString()))
        assertEquals(b.id, copy.id)
        assertTrue(NhatKy.nextId > b.id)
    }
}
