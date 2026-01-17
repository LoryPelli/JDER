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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
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
import com.jder.domain.model.Cardinality
import com.jder.domain.model.Connection
import com.jder.domain.model.DiagramState
import com.jder.domain.model.ToolMode
import com.jder.ui.components.ContextMenu
import com.jder.ui.components.ContextMenuType
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
import com.jder.ui.dialogs.NotePropertiesDialog
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
    var showNoteDialog by remember { mutableStateOf(false) }
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
    var contextMenuType by remember { mutableStateOf(ContextMenuType.ENTITY) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                when {
                    event.isCtrlPressed && event.key == Key.Z -> {
                        if (state.canUndo()) {
                            state.undo()
                            snackbarMessage = "Azione annullata"
                        }
                        true
                    }
                    event.isCtrlPressed && event.key == Key.Y -> {
                        if (state.canRedo()) {
                            state.redo()
                            snackbarMessage = "Azione ripristinata"
                        }
                        true
                    }
                    event.isCtrlPressed && event.key == Key.N -> {
                        if (state.isModified) {
                            showNewDiagramConfirmDialog = true
                        } else {
                            state.newDiagram()
                            snackbarMessage = "Nuovo diagramma creato"
                        }
                        true
                    }
                    event.isCtrlPressed && event.key == Key.O -> {
                        if (state.isModified) {
                            showOpenDiagramConfirmDialog = true
                        } else {
                            showOpenDialog = true
                        }
                        true
                    }
                    event.isCtrlPressed && event.key == Key.S -> {
                        state.currentFile?.let { text ->
                            val file = File(text)
                            val updatedDiagram = state.diagram.copy(name = file.nameWithoutExtension)
                            repository.saveDiagram(updatedDiagram, file).fold(
                                onSuccess = {
                                    state.updateDiagramName(file.nameWithoutExtension)
                                    state.markAsSaved(file.absolutePath)
                                    snackbarMessage = "Diagramma salvato: ${file.name}"
                                },
                                onFailure = { snackbarMessage = "Errore nel salvataggio: ${it.message}" }
                            )
                        } ?: run {
                            showSaveAsDialog = true
                        }
                        true
                    }
                    event.key == Key.Delete || event.key == Key.Backspace -> {
                        state.selectedEntityId?.let {
                            state.deleteEntity(it)
                            snackbarMessage = "Entità eliminata"
                        }
                        state.selectedRelationshipId?.let {
                            state.deleteRelationship(it)
                            snackbarMessage = "Relazione eliminata"
                        }
                        state.selectedNoteId?.let {
                            state.deleteNote(it)
                            snackbarMessage = "Nota eliminata"
                        }
                        true
                    }
                    event.isCtrlPressed && event.key == Key.Plus -> {
                        state.zoom = (state.zoom * 1.2f).coerceAtMost(3f)
                        true
                    }
                    event.isCtrlPressed && event.key == Key.Minus -> {
                        state.zoom = (state.zoom / 1.2f).coerceAtLeast(0.25f)
                        true
                    }
                    event.isCtrlPressed && event.key == Key.Zero -> {
                        state.resetView()
                        true
                    }
                    event.key == Key.Escape -> {
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
                    state.currentFile?.let { text ->
                        val file = File(text)
                        val updatedDiagram = state.diagram.copy(name = file.nameWithoutExtension)
                        repository.saveDiagram(updatedDiagram, file).fold(
                            onSuccess = {
                                state.updateDiagramName(file.nameWithoutExtension)
                                state.markAsSaved(file.absolutePath)
                                snackbarMessage = "Diagramma salvato: ${file.name}"
                            },
                            onFailure = {
                                snackbarMessage = "Errore nel salvataggio: ${it.message}"
                            }
                        )
                    } ?: run {
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
                onShowSnackbar = {
                    snackbarMessage = it
                },
                modifier = Modifier
            )
        }
    ) { values ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(values)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                ERDiagramCanvas(
                    state = state,
                    onContextMenuRequest = { position, type ->
                        contextMenuPosition = position
                        contextMenuType = type
                        showContextMenu = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (showContextMenu) {
                    val selectedRelationship = if (contextMenuType == ContextMenuType.RELATIONSHIP && state.selectedRelationshipId != null) {
                        state.diagram.relationships.find { it.id == state.selectedRelationshipId }
                    } else null
                    val isNtoN = selectedRelationship?.let { relationship ->
                        relationship.connections.size == 2 && relationship.connections.all {
                            it.cardinality == Cardinality.MANY ||
                            it.cardinality == Cardinality.ZERO_MANY ||
                            it.cardinality == Cardinality.ONE_MANY
                        }
                    } ?: false
                    ContextMenu(
                        position = contextMenuPosition,
                        type = contextMenuType,
                        onDismiss = { showContextMenu = false },
                        onEdit = {
                            showContextMenu = false
                            when (contextMenuType) {
                                ContextMenuType.ENTITY -> showEntityDialog = true
                                ContextMenuType.RELATIONSHIP -> showRelationshipDialog = true
                                ContextMenuType.NOTE -> showNoteDialog = true
                            }
                        },
                        onDelete = {
                            showContextMenu = false
                            when (contextMenuType) {
                                ContextMenuType.ENTITY -> {
                                    state.selectedEntityId?.let {
                                        state.deleteEntity(it)
                                        snackbarMessage = "Entità eliminata"
                                    }
                                }
                                ContextMenuType.RELATIONSHIP -> {
                                    state.selectedRelationshipId?.let {
                                        state.deleteRelationship(it)
                                        snackbarMessage = "Relazione eliminata"
                                    }
                                }
                                ContextMenuType.NOTE -> {
                                    state.selectedNoteId?.let {
                                        state.deleteNote(it)
                                        snackbarMessage = "Nota eliminata"
                                    }
                                }
                            }
                        },
                        onAddAttribute = if (contextMenuType != ContextMenuType.NOTE) {
                            {
                                showContextMenu = false
                                showAddAttributeDialog = true
                            }
                        } else null,
                        onAddConnection = if (contextMenuType == ContextMenuType.RELATIONSHIP) {
                            {
                                showContextMenu = false
                                showCreateConnectionDialog = true
                            }
                        } else null,
                        onConvertToAssociativeEntity = if (contextMenuType == ContextMenuType.RELATIONSHIP) {
                            {
                                showContextMenu = false
                                state.selectedRelationshipId?.let {
                                    state.convertToAssociativeEntity(it)
                                    snackbarMessage = "Relazione convertita in entità associativa"
                                }
                            }
                        } else null,
                        isNtoNRelationship = isNtoN
                    )
                }
            }
            if (state.selectedEntityId != null || state.selectedRelationshipId != null || state.selectedNoteId != null) {
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
                        onEditNote = { showNoteDialog = true },
                        onAddAttribute = { showAddAttributeDialog = true },
                        onAddConnection = { showCreateConnectionDialog = true },
                        onEditAttribute = {
                            attributeToEdit = it
                            showEditAttributeDialog = true
                        },
                        onDeleteAttribute = { attributeId ->
                            state.selectedEntityId?.let { entityId ->
                                state.deleteAttributeFromEntity(entityId, attributeId)
                                snackbarMessage = "Attributo eliminato"
                            }
                            state.selectedRelationshipId?.let { relationshipId ->
                                state.deleteAttributeFromRelationship(relationshipId, attributeId)
                                snackbarMessage = "Attributo eliminato"
                            }
                        },
                        onEditConnection = { entityId, connection ->
                            connectionToEdit = Pair(entityId, connection)
                            showEditConnectionDialog = true
                        },
                        onDeleteConnection = { entityId ->
                            state.selectedRelationshipId?.let {
                                state.deleteConnection(it, entityId)
                                snackbarMessage = "Connessione eliminata"
                            }
                        },
                        onConvertToAssociativeEntity = {
                            state.selectedRelationshipId?.let {
                                state.convertToAssociativeEntity(it)
                                snackbarMessage = "Relazione convertita in entità associativa"
                            }
                        },
                        onClose = {
                            state.selectEntity(null)
                            state.selectRelationship(null)
                            state.selectNote(null)
                        }
                    )
                }
            }
        }
    }
    if (showEntityDialog) {
        val entity = state.diagram.entities.find { it.id == state.selectedEntityId }
        entity?.let { e ->
            EntityPropertiesDialog(
                entity = e,
                onDismiss = { showEntityDialog = false },
                onSave = { updatedEntity ->
                    state.updateEntity(e.id) { updatedEntity }
                    showEntityDialog = false
                }
            )
        }
    }
    if (showRelationshipDialog) {
        val relationship = state.diagram.relationships.find { it.id == state.selectedRelationshipId }
        relationship?.let { r ->
            RelationshipPropertiesDialog(
                relationship = r,
                onDismiss = { showRelationshipDialog = false },
                onSave = { updatedRelationship ->
                    state.updateRelationship(r.id) { updatedRelationship }
                    showRelationshipDialog = false
                }
            )
        }
    }
    if (showNoteDialog) {
        val note = state.diagram.notes.find { it.id == state.selectedNoteId }
        note?.let {
            NotePropertiesDialog(
                noteText = it.text,
                onDismiss = { showNoteDialog = false },
                onConfirm = { newText ->
                    state.updateNote(it.id) { note -> note.copy(text = newText) }
                    showNoteDialog = false
                    snackbarMessage = "Nota modificata"
                }
            )
        }
    }
    if (showAddAttributeDialog) {
        AddAttributeDialog(
            onDismiss = { showAddAttributeDialog = false },
            onAdd = { attribute ->
                state.selectedEntityId?.let {
                    state.addAttributeToEntity(it, attribute)
                }
                state.selectedRelationshipId?.let {
                    state.addAttributeToRelationship(it, attribute)
                }
                showAddAttributeDialog = false
                snackbarMessage = "Attributo aggiunto"
            }
        )
    }
    if (showEditAttributeDialog) {
        attributeToEdit?.let { attribute ->
            EditAttributeDialog(
                attribute = attribute,
                onDismiss = {
                    showEditAttributeDialog = false
                    attributeToEdit = null
                },
                onSave = { updatedAttribute ->
                    state.selectedEntityId?.let {
                        state.updateAttributeInEntity(
                            it,
                            attribute.id,
                            updatedAttribute
                        )
                    }
                    state.selectedRelationshipId?.let {
                        state.updateAttributeInRelationship(
                            it,
                            attribute.id,
                            updatedAttribute
                        )
                    }
                    showEditAttributeDialog = false
                    attributeToEdit = null
                    snackbarMessage = "Attributo modificato"
                }
            )
        }
    }
    if (showCreateConnectionDialog) {
        state.selectedRelationshipId?.let { relationshipId ->
            val selectedRelationship = state.diagram.relationships.find { it.id == relationshipId }
            val connectedEntityIds = selectedRelationship?.connections?.map { it.entityId }?.toSet() ?: emptySet()
            val availableEntities = state.diagram.entities.filter { it.id !in connectedEntityIds }
            CreateConnectionDialog(
                entities = availableEntities,
                onDismiss = { showCreateConnectionDialog = false },
                onCreate = { entityId, cardinality ->
                    state.addConnection(relationshipId, entityId, cardinality)
                    showCreateConnectionDialog = false
                    snackbarMessage = "Connessione creata"
                }
            )
        }
    }
    if (showEditConnectionDialog) {
        connectionToEdit?.let { connection ->
            state.selectedRelationshipId?.let { relationshipId ->
                val selectedRelationship = state.diagram.relationships.find { it.id == relationshipId }
                val connectedEntityIds = selectedRelationship?.connections
                    ?.map { it.entityId }
                    ?.filter { it != connection.first }
                    ?.toSet() ?: emptySet()
                val availableEntities = state.diagram.entities.filter { it.id !in connectedEntityIds }
                EditConnectionDialog(
                    entities = availableEntities,
                    currentEntityId = connection.first,
                    currentCardinality = connection.second.cardinality,
                    onDismiss = {
                        showEditConnectionDialog = false
                        connectionToEdit = null
                    },
                    onSave = { newEntityId, newCardinality ->
                        state.updateConnection(
                            relationshipId,
                            connection.first,
                            newEntityId,
                            newCardinality
                        )
                        showEditConnectionDialog = false
                        connectionToEdit = null
                        snackbarMessage = "Connessione modificata"
                    }
                )
            }
        }
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
                    onSuccess = {
                        state.loadDiagram(it, file.absolutePath)
                        snackbarMessage = "Diagramma caricato: ${file.name}"
                    },
                    onFailure = {
                        snackbarMessage = "Errore nel caricamento: ${it.message}"
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
                    onFailure = {
                        snackbarMessage = "Errore nel salvataggio: ${it.message}"
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
                        onFailure = {
                            snackbarMessage = "Errore nell'esportazione: ${it.message}"
                        }
                    )
                } catch (exception: Exception) {
                    snackbarMessage = "Errore nell'esportazione: ${exception.message}"
                }
            }
        )
    }
}
