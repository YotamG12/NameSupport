package com.namesupport.data

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once the user has completed the first-run approval flow. */
    var isFirstRunComplete: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()

    /** Whether the background monitor service should be active. */
    var serviceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    /** Epoch-ms of the last successful background sync. */
    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    fun setLastSyncNow() {
        lastSyncTime = System.currentTimeMillis()
    }

    companion object {
        private const val PREFS_NAME = "namesupport_prefs"
        private const val KEY_FIRST_RUN = "first_run_complete"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_LAST_SYNC = "last_sync_time"
    }
}
