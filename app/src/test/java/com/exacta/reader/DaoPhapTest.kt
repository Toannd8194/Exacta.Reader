package com.exacta.reader

import com.exacta.reader.data.Book
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.PhapMon
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

/** The rules of 2.1: linh căn, công pháp and its tầng, ngộ tính, tâm pháp, thuật pháp, Tàng Kinh Các. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DaoPhapTest {
    private val today get() = Store.today()
    private lateinit var book: Book

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
        com.exacta.reader.data.DongPhu.reset()
        com.exacta.reader.data.TongMon.reset()
        com.exacta.reader.data.BiCanh.reset()
        com.exacta.reader.data.DaoLo.reset()
        com.exacta.reader.data.LinhGioi.reset()
        TuLuyen.reset()
        TuLuyen.started = true
        TuLuyen.lastTick = today - 1
        TuLuyen.rng = roll(0)
        DaoPhap.migrate()
        book = Store.book("/t/book.epub").apply { words = 409_600; title = "Thử" }
    }

    private fun read(words: Long) {
        var left = words
        while (left > 0) {
            val n = minOf(400L, left)
            Store.moveTo(book, book.maxProgress + n.toDouble() / book.words, pageTurn = true)
            left -= n
        }
    }

    private fun days(n: Int, minutes: Int) {
        for (d in 0 until n) Store.daySeconds[today - d] = minutes * 60L
    }

    /** Three hours in the evening (Thổ), one at dawn (Kim). */
    private fun evenings() {
        Store.hourSeconds[19] = 3 * 3600L
        Store.hourSeconds[6] = 3600L
    }

    @Test
    fun theStarterRunsAtFullSpeed() {
        assertEquals(PhapMon.DAN_KHI, DaoPhap.active)
        assertEquals(100, DaoPhap.speed())
        read(4_000)
        assertEquals(4_000, Store.tuVi())
    }

    @Test
    fun linhCănAwakensAtTầng3FromTheHours() {
        evenings()
        Store.totalWords = 25_000            // sơ kỳ
        Store.checkRealm()
        assertFalse(DaoPhap.awakened())
        Store.totalWords = 35_000            // trung kỳ
        Store.checkRealm()
        assertTrue(DaoPhap.awakened())
        assertEquals(listOf(PhapMon.THO, PhapMon.KIM), DaoPhap.roots)
        assertEquals("Thổ Kim song linh căn", DaoPhap.rootName())
        // The gifts: a Hoàng phẩm công pháp of Thổ, running, and Thổ Độn.
        assertEquals(5, DaoPhap.active)
        assertTrue(DaoPhap.on(PhapMon.T_THO_DON))
        // 100 + 15 (song linh căn) + 0 (Hoàng, tầng 1) + 20 (hợp).
        assertEquals(135, DaoPhap.speed())
        read(4_000)
        assertEquals(35_000 + 5_400, Store.tuVi())
    }

    @Test
    fun khắcAndTheCostOfLearning() {
        Store.hourSeconds[6] = 4 * 3600L     // all at dawn: Thiên linh căn Kim
        Store.totalWords = 35_000
        Store.checkRealm()
        assertEquals("Thiên linh căn Kim", DaoPhap.rootName())
        assertEquals(30, DaoPhap.rootBonus())
        assertEquals(-20, DaoPhap.affinity(PhapMon.MOC))   // Kim khắc Mộc
        assertEquals(-20, DaoPhap.affinity(PhapMon.HOA))   // Hỏa khắc Kim
        assertEquals(0, DaoPhap.affinity(PhapMon.THUY))
        assertEquals(0, DaoPhap.learnCost(PhapMon.T_KIM_CANG))
        assertEquals(3, DaoPhap.learnCost(PhapMon.T_MOC_GIAP))
        assertEquals(2, DaoPhap.learnCost(PhapMon.T_THUY_MAC))
    }

    @Test
    fun tầngWaitForNgộTính() {
        val id = DaoPhap.active              // Dẫn Khí Quyết, 1,5 vạn a tầng
        DaoPhap.onWords(50_000)
        assertEquals(3, DaoPhap.tang())
        assertTrue(DaoPhap.waiting())
        assertEquals(45_000, DaoPhap.cong[id]!!.words)
        assertEquals(1, DaoPhap.openCost())
        assertFalse(DaoPhap.open())
        DaoPhap.gainNgo(1, "thử")
        assertTrue(DaoPhap.open())
        assertEquals(4, DaoPhap.tang())
        assertEquals(0, DaoPhap.ngo)
        DaoPhap.onWords(100_000)
        assertEquals(6, DaoPhap.tang())
        assertEquals(7, DaoPhap.nextLock())
        assertEquals(2, DaoPhap.openCost())
    }

    @Test
    fun aBookGivesNgộTính() {
        read(409_600)
        // Tầng 4 and 7 of the book, read through, and a long book.
        assertEquals(4, DaoPhap.ngo)
    }

    @Test
    fun theGatesAskForAPhẩm() {
        Store.realmCap = 1
        TuLuyen.subCap = 3
        Store.totalWords = 300_000
        days(30, 30)
        assertFalse(DaoPhap.canBreakWith(2))
        assertTrue(TuLuyen.needs(trial = false).any { !it.met && it.text.startsWith("Công pháp Huyền phẩm") })
        DaoPhap.gainCong(8)                  // Huyền Thủy Chân Kinh
        assertFalse(DaoPhap.canBreakWith(2))  // owned, not running
        DaoPhap.run(8)
        assertTrue(DaoPhap.canBreakWith(2))
        com.exacta.reader.data.DaoLo.choose(com.exacta.reader.data.DaoLo.PHAP)
        assertTrue(TuLuyen.canStartTrial())
        assertTrue(TuLuyen.parts().any { it.first.startsWith("Công pháp Huyền") && it.second == 2 })
    }

    @Test
    fun tâmPhápNhậpMônByLongSittings() {
        Store.realmCap = 1
        Store.checkRealm()
        assertTrue(PhapMon.TP_THANH_TAM in DaoPhap.tamOn)
        assertFalse(DaoPhap.tamWorks(PhapMon.TP_THANH_TAM))
        repeat(5) { TuLuyen.onSeconds(book, 60, TuLuyen.TAM_SITTING) }
        assertTrue(DaoPhap.tamWorks(PhapMon.TP_THANH_TAM))
        TuLuyen.tamMa = 0
        TuLuyen.stir(10)
        assertEquals(5, TuLuyen.tamMa)
    }

    @Test
    fun thuậtPhápSoftenAFall() {
        Store.totalWords = 100_000
        days(20, 30)
        DaoPhap.gainThuat(PhapMon.T_PHONG_DON)
        DaoPhap.gainThuat(PhapMon.T_THO_DON)
        assertFalse(DaoPhap.learn(PhapMon.T_PHONG_DON))   // not awakened: one ngộ tính each
        DaoPhap.ngo = 2
        assertTrue(DaoPhap.learn(PhapMon.T_PHONG_DON))
        assertTrue(DaoPhap.learn(PhapMon.T_THO_DON))
        assertEquals(0, DaoPhap.ngo)
        assertTrue(DaoPhap.on(PhapMon.T_PHONG_DON) && DaoPhap.on(PhapMon.T_THO_DON))
        TuLuyen.rng = roll(99)
        assertEquals(false, TuLuyen.breakThrough())
        assertEquals(1_000, Store.tuViLoss)        // hụt khí: 1% with Thổ Độn
        assertEquals(today + 1, TuLuyen.restUntil) // hồi khí 1 day with Phong Độn
        assertEquals(1, DaoPhap.thuat[PhapMon.T_PHONG_DON]!!.uses)
    }

    @Test
    fun thiênNhãnBlocksAnAttack() {
        Store.realmCap = 2
        TuLuyen.subCap = 3
        Store.totalWords = 600_000
        days(30, 40)
        DaoPhap.gainCong(13)
        DaoPhap.run(13)
        DaoPhap.gainThuat(PhapMon.T_THIEN_NHAN)
        DaoPhap.ngo = 5
        DaoPhap.learn(PhapMon.T_THIEN_NHAN)
        assertTrue(TuLuyen.startTrial())
        TuLuyen.onJump()
        assertEquals(0, TuLuyen.attacks)
        TuLuyen.onJump()
        assertEquals(1, TuLuyen.attacks)
    }

    @Test
    fun tàngKinhCácOffersWhatTheGateAsks() {
        Store.realmCap = 1
        val offers = DaoPhap.weekOffers()
        assertEquals(PhapMon.CONG, offers[0].first)
        assertEquals(1, PhapMon.CONG_PHAP[offers[0].second].pham)   // Huyền for Kim Đan
        assertEquals(8, offers.size)
        val (kind, id) = offers[0]
        assertFalse(DaoPhap.buy(kind, id))
        Store.linhThach = 500
        assertTrue(DaoPhap.buy(kind, id))
        assertEquals(380, Store.linhThach)
        assertTrue(DaoPhap.owns(kind, id))
        // The week's list stays as it was.
        assertEquals(offers[0], DaoPhap.weekOffers()[0])
    }

    @Test
    fun aScrollAlreadyKnownIsNgộTính() {
        // Every công pháp owned: a scroll of công pháp turns into ngộ tính.
        PhapMon.CONG_PHAP.indices.forEach { DaoPhap.gainCong(it) }
        val before = DaoPhap.ngo
        val text = DaoPhap.dropScroll(roll(0))
        assertEquals(before + 1, DaoPhap.ngo)
        assertTrue(text.contains("ngộ tính"))
    }

    @Test
    fun theMoveFrom20() {
        evenings()
        Store.realmCap = 2
        Store.totalWords = 400_000
        Store.book("/t/a.epub").apply { words = 250_000; tang = 10; finishDay = today - 3 }
        Store.book("/t/b.epub").apply { words = 80_000; tang = 5 }
        DaoPhap.migrate()
        assertEquals(4 + 1, DaoPhap.ngo)
        assertTrue(DaoPhap.awakened())
        assertEquals(1, PhapMon.CONG_PHAP[DaoPhap.active].pham)       // Huyền: Kim Đan stays open
        assertEquals(PhapMon.THO, PhapMon.CONG_PHAP[DaoPhap.active].attr)
        assertTrue(PhapMon.TP_THANH_TAM in DaoPhap.tamOn)
        assertFalse(DaoPhap.canBreakWith(3))                          // Nguyên Anh asks for Địa
    }

    @Test
    fun savedAndRead() {
        evenings()
        Store.totalWords = 35_000
        Store.checkRealm()
        DaoPhap.ngo = 7
        DaoPhap.onWords(20_000)
        DaoPhap.gainTam(PhapMon.TP_VONG_NGA)
        DaoPhap.tam[PhapMon.TP_VONG_NGA] = 12
        DaoPhap.weekOffers()
        val o = JSONObject(DaoPhap.toJson().toString())
        val roots = DaoPhap.roots.toList()
        val active = DaoPhap.active
        val offers = DaoPhap.offers.toList()
        DaoPhap.reset()
        DaoPhap.read(o)
        assertEquals(roots, DaoPhap.roots)
        assertEquals(active, DaoPhap.active)
        assertEquals(20_000, DaoPhap.cong[active]!!.words)
        assertEquals(7, DaoPhap.ngo)
        assertEquals(12, DaoPhap.tam[PhapMon.TP_VONG_NGA])
        assertEquals(offers, DaoPhap.offers)
        assertTrue(DaoPhap.on(PhapMon.T_THO_DON))
    }
}
