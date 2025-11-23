package com.jder.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
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
import com.jder.domain.model.AttributeType
import com.jder.domain.model.Connection
import com.jder.domain.model.DiagramState
import com.jder.domain.model.ERDiagram
import com.jder.domain.model.ToolMode
import com.jder.ui.components.ContextMenu
import com.jder.ui.components.DiagramToolbar
import com.jder.ui.components.ERDiagramCanvas
import com.jder.ui.components.FileManagerDialog
import com.jder.ui.components.FileManagerMode
import com.jder.ui.dialogs.AddAttributeDialog
import com.jder.ui.dialogs.CreateConnectionDialog
import com.jder.ui.dialogs.EditAttributeDialog
import com.jder.ui.dialogs.EditConnectionDialog
import com.jder.ui.dialogs.EntityPropertiesDialog
import com.jder.ui.dialogs.RelationshipPropertiesDialog
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
@Composable
fun MainScreen(
    state: DiagramState = remember { DiagramState() },
    repository: DiagramRepository = remember { DiagramRepository() }
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
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var contextMenuForEntity by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        showOpenDialog = true
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.S -> {
                        if (state.currentFile != null) {
                            val file = File(state.currentFile!!)
                            repository.saveDiagram(state.diagram, file).fold(
                                onSuccess = {
                                    state.markAsSaved(file.absolutePath)
                                    snackbarMessage = "Salvato"
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
                onNewDiagram = {
                    if (state.isModified) {
                        showNewDiagramConfirmDialog = true
                    } else {
                        state.newDiagram()
                        snackbarMessage = "Nuovo diagramma creato"
                    }
                },
                onOpenDiagram = {
                    showOpenDialog = true
                },
                onSaveDiagram = {
                    if (state.currentFile != null) {
                        val file = File(state.currentFile!!)
                        repository.saveDiagram(state.diagram, file).fold(
                            onSuccess = {
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
                }
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
        CreateConnectionDialog(
            entities = state.diagram.entities,
            onDismiss = { showCreateConnectionDialog = false },
            onCreate = { entityId, cardinality ->
                state.addConnection(state.selectedRelationshipId!!, entityId, cardinality)
                showCreateConnectionDialog = false
                snackbarMessage = "Connessione creata"
            }
        )
    }
    if (showEditConnectionDialog && connectionToEdit != null && state.selectedRelationshipId != null) {
        EditConnectionDialog(
            entities = state.diagram.entities,
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
    if (showOpenDialog) {
        FileManagerDialog(
            mode = FileManagerMode.OPEN,
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
    if (showSaveAsDialog || showSaveDialog) {
        FileManagerDialog(
            mode = FileManagerMode.SAVE,
            fileExtension = ".json",
            title = "Salva Diagramma",
            onDismiss = {
                showSaveAsDialog = false
                showSaveDialog = false
            },
            onFileSelected = { file ->
                showSaveAsDialog = false
                showSaveDialog = false
                val finalFile = if (file.extension != "json") {
                    File(file.parentFile, "${file.nameWithoutExtension}.json")
                } else file
                repository.saveDiagram(state.diagram, finalFile).fold(
                    onSuccess = {
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
fun renderDiagramToBitmap(diagram: ERDiagram): ImageBitmap {
    val padding = 150f
    val entities = diagram.entities
    val relationships = diagram.relationships
    val minX = (entities.minOfOrNull { it.x } ?: 0f).coerceAtMost(
        relationships.minOfOrNull { it.x } ?: 0f
    ) - padding
    val minY = (entities.minOfOrNull { it.y } ?: 0f).coerceAtMost(
        relationships.minOfOrNull { it.y } ?: 0f
    ) - padding
    val maxX = (entities.maxOfOrNull { it.x + it.width } ?: 1000f).coerceAtLeast(
        relationships.maxOfOrNull { it.x + it.width } ?: 1000f
    ) + padding
    val maxY = (entities.maxOfOrNull { it.y + it.height } ?: 1000f).coerceAtLeast(
        relationships.maxOfOrNull { it.y + it.height } ?: 1000f
    ) + padding
    val width = (maxX - minX).toInt().coerceAtLeast(800)
    val height = (maxY - minY).toInt().coerceAtLeast(600)
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2d = bufferedImage.createGraphics()
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2d.color = Color.WHITE
    g2d.fillRect(0, 0, width, height)
    val offsetX = -minX
    val offsetY = -minY
    g2d.color = Color(0xBDBDBD)
    g2d.stroke = BasicStroke(2f)
    relationships.forEach { relationship ->
        val centerX = (relationship.x + relationship.width / 2 + offsetX).toInt()
        val centerY = (relationship.y + relationship.height / 2 + offsetY).toInt()
        relationship.connections.forEach { conn ->
            val entity = entities.find { it.id == conn.entityId }
            entity?.let {
                val entityCenterX = (it.x + it.width / 2 + offsetX).toInt()
                val entityCenterY = (it.y + it.height / 2 + offsetY).toInt()
                g2d.drawLine(centerX, centerY, entityCenterX, entityCenterY)
                val labelX = (centerX + entityCenterX) / 2
                val labelY = (centerY + entityCenterY) / 2
                g2d.font = Font("Arial", Font.BOLD, 14)
                g2d.color = Color(0x222222)
                g2d.drawString(conn.cardinality.display, labelX - 10, labelY)
                g2d.color = Color(0xBDBDBD)
            }
        }
    }
    g2d.stroke = BasicStroke(2.5f)
    entities.forEach { entity ->
        val x = (entity.x + offsetX).toInt()
        val y = (entity.y + offsetY).toInt()
        val w = entity.width.toInt()
        val h = entity.height.toInt()
        g2d.color = Color.WHITE
        g2d.fillRect(x, y, w, h)
        g2d.color = Color(0x64B5F6)
        g2d.drawRect(x, y, w, h)
        g2d.color = Color.BLACK
        g2d.font = Font("Arial", Font.BOLD, 14)
        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(entity.name)
        g2d.drawString(entity.name, x + (w - textWidth) / 2, y + h / 2 + fm.ascent / 2)
        entity.attributes.forEachIndexed { index, attribute ->
            val attrX = (entity.x + entity.width + offsetX + 50).toInt()
            val verticalSpacing = 45
            val offsetY2 = (index - entity.attributes.size / 2f) * verticalSpacing
            val attrY = (entity.y + entity.height / 2 + offsetY + offsetY2).toInt()
            val radius = 20
            g2d.color = Color(0xBDBDBD)
            g2d.drawLine(x + w, y + h / 2, attrX, attrY)
            g2d.color = Color.WHITE
            g2d.fillOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
            val attrColor = when {
                attribute.isPrimaryKey -> Color(0xFFEB3B)
                attribute.type == AttributeType.COMPOSITE -> Color(0xFFA726)
                else -> Color(0x90CAF9)
            }
            g2d.color = attrColor
            g2d.drawOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
            g2d.font = Font("Arial", Font.BOLD, 12)
            g2d.color = Color.BLACK
            g2d.drawString(attribute.name, attrX + radius + 10, attrY + 5)
        }
    }
    relationships.forEach { relationship ->
        val centerX = (relationship.x + relationship.width / 2 + offsetX).toInt()
        val centerY = (relationship.y + relationship.height / 2 + offsetY).toInt()
        val halfWidth = (relationship.width / 2).toInt()
        val halfHeight = (relationship.height / 2).toInt()
        val xPoints = intArrayOf(
            centerX,
            centerX + halfWidth,
            centerX,
            centerX - halfWidth
        )
        val yPoints = intArrayOf(
            centerY - halfHeight,
            centerY,
            centerY + halfHeight,
            centerY
        )
        g2d.color = Color.WHITE
        g2d.fillPolygon(xPoints, yPoints, 4)
        g2d.color = Color(0xE57373)
        g2d.drawPolygon(xPoints, yPoints, 4)
        g2d.color = Color.BLACK
        g2d.font = Font("Arial", Font.BOLD, 14)
        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(relationship.name)
        g2d.drawString(relationship.name, centerX - textWidth / 2, centerY + fm.ascent / 2)
    }
    g2d.dispose()
    return bufferedImage.toComposeImageBitmap()
}
@Composable
fun PropertiesPanel(
    state: DiagramState,
    onEditEntity: () -> Unit,
    onEditRelationship: () -> Unit,
    onAddAttribute: () -> Unit,
    onAddConnection: () -> Unit,
    onEditAttribute: (Attribute) -> Unit,
    onDeleteAttribute: (String) -> Unit,
    onEditConnection: (String, Connection) -> Unit,
    onDeleteConnection: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Proprietà",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Divider()
        when {
            state.selectedEntityId != null -> {
                val entity = state.diagram.entities.find { it.id == state.selectedEntityId }
                entity?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Entità: ${it.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tipo: ${if (it.isWeak) "Debole" else "Forte"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Attributi: ${it.attributes.size}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onEditEntity,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Modifica Proprietà")
                    }
                    OutlinedButton(
                        onClick = onAddAttribute,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Aggiungi Attributo")
                    }
                    if (it.attributes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Attributi:", style = MaterialTheme.typography.titleSmall)
                        it.attributes.forEach { attr ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            attr.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (attr.isPrimaryKey)
                                                androidx.compose.ui.text.font.FontWeight.Bold
                                            else
                                                androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                        Text(
                                            attr.type.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                onEditAttribute(attr)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Modifica attributo",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                onDeleteAttribute(attr.id)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Elimina attributo",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (it.documentation.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Documentazione:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    it.documentation,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            state.selectedRelationshipId != null -> {
                val relationship = state.diagram.relationships.find { it.id == state.selectedRelationshipId }
                relationship?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Relazione: ${it.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Connessioni: ${it.connections.size}", style = MaterialTheme.typography.bodyMedium)
                            Text("Attributi: ${it.attributes.size}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onEditRelationship,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Modifica Proprietà")
                    }
                    OutlinedButton(
                        onClick = onAddAttribute,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Aggiungi Attributo")
                    }
                    OutlinedButton(
                        onClick = onAddConnection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Aggiungi Connessione")
                    }
                    if (it.attributes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Attributi:", style = MaterialTheme.typography.titleSmall)
                        it.attributes.forEach { attr ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            attr.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (attr.isPrimaryKey)
                                                androidx.compose.ui.text.font.FontWeight.Bold
                                            else
                                                androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                        Text(
                                            attr.type.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                onEditAttribute(attr)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Modifica attributo",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                onDeleteAttribute(attr.id)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Elimina attributo",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (it.connections.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Connessioni:", style = MaterialTheme.typography.titleSmall)
                        it.connections.forEach { conn ->
                            val connectedEntity = state.diagram.entities.find { e -> e.id == conn.entityId }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            connectedEntity?.name ?: "Entità sconosciuta",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Cardinalità: ${conn.cardinality.display}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                onEditConnection(conn.entityId, conn)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Modifica connessione",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                onDeleteConnection(conn.entityId)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Elimina connessione",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (it.documentation.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Documentazione:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    it.documentation,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
