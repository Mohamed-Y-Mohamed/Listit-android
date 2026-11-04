package com.list_it.app.ui.screens.creating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.list_it.app.data.Reminders
import com.list_it.app.model.ToDoTask
import com.list_it.app.model.TodoList
import com.list_it.app.ui.Dates
import com.list_it.app.ui.Dates.day
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.AIGenerateButton
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.DateTimeField
import com.list_it.app.ui.components.DescriptionEditor
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.PlainTextField
import com.list_it.app.ui.components.ToggleRow
import com.list_it.app.ui.screens.ai.CreateTaskAISheet
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/** iOS `AddTaskView`. */
@Composable
fun ColumnScope.AddTaskSheet(list: TodoList, dismiss: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedCollectionID by remember { mutableStateOf<String?>(null) }
    var isPinned by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(Instant.now()) }
    var addDueDate by remember { mutableStateOf(false) }
    var addReminder by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf(Instant.now().plusSeconds(3600)) }
    var showAlert by remember { mutableStateOf(false) }
    var showAIView by remember { mutableStateOf(false) }

    val endOfDueDay: Instant? = if (addDueDate) Dates.endOfDay(dueDate.day()) else null

    // keepReminderWithinDueDay()
    LaunchedEffect(dueDate, addDueDate, addReminder) {
        val limit = endOfDueDay ?: return@LaunchedEffect
        if (reminderDate <= limit) return@LaunchedEffect
        val t = reminderDate.atZone(Dates.zone).toLocalTime()
        val sameTimeOnDueDay = dueDate.day().atTime(LocalTime.of(t.hour, t.minute)).atZone(Dates.zone).toInstant()
        reminderDate = minOf(maxOf(sameTimeOnDueDay, Instant.now()), limit)
    }

    fun createTask() {
        if (taskTitle.isBlank()) {
            db.setError("Error", "Your task must have a title.")
            showAlert = true
            return
        }
        if (addReminder && reminderDate <= Instant.now()) {
            db.setError("Reminder", "Please choose a reminder time in the future.")
            showAlert = true
            return
        }
        val collectionID = selectedCollectionID
            ?: db.collections.firstOrNull { it.listID == list.id && it.collectionName == "General" }?.id.orEmpty()
        val newTask = ToDoTask(
            id = UUID.randomUUID().toString().lowercase(), createdAt = Instant.now(), text = taskTitle,
            description = taskDescription, dueDate = if (addDueDate) dueDate else null, isCompleted = false,
            isDeleted = false, isPinned = isPinned, userID = "", collectionID = collectionID, listID = list.id,
        )
        scope.launch {
            if (db.saveTask(newTask)) {
                db.tasks = db.tasks + newTask
                if (addReminder) Reminders.schedule(newTask.id, newTask.text, reminderDate)
                dismiss()
            } else showAlert = true
        }
    }

    IosTopBar(
        "Add Task",
        leading = { BarButton("Dismiss", onClick = dismiss) },
        trailing = { BarButton("Create") { createTask() } },
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SheetHeader(Icons.Filled.EditNote, IosColor.orange, null)
        IosDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlainTextField(taskTitle, { taskTitle = it }, "Task title", IosFont.title3.copy(fontWeight = FontWeight.SemiBold), Modifier.fillMaxWidth())
            DescriptionEditor(taskDescription, { taskDescription = it }, "Description (optional)", minHeight = 70, dimmed = true)
            AIGenerateButton { showAIView = true }
        }
        IosDivider()
        TaskOrganisationRow(list, selectedCollectionID) { selectedCollectionID = it }
        IosDivider()
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ToggleRow(isPinned, { isPinned = it }, "Pin Task", if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin)
            ToggleRow(addDueDate, { addDueDate = it }, "Add Due Date", Icons.Filled.EventAvailable)
            AnimatedVisibility(addDueDate) {
                PickerRow("Add Due Date") {
                    DateTimeField(dueDate, { dueDate = it }, min = Dates.startOfDay(Dates.today()), includeTime = false, tint = MaterialTheme.colorScheme.primary)
                }
            }
            ToggleRow(addReminder, { addReminder = it }, "Add Reminder", Icons.Filled.NotificationsActive)
            AnimatedVisibility(addReminder) {
                PickerRow("Add Reminder") {
                    DateTimeField(reminderDate, { reminderDate = it }, min = Instant.now(), max = endOfDueDay?.let { maxOf(it, Instant.now()) }, includeTime = true, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    if (showAIView) {
        IosSheet(onDismiss = { showAIView = false }) { close ->
            CreateTaskAISheet(close) { generated ->
                taskDescription += (if (taskDescription.isEmpty()) "" else "\n\n") + generated
            }
        }
    }
}

@Composable
internal fun PickerRow(label: String, picker: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = IosFont.body, color = LocalPalette.current.primary)
        Spacer(Modifier.weight(1f))
        picker()
    }
}
