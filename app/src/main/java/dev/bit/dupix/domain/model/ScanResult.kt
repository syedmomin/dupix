package dev.bit.dupix.domain.model

/** Outcome of a full scan: duplicate groups per category, plus large files. */
data class ScanResult(
    val groupsByCategory: Map<FileCategory, List<DuplicateGroup>>,
    val largeFiles: List<FileItem>,
) {
    fun groups(category: FileCategory): List<DuplicateGroup> =
        groupsByCategory[category].orEmpty()

    val totalDuplicateFiles: Int
        get() = groupsByCategory.values.sumOf { groups -> groups.sumOf { it.duplicates.size } }

    val totalReclaimableBytes: Long
        get() = groupsByCategory.values.sumOf { groups -> groups.sumOf { it.reclaimableBytes } }

    companion object {
        val EMPTY = ScanResult(emptyMap(), emptyList())
    }
}
