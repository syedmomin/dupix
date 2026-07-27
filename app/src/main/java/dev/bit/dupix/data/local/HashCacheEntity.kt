package dev.bit.dupix.data.local

import androidx.room.Entity

/**
 * Cached hash for a file, keyed by identity (path + size + lastModified). If any of
 * those change the row no longer matches, so a modified file is re-hashed automatically.
 */
@Entity(tableName = "hash_cache", primaryKeys = ["path", "size", "lastModified"])
data class HashCacheEntity(
    val path: String,
    val size: Long,
    val lastModified: Long,
    val partialHash: String,
    val fullHash: String?,
)
