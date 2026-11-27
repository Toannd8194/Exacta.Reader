package com.exacta.reader.game

import kotlin.math.floor
import kotlin.math.sin

/**
 * Vietnamese lunisolar calendar (UTC+7) and the 24 solar terms, from astronomical formulas
 * (Hồ Ngọc Đức's algorithm), no tables, no network. Ported from the Exacta.OS firmware.
 */
object Lunar {
    data class Date(val day: Int, val month: Int, val year: Int, val leap: Boolean)

    private const val TZ = 7.0

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Long {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) jd = dd + (153 * m + 2) / 5 + 365L * y + y / 4 - 32083
        return jd
    }

    private fun newMoonDay(k: Int): Long {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = Math.PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 = c1 - 0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 = c1 + 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 = c1 - 0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 = c1 - 0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 = c1 + 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        val deltat = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return floor(jd1 + c1 - deltat + 0.5 + TZ / 24).toLong()
    }

    private fun sunLongitude(jdn: Long): Double {
        val t = (jdn - 2451545.5 - TZ / 24) / 36525
        val t2 = t * t
        val dr = Math.PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl = dl + (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= Math.PI * 2 * floor(l / (Math.PI * 2))
        return l
    }

    private fun sunSector(jdn: Long) = floor(sunLongitude(jdn) / Math.PI * 6).toInt()

    private fun lunarMonth11(yy: Int): Long {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = floor(off / 29.530588853).toInt()
        var nm = newMoonDay(k)
        if (sunSector(nm) >= 9) nm = newMoonDay(k - 1)
        return nm
    }

    private fun leapMonthOffset(a11: Long): Int {
        val k = floor((a11 - 2415021.076998695) / 29.530588853 + 0.5).toInt()
        var last: Int
        var i = 1
        var arc = sunSector(newMoonDay(k + i))
        do {
            last = arc
            ++i
            arc = sunSector(newMoonDay(k + i))
        } while (arc != last && i < 14)
        return i - 1
    }

    fun fromSolar(dd: Int, mm: Int, yy: Int): Date {
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = newMoonDay(k + 1)
        if (monthStart > dayNumber) monthStart = newMoonDay(k)
        var a11 = lunarMonth11(yy)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = lunarMonth11(yy - 1)
        } else {
            lunarYear = yy + 1
            b11 = lunarMonth11(yy + 1)
        }
        val day = (dayNumber - monthStart + 1).toInt()
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var month = diff + 11
        var leap = false
        if (b11 - a11 > 365) {
            val leapDiff = leapMonthOffset(a11)
            if (diff >= leapDiff) {
                month = diff + 10
                if (diff == leapDiff) leap = true
            }
        }
        if (month > 12) month -= 12
        if (month >= 11 && diff < 4) lunarYear -= 1
        return Date(day, month, lunarYear, leap)
    }

    /** Solar term in effect: 0 = Xuân phân ... 23 = Kinh trập. */
    fun solarTerm(dd: Int, mm: Int, yy: Int): Int =
        floor(sunLongitude(jdFromDate(dd, mm, yy) + 1) / Math.PI * 12).toInt() % 24

    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
    private val TERMS = arrayOf(
        "Xuân phân", "Thanh minh", "Cốc vũ", "Lập hạ", "Tiểu mãn", "Mang chủng", "Hạ chí", "Tiểu thử",
        "Đại thử", "Lập thu", "Xử thử", "Bạch lộ", "Thu phân", "Hàn lộ", "Sương giáng", "Lập đông",
        "Tiểu tuyết", "Đại tuyết", "Đông chí", "Tiểu hàn", "Đại hàn", "Lập xuân", "Vũ thủy", "Kinh trập",
    )
    private val ANCIENT_MONTHS = arrayOf(
        "", "Mạnh Xuân", "Trọng Xuân", "Quý Xuân", "Mạnh Hạ", "Trọng Hạ", "Quý Hạ", "Mạnh Thu", "Trọng Thu",
        "Quý Thu", "Mạnh Đông", "Trọng Đông", "Quý Đông",
    )
    private val NUM = arrayOf("", "nhất", "nhị", "tam", "tứ", "ngũ", "lục", "thất", "bát", "cửu")

    fun termName(term: Int) = TERMS[((term % 24) + 24) % 24]
    fun yearName(lunarYear: Int) = CAN[((lunarYear + 6) % 10 + 10) % 10] + " " + CHI[((lunarYear + 8) % 12 + 12) % 12]

    /** "28/11 · mồng 8 tháng 10 âm lịch": the day plain, the lunar day under it. */
    fun plainDate(dd: Int, mm: Int, d: Date): String {
        val lday = if (d.day <= 10) "mồng ${d.day}" else "${d.day}"
        return "$dd/$mm · $lday tháng ${d.month}" + (if (d.leap) " nhuận" else "") + " âm lịch"
    }

    /** "Mạnh Đông · Vọng nhật": the month by season, the day the old way. */
    fun ancientMonthDay(d: Date): String {
        val day = when {
            d.day == 1 -> "Sóc nhật"
            d.day == 15 -> "Vọng nhật"
            d.day <= 9 -> "Sơ " + NUM[d.day]
            d.day == 10 -> "Sơ thập"
            d.day < 20 -> "Thập " + NUM[d.day - 10]
            d.day == 20 -> "Nhị thập"
            d.day < 30 -> "Nhị thập " + NUM[d.day - 20]
            else -> "Tam thập"
        }
        val month = (if (d.leap) "Nhuận " else "") + ANCIENT_MONTHS[d.month.coerceIn(1, 12)]
        return "$month · $day"
    }
}
