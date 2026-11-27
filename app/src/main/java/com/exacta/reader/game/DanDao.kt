package com.exacta.reader.game

/**
 * 2.2 Động Phủ Khai Lô: the linh thảo of the dược viên and the đan phương of the đan phòng.
 * Names are made for this game from the common words of the genre.
 */
object DanDao {
    // --- Linh thảo ---------------------------------------------------------------------------------

    val HERBS = arrayOf("Tụ Linh Thảo", "Bích Ngọc Lan", "Huyết Sâm", "Hỏa Linh Chi", "Băng Tâm Liên", "Kim Tủy Quả", "Tử Vân Đằng", "Cửu Diệp Linh Chi")
    val SEED_PRICES = longArrayOf(10, 15, 20, 25, 25, 30, 40, 80)
    /** Năm of dược linh a day of 30 minutes gives, before the linh mạch. */
    const val YEARS_A_DAY = 10
    const val MAX_YEARS = 9_999

    // --- Linh mạch ---------------------------------------------------------------------------------

    val GIAI = arrayOf("Nhất giai", "Nhị giai", "Tam giai", "Tứ giai", "Ngũ giai", "Lục giai", "Thất giai", "Bát giai", "Cửu giai")
    /** Linh thạch to raise the linh mạch from giai g (1-based) to g + 1. */
    val MACH_PRICES = longArrayOf(0, 200, 400, 700, 1_100, 1_600, 2_300, 3_200, 4_400)
    /** Luống open at each giai. */
    fun plots(giai: Int) = when {
        giai >= 8 -> 6
        giai >= 6 -> 5
        giai >= 4 -> 4
        giai >= 2 -> 3
        else -> 2
    }

    // --- Đan đạo -------------------------------------------------------------------------------------

    val CAP = arrayOf("Nhất phẩm", "Nhị phẩm", "Tam phẩm", "Tứ phẩm", "Ngũ phẩm", "Lục phẩm", "Thất phẩm", "Bát phẩm", "Cửu phẩm")
    /** Lò thành for each cấp of luyện đan sư (Nhất phẩm from the start). */
    val CAP_AT = intArrayOf(0, 3, 8, 15, 25, 40, 60, 85, 120)
    fun cap(made: Int) = CAP_AT.indexOfLast { made >= it } + 1

    val PHAM = arrayOf("hạ phẩm", "trung phẩm", "thượng phẩm", "cực phẩm")
    /** Đan độc left by a pill of each phẩm, as a percent of its recipe's. */
    val DOC_PCT = intArrayOf(100, 75, 50, 25)

    const val G_TU_LUYEN = 0
    const val G_PHA_CANH = 1
    const val G_CHUA_THUONG = 2
    const val G_TAM_CANH = 3
    const val G_LUYEN_THE = 4
    const val G_GIAI_DOC = 5
    const val G_HIEM = 6
    val GROUPS = arrayOf("Tu luyện", "Phá cảnh", "Chữa thương", "Tâm cảnh", "Luyện thể", "Giải độc", "Hiếm")

    /**
     * An đan phương: herbs (kind, năm at least), the minutes of hỏa hầu (one sitting), the cấp it
     * asks, the đan độc a hạ phẩm pill leaves, and what it does (`gate` for the phá cảnh ones).
     */
    class Recipe(
        val name: String, val group: Int, val herbs: List<Pair<Int, Int>>, val minutes: Int, val cap: Int,
        val doc: Int, val text: String, val gate: Int = 0, val value: Int = 0,
    )

    const val TU_KHI = 0
    const val NGUNG_NGUYEN = 1
    const val TRUC_CO = 2
    const val NGUNG_KIM = 3
    const val KET_ANH = 4
    const val HOA_THAN = 5
    const val PHA_HU = 6
    const val HOI_KHI = 7
    const val TUC_MACH = 8
    const val THANH_TAM = 9
    const val DINH_HON = 10
    const val DOAN_COT = 11
    const val KIM_THAN = 12
    const val GIAI_DOC = 13
    const val TAY_LINH = 14

    val RECIPES = listOf(
        Recipe("Tụ Khí Đan", G_TU_LUYEN, listOf(0 to 50), 20, 1, 6, "Linh lực +5.000 chữ", value = 5_000),
        Recipe("Ngưng Nguyên Đan", G_TU_LUYEN, listOf(0 to 100, 1 to 100), 30, 2, 8, "Linh lực +12.000 chữ", value = 12_000),
        Recipe("Trúc Cơ Đan", G_PHA_CANH, listOf(0 to 100, 2 to 100), 45, 1, 10, "Đột phá vào Trúc Cơ +20%", gate = 1, value = 20),
        Recipe("Ngưng Kim Đan", G_PHA_CANH, listOf(3 to 200, 1 to 200), 45, 3, 12, "Đột phá vào Kim Đan +15%", gate = 2, value = 15),
        Recipe("Kết Anh Đan", G_PHA_CANH, listOf(4 to 300, 5 to 300), 50, 4, 14, "Đột phá vào Nguyên Anh +15%", gate = 3, value = 15),
        Recipe("Hóa Thần Đan", G_PHA_CANH, listOf(6 to 500, 7 to 1_000), 60, 6, 16, "Đột phá vào Hóa Thần +12%", gate = 4, value = 12),
        Recipe("Phá Hư Đan", G_PHA_CANH, listOf(7 to 1_000, 5 to 800), 60, 8, 18, "Đột phá vào Luyện Hư +10%", gate = 5, value = 10),
        Recipe("Hồi Khí Đan", G_CHUA_THUONG, listOf(0 to 30), 15, 1, 5, "Hết hồi khí ngay"),
        Recipe("Tục Mạch Đan", G_CHUA_THUONG, listOf(2 to 150), 30, 2, 8, "Lành kinh mạch ngay"),
        Recipe("Thanh Tâm Đan", G_TAM_CANH, listOf(4 to 100), 30, 2, 6, "Tâm ma −15", value = 15),
        Recipe("Định Hồn Đan", G_TAM_CANH, listOf(4 to 300, 6 to 200), 45, 4, 10, "Xóa tẩu hỏa nhập ma, tâm ma −10", value = 10),
        Recipe("Đoán Cốt Đan", G_LUYEN_THE, listOf(2 to 200, 5 to 100), 40, 3, 10, "Thể phách +10", value = 10),
        Recipe("Kim Thân Đan", G_LUYEN_THE, listOf(5 to 500, 3 to 300), 60, 6, 14, "Thể phách +30", value = 30),
        Recipe("Giải Độc Đan", G_GIAI_DOC, listOf(1 to 50), 20, 1, 0, "Đan độc −25", value = 25),
        Recipe("Tẩy Linh Đan", G_HIEM, listOf(7 to 800, 6 to 500), 60, 7, 15, "Tẩy linh căn thêm một lần, không tốn linh thạch"),
    )

    /** The phá cảnh recipe of a gate, or -1. */
    fun breakRecipe(gate: Int) = RECIPES.indexOfFirst { it.gate == gate && it.group == G_PHA_CANH }
    fun recipePrice(i: Int) = 60L + 30L * RECIPES[i].cap
}
