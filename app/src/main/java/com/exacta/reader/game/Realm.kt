package com.exacta.reader.game

/**
 * Cảnh giới from the words read, as in Exacta.OS: nine realms of three stages (sơ, trung, hậu kỳ),
 * then Tiên nhân in nine phẩm. Floors in vạn (10,000 words). Tầng công pháp of a book from its %.
 */
object Realm {
    const val LEVELS = 36
    const val TIEN_LEVEL = 27
    private val FLOOR_VAN = longArrayOf(0, 10, 30, 60, 100, 160, 250, 400, 600, 1000)
    private val TIEN_VAN = longArrayOf(1000, 1200, 1450, 1750, 2100, 2500, 3000, 3600, 4300)
    private val NAMES = arrayOf(
        "Luyện Khí", "Trúc Cơ", "Kim Đan", "Nguyên Anh", "Hóa Thần", "Luyện Hư", "Hợp Thể", "Đại Thừa",
        "Phi Thăng", "Tiên nhân",
    )
    private val STAGES = arrayOf("sơ kỳ", "trung kỳ", "hậu kỳ")
    private val PHAM = arrayOf("cửu phẩm", "bát phẩm", "thất phẩm", "lục phẩm", "ngũ phẩm", "tứ phẩm", "tam phẩm", "nhị phẩm", "nhất phẩm")
    private val TANGS = arrayOf(
        "Nhất tầng", "Nhất tầng", "Nhị tầng", "Tam tầng", "Tứ tầng", "Ngũ tầng", "Lục tầng", "Thất tầng", "Bát tầng",
        "Cửu tầng", "Đại viên mãn",
    )

    fun floor(level: Int): Long {
        val l = level.coerceIn(0, LEVELS - 1)
        if (l >= TIEN_LEVEL) return TIEN_VAN[l - TIEN_LEVEL] * 10000
        val r = l / 3
        val lo = FLOOR_VAN[r] * 10000
        val hi = FLOOR_VAN[r + 1] * 10000
        return lo + (hi - lo) * (l % 3) / 3
    }

    fun level(words: Long): Int {
        var l = 0
        while (l < LEVELS - 1 && words >= floor(l + 1)) l++
        return l
    }

    fun realmOf(level: Int) = (level / 3).coerceIn(0, 9)
    fun baseName(realm: Int) = NAMES[realm.coerceIn(0, 9)]
    fun name(level: Int): String {
        val l = level.coerceIn(0, LEVELS - 1)
        return if (l >= TIEN_LEVEL) NAMES[9] + " " + PHAM[l - TIEN_LEVEL] else NAMES[l / 3] + " " + STAGES[l % 3]
    }

    /** Progress through the current realm in ninths (0..9), for the nine-cell bar. */
    fun ninths(words: Long, level: Int): Double {
        if (level >= TIEN_LEVEL) {
            val lo = TIEN_VAN[0] * 10000.0
            val hi = TIEN_VAN[8] * 10000.0 + 700 * 10000.0
            return ((words - lo) / (hi - lo) * 9).coerceIn(0.0, 9.0)
        }
        val r = level / 3
        val lo = FLOOR_VAN[r] * 10000.0
        val hi = FLOOR_VAN[r + 1] * 10000.0
        return ((words - lo) / (hi - lo) * 9).coerceIn(0.0, 9.0)
    }

    fun stageLabels(level: Int) =
        if (level >= TIEN_LEVEL) listOf("hạ phẩm", "trung phẩm", "thượng phẩm") else listOf("sơ kỳ", "trung kỳ", "hậu kỳ")

    fun stageIndex(level: Int) = if (level >= TIEN_LEVEL) (level - TIEN_LEVEL) / 3 else level % 3

    fun tangOf(percent: Int) = if (percent >= 100) 10 else 1 + percent.coerceIn(0, 99) * 9 / 100
    fun tangName(tang: Int) = TANGS[tang.coerceIn(0, 10)]

    /** "4,3 vạn" style. */
    fun van(words: Long): String {
        val tenths = (words + 500) / 1000
        return "${tenths / 10},${tenths % 10}"
    }
}
