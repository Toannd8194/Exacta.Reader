package com.exacta.reader

import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.DanDao
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 2.2: linh mạch, dược viên, đan phòng and hỏa hầu, đan độc, the pills. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DongPhuTest {
    private val today get() = Store.today()

    private fun roll(n: Int) = object : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(until: Int) = n
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
        TuLuyen.reset()
        TuLuyen.started = true
        TuLuyen.lastTick = today - 1
        TuLuyen.rng = roll(0)
        DaoPhap.reset()
        com.exacta.reader.data.TongMon.reset()
        com.exacta.reader.data.BiCanh.reset()
        com.exacta.reader.data.DaoLo.reset()
        DongPhu.migrate()
        DongPhu.rng = roll(0)
    }

    private fun days(n: Int, minutes: Int) {
        for (d in 0 until n) Store.daySeconds[today - d] = minutes * 60L
    }

    @Test
    fun herbsGrowOnDaysOfReading() {
        assertEquals(2, DongPhu.open())
        assertTrue(DongPhu.plant(0, 0))
        assertFalse(DongPhu.plant(2, 0))         // the third luống is closed at Nhất giai
        DongPhu.plots[0].day = today - 4
        days(5, 30)
        Store.daySeconds[today - 2] = 600         // a short day gives nothing
        assertEquals(40, DongPhu.years(0))
        assertTrue(DongPhu.harvest(0))
        assertEquals(listOf(0 to 40), DongPhu.herbs)
        assertEquals(3, DongPhu.seeds[0])         // three at the start, one sown, one back
    }

    @Test
    fun theLinhMạchRises() {
        assertFalse(DongPhu.upgrade())
        Store.linhThach = 650
        assertTrue(DongPhu.upgrade())
        assertTrue(DongPhu.upgrade())
        assertEquals(3, DongPhu.giai)
        assertEquals(50, Store.linhThach)
        assertEquals(104, DaoPhap.speed())
        assertEquals(3, DongPhu.open())
        assertEquals(12, DongPhu.yearsADay())
        assertTrue(TuLuyen.parts().any { it.first.startsWith("Linh mạch") && it.second == 2 })
    }

    @Test
    fun hỏaHầuMakesThePill() {
        DongPhu.herbs.add(0 to 60)
        assertTrue(DongPhu.canBrew(DanDao.TU_KHI))
        assertEquals(60, DongPhu.chance(DanDao.TU_KHI))
        assertTrue(DongPhu.startBrew(DanDao.TU_KHI))
        assertTrue(DongPhu.herbs.isEmpty())
        DongPhu.onSeconds(19 * 60L)
        assertEquals(DanDao.TU_KHI, DongPhu.brew)
        DongPhu.onSeconds(20 * 60L)
        assertEquals(-1, DongPhu.brew)
        assertEquals(1, DongPhu.made)
        assertEquals(1, DongPhu.count(DanDao.TU_KHI, 3))  // a margin of 60: cực phẩm
        // Cực phẩm: 5 000 × 1,6; đan độc 6 × 25%.
        assertEquals("Linh lực +0,8 vạn chữ", DongPhu.use(DanDao.TU_KHI, 3))
        assertEquals(8_000, Store.tuVi())
        assertEquals(1, DongPhu.danDoc)
        assertNull(DongPhu.use(DanDao.TU_KHI, 3))
    }

    @Test
    fun leavingEarlyBurstsTheFurnace() {
        DongPhu.herbs.add(0 to 60)
        DongPhu.startBrew(DanDao.TU_KHI)
        Store.endSession(30)                       // half a minute: not a sitting
        assertEquals(DanDao.TU_KHI, DongPhu.brew)
        Store.endSession(5 * 60)
        assertEquals(-1, DongPhu.brew)
        assertEquals(1, DongPhu.burst)
        assertTrue(DongPhu.herbs.isEmpty())
        // Put out before reading: the herbs come back.
        DongPhu.herbs.add(0 to 70)
        DongPhu.startBrew(DanDao.TU_KHI)
        assertTrue(DongPhu.cancelBrew())
        assertEquals(listOf(0 to 70), DongPhu.herbs)
    }

    @Test
    fun aFailedFurnace() {
        DongPhu.rng = roll(99)
        DongPhu.herbs.add(0 to 60)
        DongPhu.startBrew(DanDao.TU_KHI)
        DongPhu.onSeconds(20 * 60L)
        assertEquals(0, DongPhu.made)
        assertEquals(1, DongPhu.failed)
        assertTrue(DongPhu.pills.isEmpty())
    }

    @Test
    fun đanĐộcWeighs() {
        Store.totalWords = 100_000
        days(20, 30)
        val cc = TuLuyen.canCo()
        DongPhu.poison(70)
        assertEquals(cc - 10, TuLuyen.canCo())
        assertTrue(TuLuyen.parts().any { it.first.startsWith("Đan độc") && it.second == -17 })
        DongPhu.lastTick = today - 4
        DongPhu.tick()
        assertEquals(55, DongPhu.danDoc)            // three days of 30 minutes
        // A pill of the market leaves some too.
        Store.items[com.exacta.reader.game.Treasure.NGUNG_THAN_DAN] = 1
        Store.useSimple(com.exacta.reader.game.Treasure.NGUNG_THAN_DAN)
        assertEquals(55 + DongPhu.SHOP_PILL_DOC, DongPhu.danDoc)
    }

    @Test
    fun aPháCảnhPillForItsGate() {
        Store.totalWords = 100_000
        days(20, 30)
        DongPhu.pills[DanDao.TRUC_CO * 4 + 1] = 1
        assertTrue(DongPhu.usable(DanDao.TRUC_CO))
        assertFalse(DongPhu.usable(DanDao.NGUNG_KIM))
        val before = TuLuyen.chance()
        DongPhu.use(DanDao.TRUC_CO, 1)
        assertEquals(22, DongPhu.breakPill[1])
        assertEquals(7, DongPhu.danDoc)              // 10 × 75% for trung phẩm
        assertEquals(before + 22 - 1, TuLuyen.chance())
        assertEquals(true, TuLuyen.breakThrough())
        assertEquals(0, DongPhu.breakPill[1])
    }

    @Test
    fun savedAndRead() {
        DongPhu.giai = 4
        DongPhu.plant(0, 1)
        DongPhu.herbs.add(2 to 150)
        DongPhu.pills[DanDao.HOI_KHI * 4] = 2
        DongPhu.danDoc = 33
        DongPhu.known.add(DanDao.KIM_THAN)
        val o = JSONObject(DongPhu.toJson().toString())
        DongPhu.reset()
        DongPhu.read(o)
        assertEquals(4, DongPhu.giai)
        assertEquals(1, DongPhu.plots[0].herb)
        assertEquals(listOf(2 to 150), DongPhu.herbs)
        assertEquals(2, DongPhu.count(DanDao.HOI_KHI, 0))
        assertEquals(33, DongPhu.danDoc)
        assertTrue(DanDao.KIM_THAN in DongPhu.known)
    }
}
