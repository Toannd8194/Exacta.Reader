package com.exacta.reader.game

/**
 * Pháp bảo, as in Exacta.OS: six grades (each its own clan of six kinds, 36 in all), the look and
 * name from a random seed. The drawable badge_NN is the kind (clan * 6 + kind).
 */
object Treasure {
    const val TIERS = 6
    const val KINDS = 6
    val TIER_NAMES = arrayOf("Hoàng phẩm", "Huyền phẩm", "Địa phẩm", "Thiên phẩm", "Âm Dương phẩm", "Hỗn Độn phẩm")
    val CLANS = arrayOf("Phàm binh", "Linh khí", "Bảo khí", "Pháp bảo", "Tiên khí", "Thần khí")
    private val SPECIES = arrayOf(
        "Đoản kiếm", "Đơn đao", "Trường thương", "Cung", "Rìu", "Côn",
        "Song kiếm", "Thiết phiến", "Phất trần", "Phi tiêu", "Xích liên", "Nguyệt nha sạn",
        "Yển nguyệt đao", "Phương thiên kích", "Bát xà mâu", "Lưu tinh chùy", "Song câu", "Trảm mã đao",
        "Hồ lô", "Bát quái kính", "Kim cang chử", "Linh phù", "Tử kim bát", "Phiên kỳ",
        "Âm dương kiếm", "Càn khôn quyển", "Hỗn thiên lăng", "Phong hỏa luân", "Định hải châu", "Ngọc như ý",
        "Hiên Viên kiếm", "Bàn Cổ phủ", "Đông Hoàng chung", "Hạo Thiên tháp", "Phục Hy cầm", "Côn Lôn kính",
    )
    private val BRANCHES = arrayOf(
        "Hỗn", "Nguyên", "Khí", "Sinh", "Hóa", "Dục", "Âm", "Dương", "Hư",
        "Thiên", "Địa", "Không", "U", "Minh", "Tử", "Long", "Phượng", "Kỳ",
    )
    private val STAGES = arrayOf("Khoáng thạch", "Luyện hỏa", "Đúc phôi", "Khắc trận văn", "Khai phong", "Xuất thế")

    /** Gift ore: one per 10 vạn words read; grade odds per mille. */
    const val GIFT_WORDS = 100_000L
    private val GIFT_ODDS = intArrayOf(400, 280, 170, 90, 45, 15)
    /** Words read (any book) to forge an ore of each grade (the firmware's pages × 135). */
    val FORGE_WORDS = longArrayOf(5_400, 13_500, 27_000, 54_000, 135_000, 270_000)

    fun species(seed: Long, tier: Int) = tier.coerceIn(0, TIERS - 1) * KINDS + ((seed / 18) % KINDS).toInt()
    fun speciesName(index: Int) = SPECIES[index.coerceIn(0, 35)]
    private fun branch(seed: Long) = BRANCHES[(seed % 18).toInt()]

    /** "Long Phương thiên kích"; the branch is left out when the kind's name already holds it. */
    fun shortName(seed: Long, tier: Int): String {
        val s = speciesName(species(seed, tier))
        val b = branch(seed)
        return if (s.contains(b)) s else "$b $s"
    }

    fun stageName(percent: Int) = STAGES[if (percent >= 100) 5 else (percent / 20).coerceIn(0, 4)]

    fun rollTier(random: kotlin.random.Random = kotlin.random.Random): Int {
        var roll = random.nextInt(1000)
        for (t in 0 until TIERS) {
            if (roll < GIFT_ODDS[t]) return t
            roll -= GIFT_ODDS[t]
        }
        return 0
    }

    /** A finished book forges a treasure whose grade follows its length. */
    fun tierForBook(words: Long) = when {
        words < 40_000 -> 0
        words < 108_000 -> 1
        words < 202_000 -> 2
        words < 405_000 -> 3
        words < 810_000 -> 4
        else -> 5
    }
}
