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

        section("GIỚI THIỆU")
        note("Exacta Reader ${BuildConfigInfo.VERSION} · đọc EPUB, tu tiên bằng chữ.")
        note("Lõi đọc: Readium Kotlin Toolkit (BSD-3). Font Bitter (SIL OFL 1.1).")
        note("Nhật ký lỗi: ExactaReader/exacta-log.txt trên bộ nhớ máy.")
    }

    private fun section(title: String) {
        list.addView(Widgets.text(this, title, 14f, bold = true).apply { setPadding(0, dp(this@SettingsActivity, 18), 0, dp(this@SettingsActivity, 4)) })
        list.addView(Widgets.rule(this), LinearLayout.LayoutParams(-1, dp(this, 1)))
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
