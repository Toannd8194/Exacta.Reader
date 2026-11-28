package com.exacta.reader

import com.exacta.reader.data.Book
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.Realm
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

/** The rules of 2.0: conversion, tiểu cảnh, the breakthrough and its failures, tâm ma, the move from 1.0. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TuLuyenTest {
    private val today get() = Store.today()
    private lateinit var book: Book

    /** A random whose first roll is `n`. */
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
        TuLuyen.reset()
        DaoPhap.reset()
        com.exacta.reader.data.DongPhu.reset()
        com.exacta.reader.data.TongMon.reset()
        com.exacta.reader.data.BiCanh.reset()
        com.exacta.reader.data.DaoLo.reset()
        com.exacta.reader.data.LinhGioi.reset()
        com.exacta.reader.data.DoKiep.reset()
        com.exacta.reader.data.TienDinh.reset()
        com.exacta.reader.data.TeLuyen.reset(); com.exacta.reader.data.TranPhap.reset()
        com.exacta.reader.data.TrangBi.reset(); com.exacta.reader.data.PhuLuc.reset()
        TuLuyen.started = true
        TuLuyen.lastTick = today - 1
        TuLuyen.rng = roll(0)
        // 400 words a page are 1/1024 of the book: the progressions add up exactly.
        book = Store.book("/t/book.epub").apply { words = 409_600; title = "Thử" }
    }

    /** Reads `words` by page turns of 400. */
    private fun read(words: Long) {
        var left = words
        while (left > 0) {
            val n = minOf(400L, left)
            Store.moveTo(book, book.maxProgress + n.toDouble() / book.words, pageTurn = true)
            left -= n
        }
    }

    /** `n` days up to today read `minutes` each. */
    private fun days(n: Int, minutes: Int) {
        for (d in 0 until n) Store.daySeconds[today - d] = minutes * 60L
    }

    @Test
    fun fullWordsBelowTheCeiling() {
        read(4_000)
        assertEquals(4_000, Store.tuVi())
        assertEquals("Luyện Khí sơ kỳ", TuLuyen.name())
    }

    @Test
    fun bìnhCảnhTurnsWordsIntoÁpChế() {
        Store.totalWords = 100_000   // Luyện Khí's ceiling
        read(10_000)
        assertEquals(102_000, Store.tuVi())
        assertEquals(8_000, TuLuyen.apChe)
        assertTrue(TuLuyen.atCeiling())
        assertEquals("Luyện Khí đại viên mãn", TuLuyen.name())
        // 8 000 of a span of 100 000: 8%, below the first 10%.
        assertEquals(0, TuLuyen.apCheBonus())
        read(10_000)
        assertEquals(5, TuLuyen.apCheBonus())
    }

    @Test
    fun aKnotHoldsTheTiểuCảnhWithoutCănCơ() {
        Store.realmCap = 1            // Trúc Cơ, 10..30 vạn; trung kỳ from 15 vạn needs căn cơ 30
        Store.totalWords = 150_000
        TuLuyen.advance()
        assertEquals(0, TuLuyen.subCap)
        assertTrue(TuLuyen.stuck())
        read(2_000)
        assertEquals(151_000, Store.tuVi())
        assertEquals("Trúc Cơ sơ kỳ", TuLuyen.name())
        days(15, 30)                  // căn cơ 30
        Store.checkRealm()
        assertEquals(1, TuLuyen.subCap)
        assertEquals("Trúc Cơ trung kỳ", TuLuyen.name())
        assertFalse(TuLuyen.stuck())
    }

    @Test
    fun trúcCơQuanBreaks() {
        Store.totalWords = 100_000
        assertFalse(TuLuyen.canBreak())  // căn cơ 0
        days(20, 30)                     // căn cơ 40
        assertTrue(TuLuyen.canBreak())
        assertFalse(TuLuyen.canStartTrial())
        // 40 + 20 + tâm cảnh (streak 20 + order 30 = 50: Thanh Minh, +10).
        assertEquals(70, TuLuyen.chance())
        assertEquals(true, TuLuyen.breakThrough())
        assertEquals(1, Store.realmCap)
        assertEquals(0, TuLuyen.coPham)    // căn cơ 40: hạ phẩm
        assertEquals(50, Store.linhThach)
        assertEquals(today, TuLuyen.capDays[1])
        assertEquals("Trúc Cơ sơ kỳ", TuLuyen.name())
    }

    @Test
    fun failuresHurtButKeepTheRealm() {
        Store.totalWords = 100_000
        days(20, 30)
        TuLuyen.rng = roll(99)
        assertEquals(false, TuLuyen.breakThrough())
        assertEquals(0, Store.realmCap)
        assertEquals(2_000, Store.tuViLoss)     // hụt khí: 2%
        assertEquals(10, TuLuyen.tamMa)
        assertTrue(TuLuyen.resting())
        assertNull(TuLuyen.breakThrough())       // hồi khí
        assertEquals(1, TuLuyen.fails)
        // Two days later, with the words back: 10% more.
        TuLuyen.restUntil = today
        read(2_500)
        assertTrue(TuLuyen.parts().any { it.first.startsWith("Kiên trì") && it.second == 10 })

        // Below 50%: kinh mạch tổn thương; tâm ma from 70: tẩu hỏa nhập ma.
        TuLuyen.tamMa = 90
        assertEquals(TuLuyen.TAU_HOA, TuLuyen.failKind())
        assertEquals(false, TuLuyen.breakThrough())
        assertTrue(TuLuyen.tamMaKiep)
        TuLuyen.restUntil = today
        assertTrue(TuLuyen.blockers().any { it.startsWith("Tâm ma kiếp") })
        TuLuyen.calm(60)
        assertFalse(TuLuyen.tamMaKiep)
    }

    @Test
    fun ngưngĐanCountsOneSitting() {
        Store.realmCap = 1
        TuLuyen.subCap = 3
        Store.totalWords = 300_000
        days(30, 30)
        assertFalse(TuLuyen.canStartTrial())       // 2.1: Kim Đan asks for a công pháp of Huyền phẩm
        DaoPhap.gainCong(6)
        assertFalse(TuLuyen.canStartTrial())       // 2.4: the đạo lộ first
        assertTrue(com.exacta.reader.data.DaoLo.choose(com.exacta.reader.data.DaoLo.PHAP))
        assertTrue(TuLuyen.canStartTrial())
        assertFalse(TuLuyen.canBreak())
        assertTrue(TuLuyen.startTrial())
        assertTrue(Store.inTrial())
        // 50 minutes in the day but no sitting of 45: not met.
        Store.daySeconds[today] = 50 * 60L
        assertFalse(Store.trialMet(today))
        TuLuyen.onSeconds(book, 60, 46 * 60L)
        assertTrue(Store.trialMet(today))
        // Three days met: passed, then the roll.
        Store.trialStart = today - 2
        TuLuyen.dayBest[today - 1] = 50 * 60L
        TuLuyen.dayBest[today - 2] = 50 * 60L
        Store.checkRealm()
        assertFalse(Store.inTrial())
        assertEquals(2, TuLuyen.trialDone)
        assertTrue(TuLuyen.canBreak())
        assertEquals(true, TuLuyen.breakThrough())
        assertEquals(2, Store.realmCap)
        assertTrue(TuLuyen.danPham in 0..9)
    }

    @Test
    fun tâmMaKiếpBreaksTheLightning() {
        Store.realmCap = 2
        TuLuyen.subCap = 3
        Store.totalWords = 600_000
        days(30, 40)
        DaoPhap.gainCong(13)                   // Địa phẩm for Nguyên Anh
        assertTrue(TuLuyen.startTrial())
        val other = Store.book("/t/other.epub").apply { words = 500_000 }
        TuLuyen.onSeconds(book, 60, 60)
        TuLuyen.onSeconds(other, 60, 60)       // a switch
        TuLuyen.onJump()                       // a jump
        assertEquals(2, TuLuyen.attacks)
        assertTrue(Store.inTrial())
        Store.endSession(120)                  // a sitting of two minutes
        assertFalse(Store.inTrial())
        assertEquals(15, TuLuyen.tamMa)
        assertEquals(-1, TuLuyen.trialDone)
    }

    @Test
    fun aJumpIsNotRead() {
        Store.moveTo(book, 0.01, pageTurn = false)   // 4 096 words skipped
        assertEquals(0, Store.tuVi())
        assertEquals(1, TuLuyen.dayJumps[today])
    }

    @Test
    fun aBrokenStreakStirsTâmMa() {
        Store.daySeconds[today - 3] = 3600
        Store.daySeconds[today - 2] = 0
        Store.daySeconds[today - 1] = 3600
        TuLuyen.lastTick = today - 4
        TuLuyen.tick()
        // −1 for the day of 60 minutes before, +10 for the day missed, −1 for yesterday.
        assertEquals(9, TuLuyen.tamMa)
        assertEquals(today - 1, TuLuyen.lastTick)
    }

    @Test
    fun theMoveFrom10KeepsEverything() {
        Store.realmCap = 2
        Store.totalWords = 700_000            // 10 vạn past the Kim Đan ceiling
        Store.trialStart = today - 1          // a lôi kiếp under way stays
        TuLuyen.migrate()
        assertEquals(700_000, Store.tuVi())
        assertEquals(100_000, TuLuyen.apChe)
        assertEquals(3, TuLuyen.subCap)
        assertTrue(Store.inTrial())
        assertEquals("Kim Đan đại viên mãn", TuLuyen.name())

        Store.realmCap = 0
        Store.totalWords = 120_000
        Store.trialStart = today - 1          // Trúc Cơ has no thiên kiếp any more
        TuLuyen.migrate()
        assertFalse(Store.inTrial())
    }

    @Test
    fun savedAndRead() {
        TuLuyen.tamMa = 33; TuLuyen.apChe = 1234; TuLuyen.coPham = 2; TuLuyen.capDays[1] = 99
        TuLuyen.dayBest[today] = 2900; TuLuyen.dayJumps[today] = 4
        val o = JSONObject(TuLuyen.toJson().toString())
        TuLuyen.reset()
        TuLuyen.read(o)
        assertEquals(33, TuLuyen.tamMa); assertEquals(1234, TuLuyen.apChe); assertEquals(2, TuLuyen.coPham)
        assertEquals(99, TuLuyen.capDays[1]); assertEquals(2900L, TuLuyen.dayBest[today]); assertEquals(4, TuLuyen.dayJumps[today])
    }

    @Test
    fun theFloorsStay() {
        assertEquals(100_000, TuLuyen.floor(1))
        assertEquals(1_600_000, TuLuyen.floor(5))
        assertEquals(Realm.floor(27), TuLuyen.floor(9))
    }
}
