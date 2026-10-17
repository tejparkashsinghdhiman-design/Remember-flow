package com.rememberflow.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rememberflow.app.MainActivity
import com.rememberflow.app.R
import com.rememberflow.app.core.Reminder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {

    const val CHANNEL_ID = "reminders_v1"

    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dayTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm:ss")

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            enableLights(true)
            lightColor = Color.parseColor("#8B5CF6")
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Posts the notification for [reminder]. [dueCount] occurrences were due; when more than one, or
     * when the one due was [late], the text says so honestly instead of pretending it was on time.
     * Returns false when the system refused (notifications disabled).
     */
    fun post(
        context: Context,
        reminder: Reminder,
        dueTime: Instant,
        dueCount: Long,
        late: Boolean,
        zone: ZoneId
    ): Boolean {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        val id = reminder.id.toInt()
        val dueLocal = dueTime.atZone(zone)
        val timeText = dueLocal.format(timeFormat)

        val headline = reminder.title.ifBlank { context.getString(R.string.untitled_reminder) }
        val body = StringBuilder()
        if (reminder.details.isNotBlank()) body.append(reminder.details).append('\n')
        when {
            dueCount > 1 -> body.append(
                context.getString(R.string.notif_missed_many, dueCount.toInt(), dueLocal.format(dayTimeFormat))
            )
            late -> body.append(context.getString(R.string.notif_late, dueLocal.format(dayTimeFormat)))
            else -> body.append(reminder.recurrence.describe()).append(" · ").append(timeText)
        }

        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = Uri.parse("rememberflow://open/${reminder.id}")
                putExtra(MainActivity.EXTRA_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_flow)
            .setColor(Color.parseColor("#8B5CF6"))
            .setContentTitle(headline)
            .setContentText(body.toString().lineSequence().firstOrNull { it.isNotBlank() } ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.toString()))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setWhen(dueTime.toEpochMilli())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(open)
            .apply {
                if (!reminder.recurrence.isOneTime) {
                    addAction(0, context.getString(R.string.action_pause), pauseIntent(context, reminder.id))
                }
            }
            .build()

        return try {
            manager.notify(id, notification)
            true
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked on Android 13+.
            false
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    private fun pauseIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PAUSE
            data = Uri.parse("rememberflow://pause/$reminderId")
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
