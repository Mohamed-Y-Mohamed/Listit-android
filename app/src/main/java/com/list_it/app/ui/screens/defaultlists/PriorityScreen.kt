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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
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
import com.list_it.app.ui.components.materialCard
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import java.time.Instant

/** iOS `PriorityView`: pinned tasks grouped by list. */
@Composable
fun PriorityScreen(onBack: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    var searchText by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf<ToDoTask?>(null) }

    val allPinned = db.tasks.filter { it.isPinned && !it.isCompleted && !it.isDeleted }
    val grouped = db.lists.filter { it.listName.matchesListQuery(searchText) }.mapNotNull { list ->
        val tasks = allPinned.filter { it.listID == list.id }
        if (tasks.isEmpty()) null else list to tasks
    }

    DefaultListScaffold("Priority", onBack, searchText, { searchText = it }, "Search Lists...", selectedTask = selectedTask, onTaskDismiss = { selectedTask = null }) {
        if (grouped.isEmpty()) {
            ContentUnavailable(
                if (searchText.isEmpty()) "No Pinned Tasks" else "No Lists Match \"$searchText\"",
                if (searchText.isEmpty()) Icons.Outlined.PushPin else Icons.Filled.Search,
            )
        } else {
            Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                grouped.forEach { (list, tasks) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Circle, null, tint = colorFromHex(list.bgColorHex), modifier = Modifier.size(8.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(list.listName, style = IosFont.headline, fontWeight = FontWeight.Bold, color = palette.primary, modifier = Modifier.weight(1f))
                            CountBadge("${tasks.size}", palette.material, palette.primary)
                        }
                        tasks.forEach { task -> PriorityTaskCard(task, Modifier.clickable { selectedTask = task }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityTaskCard(task: ToDoTask, modifier: Modifier) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val collectionName = db.collections.firstOrNull { it.id == task.collectionID }?.collectionName
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().materialCard(16, Color.White.copy(alpha = 0.1f), palette.material).then(modifier).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (collectionName != null) {
                Icon(Icons.Filled.Folder, null, tint = palette.secondary, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(4.dp))
                Text(collectionName.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = palette.secondary)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.PushPin, null, tint = IosColor.orange, modifier = Modifier.size(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.text, style = IosFont.body, fontWeight = FontWeight.Bold, color = palette.primary)
            task.description?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = IosFont.subheadline, color = palette.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Created ${Dates.monthDay(task.createdAt)}", fontSize = 10.sp, color = palette.tertiary)
            Spacer(Modifier.weight(1f))
            task.dueDate?.let { due ->
                val color = if (due < Instant.now()) IosColor.red else palette.primary
                Icon(Icons.Filled.Schedule, null, tint = color, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(3.dp))
                Text(Dates.monthDay(due), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}
