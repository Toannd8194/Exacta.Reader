package com.exacta.reader.game

/**
 * 2.1 Vạn Pháp Quy Nguyên: the five hành and the catalog of công pháp, tâm pháp and thuật pháp.
 * Names are made for this game from the common words of the genre.
 */
object PhapMon {
    // --- Hành and the other attributes --------------------------------------------------------------

    const val NONE = -1
    const val KIM = 0
    const val MOC = 1
    const val THUY = 2
    const val HOA = 3
    const val THO = 4
    const val LOI = 5
    const val BANG = 6
    const val PHONG = 7
    const val KIEM = 8
    const val THE = 9
    const val THAN = 10
    val ATTR = arrayOf("Kim", "Mộc", "Thủy", "Hỏa", "Thổ", "Lôi", "Băng", "Phong", "Kiếm", "Thể", "Thần")
    fun attrName(a: Int) = if (a in ATTR.indices) ATTR[a] else "vô thuộc tính"

    /** Kim khắc Mộc, Mộc khắc Thổ, Thủy khắc Hỏa, Hỏa khắc Kim, Thổ khắc Thủy. */
    private val KHAC = intArrayOf(MOC, THO, HOA, KIM, THUY)
    fun khac(a: Int, b: Int) = a in 0..4 && b in 0..4 && KHAC[a] == b

    /** The hành of an hour of the day: Kim 5–9, Mộc 9–13, Hỏa 13–18, Thổ 18–23, Thủy 23–5. */
    fun hanhOfHour(h: Int) = when (h) {
        in 5..8 -> KIM
        in 9..12 -> MOC
        in 13..17 -> HOA
        in 18..22 -> THO
        else -> THUY
    }

    /** Indexed by hành. */
    val HOURS = arrayOf("5 – 9 giờ", "9 – 13 giờ", "23 – 5 giờ", "13 – 18 giờ", "18 – 23 giờ")
    val TRAITS = arrayOf("kiếm khí, phá cấm chế", "sinh trưởng, dược viên", "thần thức, tâm cảnh", "luyện đan, luyện khí", "phòng ngự, căn cơ")

    /** Linh căn of 1..5 hành: name and linh lực. */
    val ROOT_NAMES = arrayOf("", "Thiên linh căn", "song linh căn", "tam linh căn", "tứ linh căn", "Ngũ hành tạp linh căn")
    val ROOT_BONUS = intArrayOf(0, 30, 15, 5, -10, -20)
    const val DI_BONUS = 10

    // --- Công pháp ---------------------------------------------------------------------------------

    val PHAM = arrayOf("Hoàng", "Huyền", "Địa", "Thiên", "Linh", "Tiên")
    /** The highest realm a công pháp of each phẩm carries the cultivator to. */
    val REACH = intArrayOf(1, 2, 3, 4, 7, 9)
    /** The phẩm a gate asks for. */
    fun phamFor(gate: Int) = REACH.indexOfFirst { it >= gate }.coerceAtLeast(0)
    /** Linh lực of a phẩm, before its tầng (+1% a tầng). */
    val PHAM_BONUS = intArrayOf(0, 4, 8, 12, 16, 20)
    /** Words of one tầng. */
    val TANG_WORDS = longArrayOf(15_000, 30_000, 50_000, 80_000, 200_000, 500_000)
    val PRICE = longArrayOf(40, 120, 300, 700, 1500, 3000)
    /** The tầng that need ngộ tính, and the highest tầng open before each. */
    val LOCKS = intArrayOf(4, 7, 9)
    fun ngoCost(pham: Int, lock: Int) = (LOCKS.indexOf(lock) + 1) + pham / 2

    // Đặc hiệu: what a công pháp does besides its linh lực. From 3.1 each công pháp has three: the
    // first from tầng 1, the second from tầng 5, the tuyệt học at tầng 9 (it counts twice). The same
    // kind twice adds up: a level. EFFECTS reads the first level; "{}" takes the level's value.
    const val E_NONE = 0
    const val E_HOI_KHI = 1
    const val E_KIEP_NGAY = 2
    const val E_TINH_TAM = 3
    const val E_CAN_CO = 4
    const val E_DOT_PHA = 5
    const val E_AP_CHE = 6
    const val E_LO_LUYEN = 7
    const val E_KY_NGO = 8
    const val E_LINH_THACH = 9
    const val E_THE_PHACH = 10
    const val E_THAN_THUC = 11
    const val E_BINH_CANH = 12
    const val E_NUT_THAT = 13
    const val E_NGO_TINH = 14
    const val E_LINH_LUC = 15
    const val E_TAM_CANH = 16
    const val E_DAN = 17
    const val E_TE_LUYEN = 18
    const val E_DO_KIEP = 19
    const val E_VAN_CONG = 20
    const val E_BI_CANH = 21
    private val EFFECT_TEXT = arrayOf(
        "Không có đặc hiệu",
        "Đột phá hỏng chỉ hồi khí 1 ngày",
        "Ngưng đan, lôi kiếp, thiên kiếp tính sẵn {} ngày",
        "Tâm ma tăng chậm {}%",
        "Căn cơ +{}",
        "Tỉ lệ đột phá +{}%",
        "Áp chế dày thêm {}%",
        "Lò luyện cháy nhanh {}%",
        "Kỳ ngộ nhiều thêm {}%",
        "Linh thạch từ chữ đọc +{}%",
        "Thể phách +{}%",
        "Thần thức +{}",
        "Bình cảnh: thêm {}% chữ thành linh lực",
        "Nút thắt: thêm {}% chữ thành linh lực",
        "Đọc trọn một cuốn: ngộ tính +{}",
        "Linh lực +{}%",
        "Tâm cảnh +{}",
        "Luyện đan +{}%",
        "Tế luyện cấm chế nhanh {}%",
        "Độ Kiếp +{}%",
        "Chữ vào tầng công pháp +{}%",
        "Bí cảnh tuần bớt 1 ngày",
    )
    /** The value of each kind at level 1. */
    private val EFFECT_STEP = intArrayOf(0, 0, 1, 30, 5, 5, 25, 20, 30, 20, 20, 5, 10, 20, 1, 3, 5, 5, 20, 5, 25, 0)
    /** Kinds that only switch on: a second level adds nothing. */
    fun isSwitch(e: Int) = e == E_HOI_KHI || e == E_BI_CANH || e == E_NONE
    fun effectValue(e: Int, level: Int) = EFFECT_STEP[e] * level
    fun effectText(e: Int, level: Int = 1) = EFFECT_TEXT[e].replace("{}", effectValue(e, level).toString())
    /** The text with the value at each level, as "30/60/90". */
    fun effectSteps(e: Int, vararg levels: Int) =
        if (isSwitch(e)) effectText(e) else EFFECT_TEXT[e].replace("{}", levels.joinToString("/") { effectValue(e, it).toString() })
    val EFFECTS get() = EFFECT_TEXT.indices.map { effectText(it) }

