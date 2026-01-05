package com.jder.ui.components
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
enum class ContextMenuType {
    ENTITY,
    RELATIONSHIP,
    NOTE
}
@Composable
fun ContextMenu(
    position: Offset,
    type: ContextMenuType,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddAttribute: (() -> Unit)? = null,
    onAddConnection: (() -> Unit)? = null,
    onConvertToAssociativeEntity: (() -> Unit)? = null,
    isNtoNRelationship: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(position.x.toInt(), position.y.toInt()),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier.width(220.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = when (type) {
                        ContextMenuType.ENTITY -> "Azioni Entità"
                        ContextMenuType.RELATIONSHIP -> "Azioni Relazione"
                        ContextMenuType.NOTE -> "Azioni Nota"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Divider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                when (type) {
                                    ContextMenuType.ENTITY -> "Modifica Proprietà"
                                    ContextMenuType.RELATIONSHIP -> "Modifica Proprietà"
                                    ContextMenuType.NOTE -> "Modifica Testo"
                                }
                            )
                        }
                    },
                    onClick = {
                        coroutineScope.launch {
                            delay(150)
                            onDismiss()
                            onEdit()
                        }
                    },
                    leadingIcon = null
                )
                if (onAddAttribute != null && type != ContextMenuType.NOTE) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Aggiungi Attributo")
                            }
                        },
                        onClick = {
                            coroutineScope.launch {
                                delay(150)
                                onDismiss()
                                onAddAttribute()
                            }
                        },
                        leadingIcon = null
                    )
                }
                onAddConnection?.let { addConnection ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Aggiungi Connessione")
                            }
                        },
                        onClick = {
                            coroutineScope.launch {
                                delay(150)
                                onDismiss()
                                addConnection()
                            }
                        },
                        leadingIcon = null
                    )
                }
                onConvertToAssociativeEntity?.let { convertToAssociative ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isNtoNRelationship) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Converti in Entità Associativa",
                                    color = if (isNtoNRelationship) MaterialTheme.colorScheme.onSurface
                                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        },
                        onClick = {
                            if (isNtoNRelationship) {
                                coroutineScope.launch {
                                    delay(150)
                                    onDismiss()
                                    convertToAssociative()
                                }
                            }
                        },
                        leadingIcon = null,
                        enabled = isNtoNRelationship
                    )
                }
                Divider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Elimina",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onClick = {
                        coroutineScope.launch {
                            delay(150)
                            onDismiss()
                            onDelete()
                        }
                    },
                    leadingIcon = null
                )
            }
        }
    }
}
