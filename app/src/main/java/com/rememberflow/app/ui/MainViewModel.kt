package com.rememberflow.app.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rememberflow.app.core.Recurrence
import com.rememberflow.app.core.Reminder
import com.rememberflow.app.core.ReminderFilter
import com.rememberflow.app.core.ReminderPlanner
import com.rememberflow.app.data.ReminderRepository
import com.rememberflow.app.notify.NotificationHelper
import com.rememberflow.app.notify.ReminderCoordinator
import com.rememberflow.app.notify.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** What the editor hands back when the user presses Save. [id] is null for a new reminder. */
data class ReminderDraft(
    val id: Long?,
    val title: String,
    val details: String,
    val start: Instant,
    val zone: ZoneId,
    val recurrence: Recurrence
)

/** The permissions and system settings that decide whether reminders can actually arrive. */
data class SystemStatus(
    val notificationPermissionGranted: Boolean,
    val notificationsEnabled: Boolean,
    val exactAlarmsAllowed: Boolean,
    val ignoringBatteryOptimizations: Boolean
) {
    val notificationsOk: Boolean get() = notificationPermissionGranted && notificationsEnabled
    val allGood: Boolean get() = notificationsOk && exactAlarmsAllowed
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ReminderRepository.get(app)

    val reminders: StateFlow<List<Reminder>> = repo.reminders

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(ReminderFilter.ALL)
    val filter: StateFlow<ReminderFilter> = _filter.asStateFlow()

    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<SystemStatus> = _status.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setFilter(value: ReminderFilter) {
        _filter.value = value
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** Called when the app returns to the foreground: re-reads permissions and delivers anything overdue. */
    fun onForeground() {
        _status.value = readStatus()
        viewModelScope.launch(Dispatchers.IO) {
            ReminderCoordinator.processDue(getApplication())
            repo.refresh()
        }
    }

    fun refreshStatus() {
        _status.value = readStatus()
    }

    /** Creates or updates a reminder and arms its next alarm. Returns after the work is queued. */
    fun save(draft: ReminderDraft) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val now = Instant.now()
            val handled = ReminderPlanner.handledIndexFor(draft.start, draft.zone, draft.recurrence, now)
            val existing = draft.id?.let { repo.get(it) }
            val saved = if (existing == null) {
                repo.insert(
                    Reminder(
                        id = 0,
                        title = draft.title.trim(),
                        details = draft.details.trim(),
                        startMillis = draft.start.toEpochMilli(),
                        zoneId = draft.zone.id,
                        recurrence = draft.recurrence,
                        paused = false,
                        createdAtMillis = now.toEpochMilli(),
                        lastHandledIndex = handled
                    )
                )
            } else {
                existing.copy(
                    title = draft.title.trim(),
                    details = draft.details.trim(),
                    startMillis = draft.start.toEpochMilli(),
                    zoneId = draft.zone.id,
                    recurrence = draft.recurrence,
                    lastHandledIndex = handled,
                    missedCount = 0
                ).also { repo.update(it) }
            }
            val precision = ReminderCoordinator.rearm(context, saved, now)
            _message.value = when {
                saved.paused -> "Saved. This reminder is paused."
                ReminderPlanner.plan(saved, now).next == null -> "Saved, but nothing is scheduled in the future."
                precision == com.rememberflow.app.notify.AlarmPrecision.INEXACT ->
                    "Saved. Exact alarms are off, so delivery may be a few minutes late."
                else -> "Saved."
            }
            _status.value = readStatus()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderCoordinator.remove(getApplication(), id)
            _message.value = "Reminder deleted."
        }
    }

    fun pause(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderCoordinator.pause(getApplication(), id)
        }
    }

    fun resume(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderCoordinator.resume(getApplication(), id)
        }
    }

    /** Posts a real notification right now so the user can confirm the channel and permissions work. */
    fun sendTestNotification() {
        val context = getApplication<Application>()
        val now = Instant.now()
        val test = Reminder(
            id = TEST_NOTIFICATION_ID,
            title = "Test from RememberFlow",
            details = "If you can read this, notifications are working.",
            startMillis = now.toEpochMilli(),
            zoneId = ZoneId.systemDefault().id,
            recurrence = Recurrence.ONCE,
            paused = false,
            createdAtMillis = now.toEpochMilli(),
            lastHandledIndex = 0
        )
        val posted = NotificationHelper.post(context, test, now, 1, late = false, zone = test.zone)
        _message.value = if (posted) "Test notification sent." else "Android blocked the notification. Check the permission below."
    }

    private fun readStatus(): SystemStatus {
        val context = getApplication<Application>()
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val power = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        return SystemStatus(
            notificationPermissionGranted = permission,
            notificationsEnabled = NotificationHelper.areNotificationsEnabled(context),
            exactAlarmsAllowed = ReminderScheduler(context).canScheduleExact(),
            ignoringBatteryOptimizations = power.isIgnoringBatteryOptimizations(context.packageName)
        )
    }

    companion object {
        /** Distinct from any database id (those start at 1). */
        const val TEST_NOTIFICATION_ID = Int.MAX_VALUE.toLong()
    }
}