    // Khuyết điểm: from Địa phẩm, a công pháp running asks something back.
    const val F_NONE = 0
    const val F_TAM_MA = 1
    const val F_LINH_THACH = 2
    const val F_CAN_CO = 3
    const val F_NGO = 4
    const val F_HOI_KHI = 5
    const val F_THE_PHACH = 6
    const val F_KY_NGO = 7
    const val F_TAM_CANH = 8
    val FLAWS = arrayOf(
        "Không có khuyết điểm",
        "Tâm ma tăng nhanh 20%",
        "Linh thạch từ chữ đọc −20%",
        "Căn cơ −5",
        "Lĩnh ngộ tốn thêm 1 ngộ tính",
        "Đột phá hỏng hồi khí thêm 1 ngày",
        "Thể phách −15%",
        "Kỳ ngộ ít đi 20%",
        "Tâm cảnh −5",
    )

    // Mạch: twelve lines of inheritance, six phẩm each; the line is the công pháp's attribute (Hỗn
    // Nguyên for none). Each line is cultivated faster in its own way of reading.
    const val HON_NGUYEN = 11
    val MACH = arrayOf("Kim", "Mộc", "Thủy", "Hỏa", "Thổ", "Lôi", "Băng", "Phong", "Kiếm", "Thể", "Thần", "Hỗn Nguyên")
    val WAY = arrayOf(
        "đọc trong giờ Kim (5 – 9 giờ)",
        "đọc trong giờ Mộc (9 – 13 giờ)",
        "đọc trong giờ Thủy (23 – 5 giờ)",
        "đọc trong giờ Hỏa (13 – 18 giờ)",
        "đọc trong giờ Thổ (18 – 23 giờ)",
        "đọc sau nửa đêm (0 – 3 giờ) hoặc đêm rằm",
        "buổi đọc liền từ 20 phút trong ngày không nhảy chương",
        "đọc cuốn mở chưa quá 7 ngày",
        "đọc cuốn đã quá nửa, tới cùng",
        "buổi đọc liền từ 45 phút",
        "đọc sách của tác giả chưa đọc trọn cuốn nào",
        "7 ngày qua đã đọc ở bốn khung giờ hành trở lên",
    )
    /** Words read in the line's way count this much more into its tầng (not into tu vi). */
    const val WAY_PERCENT = 150

    class CongPhap(val name: String, val pham: Int, val attr: Int, val effect: Int, val e2: Int = E_NONE, val e3: Int = E_NONE, val flaw: Int = F_NONE) {
        val mach get() = if (attr < 0) HON_NGUYEN else attr
        /** The three đặc hiệu and the tầng each opens at. */
        val effects get() = listOf(effect to 1, e2 to 5, e3 to 9)
    }

