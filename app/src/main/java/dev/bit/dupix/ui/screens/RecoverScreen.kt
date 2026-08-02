package dev.bit.dupix.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bit.dupix.domain.model.FileItem
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.EmptyState
import dev.bit.dupix.ui.components.PrimaryButton
import dev.bit.dupix.ui.components.SelectableTile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverScreen(
    vm: ScanViewModel,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    val selected = remember { mutableStateMapOf<Uri, Boolean>() }
    var pending by remember { mutableStateOf<Set<Uri>>(emptySet()) }

    suspend fun reload() {
        items = vm.trashedMedia()
        loaded = true
    }
    LaunchedEffect(Unit) { reload() }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            pending.forEach { selected.remove(it) }
            scope.launch { reload() }
        }
        pending = emptySet()
    }

    fun selectedItems(): List<FileItem> = items.filter { selected[it.uri] == true }

    fun restoreSelected() {
        val sel = selectedItems()
        if (sel.isEmpty()) return
        val sender = vm.buildUntrashRequest(sel) ?: return
        pending = sel.map { it.uri }.toSet()
        restoreLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    val count = selectedItems().size

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recover Deleted") },
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
                        text = "Restore $count",
                        onClick = { restoreSelected() },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    )
                }
            },
        ) { padding ->
            if (loaded && items.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.RestoreFromTrash,
                    title = "Nothing to recover",
                    subtitle = "Photos and videos deleted to the trash in the last ~30 days would show here.",
                    modifier = Modifier.padding(padding),
                )
                return@Scaffold
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Recently deleted photos & videos still in the trash. Select and restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
                itemsIndexed(items, key = { i, _ -> "recover_$i" }) { _, file ->
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
}
