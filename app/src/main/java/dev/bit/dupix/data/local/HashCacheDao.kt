package dev.bit.dupix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HashCacheDao {

    @Query("SELECT * FROM hash_cache WHERE path = :path AND size = :size AND lastModified = :lastModified LIMIT 1")
    suspend fun find(path: String, size: Long, lastModified: Long): HashCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HashCacheEntity)

    @Query("DELETE FROM hash_cache")
    suspend fun clear()

    // Blocking variants for use inside the IO-bound hashing engine (never call on main thread).

    @Query("SELECT * FROM hash_cache WHERE path = :path AND size = :size AND lastModified = :lastModified LIMIT 1")
    fun findBlocking(path: String, size: Long, lastModified: Long): HashCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertBlocking(entity: HashCacheEntity)
}
