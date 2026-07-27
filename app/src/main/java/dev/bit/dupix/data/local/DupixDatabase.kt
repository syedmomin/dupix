package dev.bit.dupix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HashCacheEntity::class], version = 1, exportSchema = false)
abstract class DupixDatabase : RoomDatabase() {
    abstract fun hashCacheDao(): HashCacheDao
}
