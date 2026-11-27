package com.exacta.reader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.exacta.reader.data.Library
import com.exacta.reader.data.Store
import com.exacta.reader.ui.HomeView
import kotlinx.coroutines.launch

/** The app opens here: the book being read, Pháp bảo, the cultivation card and the way to the rest. */
class HomeActivity : AppCompatActivity() {
    private lateinit var view: HomeView
    private val clock = Handler(Looper.getMainLooper())
    private val minute = object : Runnable {
        override fun run() {
            view.invalidate()
            clock.postDelayed(this, 60_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = HomeView(this) { action, path ->
            when (action) {
                HomeView.Action.READ -> (path ?: Store.current()?.path)?.let {
                    startActivity(Intent(this, ReaderActivity::class.java).putExtra(ReaderActivity.EXTRA_PATH, it))
                }
                HomeView.Action.DAO_HIEU -> startActivity(Intent(this, DaoHieuActivity::class.java))
                HomeView.Action.NIEN_KY -> startActivity(Intent(this, NienKyActivity::class.java))
                HomeView.Action.LIBRARY -> startActivity(Intent(this, LibraryActivity::class.java))
                HomeView.Action.PHAP_BAO -> startActivity(Intent(this, PhapBaoActivity::class.java))
                HomeView.Action.TU_VI -> startActivity(Intent(this, TuViActivity::class.java).apply {
                    path?.toIntOrNull()?.let { putExtra(com.exacta.reader.ui.TabActivity.EXTRA_TAB, it) }
                })
                HomeView.Action.SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))
                HomeView.Action.CONG_PHAP -> startActivity(Intent(this, CongPhapActivity::class.java))
                HomeView.Action.DONG_PHU -> startActivity(Intent(this, DongPhuActivity::class.java))
                HomeView.Action.PERMISSION -> askAllFiles()
            }
        }
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()
        view.needsPermission = !canReadAll()
        val book = Store.current()
        view.book = book
        Store.checkRealm()
        Store.checkTitles()
        com.exacta.reader.ui.KyNgoPopup.show(this)
        view.shuffle()
        view.rescale()
        clock.postDelayed(minute, 60_000)
        if (book != null && (book.words < 0 || book.cover.isEmpty())) {
            lifecycleScope.launch {
                Library.prepare(this@HomeActivity, book)
                Store.save()
                view.invalidate()
            }
        }
    }

    override fun onPause() {
        clock.removeCallbacks(minute)
        Store.save()
        super.onPause()
    }

    private fun canReadAll() = ExactaApp.canReadAll()

    private fun askAllFiles() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            ExactaApp.log("no all-files settings screen: $e")
            startActivity(Intent(this, LibraryActivity::class.java))
        }
    }
}
