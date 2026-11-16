package com.list_it.app.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette

/** Explains what each colour on a task's side bar means (overdue, flagged, pinned, normal). */
@Composable
fun ColumnScope.TaskInfoSheet(dismiss: () -> Unit) {
    val palette = LocalPalette.current
    val items = listOf(
        Triple(IosColor.red, "Overdue", "The Task has a due date that has already passed."),
        Triple(IosColor.orange, "Flagged", "The Task is pinned and has an upcoming due date."),
        Triple(IosColor.yellow, "Pinned", "The Task is pinned, with no due date set."),
        Triple(IosColor.cyan, "Normal", "No due date, and not pinned."),
    )
    IosTopBar("Task Information", leading = { BarButton("Dismiss", onClick = dismiss) })
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("The coloured bar on each Task shows its status at a glance.", style = IosFont.subheadline, color = palette.secondary)
        IosDivider()
        Column {
            items.forEachIndexed { index, (color, label, description) ->
                InfoRow(color, label, description)
                if (index < items.size - 1) IosDivider()
            }
        }
    }
}

/** One colour from the key, with its name and what it means. */
@Composable
private fun InfoRow(color: Color, label: String, description: String) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(width = 3.dp, height = 36.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = IosFont.subheadline, fontWeight = FontWeight.SemiBold, color = palette.primary)
            Text(description, style = IosFont.caption, color = palette.secondary)
        }
    }
}
