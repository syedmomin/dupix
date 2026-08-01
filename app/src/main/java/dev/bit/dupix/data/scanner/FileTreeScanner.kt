package dev.bit.dupix.data.scanner

import android.net.Uri
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import java.io.File
import javax.inject.Inject

/**
 * Deep scanner: walks the entire shared storage with java.io.File. Requires "All files
 * access" (MANAGE_EXTERNAL_STORAGE). Every regular file is turned into a [FileItem] and
 * classified by extension; files with no tracked category are still returned as
 * [FileCategory.LARGE_FILE] candidates so the large-files list is complete.
 */
class FileTreeScanner @Inject constructor() {

    /**
     * @param root       directory to scan (usually Environment.getExternalStorageDirectory()).
     * @param onProgress called periodically with the running file count.
     */
    suspend fun scan(root: File, onProgress: suspend (count: Int) -> Unit = {}): List<FileItem> {
        val out = ArrayList<FileItem>()
        val stack = ArrayDeque<File>()
        stack.addLast(root)
        var count = 0
        // Guard against the same path being reached twice (symlinks / bind mounts / the
        // same storage volume exposed via multiple routes). Without this a file can be
        // emitted twice and get grouped as a "duplicate of itself".
        val seenDirs = HashSet<String>()
        val seenFiles = HashSet<String>()

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
                    val size = child.length()
                    out += FileItem(
                        uri = Uri.fromFile(child),
                        displayName = child.name,
                        path = child.absolutePath,
                        size = size,
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

    /** Skip paths the app can't read or shouldn't touch (other apps' sandboxed data). */
    private fun shouldSkip(dir: File): Boolean {
        val path = dir.absolutePath
        return path.contains("/Android/data") ||
            path.contains("/Android/obb") ||
            dir.name == ".thumbnails"
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
