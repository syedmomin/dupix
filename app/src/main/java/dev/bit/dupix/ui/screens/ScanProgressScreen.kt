package dev.bit.dupix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bit.dupix.domain.model.ScanProgress
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.PrimaryButton
import dev.bit.dupix.ui.components.SvgScanLoader

@Composable
fun ScanProgressScreen(
    vm: ScanViewModel,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    val progress by vm.progress.collectAsState()

    LaunchedEffect(progress) {
        if (progress is ScanProgress.Done) onComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val failed = progress as? ScanProgress.Failed
        if (failed != null) {
            Text("Scan failed", style = MaterialTheme.typography.titleLarge)
            Text(failed.message, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            PrimaryButton(text = "Back", onClick = onBack, modifier = Modifier.padding(top = 24.dp))
            return@Column
        }

        Text(
            "Scanning your device",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))

        // Animated scanner illustration (assets/scanner.svg).
        SvgScanLoader(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(328f / 208f),
        )
        Spacer(Modifier.height(24.dp))

        when (val p = progress) {
            is ScanProgress.Hashing -> {
                val fraction = if (p.total > 0) p.processed.toFloat() / p.total else 0f
                Text(
                    "Analyzing for duplicates…",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${p.processed} / ${p.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            is ScanProgress.Enumerating -> {
                Text(
                    "Finding files…\n${p.filesFound} files found",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            else -> {
                Text("Preparing…", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
