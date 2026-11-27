package com.exacta.reader

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.exacta.reader.Widgets.dp
import com.exacta.reader.data.Library
import com.exacta.reader.data.Store
import kotlinx.coroutines.launch

/** Cài đặt: the few things worth a setting; the rest lives in the reader's bar. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val a = this
        list = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 16), dp(a, 8), dp(a, 16), dp(a, 16))
        }
        setContentView(LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(Widgets.header(a, "Cài đặt"))
            addView(Widgets.rule(a), LinearLayout.LayoutParams(-1, dp(a, 1)))
            addView(ScrollView(a).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        })
        build()
    }

    override fun onPause() {
        Store.save()
        super.onPause()
    }

    private fun build() {
        val a = this
        list.removeAllViews()
        section("GIAO DIỆN")
        val sizes = listOf(0.85 to "Nhỏ", 1.0 to "Vừa", 1.15 to "Lớn", 1.3 to "Rất lớn")
        val at = sizes.indexOfFirst { kotlin.math.abs(it.first - Store.uiScale) < 0.01 }.coerceAtLeast(0)
        toggle("Cỡ giao diện", sizes[at].second + " · chạm để đổi") {
            Store.uiScale = sizes[(at + 1) % sizes.size].first
            Store.save()
            recreate()
        }
        note("Cỡ chữ trong sách chỉnh riêng ở bảng chỉnh khi đọc.")

        section("ĐỌC SÁCH")
        toggle("Chạm bên phải để lật trang tới", if (Store.swapTaps) "Đang đảo: bên trái lật tới" else "Bên phải lật tới, bên trái lật lui") {
            Store.swapTaps = !Store.swapTaps
        }
        toggle("Căn chữ", if (Store.justify) "Đều hai bên" else "Căn trái") { Store.justify = !Store.justify }
        note("Cỡ chữ, font, giãn dòng, lề: chạm giữa trang khi đang đọc.")

        section("THƯ VIỆN")
        list.addView(Widgets.button(a, "Quét lại thẻ nhớ") {
            lifecycleScope.launch {
                val files = Library.scan()
                files.forEach { Store.book(it.absolutePath) }
                Store.touch()
                Store.save()
                Toast.makeText(a, "Tìm thấy ${files.size} cuốn EPUB", Toast.LENGTH_SHORT).show()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(a, 8) })

        section("ĐẠO HIỆU")
        val birth = if (Store.birthDay > 0) "${Store.birthDay}/${Store.birthMonth}" else "Chưa đặt"
        toggle("Ngày sinh nhật", "$birth · cho đạo hiệu Sinh Thần Đọc Sách") { pickBirthday() }

        section("GIỚI THIỆU")
        note("Exacta Reader ${BuildConfigInfo.VERSION} · đọc EPUB, tu tiên bằng chữ.")
        note("Lõi đọc: Readium Kotlin Toolkit (BSD-3). Font Bitter (SIL OFL 1.1).")
        note("Nhật ký lỗi: ExactaReader/exacta-log.txt trên bộ nhớ máy.")
        val crash = java.io.File(filesDir, ExactaApp.LAST_CRASH)
        if (crash.exists()) {
            val lines = crash.readLines()
            val cause = lines.drop(1).firstOrNull { it.isNotBlank() } ?: ""
            val where = lines.firstOrNull { it.contains("com.exacta") } ?: ""
            note("Lỗi gần nhất (${lines.firstOrNull() ?: ""}): $cause ${where.trim()}")
            list.addView(Widgets.button(a, "Xóa ghi lỗi") {
                crash.delete()
                build()
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(a, 8) })
        }
    }

    private fun pickBirthday() {
        val a = this
        val day = android.widget.NumberPicker(a).apply { minValue = 1; maxValue = 31; value = Store.birthDay.coerceIn(1, 31) }
        val month = android.widget.NumberPicker(a).apply { minValue = 1; maxValue = 12; value = Store.birthMonth.coerceIn(1, 12) }
        val row = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(Widgets.text(a, "Ngày", 15f))
            addView(day)
            addView(Widgets.text(a, "  Tháng", 15f))
            addView(month)
        }
        androidx.appcompat.app.AlertDialog.Builder(a)
            .setTitle("Ngày sinh nhật")
            .setView(row)
            .setPositiveButton("Lưu") { _, _ ->
                Store.birthDay = day.value
                Store.birthMonth = month.value
                Store.touch()
                build()
            }
            .setNeutralButton("Bỏ") { _, _ ->
                Store.birthDay = 0
                Store.birthMonth = 0
                Store.touch()
                build()
            }
            .setNegativeButton("Thôi", null)
            .show()
    }

    private fun section(title: String) {
        list.addView(Widgets.text(this, title, 14f, bold = true).apply { setPadding(0, dp(this@SettingsActivity, 18), 0, dp(this@SettingsActivity, 4)) })
        list.addView(Widgets.rule(this), LinearLayout.LayoutParams(-1, dp(this, 1).coerceAtLeast(1)))
    }

    private fun toggle(title: String, value: String, flip: () -> Unit) {
        val a = this
        list.addView(LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(a, 12), 0, dp(a, 12))
            addView(Widgets.text(a, title, 17f, bold = true))
            addView(Widgets.text(a, "$value  ›", 15f))
            setOnClickListener {
                flip()
                Store.touch()
                build()
            }
        })
    }

    private fun note(text: String) {
        list.addView(Widgets.text(this, text, 15f).apply { setPadding(0, dp(this@SettingsActivity, 8), 0, dp(this@SettingsActivity, 4)) })
    }
}
