package com.exacta.reader

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.exacta.reader.Widgets.dp
import com.exacta.reader.data.Book
import com.exacta.reader.data.Library
import com.exacta.reader.data.Store
import com.exacta.reader.game.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thư viện: every EPUB on the storage with its cover, author and progress; the ones read recently
 * first. Covers and word counts are filled in, one book after another, while the list is open.
 */
class LibraryActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var count: TextView
    private val rows = HashMap<String, Pair<ImageView, TextView>>()

    private val pickBook = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch { importAndOpen(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val header = Widgets.header(this, "Thư viện")
        count = Widgets.text(this, "", 15f)
        header.addView(count)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@LibraryActivity, 16), 0, dp(this@LibraryActivity, 16), dp(this@LibraryActivity, 16))
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(header)
            addView(Widgets.rule(this@LibraryActivity), LinearLayout.LayoutParams(-1, dp(this@LibraryActivity, 1)))
            addView(ScrollView(this@LibraryActivity).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    override fun onResume() {
        super.onResume()
        if (!canReadAll()) {
            showPermission()
            return
        }
        show(Store.books.values.filter { File(it.path).exists() })
        lifecycleScope.launch {
            val files = Library.scan()
            files.forEach { Store.book(it.absolutePath) }
            Store.touch()
            val books = files.map { Store.book(it.absolutePath) } + Store.books.values.filter { it.path.startsWith(filesDir.path) && File(it.path).exists() }
            show(books.distinctBy { it.path })
            // Fill in titles, covers and word counts, recent books first.
            for (b in sorted(books)) {
                if (b.words >= 0 && b.cover.isNotEmpty()) continue
                Library.prepare(this@LibraryActivity, b)
                update(b)
            }
            Store.save()
        }
    }

    override fun onPause() {
        Store.save()
        super.onPause()
    }

    private fun sorted(books: Collection<Book>) =
        books.sortedWith(compareByDescending<Book> { it.lastOpened }.thenBy { it.title.lowercase() })

    private fun show(books: Collection<Book>) {
        list.removeAllViews()
        rows.clear()
        count.text = "${books.size} cuốn"
        if (books.isEmpty()) {
            list.addView(Widgets.text(this, "Chưa thấy tệp EPUB nào trên thẻ nhớ.", 17f).apply { setPadding(0, dp(this@LibraryActivity, 24), 0, dp(this@LibraryActivity, 12)) })
        }
        for (b in sorted(books)) list.addView(row(b))
        list.addView(Widgets.button(this, "Chọn tệp EPUB khác…") { pick() }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(this@LibraryActivity, 16) })
    }

    private fun row(b: Book) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(this@LibraryActivity, 10), 0, dp(this@LibraryActivity, 10))
        val cover = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { setColor(Color.rgb(205, 205, 205)); setStroke(dp(this@LibraryActivity, 1), Color.BLACK) }
            setPadding(dp(this@LibraryActivity, 1), dp(this@LibraryActivity, 1), dp(this@LibraryActivity, 1), dp(this@LibraryActivity, 1))
        }
        addView(cover, LinearLayout.LayoutParams(dp(this@LibraryActivity, 60), dp(this@LibraryActivity, 88)))
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(this@LibraryActivity, 14), 0, 0, 0) }
        val title = Widgets.text(this@LibraryActivity, b.title, 18f, bold = true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
        val sub = Widgets.text(this@LibraryActivity, subtitle(b), 14f)
        texts.addView(title)
        texts.addView(sub)
        addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        setOnClickListener { open(b) }
        rows[b.path] = cover to title
        tag = sub
        loadCover(b, cover)
    }

    private fun subtitle(b: Book): String {
        val parts = mutableListOf<String>()
        if (b.author.isNotEmpty()) parts.add(b.author)
        parts.add(
            when {
                b.finishDay >= 0 -> "Đại viên mãn"
                b.lastOpened > 0 -> "${Realm.tangName(Realm.tangOf(b.percent))} · ${b.percent}%"
                else -> "Chưa đọc"
            }
        )
        if (b.words > 0) parts.add("${Realm.van(b.words)} vạn từ")
        return parts.joinToString(" · ")
    }

    private fun update(b: Book) {
        val (cover, title) = rows[b.path] ?: return
        title.text = b.title
        ((title.parent as LinearLayout).getChildAt(1) as TextView).text = subtitle(b)
        loadCover(b, cover)
    }

    private fun loadCover(b: Book, view: ImageView) {
        if (b.cover.isEmpty() || b.cover == Library.NO_COVER) return
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(b.cover) }.getOrNull() }
            if (bmp != null) view.setImageBitmap(bmp)
        }
    }

    private fun open(b: Book) {
        startActivity(Intent(this, ReaderActivity::class.java).putExtra(ReaderActivity.EXTRA_PATH, b.path))
    }

    private fun canReadAll() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

    private fun showPermission() {
        list.removeAllViews()
        list.addView(Widgets.text(this, "Exacta Reader cần quyền đọc thẻ nhớ để tìm sách EPUB.", 17f).apply { setPadding(0, dp(this@LibraryActivity, 24), 0, dp(this@LibraryActivity, 16)) })
        list.addView(Widgets.button(this, "Cấp quyền đọc toàn bộ tệp") {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                ExactaApp.log("no all-files settings screen: $e")
            }
        })
        list.addView(Widgets.button(this, "Hoặc chọn một tệp EPUB…") { pick() }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(this@LibraryActivity, 12) })
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