    const val DAN_KHI = 0
    /** Ids 0 – 35 are the công pháp of 2.1, kept for old saves; 36 – 71 complete the twelve lines (3.1). */
    val CONG_PHAP = listOf(
        CongPhap("Dẫn Khí Quyết", 0, NONE, E_VAN_CONG, E_LINH_LUC, E_VAN_CONG),
        CongPhap("Canh Kim Quyết", 0, KIM, E_AP_CHE, E_LINH_THACH, E_DOT_PHA),
        CongPhap("Thanh Mộc Công", 0, MOC, E_CAN_CO, E_HOI_KHI, E_CAN_CO),
        CongPhap("Nhược Thủy Quyết", 0, THUY, E_TINH_TAM, E_THAN_THUC, E_TINH_TAM),
        CongPhap("Liệt Hỏa Công", 0, HOA, E_LO_LUYEN, E_KY_NGO, E_LO_LUYEN),
        CongPhap("Hậu Thổ Quyết", 0, THO, E_NUT_THAT, E_CAN_CO, E_NUT_THAT),
        // Huyền phẩm.
        CongPhap("Phá Quân Kim Quyết", 1, KIM, E_LINH_THACH, E_AP_CHE, E_LINH_THACH),
        CongPhap("Trường Xuân Công", 1, MOC, E_HOI_KHI, E_DAN, E_CAN_CO),
        CongPhap("Huyền Thủy Chân Kinh", 1, THUY, E_THAN_THUC, E_TINH_TAM, E_THAN_THUC),
        CongPhap("Xích Viêm Quyết", 1, HOA, E_KY_NGO, E_LO_LUYEN, E_KY_NGO),
        CongPhap("Trấn Nhạc Công", 1, THO, E_CAN_CO, E_BINH_CANH, E_CAN_CO),
        CongPhap("Duệ Kim Kiếm Kinh", 1, KIEM, E_DOT_PHA, E_HOI_KHI, E_DOT_PHA),
        CongPhap("Đồng Bì Thiết Cốt Công", 1, THE, E_THE_PHACH, E_CAN_CO, E_THE_PHACH),
        // Địa phẩm.
        CongPhap("Tinh Kim Bất Hoại Công", 2, KIM, E_HOI_KHI, E_AP_CHE, E_DOT_PHA, F_KY_NGO),
        CongPhap("Ất Mộc Sinh Sinh Quyết", 2, MOC, E_NGO_TINH, E_CAN_CO, E_DAN, F_THE_PHACH),
        CongPhap("Bích Hải Triều Sinh Quyết", 2, THUY, E_TINH_TAM, E_TAM_CANH, E_AP_CHE, F_LINH_THACH),
        CongPhap("Nam Minh Ly Hỏa Kinh", 2, HOA, E_KIEP_NGAY, E_LO_LUYEN, E_DAN, F_TAM_MA),
        CongPhap("Mậu Thổ Huyền Công", 2, THO, E_BINH_CANH, E_THE_PHACH, E_NUT_THAT, F_KY_NGO),
        CongPhap("Ngũ Lôi Chính Pháp", 2, LOI, E_DOT_PHA, E_KIEP_NGAY, E_LINH_LUC, F_TAM_MA),
        CongPhap("Minh Tâm Quán Tưởng Pháp", 2, THAN, E_THAN_THUC, E_NGO_TINH, E_TAM_CANH, F_THE_PHACH),
        // Thiên phẩm.
        CongPhap("Canh Kim Tru Tà Quyết", 3, KIM, E_DOT_PHA, E_TE_LUYEN, E_AP_CHE, F_TAM_CANH),
        CongPhap("Thanh Đế Trường Sinh Kinh", 3, MOC, E_CAN_CO, E_NGO_TINH, E_CAN_CO, F_LINH_THACH),
        CongPhap("Huyền Minh Chân Thủy Kinh", 3, THUY, E_AP_CHE, E_THAN_THUC, E_TINH_TAM, F_NGO),
        CongPhap("Đại Nhật Phần Thiên Quyết", 3, HOA, E_KIEP_NGAY, E_KY_NGO, E_LO_LUYEN, F_TAM_MA),
        CongPhap("Hoàng Đình Hậu Thổ Kinh", 3, THO, E_NUT_THAT, E_CAN_CO, E_BINH_CANH, F_NGO),
        CongPhap("Hàn Ngọc Băng Phách Quyết", 3, BANG, E_TINH_TAM, E_TAM_CANH, E_AP_CHE, F_THE_PHACH),
        CongPhap("Ngự Phong Lưu Vân Quyết", 3, PHONG, E_KY_NGO, E_BI_CANH, E_VAN_CONG, F_CAN_CO),
        CongPhap("Thái Bạch Kiếm Điển", 3, KIEM, E_HOI_KHI, E_DOT_PHA, E_TE_LUYEN, F_TAM_MA),
        // Linh phẩm.
        CongPhap("Vạn Kiếm Quy Tông Lục", 4, KIEM, E_DOT_PHA, E_TE_LUYEN, E_LINH_LUC, F_TAM_CANH),
        CongPhap("Cửu Thiên Lôi Đế Kinh", 4, LOI, E_KIEP_NGAY, E_DO_KIEP, E_DOT_PHA, F_TAM_MA),
        CongPhap("Thái Âm Luyện Hình Kinh", 4, THUY, E_TINH_TAM, E_TAM_CANH, E_THAN_THUC, F_THE_PHACH),
        CongPhap("Bất Diệt Kim Thân Quyết", 4, THE, E_THE_PHACH, E_DO_KIEP, E_NUT_THAT, F_NGO),
        CongPhap("Hỗn Nguyên Ngũ Hành Kinh", 4, NONE, E_BINH_CANH, E_LINH_LUC, E_VAN_CONG, F_HOI_KHI),
        // Tiên phẩm.
        CongPhap("Hồng Mông Tạo Hóa Kinh", 5, NONE, E_AP_CHE, E_LINH_LUC, E_BINH_CANH, F_HOI_KHI),
        CongPhap("Chu Thiên Tinh Thần Kinh", 5, THAN, E_THAN_THUC, E_VAN_CONG, E_NGO_TINH, F_THE_PHACH),
        CongPhap("Thái Thượng Vong Tình Lục", 5, THUY, E_TINH_TAM, E_TAM_CANH, E_THAN_THUC, F_KY_NGO),
        // 3.1: the twelve lines made whole.
        CongPhap("Canh Kim Phá Thiên Lục", 4, KIM, E_AP_CHE, E_TE_LUYEN, E_DOT_PHA, F_TAM_CANH),
        CongPhap("Thái Bạch Kim Tiên Kinh", 5, KIM, E_DOT_PHA, E_LINH_THACH, E_AP_CHE, F_TAM_MA),
        CongPhap("Kiến Mộc Thông Thiên Kinh", 4, MOC, E_CAN_CO, E_DAN, E_NGO_TINH, F_LINH_THACH),
        CongPhap("Vạn Mộc Hồi Xuân Tiên Quyết", 5, MOC, E_NGO_TINH, E_CAN_CO, E_DAN, F_THE_PHACH),
        CongPhap("Chu Tước Ly Hỏa Kinh", 4, HOA, E_LO_LUYEN, E_DAN, E_KIEP_NGAY, F_TAM_MA),
        CongPhap("Tam Muội Chân Hỏa Tiên Kinh", 5, HOA, E_KY_NGO, E_KIEP_NGAY, E_LO_LUYEN, F_TAM_MA),
        CongPhap("Huyền Hoàng Đại Địa Kinh", 4, THO, E_BINH_CANH, E_THE_PHACH, E_CAN_CO, F_KY_NGO),
        CongPhap("Tức Nhưỡng Tạo Hóa Tiên Kinh", 5, THO, E_NUT_THAT, E_CAN_CO, E_BINH_CANH, F_NGO),
        CongPhap("Dẫn Lôi Quyết", 0, LOI, E_DOT_PHA, E_LINH_LUC, E_DOT_PHA),
        CongPhap("Thanh Lôi Kinh", 1, LOI, E_KIEP_NGAY, E_DOT_PHA, E_KIEP_NGAY),
        CongPhap("Tử Tiêu Thần Lôi Quyết", 3, LOI, E_LINH_LUC, E_DO_KIEP, E_DOT_PHA, F_TAM_MA),
        CongPhap("Cửu Tiêu Thiên Lôi Tiên Kinh", 5, LOI, E_DO_KIEP, E_LINH_LUC, E_KIEP_NGAY, F_TAM_MA),
        CongPhap("Hàn Băng Quyết", 0, BANG, E_TINH_TAM, E_TAM_CANH, E_TINH_TAM),
        CongPhap("Sương Hoa Công", 1, BANG, E_TAM_CANH, E_AP_CHE, E_TAM_CANH),
        CongPhap("Huyền Băng Ngưng Phách Kinh", 2, BANG, E_AP_CHE, E_TINH_TAM, E_TAM_CANH, F_THE_PHACH),
        CongPhap("Vạn Tái Huyền Băng Kinh", 4, BANG, E_TAM_CANH, E_AP_CHE, E_TINH_TAM, F_KY_NGO),
        CongPhap("Bắc Minh Băng Hoàng Tiên Kinh", 5, BANG, E_TINH_TAM, E_TAM_CANH, E_AP_CHE, F_THE_PHACH),
        CongPhap("Khinh Phong Quyết", 0, PHONG, E_VAN_CONG, E_KY_NGO, E_VAN_CONG),
        CongPhap("Tật Phong Công", 1, PHONG, E_BI_CANH, E_VAN_CONG, E_KY_NGO),
        CongPhap("Thanh Phong Tiêu Dao Kinh", 2, PHONG, E_KY_NGO, E_LINH_THACH, E_VAN_CONG, F_CAN_CO),
        CongPhap("Cửu U Cuồng Phong Kinh", 4, PHONG, E_VAN_CONG, E_BI_CANH, E_KY_NGO, F_TAM_MA),
        CongPhap("Côn Bằng Ngự Phong Tiên Kinh", 5, PHONG, E_KY_NGO, E_VAN_CONG, E_LINH_THACH, F_CAN_CO),
        CongPhap("Tam Xích Kiếm Quyết", 0, KIEM, E_DOT_PHA, E_TE_LUYEN, E_DOT_PHA),
        CongPhap("Thanh Liên Kiếm Kinh", 2, KIEM, E_TE_LUYEN, E_DOT_PHA, E_LINH_LUC, F_TAM_CANH),
        CongPhap("Thiên Đạo Kiếm Tiên Lục", 5, KIEM, E_LINH_LUC, E_DOT_PHA, E_TE_LUYEN, F_TAM_MA),
        CongPhap("Luyện Bì Quyết", 0, THE, E_THE_PHACH, E_CAN_CO, E_THE_PHACH),
        CongPhap("Kim Cương Luyện Cốt Kinh", 2, THE, E_CAN_CO, E_THE_PHACH, E_NUT_THAT, F_NGO),
        CongPhap("Bàn Thạch Chiến Thể Kinh", 3, THE, E_NUT_THAT, E_DO_KIEP, E_THE_PHACH, F_KY_NGO),
        CongPhap("Hồng Hoang Bất Diệt Tiên Thể", 5, THE, E_DO_KIEP, E_THE_PHACH, E_CAN_CO, F_NGO),
        CongPhap("Ngưng Thần Quyết", 0, THAN, E_THAN_THUC, E_NGO_TINH, E_THAN_THUC),
        CongPhap("Tĩnh Tâm Quán Tưởng Công", 1, THAN, E_NGO_TINH, E_TAM_CANH, E_THAN_THUC),
        CongPhap("Thái Hư Thần Hồn Kinh", 3, THAN, E_TAM_CANH, E_THAN_THUC, E_VAN_CONG, F_THE_PHACH),
        CongPhap("Vạn Tượng Thần Thức Kinh", 4, THAN, E_VAN_CONG, E_NGO_TINH, E_THAN_THUC, F_THE_PHACH),
        CongPhap("Ngũ Khí Triều Nguyên Công", 1, NONE, E_LINH_LUC, E_BINH_CANH, E_LINH_LUC),
        CongPhap("Ngũ Hành Luân Chuyển Quyết", 2, NONE, E_VAN_CONG, E_LINH_LUC, E_BINH_CANH, F_HOI_KHI),
        CongPhap("Thái Cực Lưỡng Nghi Kinh", 3, NONE, E_BINH_CANH, E_KY_NGO, E_LINH_LUC, F_HOI_KHI),
    )

