package dev.madsens.geyma.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FileEvent::class, Star::class, TrashEntry::class, WorkingSet::class, SetItem::class, SeenFile::class],
    version = 2,
    exportSchema = false,
)
abstract class GeymaDb : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun stars(): StarDao
    abstract fun trash(): TrashDao
    abstract fun sets(): SetDao
    abstract fun seen(): SeenDao

    companion object {
        /**
         * v1 → v2 adds the seen-files ledger for arrival provenance. A real
         * migration (not a destructive one) keeps the journal intact across
         * updates — the whole point of an app that remembers.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `seen_files` (" +
                        "`path` TEXT NOT NULL, `firstSeenMs` INTEGER NOT NULL, " +
                        "`lastOpenedMs` INTEGER, PRIMARY KEY(`path`))",
                )
            }
        }

        @Volatile
        private var instance: GeymaDb? = null

        fun get(context: Context): GeymaDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, GeymaDb::class.java, "geyma.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
