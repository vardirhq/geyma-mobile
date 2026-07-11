package dev.madsens.geyma.files

import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.SeenFile
import dev.madsens.geyma.data.Star
import dev.madsens.geyma.data.TrashEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class Entry(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val modifiedMs: Long,
    val kind: String,
    val childCount: Int? = null,
    val starred: Boolean = false,
    val hidden: Boolean = false,
)

/** A faint marker where a file used to be — Geyma's ghost trail. */
data class GhostTrail(
    val name: String,
    val action: String,
    val fromPath: String,
    val toPath: String,
    val whenMs: Long,
)

/** One result from a journal-wide search — a file as Geyma last knew it. */
data class SearchHit(
    val name: String,
    val path: String,
    val exists: Boolean,
    val trashed: Boolean,
    val lastAction: String,
    val lastWhenMs: Long,
    val prevPath: String?,
)

/** A neglected arrival surfaced on the Sweep screen. */
data class SweepItem(
    val name: String,
    val path: String,
    val size: Long,
    val firstSeenMs: Long,
    val ageDays: Int,
    val kind: String,
)

/**
 * Every mutation goes through here so the journal, stars, sets, and trash
 * registry stay consistent — the mobile equivalent of the desktop's Rust
 * fsops + journal slice pairing.
 */
class FsRepository(private val db: GeymaDb) {

    private val events get() = db.events()
    private val stars get() = db.stars()
    private val trash get() = db.trash()
    private val sets get() = db.sets()
    private val seen get() = db.seen()

    val trashDir: File = File(StorageRoots.primaryPath(), ".geyma/trash")

    suspend fun listDir(path: String, showHidden: Boolean): List<Entry> = withContext(Dispatchers.IO) {
        val dir = File(path)
        val children = dir.listFiles() ?: return@withContext emptyList()
        val starredPaths = stars.allPaths().toHashSet()
        children
            .filter { showHidden || !it.name.startsWith(".") }
            .map { f ->
                val isDir = f.isDirectory
                Entry(
                    name = f.name,
                    path = f.absolutePath,
                    isDir = isDir,
                    size = if (isDir) 0 else f.length(),
                    modifiedMs = f.lastModified(),
                    kind = FileKind.ofName(f.name, isDir),
                    childCount = if (isDir) f.list()?.size else null,
                    starred = f.absolutePath in starredPaths,
                    hidden = f.name.startsWith("."),
                )
            }
    }

    suspend fun ghostsFor(dir: String, sinceMs: Long): List<GhostTrail> = withContext(Dispatchers.IO) {
        events.departuresFrom(dir.trimEnd('/'), sinceMs)
            .filter { it.prevPath != null && !File(it.prevPath).exists() }
            .distinctBy { it.prevPath }
            .map { e ->
                GhostTrail(
                    name = PathUtils.nameOf(e.prevPath!!),
                    action = e.action,
                    fromPath = e.prevPath,
                    toPath = e.path,
                    whenMs = e.whenMs,
                )
            }
    }

    suspend fun historyFor(path: String): List<FileEvent> = events.forPath(path)

