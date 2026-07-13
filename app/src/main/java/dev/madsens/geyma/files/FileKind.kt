package dev.madsens.geyma.files

/**
 * Extension → kind classification, mirroring the desktop app's kind palette
 * (folder / document / text / code / image / video / audio / archive / app).
 */
object FileKind {
    const val FOLDER = "folder"
    const val DOCUMENT = "document"
    const val TEXT = "text"
    const val CODE = "code"
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val AUDIO = "audio"
    const val ARCHIVE = "archive"
    const val APP = "app"

    val LABELS = mapOf(
        FOLDER to "Folder", DOCUMENT to "Document", TEXT to "Text", CODE to "Code",
        IMAGE to "Image", VIDEO to "Video", AUDIO to "Audio", ARCHIVE to "Archive", APP to "Application",
    )

    private val documentExt = setOf("pdf", "doc", "docx", "odt", "rtf", "xls", "xlsx", "ods", "ppt", "pptx", "odp", "epub", "mobi")
    private val textExt = setOf("txt", "md", "markdown", "log", "csv", "tsv", "ini", "cfg", "conf", "nfo", "srt", "vtt")
    private val codeExt = setOf(
        "kt", "kts", "java", "js", "jsx", "ts", "tsx", "py", "rs", "go", "c", "h", "cpp", "hpp", "cs",
        "rb", "php", "swift", "sh", "bash", "zsh", "html", "htm", "css", "scss", "json", "xml", "yml",
        "yaml", "toml", "sql", "gradle", "lua", "dart", "vue", "svelte",
    )
    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif", "avif", "ico", "tiff", "raw", "dng")
    private val videoExt = setOf("mp4", "mkv", "webm", "avi", "mov", "wmv", "flv", "m4v", "3gp", "ts", "mpg", "mpeg")
    private val audioExt = setOf("mp3", "wav", "flac", "ogg", "oga", "opus", "m4a", "aac", "wma", "mid", "midi", "amr")
    private val archiveExt = setOf("zip", "tar", "gz", "tgz", "bz2", "xz", "7z", "rar", "zst", "gyset", "jar")
    private val appExt = setOf("apk", "apks", "aab", "xapk")

    fun ofExtension(ext: String): String = when (ext.lowercase()) {
        in documentExt -> DOCUMENT
        in textExt -> TEXT
        in codeExt -> CODE
        in imageExt -> IMAGE
        in videoExt -> VIDEO
        in audioExt -> AUDIO
        in archiveExt -> ARCHIVE
        in appExt -> APP
        else -> TEXT
    }

    fun ofName(name: String, isDir: Boolean): String {
        if (isDir) return FOLDER
        val ext = name.substringAfterLast('.', "")
        return if (ext.isEmpty()) TEXT else ofExtension(ext)
    }

    /**
     * True when [ext] appears in one of the kind tables — i.e. it's a format we
     * actually recognize, not something [ofExtension] merely defaulted to TEXT.
     * Lets callers tell a genuine text/code file apart from an unknown blob.
     */
    fun isKnownExtension(ext: String): Boolean {
        val e = ext.lowercase()
        return e in documentExt || e in textExt || e in codeExt || e in imageExt ||
            e in videoExt || e in audioExt || e in archiveExt || e in appExt
    }

    fun mimeOf(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            ext in imageExt -> if (ext == "svg") "image/svg+xml" else "image/$ext"
            ext in videoExt -> "video/*"
            ext in audioExt -> "audio/*"
            ext == "pdf" -> "application/pdf"
            ext == "apk" -> "application/vnd.android.package-archive"
            ext == "zip" -> "application/zip"
            ext in textExt || ext in codeExt -> "text/plain"
            else -> "*/*"
        }
    }
}
