package com.rememberflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderPlannerTest {

    private val start = Instant.parse("2026-01-01T10:00:00Z")

    private fun reminder(
        recurrence: Recurrence,
        handled: Long = -1,
        paused: Boolean = false,
        startAt: Instant = start
    ) = Reminder(
        id = 1, title = "Take medicine", details = "", startMillis = startAt.toEpochMilli(), zoneId = "UTC",
        recurrence = recurrence, paused = paused, createdAtMillis = 0, lastHandledIndex = handled
    )

    private val every10 = Recurrence(listOf(Stage(10, IntervalUnit.MINUTES)), loop = true)

    @Test
    fun alarmFiringOnTimeIsDueOnceAndSchedulesTheNext() {
        val r = reminder(every10, handled = 0)
        val now = Instant.parse("2026-01-01T10:10:01Z")
        val plan = ReminderPlanner.plan(r, now)
        assertEquals(1L, plan.dueCount)
        assertEquals(1L, plan.lastDue?.index)
        assertEquals(Instant.parse("2026-01-01T10:20:00Z"), plan.next?.time)
        assertEquals(1L, plan.handledIndex)
        assertTrue(ReminderPlanner.isOnTime(plan, now))
    }

    @Test
    fun longGapCountsMissedOccurrencesAndIsNotOnTime() {
        val r = reminder(every10, handled = 0)
        val now = Instant.parse("2026-01-01T10:35:00Z") // 10:10, 10:20, 10:30 are due
        val plan = ReminderPlanner.plan(r, now)
        assertEquals(3L, plan.dueCount)
        assertEquals(Instant.parse("2026-01-01T10:30:00Z"), plan.lastDue?.time)
        assertEquals(Instant.parse("2026-01-01T10:40:00Z"), plan.next?.time)
        assertEquals(3L, plan.handledIndex)
        assertFalse(ReminderPlanner.isOnTime(plan, now))
    }

    @Test
    fun nothingIsDueBeforeTheStart() {
        val r = reminder(every10)
        val plan = ReminderPlanner.plan(r, Instant.parse("2026-01-01T09:00:00Z"))
        assertEquals(0L, plan.dueCount)
        assertNull(plan.lastDue)
        assertEquals(start, plan.next?.time)
        assertEquals(-1L, plan.handledIndex)
    }

    @Test
    fun reminderCreatedInThePastDoesNotNotifyForOldOccurrences() {
        val now = Instant.parse("2026-01-01T12:05:00Z")
        val handled = ReminderPlanner.handledIndexFor(start, java.time.ZoneOffset.UTC, every10, now)
        val plan = ReminderPlanner.plan(reminder(every10, handled = handled), now)
        assertEquals(0L, plan.dueCount)
        assertEquals(Instant.parse("2026-01-01T12:10:00Z"), plan.next?.time)
    }

    @Test
    fun oneTimeReminderCompletesAfterItFires() {
        val r = reminder(Recurrence.ONCE)
        assertEquals(ReminderStatus.ACTIVE, ReminderPlanner.status(r, start.minusSeconds(5)))
        val plan = ReminderPlanner.plan(r, start.plusSeconds(1))
        assertEquals(1L, plan.dueCount)
        assertNull(plan.next)
        val done = r.copy(lastHandledIndex = plan.handledIndex)
        assertEquals(ReminderStatus.COMPLETED, ReminderPlanner.status(done, start.plusSeconds(60)))
        assertEquals(0L, ReminderPlanner.plan(done, start.plusSeconds(60)).dueCount)
    }

    @Test
    fun finiteSequenceCompletesAfterItsLastStage() {
        val rec = Recurrence(listOf(Stage(10, IntervalUnit.MINUTES), Stage(1, IntervalUnit.HOURS)), loop = false)
        val r = reminder(rec, handled = 1)
        val plan = ReminderPlanner.plan(r, Instant.parse("2026-01-01T11:11:00Z"))
        assertEquals(1L, plan.dueCount)
        assertEquals(2L, plan.lastDue?.index)
        assertNull(plan.next)
        assertEquals(ReminderStatus.COMPLETED, ReminderPlanner.status(r.copy(lastHandledIndex = 2), Instant.parse("2026-01-01T11:11:00Z")))
    }

    @Test
    fun pausedRemindersReportPausedStatus() {
        val r = reminder(every10, paused = true)
        assertEquals(ReminderStatus.PAUSED, ReminderPlanner.status(r, start))
    }

    @Test
    fun multiStageSequenceSchedulesTheRightGapAfterEachFire() {
        val rec = Recurrence(listOf(Stage(10, IntervalUnit.MINUTES), Stage(1, IntervalUnit.HOURS), Stage(12, IntervalUnit.HOURS)), true)
        var handled = ReminderPlanner.handledIndexFor(start, java.time.ZoneOffset.UTC, rec, start.minusSeconds(60))
        var now = start
        val seen = ArrayList<Instant>()
        repeat(7) {
            val plan = ReminderPlanner.plan(reminder(rec, handled), now)
            val due = plan.lastDue
            if (due != null) {
                seen.add(due.time)
                handled = plan.handledIndex
            }
            now = plan.next!!.time.plusMillis(300)
        }
        assertEquals(
            listOf(
                "2026-01-01T10:00:00Z", "2026-01-01T10:10:00Z", "2026-01-01T11:10:00Z",
                "2026-01-01T23:10:00Z", "2026-01-01T23:20:00Z", "2026-01-02T00:20:00Z", "2026-01-02T12:20:00Z"
            ).map(Instant::parse),
            seen
        )
        assertNotNull(handled)
    }
}
