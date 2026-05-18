package com.namesupport.service

import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import com.namesupport.data.AppPreferences
import com.namesupport.data.ContactRepository
import com.namesupport.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that watches the contacts database via ContentObserver.
 * When a new Hebrew contact is detected it auto-applies the transliteration
 * without any user interaction — matching the v2 requirement.
 *
 * Restarted by BootReceiver after device reboot (START_STICKY).
 * WhatsApp contacts are caught automatically because they share the same
 * ContactsContract database.
 */
class ContactsMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: ContactRepository
    private lateinit var prefs: AppPreferences
    private var contactsObserver: ContentObserver? = null

    private val handler = Handler(Looper.getMainLooper())

    // Debounce: WhatsApp can sync dozens of contacts in quick succession.
    // We wait 4 s after the last change before processing, so one batch job
    // handles a bulk import instead of firing once per contact.
    private val processRunnable = Runnable { processNewContacts() }

    override fun onCreate() {
        super.onCreate()
        repository = ContactRepository(this)
        prefs = AppPreferences(this)

        val notification = NotificationHelper.buildForegroundNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
        }

        registerObserver()
        isRunning = true
        Log.i(TAG, "Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY  // OS restarts us if killed

    override fun onDestroy() {
        contactsObserver?.let { contentResolver.unregisterContentObserver(it) }
        handler.removeCallbacks(processRunnable)
        serviceScope.cancel()
        isRunning = false
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Observer ──────────────────────────────────────────────────────────────

    private fun registerObserver() {
        contactsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                // Debounce: reset 4 s timer on every change
                handler.removeCallbacks(processRunnable)
                handler.postDelayed(processRunnable, DEBOUNCE_MS)
            }
        }
        contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            contactsObserver!!
        )
    }

    // ── Processing ────────────────────────────────────────────────────────────

    private fun processNewContacts() {
        serviceScope.launch {
            try {
                val pending = repository.getHebrewContactsWithoutPhonetic()
                if (pending.isEmpty()) return@launch

                val count = pending.count { repository.applyPhoneticName(it) }
                Log.i(TAG, "Auto-applied transliteration to $count contact(s)")

                if (count > 0) {
                    NotificationHelper.showContactsUpdatedNotification(this@ContactsMonitorService, count)
                }
                prefs.setLastSyncNow()
            } catch (e: Exception) {
                Log.e(TAG, "processNewContacts failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "ContactsMonitorService"
        private const val DEBOUNCE_MS = 4_000L

        /** Checked by MainActivity to show live service status. */
        var isRunning = false
            private set
    }
}
