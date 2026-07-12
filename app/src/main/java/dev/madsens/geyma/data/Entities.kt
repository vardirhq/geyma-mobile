package dev.madsens.geyma.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One entry in the memory journal — Geyma's core "to keep, to guard" record. */
@Entity(tableName = "events", indices = [Index("path"), Index("whenMs"), Index("prevPath")])
data class FileEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Path after the event. */
    val path: String,
    /** One of EventActions. */
    val action: String,
    /** Free-text detail shown in timelines ("from Downloads", new name, …). */
    val detail: String? = null,
    /** Path immediately before the event, for rename/move/trash/restore. */
    val prevPath: String? = null,
    val isDir: Boolean = false,
    val whenMs: Long = System.currentTimeMillis(),
)

object EventActions {
    const val CREATED = "created"
    const val RENAMED = "renamed"
    const val MOVED = "moved"
    const val COPIED = "copied"
    const val TRASHED = "trashed"
    const val RESTORED = "restored"
    const val DELETED = "deleted"
    const val STARRED = "starred"
    const val UNSTARRED = "unstarred"

    /** A file appeared in a watched folder from outside Geyma (download, share, screenshot…). */
    const val ARRIVED = "arrived"

    /** The user opened a file through Geyma. */
    const val OPENED = "opened"
}

/**
 * Ledger of files Geyma has noticed in watched folders. firstSeenMs is the
 * arrival time; lastOpenedMs stays null until the user opens the file through
 * Geyma — "arrived but never opened" is what the Sweep screen keys on.
 */
@Entity(tableName = "seen_files")
data class SeenFile(
    @PrimaryKey val path: String,
    val firstSeenMs: Long,
    val lastOpenedMs: Long? = null,
)

@Entity(tableName = "stars")
data class Star(
    @PrimaryKey val path: String,
    val whenMs: Long = System.currentTimeMillis(),
)

/** A file parked in Geyma's recoverable trash, remembering where it came from. */
@Entity(tableName = "trash_entries")
data class TrashEntry(
    @PrimaryKey val id: String,
    /** Where the payload currently sits inside the trash directory. */
    val trashedPath: String,
    val originalPath: String,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val whenMs: Long = System.currentTimeMillis(),
)

/** Playlist-like collection of file references (the mobile take on working sets). */
@Entity(tableName = "sets")
data class WorkingSet(
    @PrimaryKey val id: String,
    val name: String,
    val note: String? = null,
    val createdMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "set_items",
    primaryKeys = ["setId", "path"],
    indices = [Index("path")],
)
data class SetItem(
    val setId: String,
    val path: String,
    val addedMs: Long = System.currentTimeMillis(),
)

/**
 * A file the user asked Geyma to resurface later — a memory with an alarm on
 * it. It reappears on Home once [dueMs] passes; the reference is kept synced
 * through moves and renames like everything else Geyma guards.
 */
@Entity(tableName = "revisits")
data class Revisit(
    @PrimaryKey val path: String,
    val dueMs: Long,
    val note: String? = null,
    val createdMs: Long = System.currentTimeMillis(),
)
