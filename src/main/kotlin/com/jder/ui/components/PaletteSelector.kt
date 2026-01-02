package com.jder.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jder.ui.theme.ColorPalette
import com.jder.ui.theme.ColorPalettes
@Composable
fun PaletteSelector(
    selectedPalette: ColorPalette,
    onPaletteSelected: (ColorPalette) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(selectedPalette.primaryColor)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val palettesPerRow = 5
            val rows = ColorPalettes.allPalettes.chunked(palettesPerRow)
            rows.forEach { rowPalettes ->
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPalettes.forEach { palette ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.primaryColor)
                                .border(
                                    width = if (palette == selectedPalette) 3.dp else 1.dp,
                                    color = if (palette == selectedPalette) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onPaletteSelected(palette)
                                    expanded = false
                                }
                        )
                    }
                }
            }
        }
    }
}
