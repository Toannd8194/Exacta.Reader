package com.exacta.reader.data

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingContainer
import org.readium.r2.shared.util.resource.map

/**
 * Our own CSS, put into every chapter when it is read from the book (before the page is laid out).
 *
 * Readium's own line-height rule only reaches body, div, li and p without !important, so a book's
 * CSS on its spans or with !important wins, and the line spacing chosen did nothing; this rule
 * follows the same --USER__lineHeight variable (so it still changes live) on every text element.
 * Readium 3.0's CSS has no rule for the font weight at all: the weight is set here, with a thin
 * text stroke so that fonts without the weight (the book's own) thicken too.
 */
object BookCss {
    val WEIGHT_NAMES = listOf("Mảnh", "Thường", "Hơi đậm", "Đậm", "Rất đậm")
    private val WEIGHTS = intArrayOf(300, 400, 500, 600, 700)
    private val STROKES = doubleArrayOf(0.0, 0.0, 0.15, 0.3, 0.45)

    private const val TEXT = "body, p, div, li, span, blockquote, dd, dt, td, th, a, em, i, font, small, cite, q, section, article"

    fun style(weight: Int): String {
        val w = weight.coerceIn(0, WEIGHTS.size - 1)
        val each = { rule: String -> TEXT.split(", ").joinToString(", ") { rule.replace("&", it) } }
        val lh = each(":root[style*=\"--USER__lineHeight\"] &")
        val wt = each("html &")
        val bold = listOf("b", "strong", "h1", "h2", "h3", "h4", "h5", "h6", "b *", "strong *").joinToString(", ") { "html body $it" }
        val stroke = if (STROKES[w] > 0) " -webkit-text-stroke: ${STROKES[w]}px currentColor;" else ""
        return "<style id=\"exacta-reader\">" +
            "$lh { line-height: var(--USER__lineHeight) !important; }" +
            "$wt { font-weight: ${WEIGHTS[w]} !important;$stroke }" +
            "$bold { font-weight: 800 !important; }" +
            "</style>"
    }

    private fun isHtml(url: Url): Boolean {
        val path = url.path?.lowercase() ?: return false
        return path.endsWith(".xhtml") || path.endsWith(".html") || path.endsWith(".htm")
    }

    /** The book's container with the style put before </head> of every HTML file. */
    fun wrap(container: Container<Resource>): Container<Resource> = TransformingContainer(container) { url, resource ->
        if (!isHtml(url)) {
            resource
        } else {
            resource.map { bytes ->
                val html = String(bytes, Charsets.UTF_8)
                val style = style(Store.textWeight)
                val at = html.indexOf("</head>", ignoreCase = true)
                val out = if (at >= 0) html.substring(0, at) + style + html.substring(at) else style + html
                Try.success(out.toByteArray(Charsets.UTF_8))
            }
        }
    }
}
