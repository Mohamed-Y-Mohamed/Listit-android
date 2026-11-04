package com.list_it.app.ui.screens.defaultlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.ToDoTask
import com.list_it.app.ui.Dates
import com.list_it.app.ui.Dates.day
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.ContentUnavailable
import com.list_it.app.ui.components.CountBadge
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.materialCard
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette

/** iOS `OverdueView`: tasks whose due day has passed. */
@Composable
fun OverdueScreen(onBack: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    var searchText by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf<ToDoTask?>(null) }

    val today = Dates.today()
    val overdue = db.tasks.filter { t -> val due = t.dueDate; due != null && !t.isCompleted && !t.isDeleted && due.day() < today }
    val grouped = db.lists.filter { it.listName.matchesListQuery(searchText) }.mapNotNull { list ->
        val tasks = overdue.filter { it.listID == list.id }
        if (tasks.isEmpty()) null else list to tasks
    }

    DefaultListScaffold("Overdue", onBack, searchText, { searchText = it }, "Search Lists...", selectedTask = selectedTask, onTaskDismiss = { selectedTask = null }) {
        if (grouped.isEmpty()) {
            ContentUnavailable(
                if (searchText.isEmpty()) "All Caught Up" else "No Overdue Lists Found",
                if (searchText.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Search,
                description = if (searchText.isEmpty()) "You have no overdue tasks." else "Try searching for a different list.",
            )
        } else {
            Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                grouped.forEach { (list, tasks) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(list.listName, style = IosFont.headline, fontWeight = FontWeight.Bold, color = palette.primary, modifier = Modifier.weight(1f))
                            CountBadge(
                                "${tasks.size} Overdue", IosColor.red.copy(alpha = 0.1f), IosColor.red,
                                padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        tasks.forEach { task -> OverdueTaskCard(task, Modifier.clickable { selectedTask = task }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueTaskCard(task: ToDoTask, modifier: Modifier) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val collectionName = db.collections.firstOrNull { it.id == task.collectionID }?.collectionName
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .materialCard(20, IosColor.red.copy(alpha = 0.2f), palette.material)
            .then(modifier).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (collectionName != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, null, tint = palette.secondary, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(collectionName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = palette.secondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(task.text, style = IosFont.headline, color = palette.primary)
            task.description?.takeIf { it.isNotEmpty() }?.let { Text(it, style = IosFont.subheadline, color = palette.secondary) }
        }
        IosDivider()
        Row {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("CREATED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = palette.tertiary)
                Text(Dates.dayMonthYear(task.createdAt), style = IosFont.caption2, color = palette.primary)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("DUE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = palette.tertiary)
                task.dueDate?.let { Text(Dates.dayMonthYear(it), style = IosFont.caption2, fontWeight = FontWeight.Bold, color = IosColor.red) }
            }
        }
    }
}
