package com.exacta.reader

import com.exacta.reader.data.BiCanh
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 3.5: Huyết Nguyệt Trầm Uyên, ma khí, ma công, phản phệ, the Tâm ma đại kiếp. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HuyetNguyetTest {
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

    /** A night of reading: `minutes` after 22 o'clock. */
    private fun night(d: Long, minutes: Int, tamMa: Int = 0) {
        Store.daySeconds[d] = minutes * 60L
        Store.dayWords[d] = 10_000
        NhatKy.day(d).apply { hours[23] = minutes * 60L; this.tamMa = tamMa; sittings.add(minutes * 60) }
    }

    /** Crossed into Huyết Nguyệt ten days ago. */
    private fun cross() {
        Store.realmCap = 9
        Store.totalWords = TuLuyen.floor(9)
        TienDinh.started = true
        TienDinh.gioi = 1; TuLuyen.gioi = 1
        TienDinh.crossDay = today - 10
        TienDinh.wordsAtCross = TuLuyen.floor(9)
    }

    @Test
    fun theAbyssOpensAndCountsItsNights() {
        cross()
        for (d in 1..10) night(today - d, 40)                    // 4 ma khí a night
        TienLo.check()
        assertTrue(TienLo.opened)
        assertEquals(1, TienLo.gioi)
        assertSame(HuyetNguyet, TienLo.rules())
        val moons = (1..10).count { NhatKy.fullMoon(today - it) }
        assertEquals(4 * (10 + moons), TienLo.res())             // 4 a night, twice under the full moon
        assertEquals("Ma Cung", TienLo.seat())
        assertEquals("Nghịch Tiên sơ kỳ", TuLuyen.name())
        // Cửu Tiêu keeps its own rules.
        TienLo.gioi = 0
        assertSame(CuuTieu, TienLo.rules())
    }

    @Test
    fun anOathKeptPaysTwice() {
        cross()
        for (d in 1..10) night(today - d, 40)
        TienLo.check()
        val base = TienLo.res()
        val cap = HuyetNguyet.stakeCap()
        assertEquals(10, cap)                                    // a quarter of a week's, at least 10
        assertFalse(HuyetNguyet.swear(0, cap + 1))
        assertTrue(HuyetNguyet.swear(0, 10))                     // 3 nights of 40 minutes
        assertEquals(base - 10, TienLo.res())
        assertFalse(HuyetNguyet.canSwear())                      // one a week
        night(today, 45)
        TienLo.check()
        assertEquals(0, HuyetNguyet.wins)
        // Two more nights.
        HuyetNguyet.oathStart = today - 2
        night(today - 2, 45); night(today - 1, 45)
        TienLo.check()
        assertEquals(1, HuyetNguyet.wins)
        assertTrue(TienLo.res() >= base + 10)                     // the stake twice back (and the nights)
        assertEquals(-1, HuyetNguyet.oath)
    }

    @Test
    fun anOathBrokenIsLost() {
        cross()
        for (d in 1..10) night(today - d, 40)
        TienLo.check()
        val base = TienLo.res()
        TienLo.halls[2] = 2                                       // Ám điện: 20% back
        assertTrue(HuyetNguyet.swear(1, 10))                     // 7 days in a row, 30 minutes
        HuyetNguyet.oathStart = today - 1                         // yesterday not read: broken
        Store.daySeconds.remove(today - 1)
        TienLo.check()
        assertEquals(1, HuyetNguyet.losses)
        assertEquals(base - 8, TienLo.res())
    }

    @Test
    fun phảnPhệEatsTheLastDays() {
        cross()
        for (d in 1..10) night(today - d, 40)
        TienLo.check()
        val before = TienLo.resTenths
        val lost = (today - 3 until today).sumOf { TienLo.dayTenths(it) }
        TuLuyen.tamMa = 95
        TienLo.check()
        assertEquals(60, TuLuyen.tamMa)
        assertEquals(before - lost, TienLo.resTenths)             // the last three nights gone
        assertEquals(1, HuyetNguyet.phanPhe)
    }

    @Test
    fun theTâmMaKiếpHoldsTâmMaBetween40And80() {
        cross()
        Store.daySeconds[today] = 1200
        TuLuyen.tamMa = 30
        assertFalse(HuyetNguyet.kiepDayKept(0, today))
        TuLuyen.tamMa = 55
        assertTrue(HuyetNguyet.kiepDayKept(0, today))
        TuLuyen.tamMa = 85
        assertFalse(HuyetNguyet.kiepDayKept(0, today))
        assertEquals(14, HuyetNguyet.kiepNeed(0))
        assertEquals(21, HuyetNguyet.kiepNeed(2))
    }

    @Test
    fun savedAndRead() {
        cross()
        for (d in 1..10) night(today - d, 40)
        TienLo.check()
        HuyetNguyet.swear(2, 10)
        val o = JSONObject(TienLo.toJson().toString())
        TienLo.reset(); TienLo.read(o)
        assertEquals(2, HuyetNguyet.oath)
        assertEquals(10, HuyetNguyet.stake)
        assertEquals(today, HuyetNguyet.lastOathDay)
    }
}
