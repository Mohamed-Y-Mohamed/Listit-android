package com.list_it.app.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.list_it.app.ui.components.DateChip
import com.list_it.app.ui.components.SectionLabel
import com.list_it.app.ui.components.ViewColor
import com.list_it.app.ui.components.ViewFooterButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.list_it.app.ui.components.BarIconButton
import com.list_it.app.ui.components.DateTimeField
import com.list_it.app.ui.components.DescriptionEditor
import com.list_it.app.ui.components.GuardSheetDismiss
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.MarkdownText
import com.list_it.app.ui.components.PlainTextField
import com.list_it.app.ui.components.rememberAddToCalendarAction
import com.list_it.app.ui.screens.ai.DescriptionTransformSheet
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime

/**
 * The page that opens when you tap a task.
 *
 * It first shows the task read-only. Tapping "Edit" switches to a form where you can change the
 * title, description, due date and reminder. Both modes share the same buttons at the bottom.
 */
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
    var originalReminder by remember { mutableStateOf<Instant?>(null) }
    var lastReminderDate by remember { mutableStateOf<Instant?>(null) }
    var isPinned by remember { mutableStateOf(task.isPinned) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf(AlertMode.ERROR) }
    var statusMessage by remember { mutableStateOf("") }
    var activeAIMode by remember { mutableStateOf<AIFeatureMode?>(null) }
    // false = reading the task, true = editing it.
    var isEditing by remember { mutableStateOf(false) }
    val live = db.tasks.firstOrNull { it.id == task.id } ?: task

    val collection = db.collections.firstOrNull { it.id == task.collectionID }
    val accent = collection?.let { colorFromHex(it.bgColorHex) } ?: IosColor.blue
    val now = Instant.now()
    val dueDateLowerBound = minOf(task.dueDate ?: now, now)
    val endOfDueDay = Dates.endOfDay(editedDueDate.day())
    val isOverdue = hasDueDate && endOfDueDay <= now
    val reminderLimit = if (hasDueDate && endOfDueDay > now) endOfDueDay else null

    // When the page opens, check whether this task already has a reminder set.
    LaunchedEffect(Unit) {
        val pending = Reminders.pendingDate(task.id)
        if (pending != null) {
            editedReminder = pending
            hasReminder = true
            hadPendingReminder = true
            originalReminder = pending
            return@LaunchedEffect
        }
        ReminderStore.date(task.id)?.takeIf { it <= Instant.now() }?.let { lastReminderDate = it }
    }

    // A drag or scrim dismiss confirms first while the form holds unsaved edits.
    GuardSheetDismiss(
        isEditing && (
            editedText != live.text ||
                editedDescription != live.description.orEmpty() ||
                isPinned != live.isPinned ||
                hasDueDate != (live.dueDate != null) ||
                (hasDueDate && editedDueDate != live.dueDate) ||
                (if (hasReminder) editedReminder != originalReminder else originalReminder != null)
            ),
    )

    // If the due date moves earlier, pull the reminder back so it still goes off before the task is due.
    LaunchedEffect(editedDueDate, hasDueDate, hasReminder) {
        val limit = reminderLimit ?: return@LaunchedEffect
        if (!hasReminder || editedReminder <= limit) return@LaunchedEffect
        val t = editedReminder.atZone(Dates.zone).toLocalTime()
        val sameTime = editedDueDate.day().atTime(LocalTime.of(t.hour, t.minute)).atZone(Dates.zone).toInstant()
        editedReminder = minOf(maxOf(sameTime, Instant.now()), limit)
    }

    /** Validates and writes the form. Returns false when it could not be saved. */
    suspend fun persistChanges(skipReminder: Boolean = false): Boolean {
        if (editedText.isBlank()) {
            db.setError("Error", "Your Task must have a title.")
            alertMode = AlertMode.ERROR; showAlert = true
            return false
        }
        if (!skipReminder && hasReminder && editedReminder <= Instant.now()) {
            db.setError("Reminder", "Please choose a reminder time in the future.")
            alertMode = AlertMode.ERROR; showAlert = true
            return false
        }
        val finalDueDate = if (hasDueDate) editedDueDate else null
        val finalReminder = if (skipReminder) null else if (hasReminder) editedReminder else null
        if (!db.updateTask(task, editedText, editedDescription, finalDueDate, isPinned)) {
            alertMode = AlertMode.ERROR; showAlert = true
            return false
        }
        if (finalReminder != null) {
            val error = Reminders.schedule(task.id, editedText, finalReminder)
            statusMessage = error?.let { "Error: $it" } ?: "Task reminder scheduled for ${Dates.abbreviatedDateTime(finalReminder)}"
        } else if (hadPendingReminder) {
            Reminders.cancel(task.id)
        }
        hadPendingReminder = finalReminder != null
        originalReminder = finalReminder
        return true
    }

    /** Checks the form, saves the task, then sets up (or cancels) its reminder. */
    fun saveChanges() = scope.launch {
        if (persistChanges()) isEditing = false
    }

    /** Complete from the edit form: keep the edits, then complete and close the page. Completing clears any reminder. */
    fun saveAndComplete() = scope.launch {
        if (!persistChanges(skipReminder = true)) return@launch
        if (db.completeTask(task)) {
            Reminders.cancel(task.id)
            dismiss()
        } else {
            alertMode = AlertMode.ERROR; showAlert = true
        }
    }

    /** Throws away unsaved edits and goes back to reading the task. */
    fun cancelEdit() {
        editedText = live.text
        editedDescription = live.description.orEmpty()
        editedDueDate = live.dueDate ?: Instant.now()
        hasDueDate = live.dueDate != null
        isPinned = live.isPinned
        isEditing = false
    }

    /** While reading the task, Pin saves straight away (no need to tap Save). */
    fun togglePin() = scope.launch {
        if (db.updatePinForTask(live, !live.isPinned)) isPinned = !live.isPinned else { alertMode = AlertMode.ERROR; showAlert = true }
    }

    /** While reading the task, Complete saves straight away; tapping it again un-completes the task. */
    fun toggleCompleted() = scope.launch {
        val ok = if (live.isCompleted) db.uncompleteTask(live) else db.completeTask(live).also { if (it) Reminders.cancel(task.id) }
        if (!ok) { alertMode = AlertMode.ERROR; showAlert = true }
    }

    /** Deletes the task for good, clears its reminder and closes the page. */
    fun performDelete() = scope.launch {
        if (db.deleteTask(task)) {
            Reminders.cancel(task.id)
            dismiss()
        } else {
            alertMode = AlertMode.ERROR; showAlert = true
        }
    }

    if (!isEditing) {
        TaskView(
            task = live, accent = accent, reminder = if (hasReminder) editedReminder else null,
            onDismiss = dismiss, onEdit = { isEditing = true },
            onTogglePin = { togglePin() }, onToggleComplete = { toggleCompleted() },
            onAddToCalendar = { addToCalendar(live.text, live.description.orEmpty()) },
            onDelete = { alertMode = AlertMode.DELETE; showAlert = true },
        )
    } else Column(Modifier.fillMaxSize().background(accent.copy(alpha = 0.08f))) {
        IosTopBar(
            "Edit Task",
            leading = { BarButton("Cancel") { cancelEdit() } },
            trailing = { BarButton("Save", bold = true) { saveChanges() } },
        )
        // The title stays at the top; only the description box scrolls when the text is long.
        Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PlainTextField(editedText, { editedText = it }, "Task title", IosFont.title3.copy(fontWeight = FontWeight.SemiBold), Modifier.fillMaxWidth())
            IosDivider()
            DescriptionEditor(
                editedDescription, { editedDescription = it }, "Add a description", minHeight = 100,
                modifier = Modifier.weight(1f), fillHeight = true,
            ) {
                AIMenuView(editedDescription) { activeAIMode = it }
            }
        }
        // Due date and reminder stay pinned above the buttons, so they're on screen however long the description is.
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            IosDivider() // line above the due date and reminder
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
            IosDivider(Modifier.padding(top = 12.dp))
        }
        TaskButtons(
            isPinned = isPinned,
            completeLabel = "Complete",
            onTogglePin = { isPinned = !isPinned },
            onAddToCalendar = { addToCalendar(editedText, editedDescription) },
            onDelete = { alertMode = AlertMode.DELETE; showAlert = true },
            onComplete = { saveAndComplete() },
        )
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

