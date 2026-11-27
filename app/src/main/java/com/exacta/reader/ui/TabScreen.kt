package com.exacta.reader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.exacta.reader.data.Store
import com.exacta.reader.game.Lunar
import java.time.LocalDate
import kotlin.math.abs

/** Tap targets registered while drawing. */
class Hits {
    val list = ArrayList<Pair<RectF, () -> Unit>>()
    fun add(r: RectF, action: () -> Unit) {
        list.add(RectF(r) to action)
    }
}

/**
 * Every screen besides Home and the reader: a header ("‹", the title, a label), a row of tabs and
 * one page of content, never scrolled. Content longer than the page is split into pages turned by
 * the "‹ ›" row at the bottom or by a sideways swipe, like the pages of a book.
 */
abstract class TabActivity : AppCompatActivity() {
    protected lateinit var screen: TabView
    var tab = 0
        private set
    var page = 0

    abstract fun title(): String
    open fun right(): String? = null
    abstract fun tabs(): List<String>

    /** Draws page `page` of tab `tab` in `area`; returns how many pages the tab has. */
    abstract fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int

    open fun onTab(tab: Int) {}

    fun selectTab(t: Int) {
        tab = t.coerceIn(0, tabs().size - 1)
        page = 0
        onTab(tab)
        screen.invalidate()
    }

    fun refresh() = screen.invalidate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getInt("tab") ?: intent.getIntExtra(EXTRA_TAB, 0)
        screen = TabView(this, this)
        setContentView(screen)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", tab)
    }

    override fun onResume() {
        super.onResume()
        screen.invalidate()
    }

    override fun onPause() {
        Store.save()
        super.onPause()
    }

    companion object {
        const val EXTRA_TAB = "tab"
    }
}

@SuppressLint("ViewConstructor")
class TabView(context: Context, private val host: TabActivity) : View(context) {
    private var ink: Ink? = null
    private val hits = Hits()
    private var pages = 1
    private var downX = 0f
    private var downY = 0f

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        ink = Ink(context, w)
    }

    override fun onDraw(c: Canvas) {
        val k = ink ?: return
        val u = k.u
        val w = width.toFloat()
        val h = height.toFloat()
        hits.list.clear()
        val today = LocalDate.now()
        k.status(c, w, Lunar.ancientMonthDay(Lunar.fromSolar(today.dayOfMonth, today.monthValue, today.year)))
        val m = Ink.MARGIN * u

        // Header: back, title, label.
        val hy = (Ink.STATUS_H + 12) * u
        val back = RectF(m, hy, m + 56 * u, hy + 46 * u)
        k.button(c, back, "‹", k.paint(30f, bold = true))
        hits.add(back) { host.finish() }
        val big = k.big
        val right = host.right()
        val rw = right?.let { k.ui10.measureText(it) + 12 * u } ?: 0f
        k.text(c, k.fit(host.title(), w - back.right - 16 * u - m - rw, big), back.right + 16 * u, hy + (46 * u - k.lineH(big)) / 2, big)
        if (right != null) k.right(c, right, w - m, hy + (46 * u - k.lineH(k.ui10)) / 2, k.ui10)

        // Tabs.
        val tabs = host.tabs()
        val ty = back.bottom + 14 * u
        val tabH = 48 * u
        if (tabs.size > 1) {
            val gap = 8 * u
            val tw = (w - 2 * m - (tabs.size - 1) * gap) / tabs.size
            for ((i, name) in tabs.withIndex()) {
                val r = RectF(m + i * (tw + gap), ty, m + i * (tw + gap) + tw, ty + tabH)
                k.tab(c, r, name, i == host.tab)
                hits.add(r) { if (host.tab != i) host.selectTab(i) }
            }
        }
        val top = if (tabs.size > 1) ty + tabH + 16 * u else ty + 4 * u

        // Content and the page row.
        val pagerH = 52 * u
        val area = RectF(m, top, w - m, h - 10 * u - pagerH - 8 * u)
        pages = try {
            host.content(c, k, area, host.tab, host.page, hits).coerceAtLeast(1)
        } catch (e: Exception) {
            com.exacta.reader.ExactaApp.log("screen ${host.title()}: ${e.stackTraceToString()}")
            k.text(c, "Lỗi vẽ màn hình: ${e.javaClass.simpleName}", area.left, area.top, k.small)
            1
        }
        if (host.page >= pages) {
            host.page = pages - 1
            post { invalidate() }
        }
        if (pages > 1) {
            val py = h - 10 * u - pagerH
            c.drawRect(m, py - 6 * u, w - m, py - 5 * u, k.gray)
            val bw = 130 * u
            val prev = RectF(m, py + 4 * u, m + bw, py + pagerH - 4 * u)
            val next = RectF(w - m - bw, py + 4 * u, w - m, py + pagerH - 4 * u)
            k.button(c, prev, "‹ Trước", k.ui10Bold, enabled = host.page > 0)
            k.button(c, next, "Sau ›", k.ui10Bold, enabled = host.page < pages - 1)
            k.centered(c, "Trang ${host.page + 1}/$pages", prev.right, next.left - prev.right, py + (pagerH - k.lineH(k.ui10)) / 2, k.ui10)
            hits.add(prev) { turn(-1) }
            hits.add(next) { turn(+1) }
        }
    }

    private fun turn(delta: Int) {
        val p = (host.page + delta).coerceIn(0, pages - 1)
        if (p != host.page) {
            host.page = p
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = e.x - downX
                val dy = e.y - downY
                val u = ink?.u ?: 1f
                if (abs(dx) > 70 * u && abs(dx) > abs(dy) * 1.5f) {
                    turn(if (dx < 0) +1 else -1)
                } else if (abs(dx) < 24 * u && abs(dy) < 24 * u) {
                    hits.list.lastOrNull { it.first.contains(e.x, e.y) }?.second?.invoke()
                }
            }
        }
        return true
    }
}