    /** The công pháp of a line, by phẩm (null when none). */
    fun ofLine(mach: Int, pham: Int): Int? = CONG_PHAP.indices.firstOrNull { CONG_PHAP[it].mach == mach && CONG_PHAP[it].pham == pham }

    /** Xung quan: the minutes of the sitting that breaks through tầng 4, 7, 9. */
    fun xungMinutes(lock: Int) = when (lock) {
        4 -> 30
        7 -> 60
        else -> 90
    }
    /** Chuyển tu: a line's next phẩm keeps this share of the words of the one completed. */
    const val INHERIT_PERCENT = 50
    /** Phụ tu: its words grow at this share, its first đặc hiệu counts. */
    const val PHU_PERCENT = 50

    // --- Tâm pháp (3.2 Tâm Đăng Bất Diệt) ----------------------------------------------------------

    const val TP_THANH_TAM = 0
    const val TP_TINH_TOA = 1
    const val TP_BANG_TAM = 2
    const val TP_VONG_NGA = 3
    const val TP_CHUNG_MA = 4
    const val TP_DUONG_KHI = 5
    const val TP_DINH_HON = 6
    const val TP_VO_NIEM = 7
    const val TP_KIM_CUONG = 8
    const val TP_TU_BI = 9
    const val TP_BAT_NHA = 10
    const val TP_HUYET_TAM = 11
    const val TP_GIAI_THE = 12
    const val TP_PHE_TAM = 13
    const val TP_HAO_NHIEN = 14
    const val TP_THU_HUONG = 15
    const val TP_TAM_TINH = 16
    const val TP_VAN_TAM = 17

