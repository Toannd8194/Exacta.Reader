package com.exacta.reader

import com.exacta.reader.data.KhaiLo
import com.exacta.reader.data.Ore
import com.exacta.reader.data.Store
import com.exacta.reader.data.TranPhap
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.Treasure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** 4.1: the six stages of a treasure in the furnace, and opening the furnace by its grade. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KhaiLoTest {
    private val today get() = Store.today()

    @Before
    fun clean() {
        Store.forge.clear(); Store.beasts.clear(); Store.bag.fill(0); Store.items.fill(0)
        Store.daySeconds.clear(); Store.bonusMinutes.clear(); Store.linhThach = 0; Store.tuViLoss = 0
        Store.realmCap = 0; Store.tinhThat = 1; Store.kyNgoTarget = Long.MAX_VALUE / 2
        Store.luckRng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = until - 1 }
        TuLuyen.reset(); TuLuyen.started = true; TuLuyen.lastTick = today - 1
        TranPhap.reset(); TranPhap.started = true
        KhaiLo.rng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = until - 1 }
    }

    private fun formed(tier: Int): Ore {
        val o = Ore(tier).apply { words = need }
        Store.forge.add(o)
        Store.complete(o, "Lò luyện")
        return o
    }

    @Test
    fun sixStagesByTheWords() {
        val o = Ore(1)
        assertEquals(1, KhaiLo.stage(o))
        o.words = o.need * 45 / 100
        assertEquals(3, KhaiLo.stage(o))
        assertEquals("Đúc phôi", KhaiLo.stageName(o))
        o.words = o.need * 99 / 100
        assertEquals(5, KhaiLo.stage(o))
        o.words = o.need
        assertEquals(6, KhaiLo.stage(o))
        assertEquals("Thành hình", KhaiLo.stageName(o))
    }

    @Test
    fun aHoàngTreasureComesOutWhenFormed() {
        formed(0)
        assertEquals(1, Store.beasts.size)
        assertTrue(Store.forge.isEmpty())
    }

    @Test
    fun aHuyềnTreasureWaitsForItsTask() {
        val o = formed(1)
        assertEquals(KhaiLo.TASK, o.khai)
        assertTrue(o.busy)
        assertEquals(0, Store.beasts.size)
        Store.daySeconds[today] = 16 * 60L
        Store.checkKiep()
        assertEquals(0, Store.beasts.size)                       // one day of two
        o.khaiStart = today - 1
        Store.daySeconds[today - 1] = 20 * 60L
        Store.checkKiep()
        assertEquals(1, Store.beasts.size)
        assertTrue(Store.forge.none { it === o })
    }

    @Test
    fun aThiênTreasureAsksTaskConditionAndKiếp() {
        val o = formed(3)
        assertEquals(KhaiLo.TASK, o.khai)
        assertEquals(3, o.cond)                                   // the rng pinned: the last of Thiên phẩm's
        assertEquals("Tâm ma dưới 30", KhaiLo.condText(o))
        o.khaiStart = today - 2
        for (d in 0..2) Store.daySeconds[today - d] = 30 * 60L
        TuLuyen.tamMa = 50
        Store.checkKiep()
        assertEquals(KhaiLo.COND, o.khai)                         // the task done, the condition not
        TuLuyen.tamMa = 20
        Store.checkKiep()
        assertEquals(KhaiLo.KIEP, o.khai)
        assertTrue(o.inKiep)
    }

    @Test
    fun tụLinhBuysAConditionNotMet() {
        val o = formed(4)
        o.khai = KhaiLo.COND
        o.cond = 4                                                // Nguyên Anh, not reached
        assertFalse(KhaiLo.condMet(o))
        assertFalse(KhaiLo.canTuLinh(o))
        Store.items[Treasure.TU_LINH_DAN] = 2
        TranPhap.built.add(TranPhap.TU_LINH); TranPhap.set.add(TranPhap.TU_LINH)
        Store.linhThach = 1_000
        Store.tinhThat = 4                                        // bế quan 75 phút
        Store.totalWords = 100_000
        assertTrue(KhaiLo.needs(o).joinToString { "${it.first}=${it.second}" }, KhaiLo.canTuLinh(o))
        assertTrue(KhaiLo.startTuLinh(o))
        assertEquals(0, Store.items[Treasure.TU_LINH_DAN])
        assertEquals(300, Store.linhThach)
        assertEquals(30_000L, Store.tuViLoss)
        KhaiLo.onBeQuan(60)
        assertEquals(1, o.tuLinh)                                 // too short
        KhaiLo.onBeQuan(75)
        assertEquals(2, o.tuLinh)
        Store.checkKiep()
        assertEquals(KhaiLo.KIEP, o.khai)
        Store.totalWords = 0
    }
}
