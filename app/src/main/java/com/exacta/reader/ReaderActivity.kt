package com.exacta.reader

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.exacta.reader.data.Book
import com.exacta.reader.data.BookCss
import com.exacta.reader.data.Library
import com.exacta.reader.data.Store
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.ReaderSheet
import kotlinx.coroutines.Job
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
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.AbsoluteUrl
import java.io.File
import java.time.LocalTime

/**
 * The reader: a header line with the book's title (it keeps the text off the top edge), the page,
 * and the footer. Tap the right part to go on, the left part to go back (swappable), the middle
 * for the top bar and the bottom sheet of Exacta.OS: progress, then Mục lục, Chữ, Trang, Khác.
 * Reading time and words read are counted for the cultivation; the footer tells of what it brings.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : AppCompatActivity(), ReaderSheet.Host {
    private lateinit var container: FrameLayout
    private lateinit var header: LinearLayout
    private lateinit var headerLeft: TextView
    private lateinit var headerTitle: TextView
    private lateinit var headerRight: TextView
    /** Each chapter's share of the book (from Readium's positions), for the time left in a chapter. */
    private var chapterShare: List<Double> = emptyList()
    private lateinit var footer: LinearLayout
    private lateinit var footerLeft: TextView
    private lateinit var footerRight: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var sheet: ReaderSheet
    private lateinit var flash: View
    private var publication: Publication? = null
    private var navigator: EpubNavigatorFragment? = null
    private var navJob: Job? = null
    private lateinit var book: Book
    private var lastLocator: Locator? = null
    private var toc: List<Pair<String, Link>> = emptyList()

    // Footer notices last a few pages.
    private var notice = ""
    private var noticePages = 0
    private var lastTang = 0
    private var chapterTitle = ""

    // Reading time: counted while there was a touch in the last 3 minutes.
    private val ticker = Handler(Looper.getMainLooper())
    private var lastInput = 0L
    private var lastTick = 0L
    private var sessionStart = 0L
    private val tick = object : Runnable {
        override fun run() {
            account()
            updateFooter()
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
        chapterTitle = book.title
        Store.touch()

        val ui = ResourcesCompat.getFont(this, R.font.bitter_ui)
        val side = Widgets.dp(this, 18)
        // The line above the page: % read, the title, the clock (it also keeps the text off the edge).
        headerLeft = footerText(ui).apply { gravity = Gravity.START }
        headerRight = footerText(ui).apply { gravity = Gravity.END }
        headerTitle = footerText(ui).apply {
            setTextColor(Ink.DARK)
            gravity = Gravity.CENTER
            ellipsize = TextUtils.TruncateAt.END
            text = book.title
            visibility = if (Store.showHeader) View.VISIBLE else View.INVISIBLE
        }
        header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(side, Widgets.dp(context, 10), side, Widgets.dp(context, 6))
            addView(headerLeft, LinearLayout.LayoutParams(Widgets.dp(context, 70), -2))
            addView(headerTitle, LinearLayout.LayoutParams(0, -2, 1f))
            addView(headerRight, LinearLayout.LayoutParams(Widgets.dp(context, 70), -2))
        }
        container = FrameLayout(this).apply { id = View.generateViewId() }
        footerLeft = footerText(ui).apply { gravity = Gravity.START; ellipsize = TextUtils.TruncateAt.END }
        footerRight = footerText(ui).apply { gravity = Gravity.END }
        footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(side, Widgets.dp(context, 6), side, Widgets.dp(context, 10))
            addView(footerLeft, LinearLayout.LayoutParams(0, -2, 1f))
            addView(footerRight, LinearLayout.LayoutParams(-2, -2))
        }
        topBar = buildTopBar()
        sheet = ReaderSheet(this, this).apply { visibility = View.GONE }
        val sheetH = (resources.displayMetrics.heightPixels * 0.56f).toInt()
        val page = FrameLayout(this).apply {
            addView(container, FrameLayout.LayoutParams(-1, -1))
            addView(topBar, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
            addView(sheet, FrameLayout.LayoutParams(-1, sheetH, Gravity.BOTTOM))
        }
        flash = View(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
            isClickable = true
        }
        setContentView(FrameLayout(this).apply {
            addView(LinearLayout(this@ReaderActivity).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                addView(header, LinearLayout.LayoutParams(-1, -2))
                addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
                addView(footer, LinearLayout.LayoutParams(-1, -2))
            }, FrameLayout.LayoutParams(-1, -1))
            addView(flash, FrameLayout.LayoutParams(-1, -1))
        })
        updateFooter()
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

    @Deprecated("Back closes the sheet first.")
    override fun onBackPressed() {
        if (sheet.visibility == View.VISIBLE) hideBars() else @Suppress("DEPRECATION") super.onBackPressed()
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
        headerTitle.text = book.title
        chapterShare = runCatching {
            val counts = pub.positionsByReadingOrder().map { it.size.toDouble() }
            val total = counts.sum()
            if (total > 0) counts.map { it / total } else emptyList()
        }.getOrDefault(emptyList())
        if (book.words < 0) {
            lifecycleScope.launch {
                book.words = Library.countWords(pub)
                Store.touch()
                ExactaApp.log("${book.title}: ${book.words} words")
            }
        }
        val flat = mutableListOf<Pair<String, Link>>()
        fun walk(links: List<Link>, depth: Int) {
            for (l in links) {
                flat.add(("\u0000".repeat(depth) + (l.title ?: "…")) to l)
                walk(l.children, depth + 1)
            }
        }
        walk(pub.tableOfContents, 0)
        toc = flat
        val initial = book.locator.takeIf { it.isNotEmpty() }?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { BookCss.importFromStorage() }.onFailure { ExactaApp.log("fonts: $it") }
        }
        if (Store.fontFamily !in fonts().map { it.first }) Store.fontFamily = "Bitter"
        buildNavigator(pub, initial)
    }

    /** (Re)creates the navigator at `initial`; the weight lives in the chapters' CSS, so it needs this. */
    private fun buildNavigator(pub: Publication, initial: Locator?) {
        val config = EpubNavigatorFragment.Configuration().apply {
            servedAssets = listOf("fonts/.*")
            // Bitter once per weight step: the family switches live when the weight changes.
            for ((family, regular, bold) in BookCss.bitterFaces()) {
                addFontFamilyDeclaration(FontFamily(family)) {
                    for ((face, weight) in listOf(regular to FontWeight.NORMAL, bold to FontWeight.BOLD)) {
                        addFontFace {
                            addSource("fonts/Bitter-$face.ttf")
                            setFontStyle(FontStyle.NORMAL)
                            setFontWeight(weight)
                        }
                        addFontFace {
                            addSource(if (face == "Regular") "fonts/Bitter-Italic.ttf" else "fonts/Bitter-${face}Italic.ttf")
                            setFontStyle(FontStyle.ITALIC)
                            setFontWeight(weight)
                        }
                    }
                }
            }
        }
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
        navJob?.cancel()
        supportFragmentManager.findFragmentByTag(NAV_TAG)?.let { old -> supportFragmentManager.commitNow { remove(old) } }
        supportFragmentManager.commitNow { add(container.id, EpubNavigatorFragment::class.java, Bundle(), NAV_TAG) }
        val nav = supportFragmentManager.findFragmentByTag(NAV_TAG) as EpubNavigatorFragment
        navigator = nav
        nav.addInputListener(object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                input()
                val w = container.width.toFloat()
                if (w <= 0f) return false
                if (sheet.visibility == View.VISIBLE) {
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
        navJob = lifecycleScope.launch {
            nav.currentLocator.collect {
                try {
                    onMoved(it)
                } catch (e: Exception) {
                    ExactaApp.log("onMoved: ${e.stackTraceToString()}")
                }
            }
        }
    }

    private var turns = 0
    private var pageTurn = false

    private fun turn(forward: Boolean) {
        val nav = navigator ?: return
        pageTurn = forward
        if (forward) nav.goForward(animated = false) else nav.goBackward(animated = false)
        turns++
        if (Store.refreshEvery > 0 && turns % Store.refreshEvery == 0) container.postDelayed({ refreshScreen() }, 250)
    }

    /** What the page really got: logged once per book and after each change (no USB debugging here). */
    private fun probe(why: String) {
        val nav = navigator ?: return
        container.postDelayed({
            lifecycleScope.launch {
                val js = "(function(){var p=document.querySelector('p');var r=document.documentElement;" +
                    "return 'css='+!!document.getElementById('exacta-reader')+' lh='+(p?getComputedStyle(p).lineHeight:'-')+" +
                    "' fs='+(p?getComputedStyle(p).fontSize:'-')+' ff='+(p?getComputedStyle(p).fontFamily:'-')+" +
                    "' var='+getComputedStyle(r).getPropertyValue('--USER__lineHeight')+' adv='+getComputedStyle(r).getPropertyValue('--USER__advancedSettings')})()"
                val r = runCatching { nav.evaluateJavascript(js) }.getOrNull()
                ExactaApp.log("page [$why] ${book.title}: $r · lineHeight ${Store.lineHeight}")
            }
        }, 800)
    }

    private var probed = false

    private fun onMoved(locator: Locator) {
        input()
        if (!probed) {
            probed = true
            probe("open")
        }
        lastLocator = locator
        val p = locator.locations.totalProgression ?: return
        Store.moveTo(book, p, pageTurn)
        pageTurn = false
        book.locator = locator.toJSON().toString()
        val tang = Realm.tangOf(book.percent)
        if (tang == lastTang + 1 && Store.showNotices) showNotice("Công pháp đột phá ${Realm.tangName(tang)}")
        lastTang = maxOf(lastTang, tang)
        chapterTitle = locator.title ?: tocTitle(tocCurrent()) ?: chapterTitle
        Store.takeNote().takeIf { it.isNotEmpty() && Store.showNotices }?.let { showNotice(it) }
        if (noticePages > 0) noticePages--
        updateFooter()
        if (sheet.visibility == View.VISIBLE) sheet.invalidate()
    }

    private fun showNotice(text: String) {
        notice = text
        noticePages = 4
    }

    private fun tocTitle(i: Int) = toc.getOrNull(i)?.first?.trimStart('\u0000')

    /** Reading pace in words a second: this book's, else the reader's overall. */
    private fun pace(): Double = when {
        book.wordsRead >= 2000 && book.seconds >= 300 -> book.wordsRead.toDouble() / book.seconds
        Store.pace() > 0 -> Store.pace() / 60.0
        else -> 0.0
    }

    private fun minutesText(minutes: Long) = if (minutes < 60) "~$minutes phút" else "~${minutes / 60} giờ " + (minutes % 60).toString().padStart(2, '0')

    /** Time to the end of this chapter at the reader's pace. */
    private fun chapterLeft(): String? {
        val pace = pace()
        val loc = lastLocator ?: return null
        if (pace <= 0 || book.words <= 0) return null
        val pub = publication ?: return null
        val href = loc.href.toString().substringBefore('#')
        val i = pub.readingOrder.indexOfFirst { it.url().toString().substringBefore('#') == href }
        val share = chapterShare.getOrNull(i) ?: return null
        val left = (1 - (loc.locations.progression ?: 0.0)) * share * book.words
        return minutesText((left / pace / 60).toLong())
    }

    /**
     * Above: % read on the left, the clock on the right. Below: the chapter (or a notice) on the left;
     * the time left in the book and in the chapter on the right.
     */
    private fun updateFooter() {
        if (!::footer.isInitialized) return
        val mode = Store.footerMode
        val now = LocalTime.now()
        headerLeft.text = "${book.percent}%"
        headerTitle.text = if (Store.beQuanTarget > 0) "Bế quan ${Store.beQuanSeconds / 60}/${Store.beQuanTarget} phút" else book.title
        headerRight.text = now.hour.toString().padStart(2, '0') + ":" + now.minute.toString().padStart(2, '0')
        footer.visibility = if (mode == 2 && noticePages <= 0) View.INVISIBLE else View.VISIBLE
        footerLeft.text = when {
            noticePages > 0 -> notice
            mode == 0 -> chapterTitle
            else -> ""
        }
        val parts = listOfNotNull(HomeView.timeLeft(book)?.let { "sách còn $it" }, chapterLeft()?.let { "chương $it" })
        footerRight.text = if (parts.isEmpty()) "" else "  " + parts.joinToString(" · ")
    }

    // --- Bars ---------------------------------------------------------------------------------

    private fun showBars() {
        sheet.opened()
        topBar.visibility = View.VISIBLE
        sheet.visibility = View.VISIBLE
    }

    private fun hideBars() {
        topBar.visibility = View.GONE
        sheet.visibility = View.GONE
    }

    private fun buildTopBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.WHITE)
        val pad = Widgets.dp(context, 14)
        setPadding(pad, Widgets.dp(context, 8), pad, Widgets.dp(context, 8))
        visibility = View.GONE
        addView(Widgets.button(this@ReaderActivity, "‹ Home") { go("home") })
        addView(Widgets.text(this@ReaderActivity, book.title, 15f, bold = true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(pad, 0, pad, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(Widgets.button(this@ReaderActivity, "Thư viện") { go("library") })
    }

    // --- Sheet host ---------------------------------------------------------------------------

    override fun progress() = book.progress

    override fun progressLabel(): String {
        val left = HomeView.timeLeft(book)?.let { " · còn $it" } ?: ""
        return "${book.percent}% · ${Realm.tangName(Realm.tangOf(book.percent))}$left"
    }

    override fun seek(progression: Double) {
        val pub = publication ?: return
        lifecycleScope.launch {
            pub.locateProgression(progression)?.let { navigator?.go(it, animated = false) }
        }
    }

    override fun chapter(delta: Int) {
        val pub = publication ?: return
        val href = lastLocator?.href?.toString()?.substringBefore('#') ?: return
        val i = pub.readingOrder.indexOfFirst { it.url().toString().substringBefore('#') == href }
        val next = pub.readingOrder.getOrNull(i + delta) ?: return
        navigator?.go(next, animated = false)
    }

    override fun toc(): List<Pair<String, Int>> = toc.map { (t, _) ->
        val depth = t.takeWhile { it == '\u0000' }.length
        t.substring(depth) to depth
    }

    override fun tocCurrent(): Int {
        val href = lastLocator?.href?.toString()?.substringBefore('#') ?: return -1
        return toc.indexOfLast { it.second.url().toString().substringBefore('#') == href }
    }

    override fun openToc(index: Int) {
        val link = toc.getOrNull(index)?.second ?: return
        navigator?.go(link, animated = false)
        hideBars()
    }

    override fun applyPrefs(rebuild: Boolean) {
        Store.touch()
        updateFooter()
        headerTitle.visibility = if (Store.showHeader) View.VISIBLE else View.INVISIBLE
        val pub = publication ?: return
        if (rebuild) buildNavigator(pub, lastLocator) else navigator?.submitPreferences(preferences())
        probe("prefs")
    }

    override fun go(where: String) {
        when (where) {
            "library" -> startActivity(Intent(this, LibraryActivity::class.java))
            "tuvi" -> startActivity(Intent(this, TuViActivity::class.java))
            "bequan" -> {
                beQuanDialog()
                return
            }
        }
        if (where == "home") finish() else hideBars()
    }

    /** Bế quan: a reading session of a set length; reading in it makes the forge burn half again. */
    private fun beQuanDialog() {
        val b = androidx.appcompat.app.AlertDialog.Builder(this)
        if (Store.beQuanTarget > 0) {
            b.setTitle("Đang bế quan · ${Store.beQuanSeconds / 60}/${Store.beQuanTarget} phút")
                .setMessage("Đọc đủ ${Store.beQuanTarget} phút thì viên mãn, nhận ${Store.beQuanReward(Store.beQuanTarget)} linh thạch. Trong lúc bế quan, lò luyện cháy ×1,5 và kỳ ngộ ×2.")
                .setPositiveButton("Tiếp tục", null)
                .setNegativeButton("Xuất quan sớm") { _, _ -> Store.cancelBeQuan(); updateFooter(); sheet.invalidate() }
        } else {
            val labels = Store.BE_QUAN_CHOICES.map { "Bế quan $it phút · ${Store.beQuanReward(it)} linh thạch" }
            b.setTitle("Bế quan")
                .setItems(labels.toTypedArray()) { _, i ->
                    Store.startBeQuan(Store.BE_QUAN_CHOICES[i])
                    hideBars()
                    updateFooter()
                }
                .setNegativeButton("Thôi", null)
        }
        b.show()
    }

    private fun preferences() = EpubPreferences(
        fontSize = Store.fontSize * Store.BASE_SIZE,
        lineHeight = Store.lineHeight,
        pageMargins = Store.margins,
        fontFamily = when (Store.fontFamily) {
            "Original" -> null
            "Bitter" -> FontFamily(BookCss.bitterFamily(Store.textWeight))
            else -> FontFamily(Store.fontFamily)
        },
        textAlign = if (Store.justify) TextAlign.JUSTIFY else TextAlign.START,
        hyphens = Store.hyphens,
        paragraphIndent = if (Store.indent) 1.0 else null,
        paragraphSpacing = Store.paraSpacing,
        wordSpacing = BookCss.strokeCarrier(Store.textWeight),
        publisherStyles = false,
        scroll = false,
    )

    override fun fonts(): List<Pair<String, String>> =
        listOf("Bitter" to "Bitter", "Original" to "Font gốc của sách", "serif" to "Có chân (máy)", "sans-serif" to "Không chân (máy)") +
            BookCss.userFonts().map { it.family to it.family }

    private val pickFont = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val name = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val display = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                } ?: "font-${System.currentTimeMillis()}.ttf"
                if (!BookCss.isFont(display)) return@withContext null
                BookCss.importFile(display) { target -> contentResolver.openInputStream(uri)?.use { i -> target.outputStream().use { i.copyTo(it) } } }
                display
            }
            if (name == null) {
                android.widget.Toast.makeText(this@ReaderActivity, "Chỉ nhận tệp .ttf hoặc .otf", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            // The new @font-face lives in the chapters' CSS: open the page again with it.
            Store.fontFamily = BookCss.userFonts().firstOrNull { f -> f.files.any { it.first.name == name } }?.family ?: Store.fontFamily
            Store.touch()
            publication?.let { buildNavigator(it, lastLocator) }
            sheet.invalidate()
        }
    }

    override fun addFont() {
        try {
            pickFont.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream", "*/*"))
        } catch (e: Exception) {
            ExactaApp.log("font picker: $e")
        }
    }

    /** A full refresh of the e-ink panel: a black frame over everything, then the page again. */
    override fun refreshScreen() {
        flash.visibility = View.VISIBLE
        flash.postDelayed({ flash.visibility = View.GONE }, 140)
    }

    private fun footerText(ui: android.graphics.Typeface?) = TextView(this).apply {
        setTextColor(Color.BLACK)
        Widgets.textSize(context, this, 11.5f)
        typeface = ui
        maxLines = 1
    }

    companion object {
        const val EXTRA_PATH = "path"
        private const val NAV_TAG = "navigator"
        private val IDLE_MS get() = Store.idleMinutes * 60 * 1000L
    }
}