    /** Four schools. Ma đạo is strong and costs; it will not run beside Phật môn or Nho môn. */
    const val PHAI_DAO = 0
    const val PHAI_PHAT = 1
    const val PHAI_MA = 2
    const val PHAI_NHO = 3
    val PHAI = arrayOf("Đạo gia", "Phật môn", "Ma đạo", "Nho môn")
    val PHAI_TEXT = arrayOf(
        "Lặng lòng, đọc liền, dưỡng căn cơ",
        "Một lòng một cuốn, vững qua kiếp nạn",
        "Mạnh và nhanh, nhưng tâm ma tăng nhanh 15% mỗi bộ đang vận",
        "Sống bằng sách: đọc trọn, đọc lại, tự xét mình",
    )
    /** Ma đạo running: a rise of tâm ma comes this much stronger, each. */
    const val MA_STIR = 15
    /** Đạo gia beside Phật môn: both turn this much more. */
    const val DAO_PHAT_PERCENT = 20

    /** The ways a tâm pháp turns: a sitting that keeps to its way counts twice. */
    const val W_NO_JUMP = 0
    const val W_LONG45 = 1
    const val W_LONG60 = 2
    const val W_LONG90 = 3
    const val W_STREAK = 4
    const val W_DAWN = 5
    const val W_NIGHT = 6
    const val W_ONE_BOOK = 7
    const val W_THIRD = 8
    const val W_NEW_AUTHOR = 9
    const val W_REREAD = 10
    const val W_FINISH = 11
    const val W_TAM_MA = 12
    const val W_RESTLESS = 13
    const val W_DEEP = 14
    val TAM_WAY = arrayOf(
        "buổi 30 phút mà trong ngày chưa nhảy chương",
        "buổi đọc liền tới 45 phút",
        "buổi đọc liền tới 60 phút",
        "buổi đọc liền tới 90 phút",
        "đọc khi chuỗi ngày đọc từ 7 ngày",
        "đọc sớm, 4 – 8 giờ sáng",
        "đọc đêm, 21 – 3 giờ",
        "cả ngày chỉ đọc một cuốn",
        "buổi 30 phút thứ ba trong ngày",
        "đọc tác giả chưa đọc trọn cuốn nào",
        "đọc lại cuốn đã đọc trọn, hay cuốn mới quay lại",
        "đọc trọn một cuốn: tính 5 lần vận chuyển",
        "đọc lúc tâm ma từ 30",
        "đọc trong ngày đã nhảy chương",
        "đọc cuốn đã quá nửa",
    )
    /** Ways that are judged when the sitting reaches this many minutes (the rest at 30). */
    fun wayMinute(w: Int) = when (w) { W_LONG45 -> 45; W_LONG60 -> 60; W_LONG90 -> 90; W_FINISH -> -1; else -> 30 }
    const val FINISH_TURNS = 5

    /**
     * A tâm pháp: its school, its way, the trait it always had (the seven of 2.1), the effect it opens
     * at nhập môn and grows with each cảnh, and the one Đại thành opens. Effects are the đặc hiệu of
     * công pháp and add up with them.
     */
    class TamPhap(val name: String, val phai: Int, val way: Int, val e1: Int, val e2: Int, val trait: String = "") {
        /** The old line of text: the trait, or the first effect. */
        val text get() = trait.ifEmpty { effectText(e1) }
    }

