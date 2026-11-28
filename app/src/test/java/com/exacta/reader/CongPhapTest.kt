package com.exacta.reader

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
import com.exacta.reader.game.PhapMon
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 3.1: seventy-two công pháp in twelve lines, đặc hiệu by tầng, the way, xung quan, chuyển tu, phụ tu. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CongPhapTest {
    private val today get() = Store.today()

    @Before
    fun clean() {
        Store.books.clear(); Store.daySeconds.clear(); Store.dayWords.clear(); Store.forge.clear(); Store.beasts.clear()
        Store.bag.fill(0); Store.items.fill(0); Store.bonusMinutes.clear(); Store.sealedDays.clear(); Store.titleDays.clear()
        Store.totalWords = 0; Store.totalSeconds = 0; Store.bonusTuVi = 0; Store.tuViLoss = 0; Store.setsDone = 0
        Store.realmCap = 0; Store.realmSeen = 0; Store.trialStart = -1; Store.trialPassed = 0; Store.failCount = 0
        Store.trialBonus = 0; Store.trialSpare = 0; Store.giftsAwarded = 0; Store.linhThach = 0; Store.kyNgoWords = 0; Store.kyNgoTarget = Long.MAX_VALUE / 2
        Store.hourSeconds.fill(0); Store.sessionNow = 0
        Store.luckRng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = until - 1 }
        TuLuyen.reset(); TuLuyen.started = true; TuLuyen.lastTick = today - 1
        DongPhu.reset(); TongMon.reset(); BiCanh.reset(); DaoLo.reset(); LinhGioi.reset(); DoKiep.reset(); TienDinh.reset()
        com.exacta.reader.data.TienLo.reset(); com.exacta.reader.data.NhatKy.reset()
        TeLuyen.reset(); TranPhap.reset(); TrangBi.reset(); PhuLuc.reset()
        DaoPhap.reset(); DaoPhap.started = true
    }

    private fun need(id: Int) = PhapMon.TANG_WORDS[PhapMon.CONG_PHAP[id].pham]

    /** Công pháp `id` running, all tầng open, filled to `tang`. */
    private fun at(id: Int, tang: Int) {
        DaoPhap.gainCong(id)
        DaoPhap.run(id)
        DaoPhap.active = id
        DaoPhap.cong[id]!!.opened = 9
        DaoPhap.cong[id]!!.words = (tang - 1) * need(id) + (if (tang == 9) need(id) else 0)
    }

    @Test
    fun seventyTwoInTwelveLines() {
        assertEquals(72, PhapMon.CONG_PHAP.size)
        assertEquals(72, PhapMon.CONG_PHAP.map { it.name }.toSet().size)
        for (m in 0 until 12) for (p in 0 until 6) assertNotNull("mạch $m phẩm $p", PhapMon.ofLine(m, p))
        // The ids of 2.1 stay where they were.
        assertEquals("Dẫn Khí Quyết", PhapMon.CONG_PHAP[0].name)
        assertEquals("Thái Thượng Vong Tình Lục", PhapMon.CONG_PHAP[35].name)
        assertEquals(PhapMon.HON_NGUYEN, PhapMon.CONG_PHAP[0].mach)
        // From Địa phẩm, every công pháp asks something back.
        assertTrue(PhapMon.CONG_PHAP.filter { it.pham >= 2 }.all { it.flaw != PhapMon.F_NONE })
        assertTrue(PhapMon.CONG_PHAP.all { it.effect != PhapMon.E_NONE && it.e2 != PhapMon.E_NONE && it.e3 != PhapMon.E_NONE })
    }

    @Test
    fun đặcHiệuOpenByTầngAndAddUp() {
        val id = 1                                           // Canh Kim Quyết: áp chế · linh thạch · tuyệt học đột phá
        at(id, 1)
        assertEquals(1, DaoPhap.lv(PhapMon.E_AP_CHE))
        assertEquals(0, DaoPhap.lv(PhapMon.E_LINH_THACH))
        at(id, 5)
        assertEquals(1, DaoPhap.lv(PhapMon.E_LINH_THACH))
        assertEquals(0, DaoPhap.lv(PhapMon.E_DOT_PHA))
        at(id, 9)
        assertEquals(2, DaoPhap.lv(PhapMon.E_DOT_PHA))
        assertEquals("Tỉ lệ đột phá +10%", PhapMon.effectText(PhapMon.E_DOT_PHA, 2))
        // A kind twice: Liệt Hỏa Công's lò luyện and its tuyệt học.
        at(4, 9)
        assertEquals(3, DaoPhap.lv(PhapMon.E_LO_LUYEN))
    }

    @Test
    fun aFlawWeighs() {
        at(16, 1)                                            // Nam Minh Ly Hỏa Kinh: tâm ma tăng nhanh 20%
        assertTrue(DaoPhap.flaw(PhapMon.F_TAM_MA))
        assertEquals(120, DaoPhap.stirPercent())
        at(13, 1)                                            // Tinh Kim Bất Hoại Công: kỳ ngộ ít đi
        assertFalse(DaoPhap.flaw(PhapMon.F_TAM_MA))
        assertEquals(100, DaoPhap.stirPercent())
    }

    @Test
    fun theWayOfTheLine() {
        at(11, 1)                                            // Duệ Kim Kiếm Kinh: Kiếm, a book past half
        DaoPhap.cong[11]!!.words = 0
        val book = Store.book("b").apply { words = 100_000; maxProgress = 0.6; firstDay = today - 30 }
        DaoPhap.onWords(1_000, book)
        assertEquals(1_500L, DaoPhap.cong[11]!!.words)
        assertEquals(1_000L, DaoPhap.wayWords)
        book.maxProgress = 0.2
        DaoPhap.onWords(1_000, book)
        assertEquals(2_500L, DaoPhap.cong[11]!!.words)
        // Thể: a sitting of 45 minutes.
        assertFalse(DaoPhap.inWay(PhapMon.THE, book))
        Store.sessionNow = 46 * 60L
        assertTrue(DaoPhap.inWay(PhapMon.THE, book))
        // Phong: a book opened this week.
        book.firstDay = today - 2
        assertTrue(DaoPhap.inWay(PhapMon.PHONG, book))
    }

    @Test
    fun xungQuanInOneSitting() {
        DaoPhap.gainCong(1)
        DaoPhap.run(1)
        DaoPhap.active = 1
        DaoPhap.cong[1]!!.words = 3 * need(1)                // tầng 3 full: tầng 4 waits
        DaoPhap.ngo = 5
        assertTrue(DaoPhap.waiting())
        val cost = DaoPhap.openCost()
        assertTrue(DaoPhap.startXung())
        assertEquals(5 - cost, DaoPhap.ngo)
        DaoPhap.onSeconds(29 * 60L)
        assertEquals(4, DaoPhap.xung)
        // The sitting stops short: khí nghịch, the ngộ tính comes back.
        val tamMa = TuLuyen.tamMa
        DaoPhap.onSessionEnd(10 * 60L)
        assertEquals(0, DaoPhap.xung)
        assertEquals(5, DaoPhap.ngo)
        assertTrue(TuLuyen.tamMa > tamMa)
        // Again, and it holds.
        assertTrue(DaoPhap.startXung())
        DaoPhap.onSeconds(30 * 60L)
        assertEquals(0, DaoPhap.xung)
        assertEquals(6, DaoPhap.cong[1]!!.opened)
        assertFalse(DaoPhap.waiting())
    }

    @Test
    fun chuyểnTuCarriesHalfTheLine() {
        at(1, 9)                                             // Canh Kim Quyết viên mãn
        assertTrue(DaoPhap.complete(1))
        DaoPhap.gainCong(6)                                  // Phá Quân Kim Quyết, the next phẩm of Kim
        assertEquals(1, DaoPhap.inheritFrom(6))
        assertTrue(DaoPhap.run(6))
        val carried = 9 * need(1) / 2
        assertEquals(carried, DaoPhap.cong[6]!!.words)
        assertEquals(1 + (carried / need(6)).toInt(), DaoPhap.tang(6))
        assertEquals(null, DaoPhap.inheritFrom(6))           // once
        // Another line inherits nothing.
        DaoPhap.gainCong(8)
        assertEquals(null, DaoPhap.inheritFrom(8))
    }

    @Test
    fun phụTuFromKimĐan() {
        at(1, 1)                                             // Kim running
        DaoPhap.gainCong(3)                                  // Nhược Thủy Quyết: tĩnh tâm
        DaoPhap.gainCong(6)                                  // Kim again
        assertFalse(DaoPhap.setPhu(3))                       // before Kim Đan
        Store.realmCap = 2
        assertFalse(DaoPhap.setPhu(6))                       // the same line
        assertTrue(DaoPhap.setPhu(3))
        assertEquals(1, DaoPhap.lv(PhapMon.E_TINH_TAM))
        DaoPhap.cong[3]!!.words = 0
        DaoPhap.onWords(1_000)
        assertEquals(500L, DaoPhap.cong[3]!!.words)
        val o = JSONObject(DaoPhap.toJson().toString())
        DaoPhap.reset(); DaoPhap.read(o)
        assertEquals(3, DaoPhap.phu)
        // Running the phụ tu takes it out of the slot.
        assertTrue(DaoPhap.run(3))
        assertEquals(-1, DaoPhap.phu)
    }
}
