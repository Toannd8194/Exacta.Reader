package com.exacta.reader

import android.graphics.Canvas
import android.graphics.RectF
import android.os.Environment
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.exacta.reader.data.BookCss
import com.exacta.reader.data.Library
import com.exacta.reader.data.Store
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Cài đặt, in five tabs: the look, reading, the cultivation, the library, and about the app (with
 * backup and restore). Each row shows its value; tapping it offers the choices.
 */
class SettingsActivity : TabActivity() {
    override fun title() = "Cài đặt"
    override fun tabs() = listOf("Giao diện", "Tu tiên", "Thư viện", "Thông tin")

    private class Row(val title: String, val value: String, val action: (() -> Unit)?)

    /** A list of choices in a dialog; `onPick` gets the index. */
    private fun choose(title: String, items: List<String>, current: Int, onPick: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(items.toTypedArray(), current) { d, which ->
                onPick(which)
                Store.touch()
                d.dismiss()
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    private fun onOff(v: Boolean) = if (v) "Bật" else "Tắt"

    private val sizes = listOf(0.85 to "Nhỏ", 1.0 to "Vừa", 1.15 to "Lớn", 1.3 to "Rất lớn")
    private val goals = listOf(0, 15, 30, 45, 60, 90, 120)
    private val idles = listOf(2, 3, 5, 10)
    private val sorts = listOf("Đọc gần đây", "Tên sách", "Tác giả")

    private fun pct(v: Double) = "${(v * 100 + 0.5).toInt()}%"
    private fun num(v: Double) = String.format(Locale.US, "%.1f", v).replace('.', ',')

    private fun rows(tab: Int): List<Row> = when (tab) {
        0 -> {
            val at = sizes.indexOfFirst { abs(it.first - Store.uiScale) < 0.01 }.coerceAtLeast(0)
            val style = Store.homeStyle.coerceIn(0, HomeView.STYLES.size - 1)
            listOf(
                Row("Kiểu Home", HomeView.STYLES[style] + " · " + HomeView.STYLE_NOTES[style]) {
                    choose("Kiểu Home", HomeView.STYLES.mapIndexed { i, s -> "$s\n${HomeView.STYLE_NOTES[i]}" }, style) { Store.homeStyle = it }
                },
                Row("Cài đặt đọc sách", "Font, cỡ chữ, độ đậm, giãn dòng, lề, giãn đoạn: chạm giữa trang khi đang đọc", null),
                Row("Cỡ giao diện", sizes[at].second + " · chữ và khung của mọi màn, không đổi chữ trong sách") {
                    choose("Cỡ giao diện", sizes.map { it.second }, at) {
                        Store.uiScale = sizes[it].first
                        Store.save()
                        recreate()
                    }
                },
                Row("Câu nói trên Home", onOff(Store.showProverb) + " · một câu tục ngữ đổi mỗi lần về Home") {
                    Store.showProverb = !Store.showProverb
                    Store.touch()
                },
            )
        }
        1 -> {
            val birth = if (Store.birthDay > 0) "${Store.birthDay}/${Store.birthMonth}" else "Chưa đặt"
            listOf(
                Row("Mục tiêu đọc mỗi ngày", if (Store.dailyGoal > 0) "${Store.dailyGoal} phút · Home hiện số phút còn thiếu" else "Không đặt") {
                    choose("Mục tiêu đọc mỗi ngày", goals.map { if (it == 0) "Không đặt" else "$it phút" }, goals.indexOf(Store.dailyGoal)) { Store.dailyGoal = goals[it] }
                },
                Row("Ngừng tính giờ đọc sau", "${Store.idleMinutes} phút không chạm màn hình") {
                    choose("Ngừng tính giờ đọc sau", idles.map { "$it phút không chạm" }, idles.indexOf(Store.idleMinutes)) { Store.idleMinutes = idles[it] }
                },
                Row("Ngày sinh nhật", "$birth · cho đạo hiệu Sinh Thần Đọc Sách") { pickBirthday() },
                Row("Luật tu tiên", "Cảnh giới, thiên kiếp, bí cảnh, pháp bảo, đạo hiệu") { showRules() },
            )
        }
        2 -> listOf(
            Row("Quét lại thẻ nhớ", "Tìm sách EPUB mới (5 tầng thư mục, bỏ qua Android/)") { rescan() },
            Row("Xếp tab Tất cả theo", sorts[Store.librarySort.coerceIn(0, 2)]) {
                choose("Xếp tab Tất cả theo", sorts, Store.librarySort) { Store.librarySort = it }
            },
            Row("Làm mới bìa sách", "Xóa ảnh bìa đã lưu, lấy lại từ sách khi mở Thư viện") {
                File(filesDir, "covers").deleteRecursively()
                Store.books.values.forEach { it.cover = "" }
                Store.touch()
                toast("Đã xóa ảnh bìa; Thư viện sẽ lấy lại")
            },
            Row("Đếm lại số chữ", "Đếm lại độ dài mọi cuốn khi mở Thư viện (không đổi tu vi)") {
                Store.books.values.forEach { it.words = -1 }
                Store.touch()
                toast("Thư viện sẽ đếm lại")
            },
            BookCss.userFonts().let { fonts ->
                Row("Font đã thêm · ${fonts.size}", if (fonts.isEmpty()) "Chép tệp TTF/OTF vào ExactaReader/fonts, hoặc thêm khi đọc (tab Chữ)" else fonts.joinToString(" · ") { it.family } + " · chạm để gỡ") {
                    if (fonts.isNotEmpty()) choose("Gỡ font nào?", fonts.map { it.family }, -1) { i -> BookCss.removeFamily(fonts[i].family) }
                }
            },
        )
        else -> {
            val list = mutableListOf(
                Row("Vạn Quyển Tiên Đồ ${BuildConfigInfo.VERSION} · ${BienNienActivity.CURRENT}", "Đọc vạn quyển, tu thành tiên đồ · máy đọc e-ink Android", null),
                Row("Biên niên phát triển", "Tiền kiếp Exacta.OS trên Xteink X4 · ${BienNienActivity.COUNT} thế hệ, từ Nhất Quyển Sơ Khai tới ${BienNienActivity.CURRENT}") { startActivity(android.content.Intent(this, BienNienActivity::class.java)) },
                Row("Sao lưu dữ liệu", "Chép tiến độ, tu vi, pháp bảo, đạo hiệu vào ExactaReader/backup") { backup() },
                Row("Khôi phục từ bản sao lưu", "Thay toàn bộ dữ liệu bằng một bản sao lưu") { restore() },
                Row("Mã nguồn mở dùng trong app", "Readium Kotlin Toolkit (BSD-3) · font Bitter (SIL OFL 1.1)", null),
                Row("Nhật ký lỗi", "ExactaReader/exacta-log.txt trên bộ nhớ máy", null),
            )
            val crash = File(filesDir, ExactaApp.LAST_CRASH)
            if (crash.exists()) {
                val lines = crash.readLines()
                val cause = lines.drop(1).firstOrNull { it.isNotBlank() } ?: ""
                list.add(Row("Lỗi gần nhất · ${lines.firstOrNull() ?: ""}", "$cause · chạm để xóa") { crash.delete() })
            }
            list
        }
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int {
        val u = k.u
        val rows = rows(tab)
        var top = area.top
        if (tab == 3 && page == 0) {
            // The logo heads the about tab.
            val s = 96 * u
            k.logo(c, area.centerX() - s / 2, top, s)
            top += s + 20 * u
        }
        val rowH = 84 * u
        val gap = 10 * u
        val per = ((area.bottom - top + gap) / (rowH + gap)).toInt().coerceAtLeast(1)
        var y = top
        for (row in rows.drop(page * per).take(per)) {
            val r = RectF(area.left, y, area.right, y + rowH)
            k.card(c, r)
            val pad = 20 * u
            val tw = r.width() - 2 * pad - if (row.action != null) 30 * u else 0f
            k.text(c, k.fit(row.title, tw, k.ui10Bold), r.left + pad, r.top + 14 * u, k.ui10Bold)
            val vy = r.top + 18 * u + k.lineH(k.ui10Bold)
            k.text(c, k.fit(row.value, tw, k.small), r.left + pad, vy, k.small)
            if (row.action != null) {
                k.right(c, "›", r.right - pad, r.centerY() - k.lineH(k.big) / 2, k.big)
                hits.add(r) {
                    row.action.invoke()
                    refresh()
                }
            }
            y += rowH + gap
        }
        return (rows.size + per - 1) / per
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun rescan() {
        lifecycleScope.launch {
            val files = Library.scan()
            files.forEach { Store.book(it.absolutePath) }
            Store.touch()
            Store.save()
            toast("Tìm thấy ${files.size} cuốn EPUB")
        }
    }

    private fun backupDir() = File(Environment.getExternalStorageDirectory(), "ExactaReader/backup")

    private fun backup() {
        try {
            val name = "exacta-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"
            Store.backup(File(backupDir(), name))
            toast("Đã sao lưu: ExactaReader/backup/$name")
        } catch (e: Exception) {
            ExactaApp.log("backup: $e")
            toast("Không sao lưu được (cần quyền đọc thẻ nhớ)")
        }
    }

    private fun restore() {
        val files = backupDir().listFiles { f -> f.name.endsWith(".json") }?.sortedByDescending { it.name } ?: emptyList()
        if (files.isEmpty()) {
            toast("Chưa có bản sao lưu trong ExactaReader/backup")
            return
        }
        choose("Khôi phục bản nào?", files.map { it.name }, -1) { i ->
            AlertDialog.Builder(this)
                .setTitle("Khôi phục ${files[i].name}?")
                .setMessage("Dữ liệu hiện tại (tiến độ, tu vi, pháp bảo, đạo hiệu) sẽ bị thay bằng bản này. Nên sao lưu trước.")
                .setPositiveButton("Khôi phục") { _, _ ->
                    toast(if (Store.restore(files[i])) "Đã khôi phục" else "Không đọc được bản sao lưu")
                    refresh()
                }
                .setNegativeButton("Thôi", null)
                .show()
        }
    }

    private fun showRules() {
        AlertDialog.Builder(this)
            .setTitle("Luật tu tiên")
            .setMessage(
                "• Cảnh giới: chữ đọc tuần tự (lật trang) quy ra tu vi. Luyện Khí 9 tầng; từ Trúc Cơ mỗi cảnh giới có sơ, trung, hậu kỳ, viên mãn (cần căn cơ 30, 45, 60). Tới đỉnh là bình cảnh: chữ đọc dồn thành áp chế.\n\n" +
                    "• Đột phá: tự bấm ở Tu vi › Đột phá, tỉ lệ hiện từng phần. Cửa ải Nhân giới đòi căn cơ, ngưng đan, lôi kiếp kèm tâm ma kiếp, ngộ đạo; từ Kim Đan đòi công pháp đủ phẩm. Hỏng thì bị thương, không rớt cảnh giới.\n\n" +
                    "• Năm thước đo (Tu vi › Thân): linh lực, căn cơ, tâm cảnh, thần thức, thể phách; tâm ma tăng khi đứt chuỗi, nhảy chương, thất bại.\n\n" +
                    "• Linh căn (Công pháp): thức tỉnh ở Luyện Khí tầng 3 theo giờ hay đọc. Công pháp chín tầng luyện bằng chữ, tầng 4, 7, 9 mở bằng ngộ tính (từ tầng sách, sách đọc trọn). Tâm pháp luyện bằng buổi đọc 30 phút; thuật pháp tự ra tay khi có biến cố.\n\n" +
                    "• Bí cảnh tuần: 5 ngày đọc từ 30 phút trong tuần được khoáng thạch Huyền phẩm trở lên.\n\n" +
                    "• Lò luyện 6 ô: khoáng thạch cháy bằng chữ đọc; một cuốn sách luyện tối đa 3 chặng; Thiên phẩm trở lên phải qua kiếp phá lô.\n\n" +
                    "• Pháp bảo: 72 loại, 18 bộ bốn món, đủ bộ có hiệu quả mãi mãi. Hợp luyện: 3 món dư cùng phẩm thành 1 khoáng thạch phẩm trên.\n\n" +
                    "• Kỳ ngộ mỗi vạn từ: khoáng thạch, linh thạch, 18 loại đan dược và phù chú, khí linh thức tỉnh.\n\n" +
                    "• Linh thạch: mỗi vạn từ +5, thưởng bế quan, bí cảnh, vượt kiếp; tiêu ở Phường thị. Bế quan 30–120 phút: lò ×1,5, kỳ ngộ ×2.\n\n" +
                    "• Đạo hiệu: 72 đạo hiệu theo thói quen đọc, chọn một để mang.\n\n" +
                    "• Niên kỷ: tổng kết mỗi năm âm lịch, mở vào dịp Tết."
            )
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun pickBirthday() {
        val a = this
        val day = NumberPicker(a).apply { minValue = 1; maxValue = 31; value = Store.birthDay.coerceIn(1, 31) }
        val month = NumberPicker(a).apply { minValue = 1; maxValue = 12; value = Store.birthMonth.coerceIn(1, 12) }
        val row = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(Widgets.text(a, "Ngày", 15f))
            addView(day)
            addView(Widgets.text(a, "  Tháng", 15f))
            addView(month)
        }
        AlertDialog.Builder(a)
            .setTitle("Ngày sinh nhật")
            .setView(row)
            .setPositiveButton("Lưu") { _, _ ->
                Store.birthDay = day.value
                Store.birthMonth = month.value
                Store.touch()
                refresh()
            }
            .setNeutralButton("Bỏ") { _, _ ->
                Store.birthDay = 0
                Store.birthMonth = 0
                Store.touch()
                refresh()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }
}
