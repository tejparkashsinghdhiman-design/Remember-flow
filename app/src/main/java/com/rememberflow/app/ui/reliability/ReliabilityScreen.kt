package com.rememberflow.app.ui.reliability

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rememberflow.app.ui.SystemStatus
import com.rememberflow.app.ui.components.AppIcons
import com.rememberflow.app.ui.components.GlassCard
import com.rememberflow.app.ui.components.GradientButton
import com.rememberflow.app.ui.components.Pill
import com.rememberflow.app.ui.components.SectionTitle
import com.rememberflow.app.ui.theme.Palette

@Composable
fun ReliabilityScreen(
    status: SystemStatus,
    onRequestNotificationPermission: () -> Unit,
    onSendTest: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "title") {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("Reliability", style = MaterialTheme.typography.headlineMedium, color = Palette.TextHigh)
                Text(
                    "Android decides whether a closed app may wake your phone. Here is exactly what's allowed right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextMid
                )
            }
        }

        item(key = "notifications") {
            StatusCard(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                ok = status.notificationsOk,
                okText = "Allowed",
                badText = "Blocked",
                body = when {
                    status.notificationsOk -> "Reminders can appear on your lock screen and notification shade."
                    !status.notificationPermissionGranted -> "Android 13+ needs your permission before any app may show notifications. Without it, reminders fire silently and you'll never see them."
                    else -> "Notifications for RememberFlow are switched off in system settings."
                }
            ) {
                if (!status.notificationPermissionGranted && Build.VERSION.SDK_INT >= 33) {
                    GradientButton("Allow notifications", onRequestNotificationPermission)
                } else if (!status.notificationsOk) {
                    GradientButton("Open notification settings", { openNotificationSettings(context) })
                }
            }
        }

        item(key = "exact") {
            StatusCard(
                icon = AppIcons.Schedule,
                title = "Exact alarms",
                ok = status.exactAlarmsAllowed,
                okText = "Allowed",
                badText = "Not allowed",
                body = if (status.exactAlarmsAllowed) {
                    "Reminders fire at the chosen second, even when the phone is idle (Doze)."
                } else {
                    "Without \"Alarms & reminders\" access, Android may delay reminders by several minutes to save power. They still arrive, just not on the second."
                }
            ) {
                if (!status.exactAlarmsAllowed) {
                    GradientButton("Allow alarms & reminders", { openExactAlarmSettings(context) })
                }
            }
        }

        item(key = "battery") {
            StatusCard(
                icon = AppIcons.Shield,
                title = "Battery optimisation",
                ok = status.ignoringBatteryOptimizations,
                okText = "Unrestricted",
                badText = "Optimised",
                neutralWhenNotOk = true,
                body = if (status.ignoringBatteryOptimizations) {
                    "RememberFlow is exempt from battery optimisation."
                } else {
                    "Exact alarms normally work with default settings. But some phone makers (Xiaomi, Oppo, Vivo, Huawei, Samsung's \"sleeping apps\") add aggressive task-killing that can block alarms. If reminders ever fail to arrive, exempt RememberFlow here."
                }
            ) {
                if (!status.ignoringBatteryOptimizations) {
                    GradientButton("Open battery settings", { openBatterySettings(context) })
                }
            }
        }

        item(key = "test") {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Try it now", style = MaterialTheme.typography.titleMedium, color = Palette.TextHigh)
                    Text(
                        "Sends a real notification immediately, so you can confirm the sound, vibration and lock-screen behaviour you set for the \"Reminders\" channel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextMid
                    )
                    GradientButton("Send test notification", onSendTest, icon = Icons.Filled.Notifications)
                }
            }
        }

        item(key = "honest-title") {
            Spacer(Modifier.height(6.dp))
            SectionTitle("What to expect, honestly")
        }
        items(HONEST_NOTES, key = { it.first }) { (title, body) ->
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.TextHigh)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMid)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    ok: Boolean,
    okText: String,
    badText: String,
    body: String,
    neutralWhenNotOk: Boolean = false,
    action: @Composable () -> Unit
) {
    val badColor = if (neutralWhenNotOk) Palette.Amber else Palette.Rose
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (ok) Palette.Cyan else badColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.TextHigh, modifier = Modifier.weight(1f))
                Pill(if (ok) okText else badText, color = if (ok) Palette.Mint else badColor)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMid)
            action()
        }
    }
}

private val HONEST_NOTES: List<Pair<String, String>> = listOf(
    "Closed app" to "Reminders are stored in Android's alarm system and in a private database on this phone, so they fire whether or not the app is open.",
    "Restarts" to "After a reboot, an app update, or a clock or time-zone change, RememberFlow re-arms every alarm. Anything that came due while the phone was off arrives as one \"missed\" notification that says how many were skipped.",
    "Force stop" to "If you force-stop RememberFlow from Android's app settings, Android cancels all its alarms until you open the app again. That's an Android rule no app can bypass.",
    "Do Not Disturb" to "Silent modes and Do Not Disturb still apply. The notification will appear but may not make a sound.",
    "Privacy" to "RememberFlow has no internet permission at all. Your reminders never leave this phone and aren't included in cloud backups."
)

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchSafely(context, intent) { openAppDetails(context) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent) { openAppDetails(context) }
    }
}

private fun openBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchSafely(context, intent) { openAppDetails(context) }
}

private fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launchSafely(context, intent) { }
}

private fun launchSafely(context: Context, intent: Intent, fallback: () -> Unit) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        fallback()
    }
}
