# Security Policy

RememberFlow is an offline Android reminder app. It declares no `INTERNET` permission, keeps reminders in a private local database and opts out of cloud backup. Security reports that help keep it that way are very welcome.

## Supported versions

RememberFlow is a small, single-maintainer project, and only the most recent code is maintained.

| Version | Supported |
| --- | --- |
| `main` branch | Yes |
| Latest published release | Yes |
| Older releases and CI-built APKs of older commits | No. Please update to the latest release first. |

## Reporting a vulnerability

**Please do not open a public issue, pull request or discussion for a security problem.**

Report it privately through GitHub's private vulnerability reporting:

1. Go to the repository's **Security** tab.
2. Choose **Report a vulnerability**.
3. Describe the problem as described below.

If you do not see the **Report a vulnerability** button, private reporting has not been enabled on the repository yet. In that case, open a public issue titled **"Security contact request"** that contains **no technical details at all**, and the maintainer will arrange a private channel. Please don't post the vulnerability itself in that issue.

A useful report includes:

- What the issue is and what an attacker could achieve (for example, read reminder content, trigger unwanted behaviour, or obtain a signing key).
- The affected version or commit, the Android version and the device or emulator.
- Step-by-step reproduction, or a minimal proof of concept.
- Any suggested fix, if you have one.

Please use made-up reminder text in your proof of concept and do not include real personal data.

## What to expect

This is a volunteer project, so these are goals rather than guarantees:

- Acknowledgement of your report within about 7 days.
- An initial assessment, and follow-up questions if needed, within about 14 days.
- A fix or mitigation prioritised by severity, with credit in the release notes if you want it.

## Disclosure

We ask for coordinated disclosure. Please give the maintainer reasonable time to investigate and release a fix before you publish details. Up to 90 days is a common and reasonable window, and we will aim to be well inside it. If you have not heard back, or a fix is taking too long, you are free to discuss next steps with us. We will not pursue action against people who report in good faith, who stay within the scope below, and who avoid accessing other people's data.

## Scope

In scope:

- Exposure or leakage of reminder titles, notes or other user data (for example through logs, backups, notifications shown unexpectedly, exported components or intents).
- Misuse of exported components (`MainActivity`, `BootReceiver`) or crafted broadcasts or intents.
- Any change that adds network access or other unexpected permissions to the app.
- Weaknesses in how the app's database or files are stored on the device.
- Leakage of signing keys or secrets, or unsafe handling of secrets in the GitHub Actions workflows (`.github/workflows/`).
- Vulnerabilities in dependencies that actually affect RememberFlow. Please say how.

Out of scope:

- Reminders not firing after the user force-stops the app, or because a manufacturer's battery manager killed it. These are Android and device limitations, documented in the README.
- Attacks that need a rooted or already compromised device, or physical access to an unlocked phone.
- Social engineering, and attacks on GitHub or other third-party infrastructure.
- Reports from automated scanners with no demonstrated impact on RememberFlow.

## Security expectations for contributors

- Never commit secrets: keystores, passwords, tokens or `keystore.properties`. Signing material belongs in GitHub Actions secrets only.
- Don't add the `INTERNET` permission or network-capable libraries without prior discussion. CI fails if `INTERNET` appears in the built APK.
- Keep non-essential components unexported (`android:exported="false"`), and keep exported ones to the minimum the system requires.
- Don't log reminder contents.
- Keep the third-party GitHub Actions used in workflows to a minimum, and prefer pinned, well-known actions.

## Sensitive data in reports and issues

Reminders can be very personal. Never include real reminder text, names, phone numbers, locations, screenshots of your real data or credentials in a report. If a log or screenshot is needed, redact it first. If you have accidentally exposed a secret, say so in your private report so it can be revoked.
