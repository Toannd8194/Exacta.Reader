package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity

/**
 * Biên niên: how the app came to be. Tab one is its former life, Exacta.OS on the Xteink X4; tab two
 * the generations of Vạn Quyển Tiên Đồ, each with a name of its own, from Nhất Quyển Sơ Khai to Vạn Pháp Quy Nhất.
 */
class BienNienActivity : TabActivity() {
    private class Era(val name: String, val title: String, val text: String)

    override fun title() = "Biên Niên"
    override fun right() = "Bản ${BuildConfigInfo.VERSION}"
    override fun tabs() = listOf("Tiền kiếp · Exacta.OS", "Thế hệ · Tiên Đồ")

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int {
        val u = k.u
        val small = k.small
        val sl = k.lineH(small)
        val head = k.ui10Bold
        val list = if (tab == 0) PAST else GENERATIONS
        var top = area.top
        if (page == 0) {
            val intro = if (tab == 0) "Trước khi có app, trò tu tiên sống trong Exacta.OS: firmware nhẹ cho máy Xteink X4 Classic, tách từ CrossPoint 2.0.0, tự viết và tự nạp vào máy."
                else "Chuyển kiếp sang Android cho iReader Neo 3. Mỗi thế hệ một danh hiệu riêng, từ bản dựng thử tới bản chính thức và những lần tu bổ sau đó."
            for (l in k.wrap(intro, area.width(), small, 3)) {
                k.text(c, l, area.left, top, small)
                top += sl
            }
            top += 14 * u
        }
        val lines = 3
        val pad = 14 * u
        val cardH = 2 * pad + k.lineH(head) + 4 * u + lines * sl
        val gap = 10 * u
        // Page 0 holds fewer cards (the introduction); later pages hold the full count.
        val perFirst = ((area.bottom - top + gap) / (cardH + gap)).toInt().coerceAtLeast(1)
        val per = ((area.height() + gap) / (cardH + gap)).toInt().coerceAtLeast(1)
        val from = if (page == 0) 0 else perFirst + (page - 1) * per
        val count = if (page == 0) perFirst else per
        var y = top
        for ((j, e) in list.drop(from).take(count).withIndex()) {
            val i = from + j
            val r = RectF(area.left, y, area.right, y + cardH)
            val now = tab == 1 && i == list.lastIndex
            k.card(c, r, selected = now)
            val ic = 56 * u
            if (now) k.logo(c, r.left + pad, r.top + (cardH - ic) / 2, ic)
            else if (tab == 1) {
                // The version number in a seal.
                val box = RectF(r.left + pad, r.centerY() - ic / 2, r.left + pad + ic, r.centerY() + ic / 2)
                k.card(c, box)
                k.centered(c, e.name, box.left + 2 * u, box.width() - 4 * u, box.centerY() - k.lineH(k.ui10Bold) / 2, k.ui10Bold)
            } else k.diamond(c, r.left + pad + ic / 2, r.centerY(), 12 * u)
            val tx = r.left + 2 * pad + ic
            val tw = r.right - pad - tx
            val heading = if (tab == 1) e.title else "${e.name} · ${e.title}"
            k.text(c, k.fit(heading, tw, head), tx, r.top + pad, head)
            var ty = r.top + pad + k.lineH(head) + 4 * u
            for (l in k.wrap(e.text, tw, small, lines)) {
                k.text(c, l, tx, ty, small)
                ty += sl
            }
            y += cardH + gap
        }
        val rest = (list.size - perFirst).coerceAtLeast(0)
        return 1 + (rest + per - 1) / per
    }

