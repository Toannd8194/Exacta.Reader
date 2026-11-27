package com.exacta.reader

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.exacta.reader.ui.Ink

/**
 * Plain e-ink widgets for the list screens: black on white, Bitter, outlined buttons. Sizes are
 * in "dp" measured on the real screen density (and the UI size setting), not the ROM's density.
 */
object Widgets {
    /** Pixels per dp: one dp is about 1/165 inch at "Vừa". */
    fun px(c: Context) = Ink.unit(c) * 1.3f
    fun dp(c: Context, v: Int) = (v * px(c)).toInt()
    fun textSize(c: Context, tv: TextView, sp: Float) = tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp * px(c))

    fun text(a: Activity, s: String, sp: Float, bold: Boolean = false, color: Int = Color.BLACK) = TextView(a).apply {
        text = s
        setTextColor(color)
        textSize(a, this, sp)
        typeface = ResourcesCompat.getFont(a, if (bold) R.font.bitter_ui_bold else R.font.bitter_ui)
    }

    fun button(a: Activity, s: String, onClick: () -> Unit) = text(a, s, 15f, bold = true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(a, 12), dp(a, 9), dp(a, 12), dp(a, 9))
        background = GradientDrawable().apply {
            cornerRadius = dp(a, 10).toFloat()
            setColor(Color.WHITE)
            setStroke(dp(a, 2).coerceAtLeast(2), Color.BLACK)
        }
        setOnClickListener { onClick() }
    }

    /** A header: "‹" on the left, the title, an optional label on the right. */
    fun header(a: Activity, title: String, right: String? = null) = LinearLayout(a).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 8))
        addView(button(a, "‹") { a.finish() }, LinearLayout.LayoutParams(dp(a, 46), -2))
        addView(text(a, title, 22f, bold = true).apply { setPadding(dp(a, 12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        if (right != null) addView(text(a, right, 14f))
    }

    fun rule(a: Activity) = android.view.View(a).apply { setBackgroundColor(Color.BLACK) }
}