    /** Ids 0 – 6 are the tâm pháp of 2.1, kept for old saves. */
    val TAM_PHAP = listOf(
        TamPhap("Thanh Tâm Quyết", PHAI_DAO, W_NO_JUMP, E_TAM_CANH, E_CAN_CO, "Tâm ma tăng chậm một nửa"),
        TamPhap("Tĩnh Tọa Kinh", PHAI_DAO, W_LONG45, E_THE_PHACH, E_LINH_THACH, "Bế quan viên mãn được thêm 30% linh thạch"),
        TamPhap("Băng Tâm Quyết", PHAI_DAO, W_STREAK, E_TINH_TAM, E_HOI_KHI, "Đứt chuỗi lần đầu mỗi tháng không sinh tâm ma"),
        TamPhap("Vong Ngã Chân Quyết", PHAI_DAO, W_LONG90, E_TAM_CANH, E_BINH_CANH, "Buổi đọc từ 90 phút tính gấp đôi cho thể phách và tâm cảnh"),
        TamPhap("Đạo Tâm Chủng Ma", PHAI_MA, W_TAM_MA, E_DOT_PHA, E_AP_CHE, "Tâm ma cao lại cộng linh lực (tâm ma / 5, tối đa 20%) khi vận công pháp Kiếm"),
        TamPhap("Dưỡng Khí Thiên", PHAI_DAO, W_DAWN, E_CAN_CO, E_LINH_LUC, "Căn cơ +5"),
        TamPhap("Định Hồn Chân Ngôn", PHAI_PHAT, W_ONE_BOOK, E_TINH_TAM, E_DO_KIEP, "Đột phá hỏng chỉ tẩu hỏa nhập ma khi tâm ma từ 85"),
        TamPhap("Vô Niệm Kinh", PHAI_PHAT, W_LONG60, E_THAN_THUC, E_NUT_THAT),
        TamPhap("Kim Cương Bất Động Tâm", PHAI_PHAT, W_NO_JUMP, E_DO_KIEP, E_KIEP_NGAY),
        TamPhap("Từ Bi Quán", PHAI_PHAT, W_NEW_AUTHOR, E_CAN_CO, E_DAN),
        TamPhap("Bát Nhã Tâm Đăng", PHAI_PHAT, W_DAWN, E_THAN_THUC, E_NGO_TINH),
        TamPhap("Huyết Tâm Quyết", PHAI_MA, W_NIGHT, E_LINH_LUC, E_DOT_PHA),
        TamPhap("Thiên Ma Giải Thể", PHAI_MA, W_LONG90, E_DOT_PHA, E_DO_KIEP),
        TamPhap("Vạn Niệm Phệ Tâm", PHAI_MA, W_RESTLESS, E_KY_NGO, E_LINH_LUC),
        TamPhap("Hạo Nhiên Chính Khí", PHAI_NHO, W_FINISH, E_TAM_CANH, E_NGO_TINH),
        TamPhap("Thư Hương Dưỡng Tâm", PHAI_NHO, W_REREAD, E_VAN_CONG, E_KY_NGO),
        TamPhap("Tam Tỉnh Ngô Thân", PHAI_NHO, W_THIRD, E_TINH_TAM, E_TAM_CANH),
        TamPhap("Văn Tâm Quyết", PHAI_NHO, W_DEEP, E_VAN_CONG, E_LINH_THACH),
    )
    const val TAM_PRICE = 150L

    /** Năm cảnh, at these vận chuyển. */
    val CANH = arrayOf("Chưa nhập môn", "Nhập môn", "Tiểu thành", "Đại thành", "Viên mãn", "Hóa cảnh")
    val CANH_AT = intArrayOf(0, 5, 15, 30, 60, 100)
    fun canh(turns: Int) = CANH_AT.indexOfLast { turns >= it }
    /** The level each cảnh gives the first effect, and the second. */
    val E1_LV = intArrayOf(0, 1, 2, 2, 3, 3)
    val E2_LV = intArrayOf(0, 0, 0, 1, 1, 2)
    const val TAM_NHAP_MON = 5
    const val TAM_DAI_THANH = 30
    const val TAM_HOA_CANH = 100

    /** Ma đạo will not run beside Phật môn or Nho môn. */
    fun clash(a: Int, b: Int): Boolean {
        val pa = TAM_PHAP[a].phai
        val pb = TAM_PHAP[b].phai
        return (pa == PHAI_MA && (pb == PHAI_PHAT || pb == PHAI_NHO)) || (pb == PHAI_MA && (pa == PHAI_PHAT || pa == PHAI_NHO))
    }

    // --- Thuật pháp (3.3 Thần Thông Quảng Đại) ----------------------------------------------------

    const val K_CONG_KICH = 0
    const val K_PHONG_NGU = 1
    const val K_DON = 2
    const val K_PHU_TRO = 3
    const val K_THAN_THONG = 4
    const val K_KHONG_CHE = 5
    val KINDS = arrayOf("Công kích", "Phòng ngự", "Độn thuật", "Phụ trợ", "Thần thông", "Khống chế")
    /** What each kind does when it is cast, in a line. */
    val KIND_TEXT = arrayOf(
        "Bí cảnh: diệt yêu thú, đem về linh thạch; quyết chiến chủ bí cảnh dễ hơn",
        "Kiếp đang độ: lỡ một ngày vẫn không sao (Viên Mãn hai ngày); bí cảnh: che chở khi gặp nguy",
        "Đột phá hỏng: bớt hậu quả; bí cảnh: thoát thân, lấy bảo rồi rút",
        "Tự ra tay khi biến cố tới",
        "Thần thông: mạnh, hồi chiêu lâu; từ Nguyên Anh",
        "Bí cảnh: trói yêu thú lấy thưởng chắc chắn; trấn áp chủ bí cảnh rồi quyết chiến",
    )
    /** Hồi chiêu, in days of reading, by kind (a thuật may set its own). */
    val KIND_CD = intArrayOf(1, 3, 3, 2, 6, 2)

