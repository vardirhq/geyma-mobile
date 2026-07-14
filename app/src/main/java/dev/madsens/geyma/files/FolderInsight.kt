package dev.madsens.geyma.files

/**
 * A plain-language read of what a folder actually holds — the "explain this
 * folder" answer people wish a file manager could give when they open a
 * mystery directory. [sentence] is the one-liner; the tallies back it up.
 *
 * Pure Kotlin (no Android, no filesystem) so it stays unit-tested: it works
 * only from the [Entry] rows the browser already has in hand.
 */
data class FolderDigest(
    val sentence: String,
    val totalItems: Int,
    val fileCount: Int,
    val folderCount: Int,
    val totalBytes: Long,
)

object FolderInsight {

    /**
     * Compose a digest of [entries] — the direct children of one folder. Leads
     * with the dominant file kinds ("Mostly 40 photos and 12 PDFs"), then the
     * subfolder count, total size and how recent the newest item is.
     */
    fun describe(entries: List<Entry>, nowMs: Long = System.currentTimeMillis()): FolderDigest {
        val files = entries.filter { !it.isDir }
        val folders = entries.filter { it.isDir }
        val totalBytes = files.sumOf { it.size }

        if (entries.isEmpty()) {
            return FolderDigest("This folder is empty.", 0, 0, 0, 0)
        }

        val byKind = files.groupingBy { it.kind }.eachCount()
            .entries.sortedByDescending { it.value }

        val what: String = when {
            files.isEmpty() ->
                "${folders.size} subfolder${plural(folders.size)} and no loose files"
            else -> {
                val listed = byKind.take(2).joinToString(" and ") { (kind, n) -> "$n ${noun(kind, n)}" }
                val dominant = files.size >= 3 && byKind.first().value * 100 / files.size >= 60
                val tail = if (byKind.size > 2) ", plus other files" else ""
                (if (dominant) "Mostly " else "") + listed + tail
            }
        }

        val newest = entries.maxOfOrNull { it.modifiedMs }.takeIf { it != null && it > 0 }

        val sentence = buildString {
            append(what)
            if (files.isNotEmpty() && folders.isNotEmpty()) {
                append(", in ${folders.size} subfolder${plural(folders.size)}")
            }
            if (totalBytes > 0) append(", ${PathUtils.humanSize(totalBytes)} in all")
            newest?.let { append(", newest ${ago(nowMs, it)}") }
            append(".")
        }.replaceFirstChar { it.uppercase() }

        return FolderDigest(
            sentence = sentence,
            totalItems = entries.size,
            fileCount = files.size,
            folderCount = folders.size,
            totalBytes = totalBytes,
        )
    }

    /** A friendly plural noun for a file kind, e.g. image → "photos". */
    private fun noun(kind: String, count: Int): String {
        val one = count == 1
        return when (kind) {
            FileKind.IMAGE -> if (one) "photo" else "photos"
            FileKind.VIDEO -> if (one) "video" else "videos"
            FileKind.AUDIO -> if (one) "audio file" else "audio files"
            FileKind.DOCUMENT -> if (one) "document" else "documents"
            FileKind.TEXT -> if (one) "text file" else "text files"
            FileKind.CODE -> if (one) "code file" else "code files"
            FileKind.ARCHIVE -> if (one) "archive" else "archives"
            FileKind.APP -> if (one) "app" else "apps"
            else -> if (one) "file" else "files"
        }
    }

    private fun plural(n: Int) = if (n == 1) "" else "s"

    /** A coarse, deterministic "how long ago" — pure, so it is unit-tested. */
    private fun ago(nowMs: Long, thenMs: Long): String {
        val d = nowMs - thenMs
        val min = 60_000L
        val hour = 60 * min
        val day = 24 * hour
        val month = 30 * day
        val year = 365 * day
        return when {
            d < 0 -> "just now"
            d < hour -> "just now"
            d < day -> "today"
            d < 2 * day -> "yesterday"
            d < month -> "${d / day} days ago"
            d < year -> "${d / month} month${plural((d / month).toInt())} ago"
            else -> "${d / year} year${plural((d / year).toInt())} ago"
        }
    }
}
