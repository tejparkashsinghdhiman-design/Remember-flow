package com.rememberflow.app.ui.editor

import com.rememberflow.app.core.IntervalUnit
import com.rememberflow.app.core.Recurrence
import com.rememberflow.app.core.Reminder
import com.rememberflow.app.core.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class EditorStateTest {

    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun newState(): EditorState = EditorState(initial = null, zone = zone).apply {
        date = LocalDate.of(2026, 1, 2)
        time = LocalTime.of(9, 0)
    }

    @Test
    fun emptyDraftCannotBeSaved() {
        val state = newState()
        assertEquals("Write something you want to remember.", state.validate(now))
        state.title = "Call the bank"
        assertNull(state.validate(now))
    }

    @Test
    fun oneTimeReminderMustBeInTheFuture() {
        val state = newState().apply { title = "x" }
        state.date = LocalDate.of(2025, 12, 31)
        assertEquals("Pick a time in the future for a one-time reminder.", state.validate(now))
        state.mode = RepeatMode.PRESET
        assertNull("repeating reminders may be anchored in the past", state.validate(now))
    }

    @Test
    fun presetModeUsesTheChosenPreset() {
        val state = newState().apply { title = "x"; mode = RepeatMode.PRESET; presetKey = "every_5_days" }
        assertEquals(Recurrence.PRESETS.first { it.key == "every_5_days" }.recurrence, state.recurrence)
    }

    @Test
    fun defaultCustomSequenceIsTenMinutesOneHourTwelveHoursRepeating() {
        val state = newState().apply { title = "x"; mode = RepeatMode.CUSTOM }
        assertEquals(
            Recurrence(
                listOf(Stage(10, IntervalUnit.MINUTES), Stage(1, IntervalUnit.HOURS), Stage(12, IntervalUnit.HOURS)),
                loop = true
            ),
            state.recurrence
        )
    }

    @Test
    fun stagesCanBeAddedEditedReorderedAndDeleted() {
        val state = newState().apply { mode = RepeatMode.CUSTOM }
        state.addStage() // 4th stage: 1 hour
        assertEquals(4, state.stages.size)

        state.updateStage(3, amount = "2x9", unit = IntervalUnit.DAYS) // non-digits are dropped
        assertEquals("29", state.stages[3].amount)
        assertEquals(IntervalUnit.DAYS, state.stages[3].unit)

        state.moveStage(3, -3) // to the front
        assertEquals(listOf("29", "10", "1", "12"), state.stages.map { it.amount })
        state.moveStage(0, -1) // already first: no change
        assertEquals("29", state.stages[0].amount)
        state.moveStage(3, +1) // already last: no change
        assertEquals("12", state.stages[3].amount)

        state.removeStage(0)
        assertEquals(listOf("10", "1", "12"), state.stages.map { it.amount })
        assertEquals(
            listOf(Stage(10, IntervalUnit.MINUTES), Stage(1, IntervalUnit.HOURS), Stage(12, IntervalUnit.HOURS)),
            state.recurrence?.stages
        )
    }

    @Test
    fun invalidStagesBlockSavingWithAClearMessage() {
        val state = newState().apply { title = "x"; mode = RepeatMode.CUSTOM }
        state.updateStage(1, amount = "")
        assertEquals("Stage 2 needs a number of at least 1.", state.validate(now))
        assertNull(state.recurrence)

        state.updateStage(1, amount = "0")
        assertEquals("Stage 2 needs a number of at least 1.", state.validate(now))

        state.updateStage(1, amount = "5")
        assertNull(state.validate(now))

        while (state.stages.isNotEmpty()) state.removeStage(0)
        assertEquals("Add at least one stage.", state.validate(now))
    }

    @Test
    fun stageCountIsCapped() {
        val state = newState().apply { mode = RepeatMode.CUSTOM }
        repeat(20) { state.addStage() }
        assertEquals(Recurrence.MAX_STAGES, state.stages.size)
    }

    @Test
    fun loopOffMakesTheSequenceFinite() {
        val state = newState().apply { title = "x"; mode = RepeatMode.CUSTOM; loop = false }
        val rec = state.recurrence
        assertNotNull(rec)
        assertEquals(false, rec!!.loop)
        val preview = state.preview(now)
        assertEquals(4, preview.size) // start + three gaps, then it stops
    }

    @Test
    fun previewShowsTheNextOccurrencesWithTheirGaps() {
        val state = newState().apply { title = "x"; mode = RepeatMode.CUSTOM }
        val preview = state.preview(now, count = 5)
        assertEquals(5, preview.size)
        assertEquals(Instant.parse("2026-01-02T09:00:00Z"), preview[0].time)
        assertEquals(Instant.parse("2026-01-02T09:10:00Z"), preview[1].time)
        assertEquals(Duration.ofMinutes(10), preview[1].gap)
        assertEquals(Instant.parse("2026-01-02T10:10:00Z"), preview[2].time)
        assertEquals(Duration.ofHours(1), preview[2].gap)
        assertEquals(Instant.parse("2026-01-02T22:10:00Z"), preview[3].time)
        assertEquals(Duration.ofHours(12), preview[3].gap)
        assertEquals(Instant.parse("2026-01-02T22:20:00Z"), preview[4].time)
        assertEquals(Duration.ofMinutes(10), preview[4].gap)
    }

    @Test
    fun draftCarriesStartRecurrenceAndFallsBackToNotesWhenTitleIsBlank() {
        val state = newState().apply { details = "Bring the documents\nand the form"; mode = RepeatMode.PRESET }
        val draft = state.toDraft(id = null)
        assertNotNull(draft)
        assertEquals("Bring the documents", draft!!.title)
        assertEquals("", draft.details)
        assertEquals(Instant.parse("2026-01-02T09:00:00Z"), draft.start)
        assertTrue(draft.recurrence.loop)
    }

    @Test
    fun editingAnExistingCustomReminderRestoresItsStagesInOrder() {
        val custom = Recurrence(
            listOf(Stage(3, IntervalUnit.DAYS), Stage(30, IntervalUnit.MINUTES)),
            loop = false
        )
        val reminder = Reminder(
            id = 7, title = "Water plants", details = "balcony", startMillis = Instant.parse("2026-03-04T06:30:00Z").toEpochMilli(),
            zoneId = "UTC", recurrence = custom, paused = false, createdAtMillis = 0, lastHandledIndex = -1
        )
        val state = EditorState(reminder, zone)
        assertEquals(RepeatMode.CUSTOM, state.mode)
        assertEquals(false, state.loop)
        assertEquals(listOf("3" to IntervalUnit.DAYS, "30" to IntervalUnit.MINUTES), state.stages.map { it.amount to it.unit })
        assertEquals(LocalDate.of(2026, 3, 4), state.date)
        assertEquals(LocalTime.of(6, 30), state.time)
        assertEquals(custom, state.recurrence)
    }

    @Test
    fun editingAPresetReminderSelectsThatPreset() {
        val preset = Recurrence.PRESETS.first { it.key == "every_12_hours" }
        val reminder = Reminder(
            id = 1, title = "Vitamins", details = "", startMillis = 0, zoneId = "UTC",
            recurrence = preset.recurrence, paused = false, createdAtMillis = 0, lastHandledIndex = 0
        )
        val state = EditorState(reminder, zone)
        assertEquals(RepeatMode.PRESET, state.mode)
        assertEquals("every_12_hours", state.presetKey)
    }
}
