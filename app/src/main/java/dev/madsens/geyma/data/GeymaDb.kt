package dev.madsens.geyma.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FileEvent::class, Star::class, TrashEntry::class, WorkingSet::class,
        SetItem::class, SeenFile::class, Revisit::class, Note::class, Seal::class,
        OcrText::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class GeymaDb : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun stars(): StarDao
    abstract fun trash(): TrashDao
    abstract fun sets(): SetDao
    abstract fun seen(): SeenDao
    abstract fun revisits(): RevisitDao
    abstract fun notes(): NoteDao
    abstract fun seals(): SealDao
    abstract fun ocr(): OcrDao

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

        /**
         * v2 → v3 adds the revisit table (resurfacing reminders). Additive, so
         * the journal and everything else Geyma remembers survives the update.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `revisits` (" +
                        "`path` TEXT NOT NULL, `dueMs` INTEGER NOT NULL, " +
                        "`note` TEXT, `createdMs` INTEGER NOT NULL, PRIMARY KEY(`path`))",
                )
            }
        }

        /**
         * v3 → v4 adds the `notes` (sticky notes pinned to files) and `seals`
         * (guarded-against-change) tables. Additive, so the journal and every
         * other memory Geyma keeps survives the update.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (" +
                        "`path` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                        "`updatedMs` INTEGER NOT NULL, `createdMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`path`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `seals` (" +
                        "`path` TEXT NOT NULL, `whenMs` INTEGER NOT NULL, PRIMARY KEY(`path`))",
                )
            }
        }

        /**
         * v4 → v5 adds the `ocr_index` table (text recognized inside images, for
         * content search). Additive — nothing Geyma remembers is disturbed.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ocr_index` (" +
                        "`path` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                        "`modifiedMs` INTEGER NOT NULL, `size` INTEGER NOT NULL, " +
                        "`indexedMs` INTEGER NOT NULL, PRIMARY KEY(`path`))",
                )
            }
        }

        @Volatile
        private var instance: GeymaDb? = null

        fun get(context: Context): GeymaDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, GeymaDb::class.java, "geyma.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { instance = it }
        }
    }
}
