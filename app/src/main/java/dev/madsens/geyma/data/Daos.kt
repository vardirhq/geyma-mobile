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

    /**
     * Ghost trails: departures (move/rename/trash) whose origin sat directly in
     * [dir], newer than [sinceMs]. prevPath NOT LIKE dir/%/% keeps it to direct
     * children.
     */
    @Query(
        "SELECT * FROM events WHERE action IN ('moved','renamed','trashed') " +
            "AND prevPath LIKE :dir || '/%' AND prevPath NOT LIKE :dir || '/%/%' " +
            "AND whenMs > :sinceMs ORDER BY whenMs DESC LIMIT 30",
    )
    suspend fun departuresFrom(dir: String, sinceMs: Long): List<FileEvent>

    /** Rewrite journal paths after a move/rename so timelines follow the file. */
    @Query(
        "UPDATE events SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBase || '/%'",
    )
    suspend fun rebasePaths(oldBase: String, newBase: String)

    @Query("DELETE FROM events WHERE whenMs < :beforeMs")
    suspend fun prune(beforeMs: Long)
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

    @Query("SELECT EXISTS(SELECT 1 FROM stars WHERE path = :path)")
    suspend fun isStarred(path: String): Boolean

    @Query(
        "UPDATE OR REPLACE stars SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBase || '/%'",
    )
    suspend fun rebasePaths(oldBase: String, newBase: String)

    @Query("DELETE FROM stars WHERE path = :path OR path LIKE :path || '/%'")
    suspend fun removeTree(path: String)
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

    @Query("SELECT COUNT(*) FROM set_items WHERE setId = :setId")
    fun itemCount(setId: String): Flow<Int>

    /** Keeps set references synced through moves and renames, like the desktop. */
    @Query(
        "UPDATE OR REPLACE set_items SET path = :newBase || substr(path, length(:oldBase) + 1) " +
            "WHERE path = :oldBase OR path LIKE :oldBase || '/%'",
    )
    suspend fun rebasePaths(oldBase: String, newBase: String)
}
