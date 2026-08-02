package dev.bit.dupix.data.repository

import android.os.Environment
import dev.bit.dupix.data.local.TrashDao
import dev.bit.dupix.data.local.TrashEntry
import dev.bit.dupix.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Dupix recycle bin for file-based items (deep scan / All files access). Instead of
 * deleting, files are moved to `Dupix/.trash` and indexed so they can be restored to their
 * original location. (Media handled via MediaStore's system trash lives in DeleteRepository.)
 */
class TrashRepository @Inject constructor(
    private val dao: TrashDao,
) {
    private val trashDir: File
        get() = File(Environment.getExternalStorageDirectory(), "Dupix/.trash")

    /** Moves file:// [items] into the recycle bin. Returns bytes moved out of their folders. */
    suspend fun trashFiles(items: List<FileItem>): Long = withContext(Dispatchers.IO) {
        val dir = trashDir
        if (!dir.exists()) dir.mkdirs()
        var moved = 0L
        for (item in items) {
            if (item.uri.scheme != "file") continue
            val src = File(item.path)
            if (!src.exists()) continue
            val id = UUID.randomUUID().toString()
            val dest = File(dir, "${id}_${item.displayName}")
            if (moveFile(src, dest)) {
                dao.insert(
                    TrashEntry(
                        id = id,
                        originalPath = item.path,
                        trashPath = dest.absolutePath,
                        displayName = item.displayName,
                        size = item.size,
                        category = item.category.name,
                        deletedAt = System.currentTimeMillis(),
                    )
                )
                moved += item.size
            }
        }
        moved
    }

    suspend fun all(): List<TrashEntry> = withContext(Dispatchers.IO) { dao.all() }

    /** Moves the file back to its original location. */
    suspend fun restore(entry: TrashEntry): Boolean = withContext(Dispatchers.IO) {
        val src = File(entry.trashPath)
        val dest = File(entry.originalPath)
        dest.parentFile?.mkdirs()
        val ok = src.exists() && moveFile(src, dest)
        if (ok || !src.exists()) dao.delete(entry)
        ok
    }

    /** Permanently deletes a single bin entry. */
    suspend fun purge(entry: TrashEntry): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(entry.trashPath).delete() }
        dao.delete(entry)
        true
    }

    /** Permanently deletes everything in the bin. */
    suspend fun emptyBin() = withContext(Dispatchers.IO) {
        dao.all().forEach { runCatching { File(it.trashPath).delete() } }
        dao.clear()
    }

    private fun moveFile(src: File, dest: File): Boolean {
        if (runCatching { src.renameTo(dest) }.getOrDefault(false)) return true
        // Fallback for cross-volume moves where renameTo fails.
        return runCatching {
            src.copyTo(dest, overwrite = true)
            src.delete()
        }.getOrDefault(false)
    }
}
