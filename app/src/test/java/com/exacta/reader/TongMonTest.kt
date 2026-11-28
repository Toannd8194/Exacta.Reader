package com.exacta.reader

import com.exacta.reader.data.BiCanh
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.Store
import com.exacta.reader.data.TongMon
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.KyNgo
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

/** The rules of 2.3: the sect, its tasks, rival and contest; the bí cảnh map; the small breakthrough; the 72 kỳ ngộ. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TongMonTest {
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
        Store.biCanhWeek = -1; Store.biCanhCount = 0; Store.biCanhBonusWeek = -1; Store.biCanhBonusDays = 0
        Store.hourSeconds.fill(0)
        Store.luckRng = object : Random() { override fun nextBits(bitCount: Int) = 0; override fun nextInt(until: Int) = until - 1 }
        TuLuyen.reset()
        TuLuyen.started = true
        TuLuyen.lastTick = today - 1
        TuLuyen.rng = roll(0)
        DaoPhap.reset()
        DongPhu.reset()
        TongMon.reset()
        TongMon.started = true
        BiCanh.reset()
        com.exacta.reader.data.DaoLo.reset()
        com.exacta.reader.data.LinhGioi.reset()
        com.exacta.reader.data.DoKiep.reset()
        com.exacta.reader.data.TienDinh.reset()
        com.exacta.reader.data.TeLuyen.reset(); com.exacta.reader.data.TranPhap.reset()
        com.exacta.reader.data.TrangBi.reset(); com.exacta.reader.data.PhuLuc.reset()
        BiCanh.started = true
        BiCanh.rng = roll(0)
    }

    private fun awaken() {
        DaoPhap.started = true
        DaoPhap.roots.add(0)
    }

    @Test
    fun aSectOnceTheRootAwakens() {
        assertFalse(TongMon.canJoin())
        assertFalse(TongMon.join(TongMon.KIEM_LU))
        awaken()
        assertTrue(TongMon.KIEM_LU in TongMon.choices())       // Kim
        assertEquals(TongMon.choices().first(), TongMon.suggested())
        assertEquals(8, TongMon.choiceCount())                 // Thiên linh căn
        assertTrue(TongMon.join(TongMon.KIEM_LU))
        assertEquals(3, TongMon.tasks.size)
        assertTrue(TongMon.rivalName.startsWith("Sư"))
        assertEquals("Ngoại môn", TongMon.rankName())
        // Kiếm Lư: a công pháp of Kiếm is always in harmony.
        assertEquals(20, DaoPhap.affinity(com.exacta.reader.game.PhapMon.KIEM))
    }

    @Test
    fun tasksPayCốngHiến() {
        awaken()
        TongMon.join(TongMon.MINH_TAM, check = false)
        TongMon.tasks.clear(); TongMon.tasks.addAll(listOf(0, 6, 4))
        val w = TongMon.week
        for (d in w..today) Store.daySeconds[d] = 1900
        Store.dayWords[today] = 40_000
        // Task 4: 3 vạn words this week.
        assertTrue(TongMon.done(2))
        assertTrue(TongMon.claim(2))
        assertFalse(TongMon.claim(2))
        assertEquals(40, TongMon.gongs)
        assertEquals(40, TongMon.gongsTotal)
    }

    @Test
    fun rankAsksTheRealmToo() {
        awaken()
        TongMon.join(TongMon.VAN_QUYEN, check = false)
        TongMon.gain(1_200, "thử")
        assertEquals(1, TongMon.rank())            // Chân truyền asks Kim Đan
        Store.realmCap = 2
        assertEquals(2, TongMon.rank())
        // Bảo khố: an Địa phẩm ore for 150.
        val gongs = TongMon.gongs
        assertTrue(TongMon.buy(5) != null)
        assertEquals(gongs - 150, TongMon.gongs)
        assertEquals(1, Store.bag[2] + Store.forge.count { it.tier == 2 && !it.isBook })   // to the bag, then the forge
    }

    @Test
    fun theRivalKeepsYourPace() {
        awaken()
        TongMon.join(TongMon.THIET_COT, check = false)
        TongMon.rivalWords = 10_000.0
        TongMon.rivalDay = today - 3
        for (d in 0..40) Store.dayWords[today - d] = 6_000
        Store.totalWords = 100_000
        TongMon.check()
        // Three days at 6 000 a day, a tenth faster.
        assertEquals(10_000.0 + 3 * 6_000 * 1.1, TongMon.rivalWords, 1.0)
        assertTrue(TongMon.ahead)
        assertEquals(40, TongMon.gongs)
    }

    @Test
    fun theContestOfTheQuarter() {
        awaken()
        TongMon.join(TongMon.BACH_THAO, check = false)
        TongMon.joinDay = today - 200
        TongMon.quarter = TongMon.quarterOf(today) - 1
        TongMon.check()
        assertEquals(8, TongMon.lastTable.size)
        assertTrue(TongMon.lastRank in 1..8)
        assertTrue(TongMon.gongs >= 20)
    }

    @Test
    fun theMapOfTheWeek() {
        BiCanh.check()
        assertEquals(5, BiCanh.length())
        assertFalse(BiCanh.atFork())
        Store.biCanhBonusWeek = BiCanh.week
        Store.biCanhBonusDays = 5
        assertTrue(BiCanh.atFork())
        assertTrue(BiCanh.choose(false))
        assertEquals(BiCanh.YEU_THU, BiCanh.current())
        // Liều đánh at a roll of 0: won.
        assertTrue(BiCanh.resolve(2)!!.startsWith("Thắng"))
        // Four more: Cấm chế "Bỏ qua" is always there; the last is the master.
        while (!BiCanh.done()) {
            if (BiCanh.atFork()) BiCanh.choose(true)
            val type = BiCanh.current()
            val i = BiCanh.choices(type).indexOfLast { it.ok }
            assertTrue(BiCanh.resolve(i) != null)
        }
        assertEquals(1, Store.biCanhCount)
        assertTrue(Store.biCanhDone())
    }

    @Test
    fun twoWoundsAreTrọngThương() {
        BiCanh.check()
        Store.biCanhBonusWeek = BiCanh.week
        Store.biCanhBonusDays = 5
        BiCanh.rng = roll(99)
        BiCanh.choose(false)
        BiCanh.resolve(2)                    // lost
        assertEquals(1, BiCanh.wounds)
        BiCanh.nodes.add(BiCanh.YEU_THU)
        BiCanh.resolve(2)
        assertTrue(BiCanh.fled)
        assertNull(BiCanh.resolve(0))
        assertEquals(Store.weekStart(today) + 7, BiCanh.woundWeek)
        assertEquals(0, Store.biCanhCount)
    }

    @Test
    fun theSmallBreakthroughFromKimĐan() {
        Store.realmCap = 2
        Store.totalWords = TuLuyen.quarter(2, 1) + 1_000
        for (d in 0 until 20) Store.daySeconds[today - d] = 1800     // căn cơ 40
        TuLuyen.advance()
        assertEquals(0, TuLuyen.subCap)
        assertTrue(TuLuyen.minorReady())
        assertEquals("Kim Đan sơ kỳ", TuLuyen.name())
        assertEquals(true, TuLuyen.minorBreak())
        assertEquals(1, TuLuyen.subCap)
        assertEquals("Kim Đan trung kỳ", TuLuyen.name())
        // Trúc Cơ still opens by căn cơ alone.
        Store.realmCap = 1
        TuLuyen.subCap = 0
        Store.totalWords = TuLuyen.quarter(1, 1) + 1_000
        TuLuyen.advance()
        assertEquals(1, TuLuyen.subCap)
    }

    @Test
    fun aFailedSmallBreakthroughIsLight() {
        Store.realmCap = 2
        Store.totalWords = TuLuyen.quarter(2, 1) + 1_000
        for (d in 0 until 20) Store.daySeconds[today - d] = 1800
        TuLuyen.rng = roll(99)
        assertEquals(false, TuLuyen.minorBreak())
        assertEquals(3, TuLuyen.tamMa)
        assertTrue(TuLuyen.minorResting())
        assertNull(TuLuyen.minorBreak())
        assertEquals(0, Store.tuViLoss)
    }

    @Test
    fun lụcGiớiByTheLifeOfCultivation() {
        Store.realmCap = 8
        TuLuyen.subCap = 3
        Store.totalWords = TuLuyen.floor(9)
        assertEquals("Phi Thăng đại viên mãn", TuLuyen.name())
        // Hỏa root, Kiếm tu, two failed breakthroughs: Huyết Nguyệt Trầm Uyên answers.
        DaoPhap.started = true
        DaoPhap.roots.add(com.exacta.reader.game.PhapMon.HOA)
        com.exacta.reader.data.DaoLo.way = com.exacta.reader.data.DaoLo.KIEM
        TuLuyen.breakFails = 2
        // Hỏa 30, Kiếm tu 20, đời tu 10, Bính Hỏa Đan 10, Kiếm Ý Thông Thiên 10, less nghiệp lực.
        val score = TuLuyen.gioiScore(1)
        assertEquals(80 + com.exacta.reader.data.DaoLo.karmaPart(1), score)
        assertEquals(1, TuLuyen.destined())
        assertTrue(TuLuyen.gioiClear())
        assertTrue(TuLuyen.needs().any { it.met && it.text.startsWith("Lục Giới cảm ứng rõ") })
        // Kiếm Lư adds 10 more.
        TongMon.sect = TongMon.KIEM_LU
        assertEquals(score + 10, TuLuyen.gioiScore(1))
        Store.realmCap = 9
        assertTrue(TuLuyen.name().startsWith("Nghịch Tiên"))
    }

    @Test
    fun seventyTwoKỳNgộByRarity() {
        val counts = KyNgo.EVENTS.groupingBy { it.rarity }.eachCount()
        assertEquals(mapOf(0 to 36, 1 to 18, 2 to 9, 3 to 6, 4 to 3), counts)
        assertEquals(72, KyNgo.EVENTS.map { it.name }.toSet().size)
        val order = KyNgo.ORDER.map { KyNgo.EVENTS[it].rarity }
        assertEquals(order.sorted(), order)
        assertTrue(KyNgo.OLD_NAMES.all { KyNgo.index(it) >= 0 })
    }

    @Test
    fun thirtySixSectsSixToEightOpen() {
        assertEquals(36, TongMon.SECTS.size)
        for (f in 0..4) assertEquals(f, TongMon.SECTS[f].family)
        DaoPhap.started = true
        DaoPhap.roots.addAll(listOf(com.exacta.reader.game.PhapMon.HOA, com.exacta.reader.game.PhapMon.THUY, com.exacta.reader.game.PhapMon.THO))
        assertEquals(6, TongMon.choiceCount())
        val list = TongMon.choices()
        assertTrue(list.size <= 6 && list.isNotEmpty())
        assertTrue(list.all { TongMon.admits(it) })
        assertTrue(TongMon.SECTS.indices.first { TongMon.SECTS[it].name == "Xích Viêm Cốc" } in list)
        assertFalse(TongMon.join(TongMon.SECTS.indices.first { TongMon.SECTS[it].name == "Long Tượng Môn" }))
        // The perks follow the line.
        assertTrue(TongMon.join(TongMon.SECTS.indices.first { TongMon.SECTS[it].name == "Xích Viêm Cốc" }))
        assertTrue(TongMon.perk(TongMon.VAN_QUYEN))
        assertEquals(0, TongMon.family())
    }

    @Test
    fun savedAndRead() {
        awaken()
        TongMon.join(TongMon.KIEM_LU)
        TongMon.gain(77, "thử")
        BiCanh.check()
        BiCanh.nodes.add(BiCanh.LINH_THAO)
        BiCanh.wounds = 1
        val a = JSONObject(TongMon.toJson().toString())
        val b = JSONObject(BiCanh.toJson().toString())
        val rival = TongMon.rivalName
        TongMon.reset(); BiCanh.reset()
        TongMon.read(a); BiCanh.read(b)
        assertEquals(TongMon.KIEM_LU, TongMon.sect)
        assertEquals(77, TongMon.gongs)
        assertEquals(rival, TongMon.rivalName)
        assertEquals(listOf(BiCanh.LINH_THAO), BiCanh.nodes)
        assertEquals(1, BiCanh.wounds)
    }
}
