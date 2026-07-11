package dev.madsens.geyma.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FileEvent::class, Star::class, TrashEntry::class, WorkingSet::class, SetItem::class],
    version = 1,
    exportSchema = false,
)
abstract class GeymaDb : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun stars(): StarDao
    abstract fun trash(): TrashDao
    abstract fun sets(): SetDao

    companion object {
        @Volatile
        private var instance: GeymaDb? = null

        fun get(context: Context): GeymaDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, GeymaDb::class.java, "geyma.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
