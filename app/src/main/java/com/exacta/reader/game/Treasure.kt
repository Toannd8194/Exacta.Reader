package com.exacta.reader.game

/**
 * Pháp bảo: six grades, each its own clan of twelve kinds (72 in all); a treasure's kind is kept with
 * it, its branch (the first word of its name) comes from its seed. The drawable badge_NN is the kind
 * (grade * 12 + kind). Treasures from before 0.8 kept only a seed: their kind is (seed / 18) % 6.
 */
object Treasure {
    const val TIERS = 6
    const val KINDS = 12
    const val SPECIES = TIERS * KINDS
    val TIER_NAMES = arrayOf("Hoàng phẩm", "Huyền phẩm", "Địa phẩm", "Thiên phẩm", "Âm Dương phẩm", "Hỗn Độn phẩm")
    val CLANS = arrayOf("Phàm binh", "Linh khí", "Bảo khí", "Pháp bảo", "Tiên khí", "Thần khí")
    private val NAMES = arrayOf(
        "Đoản kiếm", "Đơn đao", "Trường thương", "Cung", "Rìu", "Côn",
        "Thiết thuẫn", "Câu liêm", "Thiết giản", "Phi trảo", "Nỏ", "Song tiết côn",
        "Song kiếm", "Thiết phiến", "Phất trần", "Phi tiêu", "Xích liên", "Nguyệt nha sạn",
        "Ngọc tiêu", "Linh đăng", "Phong linh", "Tỳ bà", "Ngọc bội", "Ngọc trâm",
        "Yển nguyệt đao", "Phương thiên kích", "Bát xà mâu", "Lưu tinh chùy", "Song câu", "Trảm mã đao",
        "Lang nha bổng", "Tam xoa kích", "Hổ trảo", "Thất tinh đao", "Long đầu quải", "Kim cương trạc",
        "Hồ lô", "Bát quái kính", "Kim cang chử", "Linh phù", "Tử kim bát", "Phiên kỳ",
        "Ngọc tịnh bình", "Liên hoa đài", "Khổn tiên thằng", "Phiên thiên ấn", "Lạc bảo kim tiền", "Tử kim linh",
        "Âm dương kiếm", "Càn khôn quyển", "Hỗn thiên lăng", "Phong hỏa luân", "Định hải châu", "Ngọc như ý",
        "Thái cực đồ", "Ngũ hỏa phiến", "Định phong châu", "Thất bảo diệu thụ", "Cửu long hỏa tráo", "Tử điện chùy",
        "Hiên Viên kiếm", "Bàn Cổ phủ", "Đông Hoàng chung", "Hạo Thiên tháp", "Phục Hy cầm", "Côn Lôn kính",
        "Luyện yêu hồ", "Thần Nông đỉnh", "Không Động ấn", "Nữ Oa thạch", "Hỗn Độn châu", "Tạo Hóa ngọc điệp",
    )
    private val BRANCHES = arrayOf(
        "Hỗn", "Nguyên", "Khí", "Sinh", "Hóa", "Dục", "Âm", "Dương", "Hư",
        "Thiên", "Địa", "Không", "U", "Minh", "Tử", "Long", "Phượng", "Kỳ",
    )
    private val STAGES = arrayOf("Khoáng thạch", "Luyện hỏa", "Đúc phôi", "Khắc trận văn", "Khai phong", "Xuất thế")

    /** Khí linh: the names a treasure's spirit may wake under (kỳ ngộ), and the mark of a forge trial. */
    val SPIRITS = arrayOf("Thông Linh", "Tụ Hồn", "Ẩn Long", "Lưu Quang", "Thiên Ý", "Huyền Âm", "Liệt Dương", "Vô Cực")
    const val TRIAL_MARK = "Kiếp Luyện"

    /** Gift ore: one per 10 vạn words read; grade odds per mille. */
    const val GIFT_WORDS = 100_000L
    private val GIFT_ODDS = intArrayOf(400, 280, 170, 90, 45, 15)
    /** Words read (any book) to forge an ore of each grade (the firmware's pages × 135). */
    val FORGE_WORDS = longArrayOf(5_400, 13_500, 27_000, 54_000, 135_000, 270_000)

    fun speciesOf(tier: Int, kind: Int) = tier.coerceIn(0, TIERS - 1) * KINDS + kind.coerceIn(0, KINDS - 1)
    /** The kind of a treasure from before 0.8 (only its seed was kept). */
    fun legacyKind(seed: Long) = ((seed / 18) % 6).toInt()
    fun speciesName(index: Int) = NAMES[index.coerceIn(0, SPECIES - 1)]
    private fun branch(seed: Long) = BRANCHES[(seed % 18).toInt()]

