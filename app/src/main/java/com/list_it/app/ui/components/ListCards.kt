
package com.list_it.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon

/**
 * What one list holds. The card shows the contents on one line and the task progress on the
 * next, directly above the bar, so no figure is stated twice.
 */
data class ListCardMetrics(
    val collections: Int = 0,
    val open: Int = 0,
    val done: Int = 0,
    val notes: Int = 0,
) {
    val total: Int get() = open + done
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total

    /** "3 collections · 2 notes": what is inside the list, never repeating the task figures. */
    fun contents(): String = "${plural(collections, "collection")} · ${plural(notes, "note")}"

    /** "1 of 3 tasks done": the caption that sits on top of the progress bar. */
    fun progress(): String = when {
        total == 0 -> "No tasks yet"
        done == total -> if (total == 1) "1 task done" else "All $total tasks done"
        total == 1 -> "$done of 1 task done"
        else -> "$done of $total tasks done"
    }

    private fun plural(count: Int, noun: String) = if (count == 1) "1 $noun" else "$count ${noun}s"
}

/**
 * Metrics for every list, grouped once per change to the underlying data. Counting inside each
 * card walked every task and note per card, so a screen of lists cost cards x rows on every
 * recomposition, including each search keystroke.
 */
@Composable
fun rememberListMetrics(): Map<String, ListCardMetrics> {
    val db = LocalDb.current
    val tasks = db.tasks
    val notes = db.notes
    val collections = db.collections
    return remember(tasks, notes, collections) {
        val open = HashMap<String, Int>()
        val done = HashMap<String, Int>()
        for (task in tasks) {
            if (task.isDeleted) continue
            val bucket = if (task.isCompleted) done else open
            bucket[task.listID] = (bucket[task.listID] ?: 0) + 1
        }
        val noteCounts = notes.asSequence().filter { !it.isDeleted }.groupingBy { it.listID }.eachCount()
        val collectionCounts = collections.groupingBy { it.listID }.eachCount()
        (open.keys + done.keys + noteCounts.keys + collectionCounts.keys).associateWith { id ->
            ListCardMetrics(
                collections = collectionCounts[id] ?: 0,
                open = open[id] ?: 0,
                done = done[id] ?: 0,
                notes = noteCounts[id] ?: 0,
            )
        }
    }
}

private data class Ink(val strong: Color, val quiet: Color)

private data class FolioColors(
    val paper: Color,
    val ledger: Color,
    val tab: Color,
    val outline: Color,
    val paperInk: Ink,
    val ledgerInk: Ink,
    val tabInk: Color,
    val progressInk: Color,
)

@Composable
private fun folioColors(list: TodoList): FolioColors {
    val palette = LocalPalette.current
    val paper = palette.ramp.card.compositeOver(if (palette.isDark) Color.Black else Color.White)
    val accent = colorFromHex(list.bgColorHex).compositeOver(paper)
    val ledger = accent.copy(alpha = if (palette.isDark) 0.15f else 0.09f).compositeOver(paper)
    val tab = accent.copy(alpha = if (palette.isDark) 0.36f else 0.26f).compositeOver(paper)
    val paperInk = inkOn(paper)
    val ledgerInk = inkOn(ledger)
    return FolioColors(
        paper = paper,
        ledger = ledger,
        tab = tab,
        outline = paperInk.strong.copy(alpha = 0.15f),
        paperInk = paperInk,
        ledgerInk = ledgerInk,
        tabInk = inkOn(tab).strong,
        progressInk = if (contrastRatio(accent, ledger) >= 3f) accent else ledgerInk.quiet,
    )
}

private fun inkOn(background: Color): Ink {
    val strong = if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) {
        Color.White
    } else {
        Color.Black
    }
    val softened = strong.copy(alpha = 0.73f).compositeOver(background)
    return Ink(strong, if (contrastRatio(softened, background) >= 4.5f) softened else strong)
}

private fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

private val FolioShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 8.dp,
    bottomEnd = 18.dp,
    bottomStart = 18.dp,
)

private fun Modifier.folioSurface(colors: FolioColors): Modifier =
    clip(FolioShape).background(colors.paper).border(1.dp, colors.outline, FolioShape)

