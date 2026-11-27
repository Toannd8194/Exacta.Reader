package com.exacta.reader.game

/**
 * The 72 kỳ ngộ, by how rarely they come: 36 thường gặp, 18 ít gặp, 9 hiếm gặp, 6 cực kỳ hiếm,
 * 3 thiên cổ kỳ ngộ. Each waits for its own moment of reading (a sitting, an hour of the day, a day
 * of the week, a lunar feast, a book, a streak, a breakthrough…) and rolls its chance once a day
 * when the moment comes. The rarer the kỳ ngộ, the richer what it gives.
 */
object KyNgo {
    const val T_WORDS = 0        // the words since the last reach a random 10..50 vạn
    const val T_SITTING = 1      // a sitting reaches arg minutes
    const val T_DAY = 2          // the day's reading reaches arg minutes
    const val T_HOUR = 3         // ten minutes of a sitting in the hours arg..arg2
    const val T_LUNAR = 4        // ten minutes read on lunar day arg of month arg2 (0: any month)
    const val T_TERM = 5         // ten minutes read on the first day of a tiết khí
    const val T_NEW_BOOK = 6
    const val T_HALF_BOOK = 7
    const val T_FINISH = 8       // arg: 0 any, 1 within three days, 2 a new author, 3 a book of 100 vạn, 4 the hundredth book
    const val T_STREAK = 9       // the streak reaches a multiple of arg
    const val T_BREAK = 10       // a major breakthrough into realm arg or higher
    const val T_BICANH = 11
    const val T_WEEKDAY = 12     // 30 minutes read on weekday arg (1 Monday … 7 Sunday)
    const val T_VAN = 13         // every vạn of words
    const val T_MINOR = 14       // a tiểu cảnh broken
    const val T_PILL = 15        // a furnace of pills made, of phẩm arg or better
    const val T_FORGE = 16       // a treasure born
    const val T_HARVEST = 17
    const val T_BEQUAN = 18
    const val T_TASK = 19        // a sect task done
    const val T_WEEKEND = 20     // 30 minutes on both Saturday and Sunday
    const val T_RIVAL = 21       // the rival overtaken
    const val T_MASTER = 22      // the master of a bí cảnh beaten
    const val T_KIEP = 23        // a forge trial passed
    const val T_NIGHT = 24       // a sitting of an hour reaching into giờ Tý
    const val T_DAWN = 25        // a sitting of 30 minutes in giờ Mão
    const val T_DITICH = 26
    const val T_DAIBI = 27       // a sect contest ended in the top three
    const val T_TET = 28         // read on one of the three days of Tết

    val RARITY = arrayOf("Thường gặp", "Ít gặp", "Hiếm gặp", "Cực kỳ hiếm", "Thiên cổ kỳ ngộ")
    val TIERS = arrayOf("thường", "khá", "quý", "hiếm", "tuyệt thế")

    class Event(val name: String, val rarity: Int, val trigger: Int, val arg: Int, val arg2: Int, val permille: Int, val text: String, val tale: String)

    private fun e(name: String, rarity: Int, trigger: Int, arg: Int, arg2: Int, permille: Int, text: String, tale: String) =
        Event(name, rarity, trigger, arg, arg2, permille, text, tale)

