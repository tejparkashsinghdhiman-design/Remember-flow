package com.rememberflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Random

class RecurrenceEngineTest {

    private val utc: ZoneId = ZoneOffset.UTC
    private val start: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun loop(vararg stages: Stage) = Recurrence(stages.toList(), loop = true)
    private fun once(vararg stages: Stage) = Recurrence(stages.toList(), loop = false)
    private fun min(n: Int) = Stage(n, IntervalUnit.MINUTES)
    private fun hr(n: Int) = Stage(n, IntervalUnit.HOURS)
    private fun day(n: Int) = Stage(n, IntervalUnit.DAYS)
    private fun week(n: Int) = Stage(n, IntervalUnit.WEEKS)

    private fun times(rec: Recurrence, zone: ZoneId, from: Instant, startAt: Instant, count: Int) =
        RecurrenceEngine.upcoming(startAt, zone, rec, from, count).map { it.time }

    @Test
    fun oneTimeReminderHasExactlyOneOccurrence() {
        val rec = Recurrence.ONCE
        assertEquals(start, RecurrenceEngine.occurrenceAt(start, utc, rec, 0)?.time)
        assertNull(RecurrenceEngine.occurrenceAt(start, utc, rec, 1))
        assertEquals(start, RecurrenceEngine.nextAfter(start, utc, rec, start.minusSeconds(1))?.time)
        assertNull(RecurrenceEngine.nextAfter(start, utc, rec, start))
        assertEquals(1L, RecurrenceEngine.totalOccurrences(rec))
    }

    @Test
    fun everyTenMinutes() {
        val rec = loop(min(10))
        val got = times(rec, utc, start.minusSeconds(1), start, 4)
        assertEquals(
            listOf(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:10:00Z"),
                Instant.parse("2026-01-01T00:20:00Z"),
                Instant.parse("2026-01-01T00:30:00Z")
            ),
            got
        )
    }

    @Test
    fun multiStageSequenceLoops() {
        // 10 minutes -> 1 hour -> 12 hours -> repeat
        val rec = loop(min(10), hr(1), hr(12))
        val got = times(rec, utc, start.minusSeconds(1), start, 7)
        assertEquals(
            listOf(
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:10:00Z",
                "2026-01-01T01:10:00Z",
                "2026-01-01T13:10:00Z", // end of first cycle
                "2026-01-01T13:20:00Z", // second cycle starts
                "2026-01-01T14:20:00Z",
                "2026-01-02T02:20:00Z"
            ).map(Instant::parse),
            got
        )
    }

    @Test
    fun nonLoopingSequenceStopsAfterLastStage() {
        val rec = once(min(10), hr(1))
        assertEquals(3L, RecurrenceEngine.totalOccurrences(rec))
        val all = times(rec, utc, start.minusSeconds(1), start, 10)
        assertEquals(3, all.size)
        assertEquals(Instant.parse("2026-01-01T01:10:00Z"), all.last())
        assertNull(RecurrenceEngine.occurrenceAt(start, utc, rec, 3))
        assertNull(RecurrenceEngine.nextAfter(start, utc, rec, Instant.parse("2026-01-01T01:10:00Z")))
        assertEquals(2L, RecurrenceEngine.lastIndexAtOrBefore(start, utc, rec, Instant.parse("2030-01-01T00:00:00Z")))
    }

    @Test
    fun nextAfterIsStrictlyLater() {
        val rec = loop(hr(1))
        val exactlyOnOccurrence = Instant.parse("2026-01-01T03:00:00Z")
        assertEquals(Instant.parse("2026-01-01T04:00:00Z"), RecurrenceEngine.nextAfter(start, utc, rec, exactlyOnOccurrence)?.time)
        assertEquals(Instant.parse("2026-01-01T03:00:00Z"), RecurrenceEngine.nextAfter(start, utc, rec, exactlyOnOccurrence.minusMillis(1))?.time)
    }

