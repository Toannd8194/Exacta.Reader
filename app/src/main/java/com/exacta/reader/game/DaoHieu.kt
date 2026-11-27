package com.exacta.reader.game

/**
 * Đạo hiệu: 36 titles earned by reading habits, six groups of six, as in Exacta.OS Omega. The
 * firmware's titles for systems this app does not have (kỳ ngộ, bế quan, hợp luyện, linh thạch)
 * are replaced by ones the app can count.
 */
object DaoHieu {
    const val COUNT = 36
    const val GROUPS = 6
    const val PER_GROUP = COUNT / GROUPS

    val GROUP_NAMES = arrayOf("Tu hành", "Đạo tâm", "Thời khắc", "Kinh các", "Pháp bảo", "Kỳ duyên")

    val NAMES = arrayOf(
        // Tu hành
        "Nhập Môn Đệ Tử", "Ngoại Môn Đệ Tử", "Nội Môn Đệ Tử", "Chân Truyền Đệ Tử", "Thư Hải Vô Nhai", "Phi Thăng Tiên Nhân",
        // Đạo tâm
        "Bất Động Tâm", "Kiên Như Bàn Thạch", "Bách Nhật Trúc Cơ", "Niên Niên Bất Đoạn", "Nghịch Thiên Độ Kiếp", "Bại Nhi Bất Khuất",
        // Thời khắc
        "Dạ Tu Giả", "Thần Khởi Giả", "Tý Thời Ngộ Đạo", "Nhất Tọa Vong Ưu", "Nhật Đọc Vạn Ngôn", "Tân Xuân Tu Đạo",
        // Kinh các
        "Sơ Thông Kinh Điển", "Bác Lãm Quần Thư", "Vạn Quyển Tàng Thư", "Nhất Kiếm Thông Thiên", "Bách Tầng Công Pháp", "Thiên Hạ Thư Sinh",
        // Pháp bảo
        "Sơ Luyện Pháp Bảo", "Luyện Khí Đại Sư", "Vạn Bảo Chi Chủ", "Thần Khí Nhận Chủ", "Lò Hỏa Thuần Thanh", "Tứ Bảo Tề Tụ",
        // Kỳ duyên
        "Phúc Duyên Thâm Hậu", "Bế Quan Khổ Tu", "Bí Cảnh Thám Hiểm", "Nguyệt Hạ Độc Thư", "Sinh Thần Đọc Sách", "Thiên Kiếp Bất Diệt",
    )

    val RULES = arrayOf(
        "Đọc được 1 vạn từ",
        "Đọc được 10 vạn từ",
        "Đọc được 100 vạn từ",
        "Đọc được 500 vạn từ",
        "Đọc được 1.000 vạn từ",
        "Độ kiếp phi thăng, lên Tiên nhân",
        "Chuỗi đọc 7 ngày",
        "Chuỗi đọc 30 ngày",
        "Chuỗi đọc 100 ngày",
        "Chuỗi đọc 365 ngày",
        "Vượt thiên kiếp lần đầu",
        "Độ kiếp thất bại rồi vẫn vượt kiếp",
        "Đọc tổng 10 giờ từ 23 giờ đến 5 giờ sáng",
        "Đọc tổng 5 giờ từ 5 đến 7 giờ sáng",
        "Đọc tổng 1 giờ lúc nửa đêm (0 – 1 giờ)",
        "Một lần đọc liền 2 giờ",
        "Một ngày đọc 4 giờ",
        "Đọc từ 10 phút trong ba ngày Tết",
        "Đọc xong 1 cuốn",
        "Đọc xong 10 cuốn",
        "Đọc xong 50 cuốn",
        "Đọc xong một cuốn trong 3 ngày",
        "Lên tầng công pháp 100 lần",
        "Mở 30 cuốn sách khác nhau",
        "Luyện thành pháp bảo đầu tiên",
        "Có 18 loại pháp bảo",
        "Có đủ 36 loại pháp bảo",
        "Luyện thành một món Thần khí",
        "Luyện thành 30 pháp bảo",
        "Gom đủ một bộ pháp bảo",
        "Nhận 20 khoáng thạch quà 10 vạn từ",
        "Tổng thời gian đọc 100 giờ",
        "Qua bí cảnh tuần 10 lần",
        "Đọc từ 10 phút đêm Trung Thu (rằm tháng Tám)",
        "Đọc từ 10 phút đúng ngày sinh nhật",
        "Vượt thiên kiếp 5 lần",
    )

    fun group(t: Int) = t / PER_GROUP
}
