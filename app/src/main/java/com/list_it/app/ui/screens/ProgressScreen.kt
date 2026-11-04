package com.list_it.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.Dates
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.WebColor
import com.list_it.app.ui.theme.appBackground
import com.list_it.app.ui.theme.colorFromHex
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

// Progress tab, from the web app's Capacitor build (Dashboard/ProgressView).

private enum class Range(val label: String) { WEEK("7 days"), MONTH("1 month"), YEAR("1 year") }

private data class Bucket(val label: String, val completed: Int, val inProgress: Int)

@Composable
fun ProgressScreen() {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val strong = if (palette.isDark) Color.White else WebColor.gray900
    val muted = if (palette.isDark) WebColor.gray400 else WebColor.gray500
    val track = if (palette.isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    var range by remember { mutableStateOf(Range.WEEK) }
    val locale = LocalConfiguration.current.locales[0]

    val tasks = db.tasks.filter { !it.isDeleted }
    val completed = tasks.count { it.isCompleted }
    val open = tasks.size - completed
    val percent = if (tasks.isEmpty()) 0 else Math.round(completed * 100f / tasks.size)
    val notes = db.notes.count { !it.isDeleted }

    // Activity: completed and still-open tasks per period.
    val now = ZonedDateTime.now(Dates.zone)
    val spans: List<Triple<String, Instant, Instant>> = when (range) {
        Range.WEEK -> (0 until 7).map { i ->
            val start = now.toLocalDate().minusDays((6 - i).toLong()).atStartOfDay(Dates.zone)
            Triple(start.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), start.toInstant(), start.plusDays(1).toInstant())
        }
        Range.MONTH -> (0 until 4).map { i ->
            val end = now.minusDays(((3 - i) * 7).toLong())
            Triple(DateTimeFormatter.ofPattern("d MMM", locale).format(end), end.minusDays(7).toInstant(), end.toInstant())
        }
        Range.YEAR -> (0 until 12).map { i ->
            val end = now.minusMonths((11 - i).toLong())
            Triple(end.month.getDisplayName(TextStyle.NARROW, locale), end.minusMonths(1).toInstant(), end.toInstant())
        }
    }
    fun inSpan(at: Instant?, start: Instant, end: Instant) = at != null && at >= start && at < end
    val activity = spans.map { (label, start, end) ->
        Bucket(
            label,
            completed = tasks.count { inSpan(it.dateCompleted, start, end) },
            inProgress = tasks.count { !it.isCompleted && inSpan(it.createdAt, start, end) },
        )
    }
    val peak = activity.maxOfOrNull { it.completed + it.inProgress } ?: 0
    val oldest = tasks.minOfOrNull { it.createdAt }
    val rangeStart = when (range) {
        Range.WEEK -> now.minusDays(7)
        Range.MONTH -> now.minusMonths(1)
        Range.YEAR -> now.minusYears(1)
    }.toInstant()
    val thinHistory = oldest != null && oldest > rangeStart

    val perList = db.lists.map { list ->
        val own = tasks.filter { it.listID == list.id }
        Triple(list, own.size, own.count { it.isCompleted })
    }.filter { it.second > 0 }.sortedByDescending { it.second }

    val cardShape = RoundedCornerShape(16.dp)
    val edge = if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    fun Modifier.card() = this.clip(cardShape).background(palette.ramp.card).border(1.dp, edge, cardShape)

    Column(
        Modifier.fillMaxSize().background(appBackground).verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 24.dp),
    ) {
        Text("Progress", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = strong)
        Text(if (tasks.isEmpty()) "No tasks yet" else "$percent% of tasks completed", fontSize = 14.sp, color = muted, modifier = Modifier.padding(top = 4.dp))
        ProgressBar(percent / 100f, WebColor.indigo, track, Modifier.padding(top = 12.dp))

        // Tiles: numbers first, no decorative icons.
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Completed" to completed, "Open" to open, "Notes" to notes, "Lists" to db.lists.size).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, value) ->
                        Column(Modifier.weight(1f).card().padding(16.dp)) {
                            Text("$value", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = strong, lineHeight = 28.sp)
                            Text(label, fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }

        // Activity chart.
        Row(Modifier.fillMaxWidth().padding(top = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Activity", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = strong, modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(if (palette.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)).padding(2.dp)) {
                Range.entries.forEach { option ->
                    val selected = option == range
                    Text(
                        option.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (selected) strong else muted,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) palette.ramp.selected else Color.Transparent)
                            .clickable { range = option }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp).card().padding(16.dp)) {
            Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                activity.forEach { bucket ->
                    fun share(v: Int) = if (peak == 0 || v == 0) 0f else maxOf(v.toFloat() / peak, 0.04f)
                    val open by animateFloatAsState(share(bucket.inProgress), tween(350), label = "open")
                    val done by animateFloatAsState(share(bucket.completed), tween(350), label = "done")
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                        if (open > 0f) Box(Modifier.fillMaxWidth().fillMaxHeight(open).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(WebColor.indigo.copy(alpha = 0.35f)))
                        if (done > 0f) Box(
                            Modifier.fillMaxWidth().fillMaxHeight(if (open > 0f) done / (1f - open).coerceAtLeast(0.01f) else done)
                                .clip(if (bucket.inProgress == 0) RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp) else RoundedCornerShape(0.dp))
                                .background(WebColor.indigo),
                        )
                        if (bucket.completed + bucket.inProgress == 0) Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(WebColor.indigo.copy(alpha = 0.25f)))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                activity.forEach { Text(it.label, fontSize = 10.sp, color = muted, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                LegendDot(WebColor.indigo, "Completed", muted)
                LegendDot(WebColor.indigo.copy(alpha = 0.35f), "In progress", muted)
            }
            if (peak == 0 || thinHistory) {
                Text(
                    if (peak == 0) "Nothing started or completed in this period yet." else "Your account does not cover this whole range yet.",
                    fontSize = 11.sp, color = muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        if (perList.isNotEmpty()) {
            Text("Progress by list", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = strong, modifier = Modifier.padding(top = 32.dp))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                perList.forEach { (list, total, done) ->
                    Column(Modifier.fillMaxWidth().card().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(list.listName.ifEmpty { "Untitled" }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = strong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            Text("$done / $total", fontSize = 12.sp, color = muted)
                        }
                        ProgressBar(done.toFloat() / total, colorFromHex(list.bgColorHex.ifEmpty { "#6366F1" }), track, Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(500), label = "progress")
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(CircleShape).background(color))
    }
}

@Composable
private fun LegendDot(color: Color, label: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = textColor)
    }
}
