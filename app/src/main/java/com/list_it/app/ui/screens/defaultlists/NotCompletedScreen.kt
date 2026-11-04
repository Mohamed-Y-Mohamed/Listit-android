package com.list_it.app.ui.screens.defaultlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.EventAvailable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.ToDoTask
import com.list_it.app.ui.Dates
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.ContentUnavailable
import com.list_it.app.ui.components.CountBadge
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.materialCard
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import java.time.Instant

/** iOS `NotCompletedView`: every pending task grouped by list. */
@Composable
fun NotCompletedScreen(onBack: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    var searchText by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf<ToDoTask?>(null) }

    val grouped = db.lists.filter { searchText.isEmpty() || it.listName.contains(searchText, ignoreCase = true) }.mapNotNull { list ->
        val tasks = db.tasks.filter { it.listID == list.id && !it.isCompleted && !it.isDeleted }
        if (tasks.isEmpty()) null else list to tasks
    }

    DefaultListScaffold("Pending Tasks", onBack, searchText, { searchText = it }, "Search Lists...", selectedTask = selectedTask, onTaskDismiss = { selectedTask = null }) {
        if (grouped.isEmpty()) {
            ContentUnavailable(
                if (searchText.isEmpty()) "All Caught Up!" else "No Lists Found",
                if (searchText.isEmpty()) Icons.Filled.Verified else Icons.Filled.Search,
                description = if (searchText.isEmpty()) "You have no pending tasks." else "Try a different list name.",
            )
        } else {
            Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                grouped.forEach { (list, tasks) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(list.listName, style = IosFont.title3, fontWeight = FontWeight.Bold, color = palette.primary)
                            Spacer(Modifier.width(8.dp))
                            CountBadge("${tasks.size}", colorFromHex(list.bgColorHex), style = IosFont.caption.copy(fontWeight = FontWeight.Bold))
                        }
                        tasks.forEach { task -> PendingTaskCard(task, Modifier.clickable { selectedTask = task }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTaskCard(task: ToDoTask, modifier: Modifier) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val collectionName = db.collections.firstOrNull { it.id == task.collectionID }?.collectionName
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().materialCard(16, Color.White.copy(alpha = 0.1f), palette.material).then(modifier).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (collectionName != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, null, tint = palette.secondary, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(6.dp))
                Text(collectionName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.secondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.text, style = IosFont.headline, color = palette.primary)
            task.description?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = IosFont.subheadline, color = palette.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        IosDivider(Modifier.padding(vertical = 0.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.EventAvailable, null, tint = palette.secondary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(Dates.dayMonthYear(task.createdAt), style = IosFont.caption, color = palette.secondary)
            Spacer(Modifier.weight(1f))
            task.dueDate?.let { due ->
                val color = if (due < Instant.now()) IosColor.red else IosColor.blue
                Icon(Icons.Filled.Schedule, null, tint = color, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(Dates.monthDay(due), style = IosFont.caption, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}
