package dev.madsens.geyma.files

/**
 * Which built-in viewer, if any, can render a file inside Geyma — so that
 * tapping a file keeps you in the app instead of bouncing out to a chooser.
 * Pure Kotlin (no Android imports) so it stays unit-testable.
 */
enum class ViewerKind { IMAGE, VIDEO, AUDIO, PDF, WEB, TEXT, NONE }

object InAppViewer {
    /**
     * The largest file we'll pull into memory for the text/web viewers. Bigger
     * *known* text files are still shown (the text viewer reads only this much
     * and flags the rest as truncated); unknown/binary blobs above it fall back
     * to an external app rather than risk dumping garbage into the text pane.
     */
    const val MAX_TEXT_BYTES = 4L * 1024 * 1024 // 4 MB

    /** Markup we render as a formatted page rather than showing as source. */
    private val renderedExt = setOf("html", "htm", "xhtml", "md", "markdown")

    /**
     * Pick the in-app viewer for [name]. [sizeBytes] gates the WEB viewer and the
     * text viewer for unknown extensions; pass 0 to skip the size check (e.g. when
     * the size isn't known yet).
     */
    fun kindFor(name: String, sizeBytes: Long = 0L): ViewerKind {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext == "pdf") return ViewerKind.PDF
        // HTML/Markdown render as a page, as long as they fit in memory.
        if (ext in renderedExt && withinTextCap(sizeBytes)) return ViewerKind.WEB
        return when (FileKind.ofName(name, isDir = false)) {
            FileKind.IMAGE -> ViewerKind.IMAGE // includes SVG, rendered via Coil's SVG decoder
            FileKind.VIDEO -> ViewerKind.VIDEO
            FileKind.AUDIO -> ViewerKind.AUDIO
            FileKind.TEXT, FileKind.CODE -> textKind(ext, sizeBytes)
            else -> ViewerKind.NONE // non-PDF documents, archives, apps → external
        }
    }

    /** True when Geyma can open [name] itself without handing off to another app. */
    fun canView(name: String, sizeBytes: Long = 0L): Boolean =
        kindFor(name, sizeBytes) != ViewerKind.NONE

    /**
     * A *recognized* text/code extension (`.log`, `.csv`, `.json`, `.kt`, …) stays
     * viewable even when large — the text viewer truncates it — so a big log or
     * export no longer bounces out. An *unknown* extension (which [FileKind] lumps
     * into TEXT by default) is only shown when small, so a large binary blob isn't
     * poured into the text pane.
     */
    private fun textKind(ext: String, sizeBytes: Long): ViewerKind =
        if (FileKind.isKnownExtension(ext) || withinTextCap(sizeBytes)) ViewerKind.TEXT else ViewerKind.NONE

    private fun withinTextCap(sizeBytes: Long): Boolean = sizeBytes in 0..MAX_TEXT_BYTES
}
