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
     * Builds a system delete-confirmation request for [items] backed by MediaStore.
     * The caller launches the returned [IntentSender] via ActivityResult and, on OK,
     * the OS has already deleted the files.
     * @return null if none of the items are MediaStore-backed.
     */
    fun createMediaDeleteRequest(items: List<FileItem>): IntentSender? {
        val uris: List<Uri> = items.mapNotNull { if (it.mediaId != null) it.uri else null }
        if (uris.isEmpty()) return null
        return MediaStore.createDeleteRequest(resolver, uris).intentSender
    }

    /**
     * Deletes SAF-sourced [items] directly. Assumes the tree URI permission is held.
     * @return bytes successfully freed.
     */
    suspend fun deleteSaf(items: List<FileItem>): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        for (item in items) {
            if (item.mediaId != null) continue // media handled via delete request
            val ok = runCatching {
                DocumentsContract.deleteDocument(resolver, item.uri)
            }.getOrDefault(false)
            if (ok) freed += item.size
        }
        freed
    }
}
