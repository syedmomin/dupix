package dev.bit.dupix.ui.screens

import android.content.Intent
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.PrimaryButton
import dev.bit.dupix.ui.components.StatCard
import dev.bit.dupix.ui.components.StorageCard
import dev.bit.dupix.ui.util.formatBytes
import dev.bit.dupix.ui.util.hasMediaPermissions
import dev.bit.dupix.ui.util.mediaPermissions

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

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) vm.onSafTreeGranted(uri) }

    fun launchScan() {
        // Optional: ask for a folder to include documents/APKs/large files outside media.
        if (vm.safTreeUri == null) {
            runCatching { safLauncher.launch(null) }
        }
        vm.startScan(
            categories = FileCategory.entries.toSet(),
            largeFileThreshold = 100L * 1024 * 1024,
        )
        onScanStarted()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) launchScan()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StorageCard(
                usedBytes = storage.usedBytes,
                totalBytes = storage.totalBytes,
            )

            val dupCount = result?.totalDuplicateFiles ?: 0
            val recoverable = result?.totalReclaimableBytes ?: 0L
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Duplicates Found", if (result == null) "—" else "$dupCount", Modifier.weight(1f))
                StatCard("Recoverable", if (result == null) "—" else formatBytes(recoverable), Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Scan Now",
                onClick = {
                    if (hasMediaPermissions(context)) launchScan()
                    else permissionLauncher.launch(mediaPermissions())
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