    /**
     * Journal-wide search: matches live files by name plus anything Geyma
     * remembers — trashed, moved or renamed files — collapsed to one hit per
     * file. This is "find my file" over the past, not just the current tree.
     */
    suspend fun searchJournal(query: String): List<SearchHit> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val byFile = LinkedHashMap<String, SearchHit>()
        for (e in events.search(q)) {
            val gone = e.action == EventActions.TRASHED || e.action == EventActions.DELETED
            // For trashed/deleted files the live path is an internal trash path
            // with a UUID prefix — the file the user is looking for is the origin.
            val identity = if (gone) (e.prevPath ?: e.path) else e.path
            val name = PathUtils.nameOf(identity)
            // Skip journal-internal churn that doesn't name a real file match.
            if (!name.contains(q, ignoreCase = true) &&
                !PathUtils.nameOf(e.path).contains(q, ignoreCase = true)
            ) {
                continue
            }
            if (identity in byFile) continue
            // A trashed file still physically exists (in trash); a deleted one doesn't.
            val exists = if (e.action == EventActions.DELETED) false else File(e.path).exists()
            byFile[identity] = SearchHit(
                name = name,
                path = if (gone) identity else e.path,
                exists = exists && !gone,
                trashed = gone,
                lastAction = e.action,
                lastWhenMs = e.whenMs,
                prevPath = e.prevPath,
            )
        }
        byFile.values.sortedByDescending { it.lastWhenMs }
    }

    /** Note that the user opened a file, both in the journal and the seen-ledger. */
    suspend fun recordOpen(path: String) = withContext(Dispatchers.IO) {
        seen.markOpened(path, System.currentTimeMillis())
        log(EventActions.OPENED, path, isDir = false)
    }

    /**
     * Record a file that arrived from outside the browser — a share-sheet drop or
     * a live folder-watch hit — as a journal arrival plus a seen-ledger row. It
     * counts as already opened, since the user chose to bring it in.
     */
    suspend fun recordArrival(path: String, detail: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        seen.add(SeenFile(path = path, firstSeenMs = now, lastOpenedMs = now))
        log(EventActions.ARRIVED, path, detail = detail, isDir = false)
    }

    /**
     * Files that arrived from outside Geyma and were never opened through it,
     * ranked by neglect (oldest, largest first). Powers the Sweep screen. Stale
     * ledger rows whose file is gone are cleaned up as a side effect.
     */
    suspend fun sweepCandidates(): List<SweepItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val out = ArrayList<SweepItem>()
        for (row in seen.neverOpened()) {
            val f = File(row.path)
            if (!f.exists() || f.isDirectory) {
                seen.remove(row.path)
                continue
            }
            out.add(
                SweepItem(
                    name = f.name,
                    path = row.path,
                    size = f.length(),
                    firstSeenMs = row.firstSeenMs,
                    ageDays = ((now - row.firstSeenMs) / TimeUnit.DAYS.toMillis(1)).toInt(),
                    kind = FileKind.ofName(f.name, false),
                ),
            )
        }
        // Most-neglected first: old files bubble up, ties broken by size.
        out.sortedWith(compareByDescending<SweepItem> { it.firstSeenMs.let { s -> now - s } }.thenByDescending { it.size })
    }

    /**
     * Reconcile watched folders against the seen-ledger. On the very first run
     * ([seeded] false) it silently records what's already there — no user wants
     * a thousand "arrived" events on install. After that, any genuinely new file
     * becomes an arrival in the journal. Returns how many arrivals were logged.
     */
    suspend fun reconcileArrivals(watchedDirs: List<String>, seeded: Boolean): Int = withContext(Dispatchers.IO) {
        val known = seen.allPaths().toHashSet()
        val now = System.currentTimeMillis()
        var arrivals = 0
        for (dirPath in watchedDirs) {
            val dir = File(dirPath)
            val kids = dir.listFiles() ?: continue
            for (f in kids) {
                if (f.isDirectory || f.name.startsWith(".")) continue
                val path = f.absolutePath
                if (path in known) continue
                known.add(path)
                if (seeded) {
                    // A genuine new arrival: journal it and leave it "unopened"
                    // so the Sweep screen can surface it later.
                    seen.add(SeenFile(path = path, firstSeenMs = now, lastOpenedMs = null))
                    log(
                        EventActions.ARRIVED, path,
                        detail = "in ${PathUtils.nameOf(dirPath)}", isDir = false,
                    )
                    arrivals++
                } else {
                    // First inventory: record what's already here as handled, so
                    // Geyma never proposes sweeping files that predate it.
                    seen.add(SeenFile(path = path, firstSeenMs = f.lastModified(), lastOpenedMs = f.lastModified()))
                }
            }
        }
        arrivals
    }

    suspend fun createFolder(parent: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(parent, uniqueNameIn(parent, name))
            if (!target.mkdirs()) throw IOException("Could not create ${target.name}")
            log(EventActions.CREATED, target.absolutePath, isDir = true)
            target.absolutePath
        }
    }

    suspend fun rename(path: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val src = File(path)
            val dst = File(src.parentFile, uniqueNameIn(src.parent ?: "/", newName))
            if (!src.renameTo(dst)) throw IOException("Could not rename ${src.name}")
            log(EventActions.RENAMED, dst.absolutePath, prevPath = path, detail = "from ${src.name}", isDir = dst.isDirectory)
            rebaseAll(path, dst.absolutePath)
            dst.absolutePath
        }
    }

    suspend fun move(paths: List<String>, destDir: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var moved = 0
            for (path in paths) {
                val src = File(path)
                if (!src.exists()) continue
                if (src.isDirectory && PathUtils.isAncestorOf(path, destDir)) {
                    throw IOException("Cannot move a folder into itself")
                }
                val dst = File(destDir, uniqueNameIn(destDir, src.name))
                moveFile(src, dst)
                log(
                    EventActions.MOVED, dst.absolutePath, prevPath = path,
                    detail = "from ${PathUtils.nameOf(src.parent ?: "/")}", isDir = dst.isDirectory,
                )
                rebaseAll(path, dst.absolutePath)
                moved++
            }
            moved
        }
    }

    suspend fun copy(paths: List<String>, destDir: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var copied = 0
            for (path in paths) {
                val src = File(path)
                if (!src.exists()) continue
                if (src.isDirectory && PathUtils.isAncestorOf(path, destDir)) {
                    throw IOException("Cannot copy a folder into itself")
                }
                val dst = File(destDir, uniqueNameIn(destDir, src.name))
                if (src.isDirectory) src.copyRecursively(dst) else src.copyTo(dst)
                log(
                    EventActions.COPIED, dst.absolutePath,
                    detail = "from ${PathUtils.nameOf(src.parent ?: "/")}", isDir = dst.isDirectory,
                )
                copied++
            }
            copied
        }
    }

    /** Move into Geyma's recoverable trash, remembering the origin. */
    suspend fun moveToTrash(paths: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (!trashDir.exists()) trashDir.mkdirs()
            var trashed = 0
            for (path in paths) {
                val src = File(path)
                if (!src.exists()) continue
                val id = UUID.randomUUID().toString()
                val dst = File(trashDir, "${id}__${src.name}")
                val size = if (src.isDirectory) 0 else src.length()
                val isDir = src.isDirectory
                moveFile(src, dst)
                trash.add(
                    TrashEntry(
                        id = id, trashedPath = dst.absolutePath, originalPath = path,
                        name = src.name, isDir = isDir, size = size,
                    ),
                )
                log(EventActions.TRASHED, dst.absolutePath, prevPath = path, detail = "to trash", isDir = isDir)
                stars.removeTree(path)
                seen.removeTree(path)
                trashed++
            }
            trashed
        }
    }

    suspend fun restore(entryId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val entry = trash.byId(entryId) ?: throw IOException("Trash entry missing")
            val src = File(entry.trashedPath)
            if (!src.exists()) {
                trash.remove(entryId)
                throw IOException("Trashed file is gone")
            }
            val parent = File(entry.originalPath).parentFile ?: throw IOException("No restore target")
            if (!parent.exists()) parent.mkdirs()
            val dst = File(parent, uniqueNameIn(parent.absolutePath, entry.name))
            moveFile(src, dst)
            trash.remove(entryId)
            log(EventActions.RESTORED, dst.absolutePath, prevPath = entry.trashedPath, detail = "from trash", isDir = dst.isDirectory)
            dst.absolutePath
        }
    }

    suspend fun deleteForever(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entry = trash.byId(entryId) ?: return@runCatching
            val f = File(entry.trashedPath)
            if (f.exists() && !f.deleteRecursively()) throw IOException("Could not delete ${entry.name}")
            trash.remove(entryId)
            log(EventActions.DELETED, entry.originalPath, detail = "permanently", isDir = entry.isDir)
        }
    }

    suspend fun setStarred(path: String, starred: Boolean) {
        if (starred) {
            stars.add(Star(path))
            log(EventActions.STARRED, path)
        } else {
            stars.remove(path)
            log(EventActions.UNSTARRED, path)
        }
    }

    suspend fun uniqueNameIn(dir: String, desired: String): String = withContext(Dispatchers.IO) {
        val existing = File(dir).list()?.toSet() ?: emptySet()
        PathUtils.uniqueChildName(existing, desired)
    }

    /** renameTo when possible, copy+delete across volumes. */
    private fun moveFile(src: File, dst: File) {
        if (src.renameTo(dst)) return
        if (src.isDirectory) {
            if (!src.copyRecursively(dst)) throw IOException("Could not copy ${src.name}")
            if (!src.deleteRecursively()) throw IOException("Moved but could not remove ${src.name}")
        } else {
            src.copyTo(dst)
            if (!src.delete()) throw IOException("Moved but could not remove ${src.name}")
        }
    }

    private suspend fun rebaseAll(oldBase: String, newBase: String) {
        events.rebasePaths(oldBase, newBase)
        stars.rebasePaths(oldBase, newBase)
        sets.rebasePaths(oldBase, newBase)
        seen.rebasePaths(oldBase, newBase)
    }

    private suspend fun log(
        action: String,
        path: String,
        prevPath: String? = null,
        detail: String? = null,
        isDir: Boolean = false,
    ) {
        events.insert(FileEvent(path = path, action = action, detail = detail, prevPath = prevPath, isDir = isDir))
    }
}
