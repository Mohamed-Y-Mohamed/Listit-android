
package com.list_it.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon

private data class CardMetrics(val open: Int, val done: Int, val notes: Int) {
    val total: Int get() = open + done
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total

    fun summary(): String = when {
        total == 0 && notes == 0 -> "No tasks or notes yet"
        total == 0 -> "No tasks · ${noteLabel()}"
        open == 0 -> "All tasks done · ${noteLabel()}"
        else -> "${openLabel()} · ${noteLabel()}"
    }

    fun openLabel(): String = when (open) {
        0 -> if (total == 0) "No tasks" else "All tasks done"
        1 -> "1 task left"
        else -> "$open tasks left"
    }

    private fun noteLabel(): String = if (notes == 1) "1 note" else "$notes notes"
}

@Composable
private fun readMetrics(list: TodoList): CardMetrics {
    val db = LocalDb.current
    var open = 0
    var done = 0
    for (task in db.tasks) {
        if (task.listID == list.id && !task.isDeleted) {
            if (task.isCompleted) done++ else open++
        }
    }
    val notes = db.notes.count { it.listID == list.id && !it.isDeleted }
    return CardMetrics(open, done, notes)
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
fun GridListCard(list: TodoList, modifier: Modifier = Modifier) {
    val colors = folioColors(list)
    val metrics = readMetrics(list)

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
            Detail(metrics.summary(), colors.ledgerInk.quiet)

            if (metrics.total > 0) {
                Text(
                    text = "${metrics.done} of ${metrics.total} done",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ledgerInk.strong,
                )
                LedgerProgress(metrics.fraction, colors)
            }
        }
    }
}

/** List row: an index tab, the title and status, and a directional affordance. */
@Composable
fun RowListCard(list: TodoList, modifier: Modifier = Modifier) {
    val colors = folioColors(list)
    val metrics = readMetrics(list)

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
            if (metrics.total > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Detail(metrics.summary(), colors.ledgerInk.quiet)
                    }

                    Text(
                        text = "${metrics.done} of ${metrics.total} done",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.ledgerInk.strong,
                    )
                }

                LedgerProgress(metrics.fraction, colors)
            } else {
                Detail(metrics.summary(), colors.ledgerInk.quiet)
            }
        }
    }
}
/** Pinned rail tile: a folded corner and a concise status; width is set by the caller. */
@Composable
fun PinnedListCard(
    list: TodoList,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = folioColors(list)
    val metrics = readMetrics(list)
    val status = when {
        list.isDefault -> "Default list"
        metrics.total == 0 -> "No tasks"
        else -> "${metrics.done} of ${metrics.total} done"
    }

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
private fun Detail(text: String, color: Color) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun FolioTab(colors: FolioColors, pinned: Boolean) {
    val label = if (pinned) Modifier.semantics { contentDescription = "Pinned" } else Modifier
    Canvas(
        Modifier.size(width = 34.dp, height = 32.dp)
            .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 3.dp))
            .background(colors.tab)
            .then(label),
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        if (pinned) {
            val bookmark = Path().apply {
                moveTo(w * 0.36f, h * 0.27f)
                lineTo(w * 0.64f, h * 0.27f)
                lineTo(w * 0.64f, h * 0.74f)
                lineTo(w * 0.50f, h * 0.63f)
                lineTo(w * 0.36f, h * 0.74f)
                close()
            }
            drawPath(bookmark, colors.tabInk, style = Stroke(width = stroke, join = StrokeJoin.Round))
        } else {
            for (index in 0..2) {
                val y = h * (0.34f + index * 0.16f)
                val end = if (index == 2) 0.58f else 0.70f
                drawLine(colors.tabInk, Offset(w * 0.30f, y), Offset(w * end, y), stroke, StrokeCap.Round)
            }
        }
    }
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

private fun DrawScope.drawFold(color: Color, outline: Color) {
    val edge = 24.dp.toPx().coerceAtMost(size.width)
    val drop = 14.dp.toPx().coerceAtMost(size.height)
    val end = if (layoutDirection == LayoutDirection.Ltr) size.width else 0f
    val inner = if (layoutDirection == LayoutDirection.Ltr) end - edge else edge
    val fold = Path().apply {
        moveTo(inner, 0f)
        lineTo(end, 0f)
        lineTo(end, drop)
        close()
    }
    drawPath(fold, color)
    drawLine(outline, Offset(inner, 0f), Offset(end, drop), 1.dp.toPx())
}

/** Eight visual divisions, not eight tasks; the text above gives the exact count. */
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

/** Retained for existing callers. The Folio cards use static, wrapping titles. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollingText(text: String, fontSize: Int, color: Color) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().basicMarquee(
            iterations = Int.MAX_VALUE,
            initialDelayMillis = 3_000,
            repeatDelayMillis = 1_500,
            velocity = 40.dp,
        ),
        fontSize = fontSize.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}
