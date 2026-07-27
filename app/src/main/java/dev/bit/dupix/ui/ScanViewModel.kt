package dev.bit.dupix.ui

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bit.dupix.data.repository.DeleteRepository
import dev.bit.dupix.data.repository.ScanConfig
import dev.bit.dupix.data.repository.StorageRepository
import dev.bit.dupix.domain.ScanManager
import dev.bit.dupix.domain.model.DuplicateGroup
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import dev.bit.dupix.domain.model.ScanProgress
import dev.bit.dupix.domain.model.ScanResult
import dev.bit.dupix.domain.model.StorageInfo
import dev.bit.dupix.service.ScanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val scanManager: ScanManager,
    private val storageRepository: StorageRepository,
    private val deleteRepository: DeleteRepository,
    private val resolver: ContentResolver,
) : ViewModel() {

    val progress: StateFlow<ScanProgress> = scanManager.state

    private val _result = MutableStateFlow<ScanResult?>(null)
    /** Mutable working copy of the last scan result, updated as files are deleted. */
    val result: StateFlow<ScanResult?> = _result.asStateFlow()

    private val _storage = MutableStateFlow(StorageInfo(0, 0))
    val storage: StateFlow<StorageInfo> = _storage.asStateFlow()

    /** SAF tree the user granted for documents/APKs/large-files, if any. */
    var safTreeUri: Uri? = null
        private set

    init {
        // Sync the working copy whenever a scan completes.
        scanManager.state
            .onEach { if (it is ScanProgress.Done) _result.value = it.result }
            .launchIn(viewModelScope)
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch { _storage.value = storageRepository.snapshot() }
    }

    fun onSafTreeGranted(uri: Uri) {
        runCatching {
            resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        safTreeUri = uri
    }

    fun startScan(categories: Set<FileCategory>, largeFileThreshold: Long) {
        scanManager.submit(
            ScanConfig(
                categories = categories,
                largeFileThreshold = largeFileThreshold,
                safTreeUri = safTreeUri,
            )
        )
        ScanService.start(appContext)
    }

    fun groups(category: FileCategory): List<DuplicateGroup> =
        _result.value?.groups(category).orEmpty()

    fun largeFiles(): List<FileItem> = _result.value?.largeFiles.orEmpty()

    /** Builds a MediaStore batch delete request for [items] (null if none are media). */
    fun buildMediaDeleteRequest(items: List<FileItem>): IntentSender? =
        deleteRepository.createMediaDeleteRequest(items)

    /** Deletes SAF-sourced items directly; returns bytes freed. */
    suspend fun deleteSaf(items: List<FileItem>): Long = deleteRepository.deleteSaf(items)

    /** Removes [uris] from the working result after a confirmed deletion. */
    fun onDeleted(uris: Set<Uri>) {
        val current = _result.value ?: return
        val newGroups = current.groupsByCategory.mapValues { (_, groups) ->
            groups.mapNotNull { it.withoutUris(uris) }
        }
        val newLarge = current.largeFiles.filterNot { it.uri in uris }
        _result.value = ScanResult(newGroups, newLarge)
        refreshStorage()
    }

    fun reset() {
        scanManager.reset()
        _result.value = null
    }
}

/** Rebuilds a group with [uris] removed; returns null if fewer than 2 files remain. */
private fun DuplicateGroup.withoutUris(uris: Set<Uri>): DuplicateGroup? {
    val remaining = files.filterNot { it.uri in uris }
    if (remaining.size < 2) return null
    val keptUri = keep.uri
    val newKeepIndex = remaining.indexOfFirst { it.uri == keptUri }.let { if (it >= 0) it else 0 }
    return copy(files = remaining, keepIndex = newKeepIndex)
}
