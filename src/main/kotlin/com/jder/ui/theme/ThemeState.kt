package com.jder.ui.theme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
class ThemeState(initialDarkTheme: Boolean = false) {
    var isDarkTheme by mutableStateOf(initialDarkTheme)
    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }
}
