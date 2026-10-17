package com.rememberflow.app.core

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId

/**
 * A saved reminder. [lastHandledIndex] is the highest occurrence index that has either been
 * notified or deliberately skipped (created in the past, paused for a while); -1 means none yet.
 */
data class Reminder(
    val id: Long,
    val title: String,
    val details: String,
    val startMillis: Long,
    val zoneId: String,
    val recurrence: Recurrence,
    val paused: Boolean,
    val createdAtMillis: Long,
    val lastHandledIndex: Long,
    /** When a notification was last posted for this reminder (0 = never). */
    val lastFiredMillis: Long = 0L,
    /** Occurrences that were folded into a later notification because the device was off/asleep. */
    val missedCount: Int = 0
) {
    val start: Instant get() = Instant.ofEpochMilli(startMillis)

    val zone: ZoneId
        get() = try {
            ZoneId.of(zoneId)
        } catch (_: DateTimeException) {
            ZoneId.systemDefault()
        }
}

enum class ReminderStatus { ACTIVE, PAUSED, COMPLETED }

/**
 * What to do for one reminder at a given moment.
 *
 * [dueCount] occurrences have come due since the last handled one (1 = normal, more = some were
 * missed because the phone was off or the alarm was delayed). [lastDue] is the most recent of them,
 * [next] is the next future occurrence to schedule (null once a finite sequence has finished) and
 * [handledIndex] is the value to store back into [Reminder.lastHandledIndex].
 */
data class Plan(
    val dueCount: Long,
    val lastDue: Occurrence?,
    val next: Occurrence?,
    val handledIndex: Long
)

object ReminderPlanner {

    fun plan(reminder: Reminder, now: Instant): Plan {
        val lastPassed = RecurrenceEngine.lastIndexAtOrBefore(
            reminder.start, reminder.zone, reminder.recurrence, now
        )
        val dueCount = maxOf(0L, lastPassed - reminder.lastHandledIndex)
        val lastDue = if (dueCount > 0) {
            RecurrenceEngine.occurrenceAt(reminder.start, reminder.zone, reminder.recurrence, lastPassed)
        } else null
        val next = RecurrenceEngine.occurrenceAt(reminder.start, reminder.zone, reminder.recurrence, lastPassed + 1)
        return Plan(dueCount, lastDue, next, maxOf(reminder.lastHandledIndex, lastPassed))
    }

    fun status(reminder: Reminder, now: Instant): ReminderStatus {
        if (reminder.paused) return ReminderStatus.PAUSED
        return if (plan(reminder, now).next == null) ReminderStatus.COMPLETED else ReminderStatus.ACTIVE
    }

    /** Index to store when a reminder is created, edited or resumed: everything already past is skipped. */
    fun handledIndexFor(start: Instant, zone: ZoneId, recurrence: Recurrence, now: Instant): Long =
        RecurrenceEngine.lastIndexAtOrBefore(start, zone, recurrence, now)

    /** A single due occurrence delivered within this window counts as "on time" rather than "missed". */
    const val ON_TIME_WINDOW_MILLIS = 120_000L

    fun isOnTime(plan: Plan, now: Instant): Boolean {
        val due = plan.lastDue ?: return false
        return plan.dueCount == 1L && now.toEpochMilli() - due.time.toEpochMilli() <= ON_TIME_WINDOW_MILLIS
    }
}
