package dev.bit.dupix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A file that Dupix moved to its recycle bin, so it can be restored to [originalPath]. */
@Entity(tableName = "trash")
data class TrashEntry(
    @PrimaryKey val id: String,
    val originalPath: String,
    val trashPath: String,
    val displayName: String,
    val size: Long,
    val category: String,
    val deletedAt: Long,
)
