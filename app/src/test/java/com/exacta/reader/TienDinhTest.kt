package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.DaoLo
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DoKiep
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.LinhGioi
import com.exacta.reader.data.Store
import com.exacta.reader.data.TienDinh
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.PhapMon
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

/** The rules of 2.7: through Lục Giới Môn, what is carried, the tiên hiệu, the tiền đình. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TienDinhTest {
    private val today get() = Store.today()

    @Before
    fun clean() {
        Store.books.clear(); Store.daySeconds.clear(); Store.dayWords.clear(); Store.forge.clear(); Store.beasts.clear()
        Store.bag.fill(0); Store.items.fill(0); Store.bonusMinutes.clear(); Store.sealedDays.clear(); Store.titleDays.clear()
        Store.totalWords = 0; Store.totalSeconds = 0; Store.bonusTuVi = 0; Store.tuViLoss = 0; Store.setsDone = 0
        Store.realmCap = 0; Store.realmSeen = 0; Store.trialStart = -1; Store.trialPassed = 0; Store.failCount = 0
        Store.trialBonus = 0; Store.trialSpare = 0; Store.giftsAwarded = 0; Store.linhThach = 0; Store.kyNgoWords = 0; Store.kyNgoTarget = Long.MAX_VALUE / 2
        Store.hourSeconds.fill(0)
        Store.titleChosen = -1
        Store.luckRng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = until - 1 }
        TuLuyen.reset(); TuLuyen.started = true; TuLuyen.lastTick = today - 1
        DaoPhap.reset(); DongPhu.reset(); TongMon.reset(); BiCanh.reset(); DaoLo.reset(); LinhGioi.reset(); DoKiep.reset()
        TienDinh.reset(); TienDinh.started = true
    }

    @After
    fun after() {
        TienDinh.reset()
    }

    @Test
    fun crossingWritesWhatIsCarried() {
        Store.realmCap = 9
        Store.totalWords = TuLuyen.floor(9) + 50_000
        DaoPhap.started = true
        DaoPhap.roots.add(PhapMon.HOA)
        DaoLo.way = DaoLo.KIEM
        TuLuyen.breakFails = 2
        assertEquals(1, TuLuyen.destined())
        TienDinh.onCross()
        assertTrue(TienDinh.crossed())
        assertEquals(1, TienDinh.gioi)
        assertEquals("Huyết Nguyệt Ma Tôn", TienDinh.tienHieu())
        assertFalse(TienDinh.endSeen)
        assertTrue(TienDinh.atForecourt())
        assertEquals("Nghịch Tiên · tiền đình", TuLuyen.name())
        // Words after the crossing are kept at the tiền đình.
        Store.totalWords += 30_000
        assertEquals(30_000L, TienDinh.keptWords())
        val o = JSONObject(TienDinh.toJson().toString())
        TienDinh.reset(); TienDinh.read(o)
        assertEquals(1, TienDinh.gioi)
        assertEquals(TuLuyen.floor(9) + 50_000, TienDinh.wordsAtCross)
    }

    @Test
    fun cửuTiêuGoesOnWithItsPhẩm() {
        Store.realmCap = 9
        Store.totalWords = TuLuyen.floor(9) + 50_000
        TuLuyen.gioi = 0
        TienDinh.onCross(seen = true)
        assertFalse(TienDinh.atForecourt())
        assertTrue(TuLuyen.name().startsWith("Thiên Tiên"))
    }

    @Test
    fun oldImmortalsSeeTheEnd() {
        Store.realmCap = 9
        Store.totalWords = TuLuyen.floor(9)
        TienDinh.migrate()
        assertTrue(TienDinh.crossed())
        assertFalse(TienDinh.endSeen)
        TienDinh.reset()
        Store.realmCap = 5
        TienDinh.migrate()
        assertFalse(TienDinh.crossed())
    }
}
