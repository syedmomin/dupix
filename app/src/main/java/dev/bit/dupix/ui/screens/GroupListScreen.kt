package dev.bit.dupix.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
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
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.AnimatedListItem
import dev.bit.dupix.ui.components.DuplicateGroupCard
import dev.bit.dupix.ui.components.EmptyState
import dev.bit.dupix.ui.components.PrimaryButton
import dev.bit.dupix.ui.util.formatBytes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    vm: ScanViewModel,
    category: FileCategory,
    onBack: () -> Unit,
) {
    val result by vm.result.collectAsState()
    val groups = result?.groups(category).orEmpty()
    val scope = rememberCoroutineScope()

    // Selected URIs -> deletable. Duplicates start selected; kept files are never selectable.
    val selected = remember(category) { mutableStateMapOf<Uri, Boolean>() }
    remember(groups) {
        groups.forEach { g -> g.duplicates.forEach { d -> selected.putIfAbsent(d.uri, true) } }
        true
    }

    // Track which URIs a pending media delete request will remove.
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

    fun selectedItems(): List<FileItem> =
        groups.flatMap { it.duplicates }.filter { selected[it.uri] == true }

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
            val sender = vm.buildMediaDeleteRequest(media)
            if (sender != null) {
                pendingMedia.clear()
                media.forEach { pendingMedia[it.uri] = true }
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
        }
    }

    val selectedCount = selectedItems().size
    val selectedBytes = selectedItems().sumOf { it.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (selectedCount > 0) {
                PrimaryButton(
                    text = "Delete $selectedCount · ${formatBytes(selectedBytes)}",
                    onClick = { deleteSelected() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        },
    ) { padding ->
        if (groups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.SearchOff,
                title = "No duplicates",
                subtitle = "Nothing to clean up in ${category.label}.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            groups.forEachIndexed { index, group ->
                item(key = group.hash) {
                    AnimatedListItem(index = index) {
                        DuplicateGroupCard(
                            group = group,
                            isSelected = { uri -> selected[uri] == true },
                            onToggle = { uri, value -> selected[uri] = value },
                            index = index + 1,
                        )
                    }
                }
            }
        }
    }
}
