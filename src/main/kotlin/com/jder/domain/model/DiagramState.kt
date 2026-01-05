package com.jder.domain.model
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import java.util.UUID
class DiagramState {
    var diagram by mutableStateOf(ERDiagram(name = "Nuovo Diagramma"))
        private set
    var selectedEntityId by mutableStateOf<String?>(null)
        private set
    var selectedRelationshipId by mutableStateOf<String?>(null)
        private set
    var selectedNoteId by mutableStateOf<String?>(null)
        private set
    var toolMode by mutableStateOf(ToolMode.SELECT)
    var isModified by mutableStateOf(false)
        private set
    var currentFile by mutableStateOf<String?>(null)
    var zoom by mutableStateOf(1.0f)
    var panOffset by mutableStateOf(Offset.Zero)
    private val undoStack = mutableListOf<ERDiagram>()
    private val redoStack = mutableListOf<ERDiagram>()
    private val maxUndoSize = 50
    private fun saveState() {
        undoStack.add(diagram.copy(
            entities = diagram.entities.map { it.copy(attributes = it.attributes.toList()) },
            relationships = diagram.relationships.map {
                it.copy(
                    attributes = it.attributes.toList(),
                    connections = it.connections.toList()
                )
            },
            notes = diagram.notes.toList()
        ))
        if (undoStack.size > maxUndoSize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }
    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(diagram.copy(
                entities = diagram.entities.map { it.copy(attributes = it.attributes.toList()) },
                relationships = diagram.relationships.map {
                    it.copy(
                        attributes = it.attributes.toList(),
                        connections = it.connections.toList()
                    )
                },
                notes = diagram.notes.toList()
            ))
            diagram = undoStack.removeAt(undoStack.size - 1)
            isModified = true
            if (selectedEntityId != null && diagram.entities.none { it.id == selectedEntityId }) {
                selectedEntityId = null
            }
            if (selectedRelationshipId != null && diagram.relationships.none { it.id == selectedRelationshipId }) {
                selectedRelationshipId = null
            }
            if (selectedNoteId != null && diagram.notes.none { it.id == selectedNoteId }) {
                selectedNoteId = null
            }
        }
    }
    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(diagram.copy(
                entities = diagram.entities.map { it.copy(attributes = it.attributes.toList()) },
                relationships = diagram.relationships.map {
                    it.copy(
                        attributes = it.attributes.toList(),
                        connections = it.connections.toList()
                    )
                },
                notes = diagram.notes.toList()
            ))
            diagram = redoStack.removeAt(redoStack.size - 1)
            isModified = true
            if (selectedEntityId != null && diagram.entities.none { it.id == selectedEntityId }) {
                selectedEntityId = null
            }
            if (selectedRelationshipId != null && diagram.relationships.none { it.id == selectedRelationshipId }) {
                selectedRelationshipId = null
            }
            if (selectedNoteId != null && diagram.notes.none { it.id == selectedNoteId }) {
                selectedNoteId = null
            }
        }
    }
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    fun addEntity(x: Float, y: Float, name: String) {
        saveState()
        val newEntity = Entity(
            id = UUID.randomUUID().toString(),
            name = name,
            x = x,
            y = y
        )
        diagram = diagram.copy(entities = diagram.entities + newEntity)
        isModified = true
    }
    fun addRelationship(x: Float, y: Float, name: String) {
        saveState()
        val newRelationship = Relationship(
            id = UUID.randomUUID().toString(),
            name = name,
            x = x,
            y = y
        )
        diagram = diagram.copy(relationships = diagram.relationships + newRelationship)
        isModified = true
    }
    fun addNote(x: Float, y: Float, text: String) {
        saveState()
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            text = text,
            x = x,
            y = y
        )
        diagram = diagram.copy(notes = diagram.notes + newNote)
        isModified = true
    }
    fun updateEntity(entityId: String, update: (Entity) -> Entity) {
        saveState()
        diagram = diagram.copy(
            entities = diagram.entities.map {
                if (it.id == entityId) update(it) else it
            }
        )
        isModified = true
    }
    fun updateEntityWithoutSave(entityId: String, update: (Entity) -> Entity) {
        diagram = diagram.copy(
            entities = diagram.entities.map {
                if (it.id == entityId) update(it) else it
            }
        )
        isModified = true
    }
    fun updateRelationship(relationshipId: String, update: (Relationship) -> Relationship) {
        saveState()
        diagram = diagram.copy(
            relationships = diagram.relationships.map {
                if (it.id == relationshipId) update(it) else it
            }
        )
        isModified = true
    }
    fun updateRelationshipWithoutSave(relationshipId: String, update: (Relationship) -> Relationship) {
        diagram = diagram.copy(
            relationships = diagram.relationships.map {
                if (it.id == relationshipId) update(it) else it
            }
        )
        isModified = true
    }
    fun updateNote(noteId: String, update: (Note) -> Note) {
        saveState()
        diagram = diagram.copy(
            notes = diagram.notes.map {
                if (it.id == noteId) update(it) else it
            }
        )
        isModified = true
    }
    fun updateNoteWithoutSave(noteId: String, update: (Note) -> Note) {
        diagram = diagram.copy(
            notes = diagram.notes.map {
                if (it.id == noteId) update(it) else it
            }
        )
        isModified = true
    }
    fun saveDragStartState() {
        saveState()
    }
    fun deleteEntity(entityId: String) {
        saveState()
        diagram = diagram.copy(
            entities = diagram.entities.filter { it.id != entityId },
            relationships = diagram.relationships.map {
                it.copy(connections = it.connections.filter { it.entityId != entityId })
            }
        )
        if (selectedEntityId == entityId) {
            selectedEntityId = null
        }
        isModified = true
    }
    fun deleteRelationship(relationshipId: String) {
        saveState()
        diagram = diagram.copy(
            relationships = diagram.relationships.filter { it.id != relationshipId }
        )
        if (selectedRelationshipId == relationshipId) {
            selectedRelationshipId = null
        }
        isModified = true
    }
    fun deleteNote(noteId: String) {
        saveState()
        diagram = diagram.copy(
            notes = diagram.notes.filter { it.id != noteId }
        )
        if (selectedNoteId == noteId) {
            selectedNoteId = null
        }
        isModified = true
    }
    fun addAttributeToEntity(entityId: String, attribute: Attribute) {
        updateEntity(entityId) { entity ->
            entity.copy(attributes = entity.attributes + attribute)
        }
    }
    fun addAttributeToRelationship(relationshipId: String, attribute: Attribute) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(attributes = relationship.attributes + attribute)
        }
    }
    fun deleteAttributeFromEntity(entityId: String, attributeId: String) {
        updateEntity(entityId) { entity ->
            entity.copy(attributes = entity.attributes.filter { it.id != attributeId })
        }
    }
    fun deleteAttributeFromRelationship(relationshipId: String, attributeId: String) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(attributes = relationship.attributes.filter { it.id != attributeId })
        }
    }
    fun updateAttributeInEntity(entityId: String, attributeId: String, newAttribute: Attribute) {
        updateEntity(entityId) { entity ->
            entity.copy(
                attributes = entity.attributes.map {
                    if (it.id == attributeId) newAttribute else it
                }
            )
        }
    }
    fun updateAttributeInRelationship(relationshipId: String, attributeId: String, newAttribute: Attribute) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(
                attributes = relationship.attributes.map {
                    if (it.id == attributeId) newAttribute else it
                }
            )
        }
    }
    fun addConnection(relationshipId: String, entityId: String, cardinality: Cardinality) {
        updateRelationship(relationshipId) { relationship ->
            if (relationship.connections.any { it.entityId == entityId }) {
                relationship
            } else {
                val connection = Connection(entityId = entityId, cardinality = cardinality)
                relationship.copy(connections = relationship.connections + connection)
            }
        }
    }
    fun deleteConnection(relationshipId: String, entityId: String) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(connections = relationship.connections.filter { it.entityId != entityId })
        }
    }
    fun updateConnection(relationshipId: String, oldEntityId: String, newEntityId: String, newCardinality: Cardinality) {
        updateRelationship(relationshipId) { relationship ->
            if (newEntityId != oldEntityId && relationship.connections.any { it.entityId == newEntityId }) {
                relationship
            } else {
                relationship.copy(
                    connections = relationship.connections.map {
                        if (it.entityId == oldEntityId) {
                            Connection(entityId = newEntityId, cardinality = newCardinality)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }
    fun selectEntity(entityId: String?) {
        selectedEntityId = entityId
        selectedRelationshipId = null
        selectedNoteId = null
    }
    fun selectRelationship(relationshipId: String?) {
        selectedRelationshipId = relationshipId
        selectedEntityId = null
        selectedNoteId = null
    }
    fun selectNote(noteId: String?) {
        selectedNoteId = noteId
        selectedEntityId = null
        selectedRelationshipId = null
    }
    fun clearSelection() {
        selectedEntityId = null
        selectedRelationshipId = null
        selectedNoteId = null
    }
    fun loadDiagram(newDiagram: ERDiagram, filePath: String?) {
        diagram = newDiagram
        currentFile = filePath
        isModified = false
        clearSelection()
    }
    fun newDiagram() {
        diagram = ERDiagram(name = "Nuovo Diagramma")
        currentFile = null
        isModified = false
        clearSelection()
    }
    fun markAsSaved(filePath: String) {
        currentFile = filePath
        isModified = false
    }
    fun updateDiagramName(newName: String) {
        diagram = diagram.copy(name = newName)
    }
    fun resetView() {
        zoom = 1.0f
        panOffset = Offset.Zero
    }
    fun convertToAssociativeEntity(relationshipId: String) {
        val relationship = diagram.relationships.find { it.id == relationshipId } ?: return
        if (relationship.connections.size != 2) return
        val isNtoN = relationship.connections.all {
            it.cardinality == Cardinality.MANY ||
            it.cardinality == Cardinality.ZERO_MANY ||
            it.cardinality == Cardinality.ONE_MANY
        }
        if (!isNtoN) return
        saveState()
        val conn1 = relationship.connections[0]
        val conn2 = relationship.connections[1]
        val entity1 = diagram.entities.find { it.id == conn1.entityId }
        val entity2 = diagram.entities.find { it.id == conn2.entityId }
        val centerX = relationship.x + relationship.width / 2
        val centerY = relationship.y + relationship.height / 2
        val newEntity = Entity(
            id = UUID.randomUUID().toString(),
            name = relationship.name,
            x = centerX - 70f,
            y = centerY - 35f,
            attributes = relationship.attributes,
            documentation = relationship.documentation
        )
        val entity1CenterX = entity1?.let { it.x + it.width / 2 } ?: (centerX - 200f)
        val entity1CenterY = entity1?.let { it.y + it.height / 2 } ?: centerY
        val entity2CenterX = entity2?.let { it.x + it.width / 2 } ?: (centerX + 200f)
        val entity2CenterY = entity2?.let { it.y + it.height / 2 } ?: centerY
        val rel1X = (entity1CenterX + centerX) / 2 - 60f
        val rel1Y = (entity1CenterY + centerY) / 2 - 60f
        val rel2X = (entity2CenterX + centerX) / 2 - 60f
        val rel2Y = (entity2CenterY + centerY) / 2 - 60f
        val newRelationships = listOf(
            Relationship(
                id = UUID.randomUUID().toString(),
                name = "Nuova Relazione",
                x = rel1X,
                y = rel1Y,
                connections = listOf(
                    Connection(entityId = newEntity.id, cardinality = conn1.cardinality),
                    Connection(entityId = conn1.entityId, cardinality = Cardinality.ONE_ONE)
                )
            ),
            Relationship(
                id = UUID.randomUUID().toString(),
                name = "Nuova Relazione",
                x = rel2X,
                y = rel2Y,
                connections = listOf(
                    Connection(entityId = newEntity.id, cardinality = conn2.cardinality),
                    Connection(entityId = conn2.entityId, cardinality = Cardinality.ONE_ONE)
                )
            )
        )
        diagram = diagram.copy(
            entities = diagram.entities + newEntity,
            relationships = diagram.relationships.filter { it.id != relationshipId } + newRelationships
        )
        selectedRelationshipId = null
        isModified = true
    }
}
enum class ToolMode {
    SELECT,
    ENTITY,
    RELATIONSHIP,
    NOTE
}
