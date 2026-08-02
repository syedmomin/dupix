package dev.bit.dupix.data.scanner

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import java.io.File
import javax.inject.Inject

/**
 * Deep scanner: walks EVERY mounted storage volume (internal storage AND SD card / USB)
 * with java.io.File. Requires "All files access" (MANAGE_EXTERNAL_STORAGE). Hidden folders
 * (e.g. WhatsApp `.Statuses`) are included. Only `Android/data` and `Android/obb` are
 * skipped — Android blocks those for every third-party app, so they can't be read anyway.
 */
class FileTreeScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** @param onProgress called periodically with the running file count. */
    suspend fun scan(onProgress: suspend (count: Int) -> Unit = {}): List<FileItem> {
        val out = ArrayList<FileItem>()
        val seenDirs = HashSet<String>()
        val seenFiles = HashSet<String>()
        var count = 0

        val stack = ArrayDeque<File>()
        for (root in storageRoots()) stack.addLast(root)

        while (stack.isNotEmpty()) {
            val dir = stack.removeLast()
            if (shouldSkip(dir)) continue
            val dirKey = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
            if (!seenDirs.add(dirKey)) continue
            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    stack.addLast(child)
                } else if (child.isFile) {
                    val key = runCatching { child.canonicalPath }.getOrDefault(child.absolutePath)
                    if (!seenFiles.add(key)) continue
                    out += FileItem(
                        uri = Uri.fromFile(child),
                        displayName = child.name,
                        path = child.absolutePath,
                        size = child.length(),
                        lastModified = child.lastModified(),
                        category = classify(child.name),
                        mediaId = null,
                    )
                    count++
                    if (count % 200 == 0) onProgress(count)
                }
            }
        }
        onProgress(count)
        return out
    }

    /** Roots of all mounted volumes: internal storage plus SD card / USB. */
    private fun storageRoots(): List<File> {
        val roots = LinkedHashSet<File>()

        runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
            ?.let { if (it.exists() && it.canRead()) roots.add(it) }

        // StorageManager exposes every mounted volume's mount point (API 30+).
        runCatching {
            context.getSystemService(StorageManager::class.java)?.storageVolumes?.forEach { volume ->
                runCatching { volume.directory }.getOrNull()
                    ?.let { if (it.exists() && it.canRead()) roots.add(it) }
            }
        }

        // Fallback: enumerate mounts under /storage (catches SD cards on some OEM builds).
        runCatching {
            File("/storage").listFiles()?.forEach { f ->
                if (f.isDirectory && f.canRead() &&
                    f.name != "self" && f.name != "emulated" && f.name != "knox-emulated"
                ) {
                    roots.add(f)
                }
            }
        }
        return roots.toList()
    }

    /** Only skip paths Android blocks for all apps. Everything else (incl. hidden) is scanned. */
    private fun shouldSkip(dir: File): Boolean {
        val path = dir.absolutePath
        return path.contains("/Android/data") || path.contains("/Android/obb")
    }

    private fun classify(name: String): FileCategory =
        when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> FileCategory.PHOTO
            "mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "m4v" -> FileCategory.VIDEO
            "mp3", "wav", "ogg", "m4a", "flac", "aac", "amr", "opus" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "ppt", "pptx", "txt", "xls", "xlsx", "csv" -> FileCategory.DOCUMENT
            "apk" -> FileCategory.APK
            else -> FileCategory.LARGE_FILE
        }
}
