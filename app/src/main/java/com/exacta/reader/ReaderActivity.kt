package com.exacta.reader

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.exacta.reader.data.Book
import com.exacta.reader.data.Library
import com.exacta.reader.data.Store
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.HomeView
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.epub.css.FontWeight
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.util.AbsoluteUrl
import java.io.File
import java.time.LocalTime

/**
 * The reader: the page and one footer line. Tap the right part to go on, the left part to go back
 * (swappable in Cài đặt), the middle for the bar: Home, text size, font, line spacing, margins,
 * alignment, a progress slider, contents and chapters. Reading time and words read are counted
 * for the cultivation; the footer tells of a new tầng or a treasure forged.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : AppCompatActivity() {
    private lateinit var container: FrameLayout
    private lateinit var footerLeft: TextView
    private lateinit var footerRight: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var panel: LinearLayout
    private lateinit var progressLabel: TextView
    private lateinit var seek: SeekBar
    private val rows = HashMap<String, LinearLayout>()
    private var publication: Publication? = null
    private var navigator: EpubNavigatorFragment? = null
    private lateinit var book: Book
    private var lastLocator: Locator? = null

    // Footer notices last a few pages.
    private var notice = ""
    private var noticePages = 0
    private var lastTang = 0

    // Reading time: counted while there was a touch in the last 3 minutes.
    private val ticker = Handler(Looper.getMainLooper())
    private var lastInput = 0L
    private var lastTick = 0L
    private var sessionStart = 0L
    private val tick = object : Runnable {
        override fun run() {
            account()
            ticker.postDelayed(this, 20_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The navigator fragment needs its factory first; it is rebuilt each time rather than restored.
        super.onCreate(null)
        val path = intent.getStringExtra(EXTRA_PATH) ?: run { finish(); return }
        book = Store.book(path)
        book.lastOpened = System.currentTimeMillis()
        lastTang = Realm.tangOf(book.percent)
        Store.touch()

        val ui = ResourcesCompat.getFont(this, R.font.bitter_ui)
        container = FrameLayout(this).apply { id = View.generateViewId() }
        footerLeft = footerText(ui).apply { gravity = Gravity.START; ellipsize = TextUtils.TruncateAt.END }
        footerRight = footerText(ui).apply { gravity = Gravity.END }
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(18), dp(2), dp(18), dp(6))
            addView(footerLeft, LinearLayout.LayoutParams(0, -2, 1f))
            addView(footerRight, LinearLayout.LayoutParams(-2, -2))
        }
        topBar = buildTopBar()
        panel = buildPanel()
        val page = FrameLayout(this).apply {
            addView(container, FrameLayout.LayoutParams(-1, -1))
            addView(topBar, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
            addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(-1, -2))
        })
        footerTitle = book.title
        footerLeft.text = book.title
        lifecycleScope.launch {
            try {
                open(File(path))
            } catch (e: Exception) {
                ExactaApp.log("open ${book.title}: ${e.stackTraceToString()}")
                footerLeft.text = "Không mở được sách: ${e.javaClass.simpleName}"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lastInput = SystemClock.uptimeMillis()
        lastTick = lastInput
        sessionStart = lastInput
        ticker.postDelayed(tick, 20_000)
    }

    override fun onPause() {
        account()
        ticker.removeCallbacks(tick)
        Store.save()
        super.onPause()
    }

    override fun onDestroy() {
        publication?.close()
        super.onDestroy()
    }

    private fun account() {
        if (!::book.isInitialized) return
        val now = SystemClock.uptimeMillis()
        if (now - lastInput < IDLE_MS) {
            val seconds = (now - lastTick) / 1000
            try {
                Store.addSeconds(book, seconds, (now - sessionStart) / 1000)
            } catch (e: Exception) {
                ExactaApp.log("account: ${e.stackTraceToString()}")
            }
            lastTick += seconds * 1000
        } else {
            lastTick = now
        }
    }

    private fun input() {
        val now = SystemClock.uptimeMillis()
        if (now - lastInput >= IDLE_MS) {
            // Back from a pause: the idle time does not count, and a new sitting begins.
            lastTick = now
            sessionStart = now
        }
        lastInput = now
    }

    // --- Opening ------------------------------------------------------------------------------

    private suspend fun open(file: File) {
        val pub = Library.open(file) ?: run {
            footerLeft.text = "Không mở được sách này"
            return
        }
        publication = pub
        Library.describe(this, book, pub)
        if (book.words < 0) {
            lifecycleScope.launch {
                book.words = Library.countWords(pub)
                Store.touch()
                ExactaApp.log("${book.title}: ${book.words} words")
            }
        }
        val config = EpubNavigatorFragment.Configuration().apply {
            servedAssets = listOf("fonts/.*")
            addFontFamilyDeclaration(FontFamily("Bitter")) {
                addFontFace {
                    addSource("fonts/Bitter-Regular.ttf")
                    setFontStyle(FontStyle.NORMAL)
                    setFontWeight(FontWeight.NORMAL)
                }
                addFontFace {
                    addSource("fonts/Bitter-Bold.ttf")
                    setFontStyle(FontStyle.NORMAL)
                    setFontWeight(FontWeight.BOLD)
                }
                addFontFace {
                    addSource("fonts/Bitter-Italic.ttf")
                    setFontStyle(FontStyle.ITALIC)
                    setFontWeight(FontWeight.NORMAL)
                }
            }
        }
        val initial = book.locator.takeIf { it.isNotEmpty() }?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }
        supportFragmentManager.fragmentFactory = EpubNavigatorFactory(pub).createFragmentFactory(
            initialLocator = initial,
            initialPreferences = preferences(),
            listener = object : EpubNavigatorFragment.Listener {
                override fun onExternalLinkActivated(url: AbsoluteUrl) {
                    ExactaApp.log("external link ignored: $url")
                }
            },
            configuration = config,
        )
        supportFragmentManager.commitNow { add(container.id, EpubNavigatorFragment::class.java, Bundle(), NAV_TAG) }
        val nav = supportFragmentManager.findFragmentByTag(NAV_TAG) as EpubNavigatorFragment
        navigator = nav
        nav.addInputListener(object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                input()
                val w = container.width.toFloat()
                if (w <= 0f) return false
                if (panel.visibility == View.VISIBLE) {
                    hideBars()
                    return true
                }
                val x = event.point.x
                val third = w / 3
                when {
                    x > w - third -> turn(forward = !Store.swapTaps)
                    x < third -> turn(forward = Store.swapTaps)
                    else -> showBars()
                }
                return true
            }
        })
        lifecycleScope.launch {
            nav.currentLocator.collect {
                try {
                    onMoved(it)
                } catch (e: Exception) {
                    ExactaApp.log("onMoved: ${e.stackTraceToString()}")
                }
            }
        }
    }

    private fun turn(forward: Boolean) {
        val nav = navigator ?: return
        if (forward) nav.goForward(animated = false) else nav.goBackward(animated = false)
    }

    private fun onMoved(locator: Locator) {
        input()
        lastLocator = locator
        val p = locator.locations.totalProgression ?: return
        Store.moveTo(book, p)
        book.locator = locator.toJSON().toString()
        val tang = Realm.tangOf(book.percent)
        if (tang == lastTang + 1) showNotice("Công pháp đột phá ${Realm.tangName(tang)}")
        lastTang = maxOf(lastTang, tang)
        footerTitle = locator.title ?: footerTitle
        Store.takeNote().takeIf { it.isNotEmpty() }?.let { showNotice(it) }
        if (noticePages > 0) {
            footerLeft.text = notice
            noticePages--
        } else {
            footerLeft.text = footerTitle
        }
        updateFooterRight()
        if (panel.visibility == View.VISIBLE) updateProgress()
    }

    private var footerTitle = ""

    private fun updateFooterRight() {
        val now = LocalTime.now()
        val clock = now.hour.toString().padStart(2, '0') + ":" + now.minute.toString().padStart(2, '0')
        val left = HomeView.timeLeft(book)?.let { " · còn $it" } ?: ""
        footerRight.text = "  ${book.percent}%$left · $clock"
    }

    private fun showNotice(text: String) {
        notice = text
        noticePages = 3
    }

    // --- Bars ---------------------------------------------------------------------------------

    private fun showBars() {
        updateProgress()
        refreshChoices()
        topBar.visibility = View.VISIBLE
        panel.visibility = View.VISIBLE
    }

    private fun hideBars() {
        topBar.visibility = View.GONE
        panel.visibility = View.GONE
    }

    private fun buildTopBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = GradientDrawable().apply { setColor(Color.WHITE); setStroke(dp(1), Color.BLACK) }
        setPadding(dp(10), dp(8), dp(10), dp(8))
        visibility = View.GONE
        addView(button("‹ Home", false) { finish() })
        addView(TextView(context).apply {
            text = book.title
            setTextColor(Color.BLACK)
            Widgets.textSize(context, this, 15f)
            typeface = ResourcesCompat.getFont(context, R.font.bitter_ui_bold)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun buildPanel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply { setColor(Color.WHITE); setStroke(dp(2), Color.BLACK) }
        setPadding(dp(14), dp(10), dp(14), dp(12))
        visibility = View.GONE
        isClickable = true  // taps on the panel stay on it

        addView(row("size", "Cỡ chữ"))
        addView(row("font", "Font"))
        addView(row("line", "Giãn dòng"))
        addView(row("margin", "Lề"))
        addView(row("align", "Căn chữ"))
        progressLabel = label("")
        addView(progressLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        seek = SeekBar(context).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, v: Int, fromUser: Boolean) {
                    if (fromUser) progressLabel.text = "Tới ${v / 10}%"
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) = jumpTo(s.progress / 1000.0)
            })
        }
        addView(seek, LinearLayout.LayoutParams(-1, dp(40)))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("Mục lục", false) { showToc() }, LinearLayout.LayoutParams(0, -2, 1.2f))
            addView(button("‹ Chương", false) { chapter(-1) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            addView(button("Chương ›", false) { chapter(+1) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
    }

    private fun row(key: String, title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(4), 0, dp(4))
        addView(label(title), LinearLayout.LayoutParams(dp(92), -2))
        rows[key] = this
    }

    /** The choices of every row, the current one shaded. */
    private fun refreshChoices() {
        fun fill(key: String, items: List<Pair<String, Boolean>>, onPick: (Int) -> Unit) {
            val r = rows[key] ?: return
            while (r.childCount > 1) r.removeViewAt(1)
            items.forEachIndexed { i, (text, on) ->
                r.addView(button(text, on) { onPick(i); refreshChoices() }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
            }
        }
        val pct = (Store.fontSize * 100).toInt()
        fill("size", listOf("A−" to false, "$pct%" to true, "A+" to false)) { i ->
            if (i == 0) Store.fontSize = (Store.fontSize - 0.1).coerceAtLeast(0.6)
            if (i == 2) Store.fontSize = (Store.fontSize + 0.1).coerceAtMost(2.5)
            apply()
        }
        val fonts = listOf("Bitter" to "Bitter", "Original" to "Gốc", "serif" to "Có chân", "sans-serif" to "Không chân")
        fill("font", fonts.map { it.second to (Store.fontFamily == it.first) }) { i ->
            Store.fontFamily = fonts[i].first
            apply()
        }
        val lines = listOf(1.3, 1.5, 1.7, 2.0)
        fill("line", lines.map { "%.1f".format(it).replace('.', ',') to (kotlin.math.abs(Store.lineHeight - it) < 0.01) }) { i ->
            Store.lineHeight = lines[i]
            apply()
        }
        val margins = listOf(0.8 to "Hẹp", 1.2 to "Vừa", 1.6 to "Rộng")
        fill("margin", margins.map { it.second to (kotlin.math.abs(Store.margins - it.first) < 0.01) }) { i ->
            Store.margins = margins[i].first
            apply()
        }
        fill("align", listOf("Đều hai bên" to Store.justify, "Trái" to !Store.justify)) { i ->
            Store.justify = i == 0
            apply()
        }
    }

    private fun preferences() = EpubPreferences(
        fontSize = Store.fontSize,
        lineHeight = Store.lineHeight,
        pageMargins = Store.margins,
        fontFamily = if (Store.fontFamily == "Original") null else FontFamily(Store.fontFamily),
        textAlign = if (Store.justify) TextAlign.JUSTIFY else TextAlign.START,
        publisherStyles = false,
        scroll = false,
    )

    private fun apply() {
        navigator?.submitPreferences(preferences())
        Store.touch()
    }

    private fun updateProgress() {
        seek.progress = (book.progress * 1000).toInt()
        val left = HomeView.timeLeft(book)
        progressLabel.text = "${book.percent}% · ${Realm.tangName(Realm.tangOf(book.percent))}" + (left?.let { " · còn $it" } ?: "")
    }

    private fun jumpTo(progression: Double) {
        val pub = publication ?: return
        lifecycleScope.launch {
            pub.locateProgression(progression)?.let { navigator?.go(it, animated = false) }
        }
    }

    private fun chapter(delta: Int) {
        val pub = publication ?: return
        val href = lastLocator?.href?.toString()?.substringBefore('#') ?: return
        val i = pub.readingOrder.indexOfFirst { it.url().toString().substringBefore('#') == href }
        val next = pub.readingOrder.getOrNull(i + delta) ?: return
        navigator?.go(next, animated = false)
    }

    private fun showToc() {
        val pub = publication ?: return
        val flat = mutableListOf<Pair<String, Link>>()
        fun walk(links: List<Link>, depth: Int) {
            for (l in links) {
                flat.add(("    ".repeat(depth) + (l.title ?: "…")) to l)
                walk(l.children, depth + 1)
            }
        }
        walk(pub.tableOfContents, 0)
        if (flat.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Mục lục")
            .setItems(flat.map { it.first }.toTypedArray()) { _, which ->
                navigator?.go(flat[which].second, animated = false)
                hideBars()
            }
            .show()
    }

    // --- Widgets ------------------------------------------------------------------------------

    private fun button(text: String, selected: Boolean, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(Color.BLACK)
        Widgets.textSize(context, this, 14f)
        typeface = ResourcesCompat.getFont(context, if (selected) R.font.bitter_ui_bold else R.font.bitter_ui)
        setPadding(dp(8), dp(9), dp(8), dp(9))
        maxLines = 1
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (selected) Color.rgb(205, 205, 205) else Color.WHITE)
            setStroke(dp(if (selected) 2 else 1), Color.BLACK)
        }
        setOnClickListener { input(); onClick() }
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.BLACK)
        Widgets.textSize(context, this, 14f)
        typeface = ResourcesCompat.getFont(context, R.font.bitter_ui_bold)
    }

    private fun footerText(ui: android.graphics.Typeface?) = TextView(this).apply {
        setTextColor(Color.BLACK)
        Widgets.textSize(context, this, 12f)
        typeface = ui
        maxLines = 1
    }

    private fun dp(v: Int) = Widgets.dp(this, v)

    companion object {
        const val EXTRA_PATH = "path"
        private const val NAV_TAG = "navigator"
        private const val IDLE_MS = 3 * 60 * 1000L
    }
}
