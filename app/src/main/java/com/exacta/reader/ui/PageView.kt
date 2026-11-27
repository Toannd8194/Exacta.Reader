package com.exacta.reader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * A tall page drawn with Ink inside a ScrollView: `layout` both measures and draws (canvas null =
 * measure only), so the two never disagree. Taps go to the rectangles registered while drawing.
 */
@SuppressLint("ViewConstructor")
class PageView(context: Context, private val layout: (PageView, Canvas?, Ink) -> Float) : View(context) {
    val targets = ArrayList<Pair<RectF, () -> Unit>>()
    private var downY = 0f

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = if (w > 0) layout(this, null, Ink(context, w)).toInt() else 0
        setMeasuredDimension(w, h)
    }

    override fun onDraw(c: Canvas) {
        targets.clear()
        layout(this, c, Ink(context, width))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> downY = e.y
            MotionEvent.ACTION_UP -> if (kotlin.math.abs(e.y - downY) < 20) {
                targets.firstOrNull { it.first.contains(e.x, e.y) }?.second?.invoke()
            }
        }
        return true
    }
}
