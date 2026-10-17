package com.rememberflow.app.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** All displayed times use the 24-hour clock. */
object Fmt {
    val clock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    val hourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm:ss", Locale.getDefault())

    fun clock(now: Instant, zone: ZoneId): String = now.atZone(zone).format(clock)

    /** "Today", "Tomorrow", or "Fri 16 Oct". */
    fun dayLabel(time: Instant, zone: ZoneId, today: LocalDate): String {
        val date = time.atZone(zone).toLocalDate()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(shortDate)
        }
    }

    /** "in 4m 12s", "in 3h 05m", "in 2d 4h"; "now" when under a second away. */
    fun countdown(now: Instant, target: Instant): String {
        val d = Duration.between(now, target)
        if (d.isNegative || d.toMillis() < 1000) return "now"
        val days = d.toDays()
        val hours = d.toHours() % 24
        val minutes = d.toMinutes() % 60
        val seconds = d.seconds % 60
        return "in " + when {
            days > 0 -> "${days}d ${hours}h"
            d.toHours() > 0 -> "${d.toHours()}h ${minutes.toString().padStart(2, '0')}m"
            d.toMinutes() > 0 -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
            else -> "${seconds}s"
        }
    }

    /** "2h 05m ago" style, used for last-fired information. */
    fun ago(now: Instant, past: Instant): String {
        val d = Duration.between(past, now)
        if (d.isNegative || d.seconds < 5) return "just now"
        return when {
            d.toDays() > 0 -> "${d.toDays()}d ${d.toHours() % 24}h ago"
            d.toHours() > 0 -> "${d.toHours()}h ${(d.toMinutes() % 60).toString().padStart(2, '0')}m ago"
            d.toMinutes() > 0 -> "${d.toMinutes()}m ago"
            else -> "${d.seconds}s ago"
        }
    }
}
