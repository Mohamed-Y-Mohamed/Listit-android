package com.list_it.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.WebColor
import com.list_it.app.ui.theme.colorFromHex

// List cards from the web app's Capacitor build (NativeListCard + ListWave), with the
// wave stretched to 90% of the card and the text kept inside the first 80%.

/** One band of the wave: right edge and bulge as fractions of the width, fill strength per theme. */
private data class Wave(val depth: Float, val curvature: Float, val dark: Float, val light: Float)

/** The web's three bands (38/31/23%) scaled so the outermost reaches 90% of the card. */
private val WAVES = listOf(
    Wave(0.90f, 0.164f, 0.16f, 0.14f),
    Wave(0.734f, 0.151f, 0.25f, 0.22f),
    Wave(0.545f, 0.132f, 0.45f, 0.38f),
)

/** Share of the card the name and counts may use, so text never sits on the wave's edge. */
private const val TEXT_WIDTH = 0.8f

private fun Modifier.listWave(color: Color, isDark: Boolean) = drawBehind {
    WAVES.forEach { w ->
        drawPath(
            wavePath(size, size.width * w.depth, size.width * w.curvature),
            color.copy(alpha = if (isDark) w.dark else w.light),
        )
    }
}

/** Card surface, colour border and soft shadow shared by every list card. */
@Composable
private fun Modifier.listCardSurface(color: Color, radius: Dp = 17.dp): Modifier {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(radius)
    val shadow = Color.Black.copy(alpha = if (palette.isDark) 0.12f else 0.06f)
    return this
        .shadow(6.dp, shape, ambientColor = shadow, spotColor = shadow)
        .clip(shape)
        .background(palette.ramp.card)
        .listWave(color, palette.isDark)
        .border(0.8.dp, color.copy(alpha = if (palette.isDark) 0.25f else 0.16f), shape)
}

/** "4 tasks · 2 notes", counting open tasks and notes like the iOS app. */
@Composable
private fun countsLabel(list: TodoList): String {
    val db = LocalDb.current
    val tasks = db.tasks.count { it.listID == list.id && !it.isDeleted && !it.isCompleted }
    val notes = db.notes.count { it.listID == list.id && !it.isDeleted }
    return "$tasks task${if (tasks == 1) "" else "s"} · $notes note${if (notes == 1) "" else "s"}"
}

@Composable
private fun nameColor() = if (LocalPalette.current.isDark) Color.White else WebColor.gray900

@Composable
private fun PinBadge() = Icon(Icons.Filled.PushPin, "Pinned", tint = WebColor.orange400, modifier = Modifier.size(12.dp))

/** Cards layout: two to a row. */
@Composable
fun GridListCard(list: TodoList, modifier: Modifier = Modifier) {
    val color = colorFromHex(list.bgColorHex)
    Box(modifier.fillMaxWidth().heightIn(min = 84.dp).listCardSurface(color)) {
        Column(
            Modifier.fillMaxWidth(TEXT_WIDTH).heightIn(min = 84.dp).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    list.listName.ifEmpty { "Untitled" }, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, color = nameColor(), modifier = Modifier.weight(1f, fill = false),
                )
                if (list.isPinned) Box(Modifier.padding(top = 3.dp)) { PinBadge() }
            }
            Spacer(Modifier.height(8.dp))
            Text(countsLabel(list), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = nameColor())
        }
    }
}

/** List layout: one full-width row each. */
@Composable
fun RowListCard(list: TodoList, modifier: Modifier = Modifier) {
    val color = colorFromHex(list.bgColorHex)
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).listCardSurface(color).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.fillMaxWidth(TEXT_WIDTH)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    list.listName.ifEmpty { "Untitled" }, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = nameColor(), modifier = Modifier.weight(1f, fill = false),
                )
                if (list.isPinned) PinBadge()
            }
            Text(countsLabel(list), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = nameColor())
        }
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(20.dp),
            tint = if (LocalPalette.current.isDark) WebColor.gray500 else WebColor.gray400,
        )
    }
}

/** Pinned rail card: one standard size, sized by the caller so three fit on screen. */
@Composable
fun PinnedListCard(list: TodoList, width: Dp, modifier: Modifier = Modifier) {
    val color = colorFromHex(list.bgColorHex)
    Box(modifier.width(width).height(72.dp).listCardSurface(color), contentAlignment = Alignment.CenterStart) {
        Column(Modifier.fillMaxWidth(TEXT_WIDTH).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ScrollingText(list.listName.ifEmpty { "Untitled" }, fontSize = 14, color = nameColor())
            if (!list.isDefault) {
                Text(countsLabel(list), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = nameColor())
            }
        }
    }
}

/**
 * One line that scrolls itself when it is too long, so the whole name can be read
 * (web `ScrollingText`): rests 3s, travels at 40dp/s, waits 1.5s, then repeats.
 * Names that fit never move.
 */
@Composable
fun ScrollingText(text: String, fontSize: Int, color: Color) {
    Text(
        text, fontSize = fontSize.sp, fontWeight = FontWeight.Medium, maxLines = 1, color = color,
        modifier = Modifier.fillMaxWidth().basicMarquee(
            iterations = Int.MAX_VALUE,
            initialDelayMillis = 3_000,
            repeatDelayMillis = 1_500,
            velocity = 40.dp,
        ),
    )
}
