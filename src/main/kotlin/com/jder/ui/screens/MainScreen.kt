package com.jder.ui.screens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.jder.data.DiagramRepository
import com.jder.data.ImageExporter
import com.jder.domain.model.Attribute
import com.jder.domain.model.Connection
import com.jder.domain.model.DiagramState
import com.jder.domain.model.ToolMode
import com.jder.ui.components.ContextMenu
import com.jder.ui.components.DiagramToolbar
import com.jder.ui.components.ERDiagramCanvas
import com.jder.ui.components.FileManagerDialog
import com.jder.ui.components.FileManagerMode
import com.jder.ui.components.PropertiesPanel
import com.jder.ui.dialogs.AddAttributeDialog
import com.jder.ui.dialogs.CreateConnectionDialog
import com.jder.ui.dialogs.EditAttributeDialog
import com.jder.ui.dialogs.EditConnectionDialog
import com.jder.ui.dialogs.EntityPropertiesDialog
import com.jder.ui.dialogs.RelationshipPropertiesDialog
import com.jder.ui.utils.renderDiagramToBitmap
import com.jder.ui.theme.ThemeState
import java.io.File
@Composable
fun MainScreen(
    state: DiagramState,
    repository: DiagramRepository,
    themeState: ThemeState
) {
    var showEntityDialog by remember { mutableStateOf(false) }
    var showRelationshipDialog by remember { mutableStateOf(false) }
    var showAddAttributeDialog by remember { mutableStateOf(false) }
    var showEditAttributeDialog by remember { mutableStateOf(false) }
    var attributeToEdit by remember { mutableStateOf<Attribute?>(null) }
    var showCreateConnectionDialog by remember { mutableStateOf(false) }
    var showEditConnectionDialog by remember { mutableStateOf(false) }
    var connectionToEdit by remember { mutableStateOf<Pair<String, Connection>?>(null) }
    var showNewDiagramConfirmDialog by remember { mutableStateOf(false) }
    var showOpenDiagramConfirmDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var contextMenuForEntity by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when {
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Z -> {
                        if (state.canUndo()) {
                            state.undo()
                            snackbarMessage = "Azione annullata"
                        }
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Y -> {
                        if (state.canRedo()) {
                            state.redo()
                            snackbarMessage = "Azione ripristinata"
                        }
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.N -> {
                        if (state.isModified) {
                            showNewDiagramConfirmDialog = true
                        } else {
                            state.newDiagram()
                            snackbarMessage = "Nuovo diagramma creato"
                        }
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.O -> {
                        if (state.isModified) {
                            showOpenDiagramConfirmDialog = true
                        } else {
                            showOpenDialog = true
                        }
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.S -> {
                        if (state.currentFile != null) {
                            val file = File(state.currentFile!!)
                            val updatedDiagram = state.diagram.copy(name = file.nameWithoutExtension)
                            repository.saveDiagram(updatedDiagram, file).fold(
                                onSuccess = {
                                    state.updateDiagramName(file.nameWithoutExtension)
                                    state.markAsSaved(file.absolutePath)
                                    snackbarMessage = "Diagramma salvato"
                                },
                                onFailure = { snackbarMessage = "Errore salvataggio" }
                            )
                        } else {
                            showSaveAsDialog = true
                        }
                        true
                    }
                    keyEvent.key == Key.Delete || keyEvent.key == Key.Backspace -> {
                        when {
                            state.selectedEntityId != null -> {
                                state.deleteEntity(state.selectedEntityId!!)
                                snackbarMessage = "Entità eliminata"
                            }
                            state.selectedRelationshipId != null -> {
                                state.deleteRelationship(state.selectedRelationshipId!!)
                                snackbarMessage = "Relazione eliminata"
                            }
                        }
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Equals -> {
                        state.zoom = (state.zoom * 1.2f).coerceAtMost(3f)
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Minus -> {
                        state.zoom = (state.zoom / 1.2f).coerceAtLeast(0.25f)
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Zero -> {
                        state.resetView()
                        true
                    }
                    keyEvent.key == Key.Escape -> {
                        state.clearSelection()
                        state.toolMode = ToolMode.SELECT
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        },
        topBar = {
            DiagramToolbar(
                state = state,
                themeState = themeState,
                onNewDiagram = {
                    if (state.isModified) {
                        showNewDiagramConfirmDialog = true
                    } else {
                        state.newDiagram()
                        snackbarMessage = "Nuovo diagramma creato"
                    }
                },
                onOpenDiagram = {
                    if (state.isModified) {
                        showOpenDiagramConfirmDialog = true
                    } else {
                        showOpenDialog = true
                    }
                },
                onSaveDiagram = {
                    if (state.currentFile != null) {
                        val file = File(state.currentFile!!)
                        val updatedDiagram = state.diagram.copy(name = file.nameWithoutExtension)
                        repository.saveDiagram(updatedDiagram, file).fold(
                            onSuccess = {
                                state.updateDiagramName(file.nameWithoutExtension)
                                state.markAsSaved(file.absolutePath)
                                snackbarMessage = "Diagramma salvato"
                            },
                            onFailure = { error ->
                                snackbarMessage = "Errore nel salvataggio: ${error.message}"
                            }
                        )
                    } else {
                        showSaveAsDialog = true
                    }
                },
                onSaveAsDiagram = {
                    showSaveAsDialog = true
                },
                onExportPNG = {
                    showExportDialog = true
                },
                onZoomIn = {
                    state.zoom = (state.zoom * 1.2f).coerceAtMost(3f)
                },
                onZoomOut = {
                    state.zoom = (state.zoom / 1.2f).coerceAtLeast(0.25f)
                },
                onResetZoom = {
                    state.resetView()
                },
                onUndo = {
                    if (state.canUndo()) {
                        state.undo()
                        snackbarMessage = "Azione annullata"
                    }
                },
                onRedo = {
                    if (state.canRedo()) {
                        state.redo()
                        snackbarMessage = "Azione ripristinata"
                    }
                },
                onShowSnackbar = { message ->
                    snackbarMessage = message
                },
                modifier = Modifier
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                ERDiagramCanvas(
                    state = state,
                    onContextMenuRequest = { position, isEntity ->
                        contextMenuPosition = position
                        contextMenuForEntity = isEntity
                        showContextMenu = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (showContextMenu) {
                    ContextMenu(
                        position = contextMenuPosition,
                        isEntity = contextMenuForEntity,
                        onDismiss = { showContextMenu = false },
                        onEdit = {
                            showContextMenu = false
                            if (contextMenuForEntity) {
                                showEntityDialog = true
                            } else {
                                showRelationshipDialog = true
                            }
                        },
                        onDelete = {
                            showContextMenu = false
                            if (contextMenuForEntity && state.selectedEntityId != null) {
                                state.deleteEntity(state.selectedEntityId!!)
                                snackbarMessage = "Entità eliminata"
                            } else if (!contextMenuForEntity && state.selectedRelationshipId != null) {
                                state.deleteRelationship(state.selectedRelationshipId!!)
                                snackbarMessage = "Relazione eliminata"
                            }
                        },
                        onAddAttribute = {
                            showContextMenu = false
                            showAddAttributeDialog = true
                        },
                        onAddConnection = if (!contextMenuForEntity) {
                            {
                                showContextMenu = false
                                showCreateConnectionDialog = true
                            }
                        } else null
                    )
                }
            }
            if (state.selectedEntityId != null || state.selectedRelationshipId != null) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    tonalElevation = 2.dp
                ) {
                    PropertiesPanel(
                        state = state,
                        onEditEntity = { showEntityDialog = true },
                        onEditRelationship = { showRelationshipDialog = true },
                        onAddAttribute = { showAddAttributeDialog = true },
                        onAddConnection = { showCreateConnectionDialog = true },
                        onEditAttribute = { attr ->
                            attributeToEdit = attr
                            showEditAttributeDialog = true
                        },
                        onDeleteAttribute = { attrId ->
                            when {
                                state.selectedEntityId != null -> {
                                    state.deleteAttributeFromEntity(state.selectedEntityId!!, attrId)
                                    snackbarMessage = "Attributo eliminato"
                                }
                                state.selectedRelationshipId != null -> {
                                    state.deleteAttributeFromRelationship(state.selectedRelationshipId!!, attrId)
                                    snackbarMessage = "Attributo eliminato"
                                }
                            }
                        },
                        onEditConnection = { entityId, conn ->
                            connectionToEdit = Pair(entityId, conn)
                            showEditConnectionDialog = true
                        },
                        onDeleteConnection = { entityId ->
                            state.selectedRelationshipId?.let { relId ->
                                state.deleteConnection(relId, entityId)
                                snackbarMessage = "Connessione eliminata"
                            }
                        },
                        onClose = {
                            state.selectEntity(null)
                            state.selectRelationship(null)
                        }
                    )
                }
            }
        }
    }
    if (showEntityDialog && state.selectedEntityId != null) {
        val entity = state.diagram.entities.find { it.id == state.selectedEntityId }
        entity?.let {
            EntityPropertiesDialog(
                entity = it,
                onDismiss = { showEntityDialog = false },
                onSave = { updatedEntity ->
                    state.updateEntity(it.id) { updatedEntity }
                    showEntityDialog = false
                }
            )
        }
    }
    if (showRelationshipDialog && state.selectedRelationshipId != null) {
        val relationship = state.diagram.relationships.find { it.id == state.selectedRelationshipId }
        relationship?.let {
            RelationshipPropertiesDialog(
                relationship = it,
                onDismiss = { showRelationshipDialog = false },
                onSave = { updatedRelationship ->
                    state.updateRelationship(it.id) { updatedRelationship }
                    showRelationshipDialog = false
                }
            )
        }
    }
    if (showAddAttributeDialog) {
        AddAttributeDialog(
            onDismiss = { showAddAttributeDialog = false },
            onAdd = { attribute ->
                when {
                    state.selectedEntityId != null -> {
                        state.addAttributeToEntity(state.selectedEntityId!!, attribute)
                    }
                    state.selectedRelationshipId != null -> {
                        state.addAttributeToRelationship(state.selectedRelationshipId!!, attribute)
                    }
                }
                showAddAttributeDialog = false
                snackbarMessage = "Attributo aggiunto"
            }
        )
    }
    if (showEditAttributeDialog && attributeToEdit != null) {
        EditAttributeDialog(
            attribute = attributeToEdit!!,
            onDismiss = {
                showEditAttributeDialog = false
                attributeToEdit = null
            },
            onSave = { updatedAttribute ->
                when {
                    state.selectedEntityId != null -> {
                        state.updateAttributeInEntity(
                            state.selectedEntityId!!,
                            attributeToEdit!!.id,
                            updatedAttribute
                        )
                    }
                    state.selectedRelationshipId != null -> {
                        state.updateAttributeInRelationship(
                            state.selectedRelationshipId!!,
                            attributeToEdit!!.id,
                            updatedAttribute
                        )
                    }
                }
                showEditAttributeDialog = false
                attributeToEdit = null
                snackbarMessage = "Attributo modificato"
            }
        )
    }
    if (showCreateConnectionDialog && state.selectedRelationshipId != null) {
        val selectedRelationship = state.diagram.relationships.find { it.id == state.selectedRelationshipId }
        val connectedEntityIds = selectedRelationship?.connections?.map { it.entityId }?.toSet() ?: emptySet()
        val availableEntities = state.diagram.entities.filter { it.id !in connectedEntityIds }
        CreateConnectionDialog(
            entities = availableEntities,
            onDismiss = { showCreateConnectionDialog = false },
            onCreate = { entityId, cardinality ->
                state.addConnection(state.selectedRelationshipId!!, entityId, cardinality)
                showCreateConnectionDialog = false
                snackbarMessage = "Connessione creata"
            }
        )
    }
    if (showEditConnectionDialog && connectionToEdit != null && state.selectedRelationshipId != null) {
        val selectedRelationship = state.diagram.relationships.find { it.id == state.selectedRelationshipId }
        val connectedEntityIds = selectedRelationship?.connections
            ?.map { it.entityId }
            ?.filter { it != connectionToEdit!!.first }
            ?.toSet() ?: emptySet()
        val availableEntities = state.diagram.entities.filter { it.id !in connectedEntityIds }
        EditConnectionDialog(
            entities = availableEntities,
            currentEntityId = connectionToEdit!!.first,
            currentCardinality = connectionToEdit!!.second.cardinality,
            onDismiss = {
                showEditConnectionDialog = false
                connectionToEdit = null
            },
            onSave = { newEntityId, newCardinality ->
                state.updateConnection(
                    state.selectedRelationshipId!!,
                    connectionToEdit!!.first,
                    newEntityId,
                    newCardinality
                )
                showEditConnectionDialog = false
                connectionToEdit = null
                snackbarMessage = "Connessione modificata"
            }
        )
    }
    if (showNewDiagramConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showNewDiagramConfirmDialog = false },
            title = { Text("Conferma nuovo diagramma") },
            text = { Text("Ci sono modifiche non salvate. Vuoi creare un nuovo diagramma comunque?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewDiagramConfirmDialog = false
                        state.newDiagram()
                        snackbarMessage = "Nuovo diagramma creato"
                    }
                ) {
                    Text("Sì")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDiagramConfirmDialog = false }) {
                    Text("No")
                }
            }
        )
    }
    if (showOpenDiagramConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showOpenDiagramConfirmDialog = false },
            title = { Text("Conferma apertura diagramma") },
            text = { Text("Ci sono modifiche non salvate. Vuoi aprire un altro diagramma comunque?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOpenDiagramConfirmDialog = false
                        showOpenDialog = true
                    }
                ) {
                    Text("Sì")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenDiagramConfirmDialog = false }) {
                    Text("No")
                }
            }
        )
    }
    if (showOpenDialog) {
        FileManagerDialog(
            mode = FileManagerMode.OPEN,
            initialDirectory = File(System.getProperty("user.home")),
            fileExtension = ".json",
            title = "Apri Diagramma",
            onDismiss = { showOpenDialog = false },
            onFileSelected = { file ->
                showOpenDialog = false
                repository.loadDiagram(file).fold(
                    onSuccess = { diagram ->
                        state.loadDiagram(diagram, file.absolutePath)
                        snackbarMessage = "Diagramma caricato: ${file.name}"
                    },
                    onFailure = { error ->
                        snackbarMessage = "Errore nel caricamento: ${error.message}"
                    }
                )
            }
        )
    }
    if (showSaveAsDialog) {
        FileManagerDialog(
            mode = FileManagerMode.SAVE,
            initialDirectory = File(System.getProperty("user.home")),
            fileExtension = ".json",
            title = "Salva Diagramma",
            defaultFileName = state.diagram.name,
            onDismiss = {
                showSaveAsDialog = false
            },
            onFileSelected = { file ->
                showSaveAsDialog = false
                val finalFile = if (file.extension != "json") {
                    File(file.parentFile, "${file.nameWithoutExtension}.json")
                } else file
                val updatedDiagram = state.diagram.copy(name = finalFile.nameWithoutExtension)
                repository.saveDiagram(updatedDiagram, finalFile).fold(
                    onSuccess = {
                        state.updateDiagramName(finalFile.nameWithoutExtension)
                        state.markAsSaved(finalFile.absolutePath)
                        snackbarMessage = "Diagramma salvato: ${finalFile.name}"
                    },
                    onFailure = { error ->
                        snackbarMessage = "Errore nel salvataggio: ${error.message}"
                    }
                )
            }
        )
    }
    if (showExportDialog) {
        FileManagerDialog(
            mode = FileManagerMode.SAVE,
            initialDirectory = File(System.getProperty("user.home")),
            fileExtension = ".png",
            title = "Esporta come PNG",
            onDismiss = { showExportDialog = false },
            onFileSelected = { file ->
                showExportDialog = false
                val pngFile = if (file.extension != "png") {
                    File(file.parentFile, "${file.nameWithoutExtension}.png")
                } else file
                try {
                    val imageExporter = ImageExporter()
                    val bitmap = renderDiagramToBitmap(state.diagram)
                    imageExporter.exportToPNG(bitmap, pngFile).fold(
                        onSuccess = {
                            snackbarMessage = "Immagine salvata: ${pngFile.name}"
                        },
                        onFailure = { error ->
                            snackbarMessage = "Errore nell'esportazione: ${error.message}"
                        }
                    )
                } catch (e: Exception) {
                    snackbarMessage = "Errore nell'esportazione: ${e.message}"
                }
            }
        )
    }
}
