package com.rememberflow.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.time.Instant

/** How precisely the next alarm could be scheduled. */
enum class AlarmPrecision {
    /** Exact alarm: fires at the chosen second, even in Doze. */
    EXACT,

    /** Inexact fallback because the "Alarms & reminders" permission is not granted: may be late by minutes. */
    INEXACT
}

/**
 * Thin wrapper over [AlarmManager]. One alarm per reminder, always for that reminder's *next*
 * occurrence only; the receiver schedules the following one after it fires. Alarms survive the app
 * being closed but not a reboot (see [BootReceiver]) or a force-stop.
 */
class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** True when exact alarms may be scheduled (always on Android 11 and older; permission-gated after). */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Schedules (replacing any earlier alarm for this id) or, when [at] is null, cancels. */
    fun schedule(reminderId: Long, at: Instant?): AlarmPrecision? {
        if (at == null) {
            cancel(reminderId)
            return null
        }
        val pending = pendingIntent(reminderId)
        val millis = at.toEpochMilli()
        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
                return AlarmPrecision.EXACT
            } catch (_: SecurityException) {
                // Permission was revoked between the check and the call; fall through to inexact.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        return AlarmPrecision.INEXACT
    }

    fun cancel(reminderId: Long) {
        alarmManager.cancel(pendingIntent(reminderId))
    }

    private fun pendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            data = Uri.parse("rememberflow://reminder/$reminderId")
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
