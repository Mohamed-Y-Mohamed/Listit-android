package com.list_it.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette

/** The big icon and short line of text at the top of every "Add" sheet (list, collection, task, note). */
@Composable
fun SheetHeader(icon: ImageVector, tint: Color, subtitle: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(40.dp))
        subtitle?.let { Text(it, style = IosFont.subheadline, color = LocalPalette.current.secondary, textAlign = TextAlign.Center) }
    }
}
