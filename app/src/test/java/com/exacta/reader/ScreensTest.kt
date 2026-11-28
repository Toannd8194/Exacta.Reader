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
import com.exacta.reader.data.DaoPhap
import com.exacta.reader.data.DongPhu
import com.exacta.reader.data.Ore
import com.exacta.reader.data.Store
import com.exacta.reader.data.TuLuyen
import com.exacta.reader.game.DanDao
import com.exacta.reader.game.PhapMon
import com.exacta.reader.game.Treasure
import com.exacta.reader.ui.HomeView
import com.exacta.reader.ui.ReaderSheet
import com.exacta.reader.ui.TabActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        for ((i, sp) in listOf(0, 12, 48, 60, 3, 25, 40, 63, 14, 37, 1, 26, 43, 70, 19, 55).withIndex()) {
            Store.beasts.add(Beast(i * 977L, sp / Treasure.KINDS, today - i * 3, titles[i % titles.size].first, sp % Treasure.KINDS,
                if (i % 5 == 1) Treasure.SPIRITS[i % Treasure.SPIRITS.size] else if (i == 3) Treasure.TRIAL_MARK else ""))
        }
        // Three spare Phàm phẩm of the first kind, for hợp luyện.
        for (j in 0 until 3) Store.beasts.add(Beast(50_000L + j, 0, today - j, titles[0].first, 0))
        Store.mergeCount = 2
        Store.items.fill(0)
        listOf(0, 1, 3, 4, 4, 7, 11, 13, 13, 16).forEach { Store.items[it]++ }
        Store.kyNgoCount = 7
        Store.linhThach = 420
        Store.beQuanDone = 3
        Store.kyNgoLog.clear()
        Store.kyNgoLog.addAll(listOf("27/11 · nhặt được Thiên Hỏa Phù", "25/11 · khí linh của Long Song kiếm thức tỉnh: «Ẩn Long»", "22/11 · nhặt được khoáng thạch Địa phẩm"))
        Store.forge.clear()
        Store.forge.addAll(listOf(Ore(1, 9_000), Ore(3, 12_000), Ore(0, 5_000)))
        val first = Store.books.values.first()
        Store.forge.add(Ore(2, 60_000, first.path, Ore.GOAL_STAGE))
        Store.forge.add(Ore(Treasure.tierForBook(first.words), 0, first.path, Ore.GOAL_FINISH))
        Store.forge.add(Ore(4, 0, "", Ore.GOAL_ORE, today - 1).apply { words = need })
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
        // 2.0: the state the move from 1.0 makes (Kim Đan at its ceiling, a lôi kiếp under way).
        for (d in 0..2) Store.daySeconds[today - d] = 2700L + d * 300
        TuLuyen.reset()
        TuLuyen.migrate()
        TuLuyen.tamMa = 24
        TuLuyen.dayLong[today - 1] = 2
        TuLuyen.dayVeryLong[today - 3] = 1
        TuLuyen.coPham = 2
        TuLuyen.danPham = 5
        // 2.1: evenings and some dawns; the move from 2.0, then a few more scrolls.
        Store.hourSeconds.fill(0)
        Store.hourSeconds[19] = 30 * 3600L; Store.hourSeconds[21] = 12 * 3600L; Store.hourSeconds[6] = 14 * 3600L
        Store.hourSeconds[14] = 9 * 3600L; Store.hourSeconds[10] = 6 * 3600L
        DaoPhap.migrate()
        DaoPhap.offerWeek = -1
        DaoPhap.ngo = 6
        DaoPhap.cong[DaoPhap.active]!!.words = 6 * PhapMon.TANG_WORDS[1]
        listOf(6, 11, 12, 3).forEach { DaoPhap.gainCong(it) }
        DaoPhap.tam[PhapMon.TP_THANH_TAM] = 14
        DaoPhap.gainTam(PhapMon.TP_VONG_NGA)
        listOf(PhapMon.T_KIM_CANG, PhapMon.T_THIEN_NHAN, PhapMon.T_HOA_DAN, PhapMon.T_PHONG_DON).forEach { DaoPhap.gainThuat(it) }
        DaoPhap.thuat[PhapMon.T_THO_DON]?.uses = 9
        DaoPhap.thuat[PhapMon.T_KIM_CANG]?.uses = 4
        // 2.2: a cave of Tam giai, three luống, some herbs and pills.
        DongPhu.migrate()
        DongPhu.giai = 3
        DongPhu.plant(0, 0); DongPhu.plots[0].day = today - 12
        DongPhu.plant(1, 2); DongPhu.plots[1].day = today - 4
        DongPhu.herbs.addAll(listOf(0 to 120, 1 to 60, 2 to 150, 4 to 90))
        DongPhu.known.addAll(listOf(DanDao.THANH_TAM, DanDao.TUC_MACH, DanDao.NGUNG_NGUYEN))
        DongPhu.pills[DanDao.TU_KHI * 4 + 2] = 3; DongPhu.pills[DanDao.HOI_KHI * 4] = 1; DongPhu.pills[DanDao.NGUNG_KIM * 4 + 1] = 1
        DongPhu.made = 9
        DongPhu.danDoc = 34
        DongPhu.lastResult = "Thành đan: 2 viên Tụ Khí Đan thượng phẩm"
        // 2.3: in Kiếm Lư, a contest held; a map with one node walked and one waiting.
        com.exacta.reader.data.TongMon.migrate()
        com.exacta.reader.data.TongMon.join(com.exacta.reader.data.TongMon.KIEM_LU, check = false)
        com.exacta.reader.data.TongMon.gain(1_300, "thử")
        com.exacta.reader.data.TongMon.joinDay = today - 120
        com.exacta.reader.data.TongMon.quarter = com.exacta.reader.data.TongMon.quarterOf(today) - 1
        com.exacta.reader.data.TongMon.check()
        com.exacta.reader.data.BiCanh.migrate()
        com.exacta.reader.data.BiCanh.rng = kotlin.random.Random(7)
        com.exacta.reader.data.BiCanh.check()
        Store.biCanhWeek = -1
        Store.biCanhBonusWeek = com.exacta.reader.data.BiCanh.week
        Store.biCanhBonusDays = 3
        com.exacta.reader.data.BiCanh.choose(false)
        com.exacta.reader.data.BiCanh.resolve(com.exacta.reader.data.BiCanh.choices().indexOfLast { it.ok })
        com.exacta.reader.data.BiCanh.choose(true)
        // 2.8: twenty days of the journal: evenings, a few nights, long sittings, a reread.
        val nk = com.exacta.reader.data.NhatKy
        nk.reset(); nk.migrate(); nk.since = today - 19
        val ids = Store.books.values.map { nk.idOf(it) }
        for (d in 0..19) {
            val day = nk.day(today - d)
            day.hours[20] = 1500L + 60 * d; day.hours[21] = 900
            if (d % 3 == 0) { day.hours[23] = 1200; day.hours[0] = 600 }
            if (d % 4 == 1) day.hours[5] = 1200
            day.sittings.addAll(listOf(2400 + 60 * d, 900))
            if (d % 5 == 0) day.sittings.add(5600)
            day.books[ids[d % ids.size]] = 2400
            day.reread = if (d % 6 == 0) 3000 else 0
            day.tamMa = 20 + d
            day.balance = 110
            Store.dayWords[today - d] = 9000L + 200 * d
        }
        Store.books.values.forEachIndexed { i, b -> b.genre = listOf("Tiên hiệp", "Kiếm hiệp", "Khoa học")[i % 3]; b.lang = if (i % 4 == 0) "en" else "vi" }
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

    private fun shoot(view: View, name: String): Bitmap {
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        view.draw(c)
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    /** Share of dark pixels between the tabs and the page row: a page turned to must not be empty. */
    private fun ink(bmp: Bitmap): Double {
        var dark = 0
        var all = 0
        for (y in 300 until h - 140 step 2) for (x in 0 until w step 2) {
            all++
            if (Color.red(bmp.getPixel(x, y)) < 160) dark++
        }
        return dark.toDouble() / all
    }

    private val empty = ArrayList<String>()

    private fun root(a: Activity): View = (a.findViewById<ViewGroup>(android.R.id.content)).getChildAt(0)

    /** A small EPUB opened the way the app opens books: every chapter must carry the app's CSS. */
    @Test
    fun cssInjected() {
        val f = File(RuntimeEnvironment.getApplication().cacheDir, "t.epub")
        java.util.zip.ZipOutputStream(f.outputStream()).use { z ->
            fun put(name: String, text: String, store: Boolean = false) {
                val e = java.util.zip.ZipEntry(name)
                val bytes = text.toByteArray()
                if (store) {
                    e.method = java.util.zip.ZipEntry.STORED
                    e.size = bytes.size.toLong()
                    e.crc = java.util.zip.CRC32().apply { update(bytes) }.value
                }
                z.putNextEntry(e); z.write(bytes); z.closeEntry()
            }
            put("mimetype", "application/epub+zip", store = true)
            put("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/c.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            put("OEBPS/c.opf", """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">x</dc:identifier><dc:title>Thử</dc:title><dc:language>vi</dc:language></metadata><manifest><item id="c1" href="Text/c1.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="c1"/></spine></package>""")
            put("OEBPS/Text/c1.xhtml", """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>c</title></head><body><p>Một hai ba bốn năm.</p></body></html>""")
        }
        val html = kotlinx.coroutines.runBlocking {
            val pub = com.exacta.reader.data.Library.open(f) ?: error("not opened")
            val link = pub.readingOrder.first()
            String(pub.get(link)!!.read().getOrNull()!!)
        }
        org.junit.Assert.assertTrue("CSS missing: $html", html.contains("exacta-reader") && html.contains("--USER__lineHeight"))
    }

    @Test
    fun homeEmpty() {
        Store.beasts.clear()
        Store.forge.clear()
        Store.homeStyle = 0
        val a = Robolectric.buildActivity(HomeActivity::class.java).setup().get()
        idle()
        shoot(root(a), "home_empty")
    }

    @Test
    fun home() {
        com.exacta.reader.ui.Ink.recordCuts = true
        com.exacta.reader.ui.Ink.cuts.clear()
        for (style in HomeView.STYLES.indices) {
            Store.homeStyle = style
            val a = Robolectric.buildActivity(HomeActivity::class.java).setup().get()
            a.resources.displayMetrics.xdpi = 300f
            idle()
            shoot(root(a), "home_$style")
        }
        com.exacta.reader.ui.Ink.recordCuts = false
        // What Home had to cut short, for a look.
        File(out, "home_cuts.txt").writeText(com.exacta.reader.ui.Ink.cuts.joinToString("\n"))
    }

    /** Home at each realm: the frames and cards grow finer with it. */
    @Test
    fun homeRealms() {
        for (r in listOf(0, 3, 5, 8)) {
            Store.realmCap = r
            Store.totalWords = com.exacta.reader.game.Realm.floor(r * 3) + 20_000
            com.exacta.reader.data.TuLuyen.migrate()
            Store.homeStyle = 0
            val a = Robolectric.buildActivity(HomeActivity::class.java).setup().get()
            idle()
            shoot(root(a), "home_realm_$r")
        }
    }

    private fun <T : TabActivity> tabs(cls: Class<T>, name: String) {
        com.exacta.reader.ui.Ink.recordCuts = true
        com.exacta.reader.ui.Ink.cuts.clear()
        val a = Robolectric.buildActivity(cls).setup().get()
        a.resources.displayMetrics.xdpi = 300f
        idle()
        for (t in a.tabs().indices) {
            a.selectTab(t)
            idle()
            shoot(root(a), "${name}_$t")
            // Every further page must hold something.
            val n = a.pageCount()
            for (p in 1 until n) {
                a.page = p
                a.refresh()
                idle()
                val bmp = shoot(root(a), "${name}_${t}_p${p + 1}")
                if (ink(bmp) < 0.004) empty.add("${name} tab $t page ${p + 1}/$n")
            }
            a.page = 0
        }
        com.exacta.reader.ui.Ink.recordCuts = false
        File(out, "${name}_cuts.txt").writeText(com.exacta.reader.ui.Ink.cuts.joinToString(System.lineSeparator()))
        assertTrue("Empty pages: $empty", empty.isEmpty())
    }

    @Test fun library() = tabs(LibraryActivity::class.java, "library")
    @Test fun tuVi() = tabs(TuViActivity::class.java, "tuvi")
    @Test fun phapBao() = tabs(PhapBaoActivity::class.java, "phapbao")
    @Test fun daoHieu() = tabs(DaoHieuActivity::class.java, "daohieu")
    @Test fun nienKy() {
        Store.kyNgoMet[2] = 3; Store.kyNgoMet[15] = 1; Store.kyNgoMet[27] = 2
        tabs(NienKyActivity::class.java, "nienky")
    }
    @Test fun settings() = tabs(SettingsActivity::class.java, "settings")
    @Test fun bienNien() = tabs(BienNienActivity::class.java, "biennien")
    /** Popups are framed: the forge's book picker and a kỳ ngộ. */
    @Test fun dialogs() {
        val a = Robolectric.buildActivity(PhapBaoActivity::class.java).setup().get()
        idle()
        fun snap(d: android.app.Dialog, name: String) {
            idle()
            val v = d.window!!.decorView
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST))
            v.layout(0, 0, v.measuredWidth, v.measuredHeight)
            val bmp = Bitmap.createBitmap(w, maxOf(1, v.measuredHeight) + 40, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.rgb(200, 200, 200))
            c.translate(0f, 20f)
            v.draw(c)
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            d.dismiss()
        }
        val pick = androidx.appcompat.app.AlertDialog.Builder(a).setTitle("Đặt vào lò luyện")
            .setItems(arrayOf("Sách: Đại Chu Tiên Lại · 37%", "Sách: Tiên Nghịch · 64%", "Khoáng thạch Địa phẩm (còn 1)", "Khoáng thạch Hoàng phẩm (còn 2)"), null)
            .setNegativeButton("Thôi", null).show()
        snap(pick, "dialog_forge")
        Store.meet(5)
        val (name, text) = Store.takePopup()!!
        val ky = androidx.appcompat.app.AlertDialog.Builder(a).setTitle("✦ Kỳ ngộ · $name").setMessage(text).setPositiveButton("Nhận", null).show()
        snap(ky, "dialog_kyngo")
    }

    @Test fun tongMon() = tabs(TongMonActivity::class.java, "tongmon")

    @Test fun linhGioi() {
        tabs(LinhGioiActivity::class.java, "linhgioi")
        val l = com.exacta.reader.data.LinhGioi
        val cap = Store.realmCap
        Store.realmCap = 7
        l.reset(); l.started = true
        l.stage[l.HOA] = 3; l.stage[l.KIM] = 3; l.active = l.THUY; l.activeDay = Store.today() - 10; l.stage[l.THUY] = 1
        Store.beasts.take(2).forEach { l.carried.add(it.seed) }
        Store.beasts.lastOrNull()?.let { l.banMenh.add(it.seed) }
        l.banMenhDay = Store.today() - 60
        tabs(LinhGioiActivity::class.java, "linhgioi_open")
        Store.realmCap = cap
        l.reset(); l.started = true
    }

    @Test fun ket() {
        tabs(KetActivity::class.java, "ket_locked")
        val cap = Store.realmCap
        val words = Store.totalWords
        Store.realmCap = 9
        Store.totalWords = com.exacta.reader.data.TuLuyen.floor(9) + 300_000
        com.exacta.reader.data.TuLuyen.gioi = 1
        com.exacta.reader.data.TienDinh.onCross(seen = true)
        com.exacta.reader.data.TienDinh.wordsAtCross = Store.totalWords - 120_000
        tabs(KetActivity::class.java, "ket")
        com.exacta.reader.data.TienDinh.reset(); com.exacta.reader.data.TienDinh.started = true
        com.exacta.reader.data.TuLuyen.gioi = -1
        Store.realmCap = cap
        Store.totalWords = words
    }

    /** 3.0: Cửu Tiêu opened, bậc 2 with a faction; then the greeting, a kiếp under way, and the end. */
    @Test fun gioi() {
        val tl = com.exacta.reader.data.TienLo
        val td = com.exacta.reader.data.TienDinh
        val cap = Store.realmCap
        val words = Store.totalWords
        val lt = Store.linhThach
        Store.realmCap = 9
        Store.totalWords = com.exacta.reader.data.TuLuyen.floor(9) + 1_100_000
        com.exacta.reader.data.TuLuyen.gioi = 0
        td.onCross(seen = true)
        td.crossDay = Store.today() - 19
        td.wordsAtCross = com.exacta.reader.data.TuLuyen.floor(9)
        tl.reset(); tl.started = true
        tl.check()
        tl.welcomeSeen = true
        tl.resTenths = 3_870; tl.tienThach = 64; tl.halls[0] = 2; tl.halls[1] = 1
        tl.check()
        tl.join(1); tl.congHien = 75
        com.exacta.reader.data.CuuTieu.kept = 2; com.exacta.reader.data.CuuTieu.broken = 1
        com.exacta.reader.data.CuuTieu.lastRule = "Giữ thiên quy «Đọc 5 ngày trong tuần»: +30 tiên lực"
        tabs(GioiActivity::class.java, "gioi")
        // Home in Cửu Tiêu: the tiên vị, tiên lực, the dock's Thiên Đình.
        com.exacta.reader.ui.Ink.recordCuts = true
        com.exacta.reader.ui.Ink.cuts.clear()
        val style = Store.homeStyle
        for (s in listOf(0, 3)) {
            Store.homeStyle = s
            val h = Robolectric.buildActivity(HomeActivity::class.java).setup().get()
            idle()
            shoot(root(h), "gioi_home_$s")
        }
        Store.homeStyle = style
        com.exacta.reader.ui.Ink.recordCuts = false
        File(out, "gioi_home_cuts.txt").writeText(com.exacta.reader.ui.Ink.cuts.joinToString(System.lineSeparator()))
        // The greeting, once, and a kiếp under way.
        tl.welcomeSeen = false
        tl.resTenths = 5_400; Store.totalWords = com.exacta.reader.data.TuLuyen.floor(9) + 1_450_000
        tl.startKiep(); tl.kiepDays.addAll(listOf(Store.today() - 2, Store.today() - 1))
        tabs(GioiActivity::class.java, "gioi_kiep")
        // The end of part two.
        tl.kiepActive = -1; tl.kiepPassed = 7; tl.bac = 9; tl.endDay = Store.today(); tl.endSeen = false
        Store.totalWords = com.exacta.reader.data.TuLuyen.floor(9) + 10_500_000
        tabs(GioiActivity::class.java, "gioi_end")
        tl.reset(); tl.started = true
        td.reset(); td.started = true
        com.exacta.reader.data.TuLuyen.gioi = -1
        Store.realmCap = cap
        Store.totalWords = words
        Store.linhThach = lt
    }

    @Test fun daoLo() {
        val d = com.exacta.reader.data.DaoLo
        d.reset(); d.started = true
        tabs(DaoLoActivity::class.java, "daolo")
        d.way = d.THE; d.wayDay = Store.today() - 120; d.way2 = d.THAN; d.way2Day = Store.today() - 20; d.earned = 37_000
        tabs(DaoLoActivity::class.java, "daolo_chosen")
        d.reset(); d.started = true
    }

    @Test fun biCanh() {
        tabs(BiCanhActivity::class.java, "bicanh")
        // The fork, and a sect not chosen yet.
        com.exacta.reader.data.BiCanh.resolve(com.exacta.reader.data.BiCanh.choices().indexOfLast { it.ok })
        val a = Robolectric.buildActivity(BiCanhActivity::class.java).setup().get()
        idle()
        shoot(root(a), "bicanh_fork")
        // 3.3: the master of the bí cảnh, met with a khống chế carried.
        DaoPhap.gainThuat(PhapMon.T_KHON_LONG); DaoPhap.thuat[PhapMon.T_KHON_LONG]!!.learned = true
        DaoPhap.thuatOn.clear(); DaoPhap.thuatOn.add(PhapMon.T_KHON_LONG)
        com.exacta.reader.data.BiCanh.nodes.add(com.exacta.reader.data.BiCanh.CHU)
        com.exacta.reader.data.BiCanh.resolved = com.exacta.reader.data.BiCanh.nodes.size - 1
        a.refresh()
        idle()
        shoot(root(a), "bicanh_master")
        com.exacta.reader.data.TongMon.reset()
        com.exacta.reader.data.TongMon.started = true
        val t = Robolectric.buildActivity(TongMonActivity::class.java).setup().get()
        idle()
        shoot(root(t), "tongmon_choose")
    }

    @Test fun dongPhu() {
        tabs(DongPhuActivity::class.java, "dongphu")
        // A furnace burning.
        DongPhu.herbs.add(0 to 80)
        DongPhu.startBrew(DanDao.TU_KHI)
        val a = Robolectric.buildActivity(DongPhuActivity::class.java).setup().get()
        idle()
        a.selectTab(2)
        idle()
        shoot(root(a), "dongphu_2_brew")
        DongPhu.cancelBrew()
        a.selectTab(1)
        a.picking = 0
        a.refresh()
        idle()
        shoot(root(a), "dongphu_1_sow")
    }

    @Test fun congPhap() {
        tabs(CongPhapActivity::class.java, "congphap")
        val a = Robolectric.buildActivity(CongPhapActivity::class.java).setup().get()
        idle()
        a.selectTab(1)
        a.page = 1
        a.refresh()
        idle()
        shoot(root(a), "congphap_1_p2")
        // 3.2: three slots at Luyện Hư, Đạo gia beside Phật môn, a Ma đạo bộ that will not go with them.
        Store.realmCap = 5
        listOf(PhapMon.TP_BAT_NHA, PhapMon.TP_HUYET_TAM, PhapMon.TP_HAO_NHIEN, PhapMon.TP_KIM_CUONG, PhapMon.TP_VAN_TAM).forEach { DaoPhap.gainTam(it) }
        DaoPhap.tam[PhapMon.TP_THANH_TAM] = 64
        DaoPhap.tam[PhapMon.TP_BAT_NHA] = 31
        DaoPhap.tam[PhapMon.TP_HUYET_TAM] = 7
        DaoPhap.runTam(PhapMon.TP_BAT_NHA)
        a.selectTab(2)
        idle()
        shoot(root(a), "congphap_2_tam")
        a.page = 1
        a.refresh()
        idle()
        shoot(root(a), "congphap_2_p2")
        // 3.3: thuật carried, one cooling, Phong hợp kích, a bản mệnh thần thông.
        listOf(PhapMon.T_KHON_LONG, PhapMon.T_LIET_PHONG, PhapMon.T_PHONG_DON, PhapMon.T_LOI_PHAT).forEach {
            DaoPhap.gainThuat(it); DaoPhap.thuat[it]!!.learned = true; DaoPhap.equip(it)
        }
        DaoPhap.thuat[PhapMon.T_KHON_LONG]!!.cool = 2
        DaoPhap.thuat[PhapMon.T_LIET_PHONG]!!.uses = 9
        val danBefore = com.exacta.reader.data.DaoLo.dan
        com.exacta.reader.data.DaoLo.dan = 5
        a.selectTab(3)
        a.page = 0
        a.refresh()
        idle()
        shoot(root(a), "congphap_3_thuat")
        a.page = 1
        a.refresh()
        idle()
        shoot(root(a), "congphap_3_thuat_p2")
        com.exacta.reader.data.DaoLo.dan = danBefore
        Store.realmCap = 2
        // Before the awakening.
        DaoPhap.migrate()
        Store.hourSeconds.fill(0)
        DaoPhap.reset(); DaoPhap.migrate()
        a.selectTab(0)
        idle()
        shoot(root(a), "congphap_0_early")
    }

    /** The second pages of Thân and Đột phá, and the breakthrough ready and far off. */
    @Test
    fun dotPha() {
        val a = Robolectric.buildActivity(TuViActivity::class.java).setup().get()
        a.resources.displayMetrics.xdpi = 300f
        idle()
        for (t in listOf(1, 2)) {
            a.selectTab(t)
            a.page = 1
            a.refresh()
            idle()
            shoot(root(a), "tuvi_${t}_p2")
        }
        Store.trialStart = -1
        TuLuyen.trialDone = 3
        TuLuyen.restUntil = -1
        a.selectTab(2)
        idle()
        shoot(root(a), "dotpha_ready")
        Store.realmCap = 0
        Store.totalWords = 95_000
        TuLuyen.reset()
        TuLuyen.migrate()
        a.selectTab(2)
        idle()
        shoot(root(a), "dotpha_early")
        a.selectTab(0)
        idle()
        shoot(root(a), "tuvi_0_early")
        // At Phi Thăng: the six realms.
        Store.realmCap = 8
        Store.totalWords = TuLuyen.floor(9) + 10_000
        TuLuyen.reset()
        TuLuyen.migrate()
        a.selectTab(2)
        idle()
        shoot(root(a), "dotpha_lucgioi")
    }

    @Test
    fun merge() {
        assertTrue(0 in Store.mergeTiers())
        val n = Store.beasts.size
        val kinds = Store.kinds()
        assertTrue(Store.merge(0))
        assertEquals(n - 3, Store.beasts.size)
        assertEquals(3, Store.mergeCount)
        // Every kind is still owned: only spares melt.
        assertEquals(kinds, Store.kinds())
        assertTrue(Store.spares(0).isEmpty())
    }

    @Test
    fun readerSheet() {
        val a = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        a.resources.displayMetrics.xdpi = 300f
        val host = object : ReaderSheet.Host {
            override fun progress() = 0.37
            override fun progressLabel() = "37% · Tứ tầng · còn 3 canh giờ"
            override fun seek(progression: Double) {}
            override fun chapter(delta: Int) {}
            override fun toc() = (1..400).map { "Chương $it. Một cái tên chương khá dài để thử" to (if (it % 5 == 0) 0 else 1) }
            override fun tocCurrent() = 312
            override fun openToc(index: Int) {}
            override fun applyPrefs(rebuild: Boolean) {}
            override fun go(where: String) {}
            override fun fonts() = listOf("Bitter" to "Bitter", "Original" to "Font gốc của sách")
            override fun addFont() {}
            override fun refreshScreen() {}
            override fun close() {}
        }
        for (t in 0..4) {
            // 4: the contents' range grid.
            val sheet = ReaderSheet(a, host).apply { tab = minOf(t, 3); if (t == 4) { tab = 0; rangePick = true } }
            val sh = if (t == 0 || t == 4) h else (h * 0.56f).toInt()
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
