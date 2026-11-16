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
import androidx.compose.material.icons.filled.Checklist
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

// The cards that show each list on the home page:
//  - GridListCard   "Cards" layout, two per row: a clean tinted card with a coloured icon badge.
//  - RowListCard    "List" layout, one per row: coloured wave bands on the right-hand side.
//  - PinnedListCard the small cards in the "Pinned Lists" row.

/** One band of the wave: right edge and bulge as fractions of the width, fill strength per theme. */
private data class Wave(val depth: Float, val curvature: Float, val dark: Float, val light: Float)

/** The three wave bands used on the small pinned cards. */
private val WAVES = listOf(
    Wave(0.90f, 0.164f, 0.16f, 0.14f),
    Wave(0.734f, 0.151f, 0.25f, 0.22f),
    Wave(0.545f, 0.132f, 0.45f, 0.38f),
)

/**
 * The wave bands for the full-width "List" layout. They sit close together near the right edge,
 * so the colour doesn't stretch across the whole row and the text stays on a plain background.
 */
private val ROW_WAVES = listOf(
    Wave(0.96f, 0.035f, 0.16f, 0.14f),
    Wave(0.90f, 0.035f, 0.25f, 0.22f),
    Wave(0.84f, 0.035f, 0.45f, 0.38f),
)

/** How much of the card's width the name and counts may use, so text never sits on the wave. */
private const val TEXT_WIDTH = 0.8f
private const val ROW_TEXT_WIDTH = 0.66f

/** Paints the coloured wave behind a list card. */
private fun Modifier.listWave(color: Color, isDark: Boolean, waves: List<Wave>) = drawBehind {
    waves.forEach { w ->
        drawPath(
            wavePath(size, size.width * w.depth, size.width * w.curvature),
            color.copy(alpha = if (isDark) w.dark else w.light),
        )
    }
}

/** The card's background, coloured border and soft shadow. Pass `waves = null` for a card with no wave. */
@Composable
private fun Modifier.listCardSurface(color: Color, radius: Dp = 17.dp, waves: List<Wave>? = WAVES): Modifier {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(radius)
    val shadow = Color.Black.copy(alpha = if (palette.isDark) 0.12f else 0.06f)
    return this
        .shadow(6.dp, shape, ambientColor = shadow, spotColor = shadow)
        .clip(shape)
        .background(palette.ramp.card)
        .then(if (waves != null) Modifier.listWave(color, palette.isDark, waves) else Modifier.background(color.copy(alpha = if (palette.isDark) 0.14f else 0.10f)))
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

/** The colour for a list's name, depending on light or dark mode. */
@Composable
private fun nameColor() = if (LocalPalette.current.isDark) Color.White else WebColor.gray900

/** The softer colour for the "3 tasks · 1 note" line on the card view. */
@Composable
private fun countsColor() = if (LocalPalette.current.isDark) WebColor.gray400 else WebColor.gray500

/** The little pin icon on pinned lists. */
@Composable
private fun PinBadge() = Icon(Icons.Filled.PushPin, "Pinned", tint = WebColor.orange400, modifier = Modifier.size(12.dp))

/** "Cards" layout, two to a row: a coloured icon badge, then the list's name and how many tasks and notes it has. */
@Composable
fun GridListCard(list: TodoList, modifier: Modifier = Modifier) {
    val color = colorFromHex(list.bgColorHex)
    Column(
        modifier.fillMaxWidth().heightIn(min = 112.dp).listCardSurface(color, waves = null).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Checklist, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            if (list.isPinned) PinBadge()
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                list.listName.ifEmpty { "Untitled" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, color = nameColor(),
            )
            Text(countsLabel(list), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = countsColor())
        }
    }
}

/** "List" layout, one full-width row each, with the wave bands on the right. */
@Composable
fun RowListCard(list: TodoList, modifier: Modifier = Modifier) {
    val color = colorFromHex(list.bgColorHex)
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).listCardSurface(color, waves = ROW_WAVES).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.fillMaxWidth(ROW_TEXT_WIDTH)) {
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
