package com.jder.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jder.domain.model.DiagramState
import com.jder.domain.model.ToolMode
import com.jder.ui.theme.ThemeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagramToolbar(
    state: DiagramState,
    themeState: ThemeState,
    onNewDiagram: () -> Unit,
    onOpenDiagram: () -> Unit,
    onSaveDiagram: () -> Unit,
    onExportPNG: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier
) {
    val title by remember {
        derivedStateOf {
            buildString {
                append("JDER - ")
                append(state.currentFile?.substringAfterLast("\\") ?: state.diagram.name)
                if (state.isModified) append(" *")
            }
        }
    }
    val zoomPercentage by remember {
        derivedStateOf {
            "${(state.zoom * 100).toInt()}%"
        }
    }
    Column(modifier = modifier) {
        TopAppBar(
            title = {
                Text(text = title)
            },
            actions = {
                val coroutineScope = rememberCoroutineScope()
                var showFileMenu by remember { mutableStateOf(false) }
                var showExportMenu by remember { mutableStateOf(false) }
                var showViewMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showFileMenu = true }) {
                        Text("File")
                    }
                    DropdownMenu(
                        expanded = showFileMenu,
                        onDismissRequest = { showFileMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nuovo Diagramma") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showFileMenu = false
                                    onNewDiagram()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Add, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Apri Diagramma...") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showFileMenu = false
                                    onOpenDiagram()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Salva") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showFileMenu = false
                                    onSaveDiagram()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Save, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Elimina Elemento Selezionato") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showFileMenu = false
                                    when {
                                        state.selectedEntityId != null -> {
                                            state.deleteEntity(state.selectedEntityId!!)
                                            onShowSnackbar("Entità eliminata")
                                        }
                                        state.selectedRelationshipId != null -> {
                                            state.deleteRelationship(state.selectedRelationshipId!!)
                                            onShowSnackbar("Relazione eliminata")
                                        }
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            enabled = state.selectedEntityId != null || state.selectedRelationshipId != null
                        )
                    }
                }
                Box {
                    TextButton(onClick = { showExportMenu = true }) {
                        Text("Esporta")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Esporta come PNG...") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showExportMenu = false
                                    onExportPNG()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Image, "Esporta diagramma come immagine PNG") }
                        )
                    }
                }
                Box {
                    TextButton(onClick = { showViewMenu = true }) {
                        Text("Vista")
                    }
                    DropdownMenu(
                        expanded = showViewMenu,
                        onDismissRequest = { showViewMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Zoom In") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showViewMenu = false
                                    onZoomIn()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.ZoomIn, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Zoom Out") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showViewMenu = false
                                    onZoomOut()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.ZoomOut, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Reimposta Zoom") },
                            onClick = {
                                coroutineScope.launch {
                                    delay(150)
                                    showViewMenu = false
                                    onResetZoom()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.CenterFocusWeak, null) }
                        )
                    }
                }
                ThemeToggleButton(
                    isDarkTheme = themeState.isDarkTheme,
                    onToggle = { themeState.toggleTheme() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconToggleButton(
                    checked = state.toolMode == ToolMode.SELECT,
                    onCheckedChange = { state.toolMode = ToolMode.SELECT }
                ) {
                    Icon(Icons.Default.NearMe, "Seleziona e Sposta")
                }
                Divider(modifier = Modifier.width(1.dp).height(40.dp))
                IconButton(
                    onClick = onUndo,
                    enabled = state.canUndo()
                ) {
                    Icon(Icons.Default.Undo, "Annulla (Ctrl+Z)")
                }
                IconButton(
                    onClick = onRedo,
                    enabled = state.canRedo()
                ) {
                    Icon(Icons.Default.Redo, "Ripristina (Ctrl+Y)")
                }
                Divider(modifier = Modifier.width(1.dp).height(40.dp))
                IconToggleButton(
                    checked = state.toolMode == ToolMode.ENTITY,
                    onCheckedChange = { state.toolMode = ToolMode.ENTITY }
                ) {
                    Icon(CustomIcons.Rectangle, "Crea Entità")
                }
                IconToggleButton(
                    checked = state.toolMode == ToolMode.RELATIONSHIP,
                    onCheckedChange = { state.toolMode = ToolMode.RELATIONSHIP }
                ) {
                    Icon(CustomIcons.Diamond, "Crea Relazione")
                }
                Divider(modifier = Modifier.width(1.dp).height(40.dp))
                IconButton(
                    onClick = onSaveDiagram,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Save, "Salva")
                }
                IconButton(
                    onClick = {
                        when {
                            state.selectedEntityId != null -> {
                                state.deleteEntity(state.selectedEntityId!!)
                                onShowSnackbar("Entità eliminata")
                            }
                            state.selectedRelationshipId != null -> {
                                state.deleteRelationship(state.selectedRelationshipId!!)
                                onShowSnackbar("Relazione eliminata")
                            }
                        }
                    },
                    enabled = state.selectedEntityId != null || state.selectedRelationshipId != null,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Elimina Elemento Selezionato",
                        tint = if (state.selectedEntityId != null || state.selectedRelationshipId != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = zoomPercentage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.CenterVertically)
                )
                IconButton(onClick = onZoomOut) {
                    Icon(Icons.Default.ZoomOut, "Zoom Out")
                }
                IconButton(onClick = onResetZoom) {
                    Icon(Icons.Default.CenterFocusWeak, "Reset Zoom")
                }
                IconButton(onClick = onZoomIn) {
                    Icon(Icons.Default.ZoomIn, "Zoom In")
                }
            }
        }
    }
}