    @Test
    fun beforeStartIndexIsMinusOneAndFirstOccurrenceIsTheStart() {
        val rec = loop(hr(1))
        assertEquals(-1L, RecurrenceEngine.lastIndexAtOrBefore(start, utc, rec, start.minusMillis(1)))
        assertEquals(0L, RecurrenceEngine.lastIndexAtOrBefore(start, utc, rec, start))
        assertEquals(start, RecurrenceEngine.nextAfter(start, utc, rec, start.minusSeconds(60))?.time)
    }

    @Test
    fun everyOtherDayAndWeeklyPresets() {
        val everyOtherDay = Recurrence.PRESETS.first { it.key == "every_other_day" }.recurrence
        assertEquals(
            listOf("2026-01-01T00:00:00Z", "2026-01-03T00:00:00Z", "2026-01-05T00:00:00Z").map(Instant::parse),
            times(everyOtherDay, utc, start.minusSeconds(1), start, 3)
        )
        val weekly = Recurrence.PRESETS.first { it.key == "every_week" }.recurrence
        assertEquals(
            listOf("2026-01-01T00:00:00Z", "2026-01-08T00:00:00Z", "2026-01-15T00:00:00Z").map(Instant::parse),
            times(weekly, utc, start.minusSeconds(1), start, 3)
        )
    }

    @Test
    fun allRequestedPresetsExistWithExpectedGaps() {
        val expected = mapOf(
            "every_10_min" to Duration.ofMinutes(10),
            "every_hour" to Duration.ofHours(1),
            "every_12_hours" to Duration.ofHours(12),
            "every_24_hours" to Duration.ofHours(24),
            "every_other_day" to Duration.ofDays(2),
            "every_3_days" to Duration.ofDays(3),
            "every_5_days" to Duration.ofDays(5),
            "every_week" to Duration.ofDays(7)
        )
        assertEquals(expected.keys, Recurrence.PRESETS.map { it.key }.toSet())
        for (preset in Recurrence.PRESETS) {
            val two = times(preset.recurrence, utc, start.minusSeconds(1), start, 2)
            assertEquals(preset.key, expected.getValue(preset.key), Duration.between(two[0], two[1]))
            assertEquals(preset, Recurrence.PRESETS.first { it.recurrence == preset.recurrence })
        }
    }

