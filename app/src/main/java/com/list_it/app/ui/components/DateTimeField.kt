package com.list_it.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Compact date (and optional time) picker, like iOS `DatePicker` with `.compact` style.
 * The chosen value is clamped to [min]...[max], matching the iOS `in:` range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    value: Instant,
    onValueChange: (Instant) -> Unit,
    min: Instant?,
    max: Instant? = null,
    includeTime: Boolean,
    tint: Color,
) {
    val zone = ZoneId.systemDefault()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val local = value.atZone(zone)
    val palette = LocalPalette.current

    fun clamp(i: Instant): Instant {
        var r = i
        if (min != null && r < min) r = min
        if (max != null && r > max) r = max
        return r
    }

    Row {
        Pill(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(local), palette.fill, tint) { showDate = true }
        if (includeTime) {
            Pill(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local), palette.fill, tint, Modifier.padding(start = 6.dp)) { showTime = true }
        }
    }

    if (showDate) {
        val minDay = min?.atZone(zone)?.toLocalDate()
        val maxDay = max?.atZone(zone)?.toLocalDate()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return (minDay == null || !day.isBefore(minDay)) && (maxDay == null || !day.isAfter(maxDay))
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(clamp(day.atTime(local.toLocalTime()).atZone(zone).toInstant()))
                    }
                    showDate = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state, showModeToggle = false) }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = local.hour, initialMinute = local.minute)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val day: LocalDate = local.toLocalDate()
                    onValueChange(clamp(day.atTime(LocalTime.of(state.hour, state.minute)).atZone(zone).toInstant()))
                    showTime = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun Pill(text: String, background: Color, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        style = IosFont.body,
        color = tint,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
