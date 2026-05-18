package com.namesupport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.namesupport.data.AppPreferences
import com.namesupport.service.ContactsMonitorService

/**
 * Restarts the monitor service after the device reboots.
 * Only starts if the user has completed first-run and hasn't disabled the service.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = AppPreferences(context)
        if (!prefs.isFirstRunComplete || !prefs.serviceEnabled) {
            Log.d(TAG, "Boot received but service is not needed (firstRun=${!prefs.isFirstRunComplete})")
            return
        }

        Log.i(TAG, "Boot completed — restarting ContactsMonitorService")
        val serviceIntent = Intent(context, ContactsMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
