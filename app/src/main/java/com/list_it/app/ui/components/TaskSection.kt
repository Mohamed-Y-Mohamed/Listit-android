package com.list_it.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.ToDoTask
import com.list_it.app.model.TodoList
import com.list_it.app.ui.Dates
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex

/** Card background used by the iOS `.ultraThinMaterial` cards. */
fun Modifier.materialCard(radius: Int, stroke: Color, background: Color): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this.clip(shape).background(background).border(1.dp, stroke, shape)
}

/** A list header with its tasks (iOS `TaskSection`). */
@Composable
fun TaskSection(list: TodoList, tasks: List<ToDoTask>, onSelect: (ToDoTask) -> Unit) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Circle, null, tint = colorFromHex(list.bgColorHex), modifier = Modifier.size(8.dp))
            Spacer(Modifier.width(8.dp))
            Text(list.listName, style = IosFont.headline, color = palette.primary, modifier = Modifier.weight(1f))
            CountBadge("${tasks.size}", palette.material, palette.primary)
        }
        tasks.forEach { task ->
            DetailedTaskCard(task, Modifier.clickable { onSelect(task) })
        }
    }
}

@Composable
fun DetailedTaskCard(task: ToDoTask, modifier: Modifier = Modifier) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    Column(
        modifier.fillMaxWidth().materialCard(16, Color.White.copy(alpha = 0.1f), palette.material).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            db.collections.firstOrNull { it.id == task.collectionID }?.let {
                Text(it.collectionName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = palette.secondary)
            }
            Spacer(Modifier.weight(1f))
            Text(Dates.monthDay(task.createdAt), fontSize = 10.sp, color = palette.tertiary)
        }
        Text(task.text, style = IosFont.body, fontWeight = FontWeight.Bold, color = palette.primary)
        task.description?.takeIf { it.isNotEmpty() }?.let {
            Text(it, style = IosFont.caption, color = palette.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Shared badge style used for counts beside headers. */
@Composable
fun CapsuleCount(count: Int, color: Color) {
    Text(
        "$count", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
        modifier = Modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
