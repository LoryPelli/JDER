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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.io.File
enum class FileManagerMode {
    SAVE, OPEN
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerDialog(
    mode: FileManagerMode,
    initialDirectory: File,
    fileExtension: String,
    title: String,
    defaultFileName: String? = null,
    onDismiss: () -> Unit,
    onFileSelected: (File) -> Unit
) {
    var currentDirectory by remember { mutableStateOf(initialDirectory) }
    var fileName by remember { mutableStateOf(defaultFileName ?: "") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showInvalidPathDialog by remember { mutableStateOf(false) }
    var invalidPathError by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }
    var showOnlySupportedFiles by remember { mutableStateOf(false) }
    var pathInput by remember {
        mutableStateOf(TextFieldValue(
            text = currentDirectory.absolutePath,
            selection = TextRange(currentDirectory.absolutePath.length)
        ))
    }
    val pathFocusRequester = remember { FocusRequester() }
    LaunchedEffect(currentDirectory) {
        pathInput = TextFieldValue(
            text = currentDirectory.absolutePath,
            selection = TextRange(currentDirectory.absolutePath.length)
        )
    }
    LaunchedEffect(currentDirectory) {
        if (!currentDirectory.exists() || !currentDirectory.isDirectory) {
            currentDirectory = File(System.getProperty("user.home"))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(700.dp).height(600.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentDirectory.parentFile?.let { parent ->
                                if (parent.exists() && parent.canRead()) {
                                    currentDirectory = parent
                                    selectedFile = null
                                }
                            }
                        },
                        enabled = currentDirectory.parentFile != null
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cartella superiore")
                    }
                    OutlinedTextField(
                        value = pathInput,
                        onValueChange = { pathInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(pathFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter) {
                                    val newPath = File(pathInput.text)
                                    when {
                                        !newPath.exists() -> {
                                            invalidPathError = "Il percorso specificato non esiste."
                                            showInvalidPathDialog = true
                                        }
                                        !newPath.isDirectory -> {
                                            invalidPathError = "Il percorso specificato non è una cartella."
                                            showInvalidPathDialog = true
                                        }
                                        !newPath.canRead() -> {
                                            invalidPathError = "Non hai i permessi per accedere a questa cartella."
                                            showInvalidPathDialog = true
                                        }
                                        else -> {
                                            currentDirectory = newPath
                                            selectedFile = null
                                            fileName = ""
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                        label = { Text("Percorso") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = {
                            refreshTrigger++
                            selectedFile = null
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ricarica")
                    }
                }
                if (mode == FileManagerMode.OPEN && fileExtension == ".json") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mostra solo file JSON",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = showOnlySupportedFiles,
                            onCheckedChange = { showOnlySupportedFiles = it }
                        )
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    val files = remember(currentDirectory, refreshTrigger, showOnlySupportedFiles) {
                        try {
                            val allFiles = currentDirectory.listFiles()?.sortedWith(
                                compareBy<File> { !it.isDirectory }
                                    .thenBy { it.name.lowercase() }
                            ) ?: emptyList()
                            if (showOnlySupportedFiles) {
                                allFiles.filter { file ->
                                    file.isDirectory || file.extension == "json"
                                }
                            } else {
                                allFiles
                            }
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    if (files.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    "Cartella vuota",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(files) { file ->
                                FileItem(
                                    file = file,
                                    isSelected = selectedFile == file,
                                    fileExtension = fileExtension,
                                    mode = mode,
                                    onClick = {
                                        if (file.isDirectory) {
                                            if (file.canRead()) {
                                                currentDirectory = file
                                                selectedFile = null
                                                fileName = ""
                                            }
                                        } else {
                                            selectedFile = file
                                            fileName = file.nameWithoutExtension
                                        }
                                    },
                                    onDoubleClick = {
                                        if (file.isDirectory) {
                                            if (file.canRead()) {
                                                currentDirectory = file
                                                selectedFile = null
                                                fileName = ""
                                            }
                                        } else if (mode == FileManagerMode.OPEN && file.extension == fileExtension.removePrefix(".")) {
                                            onFileSelected(file)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                if (mode == FileManagerMode.SAVE) {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                            errorMessage = null
                        },
                        label = { Text("Nome file") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = fileExtension,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        isError = errorMessage != null
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annulla")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            when (mode) {
                                FileManagerMode.SAVE -> {
                                    if (fileName.isBlank()) {
                                        errorMessage = "Inserisci un nome per il file"
                                        return@Button
                                    }
                                    val file = File(currentDirectory, "$fileName$fileExtension")
                                    onFileSelected(file)
                                }
                                FileManagerMode.OPEN -> {
                                    selectedFile?.let { file ->
                                        if (file.exists() && file.isFile) {
                                            onFileSelected(file)
                                        }
                                    }
                                }
                            }
                        },
                        enabled = when (mode) {
                            FileManagerMode.SAVE -> fileName.isNotBlank()
                            FileManagerMode.OPEN -> selectedFile != null && selectedFile?.isFile == true
                        }
                    ) {
                        Icon(
                            if (mode == FileManagerMode.SAVE) Icons.Default.Save else Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (mode == FileManagerMode.SAVE) "Salva" else "Apri")
                    }
                }
            }
        }
    }
    if (showInvalidPathDialog) {
        AlertDialog(
            onDismissRequest = {
                showInvalidPathDialog = false
                val currentPath = currentDirectory.absolutePath
                pathInput = TextFieldValue(
                    text = currentPath,
                    selection = TextRange(currentPath.length)
                )
            },
            title = {
                Text(
                    text = "Percorso non valido",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = invalidPathError,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInvalidPathDialog = false
                        val currentPath = currentDirectory.absolutePath
                        pathInput = TextFieldValue(
                            text = currentPath,
                            selection = TextRange(currentPath.length)
                        )
                    }
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
