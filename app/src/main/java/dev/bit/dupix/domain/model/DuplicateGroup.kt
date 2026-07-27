package dev.bit.dupix.domain.model

/**
 * A set of byte-identical files. Exactly one member is marked to keep; the rest are
 * candidates for deletion.
 */
data class DuplicateGroup(
    val hash: String,
    val files: List<FileItem>,
    val keepIndex: Int,
) {
    val keep: FileItem get() = files[keepIndex]
    val duplicates: List<FileItem> get() = files.filterIndexed { i, _ -> i != keepIndex }

    /** Bytes reclaimable if every non-kept duplicate is deleted. */
    val reclaimableBytes: Long get() = duplicates.sumOf { it.size }
}
