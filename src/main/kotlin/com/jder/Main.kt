package com.jder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.foundation.isSystemInDarkTheme
import com.jder.domain.model.DiagramState
import com.jder.ui.screens.MainScreen
import com.jder.ui.theme.JDERTheme
import com.jder.ui.theme.ThemeState
fun main() = application {
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp
    )
    val diagramState = remember { DiagramState() }
    val isSystemInDark = isSystemInDarkTheme()
    val themeState = remember { ThemeState(initialDarkTheme = isSystemInDark) }
    var showExitDialog by remember { mutableStateOf(false) }
    var shouldExit by remember { mutableStateOf(false) }
    if (shouldExit) {
        exitApplication()
    }
    Window(
        onCloseRequest = {
            if (diagramState.isModified) {
                showExitDialog = true
            } else {
                exitApplication()
            }
        },
        title = "JDER - Java Diagrammi E/R",
        state = windowState
    ) {
        JDERTheme(darkTheme = themeState.isDarkTheme) {
            MainScreen(
                state = diagramState,
                themeState = themeState
            )
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Conferma chiusura") },
                    text = { Text("Ci sono modifiche non salvate. Vuoi uscire comunque?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showExitDialog = false
                                shouldExit = true
                            }
                        ) {
                            Text("Sì")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExitDialog = false }
                        ) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}
