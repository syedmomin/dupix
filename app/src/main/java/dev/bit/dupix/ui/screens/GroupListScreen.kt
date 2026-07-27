package dev.bit.dupix.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.bit.dupix.domain.model.DuplicateGroup
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.domain.model.FileItem
import dev.bit.dupix.ui.ScanViewModel
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
                Button(
                    onClick = { deleteSelected() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("Delete $selectedCount · ${formatBytes(selectedBytes)}")
                }
            }
        },
    ) { padding ->
        if (groups.isEmpty()) {
            Text("No duplicates.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            groups.forEachIndexed { index, group ->
                item(key = group.hash) {
                    GroupCard(
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

@Composable
private fun GroupCard(
    group: DuplicateGroup,
    isSelected: (Uri) -> Boolean,
    onToggle: (Uri, Boolean) -> Unit,
    index: Int,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Group $index · ${group.files.size} copies · ${formatBytes(group.reclaimableBytes)} recoverable",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.padding(top = 4.dp))
            group.files.forEachIndexed { i, file ->
                if (i > 0) HorizontalDivider()
                FileRow(
                    file = file,
                    isKeep = i == group.keepIndex,
                    checked = isSelected(file.uri),
                    onCheckedChange = { onToggle(file.uri, it) },
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    file: FileItem,
    isKeep: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isVisual = file.category == FileCategory.PHOTO || file.category == FileCategory.VIDEO
        if (isVisual) {
            AsyncImage(
                model = file.uri,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                file.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (isKeep) "${formatBytes(file.size)} · KEEP" else formatBytes(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = if (isKeep) MaterialTheme.colorScheme.primary else Color.Unspecified,
                fontWeight = if (isKeep) FontWeight.Bold else FontWeight.Normal,
            )
        }
        if (!isKeep) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