    companion object {
        /** The newest generation's name, for Settings. */
        val CURRENT get() = GENERATIONS.last().title
        val COUNT get() = GENERATIONS.size

        private val PAST = listOf(
            Era("Khởi nguyên", "CrossPoint 2.0.0",
                "Tách firmware mã nguồn mở CrossPoint cho Xteink X4 Classic, gọt thành Exacta.OS: hệ điều hành đọc sách nhẹ, menu gọn, chạy nhanh."),
            Era("Linh Thú", "trò chơi đầu tiên",
                "Đọc sách nuôi linh thú: mỗi trang đọc là thức ăn, linh thú lớn dần theo chữ. Mầm mống của trò tu tiên."),
            Era("Beta 9", "Nhập đạo",
                "Linh thú hóa thành con đường tu tiên: cảnh giới theo chữ đọc, linh thạch, kỳ ngộ, bế quan, thiên phú và trang trí."),
            Era("Beta 11", "Trúc Cơ Vấn Đạo",
                "Thêm thiên kiếp chặn cửa đại cảnh giới, bí cảnh mỗi tuần, tầng công pháp theo sách, và hợp luyện pháp bảo."),
            Era("Omega.v1", "Vạn Pháp Quy Tông",
                "9 bộ pháp bảo, 36 đạo hiệu, Kinh Các, Niên kỷ, phong ấn trên màn hình ngủ, và sổ tay hướng dẫn PDF."),
            Era("Omega.v1.1", "Giao diện Home",
                "Chọn kiểu Home: Cổ điển, Thanh icon, Pháp bảo to A2; ngày âm lịch trên dòng trạng thái. Bản cuối trước khi chuyển kiếp."),
        )

        private val GENERATIONS = listOf(
            Era("0.1", "Nhất Quyển Sơ Khai",
                "Bản dựng thử trên iReader Neo 3: Kotlin và Readium mở được EPUB, ký APK để cài không cần máy tính."),
            Era("0.2", "Chuyển Thế Trùng Sinh",
                "Đem trò tu tiên của Exacta.OS sang Android: Home kiểu A2, Thư viện quét thẻ nhớ, tu vi theo chữ đọc, lò luyện pháp bảo."),
            Era("0.3", "Minh Kính Thông Thấu",
                "Hết lỗi mở sách văng app; giao diện theo mật độ thật của màn 300 ppi; có thiên kiếp, bí cảnh và đạo hiệu."),
            Era("0.4", "Lục Diện Thiên Môn",
                "Mọi màn hình chia tab và trang, bỏ cuộn; 6 kiểu Home; bảng chỉnh chữ như bản OS; bộ pháp bảo; Niên kỷ; logo đầu tiên."),
            Era("0.5", "Mặc Bút Sinh Hoa",
                "Cài đặt đọc dời vào trong sách; độ đậm chữ thật; thêm font riêng; chữ mặc định to hơn; làm sạch màn hình nhẹ hơn."),
            Era("0.6", "Chân Ngôn Bất Hư",
                "Chương sách thật sự nhận CSS nên giãn dòng, lề, giãn đoạn chạy được; đếm chữ trung thực theo trang lật; đưa sách vào lò."),
            Era("0.7", "Lục Lô Tề Hỏa",
                "Lò 6 ô, một cuốn luyện 3 chặng; vẽ lại khoáng thạch; cân lại Home và trình đọc; % sách, đồng hồ, giờ đọc còn lại."),
            Era("0.8", "Vạn Bảo Xuất Thế",
                "72 pháp bảo vẽ pixel, 18 bộ; kỳ ngộ mỗi vạn từ; kiếp phá lô; 72 đạo hiệu; đổi tên thành Vạn Quyển Tiên Đồ, logo mới."),
            Era("0.9", "Đan Hương Phù Ảnh",
                "18 loại đan dược và phù chú: tăng tốc lò, giữ chuỗi ngày đọc, khai linh, tẩy tủy, phá cảnh; Túi đồ chia trang."),
            Era("0.10", "Bế Quan Tụ Linh",
                "Linh thạch và Phường thị để mua khoáng thạch, đan phù; bế quan 30 – 120 phút, lò cháy ×1,5 và kỳ ngộ ×2."),
            Era("1.0", "Vạn Pháp Quy Nhất",
                "Bản chính thức. Hợp luyện ba pháp bảo dư thành khoáng thạch phẩm trên; rà soát và tối ưu cách vẽ; thêm Biên niên này."),
            Era("1.0.1", "Khắc Lậu Cổ Pháp",
                "Đồng hồ theo canh giờ và khắc, thời gian còn lại tính bằng canh giờ; Mục lục trọn trang, mười mục một trang, chọn theo khoảng; tùy chọn dạng thẻ."),
            Era("2.0", "Ngũ Khí Triều Nguyên",
                "Người tu có năm thước đo và tâm ma; mỗi cảnh giới bốn tiểu cảnh, bình cảnh, áp chế; đột phá tự bấm, tỉ lệ hiện rõ; Trúc Cơ quan, ngưng đan, lôi kiếp kèm tâm ma kiếp, ngộ đạo."),
            Era("2.1", "Vạn Pháp Quy Nguyên",
                "Linh căn thức tỉnh từ giờ đọc; công pháp chín tầng mở bằng ngộ tính, cửa ải đòi phẩm công pháp; tâm pháp, thuật pháp tự ra tay; Tàng Kinh Các; vẽ lại khoáng thạch, cảnh giới, khung Âm Dương."),
            Era("2.2", "Động Phủ Khai Lô",
                "Động phủ và linh mạch; dược viên trồng linh thảo theo ngày đọc; đan phòng luyện đan bằng hỏa hầu là một buổi đọc liền, đan độc; 36 kỳ ngộ theo lúc đọc, báo bằng khung nổi; Thống kê vào Niên kỷ; chữ đậm hơn."),
            Era("2.2.1", "Kim Khung Ngọc Khuyết",
                "Thẻ sách và thẻ tu vi có khung riêng: đai mực chạy theo % đọc và % cảnh giới, hoa văn góc lớn dần theo cảnh giới; thẻ trong game đẹp dần theo cấp; hết chữ bị cắt trên Home; nút Thư viện vuông; còn bốn kiểu Home."),
            Era("2.3", "Bái Nhập Sơn Môn",
                "Năm tông môn, nhiệm vụ, đối thủ, đại bỉ; bí cảnh thành bản đồ biến cố; 72 kỳ ngộ theo độ hiếm; tiểu đột phá; cảnh giới gọi theo kỳ."),
            Era("2.3.1", "Lục Giới Phân Đồ",
                "Phi Thăng thành cảnh giới, Độ Kiếp là chặng đặc biệt; sáu giới, sáu loại tiên nhân; Home gọn lại, đạo hiệu lên thanh trên, tiết khí kèm lời bình."),
            Era("2.4", "Ngũ Lộ Đồng Quy",
                "Năm tiên lộ; mười hai Kim Đan, Nguyên Anh kế thừa đan; sáu Hóa Thần đè lên Hợp Thể, Độ Kiếp; luyện thể, luyện thần, kiêm tu, Song Anh; Lục Giới do đời tu và nghiệp lực quyết định."),
            Era("2.5", "Phá Toái Hư Không",
                "Phi thăng Linh giới: tiết điểm, trảm trần duyên, phi thăng kiếp; bản mệnh, di bảo; tám pháp tắc, đạo vực; 36 tông môn; độ đậm theo %; cửa chưa tới thì khóa."),
            Era("2.6", "Cửu Cửu Quy Nhất",
                "Độ Kiếp chín tầng, kiếp lực, kiếp ấn; thiên nhân ngũ suy; thọ nguyên, chế độ Chân Tu; tĩnh thất sáu đẳng; 32 đan phương; gieo hạt bằng thẻ."),
            Era("2.7", "Tiên Môn Tiếp Dẫn",
                "Qua Lục Giới Môn: mang theo bản mệnh, công pháp, linh căn, kiếp ấn; tiên hiệu theo giới; màn kết phần một; tiền đình ghi mọi chữ đọc chờ phần hai."),
            Era("2.7.1", "Tế Luyện Bố Trận",
                "Tế luyện 36 tầng cấm chế: pháp khí, pháp bảo, linh bảo, thông thiên linh bảo; bảy trận pháp trong động phủ, trận nhãn theo linh mạch; linh thạch vẽ lại."),
            Era("2.7.2", "Hộ Thân Họa Phù",
                "Ô trang bị từ Kim Đan, chỉ món đeo mới dùng được trong biến cố và kiếp; phù thất: vẽ chín loại phù bằng phù chỉ, chu sa và một buổi đọc liền."),
            Era("2.8", "Tàng Kinh Lưu Ảnh",
                "Sổ đọc theo ngày cho phần hai: giờ đọc, từng buổi, chữ đọc lại, sách quay lại, thể loại và ngôn ngữ sách; Niên kỷ › Lối đọc; tiền đình hiện tài nguyên dự tính."),
            Era("3.0", "Cửu Tiêu Khai Thiên",
                "Phần hai mở: Cửu Tiêu Vân Khuyết, chín bậc tiên vị theo chữ và tiên lực; thiên quy tuần, tiên chức, Tiên phủ ba gian, ba thế lực, ba tiên kiếp; linh thạch vẽ lại."),
            Era("3.0.1", "Sơn Môn Định Phận",
                "36 tông môn mở cửa dần theo cảnh giới; chức vụ cần ở tông đủ lâu; bổng linh thạch mỗi ngày đọc theo chức và cống hiến; phản tông bị phạt."),
            Era("3.1", "Vạn Pháp Trùng Tu",
                "72 công pháp trong 12 mạch; ba đặc hiệu mở ở tầng 1, 5, 9 và khuyết điểm; lối vận công theo cách đọc; xung quan; chuyển tu cùng mạch; phụ tu; khoáng thạch vẽ lại."),
            Era("3.2", "Tâm Đăng Bất Diệt",
                "18 tâm pháp trong bốn phái; năm cảnh từ Nhập môn tới Hóa cảnh; vận chuyển theo lối đọc riêng; Ma đạo xung khắc Phật, Nho; ba ô tâm pháp; khoáng thạch và linh thạch vẽ lại."),
            Era("3.2.1", "Bát Quái Khai Trận",
                "Khoáng thạch vẽ lại thành pixel 32 ô có màu: đá góc cạnh, tinh thiết bám đất, hồng ngọc ẩn trong đá, thiên thạch lửa, Âm Dương có khí tức, Hỗn Độn nửa thật nửa ảo; bế quan tới 90 phút, tiềm tu 120; 18 trận pháp."),
            Era("3.3", "Thần Thông Quảng Đại",
                "36 thuật pháp sáu loại, thêm khống chế; thi triển trong bí cảnh, kiếp, đột phá hỏng; hồi chiêu tính bằng ngày đọc; thuần thục theo lần dùng và buổi đọc; tám hợp kích; mười hai bản mệnh thần thông."),
            Era("3.4", "Tàng Kinh Cửu Lâu",
                "Tàng Kinh chín tầng lầu, kinh các tông môn mở theo cống hiến và chức vụ; án tham ngộ: đọc đủ chữ mới học được ngọc giản; tàn quyển Linh, Tiên phẩm; phản tông thì phế công; đồ phổ; tự sáng công pháp."),
            Era("3.5", "Huyết Nguyệt Đương Không",
                "Huyết Nguyệt Trầm Uyên mở: ma khí từ đọc đêm và tâm ma, ma công đặt cược lời thề, phản phệ, Ma Cung ba gian, ba thế lực, Tâm ma đại kiếp; logo mới tối giản."),
            Era("3.6", "Cổ Vận Thanh Phong",
                "Câu chữ cổ phong; kỳ ngộ lục hai cột, giấu duyên khởi; Đạo lộ thành Tiên lộ; pháp bảo và icon app vẽ pixel; trình đọc tính lề, giãn dòng, giãn đoạn, thụt đầu dòng bằng em; Home hàng nút cân lại."),
            Era("3.7", "Liên Đài Hóa Độ",
                "Bát Nhã Liên Đài mở cửa: công đức thay tu vi, xá lợi giữ chuỗi và đỡ kiếp, độ hóa đồng đạo lạc trong sách bỏ dở; Thiền Viện có Kinh lâu, Xá lợi tháp, Liên trì; Vô niệm kiếp và Đại kiếp Liên Hoa."),
            Era("4.0", "Hỗn Nguyên Nhất Khí",
                "Hồng Hoang, Vong Xuyên, Thư Hải mở cửa: khí huyết và huyết mạch, hồn lực và sổ sinh tử, văn khí và thần quốc; sứ giả năm giới, hỗn nguyên khí, Quy Nhất."),
            Era("4.1", "Lô Hỏa Thuần Thanh",
                "Pháp bảo luyện qua sáu giai đoạn, từ phôi liệu tới thành hình; khai lô theo phẩm: nhiệm vụ đọc, điều kiện hoặc tụ linh, kiếp phá lô; Home cân lại ô pháp bảo."),
        )
    }
}
