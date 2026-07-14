package dev.madsens.geyma.files

/**
 * Pure decisions and text-shaping for OCR search — which images are worth
 * feeding to the recognizer, and how to show a match. No Android, no ML Kit
 * here, so the rules stay unit-tested; the actual recognition lives in
 * OcrIndexer.
 */
object Ocr {
    /**
     * Raster formats the on-device recognizer can decode. Vector (svg) and
     * animated (gif) formats are excluded — there's no still frame of text to
     * read — as are exotic camera formats that won't decode to a bitmap.
     */
    private val ocrableExt = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif")

    /** Skip anything larger than this — a screenshot is tiny; a 30 MB scan isn't worth it. */
    const val MAX_BYTES = 25L * 1024 * 1024

    fun canIndex(name: String, size: Long): Boolean {
        if (size <= 0 || size > MAX_BYTES) return false
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in ocrableExt
    }

    /**
     * A short, single-line excerpt of [text] centered on the first match of
     * [query], with ellipses where it was clipped — what a search result shows
     * beneath the file name.
     */
    fun snippet(text: String, query: String, radius: Int = 40): String {
        val flat = text.replace(Regex("\\s+"), " ").trim()
        val q = query.trim()
        if (flat.isEmpty()) return ""
        val idx = if (q.isEmpty()) -1 else flat.indexOf(q, ignoreCase = true)
        if (idx < 0) {
            return if (flat.length > radius * 2) flat.take(radius * 2) + "…" else flat
        }
        val start = (idx - radius).coerceAtLeast(0)
        val end = (idx + q.length + radius).coerceAtMost(flat.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < flat.length) "…" else ""
        return prefix + flat.substring(start, end) + suffix
    }
}
