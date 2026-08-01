package dev.bit.dupix.data.repository

import dev.bit.dupix.domain.engine.DuplicateFinder
import dev.bit.dupix.domain.engine.Hasher
import dev.bit.dupix.domain.model.DuplicateGroup
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import dev.bit.dupix.domain.model.ScanProgress
import dev.bit.dupix.domain.model.ScanResult
import dev.bit.dupix.data.scanner.FileTreeScanner
import dev.bit.dupix.data.scanner.MediaStoreScanner
import dev.bit.dupix.data.scanner.SafScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.os.Environment
import javax.inject.Inject

/**
 * Orchestrates a scan: enumerate files per category, run the staged duplicate finder, and
 * assemble the large-files list. Emits [ScanProgress] as a cold [Flow].
 */
class ScanRepository @Inject constructor(
    private val mediaScanner: MediaStoreScanner,
    private val safScanner: SafScanner,
    private val fileTreeScanner: FileTreeScanner,
    private val hasher: Hasher,
) {
    private val finder = DuplicateFinder(hasher)

    fun scan(config: ScanConfig): Flow<ScanProgress> = flow {
        // --- Enumerate ---------------------------------------------------------------
        val filesByCategory = LinkedHashMap<FileCategory, List<FileItem>>()
        val allEnumerated = ArrayList<FileItem>()

        if (config.deepScan) {
            // Full-device walk: one pass, then bucket everything by category.
            val root = Environment.getExternalStorageDirectory()
            val all = fileTreeScanner.scan(root) { count ->
                // Reuse the Enumerating event to show a live running count.
                emit(ScanProgress.Enumerating(FileCategory.LARGE_FILE, count))
            }
            allEnumerated += all
            for (cat in listOf(
                FileCategory.PHOTO, FileCategory.VIDEO, FileCategory.AUDIO,
                FileCategory.DOCUMENT, FileCategory.APK,
            )) {
                if (cat !in config.categories) continue
                val forCat = all.filter { it.category == cat }
                filesByCategory[cat] = forCat
                emit(ScanProgress.Enumerating(cat, forCat.size))
            }
        } else {
            val mediaCats = listOf(FileCategory.PHOTO, FileCategory.VIDEO, FileCategory.AUDIO)
            for (cat in mediaCats) {
                if (cat !in config.categories) continue
                val files = mediaScanner.scan(cat)
                filesByCategory[cat] = files
                allEnumerated += files
                emit(ScanProgress.Enumerating(cat, files.size))
            }

            val safCats = setOf(FileCategory.DOCUMENT, FileCategory.APK)
            val wantSaf = config.safTreeUri != null &&
                (config.categories.any { it in safCats } || FileCategory.LARGE_FILE in config.categories)
            if (wantSaf) {
                val safFiles = safScanner.scan(config.safTreeUri!!, config.categories)
                allEnumerated += safFiles
                for (cat in safCats) {
                    if (cat !in config.categories) continue
                    val forCat = safFiles.filter { it.category == cat }
                    filesByCategory[cat] = forCat
                    emit(ScanProgress.Enumerating(cat, forCat.size))
                }
            }
        }

        // --- Duplicate detection (with global hashing progress) ----------------------
        val totalCandidates = filesByCategory.values.sumOf { candidateCount(it) }
        var processedBase = 0
        val groupsByCategory = LinkedHashMap<FileCategory, List<DuplicateGroup>>()

        for ((cat, files) in filesByCategory) {
            val groups = finder.find(files) { processed, _ ->
                emit(ScanProgress.Hashing(processedBase + processed, totalCandidates))
            }
            processedBase += candidateCount(files)
            groupsByCategory[cat] = groups
        }

        // --- Large files -------------------------------------------------------------
        val largeFiles =
            if (FileCategory.LARGE_FILE in config.categories)
                allEnumerated
                    .filter { it.size >= config.largeFileThreshold }
                    .distinctBy { it.uri }
                    .sortedByDescending { it.size }
            else emptyList()

        emit(ScanProgress.Done(ScanResult(groupsByCategory, largeFiles)))
    }

    /** Count of files that will be hashed (size buckets with 2+ members). No I/O. */
    private fun candidateCount(files: List<FileItem>): Int =
        files.filter { it.size > 0 }
            .groupBy { it.size }
            .values
            .filter { it.size > 1 }
            .sumOf { it.size }
}
