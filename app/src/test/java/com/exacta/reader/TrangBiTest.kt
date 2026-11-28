package com.exacta.reader

import com.exacta.reader.data.Beast
import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.LinhGioi
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 2.7.2: ô trang bị and phù lục drawn by hand. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrangBiTest {
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
        TeLuyen.reset(); TranPhap.reset()
        TrangBi.reset(); TrangBi.started = true
        PhuLuc.reset(); PhuLuc.started = true
        PhuLuc.rng = roll(0)
    }

    private fun beast(seed: Long, tier: Int): Beast = Beast(seed, tier, today, "Thử", 0).also { Store.beasts.add(it) }

    @Test
    fun onlyTheWornServeFromKimĐan() {
        beast(1L, 4)
        beast(2L, 1)
        beast(3L, 0)
        assertEquals(4, TrangBi.bestTier())        // before Kim Đan the whole collection serves
        Store.realmCap = 2
        assertEquals(2, TrangBi.slots())
        assertEquals(-1, TrangBi.bestTier())       // nothing worn
        assertTrue(TrangBi.wear(2L))
        assertTrue(TrangBi.wear(3L))
        assertFalse(TrangBi.wear(1L))              // two slots at Kim Đan
        assertEquals(1, TrangBi.bestTier())
        assertEquals(1, TrangBi.guard())
        assertTrue(TrangBi.takeOff(3L))
        assertTrue(TrangBi.wear(1L))
        assertEquals(4, TrangBi.bestTier())
        Store.realmCap = 3
        assertEquals(3, TrangBi.slots())
        val o = JSONObject(TrangBi.toJson().toString())
        TrangBi.reset(); TrangBi.read(o)
        assertEquals(listOf(2L, 1L), TrangBi.worn)
    }

    @Test
    fun aPhùDrawnInOneSitting() {
        val i = 0                                    // Thiên Hỏa Phù: 2 phù chỉ, 1 chu sa, 20 phút
        assertFalse(PhuLuc.canDraw(i))
        PhuLuc.paper = 2
        PhuLuc.ink = 1
        assertTrue(PhuLuc.start(i))
        assertEquals(0, PhuLuc.paper)
        PhuLuc.onSeconds(19 * 60L)
        assertEquals(0, PhuLuc.drawing)
        PhuLuc.onSeconds(20 * 60L)
        assertEquals(-1, PhuLuc.drawing)
        assertEquals(2, Store.items[1])              // roll 0 at 55%: a margin of 55, two
        // The next one: the sitting stops early, the stroke breaks.
        PhuLuc.paper = 2; PhuLuc.ink = 1
        PhuLuc.start(i)
        PhuLuc.onSessionEnd(5 * 60L)
        assertEquals(-1, PhuLuc.drawing)
        assertEquals(1, PhuLuc.spoiled)
        // The bí cảnh gives paper and chu sa; paper is bought.
        PhuLuc.onBiCanh()
        assertEquals(2, PhuLuc.paper)
        assertTrue(PhuLuc.ink >= 1)
        Store.linhThach = 10
        assertTrue(PhuLuc.buyPaper())
        assertEquals(3, PhuLuc.paper)
    }

    @Test
    fun theHigherPhùAskThầnThức() {
        val last = PhuLuc.PHUS.size - 1              // Dẫn Lôi Phù asks Lục trọng
        PhuLuc.paper = 10; PhuLuc.ink = 10
        assertFalse(PhuLuc.canDraw(last))
        assertEquals(9, PhuLuc.PHUS.size)
    }
}
