package dev.madsens.geyma.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: FileEvent)

    /** Per-file activity, capped like the desktop's 200-event timeline. */
    @Query("SELECT * FROM events WHERE path = :path OR prevPath = :path ORDER BY whenMs DESC LIMIT 200")
    suspend fun forPath(path: String): List<FileEvent>

    @Query("SELECT * FROM events ORDER BY whenMs DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<FileEvent>>

    /** Every event, oldest first — used to export the journal for desktop handoff. */
    @Query("SELECT * FROM events ORDER BY whenMs ASC")
    suspend fun allChronological(): List<FileEvent>

    /**
     * Journal-wide search across everything Geyma remembers: current names and
     * the names files carried before a move or rename. One row per file (newest
     * event wins) so a file that moved three times shows up once.
     */
    @Query(
        "SELECT * FROM events WHERE " +
            "path LIKE '%' || :q || '%' ESCAPE '\\' OR prevPath LIKE '%' || :q || '%' ESCAPE '\\' " +
            "OR detail LIKE '%' || :q || '%' ESCAPE '\\' " +
            "ORDER BY whenMs DESC LIMIT 400",
    )
    suspend fun search(q: String): List<FileEvent>

    /** Recent arrivals — files that appeared from outside Geyma, newest first. */
    @Query("SELECT * FROM events WHERE action = 'arrived' ORDER BY whenMs DESC LIMIT :limit")
    fun recentArrivals(limit: Int): Flow<List<FileEvent>>

    /**
     * Ghost trails: departures (move/rename/trash) whose origin sat directly in
     * [dir], newer than [sinceMs]. prevPath NOT LIKE dir/%/% keeps it to direct
     * children.
     */
    @Query(
        "SELECT * FROM events WHERE action IN ('moved','renamed','trashed') " +
            "AND prevPath LIKE :dirLike || '/%' ESCAPE '\\' AND prevPath NOT LIKE :dirLike || '/%/%' ESCAPE '\\' " +
            "AND whenMs > :sinceMs ORDER BY whenMs DESC LIMIT 30",
    )
    suspend fun departuresFrom(dirLike: String, sinceMs: Long): List<FileEvent>

    /** Rewrite journal paths after a move/rename so timelines follow the file. */
    @Query(
        "UPDATE events SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    @Query("DELETE FROM events WHERE whenMs < :beforeMs")
    suspend fun prune(beforeMs: Long)

    /** Everything remembered since [sinceMs], newest first — feeds the Almanac. */
    @Query("SELECT * FROM events WHERE whenMs >= :sinceMs ORDER BY whenMs DESC")
    suspend fun since(sinceMs: Long): List<FileEvent>
}

@Dao
interface StarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(star: Star)

    @Query("DELETE FROM stars WHERE path = :path")
    suspend fun remove(path: String)

    @Query("SELECT * FROM stars ORDER BY whenMs DESC")
    fun all(): Flow<List<Star>>

    @Query("SELECT path FROM stars")
    suspend fun allPaths(): List<String>

    @Query("SELECT * FROM stars ORDER BY whenMs ASC")
    suspend fun snapshot(): List<Star>

    @Query("SELECT EXISTS(SELECT 1 FROM stars WHERE path = :path)")
    suspend fun isStarred(path: String): Boolean

    @Query(
        "UPDATE OR REPLACE stars SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    @Query("DELETE FROM stars WHERE path = :path OR path LIKE :pathLike || '/%' ESCAPE '\\'")
    suspend fun removeTree(path: String, pathLike: String)
}

@Dao
interface TrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: TrashEntry)

    @Query("SELECT * FROM trash_entries ORDER BY whenMs DESC")
    fun all(): Flow<List<TrashEntry>>

    @Query("SELECT * FROM trash_entries WHERE id = :id")
    suspend fun byId(id: String): TrashEntry?

    @Query("DELETE FROM trash_entries WHERE id = :id")
    suspend fun remove(id: String)
}

@Dao
interface SeenDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(seen: SeenFile)

    @Query("SELECT path FROM seen_files")
    suspend fun allPaths(): List<String>

    @Query("SELECT * FROM seen_files WHERE path = :path")
    suspend fun byPath(path: String): SeenFile?

    @Query("UPDATE seen_files SET lastOpenedMs = :whenMs WHERE path = :path")
    suspend fun markOpened(path: String, whenMs: Long)

    /** Files noticed but never opened through Geyma, oldest arrivals first. */
    @Query("SELECT * FROM seen_files WHERE lastOpenedMs IS NULL ORDER BY firstSeenMs ASC")
    suspend fun neverOpened(): List<SeenFile>

    @Query("SELECT COUNT(*) FROM seen_files WHERE lastOpenedMs IS NULL")
    suspend fun neverOpenedCount(): Int

    @Query("DELETE FROM seen_files WHERE path = :path")
    suspend fun remove(path: String)

    @Query(
        "UPDATE OR REPLACE seen_files SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    @Query("DELETE FROM seen_files WHERE path = :path OR path LIKE :pathLike || '/%' ESCAPE '\\'")
    suspend fun removeTree(path: String, pathLike: String)
}

