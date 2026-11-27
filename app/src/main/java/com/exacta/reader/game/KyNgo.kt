package com.exacta.reader.game

/**
 * 2.2: the 36 kỳ ngộ. Each is met on its own moment of reading (a sitting, an hour, a feast, a
 * book, a streak, a breakthrough) at its own chance; the rarer the moment, the richer the reward.
 * A reward is rolled in its tier: the tier itself is the rare outcome, one or two below are the
 * common ones.
 */
object KyNgo {
    const val T_WORDS = 0        // the words since the last one reach a random 10..50 vạn
    const val T_SITTING = 1      // a sitting reaches `arg` minutes
    const val T_DAY = 2          // the day's reading reaches `arg` minutes
    const val T_HOUR = 3         // ten minutes of a sitting in the hours from `arg` to `arg2`
    const val T_LUNAR = 4        // ten minutes read on lunar day `arg` of month `arg2` (0: any month)
    const val T_TERM = 5         // ten minutes read on the first day of a tiết khí
    const val T_NEW_BOOK = 6
    const val T_HALF_BOOK = 7
    const val T_FINISH = 8       // `arg`: 0 any, 1 within three days, 2 a new author, 3 a book of 100 vạn
    const val T_STREAK = 9       // the streak reaches a multiple of `arg`
    const val T_BREAK = 10
    const val T_BICANH = 11

    val TIERS = arrayOf("thường", "khá", "quý", "hiếm", "tuyệt thế")

    class Event(val name: String, val trigger: Int, val arg: Int, val arg2: Int, val permille: Int, val tier: Int, val text: String)

    val EVENTS = listOf(
        Event("Linh khí hội tụ", T_WORDS, 0, 0, 1000, 1, "Chữ đọc tích đủ một mốc ngẫu nhiên từ 10 tới 50 vạn"),
        Event("Cổ động phủ hiện thế", T_WORDS, 0, 0, 250, 3, "Cùng mốc chữ ấy, một động phủ cổ mở cửa"),
        Event("Tĩnh tọa ngộ đạo", T_SITTING, 30, 0, 250, 0, "Một buổi đọc liền 30 phút"),
        Event("Thần du thái hư", T_SITTING, 45, 0, 250, 1, "Một buổi đọc liền 45 phút"),
        Event("Nhập định một canh", T_SITTING, 60, 0, 300, 1, "Một buổi đọc liền 60 phút"),
        Event("Vong ngã chi cảnh", T_SITTING, 90, 0, 300, 2, "Một buổi đọc liền 90 phút"),
        Event("Thiên nhân hợp nhất", T_SITTING, 120, 0, 250, 3, "Một buổi đọc liền 120 phút"),
        Event("Cần tu bất đãi", T_DAY, 60, 0, 200, 0, "Một ngày đọc đủ 60 phút"),
        Event("Khổ tu nhật nguyệt", T_DAY, 120, 0, 250, 1, "Một ngày đọc đủ 120 phút"),
        Event("Quên ăn quên ngủ", T_DAY, 180, 0, 250, 2, "Một ngày đọc đủ 180 phút"),
        Event("Tử khí đông lai", T_HOUR, 5, 7, 120, 1, "Đọc trong giờ Mão (5 – 7 giờ sáng)"),
        Event("Chính ngọ dương cương", T_HOUR, 11, 13, 80, 0, "Đọc trong giờ Ngọ (11 – 13 giờ)"),
        Event("Hoàng hôn tà dương", T_HOUR, 17, 19, 80, 0, "Đọc trong giờ Dậu (17 – 19 giờ)"),
        Event("Nửa đêm giờ Tý", T_HOUR, 23, 1, 100, 1, "Đọc trong giờ Tý (23 – 1 giờ)"),
        Event("Canh ba trăng lặn", T_HOUR, 1, 3, 60, 2, "Đọc trong giờ Sửu (1 – 3 giờ sáng)"),
        Event("Sóc nhật linh triều", T_LUNAR, 1, 0, 300, 1, "Đọc vào ngày mồng một âm lịch"),
        Event("Vọng nguyệt linh triều", T_LUNAR, 15, 0, 300, 1, "Đọc vào ngày rằm"),
        Event("Nguyên tiêu đăng hội", T_LUNAR, 15, 1, 600, 2, "Đọc vào rằm tháng Giêng"),
        Event("Tân xuân phúc lộc", T_LUNAR, 2, 1, 500, 2, "Đọc vào mồng hai Tết"),
        Event("Đoan ngọ dương khí", T_LUNAR, 5, 5, 500, 2, "Đọc vào Tết Đoan ngọ (5/5)"),
        Event("Thất tịch tinh hà", T_LUNAR, 7, 7, 500, 2, "Đọc vào đêm Thất tịch (7/7)"),
        Event("Vu lan âm môn", T_LUNAR, 15, 7, 400, 3, "Đọc vào rằm tháng Bảy"),
        Event("Trung thu nguyệt hoa", T_LUNAR, 15, 8, 500, 3, "Đọc vào Tết Trung thu"),
        Event("Trùng cửu đăng cao", T_LUNAR, 9, 9, 500, 2, "Đọc vào Tết Trùng cửu (9/9)"),
        Event("Tiết khí giao thời", T_TERM, 0, 0, 250, 1, "Đọc vào ngày đầu của một tiết khí"),
        Event("Khai quyển hữu ích", T_NEW_BOOK, 0, 0, 150, 0, "Mở đọc một cuốn mới"),
        Event("Bán quyển ngộ huyền", T_HALF_BOOK, 0, 0, 150, 0, "Đọc tới nửa một cuốn"),
        Event("Quyển chung đắc pháp", T_FINISH, 0, 0, 400, 1, "Đọc xong một cuốn"),
        Event("Nhất khí a thành", T_FINISH, 1, 0, 500, 2, "Đọc xong một cuốn trong ba ngày"),
        Event("Bách gia chư tử", T_FINISH, 2, 0, 300, 2, "Đọc xong cuốn đầu tiên của một tác giả"),
        Event("Vạn quyển hữu thần", T_FINISH, 3, 0, 600, 3, "Đọc xong một cuốn từ 100 vạn chữ"),
        Event("Bảy ngày không dứt", T_STREAK, 7, 0, 400, 1, "Chuỗi ngày đọc chạm mỗi mốc 7 ngày"),
        Event("Một tháng chuyên tâm", T_STREAK, 30, 0, 600, 2, "Chuỗi ngày đọc chạm mỗi mốc 30 ngày"),
        Event("Trăm ngày trúc cơ", T_STREAK, 100, 0, 800, 4, "Chuỗi ngày đọc chạm mỗi mốc 100 ngày"),
        Event("Phá cảnh dư uy", T_BREAK, 0, 0, 500, 2, "Đột phá thành công"),
        Event("Bí cảnh thâm xứ", T_BICANH, 0, 0, 300, 2, "Qua bí cảnh tuần"),
    )

    /** The words to the next "Linh khí hội tụ": 10 to 50 vạn. */
    const val WORDS_MIN = 100_000L
    const val WORDS_MAX = 500_000L

    fun hourIn(h: Int, from: Int, to: Int) = if (from < to) h in from until to else h >= from || h < to
}
