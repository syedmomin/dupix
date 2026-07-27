package dev.bit.dupix.data.scanner

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import javax.inject.Inject

/**
 * Enumerates files under a user-granted SAF tree (documents, APKs, or any file for the
 * large-files scan). Uses DocumentsContract directly (cursor-based) rather than
 * DocumentFile for speed over large trees.
 */
class SafScanner @Inject constructor(private val resolver: ContentResolver) {

    /**
     * @param treeUri persisted tree URI from ACTION_OPEN_DOCUMENT_TREE.
     * @param categories which categories to collect. LARGE_FILE collects everything.
     */
    fun scan(treeUri: Uri, categories: Set<FileCategory>): List<FileItem> {
        val out = ArrayList<FileItem>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        walk(treeUri, rootDocId, categories, out)
        return out
    }

    private fun walk(
        treeUri: Uri,
        parentDocId: String,
        categories: Set<FileCategory>,
        out: MutableList<FileItem>,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        resolver.query(childrenUri, projection, null, null, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (c.moveToNext()) {
                val docId = c.getString(idIdx)
                val name = c.getString(nameIdx) ?: continue
                val mime = c.getString(mimeIdx) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(treeUri, docId, categories, out)
                    continue
                }
                val category = classify(name) ?: FileCategory.LARGE_FILE
                val wanted = categories.contains(category) ||
                    categories.contains(FileCategory.LARGE_FILE)
                if (!wanted) continue

                val size = if (c.isNull(sizeIdx)) 0L else c.getLong(sizeIdx)
                val mod = if (c.isNull(modIdx)) 0L else c.getLong(modIdx)
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                out += FileItem(
                    uri = docUri,
                    displayName = name,
                    path = docId,
                    size = size,
                    lastModified = mod,
                    // If large-files scan is on, keep the natural category but the finder
                    // treats large files separately; here we report the specific category.
                    category = if (categories.contains(category)) category else FileCategory.LARGE_FILE,
                )
            }
        }
    }

    /** Maps a file name to a document/APK category, or null if it isn't one we track. */
    private fun classify(name: String): FileCategory? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf", "docx", "doc", "pptx", "ppt", "txt" -> FileCategory.DOCUMENT
            "apk" -> FileCategory.APK
            else -> null
        }
    }
}
