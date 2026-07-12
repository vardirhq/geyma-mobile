package dev.madsens.geyma.files

/**
 * Which built-in viewer, if any, can render a file inside Geyma — so that
 * tapping a file keeps you in the app instead of bouncing out to a chooser.
 * Pure Kotlin (no Android imports) so it stays unit-testable.
 */
enum class ViewerKind { IMAGE, VIDEO, AUDIO, PDF, TEXT, NONE }

object InAppViewer {
    /**
     * The largest file we'll pull fully into memory for the text viewer. Bigger
     * text-ish files fall back to an external app rather than risk an OOM.
     */
    const val MAX_TEXT_BYTES = 4L * 1024 * 1024 // 4 MB

    /**
     * Pick the in-app viewer for [name]. [sizeBytes] gates the text viewer so we
     * never try to load a huge (or unknown-binary) file into a string; pass 0 to
     * skip the size check (e.g. when the size isn't known yet).
     */
    fun kindFor(name: String, sizeBytes: Long = 0L): ViewerKind {
        val ext = name.substringAfterLast('.', "").lowercase()
        // SVG is XML under the hood; without an SVG decoder we show it as text.
        if (ext == "pdf") return ViewerKind.PDF
        if (ext == "svg") return textIfSmall(sizeBytes)
        return when (FileKind.ofName(name, isDir = false)) {
            FileKind.IMAGE -> ViewerKind.IMAGE
            FileKind.VIDEO -> ViewerKind.VIDEO
            FileKind.AUDIO -> ViewerKind.AUDIO
            FileKind.TEXT, FileKind.CODE -> textIfSmall(sizeBytes)
            else -> ViewerKind.NONE // non-PDF documents, archives, apps → external
        }
    }

    /** True when Geyma can open [name] itself without handing off to another app. */
    fun canView(name: String, sizeBytes: Long = 0L): Boolean =
        kindFor(name, sizeBytes) != ViewerKind.NONE

    private fun textIfSmall(sizeBytes: Long): ViewerKind =
        if (sizeBytes in 0..MAX_TEXT_BYTES) ViewerKind.TEXT else ViewerKind.NONE
}
