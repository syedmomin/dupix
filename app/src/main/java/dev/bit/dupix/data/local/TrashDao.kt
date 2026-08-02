package dev.bit.dupix.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TrashEntry)

    @Query("SELECT * FROM trash ORDER BY deletedAt DESC")
    suspend fun all(): List<TrashEntry>

    @Delete
    suspend fun delete(entry: TrashEntry)

    @Query("DELETE FROM trash")
    suspend fun clear()
}