@Dao
interface SetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSet(set: WorkingSet)

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun removeSet(id: String)

    @Query("DELETE FROM set_items WHERE setId = :id")
    suspend fun clearItems(id: String)

    @Query("SELECT * FROM sets ORDER BY createdMs DESC")
    fun all(): Flow<List<WorkingSet>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(item: SetItem)

    @Query("DELETE FROM set_items WHERE setId = :setId AND path = :path")
    suspend fun removeItem(setId: String, path: String)

    @Query("SELECT * FROM set_items WHERE setId = :setId ORDER BY addedMs DESC")
    fun items(setId: String): Flow<List<SetItem>>

    @Query("SELECT * FROM sets ORDER BY createdMs ASC")
    suspend fun snapshotSets(): List<WorkingSet>

    @Query("SELECT * FROM set_items ORDER BY addedMs ASC")
    suspend fun snapshotItems(): List<SetItem>

    @Query("SELECT path FROM set_items WHERE setId = :setId ORDER BY addedMs DESC")
    suspend fun itemPaths(setId: String): List<String>

    @Query("SELECT path FROM set_items")
    suspend fun allItemPaths(): List<String>

    @Query("SELECT COUNT(*) FROM set_items WHERE setId = :setId")
    fun itemCount(setId: String): Flow<Int>

    /** Keeps set references synced through moves and renames, like the desktop. */
    @Query(
        "UPDATE OR REPLACE set_items SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    /** Sets that currently hold [path] — for a file's dossier. */
    @Query(
        "SELECT s.* FROM sets s INNER JOIN set_items i ON s.id = i.setId " +
            "WHERE i.path = :path ORDER BY s.createdMs DESC",
    )
    suspend fun setsContaining(path: String): List<WorkingSet>
}

@Dao
interface RevisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(revisit: Revisit)

    @Query("DELETE FROM revisits WHERE path = :path")
    suspend fun clear(path: String)

    @Query("SELECT * FROM revisits ORDER BY dueMs ASC")
    fun all(): Flow<List<Revisit>>

    @Query("SELECT * FROM revisits WHERE path = :path")
    suspend fun byPath(path: String): Revisit?

    @Query(
        "UPDATE OR REPLACE revisits SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    @Query("DELETE FROM revisits WHERE path = :path OR path LIKE :pathLike || '/%' ESCAPE '\\'")
    suspend fun removeTree(path: String, pathLike: String)
}

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(note: Note)

    @Query("DELETE FROM notes WHERE path = :path")
    suspend fun clear(path: String)

    @Query("SELECT * FROM notes ORDER BY updatedMs DESC")
    fun all(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE path = :path")
    suspend fun byPath(path: String): Note?

    @Query("SELECT * FROM notes WHERE path = :path")
    fun observe(path: String): Flow<Note?>

    @Query("SELECT path FROM notes")
    suspend fun allPaths(): List<String>

    @Query("SELECT * FROM notes ORDER BY updatedMs ASC")
    suspend fun snapshot(): List<Note>

    @Query(
        "UPDATE OR REPLACE notes SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    @Query("DELETE FROM notes WHERE path = :path OR path LIKE :pathLike || '/%' ESCAPE '\\'")
    suspend fun removeTree(path: String, pathLike: String)
}

@Dao
interface OcrDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(row: OcrText)

    @Query("SELECT * FROM ocr_index WHERE path = :path")
    suspend fun byPath(path: String): OcrText?

    @Query("SELECT COUNT(*) FROM ocr_index")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM ocr_index")
    fun countFlow(): Flow<Int>

    /** Images whose recognized text contains [q]; newest-indexed first. */
    @Query(
        "SELECT * FROM ocr_index WHERE text LIKE '%' || :q || '%' ESCAPE '\\' " +
            "ORDER BY indexedMs DESC LIMIT 100",
    )
    suspend fun search(q: String): List<OcrText>

    @Query("DELETE FROM ocr_index WHERE path = :path")
    suspend fun clear(path: String)

    @Query("DELETE FROM ocr_index")
    suspend fun clearAll()

    @Query(
        "UPDATE OR REPLACE ocr_index SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    @Query("DELETE FROM ocr_index WHERE path = :path OR path LIKE :pathLike || '/%' ESCAPE '\\'")
    suspend fun removeTree(path: String, pathLike: String)
}

@Dao
interface SealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(seal: Seal)

    @Query("DELETE FROM seals WHERE path = :path")
    suspend fun remove(path: String)

    @Query("SELECT * FROM seals ORDER BY whenMs DESC")
    fun all(): Flow<List<Seal>>

    @Query("SELECT path FROM seals")
    suspend fun allPaths(): List<String>

    @Query("SELECT * FROM seals ORDER BY whenMs ASC")
    suspend fun snapshot(): List<Seal>

    @Query("SELECT EXISTS(SELECT 1 FROM seals WHERE path = :path)")
    suspend fun isSealed(path: String): Boolean

    /**
     * True if [path] itself or anything beneath it is sealed — so a destructive
     * op on a folder can refuse to swallow a sealed file living inside it.
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM seals WHERE path = :path " +
            "OR path LIKE :pathLike || '/%' ESCAPE '\\')",
    )
    suspend fun anySealedUnder(path: String, pathLike: String): Boolean

    @Query(
        "UPDATE OR REPLACE seals SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBaseLike || '/%' ESCAPE '\\'",
    )
    suspend fun rebasePaths(oldBase: String, oldBaseLike: String, newBase: String)

    @Query("DELETE FROM seals WHERE path = :path OR path LIKE :pathLike || '/%' ESCAPE '\\'")
    suspend fun removeTree(path: String, pathLike: String)
}
