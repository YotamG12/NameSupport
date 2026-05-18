package com.namesupport.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.namesupport.data.AppPreferences
import com.namesupport.data.ContactRepository
import com.namesupport.notification.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Periodic safety-net scan that runs every 6 hours even if the ContentObserver
 * missed a change (e.g. the service was killed by the OS).
 *
 * Only runs when battery is not critically low.
 */
class ContactSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        if (!prefs.isFirstRunComplete) return Result.success()

        return try {
            val repository = ContactRepository(applicationContext)
            val pending = repository.getHebrewContactsWithoutPhonetic()
            val count = pending.count { repository.applyPhoneticName(it) }

            if (count > 0) {
                Log.i(TAG, "Periodic sync applied $count transliteration(s)")
                NotificationHelper.showContactsUpdatedNotification(applicationContext, count)
            } else {
                Log.d(TAG, "Periodic sync: nothing new to process")
            }

            prefs.setLastSyncNow()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ContactSyncWorker"
        const val WORK_NAME = "contact_periodic_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ContactSyncWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES   // flex window
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // don't reset timer if already scheduled
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
