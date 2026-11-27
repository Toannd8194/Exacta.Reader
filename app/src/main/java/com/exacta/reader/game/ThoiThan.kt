package com.exacta.reader.game

import java.time.LocalTime

/**
 * Time told the old way, for the reader: the twelve canh giờ of two hours (Tý from 23
 * to 1, Sửu, Dần…), each of eight khắc of fifteen minutes.
 */
object ThoiThan {
    val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    /** "Giờ Dần 3 khắc": the canh giờ and the khắc gone by in it; "Đầu giờ Dần" in its first khắc. */
    fun clock(t: LocalTime): String {
        val h = (t.hour + 1) % 24
        val chi = CHI[h / 2]
        val khac = ((h % 2) * 60 + t.minute) / 15
        return if (khac == 0) "Đầu giờ $chi" else "Giờ $chi $khac khắc"
    }

    /** A length of time, kept short: "8 canh giờ", "3 khắc", "nửa khắc". */
    fun span(minutes: Long): String = when {
        minutes < 10 -> "nửa khắc"
        minutes < 120 -> "${((minutes + 7) / 15).coerceAtLeast(1)} khắc"
        else -> "${(minutes + 60) / 120} canh giờ"
    }
}
