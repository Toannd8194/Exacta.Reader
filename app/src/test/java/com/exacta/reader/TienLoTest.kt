package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.CuuTieu
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
import com.exacta.reader.data.TienLo
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TranPhap
import com.exacta.reader.data.TrangBi
import com.exacta.reader.data.TuLuyen
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 3.0: the tiên lộ of Cửu Tiêu Vân Khuyết. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TienLoTest {
    private val today get() = Store.today()

    private fun roll(n: Int) = object : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(until: Int) = n.coerceAtMost(until - 1)
    }

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
        TienLo.rng = roll(0)
    }

    /** Crossed into Cửu Tiêu ten days ago, Tiên nhân cửu phẩm. */
    private fun cross(kept: Long = 0) {
        Store.realmCap = 9
        Store.totalWords = TuLuyen.floor(9) + kept
        TienDinh.started = true
        TienDinh.gioi = 0; TuLuyen.gioi = 0
        TienDinh.crossDay = today - 10
        TienDinh.wordsAtCross = TuLuyen.floor(9)
    }

    private fun readDay(d: Long, minutes: Int = 40, words: Long = 10_000) {
        Store.daySeconds[d] = minutes * 60L
        Store.dayWords[d] = words
        NhatKy.day(d).apply { hours[20] = minutes * 60L; balance = 100; sittings.add(minutes * 60) }
    }

    @Test
    fun theGiớiOpensAndCountsTheForecourt() {
        cross()
        for (d in 1..10) readDay(today - d, words = 25_000)    // 10 tiên lực a day at balance 100%
        Store.linhThach = 250
        TienLo.check()
        assertTrue(TienLo.opened)
        assertEquals(100, TienLo.retro)
        assertEquals(100, TienLo.res())
        assertEquals(10, TienLo.retroMeasured)
        assertEquals(2L, TienLo.tienThach)
        assertEquals(50L, Store.linhThach)
        assertEquals(1, TienLo.bac)
        assertEquals("Thiên Tiên sơ kỳ", TuLuyen.name())
        assertEquals(today - 1, TienLo.countedTo)
        // A day read, counted once it is over.
        TienLo.openDay = today - 2; TienLo.countedTo = today - 2
        TienLo.check()
        assertEquals(110, TienLo.res())
    }

    @Test
    fun theOtherGiớiStillWaitAtTheForecourt() {
        cross()
        TienDinh.gioi = 1
        TienLo.check()
        assertFalse(TienLo.opened)
        assertTrue(TienDinh.atForecourt())
    }

    @Test
    fun bậcNeedWordsAndTiênLựcAndTheKiếp() {
        cross(kept = 60 * 10_000L)
        TienLo.check()
        assertEquals(1, TienLo.bac)                        // the words, but no tiên lực yet
        TienLo.resTenths = 2_000
        TienLo.check()
        assertEquals(2, TienLo.bac)
        assertEquals(TienLo.TT_BAC, TienLo.tienThach)
        Store.totalWords += 80 * 10_000L
        TienLo.resTenths = 5_000
        TienLo.check()
        assertEquals(2, TienLo.bac)                        // bậc 3 waits on the Thiên Nhân kiếp
        assertEquals(0, TienLo.kiepDue())
        assertTrue(TienLo.startKiep())
        for (d in 0 until 21) TienLo.kiepDays.add(today - 30 + d)
        TienLo.check()
        assertTrue(TienLo.kiepDone(0))
        assertEquals(3, TienLo.bac)
        assertEquals("Huyền Tiên sơ kỳ", TienLo.name())
    }

    @Test
    fun aKiếpFailedWoundsButKeepsTheBậc() {
        cross(kept = 140 * 10_000L)
        TienLo.check()
        TienLo.bac = 2
        TienLo.resTenths = 5_000
        TienLo.rng = roll(99)
        assertTrue(TienLo.startKiep())
        val before = TienLo.kiepChance(0)
        for (d in 0 until 21) TienLo.kiepDays.add(today - 30 + d)
        TienLo.check()
        assertEquals(2, TienLo.bac)
        assertEquals(1, TienLo.kiepFails[0])
        assertTrue(TienLo.injured())
        assertEquals(before + 10, TienLo.kiepChance(0))
        assertFalse(TienLo.canStartKiep())                 // wounded: rest first
        // Wounded days give half.
        readDay(today - 1, words = 25_000)
        TienLo.openDay = today - 2; TienLo.countedTo = today - 2
        val res = TienLo.resTenths
        TienLo.injuryUntil = today
        TienLo.check()
        assertEquals(50L, TienLo.resTenths - res)
    }

    @Test
    fun theWeeksThiênQuy() {
        cross()
        TienLo.check()
        val week = Store.weekStart(today) - 7
        CuuTieu.ruleWeek = week - 7
        // A week that keeps every rule: 40 minutes each day, three sittings, no jump, a book finished.
        for (d in week..week + 6) readDay(d)
        Store.book("x").apply { words = 100; finishDay = week + 2 }
        val res = TienLo.resTenths
        TienLo.countedTo = today - 1
        TienLo.check()
        assertEquals(1, CuuTieu.kept)
        assertEquals(10L * CuuTieu.RULE_REWARD, TienLo.resTenths - res)
    }

    @Test
    fun hallsFactionsAndWhatWasCarried() {
        cross()
        TienLo.check()
        TienLo.tienThach = 15
        assertTrue(TienLo.raiseHall(0))
        assertEquals(0L, TienLo.tienThach)
        assertEquals(1, TienLo.halls[0])
        readDay(today - 1)
        assertEquals(10L, CuuTieu.dayExtra(today - 1))     // Tiên điền: a day of 30 minutes
        assertEquals(100, TienLo.percent())
        TienLo.join(0)
        assertEquals(105, TienLo.percent())                // Thiên Đình: +5%
        TienLo.join(2)
        assertEquals(100, TienLo.percent())
        assertEquals(65, TienLo.kiepChance(0))             // Tử Phủ: kiếp +5%
        TienLo.onWords(85_000)
        assertEquals(2L, TienLo.tienThach)
    }

    @Test
    fun theTiênLộIsSaved() {
        cross(kept = 60 * 10_000L)
        TienLo.check()
        TienLo.resTenths = 2_345; TienLo.halls[2] = 3; TienLo.join(1); TienLo.kiepFails[1] = 2
        CuuTieu.kept = 4
        val o = JSONObject(TienLo.toJson().toString())
        TienLo.reset(); TienLo.read(o)
        assertTrue(TienLo.opened)
        assertEquals(234, TienLo.res())
        assertEquals(3, TienLo.halls[2])
        assertEquals(1, TienLo.faction)
        assertEquals(2, TienLo.kiepFails[1])
        assertEquals(4, CuuTieu.kept)
    }
}
