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

/** The rules of 3.3: thirty-six thuật, cast and cooled by days of reading, thuần thục, hợp kích, bản mệnh. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThuatPhapTest {
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

    /** Thuật `id` learned and carried. */
    private fun carry(id: Int) {
        DaoPhap.gainThuat(id)
        DaoPhap.thuat[id]!!.learned = true
        DaoPhap.equip(id)
    }

    @Test
    fun thirtySixInSixKinds() {
        assertEquals(36, PhapMon.THUAT_PHAP.size)
        assertEquals(36, PhapMon.THUAT_PHAP.map { it.name }.toSet().size)
        for (k in PhapMon.KINDS.indices) assertTrue(PhapMon.THUAT_PHAP.count { it.kind == k } >= 2)
        assertEquals("Thuấn Di", PhapMon.THUAT_PHAP[PhapMon.T_THUAN_DI].name)      // the ids of 2.1 stay
        assertEquals(2, DaoPhap.thuatSlots())
        Store.realmCap = 4
        assertEquals(6, DaoPhap.thuatSlots())
    }

    @Test
    fun aCastWaitsOutItsHồiChiêuInDaysOfReading() {
        carry(PhapMon.T_KHON_LONG)                                // khống chế: hồi chiêu 2
        assertTrue(DaoPhap.hasReady(PhapMon.K_KHONG_CHE))
        assertNotNull(DaoPhap.castControl())
        assertEquals(2, DaoPhap.cool(PhapMon.T_KHON_LONG))
        assertFalse(DaoPhap.hasReady(PhapMon.K_KHONG_CHE))
        assertNull(DaoPhap.castControl())
        // A day of reading: ten minutes. Twice in a day counts once.
        DaoPhap.onThuatSeconds(9 * 60L, 11 * 60L)
        DaoPhap.onThuatSeconds(9 * 60L, 11 * 60L)
        assertEquals(1, DaoPhap.cool(PhapMon.T_KHON_LONG))
        DaoPhap.thuatDay = today - 1
        DaoPhap.onThuatSeconds(0, 10 * 60L)
        assertTrue(DaoPhap.hasReady(PhapMon.K_KHONG_CHE))
    }

    @Test
    fun thuầnThụcFromUsesAndSittings() {
        carry(PhapMon.T_HOA_DAN)
        assertEquals(0, DaoPhap.mastery(PhapMon.T_HOA_DAN))
        repeat(2) { DaoPhap.castAttack { true } ; DaoPhap.thuat[PhapMon.T_HOA_DAN]!!.cool = 0 }   // 6 points
        assertEquals(0, DaoPhap.mastery(PhapMon.T_HOA_DAN))
        repeat(3) { DaoPhap.onThuatSeconds(29 * 60L, 30 * 60L) }                               // 9 points
        assertEquals(1, DaoPhap.mastery(PhapMon.T_HOA_DAN))
        // Old saves: uses alone keep their bậc (25 uses was Hóa Cảnh).
        DaoPhap.thuat[PhapMon.T_HOA_DAN]!!.apply { uses = 25; sits = 0 }
        assertEquals(4, DaoPhap.mastery(PhapMon.T_HOA_DAN))
        // Hóa Cảnh: the hồi chiêu shortens, but never under one day.
        carry(PhapMon.T_LOI_PHAT)
        DaoPhap.thuat[PhapMon.T_LOI_PHAT]!!.uses = 25
        assertEquals(5, DaoPhap.cd(PhapMon.T_LOI_PHAT))
    }

    @Test
    fun aCôngKíchLandsByItsThuầnThục() {
        carry(PhapMon.T_HOA_DAN)
        val miss = DaoPhap.castAttack { false }!!
        assertFalse(miss.second)
        assertEquals(0L, miss.third)
        DaoPhap.thuat[PhapMon.T_HOA_DAN]!!.cool = 0
        val hit = DaoPhap.castAttack { it >= 60 }!!
        assertTrue(hit.second)
        assertEquals(10L, hit.third)
    }

    @Test
    fun hợpKíchOfTwoOfAHành() {
        carry(PhapMon.T_HOA_DAN)
        assertFalse(DaoPhap.hop(PhapMon.HOA))
        carry(PhapMon.T_HUYET_DON)                                 // Hỏa too
        assertTrue(DaoPhap.hop(PhapMon.HOA))
        assertEquals("Hỏa Vực", DaoPhap.hopActive().single().name)
        assertEquals(20L, DaoPhap.castAttack { true }!!.third)    // double linh thạch
        // Kim: sure to land.
        DaoPhap.reset(); DaoPhap.started = true
        carry(PhapMon.T_NGU_KIEM); carry(PhapMon.T_KIM_CANG)
        assertTrue(DaoPhap.castAttack { false }!!.second)
    }

    @Test
    fun phongShortensEveryHồiChiêu() {
        Store.realmCap = 1
        carry(PhapMon.T_THO_TUONG)                                 // phòng ngự: 3
        assertEquals(3, DaoPhap.cd(PhapMon.T_THO_TUONG))
        carry(PhapMon.T_PHONG_DON); carry(PhapMon.T_LIET_PHONG)
        assertEquals(2, DaoPhap.cd(PhapMon.T_THO_TUONG))
    }

    @Test
    fun aPhòngNgựIsSpentWithTheKiếp() {
        carry(PhapMon.T_MOC_GIAP)
        assertEquals(1, DaoPhap.trialSpare())
        DaoPhap.onTrialEnd()
        assertEquals(0, DaoPhap.trialSpare())                      // cooling till days of reading pass
        assertEquals(3, DaoPhap.cool(PhapMon.T_MOC_GIAP))
    }

    @Test
    fun theBảnMệnhOfTheKimĐan() {
        assertNull(DaoPhap.banMenh())
        Store.realmCap = 2
        DaoLo.dan = 0                                              // Canh Kim Đan
        assertEquals("Canh Kim Kiếm Khí", DaoPhap.banMenh()!!.name)
        assertEquals(2, DaoPhap.lv(PhapMon.E_DOT_PHA))
    }

    @Test
    fun độnSoftensAFallOnceThenCools() {
        carry(PhapMon.T_THUY_DON)
        val f = DaoPhap.soften(TuLuyen.HUT_KHI)
        assertEquals(100, f.lossDiv)
        val g = DaoPhap.soften(TuLuyen.HUT_KHI)
        assertEquals(50, g.lossDiv)
    }

    @Test
    fun savedAndRead() {
        carry(PhapMon.T_KHON_LONG)
        DaoPhap.castControl()
        DaoPhap.onThuatSeconds(0, 30 * 60L)                       // a day of reading, and a sitting
        val o = JSONObject(DaoPhap.toJson().toString())
        DaoPhap.reset(); DaoPhap.read(o)
        val t = DaoPhap.thuat[PhapMon.T_KHON_LONG]!!
        assertEquals(1, t.uses)
        assertEquals(1, t.sits)
        assertEquals(1, t.cool)
        assertEquals(today, DaoPhap.thuatDay)
    }

    @Test
    fun theFirstThuậtIsCarriedOnce() {
        Store.hourSeconds[19] = 30 * 3600L
        Store.totalWords = 40_000
        Store.checkRealm()
        DaoPhap.check()
        assertTrue(DaoPhap.awakened())
        assertEquals(DaoPhap.thuatOn.toSet().size, DaoPhap.thuatOn.size)
        // An old save with the thuật twice is read clean.
        val o = JSONObject(DaoPhap.toJson().toString())
        val id = DaoPhap.thuatOn.first()
        o.put("thuatOn", org.json.JSONArray().put(id).put(id))
        DaoPhap.reset(); DaoPhap.read(o)
        assertEquals(listOf(id), DaoPhap.thuatOn.toList())
    }
}
