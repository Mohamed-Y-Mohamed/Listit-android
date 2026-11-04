package com.list_it.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette

/** iOS `.pickerStyle(.segmented)`. */
@Composable
fun <T> SegmentedControl(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(palette.fill).padding(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(7.dp)) else Modifier)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) (if (palette.isDark) palette.fill else palette.systemBackground) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 7.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option), style = IosFont.footnote, textAlign = TextAlign.Center, maxLines = 1,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = palette.primary,
                )
            }
        }
    }
}
