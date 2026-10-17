package com.rememberflow.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms every alarm after events that silently drop or invalidate them: a reboot, an app update,
 * a clock or time-zone change, or the exact-alarm permission being granted or revoked.
 * Anything that came due in the meantime is delivered as a "missed" notification.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED -> ReminderCoordinator.processDue(context)
        }
    }

    companion object {
        // android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31+).
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
