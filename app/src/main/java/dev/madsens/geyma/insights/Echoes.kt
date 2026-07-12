package dev.madsens.geyma.insights

/** A file reduced to what makes it identical to another: its size and content hash. */
data class FileFingerprint(
    val path: String,
    val name: String,
    val size: Long,
    val hash: String,
    val kind: String,
    val modifiedMs: Long,
)

/**
 * A set of byte-for-byte identical files. The oldest is treated as the
 * original; the rest are echoes whose bytes are redundant.
 */
data class DuplicateGroup(
    val size: Long,
    val kind: String,
    val files: List<FileFingerprint>,
) {
    /** The keeper — the earliest-modified copy, the one Echoes leaves alone. */
    val original: FileFingerprint get() = files.first()

    /** The redundant copies, oldest first. */
    val echoes: List<FileFingerprint> get() = files.drop(1)

    /** Bytes that would come back if every echo but the original were cleared. */
    val reclaimable: Long get() = size * echoes.size
}

/**
 * Groups identical files into duplicate sets. Pure — the filesystem walk and
 * hashing happen in the repository; the grouping and ranking are tested here.
 */
object Echoes {

    /**
     * Files that share both size and content hash form a group. Zero-byte files
     * are ignored (every empty file hashes alike but reclaims nothing). Groups
     * come back with the most reclaimable space first, each file oldest-first.
     */
    fun group(files: List<FileFingerprint>): List<DuplicateGroup> =
        files
            .filter { it.size > 0 }
            .groupBy { it.size to it.hash }
            .values
            .filter { it.size > 1 }
            .map { g ->
                val sorted = g.sortedBy { it.modifiedMs }
                DuplicateGroup(size = sorted.first().size, kind = sorted.first().kind, files = sorted)
            }
            .sortedByDescending { it.reclaimable }

    fun totalReclaimable(groups: List<DuplicateGroup>): Long = groups.sumOf { it.reclaimable }
}
