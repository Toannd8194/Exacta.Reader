package com.exacta.reader

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/** Plain e-ink widgets for the list screens: black on white, Bitter, outlined buttons. */
object Widgets {
    fun dp(a: Activity, v: Int) = (v * a.resources.displayMetrics.density).toInt()

    fun text(a: Activity, s: String, sp: Float, bold: Boolean = false, color: Int = Color.BLACK) = TextView(a).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        typeface = ResourcesCompat.getFont(a, if (bold) R.font.bitter_ui_bold else R.font.bitter_ui)
    }

    fun button(a: Activity, s: String, onClick: () -> Unit) = text(a, s, 16f, bold = true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 10))
        background = GradientDrawable().apply {
            cornerRadius = dp(a, 12).toFloat()
            setColor(Color.WHITE)
            setStroke(dp(a, 2), Color.BLACK)
        }
        setOnClickListener { onClick() }
    }

    /** A header: "‹ Home" on the left, the title, an optional label on the right. */
    fun header(a: Activity, title: String, right: String? = null) = LinearLayout(a).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 8))
        addView(button(a, "‹") { a.finish() }, LinearLayout.LayoutParams(dp(a, 52), -2))
        addView(text(a, title, 26f, bold = true).apply { setPadding(dp(a, 14), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        if (right != null) addView(text(a, right, 15f))
    }

    fun rule(a: Activity) = android.view.View(a).apply { setBackgroundColor(Color.BLACK) }
}
