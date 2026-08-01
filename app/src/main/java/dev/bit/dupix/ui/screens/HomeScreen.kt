package dev.bit.dupix.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.PrimaryButton
import dev.bit.dupix.ui.components.StatCard
import dev.bit.dupix.ui.components.StorageCard
import dev.bit.dupix.ui.util.allFilesAccessIntent
import dev.bit.dupix.ui.util.formatBytes
import dev.bit.dupix.ui.util.hasAllFilesAccess
import dev.bit.dupix.ui.util.hasMediaPermissions
import dev.bit.dupix.ui.util.mediaPermissions

private const val LARGE_THRESHOLD = 100L * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: ScanViewModel,
    onScanStarted: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val storage by vm.storage.collectAsState()
    val result by vm.result.collectAsState()

    // Refresh storage when returning to the screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshStorage()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allCategories = FileCategory.entries.toSet()
    fun deepScan() {
        vm.startScan(allCategories, LARGE_THRESHOLD, deepScan = true)
        onScanStarted()
    }
    fun shallowScan() {
        vm.startScan(allCategories, LARGE_THRESHOLD, deepScan = false)
        onScanStarted()
    }

    // Fallback media-permission request (used when All files access is unavailable/denied).
    val mediaPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> if (grants.values.any { it }) shallowScan() }

    fun requestMediaThenScan() {
        if (hasMediaPermissions(context)) shallowScan()
        else mediaPermLauncher.launch(mediaPermissions())
    }

    // Returning from the "All files access" system screen: deep-scan if granted, else fall back.
    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { if (hasAllFilesAccess()) deepScan() else requestMediaThenScan() }

    fun onScanClick() {
        if (hasAllFilesAccess()) {
            deepScan()
        } else {
            runCatching { allFilesLauncher.launch(allFilesAccessIntent(context)) }
                .onFailure { requestMediaThenScan() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dupix") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StorageCard(usedBytes = storage.usedBytes, totalBytes = storage.totalBytes)

            val dupCount = result?.totalDuplicateFiles ?: 0
            val recoverable = result?.totalReclaimableBytes ?: 0L
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Duplicates Found", if (result == null) "—" else "$dupCount", Modifier.weight(1f))
                StatCard("Recoverable", if (result == null) "—" else formatBytes(recoverable), Modifier.weight(1f))
            }

            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = "Scan Now",
                onClick = { onScanClick() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Grant \"All files access\" to deep-scan every folder on your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