    const val T_HOA_DAN = 0
    const val T_NGU_KIEM = 1
    const val T_CHUONG_LOI = 2
    const val T_HAN_BANG = 3
    const val T_KIM_CANG = 4
    const val T_THUY_MAC = 5
    const val T_MOC_GIAP = 6
    const val T_THO_DON = 7
    const val T_PHONG_DON = 8
    const val T_HUYET_DON = 9
    const val T_THIEN_NHAN = 10
    const val T_LIEM_TUC = 11
    const val T_PHAP_TUONG = 12
    const val T_THAN_KIEM = 13
    const val T_THUAN_DI = 14
    const val T_THANH_MOC_CHAM = 15
    const val T_HUYEN_THUY_TIEN = 16
    const val T_PHI_SA = 17
    const val T_LIET_PHONG = 18
    const val T_KIM_QUANG = 19
    const val T_KHON_LONG = 20
    const val T_DANG_LA = 21
    const val T_BANG_PHONG = 22
    const val T_DINH_THAN = 23
    const val T_LOI_VONG = 24
    const val T_ME_HON = 25
    const val T_HOA_THUAN = 26
    const val T_THO_TUONG = 27
    const val T_BANG_GIAP = 28
    const val T_LOI_TRAO = 29
    const val T_THUY_DON = 30
    const val T_MOC_DON = 31
    const val T_THANH_TAM_CHU = 32
    const val T_HOI_XUAN = 33
    const val T_LOI_PHAT = 34
    const val T_BANG_VAN_LY = 35

    /** A thuật: its hành, its kind, what it does, its hồi chiêu (0: always at work, never cast). */
    class ThuatPhap(val name: String, val attr: Int, val kind: Int, val text: String, cd: Int = -1) {
        val cd = if (cd >= 0) cd else KIND_CD[kind]
    }

