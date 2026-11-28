package com.exacta.reader.data

import android.content.Context
import android.os.Environment
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingContainer
import org.readium.r2.shared.util.resource.map
import java.io.File

/**
 * The fonts of the reader and the CSS the app puts into every chapter as it is read from the book.
 *
 * Readium 3.0 has no rule for the font weight, and its line-height rule loses to a book's own CSS.
 * So: the weight of Bitter is a family per weight (Bitter Light … Bitter ExtraBold, from the static
 * files in assets/fonts), switched live like any font; other fonts are thickened with a text stroke
 * whose width rides on Readium's word-spacing variable (the word spacing itself is reset), so it too
 * changes live. Line height follows Readium's own variable on every text element.
 *
 * Fonts the reader adds (TTF/OTF from ExactaReader/fonts, or picked) are copied to the app and served
 * inside the open book under exacta-fonts/, with @font-face rules in the same CSS.
 */
object BookCss {
    /** The weight steps of 2.4 and before (kept to read old settings): the CSS weight of each. */
    val OLD_STEP_WEIGHT = intArrayOf(300, 400, 500, 600, 700, 800)

    /**
     * 2.5: the weight is a percent of the reader's reference weight (the step they had, 100%), like
     * the font size. Bitter takes the heaviest static face not above the weight asked and a text
     * stroke for the rest; other fonts take a stroke over their own regular face (400).
     */
    private val BITTER_FACES = listOf("Light", "Regular", "Medium", "SemiBold", "Bold", "ExtraBold", "Black")
    private val BITTER_WEIGHT = intArrayOf(300, 400, 500, 600, 700, 800, 900)
    private val BITTER_BOLD = listOf("SemiBold", "Bold", "ExtraBold", "Black", "Black", "Black", "Black")
    private const val STROKE_REM = 0.011  // of text stroke for 100 of CSS weight
    const val WEIGHT_MIN = 300
    const val WEIGHT_MAX = 1300

    /** The CSS weight asked for now. */
    fun weight() = (Store.weightRef * Store.weightPct / 100).coerceIn(WEIGHT_MIN, WEIGHT_MAX)

    /** Percents allowed with the reference weight: 5 apart, the weight from 300 to 1300. */
    fun pctMin() = ((WEIGHT_MIN * 100 + Store.weightRef - 1) / Store.weightRef + 4) / 5 * 5
    fun pctMax() = (WEIGHT_MAX * 100 / Store.weightRef) / 5 * 5

    private fun face(w: Int) = BITTER_WEIGHT.indexOfLast { it <= w }.coerceAtLeast(0)

    /** The Readium font family of a Bitter face. */
    private fun bitterName(i: Int) = if (i == 1) "Bitter" else "Bitter " + BITTER_FACES[i]
    fun bitterFamily() = bitterName(face(weight()))

    /** All Bitter families: name, regular face file, bold face file (italics alongside). */
    fun bitterFaces() = BITTER_FACES.indices.map { Triple(bitterName(it), BITTER_FACES[it], BITTER_BOLD[it]) }

    /** The word-spacing value that carries the text stroke: the weight above the face drawn. */
    fun strokeCarrier(): Double? {
        val w = weight()
        val base = if (Store.fontFamily == "Bitter") BITTER_WEIGHT[face(w)] else 400
        return if (w <= base) null else (w - base) * STROKE_REM / 100
    }

    // --- Fonts the reader adds ------------------------------------------------------------------

    class UserFont(val family: String, val files: List<Pair<File, Pair<Boolean, Boolean>>>)  // file, (bold, italic)

    private lateinit var dir: File

    fun init(context: Context) {
        dir = File(context.filesDir, "fonts").apply { mkdirs() }
    }

    /** Copies new TTF/OTF files from ExactaReader/fonts on the storage; returns how many. */
    fun importFromStorage(): Int {
        val src = File(Environment.getExternalStorageDirectory(), "ExactaReader/fonts")
        val files = src.listFiles { f -> f.isFile && isFont(f.name) } ?: return 0
        var n = 0
        for (f in files) {
            val target = File(dir, f.name)
            if (!target.exists() || target.length() != f.length()) {
                f.copyTo(target, overwrite = true)
                n++
            }
        }
        return n
    }

    fun importFile(name: String, write: (File) -> Unit): File {
        val target = File(dir, name.substringAfterLast('/'))
        write(target)
        return target
    }

    fun isFont(name: String) = name.endsWith(".ttf", true) || name.endsWith(".otf", true)

