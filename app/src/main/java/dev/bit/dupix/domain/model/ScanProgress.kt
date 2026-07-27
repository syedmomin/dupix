package dev.bit.dupix.domain.model

/** Streamed progress emitted by the scan pipeline / held by the scan manager. */
sealed interface ScanProgress {
    data object Idle : ScanProgress
    data object Starting : ScanProgress
    data class Enumerating(val category: FileCategory, val filesFound: Int) : ScanProgress
    data class Hashing(val processed: Int, val total: Int) : ScanProgress
    data class Done(val result: ScanResult) : ScanProgress
    data class Failed(val message: String) : ScanProgress
}
