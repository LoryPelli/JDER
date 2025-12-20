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
            }
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
                }
            ))
            diagram = undoStack.removeAt(undoStack.size - 1)
            isModified = true
            if (selectedEntityId != null && diagram.entities.none { it.id == selectedEntityId }) {
                selectedEntityId = null
            }
            if (selectedRelationshipId != null && diagram.relationships.none { it.id == selectedRelationshipId }) {
                selectedRelationshipId = null
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
                }
            ))
            diagram = redoStack.removeAt(redoStack.size - 1)
            isModified = true
            if (selectedEntityId != null && diagram.entities.none { it.id == selectedEntityId }) {
                selectedEntityId = null
            }
            if (selectedRelationshipId != null && diagram.relationships.none { it.id == selectedRelationshipId }) {
                selectedRelationshipId = null
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
    fun saveDragStartState() {
        saveState()
    }
    fun deleteEntity(entityId: String) {
        saveState()
        diagram = diagram.copy(
            entities = diagram.entities.filter { it.id != entityId },
            relationships = diagram.relationships.map { rel ->
                rel.copy(connections = rel.connections.filter { it.entityId != entityId })
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
                    connections = relationship.connections.map { conn ->
                        if (conn.entityId == oldEntityId) {
                            Connection(entityId = newEntityId, cardinality = newCardinality)
                        } else {
                            conn
                        }
                    }
                )
            }
        }
    }
    fun selectEntity(entityId: String?) {
        selectedEntityId = entityId
        selectedRelationshipId = null
    }
    fun selectRelationship(relationshipId: String?) {
        selectedRelationshipId = relationshipId
        selectedEntityId = null
    }
    fun clearSelection() {
        selectedEntityId = null
        selectedRelationshipId = null
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
}
enum class ToolMode {
    SELECT,
    ENTITY,
    RELATIONSHIP
}
