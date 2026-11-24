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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.theme.LocalPalette

// Read-only "view" parts of the task and note sheets, from the web app's
// TasksDetails / notedetail view mode.

object ViewColor {
    val success = Color(0xFF10B981)
    val info = Color(0xFF38BDF8)
    val warning = Color(0xFFF59E0B)
    val danger = Color(0xFFF43F5E)
}

/** A small heading in capitals, like "DUE DATE", above each section. Grey unless you pass a [color]. */
@Composable
fun SectionLabel(text: String, color: Color? = null) {
    Text(
        text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.9.sp,
        color = color ?: LocalPalette.current.secondary,
    )
}

/** A label on the left and its value on the right, e.g. "Collection   General". */
@Composable
fun InfoRow(label: String, value: String?) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = palette.secondary)
        Spacer(Modifier.width(16.dp))
        Text(
            value ?: "None", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
            color = if (value != null) palette.primary else palette.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A rounded date tag. It turns red when the date has passed. */
@Composable
fun DateChip(label: String, overdue: Boolean) {
    val color = if (overdue) ViewColor.danger else ViewColor.info
    Text(
        label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/**
 * A button for the bottom of the task and note pages.
 * Normally it's a light tinted outline; set [prominent] for the main action and it becomes solid.
 */
@Composable
fun ViewFooterButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true,
    contentColor: Color? = null,
    pill: Boolean = false,
    minHeight: Dp = 44.dp,
    onClick: () -> Unit,
) {
    // [pill] gives fully rounded ends; [contentColor] overrides the icon and text colour.
    val shape = if (pill) RoundedCornerShape(percent = 50) else RoundedCornerShape(12.dp)
    Row(
        modifier.heightIn(min = minHeight).alpha(if (enabled) 1f else 0.5f).clip(shape)
            .background(if (prominent) color else color.copy(alpha = 0.10f))
            .then(if (prominent) Modifier else Modifier.border(1.dp, color.copy(alpha = 0.35f), shape))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = contentColor ?: if (prominent) Color.White else color
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
