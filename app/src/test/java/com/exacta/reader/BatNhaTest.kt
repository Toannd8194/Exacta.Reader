package com.exacta.reader

import com.exacta.reader.data.BatNha
import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.Book
import com.exacta.reader.data.CuuTieu
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.HuyetNguyet
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.NhatKy
import com.exacta.reader.data.PhuLuc
import com.exacta.reader.data.Store
import com.exacta.reader.data.TeLuyen
import com.exacta.reader.data.TienDinh
import com.exacta.reader.data.TienLo
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TranPhap
import com.exacta.reader.data.TrangBi
import com.exacta.reader.data.TuLuyen
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 3.7: Bát Nhã Liên Đài, công đức, xá lợi, độ hóa đồng đạo, Liên trì, the Vô niệm kiếp and the Đại kiếp Liên Hoa. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatNhaTest {
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
        NhatKy.reset(); NhatKy.started = true; NhatKy.since = today - 20
        TienLo.reset(); TienLo.started = true
        TienLo.rng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = 0 }
    }

    /** A day of reading: `minutes` in one sitting. */
    private fun day(d: Long, minutes: Int, tamMa: Int = 0, jumps: Int = 0) {
        Store.daySeconds[d] = minutes * 60L
        Store.dayWords[d] = 10_000
        NhatKy.day(d).apply { hours[10] = minutes * 60L; this.tamMa = tamMa; sittings.add(minutes * 60) }
        if (jumps > 0) TuLuyen.dayJumps[d] = jumps
    }

    /** Crossed into Bát Nhã ten days ago. */
    private fun cross() {
        Store.realmCap = 9
        Store.totalWords = TuLuyen.floor(9)
        TienDinh.started = true
        TienDinh.gioi = 2; TuLuyen.gioi = 2
        TienDinh.crossDay = today - 10
        TienDinh.wordsAtCross = TuLuyen.floor(9)
    }

    @Test
    fun theLotusOpensAndCountsItsDays() {
        cross()
        for (d in 1..10) day(today - d, 35)
        TienLo.check()
        assertTrue(TienLo.opened)
        assertSame(BatNha, TienLo.rules())
        assertEquals(10 * 2 + 1, TienLo.res())                   // 2 a day, 1 for the seventh day of the chain
        assertEquals("Thiền Viện", TienLo.seat())
        assertEquals("Thiền Tiên sơ kỳ", TuLuyen.name())
        assertEquals(10, BatNha.chain())
    }

    @Test
    fun aJumpOrTâmMaCostsTheDay() {
        cross()
        for (d in 3..10) day(today - d, 35)
        day(today - 2, 35, tamMa = 55)                            // tâm ma from 50: nothing
        day(today - 1, 35, jumps = 1)                             // a jump: nothing, and −2 at most to zero
        TienLo.check()
        assertEquals(8 * 2 + 1, TienLo.res())
    }

    @Test
    fun aXáLợiKeepsTheChain() {
        cross()
        for (d in 3..10) day(today - d, 35)
        TienLo.check()
        // The giới opened four days ago; yesterday's yesterday was missed.
        TienLo.openDay = today - 4; TienLo.countedTo = today - 3
        Store.daySeconds.remove(today - 2); Store.dayWords.remove(today - 2); NhatKy.days.remove(today - 2)
        day(today - 1, 35)
        BatNha.peak = 300
        assertEquals(1, BatNha.held())
        TienLo.check()
        assertTrue(today - 2 in Store.sealedDays)
        assertEquals(0, BatNha.held())
        assertEquals(1, BatNha.guarded)
        assertEquals(10, BatNha.chain())                          // 8 days, the sealed one, yesterday
    }

    @Test
    fun aXáLợiTakesTheWoundOfAKiếp() {
        cross()
        for (d in 1..10) day(today - d, 35)
        TienLo.check()
        BatNha.peak = 600
        TienLo.kiepActive = 0; TienLo.kiepStart = today - 30
        TienLo.check()
        assertEquals(-1, TienLo.kiepActive)
        assertEquals(1, TienLo.kiepFails[0])
        assertFalse(TienLo.injured())
        assertEquals(1, BatNha.warded)
        assertEquals(1, BatNha.held())
        // With none left the wound comes.
        BatNha.used = 2
        TienLo.kiepActive = 0; TienLo.kiepStart = today - 30
        TienLo.check()
        assertTrue(TienLo.injured())
    }

    @Test
    fun aFellowIsFreedInThreeDays() {
        cross()
        for (d in 1..10) day(today - d, 35)
        val month = TienLo.monthStart(today)
        assumeTrue(today - month >= 2)
        val b = Book("/books/left.epub").apply { id = 7; title = "Kinh lạc"; wordsRead = 500; lastDay = month - 40 }
        Store.books[b.path] = b
        TienLo.check()                                              // opens
        TienLo.openDay = month
        TienLo.check()                                              // the fellow of the month
        assertEquals(7, BatNha.doBook)
        assertEquals("Kinh lạc", BatNha.book()?.title)
        val before = TienLo.res()
        for (d in 0..2) NhatKy.day(today - d).books[7] = 700L
        TienLo.check()
        assertTrue(BatNha.doDone)
        assertEquals(1, BatNha.freed)
        assertEquals(before + 40, TienLo.res())
    }

    @Test
    fun theKiếpOfNoThought() {
        cross()
        day(today, 50)
        assertTrue(BatNha.kiepDayKept(0, today))
        TuLuyen.dayJumps[today] = 1
        assertFalse(BatNha.kiepDayKept(0, today))
        day(today - 1, 30)
        assertFalse(BatNha.kiepDayKept(1, today - 1))               // no sitting of 45 minutes
        assertEquals(21, BatNha.kiepNeed(0))
        assertEquals(49, BatNha.kiepNeed(2))
        assertEquals(70, BatNha.kiepWindow(2))
    }

    @Test
    fun theLotusKiếpStartsAgainOnAMissedDay() {
        cross()
        for (d in 1..10) day(today - d, 35)
        TienLo.check()
        TienLo.kiepActive = 2; TienLo.kiepStart = today - 5
        TienLo.kiepDays.addAll(listOf(today - 5, today - 4, today - 2, today - 1))
        TienLo.check()
        assertEquals(setOf(today - 2, today - 1), TienLo.kiepDays.toSet())
    }

    @Test
    fun theLotusPondStillsTâmMa() {
        cross()
        for (d in 3..10) day(today - d, 35)
        TienLo.check()
        TienLo.halls[2] = 2
        TienLo.faction = 1                                          // Liên Trì Tịnh Xá: one more
        TuLuyen.tamMa = 30
        TienLo.openDay = today - 2; TienLo.countedTo = today - 3; BatNha.lienTo = today - 3
        day(today - 2, 35); day(today - 1, 35)
        TienLo.check()
        assertEquals(24, TuLuyen.tamMa)                              // 3 a day, two days
    }

    @Test
    fun savedAndRead() {
        cross()
        for (d in 1..10) day(today - d, 35)
        TienLo.check()
        BatNha.peak = 650; BatNha.used = 1; BatNha.autoGuard = false; BatNha.doBook = 4; BatNha.freed = 2
        val o = JSONObject(TienLo.toJson().toString())
        TienLo.reset(); TienLo.read(o)
        assertEquals(650, BatNha.peak)
        assertEquals(1, BatNha.held())
        assertFalse(BatNha.autoGuard)
        assertEquals(4, BatNha.doBook)
        assertEquals(2, BatNha.freed)
    }
}
