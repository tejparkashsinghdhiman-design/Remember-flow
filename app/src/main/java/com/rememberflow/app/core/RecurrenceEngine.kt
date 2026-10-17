package com.rememberflow.app.core

import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A single scheduled moment: [index] 0 is the chosen start. */
data class Occurrence(val index: Long, val time: Instant)

/**
 * Pure date maths for [Recurrence]. No Android classes, so it is fully unit-testable.
 *
 * Minute and hour stages are exact durations. Day and week stages are calendar steps in [zone], so
 * "every 2 days at 09:00" stays at 09:00 across daylight-saving changes, while "every 24 hours"
 * drifts by an hour, exactly as the names say.
 *
 * Every occurrence is derived from its index alone ([occurrenceAt]), which keeps results consistent
 * no matter how the sequence is walked and lets far-future lookups skip whole cycles.
 */
object RecurrenceEngine {

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val DST_SLACK_MILLIS = 4L * 3_600_000L

    /** The occurrence with this index, or null if the sequence has no such occurrence. */
    fun occurrenceAt(start: Instant, zone: ZoneId, recurrence: Recurrence, index: Long): Occurrence? {
        if (index < 0) return null
        val stages = recurrence.stages
        val n = stages.size
        if (n == 0) return if (index == 0L) Occurrence(0, start) else null
        if (!recurrence.loop && index > n) return null
        return try {
            val cycle = if (recurrence.loop) index / n else 0L
            val offset = if (recurrence.loop) (index % n).toInt() else index.toInt()
            var days = 0L
            var fixedMillis = 0L
            for (i in 0 until offset) {
                val s = stages[i % n]
                when (s.unit) {
                    IntervalUnit.MINUTES -> fixedMillis += s.amount * 60_000L
                    IntervalUnit.HOURS -> fixedMillis += s.amount * 3_600_000L
                    IntervalUnit.DAYS -> days += s.amount.toLong()
                    IntervalUnit.WEEKS -> days += s.amount * 7L
                }
            }
            if (cycle > 0) {
                val (cycleFixed, cycleDays) = cycleTotals(stages)
                days = Math.addExact(days, Math.multiplyExact(cycle, cycleDays))
                fixedMillis = Math.addExact(fixedMillis, Math.multiplyExact(cycle, cycleFixed))
            }
            // Calendar days first (wall-clock preserving), then exact durations. Both only ever grow
            // with the index, so occurrences are strictly increasing.
            var t = start.atZone(zone)
            if (days > 0) t = t.plusDays(days)
            if (fixedMillis > 0) t = t.plus(Duration.ofMillis(fixedMillis))
            Occurrence(index, t.toInstant())
        } catch (_: DateTimeException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Highest occurrence index whose time is at or before [at]; -1 if [at] is before the start. */
    fun lastIndexAtOrBefore(start: Instant, zone: ZoneId, recurrence: Recurrence, at: Instant): Long {
        if (at.isBefore(start)) return -1L
        val stages = recurrence.stages
        val n = stages.size
        if (n == 0) return 0L
        if (!recurrence.loop) {
            var last = -1L
            for (i in 0..n) {
                val occ = occurrenceAt(start, zone, recurrence, i.toLong()) ?: break
                if (occ.time.isAfter(at)) break
                last = i.toLong()
            }
            return last
        }
        val (fixedMillis, calendarDays) = cycleTotals(stages)
        val nominalCycle = fixedMillis + calendarDays * MILLIS_PER_DAY
        val delta = at.toEpochMilli() - start.toEpochMilli()
        val slack = if (calendarDays > 0) DST_SLACK_MILLIS else 0L
        var cycle = maxOf(0L, (delta - slack) / nominalCycle)
        while (cycle > 0) {
            val first = occurrenceAt(start, zone, recurrence, cycle * n)
            if (first == null || first.time.isAfter(at)) cycle-- else break
        }
        var index = cycle * n
        var last = -1L
        while (true) {
            val occ = occurrenceAt(start, zone, recurrence, index) ?: break
            if (occ.time.isAfter(at)) break
            last = index
            index++
        }
        return if (last < 0) fallbackScan(start, zone, recurrence, at) else last
    }

    /** First occurrence strictly after [at], or null if the sequence has ended. */
    fun nextAfter(start: Instant, zone: ZoneId, recurrence: Recurrence, at: Instant): Occurrence? {
        val last = lastIndexAtOrBefore(start, zone, recurrence, at)
        return occurrenceAt(start, zone, recurrence, last + 1)
    }

    /** Up to [count] occurrences strictly after [from], in order. */
    fun upcoming(start: Instant, zone: ZoneId, recurrence: Recurrence, from: Instant, count: Int): List<Occurrence> {
        if (count <= 0) return emptyList()
        val result = ArrayList<Occurrence>(count)
        var index = lastIndexAtOrBefore(start, zone, recurrence, from) + 1
        while (result.size < count) {
            val occ = occurrenceAt(start, zone, recurrence, index) ?: break
            result.add(occ)
            index++
        }
        return result
    }

    /** Total number of occurrences in a finite sequence, or null when it loops forever. */
    fun totalOccurrences(recurrence: Recurrence): Long? =
        if (recurrence.loop) null else recurrence.stages.size + 1L

    // --- internals -------------------------------------------------------------------------

    private fun cycleTotals(stages: List<Stage>): Pair<Long, Long> {
        var fixedMillis = 0L
        var calendarDays = 0L
        for (s in stages) {
            when (s.unit) {
                IntervalUnit.MINUTES -> fixedMillis += s.amount * 60_000L
                IntervalUnit.HOURS -> fixedMillis += s.amount * 3_600_000L
                IntervalUnit.DAYS -> calendarDays += s.amount.toLong()
                IntervalUnit.WEEKS -> calendarDays += s.amount * 7L
            }
        }
        return fixedMillis to calendarDays
    }

    /** Safety net: linear scan from index 0 (only reachable if the jump lands past every candidate). */
    private fun fallbackScan(start: Instant, zone: ZoneId, recurrence: Recurrence, at: Instant): Long {
        var index = 0L
        var last = -1L
        while (index < 100_000L) {
            val occ = occurrenceAt(start, zone, recurrence, index) ?: break
            if (occ.time.isAfter(at)) break
            last = index
            index++
        }
        return last
    }
}
