package com.exacta.reader.game

/**
 * Đạo hiệu: 72 titles earned by reading habits, six groups of twelve. The first six of each group are
 * Exacta.OS Omega's (a few adapted to what the app counts); the other six came with 0.8.
 */
object DaoHieu {
    const val COUNT = 72
    const val GROUPS = 6
    const val PER_GROUP = COUNT / GROUPS

    val GROUP_NAMES = arrayOf("Tu hành", "Đạo tâm", "Thời khắc", "Kinh các", "Pháp bảo", "Kỳ duyên")

    val NAMES = arrayOf(
        // Tu hành
        "Nhập Môn Đệ Tử", "Ngoại Môn Đệ Tử", "Nội Môn Đệ Tử", "Chân Truyền Đệ Tử", "Thư Hải Vô Nhai", "Phi Thăng Tiên Nhân",
        "Kết Đan Chân Nhân", "Nguyên Anh Lão Tổ", "Hóa Thần Tôn Giả", "Đại Thừa Tông Sư", "Thư Lâm Cự Phách", "Tiên Tôn Chí Cao",
        // Đạo tâm
        "Bất Động Tâm", "Kiên Như Bàn Thạch", "Bách Nhật Trúc Cơ", "Niên Niên Bất Đoạn", "Nghịch Thiên Độ Kiếp", "Bại Nhi Bất Khuất",
        "Tâm Như Chỉ Thủy", "Tĩnh Tọa Thập Phương", "Thiên Trường Địa Cửu", "Vạn Kiếp Bất Ma", "Bách Chiết Bất Hồi", "Đạo Tâm Kiên Cố",
        // Thời khắc
        "Dạ Tu Giả", "Thần Khởi Giả", "Tý Thời Ngộ Đạo", "Nhất Tọa Vong Ưu", "Nhật Đọc Vạn Ngôn", "Tân Xuân Tu Đạo",
        "Ngọ Thời Độc Thư", "Hoàng Hôn Thư Khách", "Thông Tiêu Đạt Đán", "Bất Miên Bất Hưu", "Tứ Thời Bất Đoạn", "Sóc Vọng Tu Hành",
        // Kinh các
        "Sơ Thông Kinh Điển", "Bác Lãm Quần Thư", "Vạn Quyển Tàng Thư", "Nhất Kiếm Thông Thiên", "Bách Tầng Công Pháp", "Thiên Hạ Thư Sinh",
        "Thư Sơn Hữu Lộ", "Học Phú Ngũ Xa", "Cự Thư Chinh Phục", "Nhất Nhật Nhất Quyển", "Tầng Tầng Đột Phá", "Bách Gia Chư Tử",
        // Pháp bảo
        "Sơ Luyện Pháp Bảo", "Luyện Khí Đại Sư", "Vạn Bảo Chi Chủ", "Thần Khí Nhận Chủ", "Lò Hỏa Thuần Thanh", "Tứ Bảo Tề Tụ",
        "Vạn Khí Triều Tông", "Thất Thập Nhị Bảo", "Cửu Bộ Quy Tâm", "Lục Phẩm Tề Tụ", "Kiếp Hỏa Luyện Bảo", "Khí Linh Chi Chủ",
        // Kỳ duyên
        "Phúc Duyên Thâm Hậu", "Bế Quan Khổ Tu", "Bí Cảnh Thám Hiểm", "Nguyệt Hạ Độc Thư", "Sinh Thần Đọc Sách", "Thiên Kiếp Bất Diệt",
        "Cơ Duyên Như Vũ", "Phúc Tinh Cao Chiếu", "Đan Đạo Sơ Thành", "Linh Phù Đại Sư", "Phá Cảnh Thần Tốc", "Bí Cảnh Tông Chủ",
    )

    val RULES = arrayOf(
        "Đọc được 1 vạn từ", "Đọc được 10 vạn từ", "Đọc được 100 vạn từ", "Đọc được 500 vạn từ", "Đọc được 1.000 vạn từ",
        "Độ kiếp phi thăng, lên Tiên nhân",
        "Đạt Kim Đan", "Phá đan thành anh, đạt Nguyên Anh", "Đạt Hóa Thần", "Đạt Đại Thừa", "Đọc được 2.000 vạn từ",
        "Đạt Tiên nhân nhất phẩm",
        "Chuỗi đọc 7 ngày", "Chuỗi đọc 30 ngày", "Chuỗi đọc 100 ngày", "Chuỗi đọc 365 ngày", "Vượt thiên kiếp lần đầu",
        "Độ kiếp thất bại rồi vẫn vượt kiếp",
        "Chuỗi đọc 14 ngày", "Chuỗi đọc 60 ngày", "Chuỗi đọc 200 ngày", "Vượt cả 9 cửa thiên kiếp", "Độ kiếp thất bại 3 lần",
        "Có 300 ngày đọc sách",
        "Đọc tổng 10 giờ từ 23 giờ đến 5 giờ sáng", "Đọc tổng 5 giờ từ 5 đến 7 giờ sáng", "Đọc tổng 1 giờ lúc nửa đêm (0 – 1 giờ)",
        "Một lần đọc liền 2 giờ", "Một ngày đọc 4 giờ", "Đọc từ 10 phút trong ba ngày Tết",
        "Đọc tổng 5 giờ từ 11 đến 13 giờ", "Đọc tổng 10 giờ từ 17 đến 19 giờ", "Một lần đọc liền 4 giờ", "Một ngày đọc 8 giờ",
        "Có đọc trong 12 tháng khác nhau", "Đọc từ 10 phút trong 12 ngày mồng một hay rằm",
        "Đọc xong 1 cuốn", "Đọc xong 10 cuốn", "Đọc xong 50 cuốn", "Đọc xong một cuốn trong 3 ngày", "Lên tầng công pháp 100 lần",
        "Mở 30 cuốn sách khác nhau",
        "Đọc xong 25 cuốn", "Đọc xong 100 cuốn", "Đọc xong một cuốn trên 100 vạn từ", "Đọc xong một cuốn trong 1 ngày",
        "Lên tầng công pháp 500 lần", "Mở 100 cuốn sách khác nhau",
        "Luyện thành pháp bảo đầu tiên", "Có 18 loại pháp bảo", "Có 36 loại pháp bảo", "Luyện thành một món Thần khí",
        "Luyện thành 30 pháp bảo", "Gom đủ một bộ pháp bảo",
        "Có 54 loại pháp bảo", "Có đủ 72 loại pháp bảo", "Gom đủ 9 bộ pháp bảo", "Có pháp bảo đủ 6 phẩm",
        "Vượt kiếp phá lô 5 lần", "5 pháp bảo thức tỉnh khí linh",
        "Nhận 20 khoáng thạch quà 10 vạn từ", "Tổng thời gian đọc 100 giờ", "Qua bí cảnh tuần 10 lần",
        "Đọc từ 10 phút đêm Trung Thu (rằm tháng Tám)", "Đọc từ 10 phút đúng ngày sinh nhật", "Vượt thiên kiếp 5 lần",
        "Gặp kỳ ngộ 20 lần", "Gặp kỳ ngộ 100 lần", "Dùng 10 viên đan dược", "Dùng 10 lá phù", "Dùng Phá Cảnh Đan khi độ kiếp",
        "Qua bí cảnh tuần 30 lần",
    )

    fun group(t: Int) = t / PER_GROUP
}
