package com.jder.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jder.domain.model.Attribute
import com.jder.domain.model.Cardinality
import com.jder.domain.model.Connection
import com.jder.domain.model.DiagramState
import com.jder.domain.model.Entity
import com.jder.domain.model.Note
import com.jder.domain.model.Relationship
@Composable
fun PropertiesPanel(
    state: DiagramState,
    onEditEntity: () -> Unit,
    onEditRelationship: () -> Unit,
    onEditNote: () -> Unit,
    onAddAttribute: () -> Unit,
    onAddConnection: () -> Unit,
    onEditAttribute: (Attribute) -> Unit,
    onDeleteAttribute: (String) -> Unit,
    onEditConnection: (String, Connection) -> Unit,
    onDeleteConnection: (String) -> Unit,
    onConvertToAssociativeEntity: () -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Proprietà",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Chiudi pannello proprietà",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Divider()
        state.selectedEntityId?.let { selId ->
            val entity = state.diagram.entities.find { it.id == selId }
            entity?.let {
                EntityPropertiesContent(
                    entity = it,
                    onEditEntity = onEditEntity,
                    onAddAttribute = onAddAttribute,
                    onEditAttribute = onEditAttribute,
                    onDeleteAttribute = onDeleteAttribute
                )
            }
        }
        state.selectedRelationshipId?.let { selId ->
            val relationship = state.diagram.relationships.find { it.id == selId }
            relationship?.let {
                RelationshipPropertiesContent(
                    relationship = it,
                    entities = state.diagram.entities,
                    onEditRelationship = onEditRelationship,
                    onAddAttribute = onAddAttribute,
                    onAddConnection = onAddConnection,
                    onEditAttribute = onEditAttribute,
                    onDeleteAttribute = onDeleteAttribute,
                    onEditConnection = onEditConnection,
                    onDeleteConnection = onDeleteConnection,
                    onConvertToAssociativeEntity = onConvertToAssociativeEntity
                )
            }
        }
        state.selectedNoteId?.let { selId ->
            val note = state.diagram.notes.find { it.id == selId }
            note?.let {
                NotePropertiesContent(
                    note = it,
                    onEditNote = onEditNote
                )
            }
        }
    }
}
@Composable
private fun EntityPropertiesContent(
    entity: Entity,
    onEditEntity: () -> Unit,
    onAddAttribute: () -> Unit,
    onEditAttribute: (Attribute) -> Unit,
    onDeleteAttribute: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Entità: ${entity.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tipo: ${if (entity.isWeak) "Debole" else "Forte"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Attributi: ${entity.attributes.size}", style = MaterialTheme.typography.bodyMedium)
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
    if (entity.attributes.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Attributi:", style = MaterialTheme.typography.titleSmall)
        entity.attributes.forEach {
            AttributeCard(
                attribute = it,
                onEdit = { onEditAttribute(it) },
                onDelete = { onDeleteAttribute(it.id) }
            )
        }
    }
    if (entity.documentation.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        DocumentationCard(documentation = entity.documentation)
    }
}
@Composable
private fun RelationshipPropertiesContent(
    relationship: Relationship,
    entities: List<Entity>,
    onEditRelationship: () -> Unit,
    onAddAttribute: () -> Unit,
    onAddConnection: () -> Unit,
    onEditAttribute: (Attribute) -> Unit,
    onDeleteAttribute: (String) -> Unit,
    onEditConnection: (String, Connection) -> Unit,
    onDeleteConnection: (String) -> Unit,
    onConvertToAssociativeEntity: () -> Unit
) {
    val isNtoN = relationship.connections.size == 2 && relationship.connections.all {
        it.cardinality == Cardinality.MANY ||
        it.cardinality == Cardinality.ZERO_MANY ||
        it.cardinality == Cardinality.ONE_MANY
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Relazione: ${relationship.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text("Connessioni: ${relationship.connections.size}", style = MaterialTheme.typography.bodyMedium)
            Text("Attributi: ${relationship.attributes.size}", style = MaterialTheme.typography.bodyMedium)
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
    OutlinedButton(
        onClick = onConvertToAssociativeEntity,
        modifier = Modifier.fillMaxWidth(),
        enabled = isNtoN
    ) {
        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Converti in Entità Associativa")
    }
    if (relationship.attributes.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Attributi:", style = MaterialTheme.typography.titleSmall)
        relationship.attributes.forEach {
            AttributeCard(
                attribute = it,
                onEdit = { onEditAttribute(it) },
                onDelete = { onDeleteAttribute(it.id) }
            )
        }
    }
    if (relationship.connections.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Connessioni:", style = MaterialTheme.typography.titleSmall)
        relationship.connections.forEach {
            val connectedEntity = entities.find { entity -> entity.id == it.entityId }
            ConnectionCard(
                connection = it,
                entityName = connectedEntity?.name ?: "Entità sconosciuta",
                onEdit = { onEditConnection(it.entityId, it) },
                onDelete = { onDeleteConnection(it.entityId) }
            )
        }
    }
    if (relationship.documentation.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        DocumentationCard(documentation = relationship.documentation)
    }
}
@Composable
private fun AttributeCard(
    attribute: Attribute,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attribute.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (attribute.isPrimaryKey) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    attribute.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica attributo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
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
@Composable
private fun ConnectionCard(
    connection: Connection,
    entityName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    entityName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Cardinalità: ${connection.cardinality.display}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica connessione",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
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
@Composable
private fun DocumentationCard(documentation: String) {
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
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                documentation,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
@Composable
private fun NotePropertiesContent(
    note: Note,
    onEditNote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Nota",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                note.text.take(50) + if (note.text.length > 50) "..." else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onEditNote,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Modifica Testo")
    }
}
