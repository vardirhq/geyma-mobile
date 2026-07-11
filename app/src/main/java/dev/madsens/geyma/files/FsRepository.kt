package dev.madsens.geyma.files

import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.Star
import dev.madsens.geyma.data.TrashEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

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
