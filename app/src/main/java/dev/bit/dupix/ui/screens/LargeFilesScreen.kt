package dev.bit.dupix.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bit.dupix.domain.model.FileItem
import dev.bit.dupix.ui.ScanViewModel
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
) {
    val result by vm.result.collectAsState()
    val files = result?.largeFiles.orEmpty()
    val scope = rememberCoroutineScope()

    val selected = remember { mutableStateMapOf<Uri, Boolean>() }
    val pendingMedia = remember { mutableStateMapOf<Uri, Boolean>() }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val removed = pendingMedia.keys.toSet()
            vm.onDeleted(removed)
            removed.forEach { selected.remove(it) }
        }
        pendingMedia.clear()
    }

    fun selectedItems(): List<FileItem> = files.filter { selected[it.uri] == true }

    fun deleteSelected() {
        val items = selectedItems()
        if (items.isEmpty()) return
        val media = items.filter { it.mediaId != null }
        val saf = items.filter { it.mediaId == null }
        if (saf.isNotEmpty()) {
            scope.launch {
                vm.deleteSaf(saf)
                val uris = saf.map { it.uri }.toSet()
                vm.onDeleted(uris)
                uris.forEach { selected.remove(it) }
            }
        }
        if (media.isNotEmpty()) {
            val sender = vm.buildMediaDeleteRequest(media) ?: return
            pendingMedia.clear()
            media.forEach { pendingMedia[it.uri] = true }
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    val count = selectedItems().size
    val bytes = selectedItems().sumOf { it.size }

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
            items(files, key = { it.uri.toString() }) { file ->
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
}
