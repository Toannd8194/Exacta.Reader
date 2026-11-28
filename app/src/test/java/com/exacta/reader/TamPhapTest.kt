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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The rules of 3.2: eighteen tâm pháp in four schools, five cảnh, the ways, xung khắc, three slots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TamPhapTest {
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
    }

    @Test
    fun eighteenInFourSchools() {
        assertEquals(18, PhapMon.TAM_PHAP.size)
        assertEquals(18, PhapMon.TAM_PHAP.map { it.name }.toSet().size)
        for (p in 0 until 4) assertTrue(PhapMon.TAM_PHAP.count { it.phai == p } >= 4)
        // The seven of 2.1 stay where they were, with their old line of text.
        assertEquals("Thanh Tâm Quyết", PhapMon.TAM_PHAP[PhapMon.TP_THANH_TAM].name)
        assertEquals("Tâm ma tăng chậm một nửa", PhapMon.TAM_PHAP[PhapMon.TP_THANH_TAM].text)
        assertEquals("Định Hồn Chân Ngôn", PhapMon.TAM_PHAP[PhapMon.TP_DINH_HON].name)
        assertTrue(PhapMon.TAM_PHAP.all { it.e1 != PhapMon.E_NONE && it.e2 != PhapMon.E_NONE })
        assertEquals(PhapMon.TAM_WAY.size, PhapMon.W_DEEP + 1)
    }

    @Test
    fun slotsOpenAtTrúcCơNguyênAnhLuyệnHư() {
        assertEquals(0, DaoPhap.tamSlots())
        Store.realmCap = 1; assertEquals(1, DaoPhap.tamSlots())
        Store.realmCap = 3; assertEquals(2, DaoPhap.tamSlots())
        Store.realmCap = 5; assertEquals(3, DaoPhap.tamSlots())
    }

    @Test
    fun fiveCảnhAndWhatEachGives() {
        Store.realmCap = 1
        DaoPhap.gainTam(PhapMon.TP_BAT_NHA)                          // thần thức, then ngộ tính
        assertEquals(0, DaoPhap.canh(PhapMon.TP_BAT_NHA))
        assertEquals(0, DaoPhap.tamLv(PhapMon.E_THAN_THUC))
        DaoPhap.tam[PhapMon.TP_BAT_NHA] = 5
        assertEquals(1, DaoPhap.tamLv(PhapMon.E_THAN_THUC))
        DaoPhap.tam[PhapMon.TP_BAT_NHA] = 15
        assertEquals(2, DaoPhap.tamLv(PhapMon.E_THAN_THUC))
        assertEquals(0, DaoPhap.tamLv(PhapMon.E_NGO_TINH))
        DaoPhap.tam[PhapMon.TP_BAT_NHA] = 30
        assertEquals(1, DaoPhap.tamLv(PhapMon.E_NGO_TINH))
        assertEquals(5, DaoPhap.tamCanhBonus())
        DaoPhap.tam[PhapMon.TP_BAT_NHA] = 100
        assertEquals(3, DaoPhap.tamLv(PhapMon.E_THAN_THUC))
        assertEquals(2, DaoPhap.lv(PhapMon.E_NGO_TINH))
        assertEquals(1, DaoPhap.hoaCanhStep())
        // Out of its slot it gives nothing.
        DaoPhap.stopTam(PhapMon.TP_BAT_NHA)
        assertEquals(0, DaoPhap.lv(PhapMon.E_NGO_TINH))
        assertEquals(0, DaoPhap.hoaCanhStep())
    }

    @Test
    fun aSittingInTheWayTurnsTwice() {
        Store.realmCap = 1
        DaoPhap.gainTam(PhapMon.TP_THANH_TAM)                        // the way: no jump today
        DaoPhap.onTamSeconds(null, 29 * 60L, 30 * 60L)
        assertEquals(2, DaoPhap.turns(PhapMon.TP_THANH_TAM))
        TuLuyen.dayJumps[today] = 1
        DaoPhap.onTamSeconds(null, 29 * 60L, 30 * 60L)
        assertEquals(3, DaoPhap.turns(PhapMon.TP_THANH_TAM))
    }

    @Test
    fun aLongWayIsJudgedAtItsMinute() {
        Store.realmCap = 1
        DaoPhap.gainTam(PhapMon.TP_VONG_NGA)                         // 90 minutes
        DaoPhap.onTamSeconds(null, 0, 30 * 60L)
        assertEquals(1, DaoPhap.turns(PhapMon.TP_VONG_NGA))
        DaoPhap.onTamSeconds(null, 30 * 60L, 89 * 60L)
        assertEquals(1, DaoPhap.turns(PhapMon.TP_VONG_NGA))
        DaoPhap.onTamSeconds(null, 89 * 60L, 91 * 60L)
        assertEquals(2, DaoPhap.turns(PhapMon.TP_VONG_NGA))
    }

    @Test
    fun hạoNhiênTurnsWithABookReadThrough() {
        Store.realmCap = 1
        DaoPhap.gainTam(PhapMon.TP_HAO_NHIEN)
        DaoPhap.onFinish(Store.book("x").apply { words = 50_000 })
        assertEquals(PhapMon.FINISH_TURNS, DaoPhap.turns(PhapMon.TP_HAO_NHIEN))
        assertEquals(1, DaoPhap.canh(PhapMon.TP_HAO_NHIEN))
    }

    @Test
    fun maĐạoWillNotSitBesidePhậtOrNho() {
        Store.realmCap = 5
        DaoPhap.gainTam(PhapMon.TP_DINH_HON)                         // Phật
        DaoPhap.gainTam(PhapMon.TP_HUYET_TAM)                        // Ma: not put in a slot
        assertFalse(PhapMon.TP_HUYET_TAM in DaoPhap.tamOn)
        assertEquals(PhapMon.TP_DINH_HON, DaoPhap.clashWith(PhapMon.TP_HUYET_TAM))
        assertFalse(DaoPhap.runTam(PhapMon.TP_HUYET_TAM))
        DaoPhap.stopTam(PhapMon.TP_DINH_HON)
        assertNull(DaoPhap.clashWith(PhapMon.TP_HUYET_TAM))
        assertTrue(DaoPhap.runTam(PhapMon.TP_HUYET_TAM))
        // The price: tâm ma rises 15% faster.
        assertEquals(115, DaoPhap.stirPercent())
        DaoPhap.gainTam(PhapMon.TP_THANH_TAM)                        // Đạo goes with Ma
        assertTrue(PhapMon.TP_THANH_TAM in DaoPhap.tamOn)
    }

    @Test
    fun đạoBesidePhậtTurnsAFifthMore() {
        Store.realmCap = 3
        DaoPhap.gainTam(PhapMon.TP_TINH_TOA)                         // Đạo, way 45 minutes
        DaoPhap.gainTam(PhapMon.TP_VO_NIEM)                          // Phật, way 60 minutes
        assertTrue(DaoPhap.daoPhat())
        repeat(5) { DaoPhap.onTamSeconds(null, 0, 30 * 60L) }
        assertEquals(6, DaoPhap.turns(PhapMon.TP_TINH_TOA))
        assertEquals(6, DaoPhap.turns(PhapMon.TP_VO_NIEM))
    }

    @Test
    fun hóaCảnhLiftsTâmCảnh() {
        Store.realmCap = 1
        val before = TuLuyen.tamCanhTier()
        DaoPhap.gainTam(PhapMon.TP_TAM_TINH)
        DaoPhap.tam[PhapMon.TP_TAM_TINH] = 100
        // Tam Tỉnh gives tâm cảnh itself at Hóa cảnh; the tier is at least one above.
        assertTrue(TuLuyen.tamCanhTier() >= before + 1)
    }

    @Test
    fun savedAndRead() {
        Store.realmCap = 3
        DaoPhap.gainTam(PhapMon.TP_TINH_TOA)
        DaoPhap.gainTam(PhapMon.TP_VO_NIEM)
        repeat(3) { DaoPhap.onTamSeconds(null, 0, 30 * 60L) }
        val o = JSONObject(DaoPhap.toJson().toString())
        val turns = DaoPhap.turns(PhapMon.TP_TINH_TOA)
        val frac = DaoPhap.tamFrac.toMap()
        DaoPhap.reset(); DaoPhap.read(o)
        assertEquals(turns, DaoPhap.turns(PhapMon.TP_TINH_TOA))
        assertEquals(frac, DaoPhap.tamFrac.toMap())
        assertEquals(3, DaoPhap.tamSittings)
        assertEquals(listOf(PhapMon.TP_TINH_TOA, PhapMon.TP_VO_NIEM), DaoPhap.tamOn.toList())
    }
}
