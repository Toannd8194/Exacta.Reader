package com.exacta.reader

import android.content.Intent
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.exacta.reader.data.Book
import com.exacta.reader.data.Library
import com.exacta.reader.data.Store
import com.exacta.reader.game.Realm
import com.exacta.reader.ui.Hits
import com.exacta.reader.ui.Ink
import com.exacta.reader.ui.TabActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thư viện: every EPUB on the storage as a grid of covers, a page at a time, in four tabs (being
 * read, not started, finished, all). Covers and word counts are filled in while the screen is open.
 */
class LibraryActivity : TabActivity() {
    private var books: List<Book> = emptyList()
    private var scanned = false

    private val pickBook = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch { importAndOpen(uri) }
    }

    override fun title() = "Thư viện"
    override fun right() = if (canReadAll()) "${books.size} cuốn" else null
    override fun tabs() = listOf("Đang đọc", "Chưa đọc", "Đã xong", "Tất cả")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        books = Store.books.values.filter { File(it.path).exists() }
        if (savedInstanceState == null && !intent.hasExtra(EXTRA_TAB) && books.none { it.lastOpened > 0 && it.finishDay < 0 }) selectTab(3)
    }

    override fun onResume() {
        super.onResume()
        if (!canReadAll()) return
        lifecycleScope.launch {
            val files = Library.scan()
            files.forEach { Store.book(it.absolutePath) }
            Store.touch()
            books = (files.map { Store.book(it.absolutePath) } +
                Store.books.values.filter { it.path.startsWith(filesDir.path) && File(it.path).exists() }).distinctBy { it.path }
            scanned = true
            refresh()
            // Fill in titles, covers and word counts, recent books first.
            for (b in books.sortedByDescending { it.lastOpened }) {
                if (b.words >= 0 && b.cover.isNotEmpty()) continue
                Library.prepare(this@LibraryActivity, b)
                refresh()
            }
            Store.save()
        }
    }

    private fun list(tab: Int): List<Book> = when (tab) {
        0 -> books.filter { it.lastOpened > 0 && it.finishDay < 0 }.sortedByDescending { it.lastOpened }
        1 -> books.filter { it.lastOpened == 0L }.sortedBy { it.title.lowercase() }
        2 -> books.filter { it.finishDay >= 0 }.sortedByDescending { it.finishDay }
        else -> when (Store.librarySort) {
            1 -> books.sortedBy { it.title.lowercase() }
            2 -> books.sortedWith(compareBy<Book> { it.author.isEmpty() }.thenBy { it.author.lowercase() }.thenBy { it.title.lowercase() })
            else -> books.sortedWith(compareByDescending<Book> { it.lastOpened }.thenBy { it.title.lowercase() })
        }
    }

    override fun content(c: Canvas, k: Ink, area: RectF, tab: Int, page: Int, hits: Hits): Int {
        val u = k.u
        if (!canReadAll()) {
            var y = area.top + 20 * u
            for (line in k.wrap("Vạn Quyển Tiên Đồ cần quyền đọc thẻ nhớ để tìm sách EPUB.", area.width(), k.ui10, 3)) {
                k.text(c, line, area.left, y, k.ui10)
                y += k.lineH(k.ui10)
            }
            y += 20 * u
            val grant = RectF(area.left, y, area.right, y + 56 * u)
            k.button(c, grant, "Cấp quyền đọc toàn bộ tệp")
            hits.add(grant) { askAllFiles() }
            val pick = RectF(area.left, grant.bottom + 14 * u, area.right, grant.bottom + 70 * u)
            k.button(c, pick, "Hoặc chọn một tệp EPUB…")
            hits.add(pick) { pick() }
            return 1
        }
        val items = list(tab)
        val withPick = tab == 3
        val count = items.size + if (withPick) 1 else 0
        if (count == 0) {
            val msg = when {
                !scanned -> "Đang tìm sách trên thẻ nhớ…"
                tab == 0 -> "Chưa có cuốn nào đang đọc. Mở tab Tất cả để chọn sách."
                tab == 1 -> "Cuốn nào cũng đã mở."
                else -> "Chưa đọc xong cuốn nào."
            }
            k.text(c, msg, area.left, area.top + 20 * u, k.ui10)
            return 1
        }
        val gap = 14 * u
        val cols = ((area.width() + gap) / (168 * u + gap)).toInt().coerceIn(2, 6)
        val tileW = (area.width() - (cols - 1) * gap) / cols
        val sb = k.smallBold
        val small = k.small
        val textH = 2 * k.lineH(sb) + k.lineH(small) + 18 * u
        // As many rows as fit at a book's proportions, then the covers grow to fill the page.
        val nominal = tileW * 1.45f
        val rows = Math.round((area.height() + gap) / (nominal + textH + gap)).coerceAtLeast(1)
        val coverH = ((area.height() + gap) / rows - gap - textH).coerceIn(tileW * 1.1f, tileW * 1.7f)
        val tileH = coverH + textH
        val perPage = rows * cols
        val pages = (count + perPage - 1) / perPage
        val first = page * perPage
        val rowGap = gap
        for (i in first until minOf(count, first + perPage)) {
            val slot = i - first
            val x = area.left + (slot % cols) * (tileW + gap)
            val y = area.top + (slot / cols) * (tileH + rowGap)
            val tile = RectF(x, y, x + tileW, y + tileH)
            if (i >= items.size) {
                // The last tile of "Tất cả": pick a file elsewhere.
                val r = RectF(x, y, x + tileW, y + coverH)
                k.card(c, r)
                k.centered(c, "+", r.left, r.width(), r.centerY() - k.lineH(k.big), k.big)
                k.centered(c, "Chọn tệp", r.left, r.width(), r.centerY() + 4 * u, sb)
                k.centered(c, "EPUB khác", r.left, r.width(), r.centerY() + 4 * u + k.lineH(sb), sb)
                hits.add(tile) { pick() }
                continue
            }
            val b = items[i]
            val cw = minOf(tileW, coverH / 1.3f)
            val cr = RectF(x + (tileW - cw) / 2, y, x + (tileW + cw) / 2, y + coverH)
            k.cover(c, b.cover, b.title, cr)
            var ty = cr.bottom + 8 * u
            val lines = k.wrap(b.title, tileW, sb, 2)
            for (l in lines) {
                k.centered(c, l, x, tileW, ty, sb)
                ty += k.lineH(sb)
            }
            ty = cr.bottom + 8 * u + 2 * k.lineH(sb)
            val sub = when {
                b.finishDay >= 0 -> "Đại viên mãn"
                b.lastOpened > 0 -> "${Realm.tangName(Realm.tangOf(b.percent))} · ${b.percent}%"
                b.words > 0 -> "${Realm.van(b.words)} vạn từ"
                else -> "Chưa đọc"
            }
            k.centered(c, sub, x, tileW, ty, small)
            if (b.lastOpened > 0) {
                val by = ty + k.lineH(small) + 4 * u
                c.drawRect(x + 10 * u, by, x + tileW - 10 * u, by + 2 * u, k.gray)
                c.drawRect(x + 10 * u, by - u, x + 10 * u + (tileW - 20 * u) * b.percent / 100f, by + 3 * u, k.fill)
            }
            hits.add(tile) { open(b) }
        }
        return pages
    }

    private fun open(b: Book) {
        startActivity(Intent(this, ReaderActivity::class.java).putExtra(ReaderActivity.EXTRA_PATH, b.path))
    }

    private fun canReadAll() = ExactaApp.canReadAll()

    private fun askAllFiles() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            ExactaApp.log("no all-files settings screen: $e")
        }
    }

    private fun pick() = pickBook.launch(arrayOf("application/epub+zip", "application/octet-stream"))

    private suspend fun importAndOpen(uri: Uri) {
        val copy = withContext(Dispatchers.IO) {
            val dir = File(filesDir, "books").apply { mkdirs() }
            val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.endsWith(".epub", true) } ?: "sach-${System.currentTimeMillis()}.epub"
            val target = File(dir, name)
            contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            target
        }
        open(Store.book(copy.absolutePath))
    }
}
