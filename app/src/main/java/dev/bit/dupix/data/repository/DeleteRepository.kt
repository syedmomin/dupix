package dev.bit.dupix.data.repository

import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import dev.bit.dupix.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Deletes duplicate files using the Android 11+ safe paths:
 *  - Media (MediaStore-sourced): a single batched system delete-confirmation request.
 *  - SAF-sourced (documents/APKs): DocumentsContract.deleteDocument per file.
 */
class DeleteRepository @Inject constructor(
    private val resolver: ContentResolver,
) {
    /**
     * Builds a system TRASH request for MediaStore-backed [items] — the OS moves them to
     * the recoverable trash (restorable for ~30 days via Google Photos / Files), not a
     * permanent delete. The caller launches the returned [IntentSender].
     * @return null if none of the items are MediaStore-backed.
     */
    fun createMediaDeleteRequest(items: List<FileItem>): IntentSender? {
        val uris: List<Uri> = items.mapNotNull { if (it.mediaId != null) it.uri else null }
        if (uris.isEmpty()) return null
        return MediaStore.createTrashRequest(resolver, uris, true).intentSender
    }

    /**
     * Deletes non-media [items] directly:
     *  - `file://` items (deep scan, All files access) via [java.io.File.delete].
     *  - SAF `content://` items via DocumentsContract (needs the tree permission).
     * Media (MediaStore) items are skipped here — they go through [createMediaDeleteRequest].
     * @return bytes successfully freed.
     */
    suspend fun deleteSaf(items: List<FileItem>): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        for (item in items) {
            if (item.mediaId != null) continue // media handled via delete request
            val ok = runCatching {
                if (item.uri.scheme == "file") {
                    val path = item.uri.path
                    path != null && java.io.File(path).delete()
                } else {
                    DocumentsContract.deleteDocument(resolver, item.uri)
                }
            }.getOrDefault(false)
            if (ok) freed += item.size
        }
        freed
    }
}
