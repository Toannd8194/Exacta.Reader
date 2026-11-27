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

    // Đặc hiệu: what a công pháp does besides its linh lực.
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
    val EFFECTS = arrayOf(
        "Không có đặc hiệu",
        "Đột phá hỏng chỉ hồi khí 1 ngày",
        "Ngưng đan, lôi kiếp, thiên kiếp tính sẵn 1 ngày",
        "Tâm ma tăng chậm 30%",
        "Căn cơ +5",
        "Tỉ lệ đột phá +5%",
        "Áp chế dày thêm 25%",
        "Lò luyện cháy nhanh 20%",
        "Kỳ ngộ nhiều thêm 30%",
        "Linh thạch từ chữ đọc +20%",
        "Thể phách +20%",
        "Thần thức +5",
        "Bình cảnh: 30% chữ thành linh lực thay vì 20%",
        "Nút thắt: 70% chữ thành linh lực thay vì 50%",
        "Đọc trọn một cuốn: ngộ tính +1",
    )

    class CongPhap(val name: String, val pham: Int, val attr: Int, val effect: Int)

    const val DAN_KHI = 0
    val CONG_PHAP = listOf(
        // Hoàng: to Trúc Cơ viên mãn.
        CongPhap("Dẫn Khí Quyết", 0, NONE, E_NONE),
        CongPhap("Canh Kim Quyết", 0, KIM, E_AP_CHE),
        CongPhap("Thanh Mộc Công", 0, MOC, E_CAN_CO),
        CongPhap("Nhược Thủy Quyết", 0, THUY, E_TINH_TAM),
        CongPhap("Liệt Hỏa Công", 0, HOA, E_LO_LUYEN),
        CongPhap("Hậu Thổ Quyết", 0, THO, E_NUT_THAT),
        // Huyền: to Kim Đan.
        CongPhap("Phá Quân Kim Quyết", 1, KIM, E_LINH_THACH),
        CongPhap("Trường Xuân Công", 1, MOC, E_HOI_KHI),
        CongPhap("Huyền Thủy Chân Kinh", 1, THUY, E_THAN_THUC),
        CongPhap("Xích Viêm Quyết", 1, HOA, E_KY_NGO),
        CongPhap("Trấn Nhạc Công", 1, THO, E_CAN_CO),
        CongPhap("Duệ Kim Kiếm Kinh", 1, KIEM, E_DOT_PHA),
        CongPhap("Đồng Bì Thiết Cốt Công", 1, THE, E_THE_PHACH),
        // Địa: to Nguyên Anh.
        CongPhap("Tinh Kim Bất Hoại Công", 2, KIM, E_HOI_KHI),
        CongPhap("Ất Mộc Sinh Sinh Quyết", 2, MOC, E_NGO_TINH),
        CongPhap("Bích Hải Triều Sinh Quyết", 2, THUY, E_TINH_TAM),
        CongPhap("Nam Minh Ly Hỏa Kinh", 2, HOA, E_KIEP_NGAY),
        CongPhap("Mậu Thổ Huyền Công", 2, THO, E_BINH_CANH),
        CongPhap("Ngũ Lôi Chính Pháp", 2, LOI, E_DOT_PHA),
        CongPhap("Minh Tâm Quán Tưởng Pháp", 2, THAN, E_THAN_THUC),
        // Thiên: to Hóa Thần.
        CongPhap("Canh Kim Tru Tà Quyết", 3, KIM, E_DOT_PHA),
        CongPhap("Thanh Đế Trường Sinh Kinh", 3, MOC, E_CAN_CO),
        CongPhap("Huyền Minh Chân Thủy Kinh", 3, THUY, E_AP_CHE),
        CongPhap("Đại Nhật Phần Thiên Quyết", 3, HOA, E_KIEP_NGAY),
        CongPhap("Hoàng Đình Hậu Thổ Kinh", 3, THO, E_NUT_THAT),
        CongPhap("Hàn Ngọc Băng Phách Quyết", 3, BANG, E_TINH_TAM),
        CongPhap("Ngự Phong Lưu Vân Quyết", 3, PHONG, E_KY_NGO),
        CongPhap("Thái Bạch Kiếm Điển", 3, KIEM, E_HOI_KHI),
        // Linh: Linh giới to Đại Thừa.
        CongPhap("Vạn Kiếm Quy Tông Lục", 4, KIEM, E_DOT_PHA),
        CongPhap("Cửu Thiên Lôi Đế Kinh", 4, LOI, E_KIEP_NGAY),
        CongPhap("Thái Âm Luyện Hình Kinh", 4, THUY, E_TINH_TAM),
        CongPhap("Bất Diệt Kim Thân Quyết", 4, THE, E_THE_PHACH),
        CongPhap("Hỗn Nguyên Ngũ Hành Kinh", 4, NONE, E_BINH_CANH),
        // Tiên: to the gate of Tiên cảnh.
        CongPhap("Hồng Mông Tạo Hóa Kinh", 5, NONE, E_AP_CHE),
        CongPhap("Chu Thiên Tinh Thần Kinh", 5, THAN, E_THAN_THUC),
        CongPhap("Thái Thượng Vong Tình Lục", 5, THUY, E_TINH_TAM),
    )

    // --- Tâm pháp ----------------------------------------------------------------------------------

    const val TP_THANH_TAM = 0
    const val TP_TINH_TOA = 1
    const val TP_BANG_TAM = 2
    const val TP_VONG_NGA = 3
    const val TP_CHUNG_MA = 4
    const val TP_DUONG_KHI = 5
    const val TP_DINH_HON = 6

    class TamPhap(val name: String, val text: String)

    val TAM_PHAP = listOf(
        TamPhap("Thanh Tâm Quyết", "Tâm ma tăng chậm một nửa"),
        TamPhap("Tĩnh Tọa Kinh", "Bế quan viên mãn được thêm 30% linh thạch"),
        TamPhap("Băng Tâm Quyết", "Đứt chuỗi lần đầu mỗi tháng không sinh tâm ma"),
        TamPhap("Vong Ngã Chân Quyết", "Buổi đọc từ 90 phút tính gấp đôi cho thể phách và tâm cảnh"),
        TamPhap("Đạo Tâm Chủng Ma", "Tâm ma cao lại cộng tốc độ tu (tâm ma / 5, tối đa 20%); chỉ khi vận công pháp Kiếm"),
        TamPhap("Dưỡng Khí Thiên", "Căn cơ +5"),
        TamPhap("Định Hồn Chân Ngôn", "Đột phá hỏng chỉ tẩu hỏa nhập ma khi tâm ma từ 85"),
    )
    const val TAM_PRICE = 150L
    /** Vận chuyển (sittings of 30 minutes) to nhập môn and đại thành. */
    const val TAM_NHAP_MON = 5
    const val TAM_DAI_THANH = 30

    // --- Thuật pháp --------------------------------------------------------------------------------

    const val K_CONG_KICH = 0
    const val K_PHONG_NGU = 1
    const val K_DON = 2
    const val K_PHU_TRO = 3
    const val K_THAN_THONG = 4
    val KINDS = arrayOf("Công kích", "Phòng ngự", "Độn thuật", "Phụ trợ", "Thần thông")

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

    class ThuatPhap(val name: String, val attr: Int, val kind: Int, val text: String)

    val THUAT_PHAP = listOf(
        ThuatPhap("Hỏa Đạn Thuật", HOA, K_CONG_KICH, "Qua bí cảnh tuần: thêm linh thạch"),
        ThuatPhap("Ngự Kiếm Thuật", KIM, K_CONG_KICH, "Qua bí cảnh tuần: thêm linh thạch"),
        ThuatPhap("Chưởng Tâm Lôi", LOI, K_CONG_KICH, "Qua bí cảnh tuần: thêm linh thạch"),
        ThuatPhap("Hàn Băng Tiễn", BANG, K_CONG_KICH, "Qua bí cảnh tuần: thêm linh thạch"),
        ThuatPhap("Kim Cang Tráo", KIM, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Thủy Mạc Thuật", THUY, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Mộc Giáp Thuật", MOC, K_PHONG_NGU, "Kiếp đang độ: lỡ một ngày vẫn không sao"),
        ThuatPhap("Thổ Độn", THO, K_DON, "Đột phá hỏng: hụt khí chỉ mất 1%, kinh mạch chỉ 3 ngày"),
        ThuatPhap("Phong Độn", PHONG, K_DON, "Đột phá hỏng: hồi khí chỉ 1 ngày"),
        ThuatPhap("Huyết Độn", HOA, K_DON, "Đột phá hỏng: tẩu hỏa nhập ma thành kinh mạch tổn thương"),
        ThuatPhap("Thiên Nhãn Thuật", THUY, K_PHU_TRO, "Tâm ma kiếp: chặn một lần tâm ma tấn công"),
        ThuatPhap("Liễm Tức Thuật", MOC, K_PHU_TRO, "Nhảy chương quá 3 lần một ngày không sinh tâm ma"),
        ThuatPhap("Pháp Tướng Thiên Địa", THO, K_THAN_THONG, "Tỉ lệ đột phá +5%, thêm 1% mỗi bậc thuần thục"),
        ThuatPhap("Thần Thức Hóa Kiếm", KIM, K_THAN_THONG, "Chặn một lần tâm ma tấn công; đột phá hỏng tâm ma chỉ +5"),
        ThuatPhap("Thuấn Di", PHONG, K_THAN_THONG, "Đột phá hỏng không phải hồi khí"),
    )
    /** The first thuật pháp a hành learns. */
    val FIRST_THUAT = intArrayOf(T_NGU_KIEM, T_MOC_GIAP, T_THUY_MAC, T_HOA_DAN, T_THO_DON)
    /** A công pháp of Hoàng phẩm for each hành. */
    val FIRST_CONG = intArrayOf(1, 2, 3, 4, 5)
    fun thuatPrice(i: Int) = if (THUAT_PHAP[i].kind == K_THAN_THONG) 400L else 80L

    val MASTERY = arrayOf("Sơ Học", "Tiểu Thành", "Đại Thành", "Viên Mãn", "Hóa Cảnh")
    val MASTERY_AT = intArrayOf(0, 3, 8, 15, 25)
    fun mastery(uses: Int) = MASTERY_AT.indexOfLast { uses >= it }

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