/**
 * The read-only version of the task page. The title, status and description scroll;
 * the due date, reminder and buttons (see [TaskButtons]) stay fixed at the bottom.
 */
@Composable
private fun ColumnScope.TaskView(
    task: ToDoTask,
    accent: Color,
    reminder: Instant?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleComplete: () -> Unit,
    onAddToCalendar: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    val muted = palette.secondary
    val due = task.dueDate
    val overdue = due != null && !task.isCompleted && Dates.endOfDay(due.day()) < Instant.now()
    Column(Modifier.fillMaxSize().background(accent.copy(alpha = 0.08f))) {
        IosTopBar(
            "Task Details",
            leading = { BarIconButton(Icons.Filled.Close, "Dismiss", onClick = onDismiss) },
            trailing = { BarButton("Edit", bold = true, onClick = onEdit) },
        )
        // The title stays at the top; only the description scrolls when it's long.
        Column(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(task.text.ifEmpty { "Untitled task" }, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, color = palette.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (task.isCompleted) ViewColor.success else ViewColor.info))
                    Spacer(Modifier.width(6.dp))
                    Text(if (task.isCompleted) "Completed" else "Incomplete", fontSize = 13.sp, color = if (task.isCompleted) ViewColor.success else muted)
                    task.dateCompleted?.takeIf { task.isCompleted }?.let { Text(" · ${Dates.abbreviatedDateTime(it)}", fontSize = 13.sp, color = muted) }
                }
            }
            task.description?.takeIf { it.isNotBlank() }?.let { description ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Description")
                    Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
                        MarkdownText(description, fontSize = 14)
                    }
                }
            }
        }
        // Due date and reminder side by side, pinned above the buttons so you never have to scroll for them.
        // Their titles are black (white in dark mode) and the values next to them dark grey.
        val valueGrey = palette.secondary
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            IosDivider() // line above the due date and reminder
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Due date", color = palette.primary)
                    if (due != null) DateChip(Dates.dayMonthYear(due), overdue)
                    else Text("No due date", fontSize = 14.sp, color = valueGrey, modifier = Modifier.padding(vertical = 6.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Reminder", color = palette.primary)
                    Text(
                        reminder?.let { Dates.abbreviatedDateTime(it) } ?: "No reminder", fontSize = 14.sp,
                        color = valueGrey, modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
            IosDivider(Modifier.padding(top = 12.dp))
        }
        TaskButtons(
            isPinned = task.isPinned,
            completeLabel = if (task.isCompleted) "Completed" else "Complete",
            onTogglePin = onTogglePin,
            onAddToCalendar = onAddToCalendar,
            onDelete = onDelete,
            onComplete = onToggleComplete,
        )
    }
}

