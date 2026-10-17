package com.rememberflow.app.core

import java.time.Instant
import java.util.Locale

enum class ReminderFilter { ALL, ACTIVE, PAUSED }

/** A reminder together with what the list needs to show: status and the next scheduled time. */
data class ReminderRow(val reminder: Reminder, val status: ReminderStatus, val next: Instant?)

/** One future occurrence of one reminder, for the chronological timeline. */
data class TimelineEntry(val reminder: Reminder, val occurrence: Occurrence)

object ReminderRows {

    /**
     * Rows ordered for the home list: active reminders by soonest next time, then paused ones, then
     * finished ones (most recently created first within the last two groups).
     */
    fun build(reminders: List<Reminder>, now: Instant): List<ReminderRow> {
        val rows = reminders.map { r ->
            val status = ReminderPlanner.status(r, now)
            val next = if (r.paused) {
                // Show when it would fire next if resumed, without arming anything.
                RecurrenceEngine.nextAfter(r.start, r.zone, r.recurrence, now)?.time
            } else {
                ReminderPlanner.plan(r, now).next?.time
            }
            ReminderRow(r, status, next)
        }
        val active = rows.filter { it.status == ReminderStatus.ACTIVE }.sortedBy { it.next }
        val paused = rows.filter { it.status == ReminderStatus.PAUSED }.sortedByDescending { it.reminder.createdAtMillis }
        val done = rows.filter { it.status == ReminderStatus.COMPLETED }.sortedByDescending { it.reminder.createdAtMillis }
        return active + paused + done
    }

    /** Applies the status filter and a case-insensitive search over title, notes and recurrence text. */
    fun filter(rows: List<ReminderRow>, query: String, filter: ReminderFilter): List<ReminderRow> {
        val needle = query.trim().lowercase(Locale.getDefault())
        return rows.filter { row ->
            val statusOk = when (filter) {
                ReminderFilter.ALL -> true
                ReminderFilter.ACTIVE -> row.status == ReminderStatus.ACTIVE
                ReminderFilter.PAUSED -> row.status == ReminderStatus.PAUSED
            }
            statusOk && (needle.isEmpty() || matches(row.reminder, needle))
        }
    }

    private fun matches(r: Reminder, needle: String): Boolean {
        val haystack = buildString {
            append(r.title).append('\n')
            append(r.details).append('\n')
            append(r.recurrence.describe()).append('\n')
            append(r.recurrence.describeShort())
        }.lowercase(Locale.getDefault())
        // Every word of the query must appear somewhere, in any order.
        return needle.split(Regex("\\s+")).all { it.isEmpty() || haystack.contains(it) }
    }

    /**
     * The next occurrences across all active reminders merged into one chronological list. Paused
     * and finished reminders do not appear. Takes up to [perReminder] from each, capped at [limit].
     */
    fun timeline(reminders: List<Reminder>, now: Instant, perReminder: Int = 6, limit: Int = 80): List<TimelineEntry> {
        val entries = ArrayList<TimelineEntry>()
        for (r in reminders) {
            if (r.paused) continue
            val upcoming = RecurrenceEngine.upcoming(r.start, r.zone, r.recurrence, now.minusMillis(1), perReminder)
            for (o in upcoming) entries.add(TimelineEntry(r, o))
        }
        entries.sortWith(compareBy<TimelineEntry> { it.occurrence.time }.thenBy { it.reminder.id })
        return if (entries.size > limit) entries.subList(0, limit).toList() else entries
    }
}