@Composable
fun GridListCard(list: TodoList, metrics: ListCardMetrics, modifier: Modifier = Modifier) {
    val colors = folioColors(list)

    Column(modifier.fillMaxWidth().folioSurface(colors)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(start = 14.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolioTitle(
                list,
                colors.paperInk.strong,
                Modifier.weight(1f).padding(vertical = 12.dp),
            )

            if (list.isPinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Pinned list",
                    modifier = Modifier.size(16.dp),
                    tint = colors.paperInk.quiet,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.ledger)
                .drawBehind {
                    drawLedgerRule(colors.ledgerInk.strong.copy(alpha = 0.22f))
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Detail(metrics.contents(), colors.ledgerInk.quiet)
            Text(
                text = metrics.progress(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.ledgerInk.strong,
            )
            LedgerProgress(metrics.fraction, colors)
        }
    }
}

/** List row: the title and status, and a directional affordance. */
@Composable
fun RowListCard(list: TodoList, metrics: ListCardMetrics, modifier: Modifier = Modifier) {
    val colors = folioColors(list)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .folioSurface(colors),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolioTitle(
                list,
                colors.paperInk.strong,
                Modifier.weight(1f).padding(vertical = 8.dp),
            )

            if (list.isPinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Pinned list",
                    modifier = Modifier.size(16.dp),
                    tint = colors.paperInk.quiet,
                )
            }

            Chevron(colors.paperInk.quiet)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.ledger)
                .drawBehind {
                    drawLedgerRule(colors.ledgerInk.strong.copy(alpha = 0.22f))
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Detail(metrics.contents(), colors.ledgerInk.quiet, Modifier.weight(1f))
                Text(
                    text = metrics.progress(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ledgerInk.strong,
                )
            }
            LedgerProgress(metrics.fraction, colors)
        }
    }
}

/** Pinned rail tile: a concise status; width is set by the caller. */
@Composable
fun PinnedListCard(
    list: TodoList,
    metrics: ListCardMetrics,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = folioColors(list)
    val status = if (list.isDefault) "Default list" else metrics.progress()

    Column(
        modifier = modifier
            .width(width)
            .folioSurface(colors),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolioTitle(
                list,
                colors.paperInk.strong,
                Modifier.weight(1f).padding(vertical = 8.dp),
                compact = true,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .background(colors.ledger)
                .drawBehind {
                    drawLedgerRule(colors.ledgerInk.strong.copy(alpha = 0.22f))
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Detail(status, colors.ledgerInk.quiet)
        }
    }
}

@Composable
private fun FolioTitle(
    list: TodoList,
    color: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Text(
        text = list.listName.ifBlank { "Untitled" },
        modifier = modifier,
        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Detail(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = color)
}

private fun DrawScope.drawLedgerRule(color: Color) {
    val dash = 4.dp.toPx()
    val step = dash + 3.dp.toPx()
    val y = 0.5.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, y), Offset((x + dash).coerceAtMost(size.width), y), 1.dp.toPx())
        x += step
    }
}

/** Eight visual divisions, not eight tasks; the caption above gives the exact count. */
@Composable
private fun LedgerProgress(progress: Float, colors: FolioColors) {
    Canvas(Modifier.fillMaxWidth().height(5.dp)) {
        val divisions = 8
        val gap = 3.dp.toPx().coerceAtMost(size.width / (divisions * 2f))
        val segmentWidth = (size.width - gap * (divisions - 1)) / divisions
        if (segmentWidth <= 0f) return@Canvas
        val track = colors.ledgerInk.strong.copy(alpha = 0.15f)
        for (index in 0 until divisions) {
            val origin = if (layoutDirection == LayoutDirection.Ltr) {
                index * (segmentWidth + gap)
            } else {
                size.width - segmentWidth - index * (segmentWidth + gap)
            }
            val radius = CornerRadius(minOf(size.height, segmentWidth) / 2f)
            drawRoundRect(track, Offset(origin, 0f), Size(segmentWidth, size.height), radius)
            val filled = segmentWidth * (progress * divisions - index).coerceIn(0f, 1f)
            if (filled > 0f) {
                val start = if (layoutDirection == LayoutDirection.Ltr) origin else origin + segmentWidth - filled
                drawRoundRect(
                    colors.progressInk,
                    Offset(start, 0f),
                    Size(filled, size.height),
                    CornerRadius(minOf(size.height, filled) / 2f),
                )
            }
        }
    }
}

@Composable
private fun Chevron(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val outer = if (ltr) 0.38f else 0.62f
        val tip = if (ltr) 0.62f else 0.38f
        val path = Path().apply {
            moveTo(size.width * outer, size.height * 0.27f)
            lineTo(size.width * tip, size.height * 0.50f)
            lineTo(size.width * outer, size.height * 0.73f)
        }
        drawPath(
            path, color,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
