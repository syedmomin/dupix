package dev.bit.dupix.domain.engine

import dev.bit.dupix.domain.model.DuplicateGroup
import dev.bit.dupix.domain.model.FileItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Finds byte-identical duplicates using a staged pipeline so we hash as little as
 * possible:
 *
 *   1. Bucket by exact byte size — only equal sizes can be duplicates.
 *   2. Partial hash (head + tail) within each size bucket — drops most non-duplicates.
 *   3. Full hash within each partial-hash bucket — confirms true duplicates.
 *
 * The finder is pure: all I/O goes through [FileHasher]/[StreamOpener], so it is fully
 * unit-testable with in-memory files. Files whose hashing throws are skipped (a file we
 * cannot read cannot be safely marked a duplicate).
 */
class DuplicateFinder(private val hasher: Hasher) {

    /**
     * @param onHashProgress called with (processed, total) as full/partial hashing advances.
     * @return one [DuplicateGroup] per set of 2+ identical files.
     */
    suspend fun find(
        files: List<FileItem>,
        onHashProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> },
    ): List<DuplicateGroup> {
        // Stage 1: bucket by size (ignore empty files — grouping zero-byte files as
        // "duplicates" is noise, not recoverable space). Dedupe by URI first so the same
        // physical file can never be grouped as a duplicate of itself.
        val bySize = files.distinctBy { it.uri }.filter { it.size > 0 }.groupBy { it.size }
        val sizeCandidates = bySize.values.filter { it.size > 1 }

        // Total files that will be hashed at least once (partial pass).
        val total = sizeCandidates.sumOf { it.size }
        var processed = 0

        val groups = ArrayList<DuplicateGroup>()

        for (bucket in sizeCandidates) {
            currentCoroutineContext().ensureActive()

            // Stage 2: partial hash.
            val byPartial = HashMap<String, MutableList<FileItem>>()
            for (item in bucket) {
                currentCoroutineContext().ensureActive()
                val ph = runCatching { hasher.partialHash(item) }.getOrNull()
                processed++
                onHashProgress(processed, total)
                if (ph != null) byPartial.getOrPut(ph) { ArrayList() }.add(item)
            }

            // Stage 3: full hash only where partial hashes collide.
            for (partialBucket in byPartial.values) {
                if (partialBucket.size < 2) continue
                val byFull = HashMap<String, MutableList<FileItem>>()
                for (item in partialBucket) {
                    currentCoroutineContext().ensureActive()
                    val fh = runCatching { hasher.fullHash(item) }.getOrNull() ?: continue
                    byFull.getOrPut(fh) { ArrayList() }.add(item)
                }
                for ((fullHash, identical) in byFull) {
                    if (identical.size < 2) continue
                    groups += DuplicateGroup(
                        hash = fullHash,
                        files = identical,
                        keepIndex = chooseKeepIndex(identical),
                    )
                }
            }
        }

        // Largest reclaimable groups first.
        return groups.sortedByDescending { it.reclaimableBytes }
    }

    /**
     * Chooses which copy to keep. Preference: the original — oldest [FileItem.lastModified];
     * tie-break by shortest path, then name — so predictable and stable.
     */
    private fun chooseKeepIndex(files: List<FileItem>): Int {
        var best = 0
        for (i in 1 until files.size) {
            if (keepPrefers(files[i], files[best])) best = i
        }
        return best
    }

    /** True if [candidate] is a better copy to keep than [current]. */
    private fun keepPrefers(candidate: FileItem, current: FileItem): Boolean {
        if (candidate.lastModified != current.lastModified)
            return candidate.lastModified < current.lastModified
        if (candidate.path.length != current.path.length)
            return candidate.path.length < current.path.length
        return candidate.displayName < current.displayName
    }
}