    val EVENTS = listOf(
        // --- Thường gặp ---------------------------------------------------------------------------------
        e("Tĩnh tọa ngộ đạo", 0, T_SITTING, 30, 0, 200, "Một buổi đọc liền 30 phút",
            "Nửa canh giờ tĩnh tọa, tâm như mặt hồ không gợn. Một tia đạo vận lướt qua, để lại chút lợi lộc bên mình."),
        e("Thần du thái hư", 0, T_SITTING, 45, 0, 200, "Một buổi đọc liền 45 phút",
            "Thần thức theo trang sách bay khỏi nhục thân, ngao du thái hư. Lúc trở về, trong tay áo có thêm vật lạ."),
        e("Nhập định một canh", 0, T_SITTING, 60, 0, 250, "Một buổi đọc liền 60 phút",
            "Một canh giờ nhập định, chu thiên vận chuyển trọn vẹn. Linh khí ngưng tụ, hóa thành vật hữu hình rơi xuống trước mặt."),
        e("Cần tu bất đãi", 0, T_DAY, 30, 0, 100, "Một ngày đọc đủ 30 phút",
            "Một ngày cần tu, chẳng chút lười nhác. Trưởng lão đi ngang khẽ gật đầu, để lại chút ban thưởng."),
        e("Khổ tu nhật nguyệt", 0, T_DAY, 60, 0, 150, "Một ngày đọc đủ 60 phút",
            "Từ lúc mặt trời mọc tới khi trăng lên, đạo hữu vẫn chưa buông sách. Khổ tu tất có hồi báo."),
        e("Hai canh khổ đọc", 0, T_DAY, 90, 0, 200, "Một ngày đọc đủ 90 phút",
            "Hai canh giờ miệt mài, đèn dầu cạn rồi lại thêm. Mực thơm thấm vào kinh mạch, thành một phần cơ duyên."),
        e("Nửa đêm giờ Tý", 0, T_HOUR, 23, 1, 100, "Đọc trong giờ Tý (23 – 1 giờ)",
            "Giờ Tý, âm dương giao thoa, vạn vật lặng im. Trong tĩnh mịch, một vật lạ lặng lẽ rơi xuống án thư."),
        e("Canh ba trăng lặn", 0, T_HOUR, 1, 3, 120, "Đọc trong giờ Sửu (1 – 3 giờ)",
            "Canh ba trăng lặn, chỉ còn ngọn đèn và trang sách. Bóng tối ban thưởng cho kẻ chịu thức cùng nó."),
        e("Sương sớm giờ Dần", 0, T_HOUR, 3, 5, 120, "Đọc trong giờ Dần (3 – 5 giờ)",
            "Giờ Dần, sương còn đọng trên lá. Một giọt sương mang linh khí rơi trúng trang sách, hóa thành vật nhỏ."),
        e("Tử khí đông lai", 0, T_HOUR, 5, 7, 100, "Đọc trong giờ Mão (5 – 7 giờ)",
            "Giờ Mão, tử khí từ phương đông tràn tới. Đạo hữu hít một hơi, tinh hoa đầu ngày ngưng lại nơi đầu ngón tay."),
        e("Thần thời long khí", 0, T_HOUR, 7, 9, 80, "Đọc trong giờ Thìn (7 – 9 giờ)",
            "Giờ Thìn, long khí thức dậy giữa núi sông. Một vảy rồng mờ ảo lướt qua, rơi lại chút quà."),
        e("Tỵ thời thanh minh", 0, T_HOUR, 9, 11, 80, "Đọc trong giờ Tỵ (9 – 11 giờ)",
            "Giờ Tỵ, trời trong gió nhẹ, đầu óc thanh minh. Chữ nghĩa trôi vào như suối, để lại một hạt minh châu."),
        e("Chính ngọ dương cương", 0, T_HOUR, 11, 13, 80, "Đọc trong giờ Ngọ (11 – 13 giờ)",
            "Chính ngọ, dương khí cực thịnh. Một luồng chí dương chi khí tụ lại, hóa thành vật nhỏ lấp lánh."),
        e("Mùi thời trà dư", 0, T_HOUR, 13, 15, 80, "Đọc trong giờ Mùi (13 – 15 giờ)",
            "Giờ Mùi, chén trà chiều còn ấm. Bên chén trà, một lá bùa cũ không biết từ đâu bay tới."),
        e("Thân thời viên khiếu", 0, T_HOUR, 15, 17, 80, "Đọc trong giờ Thân (15 – 17 giờ)",
            "Giờ Thân, tiếng vượn hú vọng từ núi xa. Một con linh hầu ném qua cửa sổ một vật tròn lấp lánh."),
        e("Hoàng hôn tà dương", 0, T_HOUR, 17, 19, 80, "Đọc trong giờ Dậu (17 – 19 giờ)",
            "Tà dương nhuộm đỏ núi xa, đạo hữu vẫn lật trang. Ánh chiều cuối ngày để lại chút quà mọn."),
        e("Tuất thời đăng hỏa", 0, T_HOUR, 19, 21, 80, "Đọc trong giờ Tuất (19 – 21 giờ)",
            "Giờ Tuất, đèn đuốc tiên thành sáng rực. Một ngọn đăng hỏa bay lạc vào phòng, trong lửa có vật quý."),
        e("Hợi thời tĩnh dạ", 0, T_HOUR, 21, 23, 80, "Đọc trong giờ Hợi (21 – 23 giờ)",
            "Giờ Hợi, đêm tĩnh như nước. Trong khoảng lặng ấy, linh khí khẽ đọng lại thành một món nhỏ."),
        e("Khai quyển hữu ích", 0, T_NEW_BOOK, 0, 0, 250, "Mở đọc một cuốn mới",
            "Mở quyển sách mới, mùi giấy mực như linh hương. Khai quyển hữu ích, cơ duyên theo trang đầu mà tới."),
        e("Bán quyển ngộ huyền", 0, T_HALF_BOOK, 0, 0, 250, "Đọc tới nửa một cuốn",
            "Đi được nửa quyển, đạo hữu chợt ngộ ra huyền cơ giấu giữa các dòng chữ."),
        e("Quyển chung đắc pháp", 0, T_FINISH, 0, 0, 500, "Đọc xong một cuốn",
            "Trang cuối khép lại, cả quyển sách hóa thành một luồng sáng chui vào thức hải. Quyển chung đắc pháp."),
        e("Nguyệt diệu thanh huy", 0, T_WEEKDAY, 1, 0, 60, "Đọc đủ 30 phút vào thứ Hai",
            "Ngày Nguyệt diệu, ánh trăng còn vương từ đêm trước. Thanh huy đọng lại thành một vật mát lạnh."),
        e("Hỏa diệu luyện tâm", 0, T_WEEKDAY, 2, 0, 60, "Đọc đủ 30 phút vào thứ Ba",
            "Ngày Hỏa diệu, lửa trong tâm cháy đều. Tâm được luyện, tay cũng có thêm chút lợi lộc."),
        e("Thủy diệu nhuận mạch", 0, T_WEEKDAY, 3, 0, 60, "Đọc đủ 30 phút vào thứ Tư",
            "Ngày Thủy diệu, chân khí như nước chảy qua kinh mạch. Một giọt thủy tinh linh khí rơi xuống lòng bàn tay."),
        e("Mộc diệu sinh cơ", 0, T_WEEKDAY, 4, 0, 60, "Đọc đủ 30 phút vào thứ Năm",
            "Ngày Mộc diệu, sinh cơ dạt dào. Một mầm linh thảo nảy ra ngay bên án thư."),
        e("Kim diệu duệ khí", 0, T_WEEKDAY, 5, 0, 60, "Đọc đủ 30 phút vào thứ Sáu",
            "Ngày Kim diệu, kiếm khí trong người sắc như gió thu. Một mảnh kim tinh bị kiếm khí cắt ra, rơi xuống."),
        e("Thổ diệu hậu đức", 0, T_WEEKDAY, 6, 0, 60, "Đọc đủ 30 phút vào thứ Bảy",
            "Ngày Thổ diệu, đất dày nâng đỡ vạn vật. Đạo hữu ngồi vững như núi, đất cũng biết đền đáp."),
        e("Nhật diệu chiếu thân", 0, T_WEEKDAY, 7, 0, 60, "Đọc đủ 30 phút vào Chủ nhật",
            "Ngày Nhật diệu, ánh dương chiếu khắp thân. Trong vầng sáng ấy, một vật nhỏ lấp lánh hiện ra."),
        e("Phiên thư hữu cảm", 0, T_VAN, 0, 0, 60, "Mỗi vạn chữ đọc",
            "Lật hết một vạn chữ, trong lòng chợt có điều cảm ngộ. Cảm ngộ hóa thành vật, nằm gọn trong tay."),
        e("Mặc hương ngưng khí", 0, T_VAN, 0, 0, 40, "Mỗi vạn chữ đọc",
            "Mùi mực thơm ngưng lại giữa không trung, kết thành một viên ngọc nhỏ màu đen."),
        e("Tiểu cảnh đột phá", 0, T_MINOR, 0, 0, 400, "Phá tiểu cảnh thành công",
            "Bình chướng nhỏ vỡ tan, linh khí trong đan điền cuộn lên một vòng. Dư ba để lại chút cơ duyên."),
        e("Lò đan tỏa hương", 0, T_PILL, 0, 0, 250, "Luyện thành một lò đan",
            "Nắp lò mở ra, đan hương ngào ngạt khắp động phủ. Hương ấy dẫn tới một món quà bất ngờ."),
        e("Bảo quang xuất thế", 0, T_FORGE, 0, 0, 200, "Một pháp bảo luyện thành",
            "Pháp bảo xuất lò, bảo quang chiếu rọi. Ánh sáng ấy làm lộ ra một vật giấu trong góc lò."),
        e("Thu hoạch linh điền", 0, T_HARVEST, 0, 0, 250, "Thu hoạch một cây linh thảo",
            "Nhổ cây linh thảo lên, dưới rễ còn vướng một vật lạ. Đất linh điền quả không phụ người chăm."),
        e("Bế quan xuất quan", 0, T_BEQUAN, 0, 0, 300, "Một kỳ bế quan viên mãn",
            "Cửa đá mở ra, đạo hữu bước ra khỏi tĩnh thất, khí tức thâm trầm hơn. Ngoài cửa đã có quà chờ sẵn."),
        e("Nhiệm vụ tông môn", 0, T_TASK, 0, 0, 200, "Xong một nhiệm vụ tông môn",
            "Chấp sự đường ghi công, tiện tay đưa thêm một món: thưởng cho đệ tử chăm chỉ."),
        // --- Ít gặp ---------------------------------------------------------------------------------------
        e("Vong ngã chi cảnh", 1, T_SITTING, 90, 0, 300, "Một buổi đọc liền 90 phút",
            "Đọc tới quên cả thân mình, vật ngã lưỡng vong. Thiên địa cảm ứng, ban cho một phần cơ duyên không nhỏ."),
        e("Thiên nhân hợp nhất", 1, T_SITTING, 120, 0, 350, "Một buổi đọc liền 120 phút",
            "Hai canh giờ không rời trang sách, thiên nhân hợp nhất trong khoảnh khắc. Đạo hữu đã chạm vào một góc của đại đạo."),
        e("Quên ăn quên ngủ", 1, T_DAY, 180, 0, 350, "Một ngày đọc đủ 180 phút",
            "Ba canh giờ chìm trong sách, quên ăn quên ngủ. Thiên đạo thương người si mê, đổ xuống một trận cơ duyên."),
        e("Sóc nhật linh triều", 1, T_LUNAR, 1, 0, 400, "Đọc vào ngày mồng một âm lịch",
            "Mồng một, linh triều dâng lên khắp sơn môn. Đạo hữu thuận dòng mà hấp thụ, được một phần lợi lộc."),
        e("Vọng nguyệt linh triều", 1, T_LUNAR, 15, 0, 400, "Đọc vào ngày rằm",
            "Trăng rằm vằng vặc, nguyệt hoa như nước tưới xuống. Đạo hữu ngưng thần thu nạp, được một phần lợi lộc."),
        e("Tiết khí giao thời", 1, T_TERM, 0, 0, 350, "Đọc vào ngày đầu của một tiết khí",
            "Tiết khí đổi thay, thiên địa linh khí chuyển mình. Đạo hữu nắm đúng khoảnh khắc giao thời, thu được lợi lộc."),
        e("Nhất khí a thành", 1, T_FINISH, 1, 0, 500, "Đọc xong một cuốn trong ba ngày",
            "Ba ngày một mạch đọc xong, nhất khí a thành. Kiếm ý trong lòng sắc thêm mấy phần, cơ duyên theo đó mà tới."),
        e("Bách gia chư tử", 1, T_FINISH, 2, 0, 400, "Đọc xong cuốn đầu tiên của một tác giả",
            "Lần đầu đọc trọn sách của vị tác giả này, như bái thêm một vị thầy. Bách gia chư tử, mỗi nhà một món quà."),
        e("Bảy ngày không dứt", 1, T_STREAK, 7, 0, 400, "Chuỗi ngày đọc chạm mỗi mốc 7 ngày",
            "Bảy ngày liền không rời đạo, chân khí lưu chuyển không ngừng. Sư môn ghi nhận, ban chút khích lệ."),
        e("Hai tuần chuyên cần", 1, T_STREAK, 14, 0, 500, "Chuỗi ngày đọc chạm mỗi mốc 14 ngày",
            "Nửa tháng chuyên cần, nền móng vững thêm một tầng. Gió núi mang tới một vật từ phương xa."),
        e("Bí cảnh thâm xứ", 1, T_BICANH, 0, 0, 400, "Qua bí cảnh tuần",
            "Rời bí cảnh, ngoái nhìn lại nơi sâu nhất, đạo hữu thấy một vật lấp lánh mà người khác đã bỏ sót."),
        e("Song nhật cần tu", 1, T_WEEKEND, 0, 0, 300, "Đọc đủ 30 phút cả thứ Bảy và Chủ nhật",
            "Hai ngày nghỉ mà chẳng nghỉ tu, đạo tâm như sắt. Nhật nguyệt cùng chiếu, ban xuống một phần cơ duyên."),
        e("Vượt mặt sư huynh", 1, T_RIVAL, 0, 0, 500, "Tu vi vượt qua đối thủ trong tông",
            "Tu vi vượt lên trước đồng môn, đồng môn cũng phải chắp tay bái phục, tiện tay đưa tặng một món."),
        e("Chủ bí cảnh bại trận", 1, T_MASTER, 0, 0, 500, "Đánh bại chủ nhân bí cảnh",
            "Chủ nhân bí cảnh ngã xuống, kho tàng sau điện lộ ra. Đạo hữu chọn một món vừa tay."),
        e("Đan thành cực phẩm", 1, T_PILL, 3, 0, 500, "Luyện ra một lò đan cực phẩm",
            "Đan thành cực phẩm, đan vân hiện trên đỉnh động phủ. Dị tượng ấy dẫn tới cơ duyên khác."),
        e("Kiếp phá lô vượt qua", 1, T_KIEP, 0, 0, 500, "Vượt một kiếp phá lô",
            "Lò luyện qua kiếp, lôi quang tan dần. Trong tro tàn còn sót lại một mảnh tinh hoa."),
        e("Đêm khuya đọc liền", 1, T_NIGHT, 0, 0, 300, "Một buổi đọc liền 60 phút kéo vào giờ Tý",
            "Đọc tới giờ Tý chưa dứt, sao trời xoay vần trên mái. Một vì sao nhỏ rơi xuống ngay trước cửa."),
        e("Bình minh đọc sách", 1, T_DAWN, 0, 0, 300, "Một buổi đọc liền 30 phút trong giờ Mão",
            "Ánh bình minh vừa ló đã thấy đạo hữu ngồi đọc. Tia nắng đầu ngày rọi vào trang sách, để lại một dấu kim quang."),
        // --- Hiếm gặp ------------------------------------------------------------------------------------
        e("Linh khí hội tụ", 2, T_WORDS, 0, 0, 1000, "Chữ đọc tích đủ một mốc ngẫu nhiên từ 10 tới 50 vạn",
            "Chữ nghĩa tích tụ thành biển, linh khí bốn phương ầm ầm đổ về đan điền. Đạo hữu khẽ mở mắt, thấy trong tay đã có thêm một phần cơ duyên."),
        e("Phá cảnh dư uy", 2, T_BREAK, 1, 0, 600, "Đột phá một đại cảnh giới",
            "Vừa phá cảnh xong, dư uy còn chấn động bốn phía. Một luồng linh quang bị cuốn tới, ngưng lại trong tay đạo hữu."),
        e("Một tháng chuyên tâm", 2, T_STREAK, 30, 0, 600, "Chuỗi ngày đọc chạm mỗi mốc 30 ngày",
            "Một tháng chuyên tâm, đạo tâm vững như bàn thạch. Tông môn trưởng lão đích thân ban thưởng."),
        e("Nguyên tiêu đăng hội", 2, T_LUNAR, 15, 1, 600, "Đọc vào rằm tháng Giêng",
            "Rằm tháng Giêng, đèn hoa giăng khắp tiên thành. Một ngọn đăng trôi tới trước mặt, trong lòng đèn có vật quý."),
        e("Đoan ngọ dương khí", 2, T_LUNAR, 5, 5, 600, "Đọc vào Tết Đoan ngọ (5/5)",
            "Tết Đoan ngọ, dương khí giữa năm dâng cao. Đạo hữu luyện hóa một luồng, được vật trừ tà hộ thân."),
        e("Thất tịch tinh hà", 2, T_LUNAR, 7, 7, 600, "Đọc vào đêm Thất tịch (7/7)",
            "Đêm Thất tịch, tinh hà rực sáng, cầu Ô Thước nối đôi bờ. Một vì sao nhỏ rơi xuống, hóa thành cơ duyên."),
        e("Trùng cửu đăng cao", 2, T_LUNAR, 9, 9, 600, "Đọc vào Tết Trùng cửu (9/9)",
            "Tết Trùng cửu, đạo hữu leo núi ngắm trời. Trên đỉnh cao, gió đưa tới một vật cổ xưa."),
        e("Di tích khai môn", 2, T_DITICH, 0, 0, 600, "Mở một thượng cổ di tích",
            "Cửa đá di tích rền vang mở ra, bụi ngàn năm bay mù. Ngoài phần thưởng của di tích, còn một vật nằm khuất."),
        e("Đại bỉ dương danh", 2, T_DAIBI, 0, 0, 600, "Vào top ba tông môn đại bỉ",
            "Tên đạo hữu được khắc lên bảng vàng đại bỉ. Chưởng môn đích thân trao thêm một món làm kỷ niệm."),
        // --- Cực kỳ hiếm ---------------------------------------------------------------------------------
        e("Cổ động phủ hiện thế", 3, T_WORDS, 0, 0, 200, "Cùng mốc chữ ấy, một động phủ cổ mở cửa",
            "Đất trời rung chuyển, một tòa động phủ thượng cổ từ lòng núi trồi lên. Cấm chế đã tan, chỉ chờ người có duyên bước vào."),
        e("Tân xuân phúc lộc", 3, T_TET, 0, 0, 500, "Đọc vào ba ngày Tết",
            "Ngày xuân đầu năm, phúc khí đầy trời. Sư môn phát lộc đầu xuân, đạo hữu nhận phần lớn nhất."),
        e("Vu lan âm môn", 3, T_LUNAR, 15, 7, 500, "Đọc vào rằm tháng Bảy",
            "Rằm tháng Bảy, âm môn hé mở. Một vị tiền bối lạc lối được đạo hữu chỉ đường, trước khi đi để lại báu vật."),
        e("Trung thu nguyệt hoa", 3, T_LUNAR, 15, 8, 600, "Đọc vào Tết Trung thu",
            "Trung thu, nguyệt hoa tròn đầy nhất năm. Thỏ ngọc giã thuốc, một hạt tiên đan rơi trúng chén trà của đạo hữu."),
        e("Vạn quyển hữu thần", 3, T_FINISH, 3, 0, 700, "Đọc xong một cuốn từ 100 vạn chữ",
            "Một bộ trăm vạn chữ đã đọc tới trang cuối. Đọc vạn quyển, hạ bút như có thần; thiên địa không thể không ban thưởng."),
        e("Trăm ngày trúc cơ", 3, T_STREAK, 100, 0, 800, "Chuỗi ngày đọc chạm mỗi mốc 100 ngày",
            "Trăm ngày không đứt đoạn, nền đạo vững như núi Thái. Trời cao cảm động, giáng xuống cơ duyên ngàn năm hiếm gặp."),
        // --- Thiên cổ kỳ ngộ ------------------------------------------------------------------------------
        e("Thiên niên nhất ngộ", 4, T_STREAK, 365, 0, 1000, "Chuỗi ngày đọc tròn một năm",
            "Ba trăm sáu mươi lăm ngày không một ngày lìa sách. Tiên âm vang trên chín tầng mây, thiên cổ kỳ ngộ giáng xuống đúng người đáng nhận."),
        e("Bách quyển thành thánh", 4, T_FINISH, 4, 0, 1000, "Đọc trọn cuốn sách thứ 100",
            "Cuốn thứ một trăm khép lại, trăm quyển hội tụ trong thức hải thành một tòa thư lâu. Thánh nhân xưa cũng chỉ đến thế."),
        e("Thiên địa dị tượng", 4, T_BREAK, 4, 0, 1000, "Đột phá vào Hóa Thần trở lên",
            "Phá cảnh vào Hóa Thần, trời đất dị tượng: mây ngũ sắc phủ kín trăm dặm, chim muông cúi đầu. Thiên đạo ban một cơ duyên vạn năm có một."),
    )

