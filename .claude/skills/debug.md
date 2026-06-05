# Debug Skill

You are a debugging assistant for the **NameSupport** Android app. Your job is to diagnose and fix issues systematically.

## When invoked

Follow this structured debugging workflow:

### Step 1 — Reproduce & isolate
- Ask: "What is the exact symptom?" (crash, wrong output, missing notification, etc.)
- Ask: "On which API level / device?" (minSdk 24, so Android 7.0+)
- Check logcat first: `adb logcat -s NameSupport:V ContactRepository:V ApprovalReceiver:V WorkManager:V`

### Step 2 — Known failure patterns for this app

**WorkManager not firing:**
- Check: `adb shell dumpsys jobscheduler | grep namesupport`
- Battery optimization may kill periodic work → Settings → Battery → Unrestricted for NameSupport
- Verify `ExistingPeriodicWorkPolicy.KEEP` is used (not REPLACE, which resets the timer)
- Min interval is 15 min; on Android 12+ the OS may batch further

**Notifications not appearing:**
- Check `POST_NOTIFICATIONS` permission was granted (Android 13+)
- Verify notification channel exists: `adb shell dumpsys notification | grep namesupport_approval`
- Check `NotificationManagerCompat.areNotificationsEnabled(context)` returns true
- Verify `IMPORTANCE_DEFAULT` — `IMPORTANCE_NONE` silences all notifications

**Contacts not updating:**
- Check `WRITE_CONTACTS` permission was granted
- Verify `getRawContactIds()` returns non-empty list for the contact
- Check if contact is read-only (e.g., synced from Google — can only add phonetic via Google account)
- Log: `ContactRepository` already has `Log.d(TAG, ...)` calls — enable with `adb logcat -s ContactRepository:V`

**Room DAO returning wrong data:**
- Verify `allowMainThreadQueries()` is NOT used in production (only in tests)
- Check `@Insert(onConflict = OnConflictStrategy.REPLACE)` is present on upsert
- Use `adb shell run-as com.namesupport sqlite3 databases/namesupport.db "SELECT * FROM contact_records;"` to inspect live DB

**BroadcastReceiver not called on notification tap:**
- Verify receiver is registered in manifest with correct action strings
- Check `PendingIntent.FLAG_IMMUTABLE` is set (required API 31+)
- Verify the request code is unique per contact (using `contactId.toInt() * 2`)

**BootReceiver not triggering:**
- `RECEIVE_BOOT_COMPLETED` permission must be in the manifest
- `android:exported="true"` required on the receiver
- Test: `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.namesupport`

### Step 3 — Root cause analysis
- Read the full stack trace; identify the top-most app frame (not framework)
- Check if the error is in `ContactRepository`, `ApprovalBroadcastReceiver`, or `ContactMonitorWorker`
- Reproduce in a unit test if possible (Robolectric for ContactRepository, in-memory Room for DB)

### Step 4 — Fix & verify
- Fix the minimal change needed; do not refactor surrounding code
- Re-run `./gradlew test` after the fix
- Test on a real device if the bug is permission- or background-execution-related (emulators behave differently)

## Useful adb commands
```bash
# Force-run the WorkManager task immediately
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.namesupport

# Check scheduled jobs
adb shell dumpsys jobscheduler | grep -A5 namesupport

# Watch logcat for this app only
adb logcat --pid=$(adb shell pidof com.namesupport)

# Simulate boot completed
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.namesupport

# Inspect Room database
adb shell run-as com.namesupport sqlite3 databases/namesupport.db ".tables"
```
