package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.Book
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.Store
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TuLuyen
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

/** The rules of 2.4: the five ways, luyện thể and luyện thần, kiêm tu, Song Anh. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DaoLoTest {
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
        TuLuyen.reset()
        TuLuyen.started = true
        TuLuyen.lastTick = today - 1
        TuLuyen.rng = roll(0)
        DaoPhap.reset()
        DongPhu.reset()
        TongMon.reset()
        BiCanh.reset()
        DaoLo.reset()
        DaoLo.started = true
        DaoLo.rng = roll(99)
    }

    @Test
    fun aWayAtTrúcCơOnce() {
        assertFalse(DaoLo.choose(DaoLo.PHAP))       // Luyện Khí: not yet
        Store.realmCap = 1
        assertTrue(DaoLo.canChoose())
        assertTrue(DaoLo.choose(DaoLo.THE))
        assertFalse(DaoLo.choose(DaoLo.PHAP))       // once
        assertEquals(2, Store.trialNeed(2))           // Thể tu: ngưng đan 2 days × 60
        assertEquals(60, Store.trialMinutes(2))
        assertNull(DaoLo.form())
        Store.realmCap = 3
        assertEquals("Mậu Thổ Thể Anh", DaoLo.form())
    }

    @Test
    fun speedOfEachWay() {
        Store.realmCap = 1
        val base = DaoPhap.speed()
        DaoLo.choose(DaoLo.PHAP)
        assertEquals(base + 15, DaoPhap.speed())
        Store.realmCap = 3
        assertTrue(DaoLo.chooseSecond(DaoLo.THE))
        assertFalse(DaoLo.chooseSecond(DaoLo.THAN))   // once
        // Kiêm tu: −20, and Thể tu's weakness is not carried.
        assertEquals(base + 15 - 20, DaoPhap.speed())
        assertEquals(200, DaoLo.thePercent())
    }

    @Test
    fun theBodyAndTheMind() {
        Store.totalSeconds = 100 * 3600L
        assertEquals(100, TuLuyen.thePhach())
        Store.realmCap = 1
        DaoLo.choose(DaoLo.THAN)
        assertEquals(75, TuLuyen.thePhach())           // Thần tu: thể phách mỏng
        val b = Store.book("/t/a.epub").apply { author = "A"; finishDay = today; firstDay = today - 5 }
        assertEquals(12, TuLuyen.thanThuc())          // (5 + 1) × 2
        // Tâm ma rises by half.
        TuLuyen.stir(10)
        assertEquals(5, TuLuyen.tamMa)
        // A book of a new author: +5 000 linh lực.
        DaoLo.onFinish(b)
        assertEquals(5_000, Store.bonusTuVi)
    }

    @Test
    fun luyệnThểHelpsThroughTheKiếp() {
        Store.totalSeconds = 70 * 3600L                // Thiết Cốt
        Store.realmCap = 2
        assertTrue(DaoLo.breakParts(3).any { it.second == 2 })
        DaoLo.way = DaoLo.THE
        assertTrue(DaoLo.breakParts(3).any { it.second == 4 })   // Thể tu: 2 a tier
    }

    @Test
    fun kiếmÝChíMạng() {
        Store.realmCap = 1
        TuLuyen.subCap = 3
        Store.totalWords = 300_000
        for (d in 0 until 30) Store.daySeconds[today - d] = 1800
        DaoPhap.gainCong(6)
        DaoLo.choose(DaoLo.KIEM)
        TuLuyen.trialDone = 2
        DaoLo.rng = roll(0)
        assertEquals(true, TuLuyen.breakThrough())
        assertEquals(2, Store.realmCap)
        assertEquals(1, TuLuyen.subCap)
        assertEquals(1, DaoLo.crits)
        assertTrue(TuLuyen.lastResult.contains("Mậu Thổ Đan"))
        assertEquals("Kim Đan trung kỳ", TuLuyen.name())
    }

    @Test
    fun songAnh() {
        Store.realmCap = 3
        TuLuyen.subCap = 3
        Store.totalWords = TuLuyen.quarter(3, 3) + 1_000
        DaoLo.way = DaoLo.THE
        DaoLo.way2 = DaoLo.THAN
        assertFalse(DaoLo.canSong())                   // thể phách, thần thức short
        Store.totalSeconds = 150 * 3600L                // ×2 = 300: Kim Thân
        for (i in 0 until 6) Store.book("/t/$i.epub").apply { author = "T$i"; finishDay = today; firstDay = today - 40 }
        assertTrue(DaoLo.songNeeds().all { it.met })
        DaoLo.rng = roll(99)
        assertEquals(false, DaoLo.trySong())
        assertTrue(DaoLo.songResting())
        assertNull(DaoLo.trySong())
        DaoLo.songRest = today
        DaoLo.rng = roll(0)
        assertEquals(true, DaoLo.trySong())
        assertTrue(DaoLo.songAnh)
        assertTrue(DaoLo.breakParts(4).any { it.first == "Song Anh" && it.second == 5 })
    }

    @Test
    fun twelveKimĐanAndTheAnh() {
        DaoPhap.started = true
        DaoPhap.roots.add(com.exacta.reader.game.PhapMon.HOA)
        assertEquals(3, DaoLo.danFor())             // Bính Hỏa
        DaoPhap.di = com.exacta.reader.game.PhapMon.LOI
        assertEquals(5, DaoLo.danFor())             // Tử Lôi
        TuLuyen.breakFails = 3
        assertEquals(11, DaoLo.danFor())            // Tịch Diệt
        TuLuyen.breakFails = 0
        Store.realmCap = 2
        DaoLo.way = DaoLo.KIEM
        DaoLo.onBreak(2)
        assertEquals("Tử Lôi Đan", DaoLo.form())
        assertEquals(2, Store.trialNeed(3))         // lôi kiếp 4 − 2
        Store.realmCap = 3
        assertEquals("Tử Lôi Kiếm Anh", DaoLo.form())
        assertEquals(12, DaoLo.danSpeed())          // 8, half again in the anh
    }

    @Test
    fun sixHóaThầnWeighOnHợpThểAndĐộKiếp() {
        Store.realmCap = 4
        DaoLo.way = DaoLo.THE
        DaoLo.onBreak(4)
        assertEquals("Bất Hoại Pháp Thân", DaoLo.form())
        assertEquals(4, Store.trialNeed(6))         // 5 − 1
        assertTrue(DaoLo.breakParts(6).any { it.second == 10 })
        assertTrue(DaoLo.breakParts(8).any { it.first.startsWith("Bất Hoại") && it.second == 5 })
    }

    @Test
    fun nghiệpLựcFromPhiThăng() {
        for (i in 0 until 10) Store.book("/t/k$i.epub").apply { finishDay = today; firstDay = today - 3 }
        val k = DaoLo.karma()
        assertTrue(k >= 20)
        assertEquals(0, DaoLo.karmaPart(0))         // hidden before Phi Thăng
        Store.realmCap = 8
        assertEquals(k * 4 / 10, DaoLo.karmaPart(0))
        assertEquals(k * -5 / 10, DaoLo.karmaPart(1))
        TuLuyen.tamMa = 100
        assertEquals(k - 50, DaoLo.karma())
    }

    @Test
    fun savedAndRead() {
        Store.realmCap = 3
        DaoLo.choose(DaoLo.BACH)
        DaoLo.chooseSecond(DaoLo.KIEM)
        DaoLo.earned = 4_500
        val o = JSONObject(DaoLo.toJson().toString())
        DaoLo.reset()
        DaoLo.read(o)
        assertEquals(DaoLo.BACH, DaoLo.way)
        assertEquals(DaoLo.KIEM, DaoLo.way2)
        assertEquals(4_500, DaoLo.earned)
        assertEquals(15, DaoLo.brewBonus())
    }
}
