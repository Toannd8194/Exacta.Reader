package com.exacta.reader.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.exacta.reader.ExactaApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.getOrElse
import java.io.File

/**
 * The books: EPUB files found on the storage, their title, author, cover (cached as a small PNG)
 * and word count (counted once, from the text of every chapter).
 */
object Library {
    const val NO_COVER = "none"

    /** EPUB files on the shared storage, five folders deep, skipping Android/ and hidden folders. */
    suspend fun scan(): List<File> = withContext(Dispatchers.IO) {
        val out = mutableListOf<File>()
        fun walk(dir: File, depth: Int) {
            val entries = dir.listFiles() ?: return
            for (f in entries) {
                if (f.isDirectory) {
                    if (depth < 5 && !f.name.startsWith(".") && f.name != "Android") walk(f, depth + 1)
                } else if (f.name.endsWith(".epub", ignoreCase = true)) {
                    out.add(f)
                }
            }
        }
        walk(Environment.getExternalStorageDirectory(), 0)
        out
    }

    suspend fun open(file: File): Publication? {
        val app = ExactaApp.instance
        val asset = app.assetRetriever.retrieve(file).getOrElse {
            ExactaApp.log("retrieve ${file.name}: $it")
            return null
        }
        // The app's CSS goes into every chapter here (the opener's own callback is not applied by Readium 3.0).
        return app.publicationOpener.open(
            asset,
            allowUserInteraction = false,
            onCreatePublication = { container = BookCss.wrap(container) },
        ).getOrElse {
            ExactaApp.log("open ${file.name}: $it")
            null
        }
    }

    /** Title, author and cover from an open publication. */
    suspend fun describe(context: Context, book: Book, pub: Publication) {
        pub.metadata.title?.takeIf { it.isNotBlank() }?.let { book.title = it }
        book.author = pub.metadata.authors.joinToString(", ") { it.name }
        book.genre = pub.metadata.subjects.joinToString(", ") { it.name }
        book.lang = pub.metadata.languages.joinToString(",")
        book.meta = true
        if (book.cover.isEmpty() || (book.cover != NO_COVER && !File(book.cover).exists())) {
            val bitmap = runCatching { pub.cover() }.getOrNull()
            book.cover = if (bitmap != null) withContext(Dispatchers.IO) { saveCover(context, book, bitmap) } else NO_COVER
        }
        Store.touch()
    }

    private fun saveCover(context: Context, book: Book, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "covers").apply { mkdirs() }
        val target = File(dir, "${book.path.hashCode().toUInt()}.png")
        val h = 420
        val w = (bitmap.width * h / bitmap.height.coerceAtLeast(1)).coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        target.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return target.absolutePath
    }

    private val TAG = Regex("<[^>]*>")
    private val ENTITY = Regex("&[a-zA-Z#0-9]+;")
    private val SPACE = Regex("\\s+")
    private val HEAD = Regex("(?is)<head.*?</head>")

    /** Words in the book: the text of every chapter in reading order. */
    suspend fun countWords(pub: Publication): Long = withContext(Dispatchers.IO) {
        var total = 0L
        for (link in pub.readingOrder) {
            val resource = pub.get(link) ?: continue
            val bytes = resource.read().getOrElse { null }
            resource.close()
            if (bytes == null) continue
            val text = String(bytes, Charsets.UTF_8).replace(HEAD, " ").replace(TAG, " ").replace(ENTITY, " ")
            total += text.split(SPACE).count { it.any(Char::isLetterOrDigit) }
        }
        total
    }

    /** Opens the book if needed and fills in what is missing (title, cover, words). */
    suspend fun prepare(context: Context, book: Book) {
        if (book.words >= 0 && book.cover.isNotEmpty() && book.meta) return
        val pub = open(File(book.path)) ?: return
        try {
            describe(context, book, pub)
            if (book.words < 0) book.words = countWords(pub)
            Store.touch()
        } finally {
            pub.close()
        }
    }
}
