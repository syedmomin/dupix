package dev.bit.dupix.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Recovers media that is currently in the Android **system trash** (photos/videos deleted
 * to trash in the last ~30 days — by any app). This is the only real, no-root recovery
 * possible: it restores items that still exist in the trash, not files already erased.
 */
class RecoverRepository @Inject constructor(
    private val resolver: ContentResolver,
) {
    suspend fun trashedMedia(): List<FileItem> = withContext(Dispatchers.IO) {
        val out = ArrayList<FileItem>()
        out += queryTrashed(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, FileCategory.PHOTO)
        out += queryTrashed(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, FileCategory.VIDEO)
        out.sortedByDescending { it.lastModified }
    }

    private fun queryTrashed(collection: Uri, category: FileCategory): List<FileItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val queryArgs = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }
        val out = ArrayList<FileItem>()
        runCatching {
            resolver.query(collection, projection, queryArgs, null)?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val modIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    out += FileItem(
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = c.getString(nameIdx) ?: "item_$id",
                        path = c.getString(nameIdx) ?: "",
                        size = if (c.isNull(sizeIdx)) 0L else c.getLong(sizeIdx),
                        lastModified = if (c.isNull(modIdx)) 0L else c.getLong(modIdx) * 1000L,
                        category = category,
                        mediaId = id,
                    )
                }
            }
        }
        return out
    }

    /** Builds a request to UNTRASH (restore) the given trashed media. Caller launches it. */
    fun createUntrashRequest(items: List<FileItem>): IntentSender? {
        val uris = items.mapNotNull { if (it.mediaId != null) it.uri else null }
        if (uris.isEmpty()) return null
        return MediaStore.createTrashRequest(resolver, uris, false).intentSender
    }
}
