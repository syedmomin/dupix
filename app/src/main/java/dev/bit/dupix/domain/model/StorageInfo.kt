package dev.bit.dupix.domain.model

/** Device storage snapshot for the dashboard. */
data class StorageInfo(val usedBytes: Long, val totalBytes: Long) {
    val freeBytes: Long get() = (totalBytes - usedBytes).coerceAtLeast(0)
}
