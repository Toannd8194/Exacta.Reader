package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.Book
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.HongHoang
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.NhatKy
import com.exacta.reader.data.PhuLuc
import com.exacta.reader.data.QuyNhat
import com.exacta.reader.data.Store
import com.exacta.reader.data.TeLuyen
import com.exacta.reader.data.ThuHai
import com.exacta.reader.data.TienDinh
import com.exacta.reader.data.TienLo
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TranPhap
import com.exacta.reader.data.TrangBi
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.data.VongXuyen
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

/** The rules of 4.0: Hồng Hoang Cổ Địa, Vong Xuyên Bỉ Ngạn, Thư Hải Vô Nhai, and Quy Nhất. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LucGioiTest {
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

    /** A sitting of `minutes` on day `d`. */
    private fun day(d: Long, minutes: Int) {
        Store.daySeconds[d] = Store.daySeconds.getOrDefault(d, 0L) + minutes * 60L
        Store.dayWords[d] = 10_000
        NhatKy.day(d).apply { hours[10] += minutes * 60L; sittings.add(minutes * 60) }
    }

    private fun cross(g: Int, ago: Long = 10) {
        Store.realmCap = 9
        Store.totalWords = TuLuyen.floor(9)
        TienDinh.started = true
        TienDinh.gioi = g; TuLuyen.gioi = g
        TienDinh.crossDay = today - ago
        TienDinh.wordsAtCross = TuLuyen.floor(9)
    }

    private var nextId = 100
    private fun book(title: String, author: String = "", finish: Long = -1, words: Long = 100_000, genre: String = ""): Book {
        val b = Book("/books/$nextId.epub").apply {
            id = nextId; this.title = title; this.author = author; finishDay = finish; this.words = words; wordsRead = words; this.genre = genre
        }
        nextId++
        Store.books[b.path] = b
        return b
    }

    // --- Hồng Hoang --------------------------------------------------------------------------------

    @Test
    fun hồngHoangLivesOnLongSittings() {
        cross(3)
        day(today - 3, 30)                                      // short: nothing
        day(today - 2, 60)                                      // 6
        day(today - 1, 100)                                     // 10, twice
        TienLo.check()
        assertSame(HongHoang, TienLo.rules())
        assertEquals("Tổ Địa", TienLo.seat())
        assertEquals(6 + 20, TienLo.res())
        assertEquals("Chiến Tiên sơ kỳ", TuLuyen.name())
    }

    @Test
    fun aBloodWakesAtBậcTwoByTheWay() {
        cross(3)
        for (d in 1..6) day(today - d, 100)                       // long sittings: Long
        TienLo.check()
        TienLo.bac = 2
        TienLo.check()
        assertEquals(0, HongHoang.mach)
        val before = TienLo.percent()
        TienLo.bac = 6
        assertEquals(before + 6 + 3, TienLo.percent())          // hóa hình twice the blood, and the third chiến văn
    }

    @Test
    fun aBeastFallsToThreeSittings() {
        cross(3)
        TienLo.check()
        TienLo.openDay = Store.weekStart(today)
        val before = TienLo.resTenths
        val tt = TienLo.tienThach
        day(today, 50); day(today, 50)
        TienLo.check()
        assertEquals(0, HongHoang.kills)
        day(today, 46)
        TienLo.check()
        assertEquals(1, HongHoang.kills)
        assertEquals(before + 10L * HongHoang.HUNT_REWARD, TienLo.resTenths)
        assertEquals(tt + HongHoang.HUNT_TT, TienLo.tienThach)
        day(today, 50)
        TienLo.check()
        assertEquals(1, HongHoang.kills)                         // one a week
    }

    @Test
    fun theKiếpOfTheAncestorsAsksNinetyMinutes() {
        cross(3)
        day(today, 80)
        assertFalse(HongHoang.kiepDayKept(0, today))
        day(today, 95)
        assertTrue(HongHoang.kiepDayKept(0, today))
        assertFalse(HongHoang.kiepDayKept(2, today))
        assertEquals(7, HongHoang.kiepNeed(0))
        assertEquals(20, HongHoang.kiepNeed(2))
    }

    // --- Vong Xuyên --------------------------------------------------------------------------------

    @Test
    fun vongXuyênCountsTheSoulsAndTheBoat() {
        val left = book("Bỏ dở").apply { lastDay = today - 60; wordsRead = 3_000 }
        val freed = book("Siêu độ", finish = today - 20).apply { backDay = today - 40 }
        val old = book("Thuyền cũ", finish = today - 400)
        cross(4)
        TienLo.check()
        assertSame(VongXuyen, TienLo.rules())
        assertEquals(listOf(left), VongXuyen.souls())
        assertEquals(listOf(freed), VongXuyen.freed())
        TienLo.check()
        assertEquals(old.id, VongXuyen.boatBook)
        val before = TienLo.res()
        old.rereadDay = today
        TienLo.check()
        assertTrue(VongXuyen.boatDone)
        assertEquals(before + VongXuyen.BOAT_REWARD, TienLo.res())
    }

    @Test
    fun theDeadKnowNoBrokenChain() {
        cross(4)
        TienLo.check()
        Store.daySeconds[today - 3] = 1_200
        TuLuyen.tamMa = 10
        TuLuyen.lastTick = today - 3
        TuLuyen.tick()                                           // a day read, then a day missed
        assertEquals(10, TuLuyen.tamMa)
    }

    @Test
    fun theKiếpOfRebirthCountsBooks() {
        cross(4)
        TienLo.check()
        TienLo.kiepActive = 0; TienLo.kiepStart = today - 3
        book("Ngắn", finish = today - 300, words = 100_000).rereadDay = today - 1
        assertEquals(0, TienLo.kiepKept())
        book("Dài", finish = today - 300, words = 400_000).rereadDay = today - 1
        assertEquals(1, TienLo.kiepKept())
        TienLo.check()
        assertEquals(-1, TienLo.kiepActive)                      // the roll is pinned to pass
        assertTrue(TienLo.kiepDone(0))
    }

    // --- Thư Hải -----------------------------------------------------------------------------------

    @Test
    fun thưHảiBuildsIslandsAndVănMạch() {
        cross(5)
        for (i in 0 until 5) book("Kiếm $i", "Tác giả $i", finish = today - 30 + i, genre = "Tiên hiệp")
        book("Kiếm 0 tập 2", "Tác giả 0", finish = today - 5, genre = "Tiên hiệp")
        book("Kiếm 0 tập 3", "Tác giả 0", finish = today - 4, genre = "Tiên hiệp")
        TienLo.check()
        assertSame(ThuHai, TienLo.rules())
        val isl = ThuHai.islands()
        assertEquals(5, isl.size)
        assertEquals(3, isl.first().books.size)
        assertEquals(listOf("tiên hiệp"), ThuHai.vanMach())
        assertTrue(TienLo.parts().any { it.first == "Văn mạch 1" && it.second == 3 })
        TienLo.bac = 2
        assertEquals(3, ThuHai.tinNiem())                        // half of five islands and one large
        // A week ends: the tín niệm comes.
        ThuHai.tnWeek = Store.weekStart(today) - 14
        val before = TienLo.res()
        TienLo.check()
        assertEquals(before + 3, TienLo.res())
    }

    @Test
    fun theVănKiếpCountsNewAuthors() {
        cross(5)
        TienLo.check()
        TienLo.kiepActive = 0; TienLo.kiepStart = today - 10
        book("Cũ", "Lão tác giả", finish = today - 100)
        book("Mới của lão", "Lão tác giả", finish = today - 2)    // not a new author
        book("A", "Một", finish = today - 3)
        book("B", "Hai", finish = today - 2)
        assertEquals(2, TienLo.kiepKept())
        book("C", "Ba", finish = today - 1)
        TienLo.check()
        assertTrue(TienLo.kiepDone(0))
    }

    // --- Quy Nhất ----------------------------------------------------------------------------------

    @Test
    fun envoysComeFromBậcSixAndQuyNhấtOpens() {
        cross(3, ago = 40)
        TienLo.check()
        TienLo.check()
        assertEquals(-1, QuyNhat.envoy)                          // bậc 1: no envoy
        TienLo.bac = 6
        TienLo.check()
        assertTrue(QuyNhat.envoy in 0..5 && QuyNhat.envoy != 3)
        assertEquals(QuyNhat.envoyOf(TienLo.monthOf(today)), QuyNhat.envoy)
        QuyNhat.hnk = 590
        TienLo.bac = 9
        assertFalse(QuyNhat.canUnite())
        QuyNhat.hnk = 600
        assertTrue(QuyNhat.unite())
        assertTrue(QuyNhat.united())
        assertEquals("Quy Nhất · ${QuyNhat.TITLE}", TienLo.homeLines()[0].first)
    }

    @Test
    fun anEnvoyOfTheLongSittingPays() {
        cross(5, ago = 40)
        TienLo.check()
        TienLo.bac = 6
        QuyNhat.month = TienLo.monthOf(today); QuyNhat.envoy = 3; QuyNhat.done = false
        TienLo.openDay = TienLo.monthStart(today)
        for (i in 0 until 3) day(today, 95)
        TienLo.check()
        assertTrue(QuyNhat.done)
        assertEquals(QuyNhat.REWARD, QuyNhat.hnk)
    }

    @Test
    fun savedAndRead() {
        cross(3)
        TienLo.check()
        HongHoang.mach = 4; HongHoang.kills = 2
        VongXuyen.crossed = 3; VongXuyen.boatBook = 9
        ThuHai.tnTotal = 44
        QuyNhat.hnk = 120; QuyNhat.envoys = 2; QuyNhat.quyDay = today
        val o = JSONObject(TienLo.toJson().toString())
        TienLo.reset(); TienLo.read(o)
        assertEquals(4, HongHoang.mach)
        assertEquals(2, HongHoang.kills)
        assertEquals(3, VongXuyen.crossed)
        assertEquals(9, VongXuyen.boatBook)
        assertEquals(44, ThuHai.tnTotal)
        assertEquals(120, QuyNhat.hnk)
        assertTrue(QuyNhat.united())
    }
}
