package dev.bit.dupix.data.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import javax.inject.Inject

/**
 * Enumerates media (images, video, audio) via MediaStore. Returns [FileItem]s carrying a
 * MediaStore _ID so deletion can use a batched delete request.
 */
class MediaStoreScanner @Inject constructor(private val resolver: ContentResolver) {

    fun scan(category: FileCategory): List<FileItem> = when (category) {
        FileCategory.PHOTO -> queryImages()
        FileCategory.VIDEO -> queryVideos()
        FileCategory.AUDIO -> queryAudio()
        else -> emptyList()
    }

    /** All media items across images/video/audio (used for the large-files scan). */
    fun scanAllMedia(): List<FileItem> = queryImages() + queryVideos() + queryAudio()

    private fun queryImages(): List<FileItem> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return query(
            uri, FileCategory.PHOTO,
            widthCol = MediaStore.Images.Media.WIDTH,
            heightCol = MediaStore.Images.Media.HEIGHT,
        )
    }

    private fun queryVideos(): List<FileItem> {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        return query(
            uri, FileCategory.VIDEO,
            widthCol = MediaStore.Video.Media.WIDTH,
            heightCol = MediaStore.Video.Media.HEIGHT,
        )
    }

    private fun queryAudio(): List<FileItem> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        return query(uri, FileCategory.AUDIO, widthCol = null, heightCol = null)
    }

    private fun query(
        collection: android.net.Uri,
        category: FileCategory,
        widthCol: String?,
        heightCol: String?,
    ): List<FileItem> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            if (widthCol != null) add(widthCol)
            if (heightCol != null) add(heightCol)
        }.toTypedArray()

        val out = ArrayList<FileItem>()
        resolver.query(collection, projection, null, null, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val modIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val pathIdx = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val wIdx = widthCol?.let { c.getColumnIndex(it) } ?: -1
            val hIdx = heightCol?.let { c.getColumnIndex(it) } ?: -1

            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val name = c.getString(nameIdx) ?: continue
                val size = c.getLong(sizeIdx)
                val dateMod = if (c.isNull(modIdx)) 0L else c.getLong(modIdx) * 1000L
                val relPath = if (pathIdx >= 0 && !c.isNull(pathIdx)) c.getString(pathIdx) else ""
                val w = if (wIdx >= 0 && !c.isNull(wIdx)) c.getLong(wIdx) else 0L
                val h = if (hIdx >= 0 && !c.isNull(hIdx)) c.getLong(hIdx) else 0L
                out += FileItem(
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = name,
                    path = relPath + name,
                    size = size,
                    lastModified = dateMod,
                    category = category,
                    mediaId = id,
                    widthHeight = w * h,
                )
            }
        }
        return out
    }
}
