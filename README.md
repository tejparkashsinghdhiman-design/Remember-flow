# RememberFlow

A native Android reminder app for things you can't afford to forget, built with Kotlin and Jetpack Compose.

Its core idea is the **flow**: a reminder doesn't have to repeat at one fixed interval. You can chain your own stages, for example **10 minutes → 1 hour → 12 hours → repeat**, and RememberFlow walks through that sequence for you, notifying at every step, even when the app is closed.

Everything stays on your phone. The app declares **no `INTERNET` permission**, stores reminders in a private local database, and opts out of cloud backup.

## Features

- Write and save anything you don't want to forget, with optional notes.
- Live 24-hour clock (`HH:mm:ss`) on the dashboard, with the next reminder and a countdown.
- Eight recurrence presets: every 10 minutes, hour, 12 hours, 24 hours, every other day, every 3 days, every 5 days, every week.
- **Custom multi-stage recurrence editor**: create stages (minutes / hours / days / weeks), edit them, reorder them with the arrows, delete them, and choose whether the whole sequence repeats forever or runs once and stops.
- Choose the start date and time (24-hour picker). A live preview shows the next 8 occurrences and the gap between them before you save.
- Edit, pause, resume, search and delete each reminder independently. Search matches title, notes and the recurrence description.
- Chronological **timeline** of upcoming reminders across all active reminders, grouped by day.
- Real notifications through `AlarmManager`: they fire with the app closed, survive reboots, app updates and clock or time-zone changes, and tell you honestly when occurrences were missed.
- A **Reliability** screen showing the live state of notification permission, exact-alarm access and battery optimisation, with one-tap links to fix each, plus a test-notification button.

## How a flow works

A recurrence is a list of **gaps**. The start time you pick is occurrence #1; each stage is the wait until the next occurrence.

| Sequence | Occurrences from a 09:00 start |
| --- | --- |
| `10m → 1h → 12h` (repeating) | 09:00, 09:10, 10:10, **22:10**, 22:20, 23:20, 11:20 (+1 day), … |
| `10m → 1h` (runs once) | 09:00, 09:10, 10:10, then it stops |
| `2d` (repeating) | every other day at 09:00 |

Minutes and hours are exact durations. Days and weeks follow the calendar, so "every 2 days at 09:00" stays at 09:00 across daylight-saving changes, whereas "every 24 hours" is exactly 24 hours and drifts by an hour, as the name says.

## Notifications: what is guaranteed and what isn't

RememberFlow is honest about Android's limits.

| Situation | What happens |
| --- | --- |
| App closed or swiped away | Fires normally. Alarms live in Android's `AlarmManager`, not in the app process. |
| Phone rebooted, app updated, clock or time zone changed | Every alarm is re-armed by a receiver. Anything that came due meanwhile arrives as **one** notification saying how many occurrences were missed, rather than a flood or silence. |
| Android 13+ notification permission denied | The app can't show anything. It says so on the dashboard and the Reliability screen and offers to ask again. |
| Android 12+ "Alarms & reminders" access off | Reminders fall back to inexact alarms and may arrive several minutes late. The app tells you and links to the setting. |
| Phone in Doze / idle | Exact alarms use `setExactAndAllowWhileIdle`, so they still fire (when exact alarms are allowed). |
| You **force-stop** the app in Android settings | Android cancels all its alarms until the app is opened again. No app can prevent this. |
| Aggressive manufacturer battery managers (Xiaomi, Oppo, Vivo, Huawei, some Samsung modes) | Can kill background work and block alarms regardless of what the app does. The Reliability screen links to battery settings; see [dontkillmyapp.com](https://dontkillmyapp.com) for per-brand steps. |
| Do Not Disturb / silent mode | The notification still appears but follows your sound settings. |

Each reminder only ever has **one** pending alarm, for its next occurrence. When it fires, the app calculates what is due from stored state and the clock (not from a counter in memory), posts the notification and arms the next one. That makes it safe to run after any interruption.

## Project layout

```
app/src/main/java/com/rememberflow/app/
  core/      Pure Kotlin (no Android): Recurrence, RecurrenceEngine, ReminderPlanner, ReminderRows, codec
  data/      ReminderRepository: private SQLite storage, exposes a StateFlow
  notify/    ReminderScheduler (AlarmManager), ReminderCoordinator, NotificationHelper, receivers
  ui/        Compose screens: home + live clock, timeline, editor, reliability, theme, components
app/src/test/  JUnit tests for everything in core/
.github/workflows/  build-apk.yml (tests + APK artifact), release.yml (GitHub Release)
```

## Build locally

Requirements: JDK 17 and the Android SDK (platform 35).

```bash
./gradlew testDebugUnitTest      # run the unit tests
./gradlew assembleRelease        # APK at app/build/outputs/apk/release/
```

Android Studio (Ladybug or newer) can open the project directly.

## Build on GitHub

The **Build APK** workflow runs on every push, every pull request and manually: open **Actions → Build APK → Run workflow**. It runs the unit tests, builds the APK, checks it (package info, permissions, signature, and fails if the `INTERNET` permission ever appears) and uploads it as the **RememberFlow-apk** artifact on the run page.

### Publishing a release with a public link

Push a version tag and the **Release APK** workflow attaches the APK to a GitHub Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

(or run the workflow manually and enter a tag). The APK's download link is then `https://github.com/<owner>/<repo>/releases/download/v1.0.0/RememberFlow-v1.0.0.apk`. Note that **anyone can use that link only if the repository is public**; on a private repository only people with access can download it.

### Signing (optional, recommended)

No keys or secrets are stored in this repository. Without secrets the workflows still produce an installable APK, signed with a temporary debug key. Because that key is regenerated on every CI run, installing a newer build over an older one fails with a signature mismatch and you must uninstall first.

For stable updates, create your own keystore once on your machine:

```bash
keytool -genkeypair -v -keystore rememberflow.keystore -alias rememberflow \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 rememberflow.keystore    # copy the output
```

Then add these repository secrets (Settings → Secrets and variables → Actions) and keep the keystore file somewhere safe and **out of git**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | the base64 output above |
| `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
| `ANDROID_KEY_ALIAS` | `rememberflow` |
| `ANDROID_KEY_PASSWORD` | the key password |

## Installing the APK

1. Download the APK on your phone (from the workflow artifact, which is a zip, or from the release page).
2. Open it and allow "Install unknown apps" for your browser or file manager when asked.
3. Google Play Protect may warn about an app it hasn't seen before. That is expected for any APK distributed outside the Play Store.
4. On first run, allow notifications, then open the **Reliability** tab and allow "Alarms & reminders" if it shows a warning.

## Tech

Kotlin 2.0, Jetpack Compose (Material 3), `AlarmManager` exact alarms, SQLite, minSdk 26 (Android 8.0), targetSdk 35. Fonts: Inter and JetBrains Mono, both under the SIL Open Font License (see `licenses/`).

## Known limitations

- Reminders are time-based only (no location triggers) and there is no cloud sync or backup by design.
- Reordering stages uses up/down arrows rather than drag-and-drop.
- Notifications offer a **Pause** action but no snooze button.
