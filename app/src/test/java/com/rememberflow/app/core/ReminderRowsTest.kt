package com.rememberflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderRowsTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")

    private fun loop(vararg s: Stage) = Recurrence(s.toList(), true)

    private fun reminder(
        id: Long,
        title: String,
        recurrence: Recurrence,
        startAt: String = "2026-01-01T12:30:00Z",
        details: String = "",
        paused: Boolean = false,
        handled: Long = -1
    ) = Reminder(
        id = id, title = title, details = details, startMillis = Instant.parse(startAt).toEpochMilli(),
        zoneId = "UTC", recurrence = recurrence, paused = paused, createdAtMillis = id, lastHandledIndex = handled
    )

    @Test
    fun rowsAreOrderedActiveByNextThenPausedThenFinished() {
        val soon = reminder(1, "Soon", Recurrence.ONCE, "2026-01-01T12:05:00Z")
        val later = reminder(2, "Later", Recurrence.ONCE, "2026-01-01T18:00:00Z")
        val paused = reminder(3, "Paused", loop(Stage(1, IntervalUnit.HOURS)), paused = true)
        val finished = reminder(4, "Finished", Recurrence.ONCE, "2026-01-01T08:00:00Z", handled = 0)
        val rows = ReminderRows.build(listOf(later, finished, paused, soon), now)
        assertEquals(listOf("Soon", "Later", "Paused", "Finished"), rows.map { it.reminder.title })
        assertEquals(
            listOf(ReminderStatus.ACTIVE, ReminderStatus.ACTIVE, ReminderStatus.PAUSED, ReminderStatus.COMPLETED),
            rows.map { it.status }
        )
        assertEquals(Instant.parse("2026-01-01T12:05:00Z"), rows[0].next)
        assertEquals(null, rows[3].next)
    }

    @Test
    fun searchMatchesTitleNotesAndRecurrenceInAnyWordOrder() {
        val a = reminder(1, "Take vitamins", loop(Stage(12, IntervalUnit.HOURS)), details = "with breakfast")
        val b = reminder(2, "Call mum", loop(Stage(1, IntervalUnit.WEEKS)))
        val rows = ReminderRows.build(listOf(a, b), now)
        assertEquals(listOf("Take vitamins"), ReminderRows.filter(rows, "VITAMINS", ReminderFilter.ALL).map { it.reminder.title })
        assertEquals(listOf("Take vitamins"), ReminderRows.filter(rows, "breakfast take", ReminderFilter.ALL).map { it.reminder.title })
        assertEquals(listOf("Call mum"), ReminderRows.filter(rows, "every week", ReminderFilter.ALL).map { it.reminder.title })
        assertEquals(emptyList<String>(), ReminderRows.filter(rows, "nothing like this", ReminderFilter.ALL).map { it.reminder.title })
        assertEquals(2, ReminderRows.filter(rows, "   ", ReminderFilter.ALL).size)
    }

    @Test
    fun statusFilterSeparatesActiveFromPaused() {
        val active = reminder(1, "Active", loop(Stage(1, IntervalUnit.HOURS)))
        val paused = reminder(2, "Paused", loop(Stage(1, IntervalUnit.HOURS)), paused = true)
        val rows = ReminderRows.build(listOf(active, paused), now)
        assertEquals(listOf("Active"), ReminderRows.filter(rows, "", ReminderFilter.ACTIVE).map { it.reminder.title })
        assertEquals(listOf("Paused"), ReminderRows.filter(rows, "", ReminderFilter.PAUSED).map { it.reminder.title })
    }

    @Test
    fun timelineMergesRemindersChronologicallyAndSkipsPaused() {
        val frequent = reminder(1, "Frequent", loop(Stage(10, IntervalUnit.MINUTES)), startAt = "2026-01-01T12:05:00Z")
        val hourly = reminder(2, "Hourly", loop(Stage(1, IntervalUnit.HOURS)), startAt = "2026-01-01T12:20:00Z")
        val paused = reminder(3, "Paused", loop(Stage(1, IntervalUnit.HOURS)), paused = true)
        val entries = ReminderRows.timeline(listOf(hourly, paused, frequent), now, perReminder = 3)
        val times = entries.map { it.occurrence.time }
        assertEquals(times.sorted(), times)
        assertEquals(6, entries.size)
        assertTrue(entries.none { it.reminder.title == "Paused" })
        assertEquals(
            listOf("Frequent", "Frequent", "Hourly", "Frequent", "Hourly", "Hourly"),
            entries.map { it.reminder.title }
        )
    }

    @Test
    fun timelineRespectsTheOverallLimitAndFinishedSequences() {
        val many = (1L..30L).map { reminder(it, "R$it", loop(Stage(10, IntervalUnit.MINUTES))) }
        assertEquals(40, ReminderRows.timeline(many, now, perReminder = 6, limit = 40).size)
        val finished = reminder(99, "Done", Recurrence.ONCE, "2026-01-01T08:00:00Z")
        assertEquals(0, ReminderRows.timeline(listOf(finished), now).size)
    }
}
