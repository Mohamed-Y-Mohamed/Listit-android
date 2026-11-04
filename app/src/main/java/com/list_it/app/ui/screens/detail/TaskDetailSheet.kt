package com.list_it.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.list_it.app.ui.components.DateChip
import com.list_it.app.ui.components.InfoRow
import com.list_it.app.ui.components.SectionLabel
import com.list_it.app.ui.components.ViewColor
import com.list_it.app.ui.components.ViewFooterButton
import com.list_it.app.ui.theme.WebColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.data.ReminderStore
import com.list_it.app.data.Reminders
import com.list_it.app.model.AIFeatureMode
import com.list_it.app.model.AlertMode
import com.list_it.app.model.ToDoTask
import com.list_it.app.ui.Dates
import com.list_it.app.ui.Dates.day
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.AIMenuView
import com.list_it.app.ui.components.AlertButton
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.DateTimeField
import com.list_it.app.ui.components.DescriptionEditor
import com.list_it.app.ui.components.DescriptionModeSwitcher
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.MarkdownText
import com.list_it.app.ui.components.PlainTextField
import com.list_it.app.ui.components.TintedButton
import com.list_it.app.ui.components.rememberAddToCalendarAction
import com.list_it.app.ui.screens.ai.DescriptionTransformSheet
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime

