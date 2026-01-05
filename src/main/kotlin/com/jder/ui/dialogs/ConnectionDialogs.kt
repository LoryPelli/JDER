package com.jder.ui.dialogs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jder.domain.model.Cardinality
import com.jder.domain.model.Entity
@Composable
fun CreateConnectionDialog(
    entities: List<Entity>,
    onDismiss: () -> Unit,
    onCreate: (String, Cardinality) -> Unit
) {
    ConnectionDialog(
        title = "Crea Connessione",
        entities = entities,
        initialEntityId = null,
        initialCardinality = Cardinality.ONE,
        onDismiss = onDismiss,
        onConfirm = onCreate
    )
}
@Composable
fun EditConnectionDialog(
    entities: List<Entity>,
    currentEntityId: String,
    currentCardinality: Cardinality,
    onDismiss: () -> Unit,
    onSave: (String, Cardinality) -> Unit
) {
    ConnectionDialog(
        title = "Modifica Connessione",
        entities = entities,
        initialEntityId = currentEntityId,
        initialCardinality = currentCardinality,
        onDismiss = onDismiss,
        onConfirm = onSave
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionDialog(
    title: String,
    entities: List<Entity>,
    initialEntityId: String?,
    initialCardinality: Cardinality,
    onDismiss: () -> Unit,
    onConfirm: (String, Cardinality) -> Unit
) {
    var selectedEntity by remember { mutableStateOf(entities.find { it.id == initialEntityId }) }
    var selectedCardinality by remember { mutableStateOf(initialCardinality) }
    var expandedEntity by remember { mutableStateOf(false) }
    var expandedCardinality by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (entities.isEmpty()) {
                    Text(
                        text = "Nessuna entità disponibile. Tutte le entità sono già connesse a questa relazione.",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedEntity,
                        onExpandedChange = { expandedEntity = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEntity?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Entità") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEntity) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedEntity,
                            onDismissRequest = { expandedEntity = false }
                        ) {
                            entities.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    onClick = {
                                        selectedEntity = it
                                        expandedEntity = false
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = expandedCardinality,
                        onExpandedChange = { expandedCardinality = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCardinality.display,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cardinalità") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCardinality) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCardinality,
                            onDismissRequest = { expandedCardinality = false }
                        ) {
                            Cardinality.entries.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.display) },
                                    onClick = {
                                        selectedCardinality = it
                                        expandedCardinality = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (entities.isNotEmpty()) {
                Button(
                    onClick = {
                        selectedEntity?.let { entity ->
                            onConfirm(entity.id, selectedCardinality)
                        }
                    },
                    enabled = selectedEntity != null
                ) {
                    Text(if (initialEntityId != null) "Salva" else "Crea")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (entities.isEmpty()) "Chiudi" else "Annulla")
            }
        }
    )
}
