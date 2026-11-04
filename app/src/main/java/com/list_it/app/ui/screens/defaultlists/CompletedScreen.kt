package com.list_it.app.ui.screens.defaultlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.list_it.app.data.ReminderStore
import com.list_it.app.model.ToDoTask
import com.list_it.app.ui.Dates
import com.list_it.app.ui.Dates.day
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.ContentUnavailable
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.MenuDivider
import com.list_it.app.ui.components.MenuItem
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import kotlinx.coroutines.launch
import java.time.Instant

/** iOS `CompletedView`: a timeline of completed tasks, grouped by day, with undo for today. */
@Composable
fun CompletedScreen(onBack: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    var selectedListID by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    var completed = db.tasks.filter { it.isCompleted }
    selectedListID?.let { id -> completed = completed.filter { it.listID == id } }
    val query = searchText.trim()
    val filtered = if (query.isEmpty()) completed else {
        val byId = db.collections.associateBy { it.id }
        completed.filter { byId[it.collectionID]?.collectionName?.contains(query, ignoreCase = true) == true }
    }
    val grouped = filtered.groupBy { (it.dateCompleted ?: it.createdAt).day() }.toList().sortedByDescending { it.first }

    val active = db.tasks.filter { !it.isDeleted }
    val completionPercentage = if (active.isEmpty()) 0.0 else active.count { it.isCompleted }.toDouble() / active.size * 100
    val selectedListTitle = selectedListID?.let { id -> db.lists.firstOrNull { it.id == id }?.listName } ?: "All Lists"

    fun uncomplete(task: ToDoTask) = scope.launch {
        if (db.uncompleteTask(task)) {
            if (ReminderStore.date(task.id)?.let { it > Instant.now() } == true) ReminderStore.remove(task.id)
        } else showAlert = true
    }

    DefaultListScaffold(
        "Completed Tasks", onBack, searchText, { searchText = it }, "Search Collections",
        trailing = {
            Text("${completionPercentage.toInt()}% Done", style = IosFont.caption2, fontWeight = FontWeight.Bold, color = IosColor.green, modifier = Modifier.padding(end = 16.dp))
        },
        selectedTask = null, onTaskDismiss = {},
    ) {
        // Lists filter menu
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
            Row(
                Modifier.clip(CircleShape).background(palette.secondaryBackground).clickable { menuOpen = true }.padding(vertical = 8.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedListTitle, style = IosFont.subheadline, fontWeight = FontWeight.Medium, maxLines = 1, color = palette.primary)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = palette.secondary, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                MenuItem("All Lists", checked = selectedListID == null) { menuOpen = false; selectedListID = null }
                MenuDivider()
                db.lists.forEach { list ->
                    MenuItem(list.listName, checked = selectedListID == list.id) { menuOpen = false; selectedListID = list.id }
                }
            }
        }
        IosDivider(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp))

        if (grouped.isEmpty()) {
            ContentUnavailable(
                if (searchText.isEmpty()) "No Completed Tasks" else "No Results for \"$searchText\"",
                if (searchText.isEmpty()) Icons.Filled.Checklist else Icons.Filled.Search,
            )
        } else {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                grouped.forEach { (day, tasks) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
                            val start = Dates.startOfDay(day)
                            val title = when {
                                Dates.isToday(start) -> "Today"
                                Dates.isYesterday(start) -> "Yesterday"
                                else -> Dates.weekdayDayMonth(start)
                            }
                            Text(title, style = IosFont.subheadline, fontWeight = FontWeight.SemiBold, color = palette.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("${tasks.size} completed", style = IosFont.footnote, color = palette.tertiary)
                        }
                        Column {
                            tasks.forEachIndexed { index, task ->
                                TimelineRow(task, isLast = index == tasks.size - 1) { uncomplete(task) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
}

@Composable
private fun TimelineRow(task: ToDoTask, isLast: Boolean, onUndo: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val parentList = db.lists.firstOrNull { it.id == task.listID }
    val parentCollection = db.collections.firstOrNull { it.id == task.collectionID }
    val canUndo = task.dateCompleted?.let { Dates.isToday(it) } == true

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(12.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.padding(top = 4.dp).size(12.dp).clip(CircleShape).background(palette.systemBackground)
                    .border(2.dp, parentList?.let { colorFromHex(it.bgColorHex) } ?: IosColor.green, CircleShape),
            )
            if (!isLast) {
                Box(Modifier.padding(vertical = 2.dp).width(1.dp).weight(1f).heightIn(min = 28.dp).background(palette.separator))
            }
        }
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(task.text, style = IosFont.body, fontWeight = FontWeight.Medium, color = palette.primary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                task.dateCompleted?.let { Text(Dates.time(it), style = IosFont.subheadline, color = palette.tertiary) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(parentList?.listName ?: "General", style = IosFont.footnote, color = palette.secondary)
                parentCollection?.let {
                    Text("/", style = IosFont.footnote, color = palette.tertiary)
                    Text(it.collectionName, style = IosFont.footnote, color = palette.tertiary)
                }
            }
        }
        if (canUndo) {
            Box(
                Modifier.clip(RoundedCornerShape(6.dp)).background(palette.fill).clickable(onClick = onUndo).padding(6.dp),
            ) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = palette.tertiary, modifier = Modifier.size(14.dp)) }
        }
    }
}