/** iOS `TaskDetailView`: edit, schedule, pin, complete or delete a task. */
@Composable
fun ColumnScope.TaskDetailSheet(task: ToDoTask, dismiss: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val addToCalendar = rememberAddToCalendarAction()

    var editedText by remember { mutableStateOf(task.text) }
    var editedDescription by remember { mutableStateOf(task.description.orEmpty()) }
    var editedDueDate by remember { mutableStateOf(task.dueDate ?: Instant.now()) }
    var hasDueDate by remember { mutableStateOf(task.dueDate != null) }
    var editedReminder by remember { mutableStateOf(Instant.now().plusSeconds(3600)) }
    var hasReminder by remember { mutableStateOf(false) }
    var hadPendingReminder by remember { mutableStateOf(false) }
    var lastReminderDate by remember { mutableStateOf<Instant?>(null) }
    var isPinned by remember { mutableStateOf(task.isPinned) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf(AlertMode.ERROR) }
    var statusMessage by remember { mutableStateOf("") }
    var isMarkdownPreview by remember { mutableStateOf(false) }
    var activeAIMode by remember { mutableStateOf<AIFeatureMode?>(null) }
    // The sheet opens to read the task; Edit swaps in the form (web TasksDetails).
    var isEditing by remember { mutableStateOf(false) }
    val live = db.tasks.firstOrNull { it.id == task.id } ?: task

    val collection = db.collections.firstOrNull { it.id == task.collectionID }
    val accent = collection?.let { colorFromHex(it.bgColorHex) } ?: IosColor.blue
    val now = Instant.now()
    val dueDateLowerBound = minOf(task.dueDate ?: now, now)
    val endOfDueDay = Dates.endOfDay(editedDueDate.day())
    val isOverdue = hasDueDate && endOfDueDay <= now
    val reminderLimit = if (hasDueDate && endOfDueDay > now) endOfDueDay else null

    // loadExistingReminder()
    LaunchedEffect(Unit) {
        val pending = Reminders.pendingDate(task.id)
        if (pending != null) {
            editedReminder = pending
            hasReminder = true
            hadPendingReminder = true
            return@LaunchedEffect
        }
        ReminderStore.date(task.id)?.takeIf { it <= Instant.now() }?.let { lastReminderDate = it }
    }

    // keepReminderWithinDueDay()
    LaunchedEffect(editedDueDate, hasDueDate, hasReminder) {
        val limit = reminderLimit ?: return@LaunchedEffect
        if (!hasReminder || editedReminder <= limit) return@LaunchedEffect
        val t = editedReminder.atZone(Dates.zone).toLocalTime()
        val sameTime = editedDueDate.day().atTime(LocalTime.of(t.hour, t.minute)).atZone(Dates.zone).toInstant()
        editedReminder = minOf(maxOf(sameTime, Instant.now()), limit)
    }

    fun saveChanges() {
        if (editedText.isBlank()) {
            db.setError("Error", "Your Task must have a title.")
            alertMode = AlertMode.ERROR; showAlert = true
            return
        }
        if (hasReminder && editedReminder <= Instant.now()) {
            db.setError("Reminder", "Please choose a reminder time in the future.")
            alertMode = AlertMode.ERROR; showAlert = true
            return
        }
        val finalDueDate = if (hasDueDate) editedDueDate else null
        val finalReminder = if (hasReminder) editedReminder else null
        scope.launch {
            if (db.updateTask(task, editedText, editedDescription, finalDueDate, isPinned)) {
                if (finalReminder != null) {
                    val error = Reminders.schedule(task.id, editedText, finalReminder)
                    statusMessage = error?.let { "Error: $it" } ?: "Task reminder scheduled for ${Dates.abbreviatedDateTime(finalReminder)}"
                } else if (hadPendingReminder) {
                    Reminders.cancel(task.id)
                    ReminderStore.remove(task.id)
                }
                hadPendingReminder = finalReminder != null
                isEditing = false
            } else {
                alertMode = AlertMode.ERROR; showAlert = true
            }
        }
    }

    fun completeTask() = scope.launch {
        if (db.completeTask(task)) {
            Reminders.cancel(task.id)
            dismiss()
        } else showAlert = true
    }

    fun cancelEdit() {
        editedText = live.text
        editedDescription = live.description.orEmpty()
        editedDueDate = live.dueDate ?: Instant.now()
        hasDueDate = live.dueDate != null
        isPinned = live.isPinned
        isEditing = false
    }

    /** Pin and complete apply the moment they are tapped, in view mode. */
    fun togglePin() = scope.launch {
        if (db.updatePinForTask(live, !live.isPinned)) isPinned = !live.isPinned else { alertMode = AlertMode.ERROR; showAlert = true }
    }

    fun toggleCompleted() = scope.launch {
        val ok = if (live.isCompleted) db.uncompleteTask(live) else db.completeTask(live).also { if (it) Reminders.cancel(task.id) }
        if (!ok) { alertMode = AlertMode.ERROR; showAlert = true }
    }

    fun performDelete() = scope.launch {
        if (db.deleteTask(task)) {
            Reminders.cancel(task.id)
            ReminderStore.remove(task.id)
            dismiss()
        } else {
            alertMode = AlertMode.ERROR; showAlert = true
        }
    }

    if (!isEditing) {
        TaskView(
            task = live, accent = accent, reminder = if (hasReminder) editedReminder else null,
            onDismiss = dismiss, onEdit = { isEditing = true }, onAddDueDate = { isEditing = true; hasDueDate = true },
            onTogglePin = { togglePin() }, onToggleComplete = { toggleCompleted() },
            onDelete = { alertMode = AlertMode.DELETE; showAlert = true },
        )
    } else Column(Modifier.fillMaxSize().background(accent.copy(alpha = 0.08f))) {
        IosTopBar(
            "Edit Task",
            leading = { BarButton("Cancel") { cancelEdit() } },
            trailing = { BarButton("Save", bold = true) { saveChanges() } },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PlainTextField(editedText, { editedText = it }, "Task title", IosFont.title3.copy(fontWeight = FontWeight.SemiBold), Modifier.fillMaxWidth())
            IosDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DescriptionModeSwitcher(isMarkdownPreview, { isMarkdownPreview = it }, accent)
                    Spacer(Modifier.weight(1f))
                    AIMenuView(editedDescription) { activeAIMode = it }
                }
                if (isMarkdownPreview) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(top = 8.dp, start = 5.dp)) {
                        if (editedDescription.isEmpty()) Text("Nothing to preview", fontSize = 13.sp, color = palette.secondary)
                        else MarkdownText(editedDescription, fontSize = 13)
                    }
                } else {
                    DescriptionEditor(editedDescription, { editedDescription = it }, "Add a description", minHeight = 70)
                }
            }
            IosDivider()

            // Schedule section
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    if (hasDueDate) {
                        DateTimeField(editedDueDate, { editedDueDate = it }, min = Dates.startOfDay(dueDateLowerBound.day()), includeTime = false, tint = accent)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { hasDueDate = false }) { Icon(Icons.Filled.Cancel, "Remove due date", tint = palette.secondary) }
                    } else {
                        Text("No due date", color = palette.secondary, style = IosFont.body)
                        Spacer(Modifier.weight(1f))
                        BarButton("Add Due Date", color = accent) { hasDueDate = true }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    if (hasReminder) {
                        DateTimeField(editedReminder, { editedReminder = it }, min = Instant.now(), max = reminderLimit, includeTime = true, tint = accent)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { hasReminder = false }) { Icon(Icons.Filled.Cancel, "Remove reminder", tint = palette.secondary) }
                    } else {
                        val last = lastReminderDate
                        Text(
                            if (last != null) "Reminded at: ${Dates.abbreviatedDateTime(last)}" else "No reminder",
                            style = if (last != null) IosFont.subheadline else IosFont.body, color = palette.secondary,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.alpha(if (isOverdue) 0.4f else 1f)) {
                            BarButton(if (last == null) "Add Reminder" else "Remind Again", color = accent, enabled = !isOverdue) {
                                editedReminder = Instant.now().plusSeconds(3600)
                                hasReminder = true
                            }
                        }
                    }
                }
                if (isOverdue && !hasReminder) {
                    Text(
                        "This Task is overdue, so a reminder can't be set. Change the due date to today or later to set one.",
                        style = IosFont.caption, color = palette.secondary,
                    )
                }
                if (statusMessage.isNotEmpty()) Text(statusMessage, style = IosFont.caption, color = palette.secondary)
            }
            IosDivider()

            // Actions grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TintedButton(if (isPinned) "Unpin" else "Pin", if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, IosColor.orange, Modifier.weight(1f), minHeight = 40.dp) {
                        isPinned = !isPinned
                    }
                    TintedButton("Delete", Icons.Filled.Delete, IosColor.red, Modifier.weight(1f), minHeight = 40.dp) {
                        alertMode = AlertMode.DELETE; showAlert = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TintedButton("Add to Calendar", Icons.Filled.EditCalendar, IosColor.blue, Modifier.weight(1f), minHeight = 40.dp) {
                        addToCalendar(editedText, editedDescription)
                    }
                    TintedButton(
                        "Complete", Icons.Filled.CheckCircle, accent, Modifier.weight(1f), prominent = true, minHeight = 40.dp,
                        textStyle = IosFont.subheadline.copy(fontWeight = FontWeight.SemiBold, color = Color.White),
                    ) { completeTask() }
                }
            }
        }
    }

    if (showAlert) {
        if (alertMode == AlertMode.DELETE) {
            IosAlert(
                "Delete Task?", "This action cannot be undone.", onDismiss = { showAlert = false },
                buttons = listOf(AlertButton("Cancel", cancel = true), AlertButton("Delete", destructive = true) { performDelete() }),
            )
        } else IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    }
    activeAIMode?.let { mode ->
        IosSheet(onDismiss = { activeAIMode = null }) { close ->
            DescriptionTransformSheet(editedDescription, mode, close) { editedDescription = it }
        }
    }
}

