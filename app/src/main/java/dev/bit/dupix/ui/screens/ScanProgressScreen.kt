package dev.bit.dupix.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bit.dupix.domain.model.ScanProgress
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.components.AnimatedProgressRing
import dev.bit.dupix.ui.components.PrimaryButton

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
        AnimatedContent(
            targetState = progress,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "scanProgressState",
        ) { p ->
            when (p) {
                is ScanProgress.Hashing -> {
                    val fraction = if (p.total > 0) p.processed.toFloat() / p.total else 0f
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedProgressRing(progress = fraction.coerceIn(0f, 1f))
                        Text(
                            "Analyzing files…\n${p.processed} / ${p.total}",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
                is ScanProgress.Enumerating -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedProgressRing(progress = null)
                        Text(
                            "Scanning ${p.category.label}…\n${p.filesFound} files found",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
                is ScanProgress.Failed -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scan failed", style = MaterialTheme.typography.titleLarge)
                        Text(p.message, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        PrimaryButton(text = "Back", onClick = onBack, modifier = Modifier.padding(top = 24.dp))
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedProgressRing(progress = null)
                        Text(
                            "Preparing scan…",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
            }
        }
    }
}
