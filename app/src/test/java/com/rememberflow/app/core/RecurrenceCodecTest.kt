package com.rememberflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RecurrenceCodecTest {

    @Test
    fun everyPresetSurvivesARoundTrip() {
        for (p in Recurrence.PRESETS) {
            assertEquals(p.key, p.recurrence, RecurrenceCodec.decode(RecurrenceCodec.encode(p.recurrence)))
        }
    }

    @Test
    fun customSequenceRoundTrips() {
        val rec = Recurrence(
            listOf(Stage(10, IntervalUnit.MINUTES), Stage(1, IntervalUnit.HOURS), Stage(12, IntervalUnit.HOURS), Stage(2, IntervalUnit.WEEKS)),
            loop = true
        )
        assertEquals("L:10m,1h,12h,2w", RecurrenceCodec.encode(rec))
        assertEquals(rec, RecurrenceCodec.decode("L:10m,1h,12h,2w"))
        val finite = rec.copy(loop = false)
        assertEquals("S:10m,1h,12h,2w", RecurrenceCodec.encode(finite))
        assertEquals(finite, RecurrenceCodec.decode("S:10m,1h,12h,2w"))
    }

    @Test
    fun oneTimeIsAnEmptyString() {
        assertEquals("", RecurrenceCodec.encode(Recurrence.ONCE))
        assertEquals(Recurrence.ONCE, RecurrenceCodec.decode(""))
        assertEquals(Recurrence.ONCE, RecurrenceCodec.decode(null))
    }

    @Test
    fun malformedInputFallsBackToOnceInsteadOfCrashing() {
        for (bad in listOf("x", "L:", "L:10", "L:10x", "L:0m", "L:-5m", "Q:10m", "L:10m,,1h", "L:99999m", "L:abc")) {
            assertEquals(bad, Recurrence.ONCE, RecurrenceCodec.decode(bad))
        }
        val tooMany = "L:" + List(Recurrence.MAX_STAGES + 1) { "1h" }.joinToString(",")
        assertEquals(Recurrence.ONCE, RecurrenceCodec.decode(tooMany))
    }
}
