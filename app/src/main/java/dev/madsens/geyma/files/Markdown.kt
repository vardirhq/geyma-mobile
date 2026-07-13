package dev.madsens.geyma.files

/**
 * A deliberately small Markdown → HTML converter so Geyma can render `.md` files
 * as a formatted page in its WebView-backed viewer instead of showing raw source
 * or bouncing to another app. Pure Kotlin (no Android imports) so it stays
 * unit-testable.
 *
 * It covers the common subset — ATX headings, emphasis, inline and fenced code,
 * links, images (shown as their alt text, since the viewer blocks network/file
 * loads), unordered/ordered lists, blockquotes, horizontal rules and paragraphs —
 * and HTML-escapes every piece of document text so a file can't inject markup.
 * It is a previewer, not a spec-complete parser.
 */
object Markdown {

    fun toHtml(src: String): String {
        val out = StringBuilder()
        val lines = src.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.trimStart().startsWith("```") -> {
                    val code = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        code.append(escape(lines[i])).append('\n')
                        i++
                    }
                    if (i < lines.size) i++ // consume the closing fence
                    out.append("<pre><code>").append(code).append("</code></pre>\n")
                }
                line.isBlank() -> i++
                isHorizontalRule(line) -> {
                    out.append("<hr>\n")
                    i++
                }
                headingLevel(line) > 0 -> {
                    val level = headingLevel(line)
                    val text = line.trimStart().substring(level).trim()
                    out.append("<h").append(level).append('>')
                        .append(inline(text))
                        .append("</h").append(level).append(">\n")
                    i++
                }
                line.trimStart().startsWith(">") -> {
                    val buf = StringBuilder()
                    while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                        val content = lines[i].trimStart().removePrefix(">").removePrefix(" ")
                        if (buf.isNotEmpty()) buf.append(' ')
                        buf.append(content)
                        i++
                    }
                    out.append("<blockquote>").append(inline(buf.toString())).append("</blockquote>\n")
                }
                unorderedMarker(line) != null -> {
                    out.append("<ul>\n")
                    while (i < lines.size && unorderedMarker(lines[i]) != null) {
                        val marker = unorderedMarker(lines[i])!!
                        val item = lines[i].trimStart().substring(marker.length).trim()
                        out.append("<li>").append(inline(item)).append("</li>\n")
                        i++
                    }
                    out.append("</ul>\n")
                }
                orderedMarker(line) != null -> {
                    out.append("<ol>\n")
                    while (i < lines.size && orderedMarker(lines[i]) != null) {
                        val marker = orderedMarker(lines[i])!!
                        val item = lines[i].trimStart().substring(marker.length).trim()
                        out.append("<li>").append(inline(item)).append("</li>\n")
                        i++
                    }
                    out.append("</ol>\n")
                }
                else -> {
                    val buf = StringBuilder()
                    while (i < lines.size && lines[i].isNotBlank() && !isBlockStart(lines[i])) {
                        if (buf.isNotEmpty()) buf.append(' ')
                        buf.append(lines[i].trim())
                        i++
                    }
                    if (buf.isNotEmpty()) out.append("<p>").append(inline(buf.toString())).append("</p>\n")
                }
            }
        }
        return out.toString()
    }

    private fun isBlockStart(line: String): Boolean =
        isHorizontalRule(line) || headingLevel(line) > 0 ||
            line.trimStart().startsWith(">") || line.trimStart().startsWith("```") ||
            unorderedMarker(line) != null || orderedMarker(line) != null

    private fun headingLevel(line: String): Int {
        val s = line.trimStart()
        var n = 0
        while (n < s.length && s[n] == '#') n++
        return if (n in 1..6 && n < s.length && s[n] == ' ') n else 0
    }

    private fun isHorizontalRule(line: String): Boolean {
        val s = line.trim()
        return s.length >= 3 && (s.all { it == '-' } || s.all { it == '*' } || s.all { it == '_' })
    }

    private fun unorderedMarker(line: String): String? {
        val s = line.trimStart()
        return when {
            s.startsWith("- ") -> "- "
            s.startsWith("* ") -> "* "
            s.startsWith("+ ") -> "+ "
            else -> null
        }
    }

    private val orderedRegex = Regex("""^\d+\.\s+""")
    private fun orderedMarker(line: String): String? =
        orderedRegex.find(line.trimStart())?.value

    private val codeSpan = Regex("`([^`]+)`")
    private val imageRegex = Regex("""!\[([^\]]*)\]\([^)]*\)""")
    private val linkRegex = Regex("""\[([^\]]+)\]\(([^)\s]+)\)""")
    private val boldStar = Regex("""\*\*([^*]+)\*\*""")
    private val boldUnderscore = Regex("""__([^_]+)__""")
    private val italicStar = Regex("""\*([^*]+)\*""")
    private val italicUnderscore = Regex("""(?<![A-Za-z0-9])_([^_]+)_(?![A-Za-z0-9])""")

    /**
     * Escape [text], then apply emphasis only to the stretches *between* inline
     * code spans — the code spans are emitted verbatim so their contents never
     * get mangled by the `*`/`_` passes. Splitting this way avoids any sentinel
     * placeholder that could collide with real document text.
     */
    private fun inline(text: String): String {
        val escaped = escape(text)
        val sb = StringBuilder()
        var last = 0
        for (m in codeSpan.findAll(escaped)) {
            sb.append(emphasize(escaped.substring(last, m.range.first)))
            sb.append("<code>").append(m.groupValues[1]).append("</code>")
            last = m.range.last + 1
        }
        sb.append(emphasize(escaped.substring(last)))
        return sb.toString()
    }

    private fun emphasize(s: String): String {
        var r = imageRegex.replace(s) { it.groupValues[1] } // images → alt text
        r = linkRegex.replace(r) { m ->
            val label = m.groupValues[1]
            val href = sanitizeHref(m.groupValues[2])
            if (href == null) label else "<a href=\"$href\">$label</a>"
        }
        r = boldStar.replace(r) { "<strong>${it.groupValues[1]}</strong>" }
        r = boldUnderscore.replace(r) { "<strong>${it.groupValues[1]}</strong>" }
        r = italicStar.replace(r) { "<em>${it.groupValues[1]}</em>" }
        r = italicUnderscore.replace(r) { "<em>${it.groupValues[1]}</em>" }
        return r
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** Drop links whose scheme could execute; the WebView disables JS anyway. */
    private fun sanitizeHref(raw: String): String? {
        val lower = raw.trim().lowercase()
        return if (lower.startsWith("javascript:") || lower.startsWith("vbscript:") || lower.startsWith("data:")) {
            null
        } else {
            raw
        }
    }
}