    @Test
    fun calendarDaysKeepWallClockAcrossDaylightSaving() {
        val ny = ZoneId.of("America/New_York")
        // US clocks go forward on 2026-03-08. Start Saturday 09:00 local.
        val s = LocalDateTime.of(2026, 3, 7, 9, 0).atZone(ny).toInstant()
        val got = times(loop(day(1)), ny, s.minusSeconds(1), s, 3).map { it.atZone(ny).toLocalDateTime() }
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 3, 7, 9, 0),
                LocalDateTime.of(2026, 3, 8, 9, 0),
                LocalDateTime.of(2026, 3, 9, 9, 0)
            ),
            got
        )
    }

    @Test
    fun twentyFourHoursIsAnExactDurationAcrossDaylightSaving() {
        val ny = ZoneId.of("America/New_York")
        val s = LocalDateTime.of(2026, 3, 7, 9, 0).atZone(ny).toInstant()
        val got = times(loop(hr(24)), ny, s.minusSeconds(1), s, 3)
        assertEquals(Duration.ofHours(24), Duration.between(got[0], got[1]))
        assertEquals(Duration.ofHours(24), Duration.between(got[1], got[2]))
        // After the spring-forward the wall clock reads 10:00, not 09:00.
        assertEquals(10, got[2].atZone(ny).hour)
    }

    @Test
    fun farFutureLookupMatchesStepByStepWalk() {
        val rnd = Random(42)
        val zones = listOf(utc, ZoneId.of("America/New_York"), ZoneId.of("Asia/Kolkata"), ZoneId.of("Europe/London"))
        val recs = listOf(
            loop(min(10)),
            loop(min(10), hr(1), hr(12)),
            loop(day(2)),
            loop(day(1), min(10)),
            loop(week(1), hr(5), day(3)),
            loop(hr(7), min(45), day(1), min(5))
        )
        for (zone in zones) for (rec in recs) {
            val s = ZonedDateTime.of(2026, 2, 25, 13, 35, 0, 0, zone).toInstant()
            // brute force walk of the first 4000 occurrences
            val walk = (0L until 4000L).map { RecurrenceEngine.occurrenceAt(s, zone, rec, it)!!.time }
            for (i in 1 until walk.size) assertTrue("monotonic $zone ${rec.describeShort()} at $i", walk[i].isAfter(walk[i - 1]))
            repeat(60) {
                val pick = walk[rnd.nextInt(3990) + 2].plusMillis(rnd.nextInt(5) - 2L)
                val expectedLast = walk.indexOfLast { !it.isAfter(pick) }.toLong()
                val actual = RecurrenceEngine.lastIndexAtOrBefore(s, zone, rec, pick)
                assertEquals("$zone ${rec.describeShort()} at $pick", expectedLast, actual)
                assertEquals(walk[(expectedLast + 1).toInt()], RecurrenceEngine.nextAfter(s, zone, rec, pick)?.time)
            }
        }
    }

    @Test
    fun veryDistantStartIsFast() {
        val longAgo = Instant.parse("2001-01-01T00:00:00Z")
        val now = Instant.parse("2026-10-14T07:00:00Z")
        val t0 = System.nanoTime()
        val next = RecurrenceEngine.nextAfter(longAgo, utc, loop(min(10)), now)
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertNotNull(next)
        assertTrue(next!!.time.isAfter(now))
        assertTrue(Duration.between(now, next.time) <= Duration.ofMinutes(10))
        assertTrue("took ${ms}ms", ms < 1000)
    }

    @Test
    fun upcomingReturnsRequestedCountInOrder() {
        val rec = loop(min(10), hr(1), hr(12))
        val list = RecurrenceEngine.upcoming(start, utc, rec, Instant.parse("2026-01-03T00:00:00Z"), 8)
        assertEquals(8, list.size)
        for (i in 1 until list.size) {
            assertTrue(list[i].time.isAfter(list[i - 1].time))
            assertEquals(list[i - 1].index + 1, list[i].index)
        }
        assertTrue(list.first().time.isAfter(Instant.parse("2026-01-03T00:00:00Z")))
        assertTrue(RecurrenceEngine.upcoming(start, utc, rec, start, 0).isEmpty())
    }

    @Test
    fun descriptionsAreReadable() {
        assertEquals("Once", Recurrence.ONCE.describe())
        assertEquals("Every hour", loop(hr(1)).describe())
        assertEquals("Every 45 minutes", loop(min(45)).describe())
        assertEquals("10 minutes → 1 hour → 12 hours, repeating", loop(min(10), hr(1), hr(12)).describe())
        assertEquals("10 minutes → 1 hour, then stops", once(min(10), hr(1)).describe())
        assertEquals("10m → 1h → 12h ↻", loop(min(10), hr(1), hr(12)).describeShort())
    }

    @Test
    fun invalidRecurrencesAreRejected() {
        assertFalse(runCatching { Stage(0, IntervalUnit.MINUTES) }.isSuccess)
        assertFalse(runCatching { Stage(Stage.MAX_AMOUNT + 1, IntervalUnit.DAYS) }.isSuccess)
        assertFalse(runCatching { Recurrence(emptyList(), loop = true) }.isSuccess)
        assertFalse(runCatching { Recurrence(List(Recurrence.MAX_STAGES + 1) { Stage(1, IntervalUnit.HOURS) }, true) }.isSuccess)
    }

    @Test
    fun hugeIndexReturnsNullInsteadOfThrowing() {
        val rec = loop(week(9999))
        assertNull(RecurrenceEngine.occurrenceAt(start, utc, rec, Long.MAX_VALUE / 4))
    }
}
