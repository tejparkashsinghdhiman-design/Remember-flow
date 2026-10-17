package com.rememberflow.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives the per-reminder alarm and the notification's "Pause" action. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE -> {
                val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (id >= 0) ReminderCoordinator.pause(context, id)
            }
            else -> ReminderCoordinator.processDue(context)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.rememberflow.app.action.FIRE"
        const val ACTION_PAUSE = "com.rememberflow.app.action.PAUSE"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
