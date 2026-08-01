package dev.bit.dupix.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Determinate progress ring with an animated fill and a crossfading percentage label.
 * Pass null for [progress] to show an indeterminate spinner (e.g. while enumerating files).
 */
@Composable
fun AnimatedProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 96.dp,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (progress == null) {
            CircularProgressIndicator(modifier = Modifier.size(size), strokeWidth = 6.dp)
        } else {
            val animated by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 400),
                label = "ringProgress",
            )
            CircularProgressIndicator(
                progress = { animated },
                modifier = Modifier.size(size),
                strokeWidth = 6.dp,
            )
            AnimatedContent(
                targetState = (animated * 100).toInt(),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "ringPercent",
            ) { percent ->
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
