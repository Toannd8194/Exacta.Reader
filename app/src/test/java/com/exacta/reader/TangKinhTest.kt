package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
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
import com.exacta.reader.game.PhapMon
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 3.4: nine floors, the án tham ngộ, tàn quyển, phế công, đồ phổ, tự sáng công pháp. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TangKinhTest {
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
        TienLo.reset(); NhatKy.reset()
        TeLuyen.reset(); TranPhap.reset(); TrangBi.reset(); PhuLuc.reset()
        DaoPhap.reset(); DaoPhap.started = true
        TongMon.started = true
    }

    /** Công pháp `id` owned and read to viên mãn. */
    private fun complete(id: Int) {
        DaoPhap.gainCong(id)
        DaoPhap.cong[id]!!.opened = 9
        DaoPhap.cong[id]!!.words = 9 * PhapMon.TANG_WORDS[PhapMon.CONG_PHAP[id].pham]
    }

    @Test
    fun theFloorsOpenBySectAndRank() {
        for (f in 1..3) assertTrue(DaoPhap.floorOpen(f))
        assertFalse(DaoPhap.floorOpen(4))
        DaoPhap.roots.add(PhapMon.KIM)                     // a sect takes the awakened only
        assertTrue(TongMon.join(TongMon.VAN_QUYEN, check = false))
        assertFalse(DaoPhap.floorOpen(4))
        TongMon.gongsTotal = 300
        assertTrue(DaoPhap.floorOpen(4))
        assertFalse(DaoPhap.floorOpen(5))
        assertFalse(DaoPhap.floorOpen(7))
        // Every ngọc giản has its floor: Hoàng on Nhất lâu … Tiên on Cửu lâu.
        assertEquals(1, PhapMon.floorOf(PhapMon.CONG, 1))
        assertEquals(9, PhapMon.floorOf(PhapMon.CONG, PhapMon.ofLine(0, 5)!!))
        assertEquals(36, PhapMon.THUAT_FLOOR.size)
        assertEquals(18, PhapMon.TAM_FLOOR.size)
    }

    @Test
    fun theGateCôngPhápIsKeptLowWhenItsFloorIsShut() {
        Store.realmCap = 3                                  // the next gate asks Thiên phẩm, kept on Ngũ lâu
        val o = DaoPhap.weekOffers()[0]
        assertEquals(PhapMon.CONG, o.kind)
        assertEquals(3, PhapMon.CONG_PHAP[o.id].pham)
        assertEquals(3, o.floor)
        assertEquals(300L, DaoPhap.price(o))               // dearer down here: 60 a floor
        assertTrue(DaoPhap.weekOffers().drop(1).none { it.floor >= 4 && DaoPhap.floorOpen(it.floor) })
    }

    @Test
    fun wordsReadFlowThroughTheÁn() {
        DaoPhap.toShelf(PhapMon.CONG, 1)                    // Hoàng: 1 vạn
        DaoPhap.toShelf(PhapMon.THUAT, PhapMon.T_HOA_DAN)  // Nhất lâu: 1 vạn
        assertEquals(2, DaoPhap.shelf.size)
        DaoPhap.onWords(15_000)
        assertTrue(DaoPhap.owns(PhapMon.CONG, 1))
        assertEquals(5_000L, DaoPhap.shelf[0].done)        // the rest went on to the next
        DaoPhap.toShelf(PhapMon.TAM, PhapMon.TP_THANH_TAM)
        DaoPhap.putOnDesk(1)
        assertEquals(PhapMon.TAM, DaoPhap.shelf[0].kind)
        assertFalse(DaoPhap.toShelf(PhapMon.TAM, PhapMon.TP_THANH_TAM))   // once
    }

    @Test
    fun linhAndTiênComeInPieces() {
        Store.realmCap = 5                                  // the gates ask Linh phẩm now
        val id = PhapMon.ofLine(0, 4)!!
        val need = PhapMon.fragNeed(id)
        // Every công pháp but this one owned, so the drop must be it.
        (0 until PhapMon.BASE_CONG).filter { it != id }.forEach { DaoPhap.gainCong(it) }
        val first = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = 0 }
        repeat(need - 1) { assertTrue(DaoPhap.dropScroll(first).contains("tàn quyển")) }
        assertEquals(need - 1, DaoPhap.frags[id])
        DaoPhap.dropScroll(first)
        assertNull(DaoPhap.frags[id])
        assertTrue(DaoPhap.onShelf(PhapMon.CONG, id))
    }

    @Test
    fun phảnTôngThìPhếCông() {
        DaoPhap.roots.add(PhapMon.KIM)
        assertTrue(TongMon.join(TongMon.KIEM_LU, check = false))
        TongMon.gongsTotal = 300; TongMon.gongs = 300
        val id = 8                                          // a công pháp
        DaoPhap.toShelf(PhapMon.CONG, id, TongMon.sect)
        DaoPhap.study(1_000_000)
        assertEquals(TongMon.KIEM_LU, DaoPhap.cong[id]!!.sect)
        DaoPhap.cong[id]!!.opened = 9
        DaoPhap.cong[id]!!.words = 9 * PhapMon.TANG_WORDS[PhapMon.CONG_PHAP[id].pham]
        assertTrue(DaoPhap.slotOpen(id, 2))
        TongMon.leave()
        assertTrue(DaoPhap.crippled(id))
        assertTrue(DaoPhap.slotOpen(id, 0))
        assertFalse(DaoPhap.slotOpen(id, 1))
        assertFalse(DaoPhap.slotOpen(id, 2))
    }

    @Test
    fun theĐồPhổRemembersWhatWasSeen() {
        assertFalse(DaoPhap.seen(PhapMon.TAM, PhapMon.TP_HAO_NHIEN))
        DaoPhap.weekOffers()
        val shown = DaoPhap.offers.first { it.floor <= 3 && it.kind != PhapMon.DAN }
        assertTrue(DaoPhap.seen(shown.kind, shown.id))
        val o = JSONObject(DaoPhap.toJson().toString())
        DaoPhap.reset(); DaoPhap.read(o)
        assertTrue(DaoPhap.seen(shown.kind, shown.id))
    }

    @Test
    fun aCôngPhápOfOnesOwnMaking() {
        complete(1)                                         // Canh Kim Quyết, Hoàng
        complete(6)                                         // Phá Quân Kim Quyết, Huyền
        DaoPhap.ngo = 12
        assertFalse(DaoPhap.canCreate())                    // before Hóa Thần
        Store.realmCap = 4
        assertTrue(DaoPhap.canCreate())
        val e1 = PhapMon.CONG_PHAP[1].effect
        val e2 = PhapMon.CONG_PHAP[6].e2
        val id = DaoPhap.create(1, 6, e1, e2, "Thiên Kim Quyết")!!
        assertEquals(PhapMon.BASE_CONG, id)
        val d = PhapMon.CONG_PHAP[id]
        assertEquals(1, d.pham)                             // the lower plus one
        assertEquals(PhapMon.KIM, d.attr)
        assertEquals(e1, d.e3)                              // the first one's đặc hiệu again as tuyệt học
        assertEquals(PhapMon.F_NONE, d.flaw)
        assertEquals(2, DaoPhap.ngo)
        assertTrue(DaoPhap.owns(PhapMon.CONG, id))
        // Kept across a save, and gone with a reset.
        val o = JSONObject(DaoPhap.toJson().toString())
        DaoPhap.reset()
        assertEquals(PhapMon.BASE_CONG, PhapMon.CONG_PHAP.size)
        DaoPhap.read(o)
        assertEquals("Thiên Kim Quyết", PhapMon.CONG_PHAP[id].name)
        assertNotNull(DaoPhap.cong[id])
        assertNull(DaoPhap.create(1, 6, e1, e2, ""))
    }
}
