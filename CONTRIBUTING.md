# Contributing to RememberFlow

Thanks for taking an interest in RememberFlow, a native Android reminder app (Kotlin, Jetpack Compose) whose reminders never leave the device. This guide explains how to set up, build, test and submit changes.

By contributing you agree that your contribution is licensed under the project's [MIT licence](LICENSE). See the "Licence" section of the [README](README.md) for what the licence does and does not cover.

## Ground rules

- **Privacy is a feature.** The app declares no `INTERNET` permission and opts out of cloud backup. The CI build fails if `INTERNET` ever appears in the merged manifest. Please don't add analytics, crash reporters, ad SDKs or anything else that needs network access. If you think a feature genuinely needs it, open an issue and discuss it first.
- **Reliability beats polish.** A reminder app that misses a reminder has failed. Changes to scheduling, notifications or boot handling get extra scrutiny and need to be tested on a real device or emulator (see [Testing Android-specific behaviour](#testing-android-specific-behaviour)).
- **Small, focused changes.** One logical change per pull request is much easier to review than a large mixed one.
- **Be kind.** Assume good faith, keep feedback about the code, and remember this is a small project maintained in spare time.

## Development environment

| Requirement | Version |
| --- | --- |
| JDK | 17 (CI uses Temurin 17; the build targets Java 17) |
| Android SDK | Platform 35 (`compileSdk` / `targetSdk` 35), `minSdk` 26 |
| Android Studio | Ladybug (2024.2) or newer |
| Gradle | Provided by the wrapper (`./gradlew`), currently 8.10.2. Don't install it separately. |

Android Studio will offer to install the missing SDK platform on first sync. For command-line builds, set `ANDROID_HOME` (or create an untracked `local.properties` containing `sdk.dir=/path/to/Android/Sdk`).

## Getting the code and building

```bash
git clone https://github.com/tejparkashsinghdhiman-design/Remember-flow.git
cd Remember-flow

./gradlew assembleDebug      # debug APK in app/build/outputs/apk/debug/
./gradlew assembleRelease    # release APK in app/build/outputs/apk/release/
```

On Windows use `gradlew.bat`. Without signing secrets, the release build is signed with the local debug key. That is fine for development and is described in the README's "Signing" section. You can also open the project folder directly in Android Studio and run the `app` configuration on an emulator or device.

If you fork the repository, you can build an APK without installing anything locally: the **Build APK** workflow runs on every push and pull request and uploads the APK as a workflow artifact.

## Running the tests

```bash
./gradlew testDebugUnitTest
```

The unit tests (JUnit 4, in `app/src/test/`) cover the pure-Kotlin logic in `core/` and the editor state. This is also what CI runs. HTML reports are written to `app/build/reports/tests/testDebugUnitTest/`.

Please follow these expectations:

- New behaviour in `core/` (recurrence maths, planning, codec, row building) comes with unit tests. Bug fixes should include a test that fails without the fix.
- Keep `core/` free of Android framework imports so it stays fast to test on the JVM.
- Time-dependent logic must take the clock or time zone as a parameter rather than reading the system clock directly, so tests are deterministic. Pay attention to daylight-saving boundaries.
- Run the tests before you push. A green **Build APK** check is required before merge.

Android Lint is available through `./gradlew lintDebug`. It is not enforced by CI yet. If you touch a file, please don't introduce new lint warnings in it.

## Code quality expectations

- **Kotlin style:** the official Kotlin code style (`kotlin.code.style=official`). Android Studio's default formatter produces it.
- **Architecture:** keep to the existing layering: `core/` (pure logic) → `data/` (storage) → `notify/` (alarms and notifications) → `ui/` (Compose). UI state lives in view models and `StateFlow`; composables should be as stateless as practical.
- **No needless rewrites.** Please don't reformat or reorganise files you aren't otherwise changing.
- **Dependencies:** adding a dependency needs a justification in the PR (what it does, its licence, its size impact). Versions go in `gradle/libs.versions.toml`.
- **Permissions:** don't add manifest permissions without explaining why in the PR. Every permission must be justified by a user-visible feature.
- **Accessibility:** provide content descriptions for icons and meaningful controls, keep touch targets large enough, and don't convey information by colour alone.
- **User-facing text** belongs in `strings.xml`.
- **Comments** should explain *why*, not restate the code.

## Branches, commits and pull requests

1. Fork the repository (or create a branch if you have write access) and branch from the latest `main`.
2. Name the branch after the change: `fix/…`, `feature/…`, `docs/…`, `refactor/…`, `test/…`, `ci/…`.
3. Write clear commit messages: a short imperative summary (about 72 characters), then a body explaining *why* if it isn't obvious. Example: `Fix missed-occurrence count after time zone change`.
4. Open the pull request against `main` and fill in the template. Link the issue it addresses (`Fixes #123`).
5. For UI changes, include before/after screenshots. Use dummy reminders, not your real ones.
6. Keep the branch up to date with `main` and respond to review comments. Draft pull requests are welcome for early feedback.

For anything larger than a small fix, please open an issue first so we can agree on the approach before you invest the time.

## Reporting bugs and suggesting features

Use the issue forms: **Bug report** or **Feature request**. A good bug report includes exact reproduction steps, the app version, the Android version and the device model, because reminder behaviour varies a lot between manufacturers.

**Please don't paste your real reminders, personal data or credentials into issues, logs or screenshots.** Replace them with placeholder text first.

**Security vulnerabilities must not be reported in public issues.** Follow [SECURITY.md](SECURITY.md) instead.

## Testing Android-specific behaviour

Most of the risk in this app is in how it interacts with Android, not in the Kotlin logic. If your change touches `notify/`, the manifest, permissions or boot handling, test the relevant scenarios on a real device where possible, and list what you tried in the PR.

| Area | What to check |
| --- | --- |
| Android versions | At least one device or emulator at API 26–32 and one at API 33+ (runtime notification permission) |
| Notification permission (API 33+) | Denied, then granted. The app should say clearly that it can't notify. |
| Exact alarms (API 31+) | "Alarms & reminders" access on and off. Off should fall back to inexact alarms and tell the user. |
| App closed | Swipe the app away, then wait for a reminder. |
| Reboot / app update | Reminders are re-armed and missed occurrences are summarised in one notification. |
| Clock and time zone | Change the time, date and time zone. Pending alarms must be recomputed. |
| Doze and battery | Idle mode and battery optimisation on and off |
| Force-stop | Expected to cancel alarms until the app is opened again. This is an Android limitation, not a bug. |

Some useful `adb` commands (package name `com.rememberflow.app`):

```bash
adb shell dumpsys alarm | grep -A4 com.rememberflow.app            # inspect pending alarms
adb shell pm revoke com.rememberflow.app android.permission.POST_NOTIFICATIONS   # API 33+
adb shell cmd appops set com.rememberflow.app SCHEDULE_EXACT_ALARM deny          # API 31+
adb shell dumpsys battery unplug && adb shell dumpsys deviceidle force-idle      # simulate Doze
adb shell dumpsys deviceidle unforce && adb shell dumpsys battery reset          # undo Doze simulation
adb shell am force-stop com.rememberflow.app                       # alarms are cancelled (expected)
adb reboot                                                         # verify re-arming after boot
```

Manufacturer battery managers (Xiaomi, Oppo, Vivo, Huawei, some Samsung modes) can kill background work regardless of what the app does. If you find a device-specific problem, record the exact model and Android version in the issue.

## Handling personal data and secrets

- **Never commit secrets.** Keystores (`*.keystore`, `*.jks`), `keystore.properties` and `local.properties` are git-ignored. Don't remove those entries. Signing material belongs in GitHub Actions secrets only (see the README's "Signing" section) and never in the repository, issues or pull requests.
- If you accidentally commit a secret, don't just delete it in a follow-up commit. Tell the maintainer through the private route in [SECURITY.md](SECURITY.md) so it can be revoked and the history cleaned.
- Use made-up reminder text in tests, screenshots and screen recordings. Reminder titles and notes can be very personal.
- Review `adb logcat` output and screenshots for personal content before attaching them to anything public.
- Don't add code that logs reminder contents.

## Licence

By submitting a contribution you confirm that you wrote it (or have the right to submit it) and that it may be distributed under the [MIT licence](LICENSE). Don't copy code, icons, fonts or other assets from other projects unless their licence permits it. If you do include third-party material, say so in the PR and add its licence to the `licenses/` folder.
