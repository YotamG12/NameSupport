package com.namesupport.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.namesupport.data.AppPreferences
import com.namesupport.data.ContactRepository
import com.namesupport.data.db.AppDatabase
import com.namesupport.notification.NotificationHelper
import kotlinx.coroutines.flow.first

class ContactMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        if (!prefs.isMonitoringEnabled.first()) return Result.success()

        NotificationHelper.createChannel(applicationContext)

        val dao = AppDatabase.getInstance(applicationContext).contactRecordDao()
        val handledIds = dao.getAllHandledIds().toSet()

        val repository = ContactRepository(applicationContext)
        val candidates = repository.getHebrewContactsWithoutPhonetic()

        for (contact in candidates) {
            if (contact.id !in handledIds) {
                NotificationHelper.showApprovalNotification(applicationContext, contact)
            }
        }

        return Result.success()
    }
}
