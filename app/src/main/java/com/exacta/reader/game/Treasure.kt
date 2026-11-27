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

    // --- Bộ pháp bảo: nine sets of four kinds, every kind in exactly one set (as on Exacta.OS) ---

    const val SETS = 9
    val SET_NAMES = arrayOf(
        "Vạn Kiếm Quy Tông", "Khai Thiên Phủ Đao", "Thương Kích Liệt Trận", "Phật Môn Kim Cang", "Đạo Môn Tứ Bảo",
        "Phong Thần Bảo Lục", "Tứ Hải Long Cung", "Giang Hồ Kỳ Binh", "Thượng Cổ Thần Khí",
    )
    val SET_MEMBERS = arrayOf(
        intArrayOf(0, 6, 24, 30), intArrayOf(1, 4, 12, 31), intArrayOf(2, 13, 14, 17),
        intArrayOf(5, 11, 20, 22), intArrayOf(8, 18, 19, 21), intArrayOf(23, 25, 26, 27),
        intArrayOf(7, 10, 28, 29), intArrayOf(3, 9, 15, 16), intArrayOf(32, 33, 34, 35),
    )
    /** The effects, adapted to what the app has (no kỳ ngộ, bế quan or linh thạch here). */
    val SET_EFFECTS = arrayOf(
        "Quà 10 vạn từ gieo hai lần",
        "Lò luyện nhanh 10%",
        "Bí cảnh chỉ cần 4 ngày",
        "Hộ pháp phù giữ chuỗi",
        "Thiên kiếp tha 1 ngày lỡ",
        "Bí cảnh thêm 1 khoáng thạch",
        "Sóc, Vọng, Tết: lò ×2",
        "Đọc xong sách thêm khoáng thạch",
        "Tu vi +5%",
    )
    val SET_DETAILS = arrayOf(
        "Khoáng thạch quà mỗi 10 vạn từ được gieo hai lần, lấy phẩm cao hơn.",
        "Mọi khoáng thạch trong lò cần ít hơn 10% số chữ để luyện thành.",
        "Bí cảnh tuần chỉ cần 4 ngày đọc từ 30 phút thay vì 5.",
        "Mỗi tuần (thứ Hai đến Chủ nhật) chuỗi ngày đọc được tha một ngày lỡ.",
        "Mỗi kỳ thiên kiếp được lỡ một ngày mà không thất bại.",
        "Qua bí cảnh tuần được thêm một khoáng thạch nữa.",
        "Ngày mồng một, ngày rằm và ba ngày Tết, chữ đọc luyện lò gấp đôi.",
        "Mỗi cuốn đọc xong cho thêm một khoáng thạch vào túi.",
        "Tu vi tính thêm 5% số chữ đã đọc.",
    )
    fun setOf(species: Int) = SET_MEMBERS.indexOfFirst { species in it }
    /** A seed whose kind is `species` (its grade is species / 6). */
    fun seedFor(species: Int) = (species % KINDS).toLong() * 18

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
