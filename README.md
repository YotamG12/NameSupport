# NameSupport

An Android app that automatically transliterates Hebrew contacts into English phonetic names so voice assistants (Google Assistant, Bixby, Samsung Voice) can recognise and dial them correctly.

## The Problem

Hebrew names stored in your contacts are unreadable by voice assistants. Saying "Call יוסי כהן" fails because the assistant cannot parse Hebrew script. NameSupport fixes this by writing a phonetic English version into the contact's phonetic fields — without changing the display name.

## Features

### v1 — Manual Flow
- Scan all contacts for Hebrew names that lack a phonetic field
- Preview the suggested transliteration for each contact
- Select / deselect individual contacts
- Apply changes with one tap

### v2 — Background Monitoring (current)
- **First-run approval screen** — on install, scan all existing contacts and ask for approval once
- **Auto-apply for new contacts** — new contacts added via phone or WhatsApp are transliterated silently, no dialog needed
- **Universal voice assistant support** — writes to both `PHONETIC_GIVEN_NAME` (Google, stock dialer) and `NICKNAME` (Bixby, Samsung)
- **Foreground service** with a `ContentObserver` (4 s debounce) for real-time detection
- **WorkManager 6-hour safety net** — catches any contacts missed while the service was off
- **Boot receiver** — service restarts automatically after a reboot
- **Service toggle** — enable or pause monitoring from the main screen

## Architecture

```
MainActivity
 └── MainViewModel  (MVVM, LiveData, Coroutines)
      ├── ContactRepository   ← ContactsContract queries & writes
      ├── AppPreferences      ← SharedPreferences (first-run, service state)
      └── ContactSyncWorker   ← WorkManager periodic task

ContactsMonitorService  (ForegroundService)
 └── ContentObserver   ← debounced 4 s, triggers processNewContacts()

BootReceiver  ← restarts the service after device reboot
NotificationHelper  ← CHANNEL_MONITOR (persistent) + CHANNEL_SYNC (auto-cancel)
HebrewTransliterator  ← dictionary (120+ names) + character-level fallback
```

## Permissions

| Permission | Reason |
|---|---|
| `READ_CONTACTS` | Scan contacts for Hebrew names |
| `WRITE_CONTACTS` | Write phonetic / nickname fields |
| `FOREGROUND_SERVICE` | Run background monitoring service |
| `FOREGROUND_SERVICE_DATA_SYNC` | Required on API 34+ for data-sync foreground services |
| `RECEIVE_BOOT_COMPLETED` | Restart service after reboot |
| `POST_NOTIFICATIONS` | Show service status and update notifications (Android 13+) |

## How Transliteration Works

1. **Nikud stripping** — vowel diacritics (U+0591–U+05C7) are removed first
2. **Dictionary lookup** — 120+ common Hebrew given names and family names map directly to their standard English spelling (e.g. `יוסי` → `Yossi`, `כהן` → `Cohen`)
3. **Character-level fallback** — unknown words are transliterated letter-by-letter using a phoneme map with position-aware rules:
   - `ב` → `B` at word start, `V` elsewhere
   - `כ` → `K` at word start, `Ch` elsewhere
   - `פ` → `P` at word start, `F` elsewhere
   - `ש` → `Sh`, `ח` → `Ch`, `צ` → `Tz`, etc.
4. Each word is title-cased; words are joined with spaces

## Tech Stack

- **Language**: Kotlin 1.9
- **Min SDK**: 24 (Android 7.0)  |  **Target SDK**: 34
- **Architecture**: MVVM, LiveData, Coroutines
- **Background**: ForegroundService + WorkManager 2.9
- **UI**: Material Design 3, RecyclerView, CardView
- **Build**: AGP 8.2, Gradle 8.6

## Getting Started

### Build from source

```bash
git clone https://github.com/YotamG12/NameSupport.git
cd NameSupport
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Install on device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run unit tests

```bash
./gradlew test
```

### Run instrumented tests (requires device/emulator)

```bash
./gradlew connectedAndroidTest
```

## Branch Strategy

| Branch | Purpose |
|---|---|
| `master` | Stable releases |
| `feature/v2-background-service` | v2 background monitoring (this branch) |

## Changelog

### v2.0
- Background foreground service with ContentObserver
- WorkManager 6 h periodic safety net
- BootReceiver for post-reboot restart
- Dual-field write: PHONETIC_GIVEN_NAME + NICKNAME
- First-run onboarding card + status card with service toggle
- Unit tests (HebrewTransliterator, ContactItem) + instrumented tests (ContactRepository)

### v1.0
- Initial release: manual scan → preview → apply flow
- Hebrew detection and transliteration engine
- RecyclerView with per-contact checkboxes

## License

MIT
