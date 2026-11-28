package com.exacta.reader

import com.exacta.reader.data.Beast
import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.Store
import com.exacta.reader.data.TeLuyen
import com.exacta.reader.data.TienDinh
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TranPhap
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

/** The rules of 2.7.1: tế luyện cấm chế and trận pháp. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TeLuyenTest {
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
        TeLuyen.reset(); TeLuyen.started = true
        TranPhap.reset(); TranPhap.started = true
    }

    private fun beast(seed: Long, tier: Int): Beast = Beast(seed, tier, today, "Thử", 0).also { Store.beasts.add(it) }

    @Test
    fun nineTầngMakeAPhápKhíThenAnOreOpensTheNext() {
        val b = beast(1L, 2)
        assertTrue(TeLuyen.refine(1L))
        // Tầng 1 … 9: 2.000 + 1.000 × n each.
        val words = (0 until 9).sumOf { TeLuyen.need(it) }
        TeLuyen.onWords(words)
        assertEquals(9, TeLuyen.level(1L))
        assertEquals("Pháp khí", TeLuyen.gradeName(1L))
        // The gate into pháp bảo waits for an ore of Địa phẩm and 50 linh thạch.
        TeLuyen.onWords(TeLuyen.need(9))
        assertEquals(9, TeLuyen.level(1L))
        assertFalse(TeLuyen.gateReady())
        Store.bag[2] = 1
        Store.linhThach = 50
        assertTrue(TeLuyen.breakGate())
        assertEquals(10, TeLuyen.level(1L))
        assertEquals(0, Store.bag[2])
        assertEquals("Pháp bảo", TeLuyen.gradeName(b.seed))
        assertEquals(2, TeLuyen.speedBonus())          // 10 / 4
        assertEquals(2, TeLuyen.breakPart())
    }

    @Test
    fun thôngThiênOnlyInLinhGiới() {
        beast(2L, 3)
        TeLuyen.levels[2L] = 27
        TeLuyen.refine(2L)
        TeLuyen.onWords(TeLuyen.need(27))
        Store.bag[3] = 1
        Store.linhThach = 400
        assertFalse(TeLuyen.gateReady())
        Store.realmCap = 5
        assertTrue(TeLuyen.breakGate())
        assertEquals("Thông thiên linh bảo", TeLuyen.gradeName(2L))
        val o = JSONObject(TeLuyen.toJson().toString())
        TeLuyen.reset(); TeLuyen.read(o)
        assertEquals(28, TeLuyen.level(2L))
    }

    @Test
    fun trậnPhápInTheEyes() {
        Store.linhThach = 2_000
        assertFalse(TranPhap.canBuild(TranPhap.HO_SON))     // Nhị giai first
        assertTrue(TranPhap.build(TranPhap.TU_LINH))
        assertEquals(1, TranPhap.eyes())
        assertTrue(TranPhap.on(TranPhap.TU_LINH))
        assertTrue(TranPhap.build(TranPhap.ME_TUNG))
        assertFalse(TranPhap.on(TranPhap.ME_TUNG))          // one eye only
        assertFalse(TranPhap.toggle(TranPhap.ME_TUNG))
        DongPhu.giai = 4
        assertEquals(2, TranPhap.eyes())
        assertTrue(TranPhap.toggle(TranPhap.ME_TUNG))
        assertEquals(10, TranPhap.biCanhPart())
        val withEyes = DaoPhap.speed()
        assertTrue(TranPhap.toggle(TranPhap.TU_LINH))       // taken out
        assertEquals(withEyes - 10, DaoPhap.speed())
    }
}