    /** The fonts added, grouped by family (from the file names: "Name Bold Italic.ttf"). */
    fun userFonts(): List<UserFont> {
        if (!::dir.isInitialized) return emptyList()
        val files = dir.listFiles { f -> isFont(f.name) }?.sortedBy { it.name } ?: return emptyList()
        return files.groupBy { family(it.name) }.map { (fam, list) ->
            UserFont(fam, list.map { f ->
                val n = f.nameWithoutExtension.lowercase()
                f to ((n.contains("bold") || n.contains("black") || n.contains("heavy")) to (n.contains("italic") || n.contains("oblique")))
            })
        }.sortedBy { it.family.lowercase() }
    }

    private fun family(fileName: String): String {
        var base = fileName.substringBeforeLast('.')
        base = base.replace(Regex("(?i)(bold ?italic|bold ?oblique|semibold|extrabold|black|heavy|medium|light|regular|bold|italic|oblique|book|roman)"), " ")
        return base.replace('_', ' ').replace('-', ' ').replace(Regex(" +"), " ").trim().ifEmpty { fileName.substringBeforeLast('.') }
    }

    fun removeFamily(family: String) {
        userFonts().firstOrNull { it.family == family }?.files?.forEach { it.first.delete() }
    }

    // --- The CSS ------------------------------------------------------------------------------

    private const val TEXT = "body, p, div, li, span, blockquote, dd, dt, td, th, a, em, i, b, strong, font, small, cite, q, section, article, h1, h2, h3, h4, h5, h6"
    private const val SERVE = "https://readium/publication/exacta-fonts/"

    fun style(): String {
        val each = { rule: String -> TEXT.split(", ").joinToString(", ") { rule.replace("&", it) } }
        val sb = StringBuilder("<style id=\"exacta-reader\">")
        // Line height on every text element, following Readium's variable.
        // The :not(#…) parts weigh like ids, so the rule wins over a book's own id or !important rules.
        sb.append(":root:not(#x1):not(#x2)[style*=\"--USER__lineHeight\"] body, :root:not(#x1):not(#x2)[style*=\"--USER__lineHeight\"] body *")
            .append(" { line-height: var(--USER__lineHeight) !important; }")
        // The weight stroke, carried by --USER__wordSpacing; the spacing itself stays normal.
        sb.append(each(":root[style*=\"--USER__wordSpacing\"] &"))
            .append(" { word-spacing: normal !important; -webkit-text-stroke: var(--USER__wordSpacing) currentColor; }")
        // The fonts the reader added.
        for (font in userFonts()) {
            for ((file, style) in font.files) {
                sb.append("@font-face { font-family: \"").append(font.family).append("\"; src: url(\"").append(SERVE)
                    .append(Url.fromDecodedPath(file.name)?.toString() ?: file.name).append("\"); font-weight: ")
                    .append(if (style.first) "700" else "400").append("; font-style: ").append(if (style.second) "italic" else "normal").append("; }")
            }
        }
        return sb.append("</style>").toString()
    }

    private fun isHtml(url: Url): Boolean {
        val path = url.path?.lowercase() ?: return false
        return path.endsWith(".xhtml") || path.endsWith(".html") || path.endsWith(".htm")
    }

    /** The book's container: the CSS before </head> of every HTML file, and the added fonts served. */
    fun wrap(container: Container<Resource>): Container<Resource> {
        val styled = TransformingContainer(container) { url, resource ->
            if (!isHtml(url)) {
                resource
            } else {
                resource.map { bytes ->
                    val html = String(bytes, Charsets.UTF_8)
                    val at = html.indexOf("</head>", ignoreCase = true)
                    val out = if (at >= 0) html.substring(0, at) + style() + html.substring(at) else style() + html
                    Try.success(out.toByteArray(Charsets.UTF_8))
                }
            }
        }
        return FontServing(styled)
    }

    private class FontServing(private val inner: Container<Resource>) : Container<Resource> {
        override val sourceUrl: AbsoluteUrl? get() = inner.sourceUrl
        override val entries: Set<Url> get() = inner.entries
        override fun iterator(): Iterator<Url> = inner.iterator()
        override fun close() = inner.close()
        override fun get(url: Url): Resource? {
            val path = url.path
            if (path != null && path.startsWith("exacta-fonts/") && ::dir.isInitialized) {
                val f = File(dir, path.removePrefix("exacta-fonts/"))
                if (f.exists() && f.parentFile == dir) return FileResource(f)
            }
            return inner[url]
        }
    }
}
