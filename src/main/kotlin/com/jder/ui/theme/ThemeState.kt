package com.jder.ui.theme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
class ThemeState(
    initialDarkTheme: Boolean = false,
    initialPalette: ColorPalette = ColorPalettes.Blue
) {
    var isDarkTheme by mutableStateOf(initialDarkTheme)
    var selectedPalette by mutableStateOf(initialPalette)
    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }
    fun selectPalette(palette: ColorPalette) {
        selectedPalette = palette
    }
}
