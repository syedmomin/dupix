package dev.bit.dupix.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bit.dupix.domain.model.FileItem
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.DeletingOverlay
import dev.bit.dupix.ui.components.EmptyState
import dev.bit.dupix.ui.components.PrimaryButton
import dev.bit.dupix.ui.components.SelectableTile
import dev.bit.dupix.ui.util.formatBytes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFilesScreen(
    vm: ScanViewModel,
    onBack: () -> Unit,
    onDeleteComplete: () -> Unit,
) {
    val result by vm.result.collectAsState()
    val files = result?.largeFiles.orEmpty()
    val scope = rememberCoroutineScope()

    val selected = remember { mutableStateMapOf<Uri, Boolean>() }
    var deleting by remember { mutableStateOf(false) }
    var pendingMediaUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }

    fun finishAndGoHome() {
        deleting = false
        onDeleteComplete()
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            vm.onDeleted(pendingMediaUris)
            pendingMediaUris.forEach { selected.remove(it) }
        }
        pendingMediaUris = emptySet()
        finishAndGoHome()
    }

    fun selectedItems(): List<FileItem> = files.filter { selected[it.uri] == true }

    fun deleteSelected() {
        val items = selectedItems()
        if (items.isEmpty()) return
        deleting = true
        scope.launch {
            val media = items.filter { it.mediaId != null }
            val fileItems = items.filter { it.mediaId == null && it.uri.scheme == "file" }
            val safDocs = items.filter { it.mediaId == null && it.uri.scheme != "file" }
            if (fileItems.isNotEmpty()) {
                vm.trashFiles(fileItems) // recoverable via Recycle Bin
                val uris = fileItems.map { it.uri }.toSet()
                vm.onDeleted(uris)
                uris.forEach { selected.remove(it) }
            }
            if (safDocs.isNotEmpty()) {
                vm.deleteSaf(safDocs)
                val uris = safDocs.map { it.uri }.toSet()
                vm.onDeleted(uris)
                uris.forEach { selected.remove(it) }
            }
            val sender = if (media.isNotEmpty()) vm.buildMediaDeleteRequest(media) else null
            if (sender != null) {
                pendingMediaUris = media.map { it.uri }.toSet()
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                finishAndGoHome()
            }
        }
    }

    val count = selectedItems().size
    val bytes = selectedItems().sumOf { it.size }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Large Files") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
            bottomBar = {
                if (count > 0) {
                    PrimaryButton(
                        text = "Delete $count · ${formatBytes(bytes)}",
                        onClick = { deleteSelected() },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    )
                }
            },
        ) { padding ->
            if (files.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.FolderOff,
                    title = "No large files found",
                    modifier = Modifier.padding(padding),
                )
                return@Scaffold
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            ) {
                itemsIndexed(files, key = { i, _ -> "large_$i" }) { _, file ->
                    SelectableTile(
                        uri = file.uri,
                        category = file.category,
                        displayName = file.displayName,
                        sizeBytes = file.size,
                        checked = selected[file.uri] == true,
                        isKeep = false,
                        selectable = true,
                        onToggle = { selected[file.uri] = it },
                    )
                }
            }
        }

        DeletingOverlay(visible = deleting)
    }
}
