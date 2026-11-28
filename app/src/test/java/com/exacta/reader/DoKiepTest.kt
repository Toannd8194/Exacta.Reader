package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.Store
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.DanDao
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 2.6: the nine tầng of Độ Kiếp, ngũ suy, thọ nguyên and Chân Tu; the tĩnh thất and the đan phương. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DoKiepTest {
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
        Store.chanTu = false
        Store.tinhThat = 1
        Store.luckRng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = until - 1 }
        TuLuyen.reset()
        TuLuyen.started = true
        TuLuyen.lastTick = today - 1
        DaoPhap.reset()
        DongPhu.reset()
        TongMon.reset()
        BiCanh.reset()
        DaoLo.reset()
        LinhGioi.reset()
        DoKiep.reset()
        com.exacta.reader.data.TienDinh.reset()
        DoKiep.started = true
        DoKiep.lifeStart = today
        DoKiep.rng = roll(0)
    }

    @After
    fun after() {
        Store.chanTu = false
        DoKiep.reset()
    }

    private fun days(from: Long, to: Long, minutes: Int) {
        for (d in from..to) Store.daySeconds[d] = minutes * 60L
    }

    @Test
    fun aLôiKiếpTầng() {
        Store.realmCap = 7
        DoKiep.active = 0
        DoKiep.activeDay = today - 4
        days(today - 4, today, 55)
        DoKiep.check()
        assertEquals(1, DoKiep.passed)
        assertEquals(-1, DoKiep.active)
        assertEquals(1, DoKiep.an.size)
        assertEquals(100, Store.linhThach)
    }

    @Test
    fun aMissedDayFailsButKeepsThePassed() {
        Store.realmCap = 7
        DoKiep.passed = 1
        DoKiep.active = 1
        DoKiep.activeDay = today - 3
        days(today - 3, today, 55)
        Store.daySeconds.remove(today - 2)
        DoKiep.check()
        assertEquals(1, DoKiep.passed)
        assertEquals(1, DoKiep.fails[1])
        assertTrue(DoKiep.resting())
        assertTrue(TuLuyen.injured())
        assertEquals(10, TuLuyen.tamMa)
        assertTrue(DoKiep.parts(1).any { it.first.startsWith("Kiên trì") && it.second == 10 })
    }

    @Test
    fun tâmMaKiếpBrokenByAJump() {
        Store.realmCap = 7
        DoKiep.passed = 6
        DoKiep.active = 6
        DoKiep.activeDay = today
        TuLuyen.onJump()
        DoKiep.check()
        assertEquals(6, DoKiep.passed)
        assertEquals(1, DoKiep.fails[6])
    }

    @Test
    fun theNinthTầngIsPhiThăng() {
        Store.realmCap = 7
        Store.totalWords = TuLuyen.floor(8)
        DoKiep.passed = 8
        DoKiep.active = 8
        DoKiep.activeDay = today - 14
        // Nine waves, never more than six days apart.
        for (d in listOf(14L, 13, 11, 9, 7, 5, 3, 1, 0)) Store.daySeconds[today - d] = 3600
        DoKiep.check()
        assertEquals(9, DoKiep.passed)
        assertEquals(8, Store.realmCap)
        // Ngũ suy comes down.
        DoKiep.check()
        assertTrue(DoKiep.suyActive())
        assertEquals(15, DoKiep.suyPenalty())
        assertTrue(TuLuyen.needs().any { it.text.startsWith("Qua thiên nhân ngũ suy") && !it.met })
    }

    @Test
    fun thọNguyênAndChânTu() {
        Store.realmCap = 1
        Store.totalWords = 150_000
        DoKiep.lifeStart = today - 230
        assertTrue(DoKiep.lifeGamble() != null)        // 230 of 250
        DoKiep.lifeStart = today - 300
        DoKiep.check()
        assertEquals(1, Store.realmCap)                // Tùy Duyên: nothing happens
        Store.chanTu = true
        DoKiep.check()
        assertEquals(0, Store.realmCap)
        assertEquals(0L, Store.tuVi())
        assertEquals(1, DoKiep.rebirths)
        assertEquals(50, DoKiep.speedBonus())          // ký ức tiền kiếp, below Trúc Cơ
        assertEquals(today, DoKiep.lifeStart)
        val o = JSONObject(DoKiep.toJson().toString())
        DoKiep.reset(); DoKiep.read(o)
        assertEquals(1, DoKiep.rebirths)
    }

    @Test
    fun sixĐẳngOfTĩnhThấtAndThirtyTwoPills() {
        assertEquals(listOf(30), Store.BE_QUAN_CHOICES.toList())
        Store.linhThach = 200
        assertTrue(Store.upgradeTinhThat())
        assertEquals(listOf(30, 45), Store.BE_QUAN_CHOICES.toList())
        assertEquals(25L, Store.beQuanReward(45))
        assertFalse(Store.upgradeTinhThat())           // 400 for Tam đẳng
        assertEquals(32, DanDao.RECIPES.size)
        assertEquals(12, DanDao.HERBS.size)
        // Diên Thọ Đan gives back fifty years.
        DongPhu.started = true
        DongPhu.pills[DanDao.DIEN_THO * 4] = 1
        val before = DoKiep.lifeStart
        DongPhu.use(DanDao.DIEN_THO, 0)
        assertEquals(before + 50, DoKiep.lifeStart)
    }
}
