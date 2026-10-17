package com.rememberflow.app.notify

import android.content.Context
import com.rememberflow.app.core.Reminder
import com.rememberflow.app.core.ReminderPlanner
import com.rememberflow.app.data.ReminderRepository
import java.time.Instant

/**
 * The single place where "what is due right now" is decided and the next alarm is armed.
 *
 * It is idempotent and driven only by stored state plus the clock, so every trigger can call it:
 * an alarm firing, a reboot, a clock or time-zone change, an app update, or the app being opened.
 * Occurrences that came due while the device was off or asleep are folded into one notification
 * that says how many were missed, rather than flooding the shade or silently vanishing.
 */
object ReminderCoordinator {

    private val lock = Any()

    /** Result of one pass, used by the UI to report problems honestly. */
    data class Summary(
        val notified: Int,
        val suppressedByNotificationSettings: Int,
        val inexactAlarms: Int
    )

    /** Notifies everything that is due and re-arms one alarm per active reminder. */
    fun processDue(context: Context, now: Instant = Instant.now()): Summary = synchronized(lock) {
        val appContext = context.applicationContext
        val repo = ReminderRepository.get(appContext)
        val scheduler = ReminderScheduler(appContext)
        var notified = 0
        var suppressed = 0
        var inexact = 0

        for (reminder in repo.getAll()) {
            if (reminder.paused) {
                scheduler.cancel(reminder.id)
                continue
            }
            val plan = ReminderPlanner.plan(reminder, now)
            var current: Reminder = reminder

            val due = plan.lastDue
            if (plan.dueCount > 0 && due != null) {
                val late = !ReminderPlanner.isOnTime(plan, now)
                val posted = NotificationHelper.post(
                    context = appContext,
                    reminder = reminder,
                    dueTime = due.time,
                    dueCount = plan.dueCount,
                    late = late,
                    zone = reminder.zone
                )
                val folded = (plan.dueCount - 1).toInt()
                current = reminder.copy(
                    lastHandledIndex = plan.handledIndex,
                    lastFiredMillis = if (posted) now.toEpochMilli() else reminder.lastFiredMillis,
                    // If the system blocked the notification, every due occurrence counts as missed.
                    missedCount = reminder.missedCount + if (posted) folded else plan.dueCount.toInt()
                )
                if (posted) notified++ else suppressed++
                repo.update(current)
            } else if (plan.handledIndex != reminder.lastHandledIndex) {
                current = reminder.copy(lastHandledIndex = plan.handledIndex)
                repo.update(current)
            }

            when (scheduler.schedule(current.id, plan.next?.time)) {
                AlarmPrecision.INEXACT -> inexact++
                else -> Unit
            }
        }
        Summary(notified, suppressed, inexact)
    }

    /**
     * Re-arms a single reminder after the UI created, edited, paused or resumed it. [handledIndex]
     * is supplied by the caller (past occurrences are skipped, never replayed).
     */
    fun rearm(context: Context, reminder: Reminder, now: Instant = Instant.now()): AlarmPrecision? =
        synchronized(lock) {
            val scheduler = ReminderScheduler(context.applicationContext)
            if (reminder.paused) {
                scheduler.cancel(reminder.id)
                return@synchronized null
            }
            val next = ReminderPlanner.plan(reminder, now).next?.time
            scheduler.schedule(reminder.id, next)
        }

    fun remove(context: Context, reminderId: Long) = synchronized(lock) {
        val appContext = context.applicationContext
        ReminderScheduler(appContext).cancel(reminderId)
        NotificationHelper.cancel(appContext, reminderId)
        ReminderRepository.get(appContext).delete(reminderId)
    }

    /** Pauses from a notification action or the UI: stops alarms and clears the shown notification. */
    fun pause(context: Context, reminderId: Long) = synchronized(lock) {
        val appContext = context.applicationContext
        val repo = ReminderRepository.get(appContext)
        val reminder = repo.get(reminderId) ?: return@synchronized
        repo.update(reminder.copy(paused = true))
        ReminderScheduler(appContext).cancel(reminderId)
        NotificationHelper.cancel(appContext, reminderId)
    }

    /** Resumes without replaying anything that passed while paused. */
    fun resume(context: Context, reminderId: Long, now: Instant = Instant.now()) = synchronized(lock) {
        val appContext = context.applicationContext
        val repo = ReminderRepository.get(appContext)
        val reminder = repo.get(reminderId) ?: return@synchronized
        val handled = ReminderPlanner.handledIndexFor(reminder.start, reminder.zone, reminder.recurrence, now)
        val resumed = reminder.copy(paused = false, lastHandledIndex = handled, missedCount = 0)
        repo.update(resumed)
        rearm(appContext, resumed, now)
    }
}
