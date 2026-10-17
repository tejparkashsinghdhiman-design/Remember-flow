package com.rememberflow.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rememberflow.app.core.IntervalUnit
import com.rememberflow.app.core.Recurrence
import com.rememberflow.app.core.RecurrenceEngine
import com.rememberflow.app.core.Reminder
import com.rememberflow.app.core.Stage
import com.rememberflow.app.ui.ReminderDraft
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class RepeatMode { ONCE, PRESET, CUSTOM }

/** One editable row of the custom sequence. [amount] is raw text so half-typed input stays valid. */
data class StageDraft(val id: Long, val amount: String, val unit: IntervalUnit)

/** All the editor's mutable state, kept out of the composables so the rules are easy to read. */
class EditorState(initial: Reminder?, private val zone: ZoneId = ZoneId.systemDefault()) {

    private var nextStageId = 0L

    var title by mutableStateOf(initial?.title ?: "")
    var details by mutableStateOf(initial?.details ?: "")
    var date by mutableStateOf(LocalDate.now(zone))
    var time by mutableStateOf(LocalTime.now(zone))
    var mode by mutableStateOf(RepeatMode.ONCE)
    var presetKey by mutableStateOf(Recurrence.PRESETS.first().key)
    var loop by mutableStateOf(true)
    val stages = mutableStateListOf<StageDraft>()

    init {
        if (initial == null) {
            val start = LocalDateTime.now(zone).plusMinutes(10).withSecond(0).withNano(0)
            date = start.toLocalDate()
            time = start.toLocalTime()
        } else {
            val start = initial.start.atZone(zone).toLocalDateTime()
            date = start.toLocalDate()
            time = start.toLocalTime().withNano(0)
            val rec = initial.recurrence
            val preset = rec.matchingPreset()
            when {
                rec.isOneTime -> mode = RepeatMode.ONCE
                preset != null -> {
                    mode = RepeatMode.PRESET
                    presetKey = preset.key
                }
                else -> {
                    mode = RepeatMode.CUSTOM
                    loadCustom(rec)
                }
            }
        }
        if (stages.isEmpty()) loadTemplate()
    }

    val startInstant: Instant
        get() = LocalDateTime.of(date, time.withSecond(0).withNano(0)).atZone(zone).toInstant()

    /** The recurrence the current inputs describe, or null (with [recurrenceError]) when incomplete. */
    val recurrence: Recurrence?
        get() = when (mode) {
            RepeatMode.ONCE -> Recurrence.ONCE
            RepeatMode.PRESET -> Recurrence.PRESETS.firstOrNull { it.key == presetKey }?.recurrence
            RepeatMode.CUSTOM -> customRecurrence()
        }

    val recurrenceError: String?
        get() {
            if (mode != RepeatMode.CUSTOM) return null
            if (stages.isEmpty()) return "Add at least one stage."
            stages.forEachIndexed { i, s ->
                val n = s.amount.toIntOrNull()
                if (n == null || n < 1) return "Stage ${i + 1} needs a number of at least 1."
                if (n > Stage.MAX_AMOUNT) return "Stage ${i + 1} is too large (max ${Stage.MAX_AMOUNT})."
            }
            return null
        }

    /** Why Save is unavailable, or null when the draft can be saved. */
    fun validate(now: Instant): String? {
        if (title.isBlank() && details.isBlank()) return "Write something you want to remember."
        recurrenceError?.let { return it }
        val rec = recurrence ?: return "Choose how it repeats."
        if (rec.isOneTime && !startInstant.isAfter(now)) return "Pick a time in the future for a one-time reminder."
        return null
    }

    fun toDraft(id: Long?): ReminderDraft? {
        val rec = recurrence ?: return null
        return ReminderDraft(
            id = id,
            title = title.ifBlank { details.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty() },
            details = if (title.isBlank()) "" else details,
            start = startInstant,
            zone = zone,
            recurrence = rec
        )
    }

    /** Up to [count] upcoming occurrences with the gap from the previous one, for the live preview. */
    fun preview(now: Instant, count: Int = 8): List<PreviewRow> {
        val rec = recurrence ?: return emptyList()
        if (rec.isOneTime && !startInstant.isAfter(now)) return emptyList()
        val list = RecurrenceEngine.upcoming(startInstant, zone, rec, now, count)
        return list.mapIndexed { i, occ ->
            val previous = if (i > 0) list[i - 1].time
            else RecurrenceEngine.occurrenceAt(startInstant, zone, rec, occ.index - 1)?.time
            PreviewRow(occ.index, occ.time, previous?.let { Duration.between(it, occ.time) })
        }
    }

    // --- custom sequence editing ----------------------------------------------------------

    fun addStage() {
        if (stages.size >= Recurrence.MAX_STAGES) return
        stages.add(StageDraft(nextStageId++, "1", IntervalUnit.HOURS))
    }

    fun removeStage(index: Int) {
        if (index in stages.indices) stages.removeAt(index)
    }

    fun moveStage(index: Int, delta: Int) {
        val target = index + delta
        if (index !in stages.indices || target !in stages.indices) return
        val item = stages.removeAt(index)
        stages.add(target, item)
    }

    fun updateStage(index: Int, amount: String? = null, unit: IntervalUnit? = null) {
        val current = stages.getOrNull(index) ?: return
        stages[index] = current.copy(
            amount = amount?.filter { it.isDigit() }?.take(4) ?: current.amount,
            unit = unit ?: current.unit
        )
    }

    /** Fills the editor with the 10 minutes, 1 hour, 12 hours example, repeating. */
    fun loadTemplate() {
        stages.clear()
        stages.add(StageDraft(nextStageId++, "10", IntervalUnit.MINUTES))
        stages.add(StageDraft(nextStageId++, "1", IntervalUnit.HOURS))
        stages.add(StageDraft(nextStageId++, "12", IntervalUnit.HOURS))
        loop = true
    }

    private fun loadCustom(rec: Recurrence) {
        stages.clear()
        rec.stages.forEach { stages.add(StageDraft(nextStageId++, it.amount.toString(), it.unit)) }
        loop = rec.loop
    }

    private fun customRecurrence(): Recurrence? {
        if (recurrenceError != null) return null
        return Recurrence(stages.map { Stage(it.amount.toInt(), it.unit) }, loop)
    }

    // --- start-time helpers -----------------------------------------------------------------

    fun setStartFromNowPlus(minutes: Long) {
        val start = LocalDateTime.now(zone).plusMinutes(minutes).withSecond(0).withNano(0)
        date = start.toLocalDate()
        time = start.toLocalTime()
    }

    fun setStartTomorrowAt(hour: Int, minute: Int) {
        date = LocalDate.now(zone).plusDays(1)
        time = LocalTime.of(hour, minute)
    }
}

data class PreviewRow(val index: Long, val time: Instant, val gap: Duration?)
