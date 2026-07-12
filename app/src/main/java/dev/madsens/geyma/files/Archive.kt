package dev.madsens.geyma.files

/** One raw record read from a zip container: its internal name and (file) size. */
data class ArchiveRecord(val name: String, val size: Long)

/** A single row when browsing inside an archive — a file or a (possibly implicit) folder. */
data class ArchiveNode(
    val name: String,
    /** Full internal path; folders carry a trailing slash, files do not. */
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val kind: String,
)

/** Which archive formats Geyma can browse in-app (everything readable via java.util.zip). */
object ArchiveSupport {
    private val zipLike = setOf("zip", "jar", "gyset")

    fun canBrowse(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in zipLike
}

/**
 * Turns a flat list of zip entries into a navigable tree. Zip files often omit
 * explicit directory entries, so intermediate folders are synthesized from the
 * paths of the files they contain. Pure Kotlin so the traversal is unit-tested.
 */
object ArchiveTree {
    /**
     * Immediate children of [dir] (use "" for the archive root, otherwise a
     * folder path with a trailing slash such as "docs/" or "docs/img/").
     * Folders sort before files, each alphabetically (case-insensitive).
     */
    fun childrenOf(records: List<ArchiveRecord>, dir: String): List<ArchiveNode> {
        val prefix = if (dir.isEmpty()) "" else dir.trimEnd('/') + "/"
        // Keyed by "name/" for folders and "name" for files so the two never collide.
        val children = LinkedHashMap<String, ArchiveNode>()
        for (record in records) {
            val explicitDir = record.name.endsWith("/")
            val clean = record.name.trimEnd('/')
            if (clean.isEmpty() || !clean.startsWith(prefix)) continue
            val rest = clean.substring(prefix.length)
            if (rest.isEmpty()) continue
            val slash = rest.indexOf('/')
            if (slash >= 0) {
                val childName = rest.substring(0, slash)
                children.putIfAbsent(
                    "$childName/",
                    ArchiveNode(childName, "$prefix$childName/", isDir = true, size = 0, kind = FileKind.FOLDER),
                )
            } else if (explicitDir) {
                children.putIfAbsent(
                    "$rest/",
                    ArchiveNode(rest, "$prefix$rest/", isDir = true, size = 0, kind = FileKind.FOLDER),
                )
            } else {
                children["$rest"] = ArchiveNode(
                    rest, "$prefix$rest", isDir = false, size = record.size,
                    kind = FileKind.ofName(rest, isDir = false),
                )
            }
        }
        return children.values.sortedWith(
            compareBy({ !it.isDir }, { it.name.lowercase() }),
        )
    }
}