/** Read-only task view (web TasksDetails view mode): Dismiss | Edit, sections, then Pin, Delete, Complete. */
@Composable
private fun ColumnScope.TaskView(
    task: ToDoTask,
    accent: Color,
    reminder: Instant?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddDueDate: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val muted = if (palette.isDark) WebColor.gray400 else WebColor.gray500
    val due = task.dueDate
    val overdue = due != null && !task.isCompleted && Dates.endOfDay(due.day()) < Instant.now()
    Column(Modifier.fillMaxSize().background(accent.copy(alpha = 0.08f))) {
        IosTopBar(
            "Task Details",
            leading = { BarButton("Dismiss", onClick = onDismiss) },
            trailing = { BarButton("Edit", bold = true, onClick = onEdit) },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(task.text.ifEmpty { "Untitled task" }, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, color = palette.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (task.isCompleted) ViewColor.success else ViewColor.info))
                    Spacer(Modifier.width(6.dp))
                    Text(if (task.isCompleted) "Completed" else "Incomplete", fontSize = 13.sp, color = if (task.isCompleted) ViewColor.success else muted)
                    task.dateCompleted?.takeIf { task.isCompleted }?.let { Text(" · ${Dates.abbreviatedDateTime(it)}", fontSize = 13.sp, color = muted) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Due date")
                if (due != null) DateChip(Dates.dayMonthYear(due), overdue)
                else Row(Modifier.clickable(onClick = onAddDueDate).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EditCalendar, null, tint = ViewColor.info, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add due date", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ViewColor.info)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Reminder")
                Text(reminder?.let { Dates.abbreviatedDateTime(it) } ?: "No reminder", fontSize = 14.sp, color = if (reminder != null) palette.primary else muted)
            }
            task.description?.takeIf { it.isNotBlank() }?.let { description ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Description")
                    MarkdownText(description, fontSize = 14)
                }
            }
            Column {
                IosDivider()
                Spacer(Modifier.height(12.dp))
                InfoRow("Collection", db.collections.firstOrNull { it.id == task.collectionID }?.collectionName)
                InfoRow("List", db.lists.firstOrNull { it.id == task.listID }?.listName)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewFooterButton(if (task.isPinned) "Unpin" else "Pin", if (task.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, ViewColor.warning, onClick = onTogglePin)
            ViewFooterButton("Delete", Icons.Filled.Delete, ViewColor.danger, onClick = onDelete)
            Spacer(Modifier.weight(1f))
            ViewFooterButton(if (task.isCompleted) "Completed" else "Complete", Icons.Filled.CheckCircle, accent, prominent = true, onClick = onToggleComplete)
        }
    }
}
