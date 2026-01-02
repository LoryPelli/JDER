package com.jder.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
@Composable
fun JDERTheme(
    darkTheme: Boolean,
    colorPalette: ColorPalette = ColorPalettes.Blue,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) colorPalette.darkScheme else colorPalette.lightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
