package com.exacta.reader

import com.exacta.reader.data.Beast
import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.BookCss
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.Store
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Treasure
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

/** The rules of 2.5: phi thăng, trảm trần duyên, pháp tắc, đạo vực; the weight of the letters in percent. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LinhGioiTest {
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
        TuLuyen.reset()
        TuLuyen.started = true
        TuLuyen.lastTick = today - 1
        DaoPhap.reset()
        DongPhu.reset()
        TongMon.reset()
        BiCanh.reset()
        DaoLo.reset()
        DaoLo.started = true
        LinhGioi.reset()
        com.exacta.reader.data.DoKiep.reset()
        LinhGioi.started = true
    }

    private fun beast(species: Int, seed: Long): Beast {
        val b = Beast(seed, species / Treasure.KINDS, today, "Thử", species % Treasure.KINDS)
        Store.beasts.add(b)
        return b
    }

    @Test
    fun khôngGianTiếtĐiểm() {
        val w = Store.weekStart(today)
        for (i in 1..3) LinhGioi.onBiCanh(w - 7 * i)
        assertEquals(3, LinhGioi.tietRun())          // this week still open
        LinhGioi.onBiCanh(w)
        assertEquals(4, LinhGioi.tietRun())
        LinhGioi.tietWeeks.remove(w - 14)
        assertEquals(2, LinhGioi.tietRun())
        Store.realmCap = 4
        assertTrue(TuLuyen.needs().any { it.text.startsWith("Không gian tiết điểm") && !it.met })
        assertEquals(Store.PHI_THANG_DAYS, Store.trialNeed(5))
        assertEquals(Store.PHI_THANG_MINUTES, Store.trialMinutes(5))
    }

    @Test
    fun trảmTrầnDuyênHoldsTheSetsLeftBehind() {
        val set = 0
        val members = Treasure.SET_MEMBERS[set]
        members.forEachIndexed { i, s -> beast(s, 100L + i) }
        Store.checkSets()
        assertTrue(Store.setActive(set))
        Store.realmCap = 4
        assertFalse(LinhGioi.trimDone())
        assertTrue(LinhGioi.makeBanMenh(100L))
        assertTrue(LinhGioi.carry(101L)); assertTrue(LinhGioi.carry(102L)); assertTrue(LinhGioi.carry(103L))
        assertFalse(LinhGioi.carry(104L))             // three at most
        assertTrue(LinhGioi.leave(103L))              // before phi thăng anything goes
        Store.realmCap = 5
        assertTrue(Store.setActive(set))              // one of its kinds was carried
        LinhGioi.leave(101L)                           // locked now
        assertTrue(LinhGioi.isCarried(101L))
        LinhGioi.banMenh.clear(); LinhGioi.carried.clear()
        assertFalse(Store.setActive(set))             // held down by the linh khí
        assertTrue(Store.setOwned(set))
    }

    @Test
    fun aLawInThreeTrials() {
        DaoPhap.started = true
        DaoPhap.roots.add(PhapMon.MOC)
        assertFalse(LinhGioi.canPonder(LinhGioi.MOC))   // Luyện Hư first
        Store.realmCap = 5
        assertFalse(LinhGioi.canPonder(LinhGioi.LOI))   // not the cultivator's
        assertTrue(LinhGioi.ponder(LinhGioi.MOC))
        LinhGioi.activeDay = today - 100
        for (d in 0 until 80) Store.daySeconds[today - d] = 1800
        LinhGioi.check()
        assertTrue(LinhGioi.learned(LinhGioi.MOC))
        assertEquals(-1, LinhGioi.active)
        assertEquals(5, LinhGioi.canCoBonus())
        assertTrue(TuLuyen.needs().any { it.text.startsWith("Lĩnh ngộ một pháp tắc") && it.met } || TuLuyen.gate() != 6)
    }

    @Test
    fun đạoVựcDoublesItsLaws() {
        Store.realmCap = 6
        for (i in listOf(LinhGioi.HOA, LinhGioi.KIM, LinhGioi.THUY)) LinhGioi.stage[i] = 3
        assertEquals(10, LinhGioi.speedBonus())
        assertFalse(LinhGioi.formVuc(listOf(LinhGioi.HOA, LinhGioi.KIM), "x"))
        assertTrue(LinhGioi.formVuc(listOf(LinhGioi.HOA, LinhGioi.KIM, LinhGioi.THUY), ""))
        assertEquals("Hỏa Kim Thủy Đạo Vực", LinhGioi.vucName)
        assertEquals(30, LinhGioi.speedBonus())        // 20 + 10
        assertEquals(50, LinhGioi.stirPercent())
        assertTrue(LinhGioi.breakParts(7).any { it.second == 10 })
        assertTrue(LinhGioi.needs(7).all { it.met })
    }

    @Test
    fun oldSavesInLinhGiới() {
        Store.realmCap = 7
        DaoPhap.started = true
        DaoPhap.roots.add(PhapMon.HOA)
        beast(Treasure.SET_MEMBERS[2][0], 7L)
        LinhGioi.migrate()
        assertEquals(listOf(7L), LinhGioi.banMenh)
        assertTrue(LinhGioi.rechoose)
        assertEquals(3, LinhGioi.learnedCount())
        assertTrue(LinhGioi.learned(LinhGioi.HOA))
        assertEquals(3, LinhGioi.vuc.size)
        val o = JSONObject(LinhGioi.toJson().toString())
        LinhGioi.reset()
        LinhGioi.read(o)
        assertEquals(LinhGioi.vuc.size, 3)
        assertEquals(listOf(7L), LinhGioi.banMenh)
    }

    @Test
    fun weightInPercent() {
        Store.weightRef = 600
        Store.fontFamily = "Bitter"
        Store.weightPct = 100
        assertEquals("Bitter SemiBold", BookCss.bitterFamily())
        assertNull(BookCss.strokeCarrier())
        Store.weightPct = 105
        assertEquals("Bitter SemiBold", BookCss.bitterFamily())
        assertEquals(30 * 0.011 / 100, BookCss.strokeCarrier()!!, 1e-9)
        Store.weightPct = 150
        assertEquals("Bitter Black", BookCss.bitterFamily())
        assertEquals(50, BookCss.pctMin())
        Store.weightPct = 50
        assertEquals("Bitter Light", BookCss.bitterFamily())
        Store.weightPct = 100
    }
}