    /** The order of the 36 of 2.2, to carry what was met. */
    val OLD_NAMES = listOf(
        "Linh khí hội tụ", "Cổ động phủ hiện thế", "Tĩnh tọa ngộ đạo", "Thần du thái hư", "Nhập định một canh", "Vong ngã chi cảnh",
        "Thiên nhân hợp nhất", "Cần tu bất đãi", "Khổ tu nhật nguyệt", "Quên ăn quên ngủ", "Tử khí đông lai", "Chính ngọ dương cương",
        "Hoàng hôn tà dương", "Nửa đêm giờ Tý", "Canh ba trăng lặn", "Sóc nhật linh triều", "Vọng nguyệt linh triều", "Nguyên tiêu đăng hội",
        "Tân xuân phúc lộc", "Đoan ngọ dương khí", "Thất tịch tinh hà", "Vu lan âm môn", "Trung thu nguyệt hoa", "Trùng cửu đăng cao",
        "Tiết khí giao thời", "Khai quyển hữu ích", "Bán quyển ngộ huyền", "Quyển chung đắc pháp", "Nhất khí a thành", "Bách gia chư tử",
        "Vạn quyển hữu thần", "Bảy ngày không dứt", "Một tháng chuyên tâm", "Trăm ngày trúc cơ", "Phá cảnh dư uy", "Bí cảnh thâm xứ",
    )

    /** The 72 from the easiest to meet to the rarest. */
    val ORDER: List<Int> = EVENTS.indices.sortedWith(compareBy({ EVENTS[it].rarity }, { -EVENTS[it].permille }))

    fun index(name: String) = EVENTS.indexOfFirst { it.name == name }

    /** The words to the next "Linh khí hội tụ": 10 to 50 vạn. */
    const val WORDS_MIN = 100_000L
    const val WORDS_MAX = 500_000L

    fun hourIn(h: Int, from: Int, to: Int) = if (from < to) h in from until to else h >= from || h < to
}
