package com.rememberflow.app.core

/**
 * Compact text form used for storage and backups: "L:10m,1h,12h" (looping) or "S:10m,1h" (stops
 * after the last stage). An empty string means a one-time reminder.
 */
object RecurrenceCodec {

    fun encode(recurrence: Recurrence): String {
        if (recurrence.isOneTime) return ""
        val prefix = if (recurrence.loop) "L" else "S"
        return prefix + ":" + recurrence.stages.joinToString(",") { it.shortLabel() }
    }

    /** Returns [Recurrence.ONCE] for blank or malformed input rather than throwing. */
    fun decode(text: String?): Recurrence {
        if (text.isNullOrBlank()) return Recurrence.ONCE
        val colon = text.indexOf(':')
        if (colon != 1) return Recurrence.ONCE
        val loop = when (text[0]) {
            'L' -> true
            'S' -> false
            else -> return Recurrence.ONCE
        }
        val stages = text.substring(2).split(',').map { part ->
            val unit = when (part.lastOrNull()) {
                'm' -> IntervalUnit.MINUTES
                'h' -> IntervalUnit.HOURS
                'd' -> IntervalUnit.DAYS
                'w' -> IntervalUnit.WEEKS
                else -> return Recurrence.ONCE
            }
            val amount = part.dropLast(1).toIntOrNull() ?: return Recurrence.ONCE
            if (amount !in 1..Stage.MAX_AMOUNT) return Recurrence.ONCE
            Stage(amount, unit)
        }
        if (stages.isEmpty() || stages.size > Recurrence.MAX_STAGES) return Recurrence.ONCE
        return Recurrence(stages, loop)
    }
}
