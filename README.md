# NameSupport

**Make "Hey Google, call שרה" actually work.**

Android voice assistants cannot pronounce Hebrew contact names, so commands like *"Hey Google, call דוד"* silently fail. NameSupport fixes this by writing an English phonetic transliteration (e.g. `David`) into the `PHONETIC_GIVEN_NAME` field of each contact — the exact field Google Assistant uses when matching spoken names.

---

## Features

| Feature | Details |
|---|---|
| Manual scan | Scan all contacts, review suggestions with inline editing, apply selected |
| Background monitoring | WorkManager periodic job (every 15 min) detects new Hebrew contacts |
| Real-time detection | JobScheduler ContentUri trigger fires within ~10 seconds of any contacts change |
| Notification approval | New contacts surface as notifications with **Approve / Dismiss / Save to Device** action buttons |
| WhatsApp contacts | Detects contacts saved only in WhatsApp (read-only raw contacts); writes phonetic name via a local raw contact stub; optionally copies the full contact to device storage |
| Reboot survival | `BootReceiver` re-registers background jobs after device restart |
| Settings screen | Pause/resume background monitoring; re-scan previously dismissed contacts |
| 100% offline | All transliteration is done on-device — no network calls, no API keys |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              UI Layer                        │
│  MainActivity          SettingsActivity      │
└──────────────┬──────────────────────────────┘
               │ observes LiveData
┌──────────────▼──────────────────────────────┐
│           ViewModel Layer                    │
│  MainViewModel  (viewModelScope coroutines)  │
└──────────────┬──────────────────────────────┘
               │ calls
┌──────────────▼──────────────────────────────┐
│           Data Layer                         │
│  ContactRepository  (ContentResolver)        │
│  AppDatabase (Room)   AppPreferences (DS)    │
└──────────────┬──────────────────────────────┘
               │
       Android Contacts Provider

─────────── Background ───────────────────────

  WorkManager (15 min periodic)
  JobScheduler (ContentUri trigger, ~10 s)
         │
  ContactMonitorWorker / ContactChangeJobService
         │
  NotificationHelper ──► ApprovalBroadcastReceiver

─────────── Transliteration ──────────────────

  HebrewTransliterator
    Priority 1: dictionary (~480 names + surnames)
    Priority 2: context-aware char-by-char fallback
                (final ה→a, ו between letters→o, י between letters→i)
```

### Key files

| File | Role |
|---|---|
| `app/src/main/java/com/namesupport/MainActivity.kt` | Entry point — permissions, scan/apply, WhatsApp dialog |
| `app/src/main/java/com/namesupport/ui/MainViewModel.kt` | State management; triggers WorkManager + JobScheduler after first apply |
| `app/src/main/java/com/namesupport/data/ContactRepository.kt` | All ContactsContract queries and writes; WhatsApp raw-contact fallback |
| `app/src/main/java/com/namesupport/util/HebrewTransliterator.kt` | Core transliteration engine — offline, no dependencies |
| `app/src/main/java/com/namesupport/worker/ContactMonitorWorker.kt` | Background 15-min scan logic |
| `app/src/main/java/com/namesupport/worker/ContactChangeJobService.kt` | Real-time scan via ContentUri trigger |
| `app/src/main/java/com/namesupport/worker/WorkManagerScheduler.kt` | Enqueue/cancel WorkManager + JobScheduler |
| `app/src/main/java/com/namesupport/receiver/ApprovalBroadcastReceiver.kt` | Handles Approve / Dismiss / Save-to-Device notification actions |
| `app/src/main/java/com/namesupport/notification/NotificationHelper.kt` | Notification builder; adds Save-to-Device button for WhatsApp contacts |
| `app/src/main/java/com/namesupport/data/db/AppDatabase.kt` | Room singleton — tracks APPROVED/DISMISSED contacts |

---

## Permissions

| Permission | Why |
|---|---|
| `READ_CONTACTS` | Scan contact display names |
| `WRITE_CONTACTS` | Write `PHONETIC_GIVEN_NAME`; create local raw contact stubs for WhatsApp contacts |
| `POST_NOTIFICATIONS` | Show approval notifications (runtime on Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Re-register background jobs after device restart |

---

## Build & Run

```bash
# Debug APK (signed, installs directly on device)
./gradlew assembleDebug

# Install on connected device / emulator
./gradlew installDebug

# Unit tests (no device needed)
./gradlew test
```

**Android Studio:** Open the root folder → let Gradle sync → press **Run ▶** (Shift+F10).
Requires Android Studio with an emulator (API 24+) or a physical device with USB debugging enabled.

**Minimum SDK:** API 24 (Android 7.0) — required for `JobScheduler.addTriggerContentUri()`  
**Target SDK:** API 36

---

## How It Works

1. User taps **Scan** → app queries `ContactsContract.Contacts` for Hebrew names missing a phonetic field
2. `HebrewTransliterator` produces a suggestion for each name (dictionary lookup first, then char-by-char fallback)
3. User reviews suggestions, edits if needed, taps **Apply** → app writes `PHONETIC_GIVEN_NAME` via `ContentResolver`
4. After first apply, `WorkManagerScheduler` starts both a 15-min periodic job and a real-time JobScheduler job
5. When a new Hebrew contact is added, a notification appears within ~10 seconds with one-tap approval
6. For WhatsApp-only contacts (read-only raw contacts), a local device raw contact is created and linked via `AggregationExceptions` so the aggregate contact gains the phonetic field without duplicating the contact

---

## Version History

| Version | Highlights |
|---|---|
| v5.0.3 | Real-time detection (JobScheduler), WhatsApp support, improved transliteration, removed Gemini dependency |
| v2.0 | Gemini API transliteration, inline editing |
| v1.0 | Initial release — manual scan & apply |
