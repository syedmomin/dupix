package dev.bit.dupix.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
            ),
        ) {
            item { RecoverableHeader(res.totalReclaimableBytes, res.totalDuplicateFiles) }

            itemsIndexed(dupCategories) { index, cat ->
                val groups = res.groups(cat)
                val dupes = groups.sumOf { it.duplicates.size }
                val reclaim = groups.sumOf { it.reclaimableBytes }
                AnimatedListItem(index = index) {
                    SummaryCard(
                        category = cat,
                        count = dupes,
                        detail = if (dupes == 0) "No duplicates" else "$dupes duplicates",
                        sizeLabel = if (reclaim > 0) formatBytes(reclaim) else null,
                        enabled = dupes > 0,
                        onClick = { if (dupes > 0) onOpenCategory(cat) },
                    )
                }
            }

            item {
                val large = res.largeFiles
                AnimatedListItem(index = dupCategories.size) {
                    SummaryCard(
                        category = FileCategory.LARGE_FILE,
                        count = large.size,
                        detail = if (large.isEmpty()) "None found" else "${large.size} files",
                        sizeLabel = if (large.isNotEmpty()) formatBytes(large.sumOf { it.size }) else null,
                        enabled = large.isNotEmpty(),
                        onClick = { if (large.isNotEmpty()) onOpenLargeFiles() },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecoverableHeader(reclaimableBytes: Long, duplicateFiles: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Potential Space Recovery",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                formatBytes(reclaimableBytes),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "$duplicateFiles duplicate files found",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    category: FileCategory,
    count: Int,
    detail: String,
    sizeLabel: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val (icon, tint) = categoryVisual(category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.copy(alpha = if (enabled) 0.16f else 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    category.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sizeLabel != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        sizeLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            if (enabled) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun categoryVisual(category: FileCategory): Pair<ImageVector, Color> = when (category) {
    FileCategory.PHOTO -> Icons.Default.Image to Color(0xFF2E7D32)
    FileCategory.VIDEO -> Icons.Default.Movie to Color(0xFF1565C0)
    FileCategory.AUDIO -> Icons.Default.MusicNote to Color(0xFF6A1B9A)
    FileCategory.DOCUMENT -> Icons.Default.Description to Color(0xFFEF6C00)
    FileCategory.APK -> Icons.Default.Android to Color(0xFF00897B)
    FileCategory.LARGE_FILE -> Icons.Default.Folder to Color(0xFFC62828)
}
