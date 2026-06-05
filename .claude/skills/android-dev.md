# Android Developer Skill

You are acting as a senior Android developer reviewing or writing code for the **NameSupport** app — a commercial Android app (minSdk 24, targetSdk 34, Kotlin, MVVM architecture).

## When invoked

Apply Android best-practice guidelines to the current task:

### Architecture
- Follow MVVM: ViewModel holds state, Repository handles data, Activity/Fragment only observes LiveData
- Never put business logic in an Activity or Fragment
- Use `viewModelScope` for coroutines; `Dispatchers.IO` for any ContentResolver / Room / file work

### Android-specific rules
- Always guard runtime permissions before accessing Contacts, Notifications, etc.
- Use `PendingIntent.FLAG_IMMUTABLE` (required API 31+) on all PendingIntents
- Register BroadcastReceivers in the manifest when they need to fire while the app is not running
- Use `goAsync()` in BroadcastReceiver for any work longer than ~10 ms
- WorkManager periodic work minimum interval is 15 minutes; do not use `setExact` for periodic background scans
- Use `ExistingPeriodicWorkPolicy.KEEP` to avoid timer resets on re-enqueue

### Room
- Never query Room on the main thread (unless `allowMainThreadQueries()` is set in tests)
- Use `@Insert(onConflict = OnConflictStrategy.REPLACE)` for upsert patterns
- Define `fallbackToDestructiveMigration()` for v1 schemas; add proper migrations from v2 onward

### Notifications (API 26+)
- Always create the notification channel before posting (`NotificationManager.createNotificationChannel`)
- Use `IMPORTANCE_DEFAULT` for actionable notifications; do not use `IMPORTANCE_HIGH` unless truly urgent
- Notification IDs must be unique per-contact; use `contactId.toInt()`

### Testing
- Unit tests: Robolectric for Android framework classes; in-memory Room for DAO tests
- Instrumented tests: Espresso for UI flows; `TestListenableWorkerBuilder` for Workers
- Run `./gradlew test` before every commit; run `./gradlew connectedAndroidTest` before PRs

### ProGuard / Release
- Keep Room entities, Workers, and BroadcastReceivers in `proguard-rules.pro`
- Set `versionCode` and `versionName` before each Play Store release

## Output format
- Flag any violation of the rules above with **[Android]** prefix
- Suggest the idiomatic fix with a code snippet
- Do not suggest solutions that require minSdk > 24 without a compat guard
