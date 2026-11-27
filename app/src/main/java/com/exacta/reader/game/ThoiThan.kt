package com.exacta.reader.game

import java.time.LocalTime

/**
 * Time and progress told the old way, for the reader: the twelve canh giờ of two hours (Tý from 23
 * to 1, Sửu, Dần…), each of eight khắc of fifteen minutes; a share of the book in thành (tenths)
 * and phân (hundredths), counted in Hán-Việt numerals.
 */
object ThoiThan {
    val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
    private val SO = arrayOf("", "Nhất", "Nhị", "Tam", "Tứ", "Ngũ", "Lục", "Thất", "Bát", "Cửu")

    /** "Giờ Dần 3 khắc": the canh giờ and the khắc gone by in it; "Đầu giờ Dần" in its first khắc. */
    fun clock(t: LocalTime): String {
        val h = (t.hour + 1) % 24
        val chi = CHI[h / 2]
        val khac = ((h % 2) * 60 + t.minute) / 15
        return if (khac == 0) "Đầu giờ $chi" else "Giờ $chi $khac khắc"
    }

    /** A length of time: "~4 canh giờ 3 khắc", "~5 khắc", "chưa đầy một khắc". */
    fun span(minutes: Long): String {
        if (minutes < 1) return "vài hơi thở"
        if (minutes < 15) return "chưa đầy một khắc"
        val khac = (minutes + 7) / 15
        val canh = khac / 8
        val rest = khac % 8
        return when {
            canh == 0L -> "~$rest khắc"
            rest == 0L -> "~$canh canh giờ"
            else -> "~$canh canh giờ $rest khắc"
        }
    }

    /** The share read: "Mới mở quyển", "Thất phân", "Tam thành", "Tam thành thất", "Viên mãn". */
    fun share(percent: Int): String {
        val p = percent.coerceIn(0, 100)
        if (p == 0) return "Mới mở quyển"
        if (p == 100) return "Viên mãn"
        val tens = p / 10
        val ones = p % 10
        return when {
            tens == 0 -> "${SO[ones]} phân"
            ones == 0 -> "${SO[tens]} thành"
            else -> "${SO[tens]} thành ${SO[ones].lowercase()}"
        }
    }
}
