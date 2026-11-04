package com.list_it.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Date helpers matching the iOS `Calendar` / `Date.formatted` calls used by the app. */
object Dates {
    val zone: ZoneId get() = ZoneId.systemDefault()

    fun Instant.local(): ZonedDateTime = atZone(zone)
    fun Instant.day(): LocalDate = atZone(zone).toLocalDate()
    fun today(): LocalDate = LocalDate.now(zone)
    fun startOfDay(day: LocalDate): Instant = day.atStartOfDay(zone).toInstant()
    fun endOfDay(day: LocalDate): Instant = day.atTime(23, 59, 59).atZone(zone).toInstant()

    fun isToday(i: Instant) = i.day() == today()
    fun isTomorrow(i: Instant) = i.day() == today().plusDays(1)
    fun isYesterday(i: Instant) = i.day() == today().minusDays(1)

    private fun fmt(pattern: String, i: Instant) = DateTimeFormatter.ofPattern(pattern).format(i.local())

    /** `.dateTime.month().day()` → "Mar 28" */
    fun monthDay(i: Instant) = fmt("MMM d", i)
    /** `.dateTime.day().month().year()` → "Mar 28, 2026" */
    fun dayMonthYear(i: Instant): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(i.local())
    /** `.hour().minute()` / `time: .shortened` → "3:15 PM" */
    fun time(i: Instant): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(i.local())
    /** `.day().month().hour().minute()` → "Mar 28, 3:15 PM" */
    fun monthDayTime(i: Instant) = "${monthDay(i)}, ${time(i)}"
    /** `date: .abbreviated, time: .shortened` → "Mar 28, 2026 at 3:15 PM" */
    fun abbreviatedDateTime(i: Instant) = "${dayMonthYear(i)} at ${time(i)}"
    /** `.weekday(.wide).day().month(.wide)` → "Saturday, March 28" */
    fun weekdayDayMonth(i: Instant) = fmt("EEEE, MMMM d", i)
    /** `.month(.wide).year()` → "March 2026" */
    fun monthYear(i: Instant) = fmt("MMMM yyyy", i)
}
