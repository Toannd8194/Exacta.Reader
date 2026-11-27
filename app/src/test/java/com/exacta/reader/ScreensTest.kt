package com.exacta.reader

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.exacta.reader.data.Beast
import com.exacta.reader.data.Ore
import com.exacta.reader.data.Store
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.ReaderSheet
import com.exacta.reader.ui.TabActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Draws every screen at the iReader Neo 3's size (1072 x 1448, 300 ppi) with sample data and saves
 * them to app/build/screens, so the layouts can be looked at without the device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w536dp-h724dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreensTest {
    private val out = File("build/screens").apply { mkdirs() }
    private val w = 1072
    private val h = 1448

    @Before
    fun seed() {
        val app = RuntimeEnvironment.getApplication()
        // MANAGE_EXTERNAL_STORAGE granted (op 92).
        shadowOf(app.getSystemService(android.app.AppOpsManager::class.java))
            .setMode(92, android.os.Process.myUid(), app.packageName, android.app.AppOpsManager.MODE_ALLOWED)
        app.resources.displayMetrics.xdpi = 300f
        com.exacta.reader.ui.Ink.dpiOverride = 300f
        ExactaApp.storageOverride = true
        val today = Store.today()
        Store.books.clear()
        val dir = File(app.filesDir, "books").apply { mkdirs() }
        val covers = File(app.filesDir, "covers").apply { mkdirs() }
        val titles = listOf(
            "Đại Chu Tiên Lại" to "Vinh Tiểu Vinh", "Phàm Nhân Tu Tiên" to "Vong Ngữ", "Tiên Nghịch" to "Nhĩ Căn",
            "Đấu Phá Thương Khung" to "Thiên Tàm Thổ Đậu", "Già Thiên" to "Thần Đông", "Tru Tiên" to "Tiêu Đỉnh",
            "Kiếm Lai" to "Phong Hỏa Hí Chư Hầu", "Ngã Dục Phong Thiên" to "Nhĩ Căn", "Thế Giới Hoàn Mỹ" to "Thần Đông",
        )
        for ((i, t) in titles.withIndex()) {
            val f = File(dir, "book$i.epub").apply { writeText("x") }
            val b = Store.book(f.absolutePath)
            b.title = t.first
            b.author = t.second
            b.words = 300_000L + i * 120_000L
            b.progress = listOf(0.37, 0.12, 0.64, 1.0, 0.05, 0.0, 0.81, 0.22, 0.0)[i]
            b.maxProgress = b.progress
            b.seconds = 3600L * (i + 2)
            b.wordsRead = (b.words * b.progress).toLong()
            b.lastOpened = if (b.progress > 0) System.currentTimeMillis() - i * 3_600_000L else 0
            b.firstDay = today - 30
            b.lastDay = today - i
            if (b.progress >= 1.0) b.finishDay = today - 5
            b.cover = if (i % 3 == 2) "none" else File(covers, "c$i.png").also { cover(it, t.first, i) }.absolutePath
        }
        Store.totalWords = 640_000
        Store.totalSeconds = 3600L * 71
        Store.giftsAwarded = 6
        Store.realmCap = 2
        Store.realmSeen = 8
        Store.trialStart = today - 2
        Store.tuViLoss = 0
        Store.daySeconds.clear()
        Store.dayWords.clear()
        for (d in 0..400) {
            val s = if (d % 9 == 4) 0L else (1200L + (d * 733L) % 3600L)
            Store.daySeconds[today - d] = s
            Store.dayWords[today - d] = s * 4
        }
        Store.beasts.clear()
        for ((i, sp) in listOf(0, 6, 24, 30, 3, 13, 20, 33, 8, 19, 1, 14).withIndex()) {
            Store.beasts.add(Beast(Treasure.seedFor(sp), sp / 6, today - i * 3, titles[i % titles.size].first))
        }
        Store.forge.clear()
        Store.forge.addAll(listOf(Ore(1, 9_000), Ore(3, 12_000), Ore(0, 5_000)))
        Store.bag.fill(0)
        Store.bag[0] = 2; Store.bag[2] = 1
        Store.setsDone = 0
        Store.checkSets()
        Store.titleDays.clear()
        listOf(0, 1, 6, 7, 18, 24, 15, 31).forEach { Store.titleDays[it] = today - it }
        Store.titleLast = 7
        Store.biCanhCount = 4
        Store.trialPassed = 2
        Store.unseen = 2
        Store.homeStyle = 0
    }

    private fun cover(f: File, title: String, i: Int) {
        val bmp = Bitmap.createBitmap(280, 420, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 0f, 420f, Color.rgb(60 + i * 15, 60 + i * 15, 60 + i * 15), Color.rgb(220, 220, 220), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, 280f, 420f, p)
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 34f; isFakeBoldText = true }
        title.split(' ').chunked(2).forEachIndexed { j, words -> c.drawText(words.joinToString(" "), 24f, 80f + j * 44f, t) }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun idle() {
        repeat(4) {
            Thread.sleep(150)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun shoot(view: View, name: String) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        view.draw(c)
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun root(a: Activity): View = (a.findViewById<ViewGroup>(android.R.id.content)).getChildAt(0)

    @Test
    fun home() {
        for (style in HomeView.STYLES.indices) {
            Store.homeStyle = style
            val a = Robolectric.buildActivity(HomeActivity::class.java).setup().get()
            a.resources.displayMetrics.xdpi = 300f
            idle()
            shoot(root(a), "home_$style")
        }
    }

    private fun <T : TabActivity> tabs(cls: Class<T>, name: String) {
        val a = Robolectric.buildActivity(cls).setup().get()
        a.resources.displayMetrics.xdpi = 300f
        idle()
        for (t in a.tabs().indices) {
            a.selectTab(t)
            idle()
            shoot(root(a), "${name}_$t")
        }
    }

    @Test fun library() = tabs(LibraryActivity::class.java, "library")
    @Test fun tuVi() = tabs(TuViActivity::class.java, "tuvi")
    @Test fun phapBao() = tabs(PhapBaoActivity::class.java, "phapbao")
    @Test fun daoHieu() = tabs(DaoHieuActivity::class.java, "daohieu")
    @Test fun nienKy() = tabs(NienKyActivity::class.java, "nienky")
    @Test fun settings() = tabs(SettingsActivity::class.java, "settings")

    @Test
    fun readerSheet() {
        val a = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        a.resources.displayMetrics.xdpi = 300f
        val host = object : ReaderSheet.Host {
            override fun progress() = 0.37
            override fun progressLabel() = "37% · Tứ tầng · còn ~5 giờ 20"
            override fun seek(progression: Double) {}
            override fun chapter(delta: Int) {}
            override fun toc() = (1..40).map { "Chương $it. Một cái tên chương khá dài để thử" to (if (it % 5 == 0) 0 else 1) }
            override fun tocCurrent() = 12
            override fun openToc(index: Int) {}
            override fun applyPrefs(rebuild: Boolean) {}
            override fun go(where: String) {}
        }
        for (t in 0..3) {
            val sheet = ReaderSheet(a, host).apply { tab = t }
            val sh = (h * 0.56f).toInt()
            sheet.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(sh, View.MeasureSpec.EXACTLY))
            sheet.layout(0, 0, w, sh)
            val bmp = Bitmap.createBitmap(w, sh, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            sheet.draw(c)
            File(out, "sheet_$t.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
