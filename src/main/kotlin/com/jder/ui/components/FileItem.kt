package com.jder.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
@Composable
fun FileItem(
    file: File,
    isSelected: Boolean,
    fileExtension: String,
    mode: FileManagerMode,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val isValidFile = if (mode == FileManagerMode.OPEN) {
        file.isDirectory || file.extension == fileExtension.removePrefix(".")
    } else {
        true
    }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        !isValidFile -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isValidFile) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 500 && clickCount == 1) {
                    onDoubleClick()
                } else {
                    onClick()
                }
                clickCount = if (currentTime - lastClickTime < 500 && clickCount == 1) 0 else 1
                lastClickTime = currentTime
            },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Default.Folder
                    file.extension == "json" -> Icons.Default.Description
                    file.extension == "png" -> Icons.Default.Image
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    file.isDirectory -> MaterialTheme.colorScheme.primary
                    isSelected -> contentColor
                    !isValidFile -> contentColor
                    else -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal
                )
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.length()),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
fun formatFileSize(bytes: Long): String {
    if (bytes < 0) return "0 B"
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB", "PB", "EB")
    var size = bytes.toDouble()
    var unitIndex = -1
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex < 0) {
        "$bytes B"
    } else {
        "%.2f %s".format(size, units[unitIndex])
    }
}
