package dev.bit.dupix.domain.model

import android.net.Uri

/**
 * A single file discovered during a scan.
 *
 * @param uri         Content/tree URI used to open and delete the file.
 * @param displayName File name shown to the user.
 * @param path        Best-effort filesystem-style path (may be a relative MediaStore path).
 * @param size        Size in bytes. Duplicate detection buckets by this first.
 * @param lastModified Epoch millis; used with (path, size) as the cache key.
 * @param category    Which scan category produced this item.
 * @param mediaId     MediaStore _ID when this came from MediaStore (used for batch delete),
 *                    or null for SAF-sourced files.
 * @param widthHeight Pixel area (w*h) for photos/videos when known; used by keep-best.
 */
data class FileItem(
    val uri: Uri,
    val displayName: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val category: FileCategory,
    val mediaId: Long? = null,
    val widthHeight: Long = 0L,
)