/**
 * The buttons at the bottom of the task page (and its edit screen):
 * Pin, Calendar and Delete side by side, with a full-width green Complete button underneath.
 */
@Composable
private fun TaskButtons(
    isPinned: Boolean,
    completeLabel: String,
    onTogglePin: () -> Unit,
    onAddToCalendar: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
) {
    // Icons and text are black in light mode and white in dark mode (like the website); the buttons keep their colours.
    val label = if (LocalPalette.current.isDark) Color.White else Color.Black
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewFooterButton(
                if (isPinned) "Unpin" else "Pin", if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                ViewColor.warning, Modifier.weight(1f), contentColor = label, pill = true, onClick = onTogglePin,
            )
            ViewFooterButton("Calendar", Icons.Filled.EditCalendar, IosColor.blue, Modifier.weight(1f), contentColor = label, pill = true, onClick = onAddToCalendar)
            ViewFooterButton("Delete", Icons.Filled.Delete, IosColor.red, Modifier.weight(1f), contentColor = label, pill = true, onClick = onDelete)
        }
        ViewFooterButton(
            completeLabel, Icons.Filled.CheckCircle, IosColor.green, Modifier.fillMaxWidth(), prominent = true,
            contentColor = label, pill = true, minHeight = 46.dp, onClick = onComplete,
        )
    }
}
