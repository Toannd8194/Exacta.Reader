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
    override fun tabs() = listOf("Giao diện", "Đọc sách", "Tu tiên", "Thư viện", "Thông tin")

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
    private val fonts = listOf("Bitter" to "Bitter (kèm app)", "Original" to "Font gốc của sách", "serif" to "Có chân (máy)", "sans-serif" to "Không chân (máy)")
    private val fontSizes = listOf(0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.6, 1.8, 2.0, 2.3, 2.6)
    private val lines = listOf(1.2, 1.4, 1.6, 1.8, 2.0)
    private val margins = listOf(0.6 to "Hẹp", 1.0 to "Vừa", 1.5 to "Rộng")
    private val footers = listOf("Đầy đủ: chương · % · giờ", "Chỉ % và giờ", "Ẩn")
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
        1 -> listOf(
            Row("Chạm để lật trang", if (Store.swapTaps) "Bên trái lật tới, bên phải lật lui" else "Bên phải lật tới, bên trái lật lui") {
                Store.swapTaps = !Store.swapTaps
                Store.touch()
            },
            Row("Font chữ", fonts.firstOrNull { it.first == Store.fontFamily }?.second ?: Store.fontFamily) {
                choose("Font chữ", fonts.map { it.second }, fonts.indexOfFirst { it.first == Store.fontFamily }) { Store.fontFamily = fonts[it].first }
            },
            Row("Cỡ chữ", pct(Store.fontSize)) {
                choose("Cỡ chữ", fontSizes.map { pct(it) }, fontSizes.indexOfFirst { abs(it - Store.fontSize) < 0.05 }) { Store.fontSize = fontSizes[it] }
            },
            Row("Độ đậm chữ", BookCss.WEIGHT_NAMES[Store.textWeight.coerceIn(0, 4)]) {
                choose("Độ đậm chữ", BookCss.WEIGHT_NAMES, Store.textWeight) { Store.textWeight = it }
            },
            Row("Giãn dòng", num(Store.lineHeight)) {
                choose("Giãn dòng", lines.map { num(it) }, lines.indexOfFirst { abs(it - Store.lineHeight) < 0.05 }) { Store.lineHeight = lines[it] }
            },
            Row("Lề trang", margins.firstOrNull { abs(it.first - Store.margins) < 0.05 }?.second ?: num(Store.margins)) {
                choose("Lề trang", margins.map { it.second }, margins.indexOfFirst { abs(it.first - Store.margins) < 0.05 }) { Store.margins = margins[it].first }
            },
            Row("Căn chữ", if (Store.justify) "Đều hai bên" else "Căn trái") {
                Store.justify = !Store.justify
                Store.touch()
            },
            Row("Ngắt từ cuối dòng", onOff(Store.hyphens)) {
                Store.hyphens = !Store.hyphens
                Store.touch()
            },
            Row("Thụt đầu dòng", onOff(Store.indent) + " · thụt dòng đầu mỗi đoạn") {
                Store.indent = !Store.indent
                Store.touch()
            },
            Row("Tên sách trên đầu trang", if (Store.showHeader) "Hiện" else "Ẩn (vẫn chừa lề trên)") {
                Store.showHeader = !Store.showHeader
                Store.touch()
            },
            Row("Chân trang", footers[Store.footerMode.coerceIn(0, 2)]) {
                choose("Chân trang", footers, Store.footerMode) { Store.footerMode = it }
            },
            Row("Báo tin tu tiên khi đọc", onOff(Store.showNotices) + " · đột phá, pháp bảo, thiên kiếp ở chân trang") {
                Store.showNotices = !Store.showNotices
                Store.touch()
            },
        )
        2 -> {
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
        3 -> listOf(
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
        )
        else -> {
            val list = mutableListOf(
                Row("Exacta Reader ${BuildConfigInfo.VERSION}", "Đọc EPUB, tu tiên bằng chữ · cho máy đọc e-ink Android", null),
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
        if (tab == 4 && page == 0) {
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
                "• Cảnh giới: chữ đọc tuần tự quy ra tu vi; nhảy qua mục lục hay kéo thanh tiến độ không tính. 36 cấp từ Luyện Khí tới Tiên nhân nhất phẩm.\n\n" +
                    "• Thiên kiếp: mỗi đại cảnh giới chỉ mở sau khi đọc liền 3–7 ngày, mỗi ngày 30–60 phút. Lỡ một ngày ở giữa là thất bại, tu vi lui về gần cửa ải.\n\n" +
                    "• Bí cảnh tuần: thứ Hai đến Chủ nhật, đủ 5 ngày đọc từ 30 phút được một khoáng thạch Huyền phẩm trở lên.\n\n" +
                    "• Pháp bảo: lò luyện 3 ô, khoáng thạch cháy bằng chữ đọc ở bất kỳ cuốn nào; mỗi 10 vạn từ được tặng một khoáng thạch; đọc xong một cuốn luyện thành một pháp bảo theo độ dày sách.\n\n" +
                    "• Bộ pháp bảo: 9 bộ, mỗi bộ 4 loại; gom đủ bộ thì có hiệu quả mãi mãi.\n\n" +
                    "• Đạo hiệu: 36 đạo hiệu theo thói quen đọc, chọn một để mang.\n\n" +
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
