package com.list_it.app.ui.screens.defaultlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.list_it.app.model.ToDoTask
import com.list_it.app.ui.Dates
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.ContentUnavailable
import com.list_it.app.ui.components.TaskSection
import java.time.Instant

/** iOS `TodayView`. */
@Composable
fun TodayScreen(onBack: () -> Unit) =
    DueDayScreen("Today", "All Done for Today", Icons.Filled.WbSunny, onBack) { Dates.isToday(it) }

/** iOS `TomorrowView`. */
@Composable
fun TomorrowScreen(onBack: () -> Unit) =
    DueDayScreen("Tomorrow", "Clear Schedule", Icons.Filled.WbTwilight, onBack) { Dates.isTomorrow(it) }

@Composable
private fun DueDayScreen(title: String, emptyTitle: String, emptyIcon: ImageVector, onBack: () -> Unit, isDueDay: (Instant) -> Boolean) {
    val db = LocalDb.current
    var searchText by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf<ToDoTask?>(null) }

    val groupedData = db.lists.filter { it.listName.matchesListQuery(searchText) }.mapNotNull { list ->
        val tasks = db.tasks.filter { it.listID == list.id && !it.isCompleted && it.dueDate?.let(isDueDay) == true }
        if (tasks.isEmpty()) null else list to tasks
    }

    DefaultListScaffold(title, onBack, searchText, { searchText = it }, "Search Lists", selectedTask = selectedTask, onTaskDismiss = { selectedTask = null }) {
        if (groupedData.isEmpty()) {
            ContentUnavailable(emptyTitle, emptyIcon)
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                groupedData.forEach { (list, tasks) -> TaskSection(list, tasks) { selectedTask = it } }
            }
        }
    }
}
