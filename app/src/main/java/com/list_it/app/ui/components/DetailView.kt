package com.list_it.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.WebColor

// Read-only "view" parts of the task and note sheets, from the web app's
// TasksDetails / notedetail view mode.

object ViewColor {
    val success = Color(0xFF10B981)
    val info = Color(0xFF38BDF8)
    val warning = Color(0xFFF59E0B)
    val danger = Color(0xFFF43F5E)
}

/** Small uppercase label above a section of the view (web `SectionLabel`). */
@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.9.sp,
        color = if (LocalPalette.current.isDark) WebColor.gray500 else WebColor.gray400,
    )
}

/** "Collection  General" style row (web `InfoRow`). */
@Composable
fun InfoRow(label: String, value: String?) {
    val isDark = LocalPalette.current.isDark
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = if (isDark) WebColor.gray500 else WebColor.gray400)
        Spacer(Modifier.width(16.dp))
        Text(
            value ?: "None", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
            color = if (value != null) (if (isDark) Color(0xFFE5E7EB) else Color(0xFF374151)) else (if (isDark) Color(0xFF4B5563) else WebColor.gray400),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Date pill, red when overdue (web `DateChip`). */
@Composable
fun DateChip(label: String, overdue: Boolean) {
    val color = if (overdue) ViewColor.danger else ViewColor.info
    Text(
        label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Footer buttons: outlined in a colour, or filled for the main action. */
@Composable
fun ViewFooterButton(text: String, icon: ImageVector, color: Color, prominent: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.heightIn(min = 44.dp).alpha(if (enabled) 1f else 0.5f).clip(shape)
            .background(if (prominent) color else color.copy(alpha = 0.10f))
            .then(if (prominent) Modifier else Modifier.border(1.dp, color.copy(alpha = 0.35f), shape))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (prominent) Color.White else color
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
