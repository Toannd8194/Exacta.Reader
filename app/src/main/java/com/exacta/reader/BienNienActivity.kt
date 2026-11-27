package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity

/**
 * Biên niên: how the app came to be. Tab one is its former life, Exacta.OS on the Xteink X4; tab two
 * the generations of Vạn Quyển Tiên Đồ, each named for a realm, from Sơ Nhập Đạo Môn to Phi Thăng.
 */
class BienNienActivity : TabActivity() {
    private class Era(val name: String, val title: String, val date: String, val text: String)

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
                else "Chuyển kiếp sang Android cho iReader Neo 3. Mỗi thế hệ mang tên một cảnh giới; mười một thế hệ, từ bản dựng thử tới bản chính thức."
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
            else if (tab == 1) k.realmIcon(c, i, r.left + pad, r.top + (cardH - ic) / 2, ic)
            else k.diamond(c, r.left + pad + ic / 2, r.centerY(), 12 * u)
            val tx = r.left + 2 * pad + ic
            val tw = r.right - pad - tx
            val dw = small.measureText(e.date) + 12 * u
            k.text(c, k.fit("${e.name} · ${e.title}", tw - dw, head), tx, r.top + pad, head)
            k.right(c, e.date, r.right - pad, r.top + pad + (k.lineH(head) - sl) / 2, small)
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
        private val PAST = listOf(
            Era("Khởi nguyên", "CrossPoint 2.0.0", "11/2026",
                "Tách firmware mã nguồn mở CrossPoint cho Xteink X4 Classic, gọt thành Exacta.OS: hệ điều hành đọc sách nhẹ, menu gọn, chạy nhanh."),
            Era("Linh Thú", "trò chơi đầu tiên", "11/2026",
                "Đọc sách nuôi linh thú: mỗi trang đọc là thức ăn, linh thú lớn dần theo chữ. Mầm mống của trò tu tiên."),
            Era("Beta 9", "Nhập đạo", "11/2026",
                "Linh thú hóa thành con đường tu tiên: cảnh giới theo chữ đọc, linh thạch, kỳ ngộ, bế quan, thiên phú và trang trí."),
            Era("Beta 11", "Trúc Cơ Vấn Đạo", "11/2026",
                "Thêm thiên kiếp chặn cửa đại cảnh giới, bí cảnh mỗi tuần, tầng công pháp theo sách, và hợp luyện pháp bảo."),
            Era("Omega.v1", "Vạn Pháp Quy Tông", "11/2026",
                "9 bộ pháp bảo, 36 đạo hiệu, Kinh Các, Niên kỷ, phong ấn trên màn hình ngủ, và sổ tay hướng dẫn PDF."),
            Era("Omega.v1.1", "Giao diện Home", "11/2026",
                "Chọn kiểu Home: Cổ điển, Thanh icon, Pháp bảo to A2; ngày âm lịch trên dòng trạng thái. Bản cuối trước khi chuyển kiếp."),
        )

        private val GENERATIONS = listOf(
            Era("0.1", "Sơ Nhập Đạo Môn", "27/11/2026",
                "Bản dựng thử trên iReader Neo 3: Kotlin và Readium mở được EPUB, ký APK để cài không cần máy tính."),
            Era("0.2", "Luyện Khí", "27/11/2026",
                "Đem trò tu tiên của Exacta.OS sang Android: Home kiểu A2, Thư viện quét thẻ nhớ, tu vi theo chữ đọc, lò luyện pháp bảo."),
            Era("0.3", "Trúc Cơ", "27/11/2026",
                "Hết lỗi mở sách văng app; giao diện theo mật độ thật của màn 300 ppi; có thiên kiếp, bí cảnh và đạo hiệu."),
            Era("0.4", "Kết Đan", "27/11/2026",
                "Mọi màn hình chia tab và trang, bỏ cuộn; 6 kiểu Home; bảng chỉnh chữ như bản OS; bộ pháp bảo; Niên kỷ; logo đầu tiên."),
            Era("0.5", "Nguyên Anh", "27/11/2026",
                "Cài đặt đọc dời vào trong sách; độ đậm chữ thật; thêm font riêng; chữ mặc định to hơn; làm sạch màn hình nhẹ hơn."),
            Era("0.6", "Hóa Thần", "27/11/2026",
                "Chương sách thật sự nhận CSS nên giãn dòng, lề, giãn đoạn chạy được; đếm chữ trung thực theo trang lật; đưa sách vào lò."),
            Era("0.7", "Luyện Hư", "27/11/2026",
                "Lò 6 ô, một cuốn luyện 3 chặng; vẽ lại khoáng thạch; cân lại Home và trình đọc; % sách, đồng hồ, giờ đọc còn lại."),
            Era("0.8", "Hợp Thể", "27/11/2026",
                "72 pháp bảo vẽ pixel, 18 bộ; kỳ ngộ mỗi vạn từ; kiếp phá lô; 72 đạo hiệu; đổi tên thành Vạn Quyển Tiên Đồ, logo mới."),
            Era("0.9", "Đại Thừa", "27/11/2026",
                "18 loại đan dược và phù chú: tăng tốc lò, giữ chuỗi ngày đọc, khai linh, tẩy tủy, phá cảnh; Túi đồ chia trang."),
            Era("0.10", "Độ Kiếp", "27/11/2026",
                "Linh thạch và Phường thị để mua khoáng thạch, đan phù; bế quan 30 – 120 phút, lò cháy ×1,5 và kỳ ngộ ×2."),
            Era("1.0", "Phi Thăng", "27/11/2026",
                "Bản chính thức. Hợp luyện ba pháp bảo dư thành khoáng thạch phẩm trên; rà soát và tối ưu cách vẽ; thêm Biên niên này."),
        )
    }
}
