package com.rememberflow.app.core

/** Unit of one recurrence stage. Minutes/hours are exact durations; days/weeks follow the local calendar. */
enum class IntervalUnit(val shortLabel: String, private val singular: String) {
    MINUTES("m", "minute"),
    HOURS("h", "hour"),
    DAYS("d", "day"),
    WEEKS("w", "week");

    /** Days and weeks keep the local wall-clock time across daylight-saving changes. */
    val isCalendarBased: Boolean get() = this == DAYS || this == WEEKS

    fun word(amount: Int): String = if (amount == 1) singular else singular + "s"
}

/** One gap in a sequence: "wait [amount] [unit] until the next occurrence". */
data class Stage(val amount: Int, val unit: IntervalUnit) {
    init {
        require(amount in 1..MAX_AMOUNT) { "Stage amount must be between 1 and $MAX_AMOUNT" }
    }

    /** "10 minutes", "1 hour", "2 days". */
    fun label(): String = "$amount ${unit.word(amount)}"

    /** "10m", "1h", "2d", "1w". */
    fun shortLabel(): String = "$amount${unit.shortLabel}"

    companion object {
        const val MAX_AMOUNT = 9999
    }
}

/**
 * A repeating schedule described as a list of gaps between occurrences.
 *
 * Occurrence 0 is the chosen start. Occurrence k is reached after waiting the first k gaps.
 * With [loop] the list repeats forever (10 min, 1 h, 12 h, 10 min, 1 h, ...); without it the
 * sequence ends after the last gap. An empty list is a one-time reminder.
 */
data class Recurrence(val stages: List<Stage>, val loop: Boolean) {
    init {
        require(stages.size <= MAX_STAGES) { "At most $MAX_STAGES stages are supported" }
        require(!(loop && stages.isEmpty())) { "A looping recurrence needs at least one stage" }
    }

    val isOneTime: Boolean get() = stages.isEmpty()

    /** Matching preset, if this recurrence is exactly one of the built-in presets. */
    fun matchingPreset(): RecurrencePreset? = PRESETS.firstOrNull { it.recurrence == this }

    /** Human-readable summary such as "Every hour" or "10 minutes → 1 hour → 12 hours, repeating". */
    fun describe(): String {
        if (isOneTime) return "Once"
        matchingPreset()?.let { return it.label }
        if (stages.size == 1 && loop) return "Every ${stages[0].label()}"
        val chain = stages.joinToString(" → ") { it.label() }
        return if (loop) "$chain, repeating" else "$chain, then stops"
    }

    /** Compact form for cards: "10m → 1h → 12h ↻". */
    fun describeShort(): String {
        if (isOneTime) return "Once"
        val chain = stages.joinToString(" → ") { it.shortLabel() }
        return if (loop) "$chain ↻" else chain
    }

    companion object {
        const val MAX_STAGES = 12

        val ONCE = Recurrence(emptyList(), loop = false)

        val PRESETS: List<RecurrencePreset> = listOf(
            RecurrencePreset("every_10_min", "Every 10 minutes", Recurrence(listOf(Stage(10, IntervalUnit.MINUTES)), true)),
            RecurrencePreset("every_hour", "Every hour", Recurrence(listOf(Stage(1, IntervalUnit.HOURS)), true)),
            RecurrencePreset("every_12_hours", "Every 12 hours", Recurrence(listOf(Stage(12, IntervalUnit.HOURS)), true)),
            RecurrencePreset("every_24_hours", "Every 24 hours", Recurrence(listOf(Stage(24, IntervalUnit.HOURS)), true)),
            RecurrencePreset("every_other_day", "Every other day", Recurrence(listOf(Stage(2, IntervalUnit.DAYS)), true)),
            RecurrencePreset("every_3_days", "Every 3 days", Recurrence(listOf(Stage(3, IntervalUnit.DAYS)), true)),
            RecurrencePreset("every_5_days", "Every 5 days", Recurrence(listOf(Stage(5, IntervalUnit.DAYS)), true)),
            RecurrencePreset("every_week", "Every week", Recurrence(listOf(Stage(1, IntervalUnit.WEEKS)), true))
        )
    }
}

data class RecurrencePreset(val key: String, val label: String, val recurrence: Recurrence)
