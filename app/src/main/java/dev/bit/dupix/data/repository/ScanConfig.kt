package dev.bit.dupix.data.repository

import android.net.Uri
import dev.bit.dupix.domain.model.FileCategory

/**
 * Inputs for a scan run.
 *
 * @param categories       which categories to scan.
 * @param largeFileThreshold minimum size (bytes) for the large-files list.
 * @param safTreeUri       persisted SAF tree, required to scan documents/APKs/large files
 *                         outside media collections; null means media-only.
 */
data class ScanConfig(
    val categories: Set<FileCategory> = FileCategory.entries.toSet(),
    val largeFileThreshold: Long = 100L * 1024 * 1024,
    val safTreeUri: Uri? = null,
)
