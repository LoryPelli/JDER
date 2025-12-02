package com.jder.ui.dialogs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jder.domain.model.Attribute
import com.jder.domain.model.AttributeType
import java.util.UUID
@Composable
fun AddAttributeDialog(
    onDismiss: () -> Unit,
    onAdd: (Attribute) -> Unit
) {
    AttributeDialog(
        title = "Aggiungi Attributo",
        initialAttribute = null,
        onDismiss = onDismiss,
        onConfirm = onAdd
    )
}
@Composable
fun EditAttributeDialog(
    attribute: Attribute,
    onDismiss: () -> Unit,
    onSave: (Attribute) -> Unit
) {
    AttributeDialog(
        title = "Modifica Attributo",
        initialAttribute = attribute,
        onDismiss = onDismiss,
        onConfirm = onSave
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeDialog(
    title: String,
    initialAttribute: Attribute?,
    onDismiss: () -> Unit,
    onConfirm: (Attribute) -> Unit
) {
    var name by remember { mutableStateOf(initialAttribute?.name ?: "") }
    var isPrimaryKey by remember { mutableStateOf(initialAttribute?.isPrimaryKey ?: false) }
    var attributeType by remember { mutableStateOf(initialAttribute?.type ?: AttributeType.NORMAL) }
    var multiplicity by remember { mutableStateOf(initialAttribute?.multiplicity ?: "") }
    var components by remember { mutableStateOf(initialAttribute?.components ?: emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var showAddComponentDialog by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chiave Primaria")
                    Switch(
                        checked = isPrimaryKey,
                        onCheckedChange = { isPrimaryKey = it },
                        enabled = attributeType != AttributeType.KEY
                    )
                }
                AnimatedVisibility(
                    visible = attributeType == AttributeType.KEY,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text = "Gli attributi di tipo Chiave sono sempre chiavi primarie",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (attributeType) {
                            AttributeType.NORMAL -> "Normale"
                            AttributeType.KEY -> "Chiave"
                            AttributeType.MULTIVALUED -> "Multivalore"
                            AttributeType.DERIVED -> "Derivato"
                            AttributeType.COMPOSITE -> "Composto"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Normale") },
                            onClick = {
                                attributeType = AttributeType.NORMAL
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Chiave") },
                            onClick = {
                                attributeType = AttributeType.KEY
                                isPrimaryKey = true
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Multivalore") },
                            onClick = {
                                attributeType = AttributeType.MULTIVALUED
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Derivato") },
                            onClick = {
                                attributeType = AttributeType.DERIVED
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Composto") },
                            onClick = {
                                attributeType = AttributeType.COMPOSITE
                                isPrimaryKey = false
                                expanded = false
                            }
                        )
                    }
                }
                AnimatedVisibility(
                    visible = attributeType == AttributeType.MULTIVALUED,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = multiplicity,
                        onValueChange = { multiplicity = it },
                        label = { Text("Molteplicità (es: 1..N, 0..N)") },
                        placeholder = { Text("1..N") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AnimatedVisibility(
                    visible = attributeType == AttributeType.COMPOSITE,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Componenti (${components.size})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            IconButton(onClick = { showAddComponentDialog = true }) {
                                Icon(Icons.Default.Add, "Aggiungi componente")
                            }
                        }
                        if (components.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                components.forEach { component ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                component.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            IconButton(
                                                onClick = {
                                                    components = components.filter { it.id != component.id }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    "Rimuovi componente",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Nessun componente. Aggiungi componenti per l'attributo composto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalIsPrimaryKey = attributeType == AttributeType.KEY || isPrimaryKey
                        val newAttribute = Attribute(
                            id = initialAttribute?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            type = attributeType,
                            isPrimaryKey = finalIsPrimaryKey,
                            components = if (attributeType == AttributeType.COMPOSITE) components else emptyList(),
                            multiplicity = if (attributeType == AttributeType.MULTIVALUED) multiplicity else ""
                        )
                        onConfirm(newAttribute)
                    }
                },
                enabled = name.isNotBlank() && (attributeType != AttributeType.COMPOSITE || components.isNotEmpty())
            ) {
                Text(if (initialAttribute != null) "Salva" else "Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
    if (showAddComponentDialog) {
        var componentName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text("Aggiungi Componente") },
            text = {
                OutlinedTextField(
                    value = componentName,
                    onValueChange = { componentName = it },
                    label = { Text("Nome componente") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (componentName.isNotBlank()) {
                            val newComponent = Attribute(
                                id = UUID.randomUUID().toString(),
                                name = componentName,
                                type = AttributeType.NORMAL,
                                isPrimaryKey = false,
                                components = emptyList()
                            )
                            components = components + newComponent
                            showAddComponentDialog = false
                        }
                    },
                    enabled = componentName.isNotBlank()
                ) {
                    Text("Aggiungi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddComponentDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