    /** "Long Phương thiên kích"; the branch is left out when the kind's name already holds it. */
    fun shortName(seed: Long, species: Int): String {
        val s = speciesName(species)
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

    // --- Bộ pháp bảo: eighteen sets of four kinds, every kind in exactly one set ---------------

    const val SETS = 18
    val SET_NAMES = arrayOf(
        "Vạn Kiếm Quy Tông", "Khai Thiên Phủ Đao", "Thương Kích Liệt Trận", "Phật Môn Kim Cang", "Đạo Môn Tứ Bảo",
        "Phong Thần Bảo Lục", "Tứ Hải Long Cung", "Giang Hồ Kỳ Binh", "Thượng Cổ Thần Khí",
        "Thiết Huyết Quân Trận", "Cầm Tiêu Hợp Tấu", "Linh Đăng Chiếu Lộ", "Võ Lâm Ám Khí", "Thất Tinh Bắc Đẩu",
        "Phật Quang Phổ Chiếu", "Thái Cực Lưỡng Nghi", "Thất Bảo Tiên Quang", "Tam Hoàng Thánh Khí",
    )
    private fun old(s: Int) = (s / 6) * KINDS + s % 6
    val SET_MEMBERS: Array<IntArray> = arrayOf(
        intArrayOf(0, 6, 24, 30), intArrayOf(1, 4, 12, 31), intArrayOf(2, 13, 14, 17),
        intArrayOf(5, 11, 20, 22), intArrayOf(8, 18, 19, 21), intArrayOf(23, 25, 26, 27),
        intArrayOf(7, 10, 28, 29), intArrayOf(3, 9, 15, 16), intArrayOf(32, 33, 34, 35),
    ).map { set -> set.map { old(it) }.toIntArray() }.toTypedArray() + arrayOf(
        intArrayOf(6, 10, 30, 31), intArrayOf(18, 20, 21, 47), intArrayOf(19, 22, 23, 42),
        intArrayOf(7, 9, 11, 32), intArrayOf(8, 33, 34, 35), intArrayOf(43, 44, 45, 46),
        intArrayOf(54, 55, 56, 59), intArrayOf(57, 58, 69, 70), intArrayOf(66, 67, 68, 71),
    )
    val SET_EFFECTS = arrayOf(
        "Quà 10 vạn từ gieo hai lần", "Lò luyện nhanh 10%", "Bí cảnh chỉ cần 4 ngày",
        "Hộ pháp phù giữ chuỗi", "Thiên kiếp tha 1 ngày lỡ", "Bí cảnh thêm 1 khoáng thạch",
        "Sóc, Vọng, Tết: lò ×2", "Đọc xong sách thêm khoáng thạch", "Tu vi +5%",
        "Kiếp phá lô bớt 1 ngày", "Kỳ ngộ +50%", "Tụ Linh Đan công hiệu ×2",
        "Thiên Hỏa Phù cháy gấp đôi lâu", "Thiên kiếp bớt 5 phút mỗi ngày", "Bí cảnh thưởng cao một phẩm",
        "Kiếp phá lô tha 1 ngày lỡ", "Kỳ ngộ có thể ra pháp bảo", "Tu vi thêm 5% nữa",
    )
    val SET_DETAILS = arrayOf(
        "Khoáng thạch quà mỗi 10 vạn từ được gieo hai lần, lấy phẩm cao hơn.",
        "Mọi khoáng thạch và chặng sách trong lò cần ít hơn 10% số chữ.",
        "Bí cảnh tuần chỉ cần 4 ngày đọc từ 30 phút thay vì 5.",
        "Mỗi tuần (thứ Hai đến Chủ nhật) chuỗi ngày đọc được tha một ngày lỡ.",
        "Mỗi kỳ thiên kiếp được lỡ một ngày mà không thất bại.",
        "Qua bí cảnh tuần được thêm một khoáng thạch nữa.",
        "Ngày mồng một, ngày rằm và ba ngày Tết, chữ đọc luyện lò gấp đôi.",
        "Mỗi cuốn đọc xong cho thêm một khoáng thạch vào túi.",
        "Tu vi tính thêm 5% số chữ đã đọc.",
        "Kiếp phá lô của pháp bảo cao phẩm cần ít đi một ngày.",
        "Cơ hội gặp kỳ ngộ khi đọc tăng thêm một nửa.",
        "Mỗi viên Tụ Linh Đan đẩy lò nhanh gấp đôi.",
        "Mỗi lá Thiên Hỏa Phù cháy được gấp đôi số chữ.",
        "Mỗi ngày độ kiếp chỉ cần đọc ít hơn 5 phút.",
        "Khoáng thạch bí cảnh tuần cao hơn một phẩm.",
        "Mỗi kỳ phá lô được lỡ một ngày mà không thất bại.",
        "Kỳ ngộ có thể nhặt được ngay một pháp bảo Hoàng hay Huyền phẩm.",
        "Tu vi tính thêm 5% số chữ đã đọc, cộng với Thượng Cổ Thần Khí.",
    )
    fun setOf(species: Int) = SET_MEMBERS.indexOfFirst { species in it }

    // --- Đan dược and phù, found by kỳ ngộ -------------------------------------------------------

    const val ITEMS = 4
    const val TU_LINH_DAN = 0     // +15% to a forge slot of Thiên phẩm or above
    const val THIEN_HOA_PHU = 1   // the forge burns twice as fast for 2 vạn words
    const val PHA_CANH_DAN = 2    // one day of a hard thiên kiếp (from Phá Đan Thành Anh on)
    const val HO_LO_PHU = 3       // a missed day of a kiếp phá lô forgiven (used by itself)
    val ITEM_NAMES = arrayOf("Tụ Linh Đan", "Thiên Hỏa Phù", "Phá Cảnh Đan", "Hộ Lô Phù")
    val ITEM_TEXTS = arrayOf(
        "Linh khí ngưng thành đan: rót vào một lò Thiên phẩm trở lên, đẩy thêm 15% tiến độ.",
        "Phù chú dẫn thiên hỏa: 2 vạn từ kế tiếp, lò luyện cháy gấp đôi.",
        "Đan phá bình cảnh: khi độ kiếp từ Phá Đan Thành Anh trở lên, tính thêm một ngày đã vượt.",
        "Phù hộ lô: khi kiếp phá lô lỡ một ngày, phù tự cháy thay, lò không vỡ.",
    )

    /** The nine gates of thiên kiếp, by the realm they open (1 Trúc Cơ … 9 Tiên nhân). */
    val GATES = arrayOf("", "Trúc Cơ", "Kết Đan", "Phá Đan Thành Anh", "Anh Biến Hóa Thần", "Luyện Thần Phản Hư",
        "Hợp Thể Quy Nguyên", "Đại Thừa Viên Mãn", "Độ Kiếp", "Phi Thăng")

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
