package com.exacta.reader

import android.app.Application
import android.os.Environment
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exacta Reader. Holds the Readium objects that open EPUB files, and a small log: the device gives no
 * USB debugging, so errors (and page-turn timings) go to ExactaReader/exacta-log.txt on the storage,
 * where the reader can copy it from.
 */
class ExactaApp : Application() {
    lateinit var assetRetriever: AssetRetriever
        private set
    lateinit var publicationOpener: PublicationOpener
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val httpClient = DefaultHttpClient()
        assetRetriever = AssetRetriever(contentResolver, httpClient)
        // Every chapter gets the app's CSS (line spacing, weight) as it is read from the book.
        publicationOpener = PublicationOpener(
            DefaultPublicationParser(this, httpClient, assetRetriever, pdfFactory = null),
            emptyList(),
        ) { container = com.exacta.reader.data.BookCss.wrap(container) }
        com.exacta.reader.data.Store.load(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            log("CRASH " + error.stackTraceToString())
            try {
                File(filesDir, LAST_CRASH).writeText(stamp.format(Date()) + "\n" + error.stackTraceToString().take(4000))
            } catch (_: Exception) {
            }
            previous?.uncaughtException(thread, error)
        }
        val dm = resources.displayMetrics
        log("start ${BuildConfigInfo.VERSION} · ${dm.widthPixels}x${dm.heightPixels} xdpi ${dm.xdpi} density ${dm.densityDpi}")
    }

    companion object {
        lateinit var instance: ExactaApp
            private set

        const val LAST_CRASH = "last-crash.txt"

        /** Set by the screen tests (Robolectric cannot answer the all-files question). */
        var storageOverride: Boolean? = null

        /** May the app read the whole shared storage (MANAGE_EXTERNAL_STORAGE on Android 11+)? */
        fun canReadAll(): Boolean = storageOverride
            ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
        private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        /** One line to the log files (app folder always; ExactaReader/ on the storage when allowed). */
        fun log(line: String) {
            val text = "${stamp.format(Date())}  $line\n"
            val targets = buildList {
                instance.getExternalFilesDir(null)?.let { add(File(it, "exacta-log.txt")) }
                add(File(Environment.getExternalStorageDirectory(), "ExactaReader/exacta-log.txt"))
            }
            for (file in targets) {
                try {
                    file.parentFile?.mkdirs()
                    if (file.length() > 512 * 1024) file.delete()
                    file.appendText(text)
                } catch (_: Exception) {
                    // no permission for this one yet
                }
            }
        }
    }
}

object BuildConfigInfo {
    const val VERSION = "0.4"
}
