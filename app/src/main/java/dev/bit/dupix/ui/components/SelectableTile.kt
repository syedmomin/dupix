package dev.bit.dupix.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.ui.util.formatBytes

/**
 * A square gallery tile for a file: thumbnail for photos/videos, a typed icon otherwise,
 * with a selection check and an optional KEEP badge. Tapping toggles selection.
 */
@Composable
fun SelectableTile(
    uri: Uri,
    category: FileCategory,
    displayName: String,
    sizeBytes: Long,
    checked: Boolean,
    isKeep: Boolean,
    selectable: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (checked) Modifier.border(
                        3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                    ) else Modifier
                )
                .clickable(enabled = selectable) { onToggle(!checked) },
        ) {
            val isImageOrVideo = category == FileCategory.PHOTO || category == FileCategory.VIDEO
            if (isImageOrVideo) {
                AsyncImage(
                    model = uri,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                )
                if (category == FileCategory.VIDEO) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(34.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = iconFor(category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                )
            }

            if (isKeep) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                ) {
                    Text(
                        "KEEP",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            if (selectable) {
                Icon(
                    imageVector = if (checked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (checked) "Selected" else "Not selected",
                    tint = if (checked) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp),
                )
            }
        }
        Text(
            displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        Text(
            formatBytes(sizeBytes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun iconFor(category: FileCategory) = when (category) {
    FileCategory.AUDIO -> Icons.Default.MusicNote
    FileCategory.DOCUMENT -> Icons.Default.Description
    FileCategory.APK -> Icons.Default.Android
    else -> Icons.Default.InsertDriveFile
}
