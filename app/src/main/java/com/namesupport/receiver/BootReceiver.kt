package com.namesupport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.namesupport.data.AppPreferences
import com.namesupport.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val firstScanDone = runBlocking { AppPreferences(context).hasCompletedFirstScan.first() }
        if (firstScanDone) {
            WorkManagerScheduler.enqueue(context)
        }
    }
}
