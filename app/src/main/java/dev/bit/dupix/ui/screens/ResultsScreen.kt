package dev.bit.dupix.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.AnimatedListItem
import dev.bit.dupix.ui.components.EmptyState
import dev.bit.dupix.ui.util.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    vm: ScanViewModel,
    onOpenCategory: (FileCategory) -> Unit,
    onOpenLargeFiles: () -> Unit,
    onBack: () -> Unit,
) {
    val result by vm.result.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val res = result
        if (res == null) {
            EmptyState(
                icon = Icons.Default.Inbox,
                title = "No results yet",
                subtitle = "Run a scan from the home screen to see results here.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        val dupCategories = listOf(
            FileCategory.PHOTO, FileCategory.VIDEO, FileCategory.AUDIO,
            FileCategory.DOCUMENT, FileCategory.APK,
        )
        val rows = dupCategories.size + 1

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            item {
                Text(
                    "Recoverable: ${formatBytes(res.totalReclaimableBytes)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            itemsIndexed(dupCategories) { index, cat ->
                val groups = res.groups(cat)
                val dupes = groups.sumOf { it.duplicates.size }
                val reclaim = groups.sumOf { it.reclaimableBytes }
                AnimatedListItem(index = index) {
                    SummaryRow(
                        title = cat.label,
                        subtitle = if (dupes == 0) "No duplicates" else "$dupes duplicates · ${formatBytes(reclaim)}",
                        enabled = dupes > 0,
                        onClick = { if (dupes > 0) onOpenCategory(cat) },
                    )
                }
            }

            item {
                val large = res.largeFiles
                AnimatedListItem(index = rows - 1) {
                    SummaryRow(
                        title = FileCategory.LARGE_FILE.label,
                        subtitle = if (large.isEmpty()) "None found"
                        else "${large.size} files · ${formatBytes(large.sumOf { it.size })}",
                        enabled = large.isNotEmpty(),
                        onClick = { if (large.isNotEmpty()) onOpenLargeFiles() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            if (enabled) Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
