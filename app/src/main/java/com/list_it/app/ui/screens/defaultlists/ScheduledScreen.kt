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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.ToDoTask
import com.list_it.app.ui.Dates
import com.list_it.app.ui.Dates.day
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.ContentUnavailable
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/** iOS `ScheduledView`: month calendar with the tasks due on the selected day. */
@Composable
fun ScheduledScreen(onBack: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val today = Dates.today()
    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTask by remember { mutableStateOf<ToDoTask?>(null) }

    val dueTasks = db.tasks.filter { it.dueDate != null && !it.isCompleted && !it.isDeleted }
    val daysWithTasks = dueTasks.mapNotNull { it.dueDate?.day() }.toSet()
    val tasksForSelectedDate = dueTasks.filter { it.dueDate!!.day() == selectedDate }.sortedBy { it.dueDate }
    val isShowingCurrentMonth = displayedMonth == YearMonth.from(today)
    val isViewingToday = isShowingCurrentMonth && selectedDate == today

    fun changeMonth(by: Long) {
        val newMonth = displayedMonth.plusMonths(by)
        displayedMonth = newMonth
        selectedDate = if (newMonth == YearMonth.from(today)) today else newMonth.atDay(1)
    }

    DefaultListScaffold(
        "Scheduled", onBack, searchText = null,
        trailing = {
            if (!isViewingToday) BarButton("Today", bold = true) { displayedMonth = YearMonth.from(today); selectedDate = today }
        },
        selectedTask = selectedTask, onTaskDismiss = { selectedTask = null }, scrollable = false,
    ) {
        // Calendar
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Dates.monthYear(Dates.startOfDay(displayedMonth.atDay(1))), style = IosFont.headline, color = palette.primary, modifier = Modifier.weight(1f))
                IconButton(onClick = { changeMonth(-1) }, enabled = !isShowingCurrentMonth, modifier = Modifier.alpha(if (isShowingCurrentMonth) 0f else 1f)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", tint = IosColor.orange)
                }
                IconButton(onClick = { changeMonth(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", tint = IosColor.orange)
                }
            }
            val locale = LocalConfiguration.current.locales[0]
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            Row {
                (0 until 7).forEach { i ->
                    val dow = firstDayOfWeek.plus(i.toLong())
                    Text(
                        dow.getDisplayName(TextStyle.NARROW, locale), style = IosFont.caption2, color = palette.secondary,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    )
                }
            }
            val offset = (displayedMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            val cells: List<LocalDate?> = List(offset) { null } + (1..displayedMonth.lengthOfMonth()).map { displayedMonth.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.padding(vertical = 1.dp)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(40.dp)) {
                            if (date != null) DayCell(date, date == selectedDate, date == today, date < today, date in daysWithTasks) { selectedDate = date }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        IosDivider()

        // Tasks for the selected day
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Dates.weekdayDayMonth(Dates.startOfDay(selectedDate)), style = IosFont.subheadline, fontWeight = FontWeight.SemiBold, color = palette.primary, modifier = Modifier.weight(1f))
            if (tasksForSelectedDate.isNotEmpty()) {
                Text("${tasksForSelectedDate.size} ${if (tasksForSelectedDate.size == 1) "Task" else "Tasks"}", style = IosFont.caption, color = palette.secondary)
            }
        }
        if (tasksForSelectedDate.isEmpty()) {
            ContentUnavailable("Nothing Scheduled", Icons.Outlined.CalendarMonth, description = "No Tasks are due on this day.")
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tasksForSelectedDate.forEach { task -> ScheduledTaskRow(task, Modifier.clickable { selectedTask = task }) }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isSelected: Boolean, isToday: Boolean, isPast: Boolean, hasTasks: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(if (isSelected) IosColor.orange else Color.Transparent), contentAlignment = Alignment.Center) {
            Text(
                "${date.dayOfMonth}", style = IosFont.subheadline,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> IosColor.orange
                    isPast -> palette.secondary
                    else -> palette.primary
                },
            )
        }
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (!hasTasks) Color.Transparent else if (isSelected) IosColor.orange else palette.secondary))
    }
}

@Composable
private fun ScheduledTaskRow(task: ToDoTask, modifier: Modifier) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val list = db.lists.firstOrNull { it.id == task.listID }
    val collection = db.collections.firstOrNull { it.id == task.collectionID }
    val accent = list?.let { colorFromHex(it.bgColorHex) } ?: palette.secondary
    val isOverdue = task.dueDate?.let { it < Instant.now() } == true
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape)
            .background(if (palette.isDark) palette.secondaryGroupedBackground else Color.White)
            .border(0.8.dp, accent.copy(alpha = 0.25f), shape)
            .then(modifier),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(Modifier.weight(1f).padding(vertical = 12.dp).padding(start = 13.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(task.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = palette.primary, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                task.dueDate?.let { Text(Dates.time(it), style = IosFont.caption, fontWeight = FontWeight.Medium, color = if (isOverdue) IosColor.red else palette.secondary) }
            }
            task.description?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = IosFont.footnote, color = palette.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                list?.let { Text(it.listName, style = IosFont.caption, color = palette.secondary, maxLines = 1) }
                collection?.let {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.secondary, modifier = Modifier.size(10.dp))
                    Text(it.collectionName, style = IosFont.caption, color = palette.secondary, maxLines = 1)
                }
            }
        }
    }
}
