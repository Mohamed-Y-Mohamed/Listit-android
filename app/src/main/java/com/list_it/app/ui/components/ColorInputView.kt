package com.list_it.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex

/** Colour picker grid backed by the `app_colors` table (iOS `ColorInputView`). */
@Composable
fun ColorInputView(selectedColorHex: String, onSelect: (String) -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Colour", style = IosFont.caption, color = palette.secondary)
        db.listColorHexes.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { hex ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ColorSelectionButton(hex, selectedColorHex == hex) { onSelect(hex) }
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (selectedColorHex.isNotEmpty()) {
            Text(selectedColorHex.uppercase(), style = IosFont.caption.copy(fontFamily = FontFamily.Monospace), color = palette.secondary)
        }
    }
}

@Composable
private fun ColorSelectionButton(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .size(36.dp)
            .clip(shape)
            .background(colorFromHex(hex))
            .then(if (isSelected) Modifier.border(2.5.dp, IosColor.orange, shape) else Modifier)
            .clickable(onClickLabel = hex, onClick = onClick)
            .semantics { contentDescription = "Colour $hex" },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) Icon(Icons.Filled.Check, "Selected", tint = Color.White, modifier = Modifier.size(15.dp))
    }
}
