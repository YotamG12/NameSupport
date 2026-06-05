# NameSupport — CLAUDE.md

## What this project does

NameSupport is a commercial Android app that solves a specific problem: Hebrew-speaking users cannot use voice commands ("Hey Google, call שרה") because Android voice assistants cannot pronounce Hebrew contact names.

The fix: write an English phonetic transliteration into the `PHONETIC_GIVEN_NAME` field of each contact. Android voice assistants use this field when matching spoken names. NameSupport finds Hebrew contacts missing this field, suggests a transliteration, and writes it after user approval.

**No external APIs.** All transliteration is done on-device by `HebrewTransliterator.kt`.

---

## Current version: v5.0

### Key capabilities
- **Manual scan**: Scan all contacts, review transliterations with checkboxes, Apply selected
- **Background monitoring**: After first Apply, WorkManager runs every 15 min detecting new Hebrew contacts
- **Notification approval**: New contacts surface as notifications with Approve/Dismiss action buttons
- **Reboot survival**: `BootReceiver` re-registers WorkManager after device restart
- **Settings screen**: User can pause/resume background monitoring; re-scan dismissed contacts
- **Room persistence**: Tracks APPROVED/DISMISSED contacts so the worker never re-notifies

---

## Architecture

```
MVVM pattern
  View:       MainActivity, SettingsActivity
  ViewModel:  MainViewModel (viewModelScope coroutines)
  Repository: ContactRepository (ContentResolver — Android Contacts Provider)
  DB:         Room (AppDatabase, ContactRecordDao, ContactRecord)
  Prefs:      AppPreferences (Preferences DataStore)

Background
  Worker:     ContactMonitorWorker (CoroutineWorker, every 15 min)
  Scheduler:  WorkManagerScheduler (enqueue/cancel unique periodic work)
  Boot:       BootReceiver (BOOT_COMPLETED → re-enqueue if first scan done)

Notifications
  Helper:     NotificationHelper (channel creation, notification builder)
  Receiver:   ApprovalBroadcastReceiver (Approve/Dismiss via goAsync())

Transliteration
  Utility:    HebrewTransliterator (dictionary + char-by-char fallback, fully offline)
```

---

## Key files

| File | Role |
|---|---|
| `app/src/main/java/com/namesupport/MainActivity.kt` | Entry point; permissions, scan/apply buttons, Settings menu |
| `app/src/main/java/com/namesupport/ui/MainViewModel.kt` | State management; triggers WorkManager after first Apply |
| `app/src/main/java/com/namesupport/data/ContactRepository.kt` | All ContactsContract queries and writes |
| `app/src/main/java/com/namesupport/util/HebrewTransliterator.kt` | Core transliteration engine — do not change without tests |
| `app/src/main/java/com/namesupport/worker/ContactMonitorWorker.kt` | Background scan logic |
| `app/src/main/java/com/namesupport/receiver/ApprovalBroadcastReceiver.kt` | Notification action handler |
| `app/src/main/java/com/namesupport/data/db/AppDatabase.kt` | Room singleton |
| `app/src/main/AndroidManifest.xml` | All permissions + component registrations |
| `app/build.gradle.kts` | KSP + all dependencies |
| `app/proguard-rules.pro` | Keep rules for Room/Worker/Receivers |

---

## Build & run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device / emulator
./gradlew installDebug

# Run unit tests (no device needed)
./gradlew test

# Run instrumented tests (device/emulator required)
./gradlew connectedAndroidTest
```

**Android Studio:** Open the root `C:\NameSupport` folder → let Gradle sync → press Run ▶ (Shift+F10).  
Make sure an emulator (API 34 recommended) or physical device is connected with USB debugging enabled.

---

## Permissions

| Permission | Why |
|---|---|
| `READ_CONTACTS` | Scan contact names |
| `WRITE_CONTACTS` | Write PHONETIC_GIVEN_NAME |
| `POST_NOTIFICATIONS` | Show approval notifications (runtime on API 33+) |
| `RECEIVE_BOOT_COMPLETED` | Re-register WorkManager after reboot |

---

## Coding conventions

- **Kotlin** everywhere; no Java
- **Coroutines** for async; `Dispatchers.IO` for all I/O; never block the main thread
- **No logging of PII** — never log contact names to logcat in release builds
- **No hardcoded strings** — all user-visible text in `res/values/strings.xml`
- **RTL support** — use `Start`/`End` layout attributes; test in RTL mode
- **No external API calls** — this is a commercial offline app; keep it that way

---

## Testing approach

- `HebrewTransliteratorTest` — pure JVM, no device needed
- `ContactRecordDaoTest` — Robolectric + in-memory Room
- `ContactMonitorWorkerTest` — Robolectric + `TestListenableWorkerBuilder`
- `AppDatabaseTest` — instrumented, in-memory Room
- `MainActivityTest` — Espresso UI tests

Run `./gradlew test` before committing. Run `./gradlew connectedAndroidTest` before opening a PR.

---

## Skills available in this project

| Skill | When to use |
|---|---|
| `/android-dev` | Android best practices, permissions, WorkManager, Room patterns |
| `/frontend-design` | Material Design 3, RTL layout, accessibility, notification UX |
| `/debug` | Structured debugging workflow + known failure patterns for this app |
| `/code-review` | Review a diff for correctness and simplification |
| `/security-review` | Security audit before release (PendingIntent flags, exported receivers) |

---

## GitHub workflow

All feature work goes on `feature/v5-background-monitoring` branch.  
Merge to `master` via pull request only.  
Requires: `./gradlew test` green + `/code-review` sign-off.

GitHub MCP is configured in `.claude/settings.json`.  
Set `GITHUB_PERSONAL_ACCESS_TOKEN` environment variable to activate it.