    /** Ids 0 – 14 are the thuật pháp of 2.1, kept for old saves; 15 – 35 are new in 3.3. */
    val THUAT_PHAP = listOf(
        ThuatPhap("Hỏa Đạn Thuật", HOA, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Ngự Kiếm Thuật", KIM, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Chưởng Tâm Lôi", LOI, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Hàn Băng Tiễn", BANG, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Kim Cang Tráo", KIM, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Thủy Mạc Thuật", THUY, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Mộc Giáp Thuật", MOC, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Thổ Độn", THO, K_DON, "Đột phá hỏng: hụt khí chỉ mất 1%, kinh mạch chỉ 3 ngày"),
        ThuatPhap("Phong Độn", PHONG, K_DON, "Đột phá hỏng: hồi khí chỉ 1 ngày"),
        ThuatPhap("Huyết Độn", HOA, K_DON, "Đột phá hỏng: tẩu hỏa nhập ma thành kinh mạch tổn thương"),
        ThuatPhap("Thiên Nhãn Thuật", THUY, K_PHU_TRO, "Tâm ma kiếp: chặn một lần tâm ma tấn công; bí cảnh: nhìn thấu trận văn"),
        ThuatPhap("Liễm Tức Thuật", MOC, K_PHU_TRO, "Nhảy chương quá 3 lần một ngày không sinh tâm ma", 0),
        ThuatPhap("Pháp Tướng Thiên Địa", THO, K_THAN_THONG, "Tỉ lệ đột phá +5%, thêm 1% mỗi bậc thuần thục", 0),
        ThuatPhap("Thần Thức Hóa Kiếm", KIM, K_THAN_THONG, "Tâm ma kiếp: chặn một lần tâm ma tấn công; đột phá hỏng tâm ma chỉ +5"),
        ThuatPhap("Thuấn Di", PHONG, K_THAN_THONG, "Đột phá hỏng không phải hồi khí"),
        ThuatPhap("Thanh Mộc Châm", MOC, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Huyền Thủy Tiễn", THUY, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Phi Sa Tẩu Thạch", THO, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Liệt Phong Nhận", PHONG, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch"),
        ThuatPhap("Kim Quang Kiếm Khí", KIM, K_CONG_KICH, "Bí cảnh: diệt yêu thú, đem về linh thạch", 2),
        ThuatPhap("Khốn Long Tác", KIM, K_KHONG_CHE, "Bí cảnh: trói yêu thú, trấn áp chủ bí cảnh"),
        ThuatPhap("Đằng La Triền", MOC, K_KHONG_CHE, "Bí cảnh: trói yêu thú, trấn áp chủ bí cảnh"),
        ThuatPhap("Băng Phong Thuật", BANG, K_KHONG_CHE, "Bí cảnh: trói yêu thú, trấn áp chủ bí cảnh"),
        ThuatPhap("Định Thân Thuật", THO, K_KHONG_CHE, "Bí cảnh: trói yêu thú, trấn áp chủ bí cảnh"),
        ThuatPhap("Lôi Võng Thuật", LOI, K_KHONG_CHE, "Bí cảnh: trói yêu thú, trấn áp chủ bí cảnh"),
        ThuatPhap("Mê Hồn Chú", THUY, K_KHONG_CHE, "Bí cảnh: trói yêu thú, trấn áp chủ bí cảnh", 3),
        ThuatPhap("Hỏa Thuẫn Thuật", HOA, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Thổ Tường Thuật", THO, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Băng Giáp Thuật", BANG, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Lôi Quang Tráo", LOI, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Thủy Độn", THUY, K_DON, "Đột phá hỏng: hụt khí chỉ mất 1%"),
        ThuatPhap("Mộc Độn", MOC, K_DON, "Đột phá hỏng: kinh mạch chỉ tổn thương 2 ngày"),
        ThuatPhap("Thanh Tâm Chú", THUY, K_PHU_TRO, "Tâm ma vượt 60: tự niệm, tâm ma −15", 3),
        ThuatPhap("Hồi Xuân Thuật", MOC, K_PHU_TRO, "Bí cảnh: bị thương thì chữa lành ngay một vết", 3),
        ThuatPhap("Lôi Phạt Thiên Uy", LOI, K_THAN_THONG, "Bí cảnh: quyết chiến chủ bí cảnh chắc thắng", 7),
        ThuatPhap("Băng Phong Vạn Lý", BANG, K_THAN_THONG, "Đột phá hỏng: tâm ma không tăng", 5),
    )
    /** The first thuật pháp a hành learns. */
    val FIRST_THUAT = intArrayOf(T_NGU_KIEM, T_MOC_GIAP, T_THUY_MAC, T_HOA_DAN, T_THO_DON)
    /** A công pháp of Hoàng phẩm for each hành. */
    val FIRST_CONG = intArrayOf(1, 2, 3, 4, 5)
    fun thuatPrice(i: Int) = when (THUAT_PHAP[i].kind) { K_THAN_THONG -> 400L; K_KHONG_CHE -> 120L; else -> 80L }

    /** Thuần thục: three points a use, one for each sitting of 30 minutes read while the thuật is carried. */
    val MASTERY = arrayOf("Sơ Học", "Tiểu Thành", "Đại Thành", "Viên Mãn", "Hóa Cảnh")
    val MASTERY_AT = intArrayOf(0, 9, 24, 45, 75)
    const val USE_POINTS = 3
    fun mastery(points: Int) = MASTERY_AT.indexOfLast { points >= it }
    /** What a bậc gives: a công kích or khống chế lands this often (%), and the hồi chiêu shortens. */
    fun hitChance(m: Int) = 60 + 10 * m
    fun cdLess(m: Int) = m / 2

    /** Hợp kích: two thuật of one hành carried together open a third effect. */
    class HopKich(val attr: Int, val name: String, val text: String)
    val HOP_KICH = listOf(
        HopKich(KIM, "Kim Qua Thiết Mã", "thuật công kích chắc trúng"),
        HopKich(MOC, "Vạn Mộc Hồi Xuân", "kiếp đang độ: lỡ thêm một ngày vẫn không sao"),
        HopKich(THUY, "Thủy Kính Lưu Quang", "tâm ma kiếp: chặn thêm một lần"),
        HopKich(HOA, "Hỏa Vực", "thuật công kích đem về gấp đôi linh thạch"),
        HopKich(THO, "Hậu Thổ Hộ Thân", "đột phá hỏng không hụt linh lực"),
        HopKich(LOI, "Lôi Đình Vạn Quân", "quyết chiến chủ bí cảnh +15%"),
        HopKich(BANG, "Băng Phong Thiên Lý", "thuật khống chế được thêm một phần thưởng"),
        HopKich(PHONG, "Phong Hành Vạn Lý", "mọi thuật hồi chiêu nhanh một ngày"),
    )

    /** Bản mệnh thần thông: each of the twelve Kim Đan opens its own, an effect of the công pháp kinds. */
    class BanMenh(val name: String, val effect: Int, val level: Int)
    val BAN_MENH = listOf(
        BanMenh("Canh Kim Kiếm Khí", E_DOT_PHA, 2),
        BanMenh("Ất Mộc Trường Sinh", E_CAN_CO, 2),
        BanMenh("Nhâm Thủy Tịnh Tâm", E_TINH_TAM, 1),
        BanMenh("Bính Hỏa Phần Thiên", E_LO_LUYEN, 2),
        BanMenh("Mậu Thổ Trấn Nhạc", E_KIEP_NGAY, 1),
        BanMenh("Tử Lôi Thiên Phạt", E_DO_KIEP, 2),
        BanMenh("Huyền Băng Phong Ấn", E_TAM_CANH, 2),
        BanMenh("Thanh Phong Ngự Không", E_BI_CANH, 1),
        BanMenh("Đại Nhật Chiếu Thế", E_LINH_THACH, 2),
        BanMenh("Thái Âm Hoán Nguyệt", E_THAN_THUC, 2),
        BanMenh("Khô Mộc Phùng Xuân", E_HOI_KHI, 1),
        BanMenh("Tịch Diệt Chỉ", E_NUT_THAT, 2),
    )

    // --- Ngọc giản -------------------------------------------------------------------------------

    const val CONG = 0
    const val TAM = 1
    const val THUAT = 2
    const val DAN = 3
    val SCROLL_KINDS = arrayOf("Công pháp", "Tâm pháp", "Thuật pháp", "Đan phương")
    fun scrollName(kind: Int, id: Int) = when (kind) {
        CONG -> CONG_PHAP[id].name
        TAM -> TAM_PHAP[id].name
        DAN -> DanDao.RECIPES[id].name
        else -> THUAT_PHAP[id].name
    }
}
