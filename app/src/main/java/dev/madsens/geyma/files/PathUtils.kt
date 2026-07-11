package dev.madsens.geyma.files

import java.util.Locale

/** Pure path/format helpers, kept free of Android imports so JVM tests cover them. */
object PathUtils {

    fun nameOf(path: String): String = path.trimEnd('/').substringAfterLast('/')

    fun parentOf(path: String): String? {
        val trimmed = path.trimEnd('/')
        val idx = trimmed.lastIndexOf('/')
        if (idx <= 0) return if (trimmed.isEmpty() || trimmed == "/") null else "/"
        return trimmed.substring(0, idx)
    }

    fun join(dir: String, name: String): String = dir.trimEnd('/') + "/" + name

    fun isAncestorOf(ancestor: String, path: String): Boolean {
        val a = ancestor.trimEnd('/')
        return path == a || path.startsWith("$a/")
    }

    /** Rewrites [path] after [oldBase] moved to [newBase]; null when unrelated. */
    fun rebase(path: String, oldBase: String, newBase: String): String? {
        val o = oldBase.trimEnd('/')
        if (path == o) return newBase
        if (!path.startsWith("$o/")) return null
        return newBase.trimEnd('/') + path.substring(o.length)
    }

    /**
     * Breadcrumb segments from [root] down to [path]: (label, absolutePath) pairs.
     * The root segment carries [rootLabel].
     */
    fun breadcrumbs(path: String, root: String, rootLabel: String): List<Pair<String, String>> {
        val r = root.trimEnd('/')
        val crumbs = mutableListOf(rootLabel to r)
        if (!isAncestorOf(r, path) || path == r) return crumbs
        var acc = r
        for (seg in path.removePrefix("$r/").split('/')) {
            if (seg.isEmpty()) continue
            acc = "$acc/$seg"
            crumbs.add(seg to acc)
        }
        return crumbs
    }

    /**
     * First name not present in [existing], counting up "name (2).ext",
     * "name (3).ext"… like the desktop duplicate handling.
     */
    fun uniqueChildName(existing: Set<String>, desired: String): String {
        if (desired !in existing) return desired
        val dot = desired.lastIndexOf('.')
        val (stem, ext) = if (dot > 0) desired.substring(0, dot) to desired.substring(dot) else desired to ""
        var n = 2
        while (true) {
            val candidate = "$stem ($n)$ext"
            if (candidate !in existing) return candidate
            n++
        }
    }

    fun humanSize(bytes: Long): String {
        if (bytes < 0) return "—"
        if (bytes < 1024) return "$bytes B"
        var v = bytes.toDouble()
        val units = listOf("KB", "MB", "GB", "TB")
        var u = -1
        while (v >= 1024 && u < units.size - 1) {
            v /= 1024
            u++
        }
        return if (v >= 100) String.format(Locale.US, "%.0f %s", v, units[u])
        else String.format(Locale.US, "%.1f %s", v, units[u])
    }
}
